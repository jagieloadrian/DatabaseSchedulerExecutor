#!/bin/bash
# Installs DatabaseSchedulerExecutor as a systemd service.
# Run from the folder containing the built jar + app.properties (as root/sudo).
# Uninstall: systemctl disable --now db-scheduler && rm /etc/systemd/system/db-scheduler.service
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$(ls "$SCRIPT_DIR"/DatabaseScheduleExecutor-*.jar 2>/dev/null | head -n1)"
CONFIG="$SCRIPT_DIR/app.properties"
INSTALL_DIR="${1:-/opt/db-scheduler}"

if [ -z "$JAR" ]; then
  echo "no DatabaseScheduleExecutor-*.jar found in $SCRIPT_DIR" >&2
  exit 1
fi
if [ ! -f "$CONFIG" ]; then
  echo "app.properties not found in $SCRIPT_DIR" >&2
  exit 1
fi
if [ "$(id -u)" -ne 0 ]; then
  echo "run as root (sudo $0)" >&2
  exit 1
fi

mkdir -p "$INSTALL_DIR"
cp "$JAR" "$INSTALL_DIR/"
cp "$CONFIG" "$INSTALL_DIR/"
chmod 600 "$INSTALL_DIR/app.properties"
JAR_NAME="$(basename "$JAR")"

cat > /etc/systemd/system/db-scheduler.service <<EOF
[Unit]
Description=DatabaseSchedulerExecutor
After=network.target

[Service]
ExecStart=/usr/bin/env java -jar $INSTALL_DIR/$JAR_NAME --config $INSTALL_DIR/app.properties
WorkingDirectory=$INSTALL_DIR
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF
# ponytail: runs as root (no dedicated User=), add one if hardening needed

systemctl daemon-reload
systemctl enable --now db-scheduler

echo "installed to $INSTALL_DIR, status: systemctl status db-scheduler"
