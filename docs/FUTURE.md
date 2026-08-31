# Future enhancements

Ordered by what would most improve the product, not by what would be most
interesting to build.

## 1. Administrative management

The largest functional gap. Administrators can read everything and change
nothing.

- Roster management: enrol, transfer and withdraw students.
- Timetable editing: slots, rooms, teacher assignment.
- Account lifecycle: create, disable, reset a password.
- Bulk import from CSV, which is how an institution actually onboards a cohort.

Everything needed is already in the schema. This is interface and endpoint work,
not modelling work.

## 2. Attendance corrections

`AttendanceCorrection` is modelled and unused. A teacher who confirms the wrong
student currently has no remedy.

The design should keep the original record and the correction both visible — an
attendance record that can be silently overwritten is worth much less in a
dispute than one that carries its own amendment history.

## 3. Parent access

The role is reserved. A parent view would be read-only: their child's
attendance, standing, and a notification when it falls below threshold.

The privacy questions need answering before the code does: which parent sees
which student, who authorises the link, and what happens when a student turns 18.

## 4. Push notifications

Device registration already exists and reports honestly that delivery is not
configured. Completing it means an FCM project, `google-services.json` supplied
through the build environment rather than the repository, and a delivery worker.

The notifications worth pushing are narrow: attendance dropping below threshold,
and a session opening for a class the student is in. Anything more and people
turn them off.

## 5. Real risk modelling

The `RISK_SCORER` seam exists precisely for this. A Python service consuming the
same `RiskSignals` would be a new binding and nothing else.

Worth doing only with a term or more of real history, and worth doing carefully:
a model that predicts which students will fail is a model that can be wrong about
a particular student in a way that affects them. The current rules are
explainable, which is a real advantage for a decision like this. Any replacement
should be at least as explainable, not merely more accurate.

## 6. Offline attendance capture

Currently a write must reach the server. A lecture hall with no signal is a real
scenario.

Doing this properly is harder than it looks: a queued mark must not be
indistinguishable from a completed one, the teacher must be able to see what is
pending, and reconciliation must handle a session closed while marks were still
queued. The current honest failure is better than a careless queue.

## 7. Institutional integrations

- Single sign-on against the institution's identity provider, which removes the
  password problem entirely.
- Export to whatever student information system the institution already runs.
  Attendance that has to be retyped into another system will be.

## 8. Security hardening

- Multi-factor authentication for staff accounts.
- Certificate pinning.
- Shared-store rate limiting so limits hold across instances.
- A deliberate audit retention and archival policy.

## 9. Scale work

Not needed for one department; needed for a campus.

- Pagination on every list endpoint. The audit trail is capped at 200 per
  request, which is a bound rather than pagination.
- Cursor-based history rather than a fixed `take`.
- Read replicas for analytics, which are the heaviest and least
  latency-sensitive queries.

## 10. Accessibility beyond the current pass

Live regions announce scan outcomes, touch targets meet the minimum, and the
interface holds at 1.5× font scale on a compact screen. Not yet done:

- A full TalkBack pass over every screen with a screen-reader user.
- Verified contrast ratios across the whole palette rather than the primary
  surfaces.
- Tablet and landscape layouts, which currently work but are not designed for.
