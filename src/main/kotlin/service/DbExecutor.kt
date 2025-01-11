package com.anjo.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.DriverManager

private val logger = KotlinLogging.logger {}

fun modifySqlDb(dbFilePath: String, sql: String) {
    val url = "jdbc:sqlite:$dbFilePath"

    val driveConnection = DriverManager.getConnection(url)
    driveConnection.use { connection ->
        connection.createStatement().use { statement ->
            logger.info { "Creating database connection..." }
            statement.execute(sql)
            logger.info { "Successfully executed! Close connection." }
        }
    }

    driveConnection.close()
}