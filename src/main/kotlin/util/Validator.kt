package com.anjo.util

import com.anjo.config.PropertiesConfig
import com.anjo.service.DbType
import io.github.oshai.kotlinlogging.KotlinLogging
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import java.io.File

private val logger = KotlinLogging.logger {}

fun validateArgs(args: Array<String>): Boolean {
    return args.isNotEmpty() && args.size == 2 && args[0] == "--config"
}

fun validatePath(path: String?): Boolean {
    if (path.isNullOrEmpty()) {
        logger.warn { "Provide empty path!" }
        return false
    }
    val file = File(path)
    if (file.exists().not()) {
        logger.warn { "File on path: $path doesn't exist" }
        return false
    }
    return when (file.extension) {
        "properties" -> true
        "db"         -> true
        else         -> {
            logger.warn { "File on path: $path has unexpected extension: ${file.extension}" }
            false
        }
    }
}

fun validateSql(sql: String?): Boolean {
    return if (sql.isNullOrEmpty()) {
        false
    } else {
        try {
            CCJSqlParserUtil.parse(sql)
            return doesntContainSqlInjection(sql)
        } catch (e: JSQLParserException) {
            logger.error(e) { "Error parsing sql" }
            return false
        }
    }
}

fun validateProperties(configProps: PropertiesConfig) {
    val statement = validateSql(configProps.getStatement())
    val dbType = DbType.fromStringOrSqlite(configProps.getDbType())
    val dbValid = if (dbType == DbType.SQLITE) {
        validatePath(configProps.getFileDb())
    } else {
        validateNetworkDbConfig(configProps, dbType)
    }
    require(dbValid and statement) { "DB path or statement must be properly specified" }
}

fun validateNetworkDbConfig(configProps: PropertiesConfig, dbType: DbType): Boolean {
    val host = configProps.getHost()
    val database = configProps.getDatabase()
    val user = configProps.getDbUser()
    return when {
        host.isNullOrBlank()     -> { logger.warn { "Provide host for dbtype=$dbType" }; false }
        database.isNullOrBlank() -> { logger.warn { "Provide database for dbtype=$dbType" }; false }
        user.isNullOrBlank()     -> { logger.warn { "Provide dbuser (property or DB_USER env) for dbtype=$dbType" }; false }
        else                     -> true
    }
}

private fun doesntContainSqlInjection(sql: String): Boolean {
    if (containsStackedStatements(sql)) {
        logger.error { "SQ" +
                "L contains more than one statement (stacked query)" }
        return false
    }
    val patterns = listOf(
            ".*--.*",
            ".*#.*",
            ".*/\\*.*\\*/.*",
            ".*'\\s*OR\\s*('.*'|\\d+\\s*=\\s*\\d+).*",
            ".*DROP.*",
            ".*ALTER.*",
            ".*TRUNCATE.*"
    )
    val pattern = patterns.find { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(sql) }
    return if (pattern.isNullOrEmpty()) {
        true
    } else {
        logger.error { "Contain one of sql injection pattern" }
        false
    }
}

private fun containsStackedStatements(sql: String): Boolean {
    return try {
        CCJSqlParserUtil.parseStatements(sql).size > 1
    } catch (e: JSQLParserException) {
        logger.error(e) { "Fetched exception: $e" }
        true
    }
}
