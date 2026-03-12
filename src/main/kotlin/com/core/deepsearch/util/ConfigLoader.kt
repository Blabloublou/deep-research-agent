package com.jetbrains.deepsearch.util

import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Loads configuration from environment variables and .env file.
 */
object ConfigLoader {
    
    private val config = mutableMapOf<String, String>()
    
    init {
        loadEnvFile()
        loadSystemEnv()
    }
    
    /**
     * Load variables from .env file if it exists.
     */
    private fun loadEnvFile() {
        val envFile = File(".env")
        if (envFile.exists()) {
            envFile.readLines()
                .filter { it.isNotBlank() && !it.trim().startsWith("#") }
                .forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim().removeSurrounding("\"")
                        config[key] = value
                    }
                }
        } else {
            logger.warn { ".env file not found, using system environment variables only" }
        }
    }
    
    /**
     * Load system environment variables (override .env values).
     */
    private fun loadSystemEnv() {
        System.getenv().forEach { (key, value) ->
            config[key] = value
        }
    }
    
    /**
     * Get a configuration value.
     */
    fun get(key: String): String? = config[key]
    
    /**
     * Get a configuration value with a default.
     */
    fun get(key: String, default: String): String {
        return config[key] ?: default
    }
    
    /**
     * Get a configuration value or throw an exception if not found.
     */
    fun getRequired(key: String): String {
        return get(key) ?: throw IllegalStateException("Required configuration key '$key' not found")
    }
    
    /**
     * Get a configuration value with a default.
     */
    fun getOrDefault(key: String, default: String): String {
        return get(key) ?: default
    }
    
    /**
     * Get an integer configuration value.
     */
    fun getInt(key: String, default: Int): Int {
        return get(key)?.toIntOrNull() ?: default
    }
    
    /**
     * Get a double configuration value.
     */
    fun getDouble(key: String, default: Double): Double {
        return get(key)?.toDoubleOrNull() ?: default
    }
    
    /**
     * Get a boolean configuration value.
     */
    fun getBoolean(key: String, default: Boolean): Boolean {
        return get(key)?.toBoolean() ?: default
    }
}

