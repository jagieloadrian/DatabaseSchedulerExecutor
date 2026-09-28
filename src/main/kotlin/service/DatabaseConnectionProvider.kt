package com.anjo.service

import java.sql.Connection
import java.sql.DriverManager

interface DatabaseConnectionProvider {
    fun getConnection(): Connection
}

enum class DbType(val defaultPort: Int?) {
    SQLITE(null),
    POSTGRESQL(5432),
    MYSQL(3306),
    MARIADB(3306),
    MSSQL(1433),
    ORACLE(1521);

    companion object {
        fun fromStringOrSqlite(value: String?): DbType {
            if (value.isNullOrBlank()) return SQLITE
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported dbtype: $value")
        }
    }
}

fun buildJdbcUrl(dbType: DbType, host: String, port: Int, database: String): String = when (dbType) {
    DbType.SQLITE     -> throw IllegalStateException("SQLite connects via file path, use DriverManagerConnectionProvider.sqlite()")
    DbType.POSTGRESQL -> "jdbc:postgresql://$host:$port/$database"
    DbType.MYSQL      -> "jdbc:mysql://$host:$port/$database"
    DbType.MARIADB    -> "jdbc:mariadb://$host:$port/$database"
    DbType.MSSQL      -> "jdbc:sqlserver://$host:$port;databaseName=$database"
    DbType.ORACLE     -> "jdbc:oracle:thin:@$host:$port:$database"
}

class DriverManagerConnectionProvider(
    private val url: String,
    private val user: String? = null,
    private val password: String? = null,
) : DatabaseConnectionProvider {

    override fun getConnection(): Connection {
        return if (user != null) {
            DriverManager.getConnection(url, user, password)
        } else {
            DriverManager.getConnection(url)
        }
    }

    companion object {
        fun sqlite(filePath: String): DriverManagerConnectionProvider =
            DriverManagerConnectionProvider("jdbc:sqlite:$filePath")

        fun network(
            dbType: DbType,
            host: String,
            port: Int,
            database: String,
            user: String,
            password: String,
        ): DriverManagerConnectionProvider =
            DriverManagerConnectionProvider(buildJdbcUrl(dbType, host, port, database), user, password)
    }
}
