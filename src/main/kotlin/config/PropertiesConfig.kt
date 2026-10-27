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

    fun getDbType(): String? = optional("dbtype")

    fun getHost(): String? = optional("host")

    fun getPort(): Int? {
        val raw = optional("port") ?: return null
        return raw.toIntOrNull() ?: throw IllegalArgumentException("Property 'port' must be a number, got: $raw")
    }

    fun getDatabase(): String? = optional("database")

    fun getDbUser(): String? = env("DB_USER") ?: optional("dbuser")

    fun getDbPassword(): String? = env("DB_PASSWORD") ?: optional("dbpassword")

    // blank (`host=`) counts as missing
    private fun optional(property: String): String? = properties[property]?.toString()?.takeIf { it.isNotBlank() }

    private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

    private fun throwException(property: String): Nothing {
        throw IllegalArgumentException("Cannot read property '$property'")
    }
}
