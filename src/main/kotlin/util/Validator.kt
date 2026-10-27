package com.anjo.util

import com.anjo.config.PropertiesConfig
import com.anjo.service.DbType
import io.github.oshai.kotlinlogging.KotlinLogging
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import java.io.File

private val logger = KotlinLogging.logger {}

fun validateArgs(args: Array<String>): Boolean {
    return args.isNotEmpty() && args.size == 2 && args[0] == "--config"
}

fun validateConfigPath(path: String?): Boolean {
    if (path.isNullOrEmpty()) {
        logger.warn { "Provide empty path!" }
        return false
    }
    val file = File(path)
    if (file.exists().not()) {
        logger.warn { "File on path: $path doesn't exist" }
        return false
    }
    if (file.extension != "properties") {
        logger.warn { "File on path: $path has unexpected extension: ${file.extension}" }
        return false
    }
    return true
}

fun validateSql(sql: String?): Boolean {
    if (sql.isNullOrBlank()) return false
    val statements = try {
        CCJSqlParserUtil.parseStatements(sql)
    } catch (e: JSQLParserException) {
        logger.error(e) { "Error parsing sql" }
        return false
    }
    return when (val statement = statements.singleOrNull()) {
        is Select, is Insert, is Update, is Delete -> true
        null -> {
            logger.error { "SQL must contain exactly one statement, got ${statements.size}" }
            false
        }
        else -> {
            logger.error { "Only SELECT/INSERT/UPDATE/DELETE allowed, got: ${statement::class.simpleName}" }
            false
        }
    }
}

fun validateProperties(configProps: PropertiesConfig) {
    val statement = validateSql(configProps.getStatement())
    val dbType = DbType.fromStringOrSqlite(configProps.getDbType())
    val dbValid = dbType != DbType.SQLITE || validateSqliteFile(configProps.getFileDb())
    require(dbValid and statement) { "DB path or statement must be properly specified" }
}

private fun validateSqliteFile(path: String): Boolean {
    val exists = File(path).isFile
    if (!exists) logger.warn { "SQLite file on path: $path doesn't exist" }
    return exists
}
