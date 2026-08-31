# Known limitations

An honest list of what this system does not do. Each entry says what is missing
and why it matters, because a limitation nobody understands is worse than one
written down.

## Verification

### Optical QR scanning on a physical handset is unverified

The full path — CameraX preview → ML Kit barcode detection → server verification
→ teacher confirmation — is implemented and configured for real devices. What has
not happened is a person pointing a real phone at a real screen showing a real
code.

No Android handset has been attached to the development machine, and the
emulator's virtual scene cannot be aimed at a QR target from the command line.
The decoding half is covered by `QrRoundTripTest`, which runs the production
encoder and **real ML Kit** on device, so the code the student's screen renders
is known to be a code the scanner's decoder reads. The camera half — focus,
exposure, angle, glare on a phone screen, a dim lecture hall — is not covered by
anything automated.

**This is the one outstanding verification item.** It is listed first because it
sits on the product's central workflow.

## Functionality

### No administrative management interface

Rosters, timetables, subjects and accounts exist in the database and are created
by the seed. There is no screen for adding a student, changing a timetable slot,
or disabling an account. Administrators can read everything and generate reports;
they cannot edit.

### Push notifications are in-app only

Devices can register a push token and the endpoint honestly reports that push
delivery is not configured. Notifications appear inside the app; nothing arrives
on a locked phone. Wiring FCM requires a `google-services.json` and a project,
neither of which belongs in this repository.

### No offline attendance capture

Marking attendance requires connectivity. Reads degrade gracefully — cached
analytics and profiles are served when the network is unreachable — but a write
must reach the server or fail loudly. This is a deliberate choice: a queued
"marked present" that silently fails later is worse than an immediate error,
because the teacher would have moved on believing it worked.

### Single institution, single department

The schema is multi-tenant and every query is scoped by institution, but the
seed and the interface assume one institution and one department. Nothing
structural prevents more; it simply has not been exercised.

### Parent role is declared but not implemented

`UserRole.PARENT` exists in the schema and the enum, and is deliberately not
selectable at sign-in. No parent-facing behaviour ships.

### Attendance corrections are modelled but not exposed

`AttendanceCorrection` exists in the schema for audited amendments to a record.
No interface creates one. A teacher who marks the wrong student has no way to
fix it from the app.

## Security

### No account lockout or multi-factor authentication

Sign-in is rate limited to 10 attempts per minute, which bounds online guessing.
There is no progressive lockout, no notification of failed attempts, and no
second factor. For an institutional deployment holding attendance records that
affect examination eligibility, MFA for staff accounts would be the first
addition.

### Certificate pinning is not implemented

The client trusts the system certificate store. A user who installs a malicious
root CA on their own device could inspect traffic. This matters less than it
sounds — that user can already see their own data — but it would matter for a
staff device.

### The audit trail has no retention policy

It grows without bound and is never pruned or archived. Some deliberate policy is
needed before this runs for a term.

### Rate limiting is per-instance and in-memory

Running more than one backend instance multiplies the effective limits, since
each keeps its own counters. A shared store would be needed behind a load
balancer.

## Operational

### No deployment has been performed

See [DEPLOYMENT.md](DEPLOYMENT.md). The system has been verified running
locally against a real PostgreSQL database. It has never been exposed on a
network, run under a supervisor, or served over TLS.

### The release build was signed with a debug keystore

Done to verify that R8 and the ProGuard keep rules produce a working APK. It
proves the rules are complete; it is not a distributable artefact.

### Demo credentials are weak and shared

Every seeded account uses `Cit@2026`. This is fine for a demonstration and
unacceptable anywhere else.

## Analytics

### Risk scoring is rules, not machine learning

Stated plainly because the alternative would be a lie. `RiskScorer` projects
forward from recent attendance rate and flags consecutive absences and weak
individual subjects. It is transparent and explainable, which is arguably right
for a decision that affects a student — but it is arithmetic, and the product
never calls it AI. The `RISK_SCORER` seam exists so a real model can replace it
without touching callers.

### Trends need history to be meaningful

The monthly trend renders whatever months have data. On a fresh seed that is a
short series, and a chart of two points is not a trend.
