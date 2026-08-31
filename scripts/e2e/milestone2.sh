#!/usr/bin/env bash
# End-to-end verification of the Milestone 2 attendance workflow.
# Exercises the happy path plus every failure case the milestone lists.
set -u
API=http://127.0.0.1:3000
PASS=0
FAIL=0

jqp() { python -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }

check() { # check <label> <expected> <actual>
  if [ "$2" = "$3" ]; then
    echo "  PASS  $1 (got $3)"
    PASS=$((PASS+1))
  else
    echo "  FAIL  $1 (expected $2, got $3)"
    FAIL=$((FAIL+1))
  fi
}

login() { # retries when the server rate-limits sign-in, which is expected behaviour
  local attempt=0 body
  while [ $attempt -lt 8 ]; do
    body=$(curl -s --max-time 20 -X POST "$API/auth/login" -H "Content-Type: application/json"       -d "{\"email\":\"$1\",\"password\":\"Cit@2026\",\"role\":\"$2\"}")
    case "$body" in
      *accessToken*) echo "$body"; return 0 ;;
      *RATE_LIMITED*) attempt=$((attempt+1)); sleep 10 ;;
      *) echo "$body"; return 0 ;;
    esac
  done
  echo "$body"
}

code_of() { echo "$1" | jqp "d.get('code','')"; }

login_raw() { # posts an arbitrary sign-in body, backing off when rate-limited
  local attempt=0 body
  while [ $attempt -lt 8 ]; do
    body=$(curl -s --max-time 20 -X POST "$API/auth/login" -H 'Content-Type: application/json' -d "$1")
    case "$body" in
      *RATE_LIMITED*) attempt=$((attempt+1)); sleep 10 ;;
      *) echo "$body"; return 0 ;;
    esac
  done
  echo "$body"
}

echo "=== 1. Authentication ==="
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
STU=$(login 24cs0845@citchennai.net STUDENT)
require_tokens STU
STU_TOKEN=$(echo "$STU" | jqp "d['accessToken']")
check "student signs in" "Sanjayan A" "$(echo "$STU" | jqp "d['user']['displayName']")"

TEA=$(login meenakshi.s@citchennai.net TEACHER)
TEA_TOKEN=$(echo "$TEA" | jqp "d['accessToken']")
check "teacher signs in" "TEACHER" "$(echo "$TEA" | jqp "d['user']['role']")"

ADM=$(login academics.office@citchennai.net ADMIN)
ADM_TOKEN=$(echo "$ADM" | jqp "d['accessToken']")
check "admin signs in" "ADMIN" "$(echo "$ADM" | jqp "d['user']['role']")"

check "wrong password rejected" "INVALID_CREDENTIALS" \
  "$(code_of "$(login_raw '{"email":"24cs0845@citchennai.net","password":"nope","role":"STUDENT"}')")"

check "unknown account rejected identically" "INVALID_CREDENTIALS" \
  "$(code_of "$(login_raw '{"email":"ghost@citchennai.net","password":"Cit@2026","role":"STUDENT"}')")"

check "role mismatch rejected" "ROLE_MISMATCH" \
  "$(code_of "$(login_raw '{"email":"24cs0845@citchennai.net","password":"Cit@2026","role":"TEACHER"}')")"

echo "=== 2. Authorisation (RBAC) ==="
check "student cannot read teacher dashboard" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $STU_TOKEN" "$API/teachers/me/dashboard")"
check "student cannot read admin dashboard" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $STU_TOKEN" "$API/admin/dashboard")"
check "teacher cannot read admin dashboard" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TEA_TOKEN" "$API/admin/dashboard")"
check "no token is rejected" "401" \
  "$(curl -s -o /dev/null -w '%{http_code}' "$API/students/me/dashboard")"
check "garbage token is rejected" "401" \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer not-a-token" "$API/students/me/dashboard")"

echo "=== 3. Student dashboard reads from PostgreSQL ==="
DASH=$(curl -s -H "Authorization: Bearer $STU_TOKEN" "$API/students/me/dashboard")
# Checked against the analytics endpoint rather than a frozen number: the two
# must agree, and recording more attendance must not break the test.
ANALYTICS_PCT=$(curl -s -H "Authorization: Bearer $STU_TOKEN" "$API/analytics/student" | jqp "d['overallPercentage']")
check "dashboard and analytics report the same percentage" "$ANALYTICS_PCT"   "$(echo "$DASH" | jqp "d['summary']['overallPercentage']")"
check "standing" "GOOD" "$(echo "$DASH" | jqp "d['summary']['standing']")"
check "register number" "24CS0845" "$(echo "$DASH" | jqp "d['student']['registerNumber']")"

echo "=== 4. Teacher starts a session ==="
TDASH=$(curl -s -H "Authorization: Bearer $TEA_TOKEN" "$API/teachers/me/dashboard")
# Taken from the teacher's assigned subjects rather than today's timetable, so
# the test does not depend on which day of the week it runs.
SUBJECT_ID=$(echo "$TDASH" | jqp "[s for s in d['teacher']['subjects'] if s['code']=='CS8501'][0]['id']")
CLASS_ID=$(echo "$TDASH" | jqp "d['classSections'][0]['id']")

check "student cannot start a session" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/attendance-sessions" \
     -H "Authorization: Bearer $STU_TOKEN" -H 'Content-Type: application/json' \
     -d "{\"subjectId\":\"$SUBJECT_ID\",\"classSectionId\":\"$CLASS_ID\"}")"

