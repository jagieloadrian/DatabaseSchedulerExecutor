configPath=$(pwd)/app.properties
nohup java -jar DatabaseScheduleExecutor-*.jar --config  $configPath > output.log 2>&1 &