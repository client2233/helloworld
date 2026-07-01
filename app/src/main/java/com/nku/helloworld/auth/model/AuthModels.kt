package com.nku.helloworld.auth.model

import com.google.gson.annotations.SerializedName

/**
 * 统一 API 响应结构，对应后端 {code, message, data}
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
)

/**
 * 登录请求体
 */
data class LoginRequest(
    val username: String,
    val password: String,
)

/**
 * 注册请求体
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    @SerializedName("display_name")
    val displayName: String,
)

/**
 * 登录成功返回的数据（JWT token）
 */
data class LoginData(
    val access_token: String,
    val token_type: String = "bearer",
)

/**
 * 注册成功返回的数据
 */
data class RegisterData(
    @SerializedName("avatar_url")
    val avatarURL: String = "",
    @SerializedName("display_name")
    val displayName: String = "",
    val id: Long = 0,
    val status: String = "",
    val username: String = "",
)
