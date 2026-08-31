# Database

PostgreSQL 17, accessed through Prisma. Schema:
[`backend/prisma/schema.prisma`](../backend/prisma/schema.prisma).

Table and column names are `snake_case` in the database and `camelCase` in
Prisma, mapped with `@map`/`@@map`. UUID primary keys throughout.

## Entities

### Institution structure

| Model | Purpose |
|---|---|
| `Institution` | The tenant. Every user belongs to one. |
| `Department` | e.g. Computer Science and Engineering |
| `ClassSection` | A cohort: department + year + section |
| `Subject` | A taught subject, with a code |
| `TimetableSlot` | Subject + teacher + section on a weekday, with times and room |

### People

| Model | Purpose |
|---|---|
| `User` | Credentials, role, status, display name. One per account. |
| `Student` | Student profile, register number, class section |
| `Teacher` | Teacher profile |
| `Administrator` | Administrator profile, designation |
| `TeacherSubject` | Which teacher may teach which subject |
| `Enrollment` | Which student takes which subject |
| `RefreshToken` | Issued refresh tokens, revocable |
| `DeviceToken` | Registered push targets |

`User` holds authentication; the three profile tables hold the domain identity.
A user has exactly one profile, and which one is determined by their role. The
split keeps role-specific fields (register number, designation) out of the
authentication table.

### Attendance

| Model | Purpose |
|---|---|
| `AttendanceSession` | One class period being taken. `ACTIVE` or `CLOSED`. |
| `AttendanceToken` | A minted QR credential. Hashed, expiring, single-use. |
| `AttendanceRecord` | The durable outcome: one row per student per session. |
| `AttendanceCorrection` | An audited amendment to a record. |

### Supporting

| Model | Purpose |
|---|---|
| `Notification` | In-app notification, read/unread |
| `AuditLog` | Append-only trail of who did what |

## Enums

- `UserRole` — `STUDENT`, `TEACHER`, `ADMIN`, `PARENT`
- `AccountStatus` — account enable/disable
- `AttendanceStatus` — `PRESENT`, `LATE`, `ABSENT`
- `SessionStatus` — `ACTIVE`, `CLOSED`
- `AttendanceMethod` — how a record came to exist (QR vs manual)
- `NotificationType` — what a notification is about

`PARENT` is declared but not selectable at sign-in. The architecture must not
prevent adding parent access later, and reserving the enum value now is cheaper
than a migration then.

## The constraints that carry the guarantees

Three schema-level rules do work that application code cannot be trusted to do
alone.

### `AttendanceRecord @@unique([sessionId, studentId])`

One student, one session, one record. This is what makes double-marking
impossible under concurrent scans — two teachers scanning the same student at
the same instant cannot both win, because the second insert fails at the
database. Application checks catch this earlier and produce a nicer message, but
they are the convenience, not the guarantee.

### `AttendanceToken.tokenHash @unique`

Tokens are stored as SHA-256 hashes, never in plaintext. Reading the table gives
an attacker nothing they can present to a scanner. `usedAt` records consumption,
which is what makes a token single-use; `expiresAt` bounds its life to 25
seconds.

### `AuditLog.actor` is `onDelete: SetNull`

Deleting an account must not delete the trail of what it did. The row survives
with a null actor and displays as "Deleted account".

## Indexes

Chosen for the queries that actually run:

- `AttendanceSession(classSectionId, status)` — "is there an active session for
  my class?", asked by every student device on the dashboard.
- `AttendanceToken(sessionId, studentId)` and `(expiresAt)` — verification
  lookup, and expiry sweeps.
- `AttendanceRecord(studentId)` and `(subjectId)` — the aggregation queries
  behind every percentage.
- `AuditLog(actorId)` and `(entityType, entityId)` — tracing an account, or
  tracing one disputed record.

## Aggregation

Percentages are **computed from records, never stored**. A stored percentage is a
second source of truth that will eventually disagree with the records it
summarises, and when it does, nobody can tell which is right.

Aggregation happens in the database, not the application:

```sql
SELECT date_trunc('month', marked_at) AS month,
       COUNT(*) AS held,
       COUNT(*) FILTER (WHERE status <> 'ABSENT') AS attended
FROM attendance_records
GROUP BY 1 ORDER BY 1
```

`LATE` counts as attended — a student who turned up late was present.

## Migrations and seed

```bash
npx prisma migrate deploy   # apply migrations
npx prisma db seed          # demo institution, cohort, timetable, history
```

The seed builds one department, one cohort of 31 students, six subjects, a
timetable and several weeks of attendance history, so that analytics, risk bands
and reports have something real to work with on a fresh database.
