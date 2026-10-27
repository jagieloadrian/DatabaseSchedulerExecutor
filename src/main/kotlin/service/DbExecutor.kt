package com.anjo.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import kotlin.coroutines.cancellation.CancellationException

private val logger = KotlinLogging.logger {}

private const val MAX_CONNECTION_ATTEMPTS = 3
private const val INITIAL_BACKOFF_MS = 1000L

suspend fun runSqlStatement(sql: String, connectionProvider: DatabaseConnectionProvider, queryTimeoutSeconds: Int?) {
    try {
        withContext(Dispatchers.IO) {
            connectWithRetry(connectionProvider).use { connection ->
                connection.createStatement().use { statement ->
                    queryTimeoutSeconds?.let { statement.queryTimeout = it }
                    logger.info { "Executing SQL: $sql" }
                    val rows = executeOperation(sql, statement)
                    logger.info { "Successfully executed, rows: $rows" }
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.error(e) { "Failed to run scheduled SQL statement, will retry on next tick." }
    }
}

private suspend fun connectWithRetry(connectionProvider: DatabaseConnectionProvider): Connection {
    var backoffMs = INITIAL_BACKOFF_MS
    for (attempt in 1..MAX_CONNECTION_ATTEMPTS) {
        try {
            return connectionProvider.getConnection()
        } catch (e: SQLException) {
            if (attempt == MAX_CONNECTION_ATTEMPTS) throw e
            logger.warn(e) { "Connection attempt $attempt/$MAX_CONNECTION_ATTEMPTS failed, retrying in ${backoffMs}ms..." }
            delay(backoffMs)
            backoffMs *= 2
        }
    }
    error("unreachable")
}

fun executeOperation(query: String, statement: Statement): Int {
    if (!statement.execute(query)) return statement.updateCount
    return statement.resultSet.use { resultSet ->
        var rows = 0
        while (resultSet.next()) rows++
        rows
    }
}
