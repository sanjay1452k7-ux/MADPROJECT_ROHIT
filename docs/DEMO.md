# Final demo script

Twenty-one steps, roughly twelve minutes. Each step says what to do, what should
appear, and — where it matters — what it proves.

## Before you start

1. PostgreSQL running.
2. Backend running: `cd backend && node dist/main.js`
3. `adb reverse tcp:3000 tcp:3000` for each attached device.
4. App installed: `./gradlew :app:installDebug`
5. Two devices if possible — one student, one teacher. One device works; you
   sign out and back in between the student and teacher halves.

Password for every account: `Cit@2026`.

---

## Part 1 — Identity and first impressions

**1. Launch the app.**
The butterfly emblem animates on the sign-in screen. Black, deep purple, white.
Point out it is original artwork, not any institution's actual crest.

**2. Role selection.**
Student, Teacher, Admin. Note that Parent is deliberately absent — the role is
reserved in the schema but ships no behaviour.

**3. Sign in as a student** — `24cs0845@citchennai.net`.
Watch the transition into the student shell.

**4. Try the wrong role.** Sign out, choose **Teacher**, and enter the same
student credentials.
Refused: *"This account does not hold the selected role."*
**Proves:** the role is part of the credential check, not a client-side filter.

---

## Part 2 — The student's view

**5. Sign back in as the student.** The dashboard shows overall percentage,
standing, today's classes and recent records.

**6. Subject breakdown.** Six subjects, each with attended/held and a percentage.
Every figure is computed from records — nothing is stored and drifting.

**7. Analytics tab.** Monthly trend, attendance calendar, per-subject comparison.

**8. Risk card.** Read the `method` line aloud:
*"Rule-based projection from recent attendance rate."*
**Proves:** the product does not claim arithmetic is AI. A test asserts this
string makes no machine-learning claim.

---

## Part 3 — The attendance workflow

This is the centre of the demonstration.

**9. On the teacher device, sign in** as `meenakshi.s@citchennai.net`.
Today's classes are listed.

**10. Start a session** for Mobile Application Development.
The roster appears, everyone unmarked.

**11. On the student device, refresh the dashboard.**
The class now shows an active session and a **Generate code** action.

**12. Generate the QR.**
A code appears with a countdown. Say what it is: 32 bytes of *server-side*
randomness, valid 25 seconds, stored only as a SHA-256 hash, carrying no student
data at all.
**Proves:** Section 19 — the client never holds a signing key, so it cannot
forge a code even in principle.

**13. Teacher: open the scanner and scan it.**
The camera closes and the student's identity appears — name, register number,
photo — with **Mark present** and **Reject**.

**14. Stop here and point at the roster.** The student is still unmarked.
**Proves:** Section 17 — a scan records nothing. Verification and marking are
separate calls, and between them sits a human looking at a face. This is what
stops a student scanning a screenshot of a friend's code: the friend is not in
the room.

**15. Press Mark present.**
Now the roster updates and the count increments.

---

## Part 4 — The failure cases

The interesting half. Each of these is in the automated matrix too.

**16. Replay the same code.** Scan the still-displayed QR again.
Refused — the token was consumed at verification.

**17. Wait out a code.** Generate a fresh one, let the countdown reach zero, then
scan.
Refused as expired.

**18. Duplicate.** With the student already marked present, ask their app for
another code.
The student's own app says they are already marked and refuses to render one.
**Proves:** the duplicate is stopped at the earliest possible point — the teacher
is never even offered a confirm button for someone already present.

**19. Close the session.**
Point at the roster: every unmarked student is now recorded `ABSENT`.
**Proves:** without this step an unmarked student is simply *missing* from the
data, and every percentage would silently count only the classes they attended.
Then try to scan a code minted before the close — refused, session closed.

---

## Part 5 — Institutional view

**20. Sign in as the administrator** — `academics.office@citchennai.net`.
Institution-wide figures, department breakdown, students below threshold.
Under **Insights → Reports**, generate a Class Attendance PDF, then the same as
Excel.

**21. Insights → Activity.**
The audit trail: every sign-in, session, mint, scan, rejection and mark, each
naming the account that did it.
**Proves:** Section 35 — accountability is readable, not merely recorded. Point
out that this screen is administrator-only; a teacher or student requesting the
same endpoint gets a 403.

---

## If something goes wrong

**The student's app doesn't see the session.** Pull to refresh. If still nothing,
`adb reverse tcp:3000 tcp:3000` was not run on that device.

**Sign-in returns 429.** The rate limiter (10/minute) is working as designed.
Wait a minute — and say so, it is a feature worth showing.

**The scanner shows a camera error.** Permission was refused. The screen offers
**Open settings**; grant camera access and return. Worth demonstrating
deliberately if there is time.

**Nothing scans.** Use the manual fallback on the roster. It records the student
as `MANUAL` rather than `QR`, so an audit can always tell the two apart — which
is exactly why the fallback is safe to have.
