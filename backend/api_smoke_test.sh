#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080"
EMAIL="curl_smoketest@example.com"
PASSWORD="password123"
USERNAME="smoketest"
NICKNAME="Smoke Test"

color_ok() { echo -e "\033[32m$1\033[0m"; }
color_err() { echo -e "\033[31m$1\033[0m"; }

check() {
  local code=$1 msg="$2"
  if [[ $code -ge 200 && $code -lt 300 ]]; then
    color_ok "✔ $msg ($code)"
  else
    color_err "✖ $msg ($code)"
    exit 1
  fi
}

# 1. Register (ignore conflict if already exists)
status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/users/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"nickname\":\"$NICKNAME\"}")
[ "$status" = "201" ] || [ "$status" = "409" ] && color_ok "회원가입 (이미 존재 가능) $status" || (color_err "회원가입 실패 $status" && exit 1)

# 2. Login
login_json=$(curl -s -X POST "$BASE_URL/users/login" \
  -H 'Content-Type: application/json' \
  -d "{\"usernameOrEmail\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$login_json" | python3 -c 'import sys, json; print(json.load(sys.stdin)["data"]["access_token"])')
[ -n "$TOKEN" ] && color_ok "로그인 & 토큰 획득" || { color_err "토큰 획득 실패"; exit 1; }

AUTH_HEADER="Authorization: Bearer $TOKEN"

# 3. Create portfolio
code=$(curl -s -o /tmp/port_resp.json -w "%{http_code}" -X POST "$BASE_URL/api/v1/portfolios" \
  -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"name":"SmokePortfolio","description":"smoke","initial_investment":1000000,"is_public":true}')
check $code "포트폴리오 생성"
PORT_ID=$(cat /tmp/port_resp.json | python3 -c 'import sys, json; print(json.load(sys.stdin)["data"]["id"])')

# 4. Add portfolio item (BTC)
code_item=$(curl -s -o /tmp/item_resp.json -w "%{http_code}" -X POST "$BASE_URL/api/v1/portfolios/$PORT_ID/items" \
  -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"coin_symbol":"BTC","quantity":0.1,"average_price":50000000,"notes":"smoke add"}')
check $code_item "아이템 추가"
ITEM_ID=$(python3 -c 'import sys, json, pathlib; print(json.load(open("/tmp/item_resp.json"))["data"]["id"])')

# 5. Update item quantity
code=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/portfolios/$PORT_ID/items/$ITEM_ID" \
  -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"quantity":0.15}')
check $code "아이템 수정"

# 6. List my portfolios
code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH_HEADER" "$BASE_URL/api/v1/portfolios/my")
check $code "내 포트폴리오 목록 조회"

# 7. Delete item
code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/portfolios/$PORT_ID/items/$ITEM_ID" -H "$AUTH_HEADER")
check $code "아이템 삭제"

# 8. Delete portfolio
code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/portfolios/$PORT_ID" -H "$AUTH_HEADER")
check $code "포트폴리오 삭제"

color_ok "✅ Smoke test 성공" 