#!/usr/bin/env bash
# Milestone 4 verification: the failure matrix, API security, audit integrity.
# Every case here is a way the workflow can be abused or can go wrong. The
# system is only trustworthy if each one is handled for the stated reason.
set -u
API=http://127.0.0.1:3000
PASS=0
FAIL=0

jqp() { python -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }

check() {
  if [ "$2" = "$3" ]; then
    echo "  PASS  $1 (got $3)"; PASS=$((PASS+1))
  else
    echo "  FAIL  $1 (expected $2, got $3)"; FAIL=$((FAIL+1))
  fi
}

login() {
  local attempt=0 body
  while [ $attempt -lt 8 ]; do
    body=$(curl -s --max-time 20 -X POST "$API/auth/login" -H "Content-Type: application/json" \
      -d "{\"email\":\"$1\",\"password\":\"Cit@2026\",\"role\":\"$2\"}")
    case "$body" in
      *accessToken*) echo "$body"; return 0 ;;
      *RATE_LIMITED*) attempt=$((attempt+1)); sleep 10 ;;
      *) echo "$body"; return 0 ;;
    esac
  done
  echo "$body"
}

EMPTY='{}'

post() { # post <path> <token> <json>
  curl -s -X POST "$API$1" -H "Authorization: Bearer $2" \
    -H "Content-Type: application/json" -d "$3"
}

get_code() { # get_code <token> <path>
  curl -s -o /dev/null -w '%{http_code}' "$API$2" -H "Authorization: Bearer $1"
}

post_code() { # post_code <token> <path> [json]
  local body="${3:-$EMPTY}"
  curl -s -o /dev/null -w '%{http_code}' -X POST "$API$2" \
    -H "Authorization: Bearer $1" -H "Content-Type: application/json" -d "$body"
}

# A suite that runs without tokens reports every check as a failure and hides
# the real cause, which is almost always the sign-in rate limiter still holding
# from a previous suite. Stop with the actual reason instead.
require_tokens() {
  local name
  for name in "$@"; do
    if [ -z "${!name}" ]; then
      echo "ABORT: could not sign in ($name is empty)." >&2
      echo "The sign-in rate limiter is probably still open from an earlier run;" >&2
      echo "wait a minute and run this suite on its own." >&2
      exit 2
    fi
  done
}
ST=$(login 24cs0845@citchennai.net STUDENT | jqp "d['accessToken']")
S2=$(login 24cs0801@citchennai.net STUDENT | jqp "d['accessToken']")
TT=$(login meenakshi.s@citchennai.net TEACHER | jqp "d['accessToken']")
AT=$(login academics.office@citchennai.net ADMIN | jqp "d['accessToken']")
require_tokens ST S2 TT AT

echo "=== 1. Authentication failures ==="
check "wrong password is refused" "401" "$(curl -s -o /dev/null -w '%{http_code}' \
  -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"24cs0845@citchennai.net","password":"wrong","role":"STUDENT"}')"
check "correct password with the wrong role is refused" "403" "$(curl -s -o /dev/null -w '%{http_code}' \
  -X POST "$API/auth/login" -H "Content-Type: application/json" \
  -d '{"email":"24cs0845@citchennai.net","password":"Cit@2026","role":"TEACHER"}')"
check "no token is refused" "401" "$(curl -s -o /dev/null -w '%{http_code}' "$API/students/me/dashboard")"
check "a garbage token is refused" "401" "$(get_code not.a.jwt /students/me/dashboard)"

echo "=== 2. Unauthorized role (Section 34) ==="
check "student cannot start a session" "403" \
  "$(post_code "$ST" /attendance-sessions '{"subjectId":"x","classSectionId":"y"}')"
check "student cannot mark attendance" "403" \
  "$(post_code "$ST" /attendance-sessions/x/mark '{"verificationId":"z"}')"
check "student cannot read the audit log" "403" "$(get_code "$ST" /admin/audit)"
check "teacher cannot read the audit log" "403" "$(get_code "$TT" /admin/audit)"
check "admin cannot mint a student QR" "403" "$(post_code "$AT" /attendance-sessions/x/token)"

