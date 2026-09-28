# Roadmap

## Stage 1 — Docs fix ✅ done (see .specs/stage-1-docs-fix.md)
- Add CI badge to README (workflow already exists: `build.yaml`)
- Add LICENSE file + link in README
- Fix broken link: `[cron-utils docs]()` in Features section
- Fix Contact section: GitLab link → GitHub (repo hosted on GitHub)
- Collapse repeated "install Java 17" steps into one Prerequisites block
- Add example `app.properties` snippet inline
- Add short logging section (slf4j-simple output location/level)

## Stage 2 — Correctness/consistency ✅ done (see .specs/stage-2-version-drift.md)
- Jar filename in README hardcoded `-1.0.jar`, CI auto-bumps version from `gradle.properties` → drift risk.
  Either template the README command with `$VERSION` or note "check latest release name".

## Stage 3 — Feature: process naming
- Add specific process name/PID labeling.
  Needed for `killScheduler.sh` safety when multiple instances run.

## Stage 4 — Feature: multi-DB support
- Extend `DatabaseConnectionProvider`/`DbExecutor` beyond SQLite → Oracle, MySQL, PostgreSQL.
  JDBC abstraction already present, mainly driver dependency + connection string per DB type.

## Stage 5 — Easier launch (replace runScheduler.sh/killScheduler.sh)
- Current scripts use `nohup` + `pgrep -f` — breaks with multiple instances/configs, no auto-restart, no boot start.
  Also `runScheduler.sh` hardcodes jar version `-1.0.jar` while actual build is `-1.0.1.jar` (see Stage 2 drift).
- Linux: `install-service.sh` — installs as systemd unit (`systemctl enable --now db-scheduler`),
  gives auto-restart on failure, start on boot, logs via `journalctl -u db-scheduler`.
- Windows: `install-service.ps1` — registers scheduled task (`schtasks /Create ... /SC ONSTART /RU SYSTEM`),
  runs at boot without login, no extra runtime dependency (no NSSM).
- Both scripts should locate the jar by glob (`DatabaseScheduleExecutor-*.jar`) instead of hardcoding version,
  to avoid the Stage 2 drift issue.
- ponytail: default install runs as root/SYSTEM (no dedicated service account) — add one later if hardening needed.
- Uninstall: no separate script needed, one command each
  (`systemctl disable --now db-scheduler && rm /etc/systemd/system/db-scheduler.service` /
  `schtasks /Delete /TN DatabaseSchedulerExecutor /F`).
