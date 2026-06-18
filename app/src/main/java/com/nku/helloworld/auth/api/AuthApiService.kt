package com.nku.helloworld.auth.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nku.helloworld.AppConfig
import com.nku.helloworld.auth.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 认证 API 服务，封装登录/注册等网络请求。
 * 基于 OkHttp + Gson，支持协程。
 */
object AuthApiService {

    /** 从配置文件中读取的后端服务器基础地址 */
    private val BASE_URL get() = AppConfig.baseUrl

    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.connectTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(AppConfig.readTimeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(AppConfig.writeTimeout.toLong(), TimeUnit.SECONDS)
            .build()
    }

    /**
     * 登录
     * POST /api/v1/auth/login
     */
    suspend fun login(username: String, password: String): Result<LoginData> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = LoginRequest(username, password)
                val jsonBody = gson.toJson(requestBody)

                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/auth/login")
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<LoginData>>() {}.type
                val apiResponse: ApiResponse<LoginData> = gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(
                        AuthException(apiResponse.code, apiResponse.message)
                    )
                }

                val data = apiResponse.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(AuthException(-1, "登录返回数据为空"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 注册
     * POST /api/v1/auth/register
     */
    suspend fun register(username: String, password: String, displayName: String): Result<RegisterData> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = RegisterRequest(username, password, displayName)
                val jsonBody = gson.toJson(requestBody)

                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/auth/register")
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<RegisterData>>() {}.type
                val apiResponse: ApiResponse<RegisterData> = gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(
                        AuthException(apiResponse.code, apiResponse.message)
                    )
                }

                Result.success(apiResponse.data ?: RegisterData())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

}

/**
 * 认证业务异常
 */
class AuthException(val code: Int, override val message: String) : Exception(message)
