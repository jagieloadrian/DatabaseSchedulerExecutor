package com.anjo

import com.anjo.config.PropertiesConfig
import com.anjo.service.modifySqlDb
import com.anjo.service.runSchedulerFlow
import com.anjo.util.validateArgs
import com.anjo.util.validatePath
import com.anjo.util.validateProperties

suspend fun main(args: Array<String>) {
    val configProps = getConfigProperties(args)
    validateProperties(configProps)
    runSchedulerFlow(configProps.getCron()) {
        modifySqlDb(configProps.getFileDb(), configProps.getStatement())
    }
}

fun getConfigProperties(args: Array<String>): PropertiesConfig {
    val configPath = if (validateArgs(args)) args[1] else throw IllegalArgumentException("Invalid input config file path")
    if (validatePath(configPath)) {
        return PropertiesConfig(configPath)
    } else {
        throw IllegalArgumentException("Invalid input config file path")
    }
}
