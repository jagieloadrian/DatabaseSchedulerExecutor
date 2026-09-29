configPath=$(pwd)/app.properties
pidFile=$(pwd)/scheduler.pid

if [ -f "$pidFile" ] && kill -0 "$(cat "$pidFile")" 2>/dev/null; then
  echo "already running (pid $(cat "$pidFile")), not starting a duplicate"
  exit 1
fi

nohup java -jar DatabaseScheduleExecutor-*.jar --config  $configPath > output.log 2>&1 &
echo $! > "$pidFile"