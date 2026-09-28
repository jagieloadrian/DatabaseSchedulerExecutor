jar=$(ls DatabaseScheduleExecutor-*.jar 2>/dev/null | head -n1)
pid=$(pgrep -f "$jar")
kill $pid