package com.anjo

import com.anjo.config.PropertiesConfig
import com.anjo.service.modifySqlDb
import com.anjo.service.runSchedulerFlow
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

suspend fun main(args: Array<String>) {
    val configProps = getConfigProperties(args)
    runSchedulerFlow(configProps.getCron()) {
        modifySqlDb(configProps.getFileDb(), configProps.getStatement())
    }
}

fun getConfigProperties(args: Array<String>): PropertiesConfig {
    val configPath = if (validateArgs(args)) args[1] else throw IllegalArgumentException("Invalid input file path")
    if (validatePath(configPath)) {
        return PropertiesConfig(configPath)
    } else {
        throw IllegalArgumentException("Invalid input file path")
    }
}

fun validateArgs(args: Array<String>): Boolean {
    return args.isNotEmpty() && args[0] == "--config" && args.size == 2
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
    if (file.extension != "properties") {
        logger.warn { "File on path: $path has unexpected extension" }
        return false
    }
    return true
}
