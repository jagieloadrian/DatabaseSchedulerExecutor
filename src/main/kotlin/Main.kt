package com.anjo

import com.anjo.config.PropertiesConfig
import com.anjo.service.DatabaseConnectionProvider
import com.anjo.service.DbType
import com.anjo.service.DriverManagerConnectionProvider
import com.anjo.service.calculateNextExecutionDuration
import com.anjo.service.runSchedulerFlow
import com.anjo.service.runSqlStatement
import com.anjo.util.validateArgs
import com.anjo.util.validateConfigPath
import com.anjo.util.validateProperties

// Long enough for typical batch statements, short enough that a hung one (e.g. lock wait) can't block every later tick
private const val DEFAULT_QUERY_TIMEOUT_SECONDS = 3600

suspend fun main(args: Array<String>) {
    val configProps = getConfigProperties(args)
    validateProperties(configProps)
    val connectionProvider = buildConnectionProvider(configProps)
    val queryTimeoutSeconds = resolveQueryTimeoutSeconds(configProps)
    runSchedulerFlow({ calculateNextExecutionDuration(configProps.getCron()) }) {
        runSqlStatement(configProps.getStatement(), connectionProvider, queryTimeoutSeconds)
    }
}

fun getConfigProperties(args: Array<String>): PropertiesConfig {
    val configPath =
        if (validateArgs(args)) args[1] else throw IllegalArgumentException("Invalid input config file path")
    if (validateConfigPath(configPath)) {
        return PropertiesConfig(configPath)
    } else {
        throw IllegalArgumentException("Invalid input config file path")
    }
}

fun buildConnectionProvider(configProps: PropertiesConfig): DatabaseConnectionProvider {
    val dbType = DbType.fromStringOrSqlite(configProps.getDbType())
    return if (dbType == DbType.SQLITE) {
        DriverManagerConnectionProvider.sqlite(configProps.getFileDb())
    } else {
        DriverManagerConnectionProvider.network(
            dbType = dbType,
            host = configProps.getHost() ?: throw IllegalArgumentException("Property 'host' required for dbtype=$dbType"),
            port = configProps.getPort() ?: dbType.defaultPort ?: throw IllegalArgumentException("Property 'port' required for dbtype=$dbType"),
            database = configProps.getDatabase() ?: throw IllegalArgumentException("Property 'database' required for dbtype=$dbType"),
            user = configProps.getDbUser() ?: throw IllegalArgumentException("Property 'dbuser' (or DB_USER env) required for dbtype=$dbType"),
            password = configProps.getDbPassword() ?: "",
        )
    }
}

// SQLite maps queryTimeout to its lock-wait (busy) timeout and can't cancel a running query,
// so without an explicit querytimeout it keeps the driver default (3s lock wait). null = driver default.
fun resolveQueryTimeoutSeconds(configProps: PropertiesConfig): Int? =
    configProps.getQueryTimeoutSeconds()
        ?: DEFAULT_QUERY_TIMEOUT_SECONDS.takeUnless { DbType.fromStringOrSqlite(configProps.getDbType()) == DbType.SQLITE }
