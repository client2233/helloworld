package com.nku.helloworld.auth.data

import com.nku.helloworld.auth.api.AuthApiService
import com.nku.helloworld.auth.model.LoginData
import com.nku.helloworld.auth.model.RegisterData

/**
 * 认证数据仓库，封装业务逻辑，统一处理网络结果。
 */
class AuthRepository {

    /**
     * 执行登录，返回 Result<LoginData>
     */
    suspend fun login(username: String, password: String): Result<LoginData> {
        return AuthApiService.login(username, password)
    }

    /**
     * 执行注册，返回 Result<RegisterData>
     */
    suspend fun register(username: String, password: String, displayName: String): Result<RegisterData> {
        return AuthApiService.register(username, password, displayName)
    }
}
