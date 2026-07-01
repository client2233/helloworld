package com.nku.helloworld.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.nku.helloworld.R
import com.nku.helloworld.auth.LoginActivity
import com.nku.helloworld.auth.RegisterActivity
import com.nku.helloworld.auth.SessionManager
import com.nku.helloworld.auth.model.UserProfile

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 在首次使用前初始化 SessionManager
        if (!SessionManager.isInitialized()) {
            SessionManager.init(requireContext())
        }

        return ComposeView(requireContext()).apply {
            setContent {
                ProfileApp()
            }
        }
    }
}

@Composable
fun ProfileApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn()) }
    var userProfile by remember { mutableStateOf(SessionManager.getUserProfile()) }
    var nicknameDialogShown by remember { mutableStateOf(false) }
    var logoutDialogShown by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(false) }

    // 监听生命周期变化，从 LoginActivity 返回时刷新登录状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLoggedIn = SessionManager.isLoggedIn()
                userProfile = SessionManager.getUserProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.app_bg))
    ) {
        if (isLoggedIn) {
            ProfileLoggedInScreen(
                userProfile = userProfile,
                notificationEnabled = notificationEnabled,
                onNotificationToggle = { notificationEnabled = it },
                onEditNickname = { nicknameDialogShown = true },
                onLogout = { logoutDialogShown = true }
            )
        } else {
            ProfileLoggedOutScreen(
                onOpenLogin = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                onOpenRegister = {
                    context.startActivity(Intent(context, RegisterActivity::class.java))
                }
            )
        }

        // 品牌水印（背景装饰）
        BrandWatermark()
    }

    // 编辑昵称对话框
    if (nicknameDialogShown) {
        EditNicknameDialog(
            currentNickname = userProfile.displayName.ifEmpty { userProfile.nickname },
            onDismiss = { nicknameDialogShown = false },
            onConfirm = { newNickname ->
                SessionManager.updateNickname(newNickname)
                SessionManager.updateDisplayName(newNickname)
                userProfile = userProfile.copy(
                    nickname = newNickname,
                    displayName = newNickname
                )
                nicknameDialogShown = false
            }
        )
    }

    // 退出登录确认对话框
    if (logoutDialogShown) {
        LogoutConfirmDialog(
            onDismiss = { logoutDialogShown = false },
            onConfirm = {
                SessionManager.logout()
                isLoggedIn = false
                userProfile = UserProfile()
                logoutDialogShown = false
            }
        )
    }
}

// ========== 已登录状态 ==========

@Composable
fun ProfileLoggedInScreen(
    userProfile: UserProfile,
    notificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    onEditNickname: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // 可滚动内容区域
        Column(
            modifier = Modifier
                .weight(1f)
                        .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // ---- 用户基本信息区 ----
            UserProfileSection(
                userProfile = userProfile,
                onEditNickname = onEditNickname
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ---- 数据卡片区 ----
            DataCard(
                totalDays = userProfile.totalDays,
                streakDays = userProfile.streakDays
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 设置开关区 ----
            SettingsCard(
                notificationEnabled = notificationEnabled,
                onNotificationToggle = onNotificationToggle
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ---- 退出登录按钮（固定在底部） ----
        LogoutButton(
            onClick = onLogout,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

// ========== 未登录状态 ==========

@Composable
fun ProfileLoggedOutScreen(
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 52.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            color = colorResource(id = R.color.text_primary),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.profile_not_logged_in),
            color = colorResource(id = R.color.text_secondary),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // 未登录头像占位
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(colorResource(id = R.color.brand_primary_soft))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = null,
                tint = colorResource(id = R.color.text_muted),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.brand_primary)
            )
        ) {
            Text(text = stringResource(R.string.auth_login), fontSize = 16.sp)
        }

        OutlinedButton(
            onClick = onOpenRegister,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, colorResource(id = R.color.brand_primary)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colorResource(id = R.color.brand_primary)
            )
        ) {
            Text(text = stringResource(R.string.auth_register), fontSize = 16.sp)
        }
    }
}

// ========== 用户基本信息区 ==========

