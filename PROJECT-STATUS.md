# Project Status

## Completed (branch `feature-4/fix-release-build`, 2026-10-27)
- Fat jar fixed: built with Shadow (`mergeServiceFiles()`); the old zipTree jar did not start
  (mssql-jdbc signature files) and registered only 1 of 6 JDBC drivers.
- CI: smoke test starts the release jar and checks 6 drivers register; actions v4,
  setup-java gradle cache, release skips existing tags and waits for the Windows job.
- Scheduler runs on CRON ticks only (no run at startup/restart, no drift by execution time).
- SQL validation: exactly one SELECT/INSERT/UPDATE/DELETE (parsed type allowlist) instead of
  regex blacklist.
- Executor: single `Statement.execute` path, auto-commit, SQL errors logged as failed runs.
- Config: `.properties`-only config path, SQLite file any extension, network settings
  validated once in `buildConnectionProvider`, blank values = missing.
- Logs have timestamps (`simplelogger.properties`); scripts quoted, systemd waits for
  `network-online.target`; dependabot weekly + grouped.
- Version 1.1.0.

## Decisions
- Config file is trusted input: statement check is a scope guard, not injection filtering.
- Shadow plugin kept over `application` + `distZip` to preserve the single-jar install flow.
- Coroutines/Flow loop kept (virtual-time test in `SchedulerFlowServiceKtTest`).

## Next steps
- Merge to main to publish v1.1.0 (old v1.0.1 release jar is broken).
- Optional: tag Testcontainers E2E tests (`@Tag("e2e")`) and run them in a separate CI job.
- Optional: dedicated service user instead of root/SYSTEM in install scripts.
