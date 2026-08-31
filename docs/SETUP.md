# Setup guide

Getting the system running on a fresh development machine.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 | Required by AGP 9 |
| Android Studio | Ladybug or newer | Or just the command-line SDK |
| Android SDK | API 37 | `compileSdk` / `targetSdk` |
| Node | 22 | |
| PostgreSQL | 16+ | 17.6 used in development |

Gradle itself is fetched by the wrapper — do not install it separately.

## 1. Database

Any PostgreSQL 16+ instance works. Create a database named `cit_attendance` and
point `DATABASE_URL` at it.

A portable instance was provisioned during development under `.localdb/`
(gitignored, port 55432). To start that one again:

```bash
.localdb/pgsql/bin/pg_ctl -D .localdb/data -l .localdb/server.log -o "-p 55432" start
```

## 2. Backend

```bash
cd backend
cp .env.example .env
```

Fill in `.env`. The two JWT secrets must be generated, not invented:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"
```

Then:

```bash
npm install
npx prisma migrate deploy
npx prisma db seed
npm run build
node dist/main.js
```

The service listens on port 3000. The seed creates one institution, a
Computer Science department, a cohort of 31 students, six subjects, a timetable
and several weeks of attendance history — enough for analytics, risk bands and
reports to show something real.

### Verifying it came up

```bash
curl -s -X POST http://127.0.0.1:3000/auth/login -H "Content-Type: application/json" -d '{"email":"24cs0845@citchennai.net","password":"Cit@2026","role":"STUDENT"}'
```

An `accessToken` in the response means database, migrations and seed are all
good.

## 3. Android app

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

The debug build points at `http://127.0.0.1:3000/` by default. Because that
address means "this device", the device needs the host's port forwarded to it —
run this **once per attached device or emulator**, and again after any
reconnection:

```bash
adb reverse tcp:3000 tcp:3000
```

`adb reverse` is used rather than the host's LAN IP because a typical Windows
firewall blocks inbound TCP to the development machine even when ICMP is
allowed — the emulator can ping the host and still not reach port 3000.

To point a build at a different service instead:

```bash
./gradlew :app:assembleDebug -PcitApiBaseUrl=http://192.168.1.20:3000/
```

## 4. Signing in

Every demo account uses the password `Cit@2026`.

| Role | Email |
|---|---|
| Student | `24cs0845@citchennai.net` |
| Teacher | `meenakshi.s@citchennai.net` |
| Admin | `academics.office@citchennai.net` |

Any of the 31 seeded students can sign in as `<registernumber>@citchennai.net`.

These credentials exist in `backend/prisma/seed.ts` and are for development
only. See [DEPLOYMENT.md](DEPLOYMENT.md) before exposing this to anyone.

## Troubleshooting

**The app shows "no internet connection" on a fresh emulator.**
`adb reverse` has not been run, or the emulator was restarted since. Run it
again.

**`prisma migrate deploy` cannot connect.**
The database is not running, or `DATABASE_URL` has the wrong port. The portable
instance uses 55432, not the default 5432.

**Sign-in suddenly returns 429.**
The rate limiter (10 sign-ins per minute) is doing its job. Wait a minute. If
you are running the end-to-end suites, run them one at a time.

**Gradle fails with `ProjectTypeBinding` / `NoClassDefFoundError`.**
Gradle's configuration cache is incompatible with this AGP version. Ensure
`org.gradle.configuration-cache` is not enabled in `gradle.properties`.

**`local.properties` path errors on Windows.**
Java properties files treat `\` as an escape. Use forward slashes:
`sdk.dir=C\:/Users/you/AppData/Local/Android/Sdk`.
