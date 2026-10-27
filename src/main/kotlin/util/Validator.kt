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
    if (sql.isNullOrBlank()) return false
    val statements = try {
        CCJSqlParserUtil.parseStatements(sql)
    } catch (e: JSQLParserException) {
        logger.error(e) { "Error parsing sql" }
        return false
    }
    // Scope guard, not injection filtering: the config file is trusted (its author holds the DB
    // credentials anyway). Allow exactly one DML statement, so DROP/ALTER/TRUNCATE/stacked queries fail.
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
