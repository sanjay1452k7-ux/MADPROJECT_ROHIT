# CIT Attendance — Student Attendance Management System

Native Android application for Chennai Institute of Technology (Computer Science
and Engineering). Kotlin, Jetpack Compose, Clean Architecture + MVVM.

> **Milestone 1 of 4.** This milestone delivers the foundation, the design
> system and the sign-in experience. The data layer is intentionally in-memory
> and sits behind the same repository interfaces the real backend will satisfy.
> No QR, attendance session, analytics or backend work is included yet.

## Running it

Open the project in Android Studio and run the `app` configuration, or:

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

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

The last task needs a running emulator or attached device.

## Demo accounts

Every demo account uses the password **`Cit@2026`**. These credentials exist only
in the Milestone 1 in-memory dataset and disappear when the real backend arrives
in Milestone 2.

| Role    | Email                                | Who                            |
| ------- | ------------------------------------ | ------------------------------ |
| Student | `24cs0845@citchennai.net`            | Sanjayan A — primary demo user |
| Student | `24cs0812@citchennai.net`            | Rohit                          |
| Teacher | `meenakshi.s@citchennai.net`         | Dr. Meenakshi Sundaram         |
| Admin   | `academics.office@citchennai.net`    | Dr. Rajalakshmi Narayanan      |

Any of the 31 seeded students can sign in as `<registernumber>@citchennai.net`.

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

AGP 9 provides Kotlin support itself, so the standalone `kotlin-android` plugin
is deliberately absent; only the Compose compiler plugin is applied.
