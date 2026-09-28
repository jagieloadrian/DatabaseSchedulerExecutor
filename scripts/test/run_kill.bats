#!/usr/bin/env bats
# Regression tests for scripts/runScheduler.sh + killScheduler.sh.
# Fakes `java` (a sleeping process) so these stay fast/deterministic and
# don't need a real JVM/DB — the real jar was already E2E-tested manually
# for Stage 2/3 (see .specs/stage-2-version-drift.md, stage-3-process-naming.md).

SCRIPT_SRC_DIR="$(cd "$(dirname "$BATS_TEST_FILENAME")/.." && pwd)"

setup() {
  TEST_DIR="$(mktemp -d)"
  cp "$SCRIPT_SRC_DIR/runScheduler.sh" "$SCRIPT_SRC_DIR/killScheduler.sh" "$TEST_DIR/"
  touch "$TEST_DIR/DatabaseScheduleExecutor-1.0.1.jar"
  cat > "$TEST_DIR/app.properties" <<EOF
filepath=:memory:
statement=select 1;
cron=* * * * *
EOF

  # fake java: just sleeps, so runScheduler.sh has a real backgroundable process
  cat > "$TEST_DIR/java" <<'EOF'
#!/bin/sh
sleep 60
EOF
  chmod +x "$TEST_DIR/java"
  export PATH="$TEST_DIR:$PATH"
}

teardown() {
  if [ -f "$TEST_DIR/scheduler.pid" ]; then
    kill "$(cat "$TEST_DIR/scheduler.pid")" 2>/dev/null || true
  fi
  rm -rf "$TEST_DIR"
}

@test "runScheduler.sh resolves jar by glob and writes scheduler.pid" {
  cd "$TEST_DIR"
  run sh runScheduler.sh
  [ "$status" -eq 0 ]
  [ -f "$TEST_DIR/scheduler.pid" ]
  pid=$(cat "$TEST_DIR/scheduler.pid")
  kill -0 "$pid"
}

@test "runScheduler.sh refuses to start a duplicate in the same folder" {
  cd "$TEST_DIR"
  sh runScheduler.sh
  run sh runScheduler.sh
  [ "$status" -eq 1 ]
  [[ "$output" == *"already running"* ]]
}

@test "killScheduler.sh kills the tracked process and removes the pidfile" {
  cd "$TEST_DIR"
  sh runScheduler.sh
  pid=$(cat "$TEST_DIR/scheduler.pid")
  run sh killScheduler.sh
  [ "$status" -eq 0 ]
  [ ! -f "$TEST_DIR/scheduler.pid" ]
  ! kill -0 "$pid" 2>/dev/null
}

@test "killScheduler.sh cleans up a stale pidfile without erroring" {
  cd "$TEST_DIR"
  sh runScheduler.sh
  pid=$(cat "$TEST_DIR/scheduler.pid")
  kill -9 "$pid"
  sleep 0.2
  run sh killScheduler.sh
  [ "$status" -eq 0 ]
  [[ "$output" == *"stale pidfile"* ]]
  [ ! -f "$TEST_DIR/scheduler.pid" ]
}

@test "killScheduler.sh with no pidfile does nothing and exits 0" {
  cd "$TEST_DIR"
  run sh killScheduler.sh
  [ "$status" -eq 0 ]
  [[ "$output" == *"nothing to stop"* ]]
}
