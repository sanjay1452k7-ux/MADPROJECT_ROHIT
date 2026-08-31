# CIT Attendance — Student Attendance Management System

Native Android application for Chennai Institute of Technology (Computer Science
and Engineering). Kotlin, Jetpack Compose, Clean Architecture + MVVM.

> **Complete — all four milestones.** Premium interface and branding, the
> QR attendance workflow against a real NestJS/PostgreSQL backend, analytics
> and reporting, and a final pass over security, performance, accessibility and
> documentation.

Full documentation is in [`docs/`](docs/):
[Architecture](docs/ARCHITECTURE.md) ·
[Database](docs/DATABASE.md) ·
[API](docs/API.md) ·
[Testing](docs/TESTING.md) ·
[Setup](docs/SETUP.md) ·
[Deployment](docs/DEPLOYMENT.md) ·
[Limitations](docs/LIMITATIONS.md) ·
[Future work](docs/FUTURE.md) ·
[Demo script](docs/DEMO.md)

## Running it

Three things have to be up: PostgreSQL, the backend, and the app.

### 1. Database

The repository expects a PostgreSQL database named `cit_attendance`. A local
instance was provisioned during development under `.localdb/` (gitignored). To
start that one again:

```bash
.localdb/pgsql/bin/pg_ctl -D .localdb/data -l .localdb/server.log -o "-p 55432" start
```

Any PostgreSQL 16+ instance works; point `DATABASE_URL` at it.

### 2. Backend

```bash
cd backend && npm install && npx prisma migrate deploy && npx prisma db seed && npm run build && node dist/main.js
```

Copy `backend/.env.example` to `backend/.env` first and fill in the secrets.

### 3. Android app

The debug build defaults to `http://127.0.0.1:3000/`, tunnelled to the
development machine by adb. Run this once per attached device or emulator:

```bash
adb reverse tcp:3000 tcp:3000
```

Then open the project in Android Studio and run `app`, or:

```bash
./gradlew :app:installDebug
```

**For a phone on the same Wi-Fi instead of USB**, build with the development
machine's LAN address — no code change, no `adb reverse`:

```bash
./gradlew :app:installDebug -PcitApiBaseUrl=http://192.168.1.5:3000/
```

Release builds ignore this and use HTTPS; they carry no cleartext exception at
all.

`local.properties` is generated per machine and is not committed. If Gradle
cannot find the SDK, create it with a single line pointing at your SDK, escaping
the drive colon:

```bash
echo 'sdk.dir=C\:/Users/<you>/AppData/Local/Android/Sdk' > local.properties
```

## Verification

Android:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

The last task needs a running emulator or attached device.

Backend:

```bash
cd backend && npx tsc --noEmit && npx jest && npm run build
```

End-to-end, against a running backend and a seeded database:

```bash
bash scripts/e2e/milestone4.sh
```

### Milestone 4 results

| Check | Result |
| --- | --- |
| Backend typecheck (`tsc --noEmit`) | clean |
| Backend unit tests (risk engine) | 12 passed |
| Android unit tests | 43 passed |
| Compose UI tests (on device) | 13 passed |
| Android lint | 0 findings |
| Milestone 2 regression suite | 35/35 |
| Milestone 3 regression suite | 36/36 |
| Milestone 4 failure matrix | 45/45 |
| Release build with R8 | Builds, installs, launches |
| Large font scale on a compact screen | Verified on device |

Run each end-to-end suite on its own — sign-in is rate limited to ten per
minute, and several suites back to back will exhaust it.

The Milestone 4 suite is the failure matrix: invalid, expired, replayed and
duplicate QR codes; unauthorised roles on every protected route; closed and
double-closed sessions; malformed input; the security headers; and the audit
trail's integrity. The cases and their outcomes are tabulated in
[docs/TESTING.md](docs/TESTING.md).

### What Milestone 4 changed

- **Audit trail made readable.** Every entry already named the acting account;
  it had no reader. Administrators now see it under Insights → Activity, and
  the endpoint refuses everyone else.
- **Query aggregation.** Dashboards counted each student's records in a loop.
  Grouped aggregation in the database made admin analytics 436 ms → 10 ms and
  teacher analytics 207 ms → 92 ms, with cost no longer growing with cohort
  size.
