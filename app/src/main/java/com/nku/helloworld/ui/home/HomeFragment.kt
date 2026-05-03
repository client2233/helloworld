package com.nku.helloworld.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val coroutineScope = rememberCoroutineScope()
    var aiResultText by remember { mutableStateOf("学习计划内容区域（预留）") }

    fun sendAiRequest(query: String) {
        if (query.isEmpty()) return
        aiResultText = "正在呼叫 AI..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val API_URL = ""
                val APP_ID_OR_TOKEN = ""

                val client = OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
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
                val targetUrl = if (API_URL.isNotEmpty()) API_URL else "http://10.0.2.2:8080/v1/chat/completions"
                val request = Request.Builder()
                    .url(targetUrl)
                    .post(body)
                    .header("Authorization", "Bearer $APP_ID_OR_TOKEN")
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
        HomeTopBar(onSearch = { sendAiRequest(it) })

        BottomSheetScaffold(
            modifier = Modifier.weight(1f),
            scaffoldState = scaffoldState,
            sheetPeekHeight = 190.dp, // 调整默认露出高度，使其只显示“学习计划”和“打卡提醒”的卡片区域
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
                HomeSheetContent(items, aiResultText)
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
fun HomeTopBar(onSearch: (String) -> Unit) {
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
                    text = stringResource(R.string.home_greeting),
                    color = colorResource(R.color.text_primary),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.surface_white))
                    .border(1.dp, colorResource(R.color.divider_soft), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LW",
                    color = colorResource(R.color.text_primary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
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
fun HomeSheetContent(items: List<HomeRecentItem>, aiResultText: String) {
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
                        .height(96.dp),
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

            // AI Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
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
                        text = aiResultText,
                        color = colorResource(R.color.text_primary),
                        fontSize = 14.sp
                    )
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