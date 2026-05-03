package com.nku.helloworld.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nku.helloworld.R

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterScreen(
                onBack = { finish() },
                onRegister = { phone, password ->
                    when {
                        phone.isEmpty() -> {
                            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
                        }
                        !phone.matches(Regex("^1\\d{10}$")) -> {
                            Toast.makeText(this, "请输入有效的手机号（11位，以1开头）", Toast.LENGTH_SHORT).show()
                        }
                        password.isEmpty() -> {
                            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                        }
                        password.length < 6 -> {
                            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                },
                onToLogin = { finish() }
            )
        }
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegister: (String, String) -> Unit,
    onToLogin: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.back),
            contentDescription = stringResource(R.string.auth_back),
            modifier = Modifier
                .padding(top = 16.dp)
                .size(32.dp)
                .clickable(onClick = onBack)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "创建新账号",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5F587E),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "请输入您的账号和密码",
            fontSize = 14.sp,
            color = Color(0xFFAAA5C0),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Phone Input
        RegisterInputField(
            value = phone,
            onValueChange = { phone = it },
            hint = stringResource(R.string.auth_phone_hint),
            iconResId = R.drawable.ic_user,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Password Input
        RegisterInputField(
            value = password,
            onValueChange = { password = it },
            hint = stringResource(R.string.auth_password_hint),
            iconResId = R.drawable.ic_password,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onVisibilityChange = { isPasswordVisible = it },
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Register Button
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
                .clickable { onRegister(phone, password) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "注册",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // To Login Text
        Text(
            text = "已有账号？去登录",
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
                        Text(text = hint, color = Color.Gray, fontSize = 14.sp)
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
