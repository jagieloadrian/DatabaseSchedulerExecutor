![](src%2Fmain%2Fresources%2Ficon.png)
# DatabaseSchedulerExecutor

[![Kotlin Gradle CI/CD](https://github.com/jagieloadrian/DtatabaseSchedulerExecutor/actions/workflows/build.yaml/badge.svg)](https://github.com/jagieloadrian/DtatabaseSchedulerExecutor/actions/workflows/build.yaml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Kotlin's application to schedule and run background task to execute SQL statement in file `app.properties`.  

## Table of Contents
* [General Info](#general-information)
* [Technologies Used](#technologies-used)
* [Features](#features)
* [Prerequisites](#prerequisites)
* [Setup](#setup)
* [Example configuration](#example-configuration)
* [Stop program](#stop-program)
* [Usage](#usage)
* [Logging](#logging)
* [Download](#download)
* [Room for Improvement](#room-for-improvement)
* [Project Status](#project-status)
* [Acknowledgements](#acknowledgements)
* [License](#license)
* [Contact](#contact)

## General Information

It's application which execute SQL statement on specific time defined by CRON expression. 

## Technologies Used
- Kotlin - version 2.0.21
- Kotlin Coroutines
- SQLite JDBC
- JSQLParser
- Cron-Utils

## Features
List the ready features here:
- run SQL statement on specific time
- no need to rebuild app, just restart to load properties with new information
- in case of limits by cron-utils it has to be 5 chars CRON expression with spaces (like in `app.properties` file, cause `CronType.Unix`). See [cron-utils docs](http://cron-parser.com/)
- program accept one SQL statement due to avoid SQL injection

## Prerequisites
- Java 17

## Setup
1. IDE:
- newest version Intellij/Android Studio (it can be build-in in IDE) to run locally.
- set in args `--config pathToConfig.properties`
- run `Main.kt`
2. Build manual:
- `gradle clean build :jar` to build fatJar, 
- copy properties from `src/main/resources/app.properties` to `build/libs` and adjust to your needs
- run command `java -jar DatabaseScheduleExecutor-*.jar --config $pathToConfig.properties` (jar name includes the build version, `*` picks it up regardless)
3. Build code and run by sh: 
- repeat 2 points from 2 (Build manual).
- copy properties from `src/main/resources/app.properties` to main folder and adjust to your needs
- copy jar from `build/libs` to main folder
- run command `sh runScheduler.sh`

## Example configuration
Example `app.properties`:
```properties
filepath=src/test/resources/testDb.db
statement=select * from people;
cron=* * * * *
```

## Stop program
To stop program (especially when you run by `runScheduler.sh`) run command `sh killScheduler.sh`
Each instance tracks its own PID in `scheduler.pid` in the run folder, so running multiple
instances from separate folders is safe — `killScheduler.sh` only stops the instance in the
folder it's run from. `runScheduler.sh` refuses to start a duplicate if one is already running
in the same folder.

## Usage
- Download zip from link below and unzip in destiny folder
- adjust app.properties to your needs
- run command `sh runScheduler.sh`

## Logging
Logging goes through `slf4j-simple`, written to stdout/stderr by default.
When started via `runScheduler.sh`, output is redirected to `output.log` in the
run folder (`> output.log 2>&1`).

## Download
- Link: [DBSchedulerExecutor.zip](https://github.com/jagieloadrian/DatabaseSchedulerExecutor/releases/latest)

## Room for Improvement
- Add specific name for process ID
- Handle more databases (Oracle, MySql, Postgres) 

## Project Status
Project is: finished

## Acknowledgements
- Not tested on MacOs,
- Tested only on Ubuntu 24, Debian 12

## License
This project is licensed under the [MIT License](LICENSE).

## Contact
Created by [@jagieloadrian](https://github.com/jagieloadrian) - feel free to contact me!
