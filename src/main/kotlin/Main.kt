package com.anjo

import com.anjo.config.PropertiesConfig
import com.anjo.service.DatabaseConnectionProvider
import com.anjo.service.DbType
import com.anjo.service.DriverManagerConnectionProvider
import com.anjo.service.calculateNextExecutionDuration
import com.anjo.service.runSchedulerFlow
import com.anjo.service.runSqlStatement
import com.anjo.util.validateArgs
import com.anjo.util.validatePath
import com.anjo.util.validateProperties

suspend fun main(args: Array<String>) {
    val configProps = getConfigProperties(args)
    validateProperties(configProps)
    val connectionProvider = buildConnectionProvider(configProps)
    runSchedulerFlow({ calculateNextExecutionDuration(configProps.getCron()) }) {
        runSqlStatement(configProps.getStatement(), connectionProvider)
    }
}

fun getConfigProperties(args: Array<String>): PropertiesConfig {
    val configPath =
        if (validateArgs(args)) args[1] else throw IllegalArgumentException("Invalid input config file path")
    if (validatePath(configPath)) {
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
