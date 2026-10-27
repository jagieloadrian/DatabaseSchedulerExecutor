package com.anjo.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

suspend fun runSchedulerFlow(calcDuration: () -> Duration, dbExecutor: suspend () -> Unit) {
    var count = 0

    val execFlow = flow {
        while (true) {
            delay(calcDuration())
            emit(dbExecutor())
        }
    }

    execFlow.collect {
        count++
        logger.info { "Executed $count times" }
    }
}