package com.anjo.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

private val logger = KotlinLogging.logger {}

suspend fun runSchedulerFlow(cronExpr: String, dbExecutor: () -> Unit) {
    var count = 0

    val execFlow = flow {
            while (true) {
                val delay = calculateNextExecutionDuration(cronExpr)
                emit(dbExecutor())
                delay(delay)
            }
    }

    execFlow.collect {
        count++
        logger.info { "Executed $count times" }
    }
}