SESSION=$(curl -s -X POST "$API/attendance-sessions" -H "Authorization: Bearer $TEA_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"subjectId\":\"$SUBJECT_ID\",\"classSectionId\":\"$CLASS_ID\"}")
SESSION_ID=$(echo "$SESSION" | jqp "d['id']")
check "session is active" "ACTIVE" "$(echo "$SESSION" | jqp "d['status']")"
check "roster loaded" "31" "$(echo "$SESSION" | jqp "d['totalStudents']")"

echo "=== 5. Student sees the active session ==="
ACTIVE=$(curl -s -H "Authorization: Bearer $STU_TOKEN" "$API/students/me/active-session")
check "active session visible to student" "Mobile Application Development" \
  "$(echo "$ACTIVE" | jqp "d['subjectName']")"

echo "=== 6. QR mint -> verify -> mark (the core workflow) ==="
MINT=$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/token" -H "Authorization: Bearer $STU_TOKEN")
QR=$(echo "$MINT" | jqp "d['token']")
check "token minted server-side" "25" "$(echo "$MINT" | jqp "d['validForSeconds']")"

check "teacher cannot mint a student token" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/attendance-sessions/$SESSION_ID/token" \
     -H "Authorization: Bearer $TEA_TOKEN")"

VERIFY=$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/verify" -H "Authorization: Bearer $TEA_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"token\":\"$QR\"}")
VERIFICATION_ID=$(echo "$VERIFY" | jqp "d['verificationId']")
check "verify returns the student" "Sanjayan A" "$(echo "$VERIFY" | jqp "d['student']['name']")"
check "verify returns register number" "24CS0845" "$(echo "$VERIFY" | jqp "d['student']['registerNumber']")"
check "verify returns department" "CSE" "$(echo "$VERIFY" | jqp "d['student']['department']")"

# Attendance must NOT exist yet: the teacher has not confirmed.
STILL=$(curl -s -H "Authorization: Bearer $TEA_TOKEN" "$API/attendance-sessions/$SESSION_ID")
check "nothing recorded before confirmation" "0" "$(echo "$STILL" | jqp "d['presentCount']")"

MARK=$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/mark" -H "Authorization: Bearer $TEA_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"verificationId\":\"$VERIFICATION_ID\"}")
check "attendance recorded after confirmation" "Sanjayan A" "$(echo "$MARK" | jqp "d['studentName']")"

AFTER=$(curl -s -H "Authorization: Bearer $TEA_TOKEN" "$API/attendance-sessions/$SESSION_ID")
check "present count incremented" "1" "$(echo "$AFTER" | jqp "d['presentCount']")"

echo "=== 7. Failure cases ==="
check "reusing a scanned QR is refused" "QR_ALREADY_USED" \
  "$(code_of "$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/verify" -H "Authorization: Bearer $TEA_TOKEN" \
     -H 'Content-Type: application/json' -d "{\"token\":\"$QR\"}")")"

check "an invented QR is refused" "QR_INVALID" \
  "$(code_of "$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/verify" -H "Authorization: Bearer $TEA_TOKEN" \
     -H 'Content-Type: application/json' -d '{"token":"totally-made-up-token"}')")"

check "duplicate attendance is refused at mint" "ALREADY_MARKED" \
  "$(code_of "$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/token" -H "Authorization: Bearer $STU_TOKEN")")"

# The duplicate guard fires before the ticket check, which is the more useful
# answer: the teacher learns the student is already marked, not that a token is bad.
check "confirming twice is refused" "ALREADY_MARKED" \
  "$(code_of "$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/mark" -H "Authorization: Bearer $TEA_TOKEN" \
     -H 'Content-Type: application/json' -d "{\"verificationId\":\"$VERIFICATION_ID\"}")")"

echo "--- expiry (waits for the QR window to elapse) ---"
ROHIT=$(login 24cs0812@citchennai.net STUDENT)
ROHIT_TOKEN=$(echo "$ROHIT" | jqp "d['accessToken']")
RQR=$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/token" -H "Authorization: Bearer $ROHIT_TOKEN" | jqp "d['token']")
sleep 27
check "an expired QR is refused" "QR_EXPIRED" \
  "$(code_of "$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/verify" -H "Authorization: Bearer $TEA_TOKEN" \
     -H 'Content-Type: application/json' -d "{\"token\":\"$RQR\"}")")"

echo "=== 8. Manual fallback ==="
YAMINI_ID=$(echo "$SESSION" | jqp "[s for s in d['students'] if s['registerNumber']=='24CS0863'][0]['id']")
MANUAL=$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/manual" -H "Authorization: Bearer $TEA_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"studentId\":\"$YAMINI_ID\",\"status\":\"PRESENT\",\"reason\":\"Phone battery flat\"}")
check "manual entry recorded" "True" "$(echo "$MANUAL" | jqp "'recordId' in d")"

echo "=== 9. Session closure ==="
CLOSED=$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/close" -H "Authorization: Bearer $TEA_TOKEN")
check "session closes" "CLOSED" "$(echo "$CLOSED" | jqp "d['status']")"

check "minting against a closed session is refused" "SESSION_CLOSED" \
  "$(code_of "$(curl -s -X POST "$API/attendance-sessions/$SESSION_ID/token" -H "Authorization: Bearer $ROHIT_TOKEN")")"

# Asserts the closed session specifically is gone, rather than that no session
# exists at all: a student may legitimately have another subject open.
check "the closed session is no longer offered to the student" "True"   "$(curl -s -H "Authorization: Bearer $ROHIT_TOKEN" "$API/students/me/active-session" | python -c "
import sys, json
body = sys.stdin.read().strip()
print(True if not body else json.loads(body).get('id') != '$SESSION_ID')
")"

echo
echo "=================================="
echo "  passed: $PASS   failed: $FAIL"
echo "=================================="
[ "$FAIL" -eq 0 ]
