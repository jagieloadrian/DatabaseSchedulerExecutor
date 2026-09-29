#!/usr/bin/env bats
# Guard-clause regression tests for scripts/install-service.sh.
# No real `enable --now` here (needs root) — see .specs/stage-5-service-install.md
# for the systemd-analyze verify + manual root-install verification already done.

SCRIPT_SRC_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/.." && pwd)"

setup() {
  TEST_DIR="$(mktemp -d)"
  cp "$SCRIPT_SRC_DIR/install-service.sh" "$TEST_DIR/"
}

teardown() {
  rm -rf "$TEST_DIR"
}

@test "install-service.sh fails with no jar present" {
  cd "$TEST_DIR"
  touch app.properties
  run sh install-service.sh
  [ "$status" -eq 1 ]
  [[ "$output" == *"no DatabaseScheduleExecutor-*.jar found"* ]]
}

@test "install-service.sh fails with no app.properties present" {
  cd "$TEST_DIR"
  touch DatabaseScheduleExecutor-1.0.1.jar
  run sh install-service.sh
  [ "$status" -eq 1 ]
  [[ "$output" == *"app.properties not found"* ]]
}

@test "install-service.sh refuses to run without root" {
  cd "$TEST_DIR"
  touch DatabaseScheduleExecutor-1.0.1.jar app.properties
  run sh install-service.sh
  [ "$status" -eq 1 ]
  [[ "$output" == *"run as root"* ]]
}
