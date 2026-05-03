package com.nku.helloworld.auth

import android.content.Intent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nku.helloworld.R

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(
                onLogin = { username, password ->
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show()
                    }
                },
                onToRegister = {
                    startActivity(Intent(this, RegisterActivity::class.java))
                },
                onForgotPassword = {
                    startActivity(Intent(this, ForgotPasswordActivity::class.java))
                },
                onPhoneLogin = {
                    Toast.makeText(this, getString(R.string.auth_phone_login_tips), Toast.LENGTH_SHORT).show()
                },
                onWechatLogin = {
                    Toast.makeText(this, getString(R.string.auth_third_party_tips), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onPhoneLogin: () -> Unit,
    onWechatLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "欢迎回来",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5F587E),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "要继续，请验证您的身份",
            fontSize = 14.sp,
            color = Color(0xFFAAA5C0), // hint lavender
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Username Input
        LoginInputField(
            value = username,
            onValueChange = { username = it },
            hint = "请输入您的账号",
            iconResId = R.drawable.ic_user,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Password Input
        LoginInputField(
            value = password,
            onValueChange = { password = it },
            hint = "请输入您的密码",
            iconResId = R.drawable.ic_password,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onVisibilityChange = { isPasswordVisible = it },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Remember me and Forgot Password
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF5F587E))
            )
            Text(
                text = stringResource(R.string.auth_remember_me),
                color = Color(0xFF5F587E),
                fontSize = 14.sp,
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.auth_forgot_password),
                color = Color(0xFF5F587E),
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onForgotPassword)
                    .padding(8.dp)
            )
        }

        // Login Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFA726), Color(0xFFD500F9)) // orange to purple
                    )
                )
                .clickable { onLogin(username, password) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "登录",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(R.string.auth_other_login),
            color = Color(0xFF888888),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(top = 8.dp, bottom = 10.dp)
                .background(Color(0xFFE0E0E0))
        )

        // Other logins
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ThirdPartyLoginButton(
                text = stringResource(R.string.auth_phone_login),
                iconResId = R.drawable.ic_login_phone,
                onClick = onPhoneLogin,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            ThirdPartyLoginButton(
                text = stringResource(R.string.auth_wechat_login),
                iconResId = R.drawable.ic_login_wechat,
                onClick = onWechatLogin,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }

        Text(
            text = "没有账号？去注册",
            color = Color(0xFF5F587E),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp, bottom = 8.dp)
                .clickable(onClick = onToRegister)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.weight(2f))
    }
}

@Composable
fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    iconResId: Int,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onVisibilityChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
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
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = hint, color = Color.Black, fontSize = 14.sp)
                    }
                    innerTextField()
                }
                if (isPassword && onVisibilityChange != null) {
                    val eyeIcon = if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                    Image(
                        painter = painterResource(id = eyeIcon),
                        contentDescription = "Toggle password visibility",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onVisibilityChange(!isPasswordVisible) }
                    )
                }
            }
        }
    )
}

@Composable
fun ThirdPartyLoginButton(
    text: String,
    iconResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(24.dp)
        )
        Text(
            text = text,
            color = Color(0xFF5F587E),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}