package com.anjo.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

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
    }
}