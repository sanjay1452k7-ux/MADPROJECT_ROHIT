# API

Base URL in development: `http://127.0.0.1:3000`. JSON in, JSON out.

## Authentication

Bearer access tokens (JWT), 15 minutes, with a 30-day refresh token. Every route
except `/auth/login` and `/auth/refresh` requires
`Authorization: Bearer <accessToken>`.

The client refreshes transparently: an OkHttp `Authenticator` catches a 401,
exchanges the refresh token once, and replays the original request. Two failures
in a row sign the user out rather than looping.

### `POST /auth/login`

```json
{ "email": "24cs0845@citchennai.net", "password": "…", "role": "STUDENT" }
```

The role is part of the credential check, not a hint. An account holding a
different role is refused with `ROLE_MISMATCH` (403) even when the password is
correct — signing in as the wrong role is a mistake worth naming rather than
silently correcting.

Returns `accessToken`, `refreshToken`, and the authenticated user.

| Route | Purpose |
|---|---|
| `POST /auth/login` | Sign in |
| `POST /auth/refresh` | Exchange a refresh token |
| `POST /auth/logout` | Revoke the refresh token |
| `GET /auth/me` | The current principal |

## Attendance sessions

All teacher-only unless noted. This is the workflow described in
[ARCHITECTURE.md](ARCHITECTURE.md#the-attendance-workflow).

| Route | Role | Purpose |
|---|---|---|
| `POST /attendance-sessions` | Teacher | Open a session for a subject + section |
| `GET /attendance-sessions/:id` | Teacher | Live summary and roster |
| `POST /attendance-sessions/:id/close` | Teacher | Close, and record absences |
| `POST /attendance-sessions/:id/token` | **Student** | Mint this student's QR credential |
| `POST /attendance-sessions/:id/verify` | Teacher | Validate a scanned code, identify the student |
| `POST /attendance-sessions/:id/mark` | Teacher | Confirm the student is present |
| `POST /attendance-sessions/:id/manual` | Teacher | Record attendance without a scan |

### Opening and closing are idempotent

`POST /attendance-sessions` for a class that already has an active session
returns that session rather than creating a second one, and closing an already
closed session returns its summary without re-recording absences. Both exist so
that a retry on a flaky connection cannot corrupt the record — which is the
normal case on a phone in a lecture hall, not an edge case.

### `verify` then `mark`

`verify` consumes the token and returns the student's identity. **It records
nothing.** The response exists so the teacher can look at the person in front of
them before confirming.

`mark` takes the `verificationId` from that response and creates the attendance
record. A `verificationId` can be marked once.

### `POST /attendance-sessions/:id/token` (student)

Returns an opaque token and its `expiresAt`, 25 seconds out. A student who is
already marked present receives `409 ALREADY_MARKED` with the time they were
marked, so their app can say so instead of rendering a code that will be
refused.

## Role-scoped reads

| Route | Role |
|---|---|
| `GET /students/me/dashboard` | Student |
| `GET /students/me/profile` | Student |
| `GET /students/me/attendance` | Student |
| `GET /students/me/active-session` | Student |
| `GET /teachers/me/dashboard` | Teacher |
| `GET /admin/dashboard` | Admin |
| `GET /admin/audit?limit=` | Admin |
| `GET /analytics/student` | Student |
| `GET /analytics/teacher` | Teacher |
| `GET /analytics/admin` | Admin |

`/me` routes derive the subject from the token. No route accepts a student id
from the caller for their own data — that would let anyone read anyone by
changing a path segment.

`/admin/audit` is capped at 200 entries per request regardless of `limit`, so
the trail cannot be drained in one call.

## Notifications and reports

| Route | Purpose |
|---|---|
| `GET /notifications` | List |
| `GET /notifications/unread-count` | Badge count |
| `PATCH /notifications/:id/read` | Mark one read |
| `PATCH /notifications/read-all` | Mark all read |
| `POST /notifications/device-token` | Register a push target |
| `DELETE /notifications/device-token/:token` | Deregister |
| `POST /reports` | Generate a PDF or Excel report |

Report types: `CLASS_ATTENDANCE`, `SUBJECT_ATTENDANCE`, `LOW_ATTENDANCE`,
`ABSENTEE`, `STUDENT_ATTENDANCE`. Formats: `PDF`, `XLSX`. Staff report types are
refused for students.

## Errors

Uniform shape:

```json
{ "code": "QR_EXPIRED", "message": "That code has expired. Ask for a new one." }
```

| Code | Status | Meaning |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `ACCOUNT_INACTIVE` | 403 | Account disabled |
| `ROLE_MISMATCH` | 403 | Account does not hold the selected role |
| `SESSION_EXPIRED` | 401 | Access token expired |
| `NOT_AUTHORISED` | 403 | Role may not do this |
| `NOT_FOUND` | 404 | No such entity |
| `VALIDATION` | 400 | Malformed request |
| `RATE_LIMITED` | 429 | Too many requests |
| `QR_INVALID` | 400 | Not a code this service issued |
| `QR_EXPIRED` | 410 | Past its 25-second window |
| `QR_ALREADY_USED` | 409 | Replayed code |
| `SESSION_CLOSED` | 409 | Session is no longer taking attendance |
| `NOT_ENROLLED` | 403 | Student is not enrolled in this subject |
| `ALREADY_MARKED` | 409 | Attendance already recorded |

The `message` is written for a person to read. The client maps `code` — never
the message — onto its own error type, so wording can change without breaking
the app.

## Rate limits

| Route | Limit |
|---|---|
| `POST /auth/login` | 10 / minute |
| `POST /auth/refresh` | 20 / minute |
| `POST /attendance-sessions/:id/token` | 20 / minute |
| `POST /attendance-sessions/:id/verify` | 120 / minute |
| Everything else | 120 / minute |

Sign-in is the tightest because it is the one route where guessing pays.
Minting is bounded because a code lasts 25 seconds — a legitimate student
refreshes a few times a minute, so 20 is generous while still bounding how fast
codes could be harvested. Scanning is deliberately loose: a queue of students is
the normal case.

## Response headers

Every response carries `X-Content-Type-Options: nosniff`,
`X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`,
`Cross-Origin-Resource-Policy: same-site`,
`Permissions-Policy: camera=(), geolocation=(), microphone=()` and HSTS.
`X-Powered-By` is disabled.

`Cache-Control` is set per route — see
[ARCHITECTURE.md](ARCHITECTURE.md#caching). CORS is refused entirely unless
`CORS_ALLOWED_ORIGINS` is set; the Android client is not a browser and does not
need it.
