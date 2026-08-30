# CIT Attendance — Student Attendance Management System

Native Android application for Chennai Institute of Technology (Computer Science
and Engineering). Kotlin, Jetpack Compose, Clean Architecture + MVVM.

> **Milestone 2 of 4.** The app now runs against a real NestJS + PostgreSQL
> service. The teacher-verified QR attendance workflow is complete end to end.
> Analytics, charts, reports and notifications are Milestone 3.

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
cd backend && npx tsc --noEmit && npm run build
```

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

## Configuration

The institution name, email domain and attendance threshold are build-time
configuration in `app/build.gradle.kts`, not literals in feature code:

- `INSTITUTION_NAME`
- `INSTITUTION_EMAIL_DOMAIN`
- `ATTENDANCE_THRESHOLD_PERCENT` (currently 75)

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
  splash/ auth/ shell/ student/ teacher/ admin/
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

AGP 9 provides Kotlin support itself, so the standalone `kotlin-android` plugin
is deliberately absent; only the Compose compiler plugin is applied.