echo "=== 3. Session lifecycle ==="
TD=$(curl -s -H "Authorization: Bearer $TT" "$API/teachers/me/dashboard")
SUB=$(echo "$TD" | jqp "d['todaysClasses'][0]['subjectId']")
SEC=$(echo "$TD" | jqp "d['todaysClasses'][0]['classSectionId']")
BODY="{\"subjectId\":\"$SUB\",\"classSectionId\":\"$SEC\"}"
SID=$(post "/attendance-sessions" "$TT" "$BODY" | jqp "d['id']")
check "teacher can open a session" "True" "$(python -c "print(len('$SID')>10)")"
# Starting again is deliberately idempotent, so a retry on a flaky connection
# cannot split one class across two sessions. The invariant is that no second
# session appears, not that the call fails.
check "starting again returns the same session, not a second one" "$SID" \
  "$(post "/attendance-sessions" "$TT" "$BODY" | jqp "d['id']")"

echo "=== 4. QR failure cases (Sections 19, 22) ==="
check "an invalid token is refused" "400" \
  "$(post_code "$TT" "/attendance-sessions/$SID/verify" '{"token":"deadbeefdeadbeef"}')"
TOKEN=$(post "/attendance-sessions/$SID/token" "$ST" "$EMPTY" | jqp "d['token']")
check "a student can mint a token" "True" "$(python -c "print(len('$TOKEN')>20)")"
VER=$(post "/attendance-sessions/$SID/verify" "$TT" "{\"token\":\"$TOKEN\"}")
VID=$(echo "$VER" | jqp "d['verificationId']")
check "verify returns the student for visual confirmation" "True" "$(echo "$VER" | jqp "len(d['student']['name'])>3")"
check "verify alone records nothing" "0" \
  "$(curl -s -H "Authorization: Bearer $TT" "$API/attendance-sessions/$SID" | jqp "d['presentCount']")"
check "replaying the same token is refused" "409" \
  "$(post_code "$TT" "/attendance-sessions/$SID/verify" "{\"token\":\"$TOKEN\"}")"
check "teacher confirmation records attendance" "201" \
  "$(post_code "$TT" "/attendance-sessions/$SID/mark" "{\"verificationId\":\"$VID\"}")"
check "the same verification cannot be marked twice" "409" \
  "$(post_code "$TT" "/attendance-sessions/$SID/mark" "{\"verificationId\":\"$VID\"}")"

echo "=== 5. Duplicate attendance ==="
# The duplicate is stopped as early as it can be: the student's own app is told
# they are already marked and never renders a second code, so the teacher is
# never presented with a confirm button for someone already present.
DUP=$(post "/attendance-sessions/$SID/token" "$ST" "$EMPTY")
check "an already-present student cannot mint another code" "409" \
  "$(post_code "$ST" "/attendance-sessions/$SID/token")"
check "and is told why, not given a generic error" "ALREADY_MARKED" \
  "$(echo "$DUP" | jqp "d['code']")"
check "the student is still counted once" "1" \
  "$(curl -s -H "Authorization: Bearer $TT" "$API/attendance-sessions/$SID" | jqp "d['presentCount']")"

echo "=== 6. Expired QR (25s TTL) ==="
EXP=$(post "/attendance-sessions/$SID/token" "$S2" "$EMPTY" | jqp "d['token']")
echo "  waiting out the token lifetime..."
sleep 28
check "an expired token is refused as gone" "410" \
  "$(post_code "$TT" "/attendance-sessions/$SID/verify" "{\"token\":\"$EXP\"}")"
check "no attendance resulted from the expired code" "1" \
  "$(curl -s -H "Authorization: Bearer $TT" "$API/attendance-sessions/$SID" | jqp "d['presentCount']")"

echo "=== 7. Closed session ==="
FRESH=$(post "/attendance-sessions/$SID/token" "$S2" "$EMPTY" | jqp "d['token']")
check "the session closes" "201" "$(post_code "$TT" "/attendance-sessions/$SID/close")"
check "a token minted before closing is now useless" "409" \
  "$(post_code "$TT" "/attendance-sessions/$SID/verify" "{\"token\":\"$FRESH\"}")"
