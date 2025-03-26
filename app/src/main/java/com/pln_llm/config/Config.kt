package com.pln_llm.config

import java.util.Properties
import java.io.FileInputStream
import java.io.File

object Config {
    private var properties: Properties? = null

    fun init(context: android.content.Context) {
        try {
            properties = Properties().apply {
                load(FileInputStream(context.getExternalFilesDir(null)?.absolutePath + "/config.properties"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val configFile = File(context.getExternalFilesDir(null), "config.properties")
        if (!configFile.exists()) {
            configFile.createNewFile()
            configFile.writeText("OPENAI_API_KEY=your_api_key_here")
        }
    }

    val openAiApiKey: String
        get() = properties?.getProperty("OPENAI_API_KEY") ?: throw IllegalStateException("OpenAI API key not found. Please add it to config.properties")
} 