@Composable
fun UserProfileSection(
    userProfile: UserProfile,
    onEditNickname: () -> Unit
) {
    // 取显示名称，优先 displayName，降级到 nickname
    val displayName = userProfile.displayName.ifEmpty {
        userProfile.nickname.ifEmpty { "用户" }
    }
    val avatarUrl = userProfile.avatarUrl

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---- 头像 ----
        Box(
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFA726),
                                Color(0xFFD500F9)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = displayName.take(1),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 相机/更换头像悬浮按钮
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, colorResource(id = R.color.brand_primary), CircleShape)
                    .clickable { /* 更换头像功能预留 */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.profile_camera),
                    tint = colorResource(id = R.color.brand_primary),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- 昵称 + 编辑按钮 ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onEditNickname)
        ) {
            Text(
                text = displayName,
                color = colorResource(id = R.color.text_primary),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = stringResource(R.string.profile_edit_nickname),
                tint = colorResource(id = R.color.text_muted),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ---- 用户 ID ----
        Text(
            text = stringResource(R.string.profile_user_id_prefix) + userProfile.id.toString().padStart(6, '0'),
            color = colorResource(id = R.color.text_muted),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ---- 已完成目标数 ----
        Text(
            text = stringResource(R.string.profile_completed_goals, userProfile.completedGoals),
            color = colorResource(id = R.color.brand_primary),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ========== 数据卡片区 ==========

@Composable
fun DataCard(
    totalDays: Int,
    streakDays: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：统计图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorResource(id = R.color.brand_primary_soft)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chart),
                    contentDescription = stringResource(R.string.profile_data_title),
                    tint = colorResource(id = R.color.brand_primary),
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧：数据文本
            Column {
                Text(
                    text = stringResource(R.string.profile_data_title),
                    color = colorResource(id = R.color.text_secondary),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.profile_total_days, totalDays),
                        color = colorResource(id = R.color.text_primary),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(colorResource(id = R.color.divider_soft))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.profile_streak_days, streakDays),
                        color = colorResource(id = R.color.brand_green),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ========== 设置开关卡片 ==========

@Composable
fun SettingsCard(
    notificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorResource(id = R.color.brand_orange_soft)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notifications_black_24dp),
                    contentDescription = null,
                    tint = colorResource(id = R.color.brand_orange),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 中间：标签
            Text(
                text = stringResource(R.string.profile_notification_toggle),
                color = colorResource(id = R.color.text_primary),
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            // 右侧：开关
            Switch(
                checked = notificationEnabled,
                onCheckedChange = onNotificationToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colorResource(id = R.color.brand_primary),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = colorResource(id = R.color.divider_soft)
                )
            )
        }
    }
}

// ========== 退出登录按钮 ==========

@Composable
fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE53935)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_logout),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.profile_logout),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ========== 品牌水印 ==========

@Composable
fun BrandWatermark() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "主页计划",
            color = Color(0x0D4D7CFE), // 透明度约 5%
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ========== 编辑昵称对话框 ==========

@Composable
fun EditNicknameDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_edit_nickname),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text(stringResource(R.string.profile_edit_nickname)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nickname) }
            ) {
                Text(
                    stringResource(R.string.auth_login),
                    color = colorResource(id = R.color.brand_primary)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.auth_back),
                    color = colorResource(id = R.color.text_secondary)
                )
            }
        }
    )
}

// ========== 退出确认对话框 ==========

@Composable
fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_logout_confirm),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.profile_logout_success),
                color = colorResource(id = R.color.text_secondary)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = stringResource(R.string.profile_logout),
                    color = Color(0xFFE53935)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.auth_back),
                    color = colorResource(id = R.color.text_secondary)
                )
            }
        }
    )
}

// ========== Preview ==========

@Preview(showBackground = true)
@Composable
fun ProfileLoggedInPreview() {
    ProfileLoggedInScreen(
        userProfile = UserProfile(
            id = 10086,
            nickname = "LittleWatter",
            displayName = "LittleWatter",
            phone = "13800138000",
            completedGoals = 12,
            totalDays = 68,
            streakDays = 7
        ),
        notificationEnabled = false,
        onNotificationToggle = {},
        onEditNickname = {},
        onLogout = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileLoggedOutPreview() {
    ProfileLoggedOutScreen(
        onOpenLogin = {},
        onOpenRegister = {}
    )
}
