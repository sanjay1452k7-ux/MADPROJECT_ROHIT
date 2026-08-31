#!/usr/bin/env bash
# Milestone 3 verification: analytics, risk scoring, notifications, reports.
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

atleast() { # atleast <label> <minimum> <actual>
  if [ "$3" -ge "$2" ] 2>/dev/null; then
    echo "  PASS  $1 (got $3, wanted >= $2)"; PASS=$((PASS+1))
  else
    echo "  FAIL  $1 (got $3, wanted >= $2)"; FAIL=$((FAIL+1))
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
TT=$(login meenakshi.s@citchennai.net TEACHER | jqp "d['accessToken']")
AT=$(login academics.office@citchennai.net ADMIN | jqp "d['accessToken']")
RT=$(login 24cs0863@citchennai.net STUDENT | jqp "d['accessToken']")
require_tokens ST TT AT RT

echo "=== 1. Analytics scoping (Section 34) ==="
check "student cannot read teacher analytics" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $ST" "$API/analytics/teacher")"
check "student cannot read admin analytics" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $ST" "$API/analytics/admin")"
check "teacher cannot read admin analytics" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TT" "$API/analytics/admin")"

echo "=== 2. Student analytics ==="
SA=$(curl -s -H "Authorization: Bearer $ST" "$API/analytics/student")
atleast "monthly trend points" 1 "$(echo "$SA" | jqp "len(d['monthlyTrend'])")"
check "subject breakdown covers six subjects" "6" "$(echo "$SA" | jqp "len(d['subjectBreakdown'])")"
atleast "calendar days" 20 "$(echo "$SA" | jqp "len(d['calendar'])")"
check "totals reconcile with held" "True" \
  "$(echo "$SA" | jqp "d['totals']['present']+d['totals']['late']+d['totals']['absent']==d['totals']['held']")"
check "overall matches attended over held" "True" \
  "$(echo "$SA" | jqp "abs(d['overallPercentage'] - round((d['totals']['present']+d['totals']['late'])*1000/d['totals']['held'])/10) < 0.11")"

echo "=== 3. Risk scoring is honest (Section 31) ==="
check "method names the technique" "Rule-based projection from recent attendance rate" \
  "$(echo "$SA" | jqp "d['risk']['method']")"
check "method makes no AI claim" "True" \
  "$(echo "$SA" | jqp "'ai' not in d['risk']['method'].lower() and 'machine learning' not in d['risk']['method'].lower()")"
check "risk band is one of the three" "True" \
  "$(echo "$SA" | jqp "d['risk']['band'] in ('LOW','MODERATE','HIGH')")"
check "risk reason is populated" "True" "$(echo "$SA" | jqp "len(d['risk']['reason']) > 10")"

echo "=== 4. Teacher analytics agree with themselves ==="
TA=$(curl -s -H "Authorization: Bearer $TT" "$API/analytics/teacher")
check "at-risk students are all below threshold" "True" \
  "$(echo "$TA" | jqp "all(s['percentage'] < d['threshold'] for s in d['studentsAtRisk'])")"
check "at-risk list is worst first" "True" \
  "$(echo "$TA" | jqp "[s['percentage'] for s in d['studentsAtRisk']] == sorted(s['percentage'] for s in d['studentsAtRisk'])")"
check "each reason quotes the overall figure it is shown beside" "True" \
  "$(echo "$TA" | jqp "all(str(s['percentage']) in s['reason'] or 'sessions were missed' in s['reason'] for s in d['studentsAtRisk'])")"
atleast "absentee trend days" 1 "$(echo "$TA" | jqp "len(d['absenteeTrend'])")"

echo "=== 5. Admin analytics ==="
AA=$(curl -s -H "Authorization: Bearer $AT" "$API/analytics/admin")
atleast "departments reported" 1 "$(echo "$AA" | jqp "len(d['departmentStats'])")"
check "class sections nested under department" "True" \
  "$(echo "$AA" | jqp "len(d['departmentStats'][0]['classSections']) >= 1")"
check "below-threshold count is within the cohort" "True" \
  "$(echo "$AA" | jqp "0 <= d['studentsBelowThreshold'] <= d['totalStudents']")"

echo "=== 6. Absence recording on session close (Section 32) ==="
TDASH=$(curl -s -H "Authorization: Bearer $TT" "$API/teachers/me/dashboard")
SUB=$(echo "$TDASH" | jqp "[s for s in d['teacher']['subjects'] if s['code']=='CS8505'][0]['id']")
CLS=$(echo "$TDASH" | jqp "d['classSections'][0]['id']")

BEFORE=$(curl -s -H "Authorization: Bearer $RT" "$API/notifications" | jqp "len([n for n in d if n['type']=='ABSENCE_RECORDED'])")

SESSION=$(curl -s -X POST "$API/attendance-sessions" -H "Authorization: Bearer $TT" \
  -H 'Content-Type: application/json' -d "{\"subjectId\":\"$SUB\",\"classSectionId\":\"$CLS\"}")
SID=$(echo "$SESSION" | jqp "d['id']")
check "session opened for the absence test" "ACTIVE" "$(echo "$SESSION" | jqp "d['status']")"

# Mark exactly one student present, then close: everyone else must become ABSENT.
QR=$(curl -s -X POST "$API/attendance-sessions/$SID/token" -H "Authorization: Bearer $ST" | jqp "d['token']")
VID=$(curl -s -X POST "$API/attendance-sessions/$SID/verify" -H "Authorization: Bearer $TT" \
  -H 'Content-Type: application/json' -d "{\"token\":\"$QR\"}" | jqp "d['verificationId']")
curl -s -X POST "$API/attendance-sessions/$SID/mark" -H "Authorization: Bearer $TT" \
  -H 'Content-Type: application/json' -d "{\"verificationId\":\"$VID\"}" > /dev/null

CLOSED=$(curl -s -X POST "$API/attendance-sessions/$SID/close" -H "Authorization: Bearer $TT")
check "session closed" "CLOSED" "$(echo "$CLOSED" | jqp "d['status']")"
check "every enrolled student is now accounted for" "True" \
  "$(echo "$CLOSED" | jqp "all(s['status'] is not None for s in d['students'])")"
check "exactly one present" "1" "$(echo "$CLOSED" | jqp "d['presentCount']")"

AFTER=$(curl -s -H "Authorization: Bearer $RT" "$API/notifications" | jqp "len([n for n in d if n['type']=='ABSENCE_RECORDED'])")
check "an absence notification was raised" "True" "$(python -c "print($AFTER > $BEFORE)")"

echo "=== 7. Notifications ==="
check "unread count endpoint answers" "True" \
  "$(curl -s -H "Authorization: Bearer $RT" "$API/notifications/unread-count" | jqp "isinstance(d['count'], int)")"
curl -s -X PATCH "$API/notifications/read-all" -H "Authorization: Bearer $RT" > /dev/null
check "read-all clears the unread count" "0" \
  "$(curl -s -H "Authorization: Bearer $RT" "$API/notifications/unread-count" | jqp "d['count']")"
check "device registration reports push status honestly" "False" \
  "$(curl -s -X POST "$API/notifications/device-token" -H "Authorization: Bearer $RT" \
     -H 'Content-Type: application/json' -d '{"token":"test-device-token","platform":"ANDROID"}' | jqp "d['pushConfigured']")"

echo "=== 8. Reports (Section 33) ==="
for T in CLASS_ATTENDANCE SUBJECT_ATTENDANCE LOW_ATTENDANCE ABSENTEE; do
  for F in PDF XLSX; do
    CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/reports" -H "Authorization: Bearer $TT" \
      -H 'Content-Type: application/json' -d "{\"type\":\"$T\",\"format\":\"$F\"}")
    check "$T/$F generates" "200" "$CODE"
  done
done
check "student cannot generate a class report" "403" \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/reports" -H "Authorization: Bearer $ST" \
     -H 'Content-Type: application/json' -d '{"type":"CLASS_ATTENDANCE","format":"PDF"}')"

echo
echo "=================================="
echo "  passed: $PASS   failed: $FAIL"
echo "=================================="
[ "$FAIL" -eq 0 ]
