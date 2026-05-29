package com.nku.helloworld.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nku.helloworld.auth.data.AuthRepository
import com.nku.helloworld.auth.model.LoginData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 认证 UI 状态
 */
sealed class AuthUiState {
    /** 初始空闲状态 */
    data object Idle : AuthUiState()
    /** 加载中 */
    data object Loading : AuthUiState()
    /** 登录成功，携带 token */
    data class LoginSuccess(val loginData: LoginData) : AuthUiState()
    /** 注册成功 */
    data object RegisterSuccess : AuthUiState()
    /** 操作失败，携带错误信息 */
    data class Error(val message: String) : AuthUiState()
}

/**
 * 认证 ViewModel，管理登录/注册状态。
 */
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * 登录
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("请输入账号和密码")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(username, password)
            _uiState.value = result.fold(
                onSuccess = { loginData ->
                    AuthUiState.LoginSuccess(loginData)
                },
                onFailure = { error ->
                    AuthUiState.Error(error.message ?: "登录失败，请稍后重试")
                }
            )
        }
    }

    /**
     * 注册
     */
    fun register(phone: String, password: String) {
        if (phone.isBlank()) {
            _uiState.value = AuthUiState.Error("请输入手机号")
            return
        }
        if (!phone.matches(Regex("^1\\d{10}$"))) {
            _uiState.value = AuthUiState.Error("请输入有效的手机号（11位，以1开头）")
            return
        }
        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("请输入密码")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("密码至少6位")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.register(phone, password)
            _uiState.value = result.fold(
                onSuccess = {
                    AuthUiState.RegisterSuccess
                },
                onFailure = { error ->
                    AuthUiState.Error(error.message ?: "注册失败，请稍后重试")
                }
            )
        }
    }

    /**
     * 重置状态到 Idle（例如错误提示关闭后）
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
