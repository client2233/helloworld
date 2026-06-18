package com.nku.helloworld

import android.app.Application

/**
 * 应用 Application 类
 * 负责应用级别的初始化工作
 */
class HelloworldApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化应用配置（从 assets/config.properties 加载）
        AppConfig.init(this)
    }
}
