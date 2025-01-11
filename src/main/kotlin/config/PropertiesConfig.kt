package com.anjo.config

import java.io.FileInputStream
import java.util.Properties

class PropertiesConfig(path:String) {

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

    private fun throwException(property: String): Nothing {
        throw IllegalArgumentException("Cannot read property '$property'")
    }
}
