# Testing

## What runs

| Suite | Count | Command |
|---|---|---|
| Android unit | 43 | `./gradlew :app:testDebugUnitTest` |
| Android instrumented | 13 | `./gradlew :app:connectedDebugAndroidTest` |
| Android lint | 0 findings | `./gradlew :app:lintDebug` |
| Backend unit | 12 | `cd backend && npx jest` |
| Backend typecheck | clean | `cd backend && npx tsc --noEmit` |
| Milestone 2 end-to-end | 35 checks | `bash scripts/e2e/milestone2.sh` |
| Milestone 3 end-to-end | 36 checks | `bash scripts/e2e/milestone3.sh` |
| Milestone 4 failure matrix | 45 checks | `bash scripts/e2e/milestone4.sh` |

All green as of the Milestone 4 commit.

The end-to-end scripts run against a live backend and a seeded database. They
sign in as real seeded accounts and drive the real HTTP API — no mocks, no test
doubles, no in-memory database.

## Android unit tests

Plain JVM tests over `domain/` and the mapping layer, which is where the logic
that can be wrong without a device lives.

- `ApiErrorMapperTest` — every server error code maps to an `AppError`, and
  transport failures become the right one. A code with no mapping is the bug
  this catches.
- `AnalyticsMappersTest` — DTO to domain conversion, including the rounding that
  determines whether a student reads as above or below threshold.
- `AttendancePolicyTest` — threshold and standing bands.
- `InstitutionConfigTest` — email domain rules.
- `SignInUseCaseTest` — sign-in outcomes over an in-memory session store.

## Android instrumented tests

Run on a device or emulator, because they need the real framework.

- `QrRoundTripTest` — generates a QR with the production encoder and decodes it
  with **real ML Kit** on the device. This is the closest automated check to the
  physical scanning path: it proves the code the student's screen renders is a
  code the scanner's decoder actually reads.
- `RoleSelectionScreenTest`, `SegmentedTabsTest` — Compose interaction.

## Backend unit tests

`heuristic-risk-scorer.spec.ts` covers the risk engine: band boundaries, the
recent-versus-prior trend comparison, consecutive absences, and the per-subject
signal. One test asserts the `method` string makes no AI or machine-learning
claim — Section 31 is a product requirement, so it is tested like one.

## End-to-end suites

Three bash suites under `scripts/e2e/` drive the live API.

**Milestone 2 (35 checks)** — the attendance workflow end to end: sign-in for all
three roles, session lifecycle, minting, verification, marking, and the
consistency of dashboard figures against analytics.

**Milestone 3 (36 checks)** — analytics scoping, trend and calendar data,
risk scoring honesty, absence recording on close, notifications, and report
generation in both formats for every staff report type.

**Milestone 4 (45 checks)** — the failure matrix below, plus API security
headers, audit integrity and input validation.

### The failure matrix

Every way the workflow can be abused or go wrong, and what the system does:

| Case | Expected | Result |
|---|---|---|
| Wrong password | 401 refused | ✅ |
| Right password, wrong role | 403 `ROLE_MISMATCH` | ✅ |
| No token / garbage token | 401 | ✅ |
| Student opens a session | 403 | ✅ |
| Student marks attendance | 403 | ✅ |
| Student or teacher reads the audit log | 403 | ✅ |
| Admin mints a student QR | 403 | ✅ |
| **Invalid QR** (never issued) | 400 `QR_INVALID` | ✅ |
| **Expired QR** (past 25s) | 410 `QR_EXPIRED` | ✅ |
| **Replayed QR** (already scanned) | 409 `QR_ALREADY_USED` | ✅ |
| **Duplicate** — already-present student mints again | 409 `ALREADY_MARKED` | ✅ |
| Same verification marked twice | 409 | ✅ |
| Scan against a **closed session** | 409 `SESSION_CLOSED` | ✅ |
| Mint against a closed session | 409 `SESSION_CLOSED` | ✅ |
| Second session for the same class | Returns the existing one | ✅ |
| Closing twice | Idempotent, no double absences | ✅ |
| Verify alone records attendance | Records nothing | ✅ |
| Malformed request body | 400, not a crash | ✅ |
| Unknown session id | Clean 404 | ✅ |
| `limit=99999` on the audit log | Capped at 200 | ✅ |

**Low attendance** is covered by the Milestone 3 suite: students below the
threshold appear in the at-risk list worst-first, every listed student is
genuinely below threshold, and each reason quotes the same figure displayed
beside it.

**Network loss** is covered on the client rather than by these suites:
`OfflineCacheInterceptor` serves a cached copy of a GET when the network is
unreachable and rethrows the original failure when nothing is stored, so the
interface shows "no internet connection" rather than a blank screen. Writes are
never served from cache — a mark must reach the server or fail loudly.

### Running them

Run each suite **on its own**. Sign-in is rate limited to 10 per minute, and
several suites back to back will exhaust it. Each suite aborts with a clear
message if it could not obtain tokens, rather than reporting every check as a
failure and hiding the cause.

## Manual device verification

Performed on the emulator against the live backend:

- Admin sign-in → Insights → Activity renders the audit trail with real actions,
  actor names, roles and timestamps.
- The interface holds together at **1.5× font scale on a 360dp-wide screen** —
  all content present, no clipping, no crash.
- Camera lifecycle: leaving the scanner screen releases the camera
  (`Active Camera Clients: []` after exit), so it is not held open in the
  background.
- Release build with R8: builds, installs, launches, no missing-class crash.

## What has not been verified

**Optical QR scanning on a physical handset.** No Android phone has been
attached to this machine, and the emulator's virtual scene cannot be aimed at a
QR target from the command line. The full CameraX → ML Kit → verify → confirm
path is implemented and its decoding half is covered by `QrRoundTripTest` on
device, but the end-to-end act of pointing a real phone camera at a real screen
has not been performed. This is the one outstanding verification item and it is
listed as such in [LIMITATIONS.md](LIMITATIONS.md).
