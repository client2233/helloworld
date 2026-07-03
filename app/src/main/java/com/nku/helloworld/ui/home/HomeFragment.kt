package com.nku.helloworld.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.nku.helloworld.ui.plan.CreatePlanActivity
import com.nku.helloworld.ui.plan.PlanDetailActivity
import com.nku.helloworld.ui.plan.PlanLocalStorage
import com.nku.helloworld.ui.plan.model.PlanItem
import com.nku.helloworld.auth.SessionManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.nku.helloworld.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HomeRecentItem(
    val title: String,
    val category: String,
    val time: String,
    val colorRes: Int,
)

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val items = listOf(
        HomeRecentItem("英语单词打卡", stringResource(R.string.home_category_study), "07:30", R.color.status_todo),
        HomeRecentItem("晚间复盘打卡", stringResource(R.string.home_category_study), "21:30", R.color.status_pending),
        HomeRecentItem("晨读完成记录", stringResource(R.string.home_category_study), "06:40", R.color.status_done),
    )

    // 获取登录用户信息
    val userProfile = SessionManager.getUserProfile()
    val displayName = userProfile.displayName.ifEmpty {
        userProfile.nickname.ifEmpty { "用户" }
    }
    val avatarUrl = userProfile.avatarUrl

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var latestPlan by remember { mutableStateOf<PlanItem?>(null) }
    var aiResultText by remember { mutableStateOf("") }

    // 加载最近一次学习过的计划
    fun loadLatestPlan() {
        if (!PlanLocalStorage.isInitialized()) {
            PlanLocalStorage.init(context.applicationContext as android.app.Application)
        }
        val plans = PlanLocalStorage.loadAllPlans()
        latestPlan = plans
            .filter { !it.latestDate.isNullOrBlank() }
            .maxByOrNull { it.latestDate ?: "" }
            ?: plans.firstOrNull()
    }

    // 页面恢复时重新加载
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadLatestPlan()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 首次加载
    LaunchedEffect(Unit) {
        loadLatestPlan()
    }

    fun sendAiRequest(query: String) {
        if (query.isEmpty()) return
        aiResultText = "正在呼叫 AI..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val aiApiUrl = com.nku.helloworld.AppConfig.aiApiUrl
                val aiApiKey = com.nku.helloworld.AppConfig.aiApiKey

                if (aiApiUrl.isEmpty() || aiApiKey.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        aiResultText = "AI 功能未配置，请在 config.properties 中设置 AI_API_URL 和 AI_API_KEY"
                    }
                    return@launch
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(com.nku.helloworld.AppConfig.connectTimeout.toLong() * 6, TimeUnit.SECONDS)
                    .readTimeout(com.nku.helloworld.AppConfig.readTimeout.toLong() * 6, TimeUnit.SECONDS)
                    .build()

                val jsonBody = JSONObject().apply {
                    put("model", "Volc-DeepSeek-V3.2")
                    put("stream", false)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", query)
                        })
                    })
                }

                val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(aiApiUrl)
                    .post(body)
                    .header("Authorization", "Bearer $aiApiKey")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        try {
                            val jsonRes = JSONObject(responseBody)
                            aiResultText = jsonRes.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                        } catch (e: Exception) {
                            aiResultText = "解析报错: ${e.message}\n响应原始数据:$responseBody"
                        }
                    } else {
                        aiResultText = "响应失败: HTTP ${response.code}\n$responseBody"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    aiResultText = "网络请求失败: ${e.message}"
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar(
            displayName = displayName,
            avatarUrl = avatarUrl,
            onSearch = { sendAiRequest(it) }
        )

        BottomSheetScaffold(
            modifier = Modifier.weight(1f),
            scaffoldState = scaffoldState,
            sheetPeekHeight = 190.dp, // 调整默认露出高度，使其只显示"学习计划"和"打卡提醒"的卡片区域
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetContainerColor = colorResource(R.color.surface_white),
            sheetShadowElevation = 4.dp,
            sheetDragHandle = {
                // Internal drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 16.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colorResource(R.color.divider_soft))
                )
            },
            sheetContent = {
                HomeSheetContent(items = items, aiResultText = aiResultText, latestPlan = latestPlan)
            }
        ) { innerPadding ->
            // Background area under the top bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.app_bg))
                    .padding(innerPadding)
            ) {
                Text(
                    text = stringResource(R.string.home_background_slogan),
                    color = colorResource(R.color.text_secondary),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
fun HomeTopBar(
    displayName: String = "用户",
    avatarUrl: String = "",
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.app_bg))
            .padding(top = 16.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_today),
                    color = colorResource(R.color.text_secondary),
                    fontSize = 12.sp
                )
                Text(
                    text = "你好，$displayName",
                    color = colorResource(R.color.text_primary),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 用户头像
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.surface_white))
                    .border(1.dp, colorResource(R.color.divider_soft), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "用户头像",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = displayName.take(1).ifEmpty { "?" },
                        color = colorResource(R.color.text_primary),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .border(1.dp, colorResource(R.color.divider_soft), RoundedCornerShape(26.dp)),
            textStyle = TextStyle(color = colorResource(R.color.text_primary), fontSize = 14.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search_20),
                        contentDescription = stringResource(R.string.content_desc_search),
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.home_hint),
                                color = colorResource(R.color.text_muted),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