- **Cache policy.** Set in middleware rather than an interceptor, so *error*
  responses carry it too — a rejected sign-in is the response that most needs
  `no-store`.
- **Offline reads.** An 8 MB client cache serves GETs when the network is
  unreachable, and rethrows the original failure when nothing is stored. Writes
  are never served from cache.
- **API hardening.** CORS closed by default, six security headers, `X-Powered-By`
  removed, and throttles on minting and scanning.
- **Accessibility.** Live-region announcements when a scan resolves, and minimum
  touch targets on the roster and calendar.

## Demo accounts

Every demo account uses the password **`Cit@2026`**, created by the backend seed
script. Passwords are stored as Argon2id hashes; the plaintext exists only in
`backend/prisma/seed.ts` and should be changed before any real deployment.

| Role    | Email                                | Who                            |
| ------- | ------------------------------------ | ------------------------------ |
| Student | `24cs0845@citchennai.net`            | Sanjayan A — primary demo user |
| Student | `24cs0812@citchennai.net`            | Rohit                          |
| Teacher | `meenakshi.s@citchennai.net`         | Dr. Meenakshi Sundaram         |
| Admin   | `academics.office@citchennai.net`    | Dr. Rajalakshmi Narayanan      |

Any of the 31 seeded students can sign in as `<registernumber>@citchennai.net`.

## The attendance workflow

A scan never records attendance on its own (Section 17):

1. The teacher opens a session for a subject and cohort.
2. The student sees the session and asks the server for a QR credential. The
   app never mints one itself; the token is 32 bytes of server-side randomness,
   valid for 25 seconds, stored only as a SHA-256 hash.
3. The teacher scans it. The server checks authenticity, expiry, single use,
   session state, enrolment and whether the student is already marked, then
   returns the student's identity - and records nothing.
4. The teacher looks at the student and presses **Mark present**.
5. Only then is attendance written, guarded by a unique constraint on
   (session, student) so a duplicate is impossible even under concurrent taps.

A manual fallback is available per student for a flat battery or a refused
camera, and is recorded as `MANUAL` so an audit can tell the two apart.

## Analytics and risk scoring

Every figure is computed from recorded attendance; nothing is stored
pre-aggregated, so a dashboard can never disagree with the records behind it.

- **Students** see overall and per-subject attendance, a monthly trend, an
  attendance calendar, and a risk assessment.
- **Teachers** see the cohort average, per-subject averages, absences per day,
  and the students below the requirement with the reason for each.
- **Administrators** see the institution average, department and class
  comparison, and how many students are below the requirement.

### The risk engine is not AI

The projection is a **rule-based calculation**, and the product says so: every
assessment carries the sentence *"Rule-based projection from recent attendance
rate"*, which is shown in the interface. It assumes the recent attendance rate
continues for a remaining term the same length as the part already held, and
reports where that lands.

It is defined as a port (`RISK_SCORER` in `backend/src/risk/`). Replacing the
heuristic with a Python ML service later means binding a different provider —
the API, the app and the database do not change. A test asserts the engine never
describes itself as AI or machine learning.

## Notifications

Attendance marked, absence recorded and low-attendance warnings are raised by
the server and appear in the in-app list immediately.

Closing a session is what turns "not scanned" into a recorded absence: every
enrolled student the teacher did not account for is written as `ABSENT` and
notified. Low-attendance warnings are sent only to students whose latest record
took them below the requirement, so nobody is warned repeatedly.

**Push delivery is inert.** Device registration works and tokens are stored, but
no Firebase project exists, so `UnconfiguredPushSender` logs instead of
delivering and the registration endpoint honestly reports
`pushConfigured: false`. Supplying `google-services.json` and a service account,
then binding an FCM sender to `PUSH_SENDER`, is the only change needed.

## Reports

PDF and Excel are rendered by the server from one shared report model, so the
two formats cannot disagree. Each carries the institution, department, class,
subject, period, generation timestamp and statistics.

Scope is enforced server-side: a student can only ever generate their own
attendance report, whatever the request body says.

