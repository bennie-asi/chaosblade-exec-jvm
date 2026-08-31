#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
MATRIX_DIR="$SCRIPT_DIR/.matrix-target"

fail() { echo "FAIL: $*" >&2; exit 1; }
require() { command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"; }

if [[ ${1:-} == "--self-test" ]]; then
  bash -n "$0"
  grep -q 'java-8-openjdk-amd64' "$0"
  grep -q 'java-11-openjdk-amd64' "$0"
  grep -q 'java-17-openjdk-amd64' "$0"
  echo "PASS: datasource compatibility matrix definition"
  exit 0
fi

for command in mvn curl jq git; do require "$command"; done
test -n "${BLADE_HOME:-}" || fail "BLADE_HOME must point to a built chaosblade distribution"
test -x "$BLADE_HOME/blade" || fail "blade executable not found under BLADE_HOME"
JDK8_HOME=${JDK8_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}
JDK11_HOME=${JDK11_HOME:-/usr/lib/jvm/java-11-openjdk-amd64}
JDK17_HOME=${JDK17_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
for java_home in "$JDK8_HOME" "$JDK11_HOME" "$JDK17_HOME"; do
  test -x "$java_home/bin/java" || fail "JDK not found: $java_home"
done
mkdir -p "$MATRIX_DIR"

mvn -q -f "$SCRIPT_DIR/pom.xml" clean package -Dspring.boot.version=2.7.18
cp "$SCRIPT_DIR/target/datasource-attach-app-1.0.0.jar" "$MATRIX_DIR/app-boot-2.7.18.jar"
mvn -q -f "$SCRIPT_DIR/pom.xml" clean package -Dspring.boot.version=3.2.12 -Djava.version=17
cp "$SCRIPT_DIR/target/datasource-attach-app-1.0.0.jar" "$MATRIX_DIR/app-boot-3.2.12.jar"

CURRENT_PID=""
CURRENT_HOME=""
cleanup() {
  set +e
  if [[ -n $CURRENT_PID ]]; then kill "$CURRENT_PID" >/dev/null 2>&1; wait "$CURRENT_PID" >/dev/null 2>&1; fi
  if [[ -n $CURRENT_HOME ]]; then rm -rf "$CURRENT_HOME"; fi
  rm -rf "$MATRIX_DIR"
}
trap cleanup EXIT

wait_http() {
  local url=$1 expected=$2 deadline=$((SECONDS + 60))
  while ((SECONDS < deadline)); do
    if [[ $(curl -sS -o /dev/null -w '%{http_code}' "$url" 2>/dev/null) == "$expected" ]]; then return 0; fi
    sleep 1
  done
  fail "HTTP $expected was not observed for $url"
}

wait_active() {
  local url=$1 expected=$2 deadline=$((SECONDS + 30)) value
  while ((SECONDS < deadline)); do
    value=$(curl -fsS "$url" 2>/dev/null || true)
    if jq -e --argjson expected "$expected" '.active == $expected' >/dev/null 2>&1 <<<"$value"; then return 0; fi
    sleep 1
  done
  fail "active=$expected was not observed for $url"
}

run_case() {
  local name=$1 java_home=$2 jar=$3 pool=$4 port=$5 response uid base ttl_response ttl_uid
  CURRENT_HOME=$(mktemp -d)
  cp -a "$BLADE_HOME" "$CURRENT_HOME/blade"
  "$java_home/bin/java" -jar "$jar" --server.port="$port" --pool.type="$pool" \
    >"$MATRIX_DIR/$name.log" 2>&1 &
  CURRENT_PID=$!
  base="http://127.0.0.1:$port"
  wait_http "$base/query" 200

  response=$("$CURRENT_HOME/blade/blade" create datasource connectionpoolfull \
    --data-source-name coreDataSource --target-percent 100 --timeout 30 \
    --pid "$CURRENT_PID" --javaHome "$java_home")
  jq -e '.success and .result.detail.state == "ACTIVE" and .result.detail.actualHold == 4' \
    >/dev/null <<<"$response" || fail "$name create result: $response"
  uid=$(jq -er '.result.uid' <<<"$response")
  wait_active "$base/pool" 4
  wait_http "$base/query" 503
  "$CURRENT_HOME/blade/blade" destroy "$uid" >/dev/null
  wait_active "$base/pool" 0
  wait_http "$base/query" 200

  ttl_response=$("$CURRENT_HOME/blade/blade" create datasource connectionpoolfull \
    --data-source-name coreDataSource --target-percent 100 --timeout 3 \
    --pid "$CURRENT_PID" --javaHome "$java_home")
  ttl_uid=$(jq -er '.result.uid' <<<"$ttl_response")
  test -n "$ttl_uid"
  wait_active "$base/pool" 4
  sleep 4
  wait_active "$base/pool" 0
  wait_http "$base/query" 200
  echo "PASS: $name"
  kill "$CURRENT_PID"
  wait "$CURRENT_PID" || true
  rm -rf "$CURRENT_HOME"
  CURRENT_PID=""
  CURRENT_HOME=""
}

run_case jdk8-boot27-hikari "$JDK8_HOME" "$MATRIX_DIR/app-boot-2.7.18.jar" hikari 18081
run_case jdk8-boot27-druid "$JDK8_HOME" "$MATRIX_DIR/app-boot-2.7.18.jar" druid 18082
run_case jdk11-boot27-hikari "$JDK11_HOME" "$MATRIX_DIR/app-boot-2.7.18.jar" hikari 18083
run_case jdk11-boot27-druid "$JDK11_HOME" "$MATRIX_DIR/app-boot-2.7.18.jar" druid 18084
run_case jdk17-boot32-hikari "$JDK17_HOME" "$MATRIX_DIR/app-boot-3.2.12.jar" hikari 18085
run_case jdk17-boot32-druid "$JDK17_HOME" "$MATRIX_DIR/app-boot-3.2.12.jar" druid 18086
echo "PASS: datasource attach compatibility matrix"
