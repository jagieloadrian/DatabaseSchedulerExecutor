pidFile=$(pwd)/scheduler.pid

if [ ! -f "$pidFile" ]; then
  echo "no scheduler.pid found, nothing to stop"
  exit 0
fi

pid=$(cat "$pidFile")

if kill -0 "$pid" 2>/dev/null; then
  kill "$pid"
else
  echo "process $pid not running (stale pidfile)"
fi

rm -f "$pidFile"