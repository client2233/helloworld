package com.nku.helloworld.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nku.helloworld.R

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化会话管理器
        if (!SessionManager.isInitialized()) {
            SessionManager.init(this)
        }
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

            // 处理 UI 状态变化
            LaunchedEffect(uiState) {
                when (val state = uiState) {
                    is AuthUiState.RegisterSuccess -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            "注册成功，请登录",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    is AuthUiState.Error -> {
                        Toast.makeText(
                            this@RegisterActivity,
                            state.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        authViewModel.resetState()
                    }
                    else -> { /* Idle / Loading */ }
                }
            }

            RegisterScreen(
                isLoading = uiState is AuthUiState.Loading,
                errorMessage = (uiState as? AuthUiState.Error)?.message,
                onBack = { finish() },
                onRegister = { username, password, displayName ->
                    authViewModel.register(username, password, displayName)
                },
                onToLogin = { finish() },
                onClearError = { authViewModel.resetState() }
            )
        }
    }
}

@Composable
fun RegisterScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit,
    onRegister: (String, String, String) -> Unit,
    onToLogin: () -> Unit,
    onClearError: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    // 本地表单校验（不覆盖来自 API 的错误）
    val displayError = localError ?: errorMessage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp)
    ) {
        // ── 顶部返回按钮 ──
        Icon(
            painter = painterResource(id = R.drawable.ic_auth_back),
            contentDescription = stringResource(R.string.auth_back),
            tint = Color(0xFF5F587E),
            modifier = Modifier
                .padding(top = 16.dp)
                .size(32.dp)
                .clickable(onClick = onBack)
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── 标题 ──
        Text(
            text = "创建新账号",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5F587E),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "注册后即可同步学习进度",
            fontSize = 14.sp,
            color = Color(0xFFAAA5C0),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // ── 错误提示 ──
        if (displayError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ ",
                        fontSize = 14.sp
                    )
                    Text(
                        text = displayError,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            localError = null
                            onClearError()
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Text("✕", fontSize = 11.sp, color = Color(0xFFD32F2F))
                    }
                }
            }
        }

        // ── 用户名输入 ──
        RegisterInputField(
            value = username,
            onValueChange = {
                username = it
                localError = null
                onClearError()
            },
            hint = "请输入用户名",
            iconResId = R.drawable.ic_user,
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── 显示名称输入 ──
        RegisterInputField(
            value = displayName,
            onValueChange = {
                displayName = it
                localError = null
                onClearError()
            },
            hint = "请输入显示名称（如：张三）",
            iconResId = R.drawable.ic_user,
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── 密码输入 ──
        RegisterInputField(
            value = password,
            onValueChange = {
                password = it
                localError = null
                onClearError()
            },
            hint = stringResource(R.string.auth_password_hint),
            iconResId = R.drawable.ic_password,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onVisibilityChange = { isPasswordVisible = it },
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── 确认密码输入 ──
        RegisterInputField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = null
                onClearError()
            },
            hint = stringResource(R.string.auth_confirm_password_hint),
            iconResId = R.drawable.ic_password,
            isPassword = true,
            isPasswordVisible = isConfirmPasswordVisible,
            onVisibilityChange = { isConfirmPasswordVisible = it },
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ── 注册按钮 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFA726), Color(0xFFD500F9))
                    )
                )
                .clickable(enabled = !isLoading) {
                    // 本地表单校验
                    when {
                        username.isBlank() -> {
                            localError = "请输入用户名"
                        }
                        username.length < 3 -> {
                            localError = "用户名至少 3 个字符"
                        }
                        displayName.isBlank() -> {
                            localError = "请输入显示名称"
                        }
                        password.isBlank() || password.length < 6 -> {
                            localError = "密码长度至少 6 位"
                        }
                        confirmPassword != password -> {
                            localError = "两次输入的密码不一致"
                        }
                        else -> {
                            localError = null
                            onRegister(username, password, displayName)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Text(
                    text = "注册",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── 去登录 ──
        Text(
            text = stringResource(R.string.auth_to_login),
            color = Color(0xFF5F587E),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 24.dp)
                .clickable(onClick = onToLogin)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.weight(2f))
    }
}

@Composable
fun RegisterInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    iconResId: Int,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onVisibilityChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp)),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = Color(0xFF5F587E),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = hint, color = Color.Gray, fontSize = 14.sp)
                    }
                    innerTextField()
                }
                if (isPassword && onVisibilityChange != null) {
                    val eyeIcon = if (isPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                    Icon(
                        painter = painterResource(id = eyeIcon),
                        contentDescription = stringResource(R.string.auth_toggle_password_visibility),
                        tint = Color(0xFF5F587E),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onVisibilityChange(!isPasswordVisible) }
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        onBack = {},
        onRegister = { _, _, _ -> },
        onToLogin = {}
    )
}
