# CIT Attendance — Student Attendance Management System

Native Android application for Chennai Institute of Technology (Computer Science
and Engineering). Kotlin, Jetpack Compose, Clean Architecture + MVVM.

> **Milestone 3 of 4.** Analytics, charts, the attendance calendar, risk
> scoring, notifications and PDF/Excel reports are complete, on top of the
> Milestone 2 QR workflow. Security hardening, performance and final polish are
> Milestone 4.

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

The debug build talks to `http://127.0.0.1:3000/`, tunnelled to the development
machine by adb. Run this once per device or emulator:

```bash
adb reverse tcp:3000 tcp:3000
```

Then open the project in Android Studio and run `app`, or:

```bash
./gradlew :app:installDebug
```

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

### Milestone 3 results

| Check | Result |
| --- | --- |
| Backend typecheck (`tsc --noEmit`) | clean |
| Backend unit tests (risk engine) | 12 passed |
| Android unit tests | 43 passed |
| Compose UI tests (on device) | 13 passed |
| Android lint | 0 errors, 0 warnings |
| Milestone 2 regression suite | 35/35 |
| Milestone 3 suite | 36/36 |

The Milestone 3 suite covers analytics scoping, figure consistency, the honesty
of the risk engine's self-description, absence recording on session close, the
notifications it raises, and all eight report type/format combinations.

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

## Verifying the QR scan

The scan path is verified in two independent halves:

- **Optical decode** — `QrRoundTripTest` runs on a device and pushes the real
  encoder output through the real ML Kit barcode scanner, proving that what the
  student's screen renders is what the teacher's scanner reads.
- **Validation and recording** — the end-to-end suites exercise mint, verify,
  confirm, duplicate, expiry, reuse, wrong session, unenrolled and closed
  session against the live database.

What has *not* been exercised is a physical phone's camera photographing a
second physical screen. The emulator's virtual scene cannot be aimed at a
supplied poster from the command line, and no handset was attached. Everything
between the camera frame and the database is covered; only lighting, focus and
framing remain untested. **Try one scan on a real phone before the demo.**

## Structure

```
core/
  common/         Outcome + AppError — the closed set of failures
  designsystem/   theme tokens, components, brand marks (CIT emblem, butterfly)
  navigation/     routes and the single NavHost
  security/       Android Keystore AES-GCM encryption
  ui/             UiState, error wording, locale-aware formatting
domain/
  config/ model/ repository/ usecase/
data/
  demo/           the seeded Milestone 1 dataset
  local/          encrypted session storage
  repository/     in-memory implementations of the domain interfaces
feature/
  splash/ auth/ shell/ student/ teacher/ admin/ analytics/
di/               Hilt modules — the only place that knows data is mocked
```

`di/RepositoryModule.kt` is the seam: Milestone 2 rebinds each interface to a
Retrofit-backed implementation there, and nothing else changes.

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
