package com.anjo.service

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JDuration

private val logger = KotlinLogging.logger {}

fun calculateNextExecutionDuration(cronExpr: String): Duration {
    val execTime = calculateExecTime(cronExpr)
    val now = ZonedDateTime.now()

    val nextExec = execTime.nextExecution(ZonedDateTime.now()).orElseThrow {
        IllegalStateException("Cannot calculate next execution time")
    }

    logger.info { "Next execution time: $nextExec" }

    return JDuration.between(now, nextExec).toKotlinDuration()
}

private fun calculateExecTime(cronExpr: String): ExecutionTime {
    val cronDef = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    val parser = CronParser(cronDef)
    val cron = parser.parse(cronExpr)
    return ExecutionTime.forCron(cron)
}