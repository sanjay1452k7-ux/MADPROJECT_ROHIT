# Architecture

## The shape of the system

Two deployable pieces and one database:

```
Android app (Kotlin, Compose)  ──HTTPS/JSON──▶  NestJS service  ──▶  PostgreSQL
```

The app holds no business rules that matter. Every decision that could be
abused — whether a QR code is valid, whether a person may open a session,
whether attendance may be recorded — is made by the service. The app's job is to
present state and collect intent.

This is deliberate. A mobile client is fully under the control of whoever holds
the phone; its APK can be unpacked, its traffic inspected, its checks patched
out. Treating the client as an enforcement point would mean the strongest
attendance guarantee in the system is "the student did not modify the app".

## Android: layers

Clean Architecture with MVVM in the presentation layer.

```
feature/          Compose screens + ViewModels
   │  depends on
domain/           models, repository interfaces, use cases   ← no Android imports
   │  implemented by
data/             DTO mappers, repository implementations
core/network/     Retrofit, interceptors, token handling
di/               Hilt modules
```

The dependency arrow only ever points inward. `domain/` has no knowledge of
Retrofit, Compose, or Android at all — it is plain Kotlin, which is why its use
cases are testable without a device.

### The swap seam

`domain/repository/` declares interfaces; `data/repository/NetworkRepositories.kt`
implements them against the live API; `di/RepositoryModule.kt` binds one to the
other. That binding is the only place that knows which implementation is in use.

This was the mechanism that carried the project from Milestone 1 (fakes, no
backend) to Milestone 2 (real service) without touching a single screen or
ViewModel. It is also how a future offline-first cache would be introduced.

### State handling

Every screen renders one of exactly three states:

```kotlin
sealed interface UiState<out T> {
    data object Loading
    data class Content<T>(val data: T)
    data class Failed(val error: ErrorPresentation)
}
```

Nothing can accidentally show a spinner and a result at once, and no screen can
forget to handle failure — the `when` is exhaustive.

Results cross layer boundaries as `Outcome<T>`: either `Success` or a `Failure`
carrying an `AppError` from a closed set. Exceptions are converted to `AppError`
exactly once, at the network boundary. `ErrorPresentation` turns an `AppError`
into title/message/retryable in exactly one place, so no technical string can
reach a screen (Section 37).

## Backend: modules

NestJS modules, one per bounded area:

| Module | Responsibility |
|---|---|
| `auth` | Sign-in, refresh, sign-out, principal resolution |
| `attendance` | Sessions, QR minting, verification, marking |
| `students` / `teachers` / `admin` | Role-scoped reads |
| `analytics` | Aggregated figures and trends |
| `risk` | Attendance risk scoring |
| `reports` | PDF and Excel generation |
| `notifications` | In-app notifications, device registration |
| `common` | Prisma, audit, errors, guards, shared aggregation |

Guards run in a fixed order: `JwtAuthGuard` establishes who is calling,
`RolesGuard` decides whether that role may call this route. Both are applied at
the controller, so a new route cannot be added without a role decision being
made.

### Errors

One `ApiError` type with a closed `ApiErrorCode` set maps to HTTP status codes in
a single place. The client's `ApiErrorMapper` maps those same codes back into
`AppError`. Adding an error means touching both ends, which is the point: an
error the client cannot render is not a shippable error.

## The attendance workflow

The core sequence, and the reason the system is built the way it is:

```
 teacher                    student                     service
   │                           │                           │
   │ open session ─────────────┼──────────────────────────▶│  ACTIVE
   │                           │ request code ────────────▶│  mint token
   │                           │◀──── opaque token ────────│  (25s, hashed at rest)
   │◀─ scan QR (camera) ───────│                           │
   │ verify token ─────────────┼──────────────────────────▶│  validate + consume
   │◀─ student identity ───────┼───────────────────────────│  NOTHING RECORDED YET
   │                           │                           │
   │ [teacher looks at the person in front of them]        │
   │                           │                           │
   │ MARK PRESENT ─────────────┼──────────────────────────▶│  record attendance
   │ close session ────────────┼──────────────────────────▶│  CLOSED + absences
```

Two properties matter here.

**A scan never records attendance.** Verification and marking are separate calls.
Between them sits a human looking at a face. This is what stops a student
scanning a screenshot of a friend's code — the friend is not in the room.

**Closing records absences.** Without that step an unmarked student is simply
missing from the data, and every percentage silently counts only the classes
they attended. Closing turns "not scanned" into a recorded `ABSENT`.

### QR token design

- Minted **server-side only**. The client never holds a signing key, so it
  cannot forge a code even in principle.
- **Opaque**: 32 random bytes. It carries no student data, so intercepting one
  reveals nothing about who it belongs to (Section 19).
- **Hashed at rest** (SHA-256). Reading the database does not yield usable codes.
- **Single-use**: consumed at verification. A replay is refused.
- **Short-lived**: 25 seconds, configurable within the 20–30s band.

The database's unique constraint on `(sessionId, studentId)` is the final
backstop. Even if every check above were bypassed, the second record for one
student in one session cannot exist.

## Risk scoring

Rule-based, and described as such. `RiskService` gathers signals; a `RiskScorer`
behind the `RISK_SCORER` injection token turns signals into a band and a reason.

The split exists so the rules can be replaced by a model without touching the
callers: a Python service consuming the same `RiskSignals` would be a new
`RiskScorer` binding and nothing else. What the product must never do is call
the current arithmetic "AI" (Section 31) — the `method` field says
"Rule-based projection from recent attendance rate", and a test asserts that it
makes no machine-learning claim.

## Performance

Dashboards originally counted each student's records in a loop — two queries per
student, so a screen's cost grew with the cohort. `AttendanceStatsService`
replaced that with grouped aggregation (`groupBy`, and `date_trunc` +
`COUNT(*) FILTER` for trends), making each screen a fixed number of queries
regardless of cohort size.

Measured, same data, before → after:

| Endpoint | Before | After |
|---|---|---|
| Admin analytics | 436 ms | 10 ms |
| Teacher analytics | 207 ms | 92 ms |
| Teacher dashboard | 58 ms | 22 ms |
| Admin dashboard | 42 ms | 19 ms |

## Caching

The server states cacheability per response; the client honours it.

- `no-store` for writes, anything under `/auth`, live session state, and the
  audit trail. A stale "session active" or "already marked" is worse than an
  error, because the user would act on it.
- `private, max-age=60` for analytics and profiles.
- `private, max-age=30` otherwise.

Everything is `private` — these responses are one person's data and must not be
held by a shared proxy. This is set in middleware rather than an interceptor
specifically so that *error* responses carry it too; a rejected sign-in is the
response that most needs `no-store`.

On the client, an 8 MB OkHttp disk cache plus `OfflineCacheInterceptor` serves a
cached copy when the network is unreachable, for GET requests only. If nothing
is stored, the original network failure is rethrown, so the interface says "no
internet connection" rather than showing a blank screen.

## Audit trail

Append-only. Sign-ins, session lifecycle, every mint, scan, rejection, mark and
report generation are recorded with the acting account, and surfaced to
administrators under Insights → Activity.

Writing an audit row never fails the operation being audited — failures are
logged and swallowed. An audit system that can take down attendance marking
would be its own outage.
