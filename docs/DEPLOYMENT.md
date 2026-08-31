# Deployment guide

This project has been built and verified as an academic deliverable running
locally. Nothing here has been deployed to a public environment. This document
describes what a real deployment would require — and, just as importantly, what
must change first.

## Before anything is exposed

These are not optional hardening suggestions. Each one is a live problem in the
current configuration.

1. **Replace every seeded credential.** `backend/prisma/seed.ts` creates 34
   accounts with the password `Cit@2026`. The seed is a development fixture and
   must not be run against a real database.
2. **Generate fresh JWT secrets.** Both `JWT_ACCESS_SECRET` and
   `JWT_REFRESH_SECRET`, from a CSPRNG, never reused between environments.
3. **Terminate TLS.** The service speaks plain HTTP and expects a reverse proxy
   in front of it. HSTS is already sent; it is meaningless without TLS.
4. **Remove the debug network configuration.** `app/src/debug/res/xml/network_security_config.xml`
   permits cleartext traffic. It applies only to debug builds, and release
   builds must never be given an `http://` base URL.
5. **Sign the release build with a real key.** The verified release build was
   signed with the debug keystore for testing. A production key must be
   generated, kept out of the repository, and supplied through the build
   environment.

## Backend

```bash
npm ci
npx prisma migrate deploy      # never `migrate dev`, never `db push`
npm run build
node dist/main.js
```

Run under a process supervisor (systemd, or a container orchestrator) so it
restarts on failure. It is stateless apart from the database, so it scales
horizontally without further work.

### Environment

| Variable | Notes |
|---|---|
| `DATABASE_URL` | Postgres connection string |
| `JWT_ACCESS_SECRET` | Freshly generated |
| `JWT_REFRESH_SECRET` | Freshly generated, different from the above |
| `JWT_ACCESS_TTL` | `15m` |
| `JWT_REFRESH_TTL` | `30d` |
| `ATTENDANCE_TOKEN_TTL_SECONDS` | `25` — keep within 20–30 |
| `INSTITUTION_EMAIL_DOMAIN` | Restricts who may hold an account |
| `ATTENDANCE_THRESHOLD_PERCENT` | `75` |
| `CORS_ALLOWED_ORIGINS` | Leave unset unless a browser client exists |
| `PORT` | `3000` |

Secrets belong in the platform's secret store, not in a `.env` file on disk and
never in the repository. `.env` is gitignored and must stay that way.

### Reverse proxy

Terminate TLS, forward to the service, and do not add caching. The service sets
`Cache-Control` deliberately per route — a proxy that caches on its own could
serve one student's dashboard to another. Everything is marked `private`
precisely to prevent that, so a correctly configured shared cache will already
decline; do not override it.

## Database

- Managed PostgreSQL, or self-hosted with automated backups.
- Restrict network access to the application host only.
- Point-in-time recovery matters here more than usual: attendance is a record
  students may dispute months later.
- Migrations are forward-only in production. `prisma migrate deploy` applies
  what exists; it never generates or resets.

## Android

```bash
./gradlew :app:assembleRelease -PcitReleaseApiBaseUrl=https://attendance.example.net/
```

The release build enables R8. `app/proguard-rules.pro` carries keep rules for
Retrofit interfaces, kotlinx.serialization serializers, the network DTOs and ML
Kit — the release build has been assembled, installed and launched to confirm
those rules are complete, since a missing keep rule fails at runtime rather than
at build time.

Distribution for an institution would be a managed enterprise channel or the
Play Store with closed testing. Sideloading an APK trains users to install
unsigned software, which is a poor habit to teach a campus.

## Monitoring

At minimum, watch:

- Sign-in failure rate — a spike is either an outage or an attack.
- `QR_REJECTED` audit entries — a rise suggests either clock skew or someone
  trying codes.
- Session close failures — an unclosed session means absences were never
  recorded, which silently inflates every percentage in that cohort.
- Database connection saturation.

The audit trail (`/admin/audit`) is the record of record for disputes. It should
be backed up with the same seriousness as the attendance data itself, and its
retention policy set deliberately rather than by default.

## What is not production-ready

See [LIMITATIONS.md](LIMITATIONS.md). The most significant for a deployment
decision: push notifications are in-app only, there is no administrative
interface for managing rosters or timetables, and optical QR scanning has not
been verified on a physical handset.
