package com.anjo.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement

private val logger = KotlinLogging.logger {}

private const val MAX_CONNECTION_ATTEMPTS = 3
private const val INITIAL_BACKOFF_MS = 1000L

fun runSqlStatement(sql: String, connectionProvider: DatabaseConnectionProvider) {
    try {
        connectWithRetry(connectionProvider).use { connection ->
            connection.createStatement().use { statement ->
                logger.info { "Executing SQL: $sql" }
                val rows = executeOperation(sql, statement)
                logger.info { "Successfully executed, rows: $rows" }
            }
        }
    } catch (e: Exception) {
        // Any failure (connection retries exhausted, SQL error) must not crash the scheduler loop —
        // just skip this tick, the next CRON tick will try again.
        logger.error(e) { "Failed to run scheduled SQL statement, will retry on next tick." }
    }
}

private fun connectWithRetry(connectionProvider: DatabaseConnectionProvider): Connection {
    var backoffMs = INITIAL_BACKOFF_MS
    for (attempt in 1..MAX_CONNECTION_ATTEMPTS) {
        try {
            return connectionProvider.getConnection()
        } catch (e: SQLException) {
            if (attempt == MAX_CONNECTION_ATTEMPTS) throw e
            logger.warn(e) { "Connection attempt $attempt/$MAX_CONNECTION_ATTEMPTS failed, retrying in ${backoffMs}ms..." }
            Thread.sleep(backoffMs)
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