## Configuration

The institution name, email domain and attendance threshold are build-time
configuration in `app/build.gradle.kts`, not literals in feature code:

- `INSTITUTION_NAME`
- `INSTITUTION_EMAIL_DOMAIN`
- `ATTENDANCE_THRESHOLD_PERCENT` (currently 75)

## The camera on a real phone

The scanner is written for an ordinary handset, not for the development
environment. The production path is: phone camera → CameraX → ML Kit →
backend verification → teacher confirmation → attendance.

- **The device's own camera.** `QrScannerView` asks CameraX for the back camera
  and falls back to the front camera on hardware without one. There is no
  reference anywhere in `app/src/main` to an emulator, a laptop webcam, or a
  specific AVD.
- **Permission** is declared in the manifest and requested at runtime. Refusal
  is a normal state, not an error: the screen explains what is needed, offers
  **Allow camera**, offers **Open app settings** for a permanently blocked
  permission, and points out that attendance can still be recorded manually.
  Revoking the permission while the app runs is handled the same way.
- **Lifecycle.** The scanner lives inside a tab rather than its own destination,
  so the camera is explicitly unbound when the composable leaves. Verified on
  device: the camera binds on entry, is fully released on exit
  (`Active Camera Clients: []`), and rebinds cleanly across repeated cycles with
  no crash.
- **Failure is surfaced.** If the camera cannot start — held by another app, or
  absent — the screen says so and points to the manual fallback, rather than
  showing a black rectangle.
- **Frames are dropped, not queued**, so a slow verification round trip cannot
  build a backlog of stale codes, and rotation comes from the device's own
  sensor so a code reads however the phone is held.

### What is verified, and what is not

| Link in the chain | Status |
| --- | --- |
| Encoder output decodes through ML Kit | Verified on device (`QrRoundTripTest`) |
| Camera binds, releases and rebinds | Verified on device via `dumpsys media.camera` |
| Permission grant, denial and revocation | Verified on device |
| Token mint, verify, confirm, expiry, reuse, duplicate | Verified against the live database |
| **A physical phone's camera photographing a real screen** | **Not verified — no handset was attached** |

Everything between a camera frame and the database is covered. What remains is
the physical optics: lighting, focus and framing against a real screen. Install
the APK on your phone, run one scan, and that last link is closed.

## Structure

```
core/
  common/         Outcome + AppError — the closed set of failures
  designsystem/   theme tokens, components, brand marks (CIT emblem, butterfly)
  navigation/     routes and the single NavHost
  network/        Retrofit, interceptors, token refresh, offline cache
  security/       Android Keystore AES-GCM encryption
  ui/             UiState, error wording, locale-aware formatting
domain/
  config/ model/ repository/ usecase/
data/
  local/          encrypted session storage
  mapper/         DTO to domain conversion
  repository/     Retrofit-backed implementations of the domain interfaces
feature/
  splash/ auth/ shell/ student/ teacher/ admin/ analytics/
di/               Hilt modules — the only place that knows which data source is in use
```

`di/RepositoryModule.kt` is the seam. Milestone 1 bound each domain interface to
an in-memory implementation; Milestone 2 rebound them to Retrofit-backed ones
there, and no screen or ViewModel changed.

## Toolchain

| Component        | Version                        |
| ---------------- | ------------------------------ |
| AGP              | 9.3.2 (built-in Kotlin)        |
| Gradle           | 9.7.1                          |
| Kotlin / Compose | 2.4.10 compiler plugin         |
| Compose BOM      | 2026.08.00                     |
| Hilt / KSP       | 2.60.1 / 2.3.11                |
| compileSdk       | 37                             |
| targetSdk        | 37                             |
| minSdk           | 26 (Android 8.0)               |
| JDK              | 17                             |
| NestJS           | 11.2.3                         |
| Prisma           | 6.19.3                         |
| PostgreSQL       | 17.6                           |
| Node             | 22                             |
| pdfkit / exceljs | 0.20.2 / 4.4.0                 |

AGP 9 provides Kotlin support itself, so the standalone `kotlin-android` plugin
is deliberately absent; only the Compose compiler plugin is applied.