fun HomeSheetContent(
    items: List<HomeRecentItem>,
    aiResultText: String,
    latestPlan: PlanItem? = null
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 24.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.home_section_summary),
                color = colorResource(R.color.text_secondary),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Focus Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clickable {
                            val intent = Intent(context, CreatePlanActivity::class.java)
                            context.startActivity(intent)
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.brand_primary_soft))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = stringResource(R.string.home_focus_title), color = colorResource(R.color.text_secondary), fontSize = 13.sp)
                        Text(text = stringResource(R.string.home_focus_value), color = colorResource(R.color.brand_primary), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text(text = stringResource(R.string.home_focus_desc), color = colorResource(R.color.text_secondary), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Done Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.brand_green_soft))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = stringResource(R.string.home_done_title), color = colorResource(R.color.text_secondary), fontSize = 13.sp)
                        Text(text = stringResource(R.string.home_done_value), color = colorResource(R.color.brand_green), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text(text = stringResource(R.string.home_done_desc), color = colorResource(R.color.text_secondary), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // 最近学习计划卡片（替换预留区域）
            if (latestPlan != null) {
                val plan = latestPlan!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clickable {
                            val convId = plan.conversationId ?: if (plan.id > 0) plan.id else null
                            val intent = android.content.Intent(context, PlanDetailActivity::class.java).apply {
                                convId?.let { putExtra(PlanDetailActivity.EXTRA_CONVERSATION_ID, it) }
                                plan.pathId?.let { putExtra(PlanDetailActivity.EXTRA_PATH_ID, it) }
                                putExtra(PlanDetailActivity.EXTRA_PLAN_TITLE, plan.title)
                            }
                            context.startActivity(intent)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_white)),
                    border = BorderStroke(1.dp, colorResource(R.color.divider_soft))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧彩色圆点
                        val macaronColors = listOf(
                            Color(0xFF6B9CE4),
                            Color(0xFF5BC0A0),
                            Color(0xFFF4A261),
                            Color(0xFFB583E4),
                            Color(0xFFE483B5)
                        )
                        val dotColor = macaronColors[plan.colorIndex % macaronColors.size]
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // 中间文字
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plan.title,
                                color = colorResource(R.color.text_primary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (!plan.latestDate.isNullOrBlank()) {
                                Text(
                                    text = "最近学习: ${plan.latestDate}",
                                    color = colorResource(R.color.text_secondary),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // 右侧箭头
                        Text(
                            text = "›",
                            color = colorResource(R.color.text_muted),
                            fontSize = 20.sp
                        )
                    }
                }
            } else {
                // 无计划时显示预留占位
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .padding(top = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.app_bg)),
                    border = BorderStroke(1.dp, colorResource(R.color.divider_soft))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无学习计划\n点击上方「专注」开始创建",
                            color = colorResource(R.color.text_muted),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.home_recent),
                color = colorResource(R.color.text_primary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
            )
        }

        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_white)),
                border = BorderStroke(1.dp, colorResource(R.color.divider_soft))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorResource(item.colorRes))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(text = item.title, color = colorResource(R.color.text_primary), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = item.category, color = colorResource(R.color.text_secondary), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Text(text = item.time, color = colorResource(R.color.text_secondary), fontSize = 11.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(104.dp)) // paddingBottom corresponding to original scrollview
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
