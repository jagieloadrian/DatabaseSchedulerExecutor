package com.anjo.service

import java.sql.Connection
import java.sql.DriverManager

interface DatabaseConnectionProvider {
    fun getConnection(): Connection
}


class DriverManagerConnectionProvider(private val url: String) : DatabaseConnectionProvider {
    override fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:$url")
    }
}