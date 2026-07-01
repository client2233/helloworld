package com.nku.helloworld.auth

import android.content.Context
import android.content.SharedPreferences
import com.nku.helloworld.auth.model.UserProfile

/**
 * 会话管理器，使用 SharedPreferences 持久化登录状态和用户信息。
 */
object SessionManager {

    private const val PREFS_NAME = "helloworld_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_AVATAR_URL = "avatar_url"
    private const val KEY_PHONE = "phone"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences
    private var _initialized = false

    /**
     * 初始化，需在 Application 或首次使用前调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _initialized = true
    }

    /**
     * 是否已初始化
     */
    fun isInitialized(): Boolean = _initialized

    /**
     * 保存登录信息
     */
    fun saveLogin(
        accessToken: String,
        nickname: String = "",
        displayName: String = "",
        avatarUrl: String = "",
        phone: String = "",
        userId: Int = 0
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .putString(KEY_PHONE, phone)
            .putInt(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    /**
     * 获取访问令牌
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * 获取用户信息
     */
    fun getUserProfile(): UserProfile {
        return UserProfile(
            id = prefs.getInt(KEY_USER_ID, 0),
            nickname = prefs.getString(KEY_NICKNAME, "用户") ?: "用户",
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            avatarUrl = prefs.getString(KEY_AVATAR_URL, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        )
    }

    /**
     * 更新昵称
     */
    fun updateNickname(nickname: String) {
        prefs.edit()
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }

    /**
     * 更新显示名称
     */
    fun updateDisplayName(displayName: String) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    /**
     * 更新头像地址
     */
    fun updateAvatarUrl(avatarUrl: String) {
        prefs.edit()
            .putString(KEY_AVATAR_URL, avatarUrl)
            .apply()
    }

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * 清除登录信息（退出登录）
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
