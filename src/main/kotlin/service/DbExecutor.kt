package com.anjo.service

import io.github.oshai.kotlinlogging.KotlinLogging
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import java.sql.SQLException
import java.sql.Statement

private val logger = KotlinLogging.logger {}

fun runSqlStatement(sql: String, connectionProvider: DatabaseConnectionProvider) {
    val driveConnection = connectionProvider.getConnection()
    driveConnection.use { connection ->
        logger.info { "Creating database connection..." }
        connection.createStatement().use { statement ->
            logger.info { "Execute sql statement..." }
            executeOperation(sql, statement).also {
                if (it.toIntOrNull() != null) {
                    logger.info { "SQL statement affected: $it rows" }
                } else {
                    logger.info { "SQL statement failed to execute with message: $it" }
                }
            }
            logger.info { "Successfully executed! Close connection." }
        }
    }
    driveConnection.close()
}

fun executeOperation(query: String, statement: Statement): String {
    try {
        val localStatement = CCJSqlParserUtil.parse(query)
        return when (localStatement) {
            is Select                       -> executeSelectQuery(query, statement)
            is Insert, is Update, is Delete -> executeUpdateQuery(query, statement)
            else                            -> "Unknown Operation"
        }
    } catch (e: Exception) {
        logger.error(e) { "Error executing SQL statement." }
        return "Invalid SQL"
    }
}


fun executeUpdateQuery(query: String, statement: Statement): String {
    var rowsAffected = 0
    try {
        logSqlQuery(query)
        rowsAffected = statement.executeUpdate(query)
        statement.connection.commit()
        statement.close()
    } catch (e: SQLException) {
        logger.error(e) { "Error while executing update query." }
    }
    return rowsAffected.toString()
}

fun executeSelectQuery(query: String, statement: Statement): String {
    var result = 0
    try {
        logSqlQuery(query)
        val resultSet = statement.executeQuery(query)
        while (resultSet.next()) {
            result++
        }
        resultSet.close()
        statement.close()
    } catch (e: SQLException) {
        logger.error(e) { "Error while executing select query." }
    }
    return result.toString()
}

fun logSqlQuery(query: String) {
    logger.info { "Executing SQL: $query" }
}