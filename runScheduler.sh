configPath=$(pwd)/app.properties
nohup java -jar DatabaseScheduleExecutor-1.0.jar --config  $configPath > output.log 2>&1 &