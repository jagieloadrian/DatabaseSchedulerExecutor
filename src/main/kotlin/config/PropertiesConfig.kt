package com.anjo.config

import java.io.FileInputStream
import java.util.Properties

class PropertiesConfig(path: String) {

    private val properties = Properties()

    init {
        val inputStream = FileInputStream(path)
        properties.load(inputStream)
    }

    fun getStatement(): String {
        return properties["statement"]?.toString() ?: throwException("statement")
    }

    fun getFileDb(): String {
        return properties["filepath"]?.toString() ?: throwException("filepath")
    }

    fun getCron(): String {
        return properties["cron"]?.toString() ?: throwException("cron")
    }

    fun getDbType(): String? = properties["dbtype"]?.toString()

    fun getHost(): String? = properties["host"]?.toString()

    fun getPort(): Int? {
        val raw = properties["port"]?.toString() ?: return null
        return raw.toIntOrNull() ?: throw IllegalArgumentException("Property 'port' must be a number, got: $raw")
    }

    fun getDatabase(): String? = properties["database"]?.toString()

    fun getDbUser(): String? = System.getenv("DB_USER") ?: properties["dbuser"]?.toString()

    fun getDbPassword(): String? = System.getenv("DB_PASSWORD") ?: properties["dbpassword"]?.toString()

    private fun throwException(property: String): Nothing {
        throw IllegalArgumentException("Cannot read property '$property'")
    }
}
