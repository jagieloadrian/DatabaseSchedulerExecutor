![](src%2Fmain%2Fresources%2Ficon.png)
# DatabaseSchedulerExecutor

Kotlin's application to schedule and run background task to execute SQL statement in file `app.properties`.  

## Table of Contents
* [General Info](#general-information)
* [Technologies Used](#technologies-used)
* [Features](#features)
* [Setup](#setup)
* [Stop program](#stop-program)
* [Usage](#usage)
* [Download](#download)
* [Room for Improvement](#room-for-improvement)
* [Project Status](#project-status)
* [Acknowledgements](#acknowledgements)
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
- in case of limits by cron-utils it has to be 5 chars CRON expression with spaces (like in `app.properties` file, cause `CronType.Unix`). See [cron-utils docs]()
- program accept one SQL statement due to avoid SQL injection

## Setup
1. IDE: 
- install Java 17, 
- newest version Intellij/Android Studio (it can be build-in in IDE) to run locally.
- set in args `--config pathToConfig.properties`
- run `Main.kt`
2. Build manual:
- install Java 17,
- `gradle clean build :jar` to build fatJar, 
- copy properties from `src/main/resources/app.properties` to `build/libs` and adjust to your needs
- run command `java -jar DatabaseScheduleExecutor-1.0.jar --config $pathToConfig.properties`
3. Build code and run by sh: 
- repeat 2 points from 2 (Build manual).
- copy properties from `src/main/resources/app.properties` to main folder and adjust to your needs
- copy jar from `build/libs` to main folder
- run command `sh runScheduler.sh`

## Stop program
To stop program (especially when you run by `runScheduler.sh`) run command `sh killScheduler.sh`
Attention, when you run more than one program then kill all processes by name of app. 

## Usage
- Download zip from link below and unzip in destiny folder
- adjust app.properties to your needs
- run command `sh runScheduler.sh`

## Download
- [DBSchedulerExecutor.zip](https://gitlab.com/api/v4/projects/66064754/jobs/artifacts/main/raw/build/libs/TuyaKeyExtractorKt.jar?job=build)

## Room for Improvement
- Add specific name for process ID

## Project Status
Project is: finished

## Acknowledgements
- Not tested on MacOs,
- Tested only on Ubuntu 24, Debian 12

## Contact
Created by [@jagieloadrian](https://gitlab.com/jagieloadrian) - feel free to contact me!

