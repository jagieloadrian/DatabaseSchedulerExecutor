package com.anjo.service

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun runSqlStatement(sql: String, connectionProvider: DatabaseConnectionProvider) {
    val driveConnection = connectionProvider.getConnection()
    driveConnection.use { connection ->
        logger.info { "Creating database connection..." }
        connection.createStatement().use { statement ->
            logger.info { "Execute sql statement..." }
            statement.execute(sql)
            logger.info { "Successfully executed! Close connection." }
        }
    }

    driveConnection.close()
}