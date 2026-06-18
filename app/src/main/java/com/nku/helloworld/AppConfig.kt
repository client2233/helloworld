package com.nku.helloworld

import android.content.Context
import java.util.Properties

/**
 * 应用配置管理器
 * 从 assets/config.properties 中读取配置信息
 *
 * 所有后端服务器相关的配置均从 config.properties 读取，
 * 代码中不保存任何敏感信息（服务器地址、API密钥等）。
 * config.properties 已被 .gitignore 忽略，不会提交到仓库。
 */
object AppConfig {

    private const val CONFIG_FILE = "config.properties"

    /** 后端服务器基础地址 */
    lateinit var baseUrl: String
        private set

    /** AI 问答 API 地址 */
    @JvmStatic
    lateinit var aiApiUrl: String
        private set

    /** AI 问答 API 密钥 */
    @JvmStatic
    lateinit var aiApiKey: String
        private set

    /** 连接超时（秒） */
    var connectTimeout: Int = 15
        private set

    /** 读取超时（秒） */
    var readTimeout: Int = 15
        private set

    /** 写入超时（秒） */
    var writeTimeout: Int = 15
        private set

    /**
     * 初始化配置，从 assets 中加载 config.properties
     * 需在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        val properties = Properties()
        context.assets.open(CONFIG_FILE).use { inputStream ->
            properties.load(inputStream)
        }

        // 后端服务基础地址
        baseUrl = properties.getProperty("BASE_URL")
            ?: throw IllegalStateException("config.properties 中未找到 BASE_URL 配置")
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.removeSuffix("/")
        }

        // AI 问答 API 地址（可选配置）
        aiApiUrl = properties.getProperty("AI_API_URL", "")
        if (aiApiUrl.endsWith("/")) {
            aiApiUrl = aiApiUrl.removeSuffix("/")
        }

        // AI 问答 API 密钥（可选配置）
        aiApiKey = properties.getProperty("AI_API_KEY", "")

        // 网络超时设置（可选配置，默认 15 秒）
        connectTimeout = properties.getProperty("CONNECT_TIMEOUT")?.toIntOrNull() ?: 15
        readTimeout = properties.getProperty("READ_TIMEOUT")?.toIntOrNull() ?: 15
        writeTimeout = properties.getProperty("WRITE_TIMEOUT")?.toIntOrNull() ?: 15
    }
}
