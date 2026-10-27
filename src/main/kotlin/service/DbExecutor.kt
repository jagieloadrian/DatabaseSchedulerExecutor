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
        // JDBC blocks: keep it off the thread delay() resumes on (the coroutine timer thread)
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
        throw e // shutdown/cancel must propagate, not be logged as a failed run
    } catch (e: Exception) {
        // Any failure (connection retries exhausted, SQL error) must not crash the scheduler loop —
        // just skip this tick, the next CRON tick will try again.
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

// Statement type is limited to SELECT/INSERT/UPDATE/DELETE by validateSql. Runs in auto-commit:
// a single statement is its own transaction. Returns rows read (SELECT) or rows affected.
fun executeOperation(query: String, statement: Statement): Int {
    if (!statement.execute(query)) return statement.updateCount
    return statement.resultSet.use { resultSet ->
        var rows = 0
        while (resultSet.next()) rows++
        rows
    }
}
