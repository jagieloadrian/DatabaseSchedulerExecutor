package com.anjo.util

import com.anjo.config.PropertiesConfig
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
    val dbPath = validatePath(configProps.getFileDb())
    val statement = validateSql(configProps.getStatement())
    require(dbPath and statement) { "DB path or statement must be properly specified" }
}

private fun doesntContainSqlInjection(sql: String): Boolean {
    val patterns = listOf(
            ".*(;.*;)+.*",
            ".*--.*",
            ".*' OR '.*=.*",
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
