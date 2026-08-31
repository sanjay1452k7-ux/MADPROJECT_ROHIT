# End-to-end suites

These run against a **live backend and a seeded database**. They sign in as real
seeded accounts and drive the real HTTP API — no mocks, no test doubles, no
in-memory database.

```bash
bash scripts/e2e/milestone2.sh   # 35 checks — the attendance workflow
bash scripts/e2e/milestone3.sh   # 36 checks — analytics, risk, notifications, reports
bash scripts/e2e/milestone4.sh   # 45 checks — failure matrix, API security, audit
```

## Run them one at a time

Sign-in is rate limited to ten attempts per minute. Several suites back to back
will exhaust it, and every subsequent check fails for reasons that have nothing
to do with the code under test.

Each suite aborts with an explicit message if it could not obtain tokens, rather
than reporting dozens of misleading failures. If you see that message, wait a
minute and run the suite on its own.

## What they assume

- The backend is on `http://127.0.0.1:3000`.
- The database has been seeded (`npx prisma db seed`).
- The seeded password is `Cit@2026`.

`milestone4.sh` takes about a minute: it waits out a real 25-second QR lifetime
to prove that expiry is enforced by the clock rather than by a flag.

The cases these suites cover, and what each one proves, are tabulated in
[`docs/TESTING.md`](../../docs/TESTING.md).