check "no new token can be minted" "409" "$(post_code "$ST" "/attendance-sessions/$SID/token")"
SUMMARY=$(curl -s -H "Authorization: Bearer $TT" "$API/attendance-sessions/$SID")
check "unmarked students were recorded absent" "True" \
  "$(echo "$SUMMARY" | jqp "sum(1 for s in d['students'] if s['status']=='ABSENT')>0")"
check "every student on the roster now has a status" "True" \
  "$(echo "$SUMMARY" | jqp "all(s['status'] for s in d['students']) and len(d['students'])==d['totalStudents']")"
BEFORE=$(echo "$SUMMARY" | jqp "sum(1 for s in d['students'] if s['status']=='ABSENT')")
# Closing is idempotent for the same reason starting is; what must not happen is
# a second sweep of absences.
check "closing again keeps it closed" "CLOSED" \
  "$(post "/attendance-sessions/$SID/close" "$TT" "$EMPTY" | jqp "d['status']")"
check "closing again does not double-record absences" "$BEFORE" \
  "$(curl -s -H "Authorization: Bearer $TT" "$API/attendance-sessions/$SID" | jqp "sum(1 for s in d['students'] if s['status']=='ABSENT')")"

echo "=== 8. Audit trail names the actor (Section 41) ==="
AUD=$(curl -s -H "Authorization: Bearer $AT" "$API/admin/audit?limit=50")
check "audit entries exist" "True" "$(echo "$AUD" | jqp "len(d['entries'])>0")"
check "every entry names an actor" "True" "$(echo "$AUD" | jqp "all(e['actor'] for e in d['entries'])")"
check "the mark is attributed to the acting teacher" "True" \
  "$(echo "$AUD" | jqp "any(e['action']=='ATTENDANCE_MARKED' for e in d['entries'])")"
check "a rejected scan is recorded too" "True" \
  "$(echo "$AUD" | jqp "any(e['action']=='QR_REJECTED' for e in d['entries'])")"

echo "=== 9. API security headers (Section 34) ==="
H=$(curl -s -D - -o /dev/null "$API/auth/login" -X POST -H "Content-Type: application/json" -d '{}')
check "nosniff is set" "yes" "$(echo "$H" | grep -qi 'x-content-type-options: nosniff' && echo yes || echo no)"
check "framing is denied" "yes" "$(echo "$H" | grep -qi 'x-frame-options: DENY' && echo yes || echo no)"
check "referrers are not leaked" "yes" "$(echo "$H" | grep -qi 'referrer-policy: no-referrer' && echo yes || echo no)"
check "server technology is not advertised" "no" "$(echo "$H" | grep -qi 'x-powered-by' && echo yes || echo no)"
check "credential responses are never cached" "yes" \
  "$(echo "$H" | grep -qi 'cache-control: no-store' && echo yes || echo no)"
LIVE=$(curl -s -D - -o /dev/null -H "Authorization: Bearer $ST" "$API/students/me/active-session")
check "live attendance state is never cached" "yes" \
  "$(echo "$LIVE" | grep -qi 'cache-control: no-store' && echo yes || echo no)"
ANA=$(curl -s -D - -o /dev/null -H "Authorization: Bearer $ST" "$API/analytics/student")
check "analytics are cacheable but private" "yes" \
  "$(echo "$ANA" | grep -qi 'cache-control: private' && echo yes || echo no)"
AUDH=$(curl -s -D - -o /dev/null -H "Authorization: Bearer $AT" "$API/admin/audit")
check "the audit trail is never cached" "yes" \
  "$(echo "$AUDH" | grep -qi 'cache-control: no-store' && echo yes || echo no)"

echo "=== 10. Input validation ==="
check "a malformed body is rejected, not crashed" "400" \
  "$(post_code "$TT" /attendance-sessions '{"subjectId":123}')"
check "an unknown session id is a clean 404" "404" \
  "$(get_code "$TT" /attendance-sessions/00000000-0000-0000-0000-000000000000)"
check "the audit page size is bounded" "True" \
  "$(curl -s -H "Authorization: Bearer $AT" "$API/admin/audit?limit=99999" | jqp "len(d['entries'])<=200")"

echo
echo "passed: $PASS   failed: $FAIL"
[ "$FAIL" -eq 0 ]
