package com.nku.helloworld

import android.app.Application

/**
 * 应用 Application 类
 * 负责应用级别的初始化工作
 */
class HelloworldApp : Application() {

    companion object {
        lateinit var instance: HelloworldApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppConfig.init(this)
    }
}
