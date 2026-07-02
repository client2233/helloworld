package com.nku.helloworld.ui.plan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nku.helloworld.R
import com.nku.helloworld.auth.SessionManager
import com.nku.helloworld.ui.plan.api.PlanApiService
import com.nku.helloworld.ui.plan.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI 对话式创建学习计划页面
 *
 * 参考 api.md 中的接口实现 AI 对话创建学习路径：
 * 1. POST   /api/v1/conversations          创建会话
 * 2. POST   /api/v1/messages/question       提交问题
 * 3. GET    /api/v1/tasks/{task_id}/result  轮询任务结果
 * 4. 从 meta_json.learning_path 解析学习路径
 */
class CreatePlanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isInitialized()) {
            SessionManager.init(this)
        }
        if (!PlanLocalStorage.isInitialized()) {
            PlanLocalStorage.init(this)
        }
        setContent {
            CreatePlanScreen(onBack = { finish() })
        }
    }
}


// ============================================================
// 热门学习方向（快捷输入）
// ============================================================
private val HotTopics = listOf(
    "Python 编程入门，请生成学习路径",
    "Java 后端开发，请生成学习路径",
    "英语口语练习，请生成学习路径",
    "机器学习基础，请生成学习路径",
    "Android 开发入门，请生成学习路径",
    "数据结构与算法，请生成学习路径",
)

// ============================================================
// AI 对话式创建学习计划主页面
// ============================================================

@Composable
fun CreatePlanScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // ── 聊天状态 ──
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentConversationId by remember { mutableStateOf<Long?>(null) }

    // ── 检查登录 ──
    val isLoggedIn = remember { SessionManager.isLoggedIn() }

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // ── 模拟 AI 回复（API 不可用时使用） ──
    fun createMockResponse(userQuery: String): ChatMessage {
        val lp = LearningPathMeta(
            title = when {
                userQuery.contains("Python", ignoreCase = true) -> "Python 入门路径"
                userQuery.contains("Java", ignoreCase = true) -> "Java 进阶路径"
                userQuery.contains("英语", ignoreCase = true) || userQuery.contains("English", ignoreCase = true) -> "英语学习路径"
                userQuery.contains("Android", ignoreCase = true) -> "Android 开发路径"
                userQuery.contains("机器学习", ignoreCase = true) || userQuery.contains("ML", ignoreCase = true) -> "机器学习路径"
                userQuery.contains("算法", ignoreCase = true) || userQuery.contains("数据结构", ignoreCase = true) -> "数据结构与算法路径"
                else -> "个性化学习路径"
            },
            goal = userQuery,
            nodes = listOf(
                LearningPathNodeMeta(id = 1, title = "了解基础知识", description = "学习相关领域的基础概念和原理，奠定扎实的理论基础", status = "available"),
                LearningPathNodeMeta(id = 2, title = "动手实践", description = "通过练习和项目巩固所学知识，在实践中加深理解", status = "locked"),
                LearningPathNodeMeta(id = 3, title = "进阶提升", description = "深入学习高级主题和最佳实践，拓展技术视野", status = "locked"),
                LearningPathNodeMeta(id = 4, title = "综合应用", description = "完成一个综合项目来检验学习成果，形成完整作品集", status = "locked"),
            )
        )
        return ChatMessage(
            id = "mock-${UUID.randomUUID().toString().take(8)}",
            role = "assistant",
            content = "好的！我已经为你生成了个性化的学习路径 📚\n\n路径名称：**${lp.title}**\n\n这个学习路径包含 ${lp.nodes.size} 个阶段，从基础入门到综合应用，循序渐进地帮助你掌握所学内容。你可以按照节点顺序依次学习，每完成一个节点可以标记为已完成。\n\n祝你学习愉快！🎉",
            timestamp = System.currentTimeMillis(),
            learningPath = lp
        )
    }

    /**
     * 将生成的计划保存到本地存储
     */
    fun savePlanToLocal(conversationId: Long?, title: String) {
        val now = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val plan = PlanItem(
            id = conversationId ?: PlanLocalStorage.generateLocalId(),
            title = title,
            latestDate = now,
            createdDate = now,
            colorIndex = (conversationId?.toInt() ?: 0) % 5,
            conversationId = conversationId
        )
        PlanLocalStorage.savePlan(plan)
    }

    // ── 发送消息 ──
    fun sendMessage(query: String) {
        if (query.isBlank() || isLoading) return
        if (!isLoggedIn) {
            errorMessage = "请先登录后再创建学习计划"
            return
        }

        focusManager.clearFocus()

        // 添加用户消息
        val userMsg = ChatMessage(
            id = "user-${UUID.randomUUID().toString().take(8)}",
            role = "user",
            content = query,
            timestamp = System.currentTimeMillis()
        )
        messages = messages + userMsg

        // 添加加载占位
        val loadingMsg = ChatMessage(
            id = "loading-${UUID.randomUUID().toString().take(8)}",
            role = "assistant",
            content = "",
            isLoading = true,
            timestamp = System.currentTimeMillis()
        )
        messages = messages + loadingMsg

        inputText = ""
        isLoading = true
        loadingMessage = ""
        errorMessage = null

        coroutineScope.launch {
            try {
                val token = SessionManager.getAccessToken() ?: ""
                if (token.isEmpty()) {
                    errorMessage = "登录已过期，请重新登录"
                    isLoading = false
                    messages = messages.filter { !it.isLoading }
                    return@launch
                }

                val requestId = "chat-${UUID.randomUUID().toString().take(8)}"
                var convId = currentConversationId

                // 第一次发送消息需要创建会话
                if (convId == null) {
                    loadingMessage = "正在创建会话..."
                    val conversationResult = PlanApiService.createConversation(
                        token = token,
                        title = query.take(50),
                        requestId = requestId
                    )

                    conversationResult.fold(
                        onSuccess = { conversation ->
                            convId = conversation.id
                            currentConversationId = conversation.id
                        },
                        onFailure = { error ->
                            // API 不可用，使用模拟回复
                            messages = messages.filter { !it.isLoading }
                            val mockMsg = createMockResponse(query)
                            messages = messages + mockMsg

                            // 保存模拟生成的计划到本地
                            val planTitle = mockMsg.learningPath?.title ?: query.take(30)
                            savePlanToLocal(null, planTitle)

                            isLoading = false
                            return@launch
                        }
                    )
                }

                // 提交问题
                loadingMessage = "AI 正在思考..."
                val questionRequest = QuestionSubmitRequest(
                    conversation_id = convId!!,
                    content_text = query,
                    request_id = "$requestId-q"
                )

                val questionResult = PlanApiService.submitQuestion(token, questionRequest)
                val questionData = questionResult.getOrElse { error ->
                    messages = messages.filter { !it.isLoading }
                    val mockMsg = createMockResponse(query)
                    messages = messages + mockMsg

                    // 保存模拟生成的计划到本地
                    val planTitle = mockMsg.learningPath?.title ?: query.take(30)
                    savePlanToLocal(convId, planTitle)

                    isLoading = false
                    return@launch
                }

                // 轮询任务结果
                loadingMessage = "AI 正在生成学习路径..."
                val taskId = questionData.generation_task_id
                var retryCount = 0
                val maxRetries = 40
                var responseReady = false

                while (retryCount < maxRetries && !responseReady) {
                    delay(3000)
                    retryCount++
                    loadingMessage = "AI 正在生成学习路径... (${retryCount * 3}s)"

                    val taskResult = PlanApiService.getTaskResult(token, taskId)
                    if (taskResult.isFailure) {
                        if (retryCount >= maxRetries) {
                            responseReady = true
                        }
                        continue
                    }
                    val resultData = taskResult.getOrThrow()

                    if (resultData.answer_ready && resultData.answer_message != null) {
                        responseReady = true
                        val answerMsg = resultData.answer_message
                        val learningPath = parseLearningPath(answerMsg)

                        // 构造 AI 回复消息
                        val contentText = if (answerMsg.content_text.startsWith("{")) {
                            // content_text 是 JSON，提取 readable 文本
                            learningPath?.let { lp ->
                                "好的！我已经为你生成了个性化的学习路径 📚\n\n路径名称：**${lp.title}**\n\n这个学习路径包含 ${lp.nodes.size} 个阶段，从基础入门到综合应用。你可以按照节点顺序依次学习，每完成一个节点可以标记为已完成。\n\n祝你学习愉快！🎉"
                            } ?: "学习路径已生成，请查看下方详情 👇"
                        } else {
                            answerMsg.content_text
                        }

                        val assistantMsg = ChatMessage(
                            id = "ai-${answerMsg.id}",
                            role = "assistant",
                            content = contentText,
                            timestamp = System.currentTimeMillis(),
                            learningPath = learningPath
                        )

                        messages = messages.filter { !it.isLoading }
                        messages = messages + assistantMsg
                        isLoading = false

                        // 保存到本地存储
                        val planTitle = learningPath?.title ?: convId?.let { "学习计划 #$it" } ?: query.take(30)
                        savePlanToLocal(convId, planTitle)

                        return@launch
                    }

                    if (resultData.task.status == "failed") {
                        messages = messages.filter { !it.isLoading }
                        messages = messages + ChatMessage(
                            id = "err-${UUID.randomUUID().toString().take(8)}",
                            role = "assistant",
                            content = "😅 抱歉，生成学习路径时遇到了问题：${resultData.task.error_message ?: "未知错误"}\n\n请稍后再试，或者换一种描述方式。",
                            timestamp = System.currentTimeMillis()
                        )
                        isLoading = false
                        return@launch
                    }
                }

                // 超时或异常 → 使用模拟回复
                if (!responseReady) {
                    messages = messages.filter { !it.isLoading }
                    val mockMsg = createMockResponse(query)
                    messages = messages + mockMsg

                    // 保存模拟生成的计划到本地
                    val planTitle = mockMsg.learningPath?.title ?: query.take(30)
                    savePlanToLocal(currentConversationId, planTitle)
                }
                isLoading = false

            } catch (e: Exception) {
                messages = messages.filter { !it.isLoading }
                val mockMsg = createMockResponse(query)
                messages = messages + mockMsg

                // 保存模拟生成的计划到本地
                val planTitle = mockMsg.learningPath?.title ?: query.take(30)
                savePlanToLocal(currentConversationId, planTitle)

                isLoading = false
            }
        }
    }

    // ── 检查是否有已生成的学习路径（最后一条 AI 消息含 learningPath） ──
    val hasGeneratedPlan = remember { mutableStateOf(false) }
    LaunchedEffect(messages) {
        val lastMsg = messages.lastOrNull()
        hasGeneratedPlan.value = lastMsg != null
                && lastMsg.role == "assistant"
                && !lastMsg.isLoading
                && lastMsg.learningPath != null
    }

    // ── 整体布局 ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.app_bg))
    ) {
        // ── 顶部导航栏 ──
        ChatTopBar(onBack = onBack)

        // ── 消息列表 / 欢迎区域 ──
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                // 空状态：显示欢迎语和快捷入口
                WelcomeSection(
                    onTopicClick = { topic ->
                        sendMessage(topic)
                    }
                )
            } else {
                // 聊天消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageBubble(
                            message = message,
                            onRetry = { /* 待实现 */ }
                        )
                    }
                }
            }
        }

        // ── 学习路径生成完成：底部完成横幅 ──
        if (hasGeneratedPlan.value && !isLoading) {
            PlanCompletionBanner(
                conversationId = currentConversationId,
                onGoToPlans = { onBack() }
            )
        }

        // ── 错误提示 ──
        if (errorMessage != null) {
            ErrorBanner(
                message = errorMessage!!,
                onDismiss = { errorMessage = null }
            )
        }

        // ── 底部输入区域（有学习路径后允许继续追问） ──
        ChatInputBar(
            inputText = inputText,
            onInputChange = { inputText = it },
            isLoading = isLoading,
            loadingMessage = loadingMessage,
            onSend = { sendMessage(inputText) },
            enabled = isLoggedIn && !isLoading
        )
    }
}


// ============================================================
// 顶部导航栏
// ============================================================

@Composable
fun ChatTopBar(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorResource(R.color.surface_white),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_auth_back),
                    contentDescription = stringResource(R.string.auth_back),
                    tint = colorResource(R.color.text_primary),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "AI 创建学习计划",
                color = colorResource(R.color.text_primary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

// ============================================================
// 欢迎区域（空状态时显示）
// ============================================================

@Composable
fun WelcomeSection(
    onTopicClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // ── 标题 ──
        Text(
            text = "🤖 AI 帮你创建学习计划",
            color = colorResource(R.color.text_primary),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "输入你想学习的内容，AI 会为你生成个性化的学习路径。\n你可以和 AI 对话，逐步完善学习计划。",
            color = colorResource(R.color.text_secondary),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // ── 快捷入口 ──
        Text(
            text = "💡 试试这些方向",
            color = colorResource(R.color.text_primary),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 用两列网格展示热门话题
        HotTopics.chunked(2).forEach { rowTopics ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTopics.forEach { topic ->
                    SuggestionChip(
                        text = topic,
                        onClick = { onTopicClick(topic) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (rowTopics.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 登录提示 ──
        if (!SessionManager.isLoggedIn()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.brand_orange_soft)
                )
            ) {
                Text(
                    text = "⚠️ 请先登录后再创建学习计划",
                    color = colorResource(R.color.brand_orange),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// ============================================================
// 快捷建议标签
// ============================================================

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.surface_white)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, colorResource(R.color.divider_soft)
        )
    ) {
        Text(
            text = text,
            color = colorResource(R.color.text_primary),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            maxLines = 1
        )
    }
}

// ============================================================
// 底部输入栏
// ============================================================

@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    isLoading: Boolean,
    loadingMessage: String,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorResource(R.color.surface_white),
        shadowElevation = 4.dp
    ) {
        Column {
            // 加载状态提示条
            if (isLoading) {
                LoadingIndicatorBar(message = loadingMessage)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 文本输入框
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 120.dp),
                    placeholder = {
                        Text(
                            text = "输入你想学习的内容...",
                            color = colorResource(R.color.text_muted),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.brand_primary),
                        unfocusedBorderColor = colorResource(R.color.divider_soft),
                        cursorColor = colorResource(R.color.brand_primary)
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (enabled && inputText.isNotBlank()) onSend() }),
                    singleLine = false
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 发送按钮
                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled && inputText.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorResource(R.color.brand_primary),
                        disabledContainerColor = colorResource(R.color.divider_soft)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = "发送",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// 学习路径生成完成横幅
// ============================================================

@Composable
fun PlanCompletionBanner(
    conversationId: Long?,
    onGoToPlans: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorResource(R.color.brand_green_soft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎉 学习计划已生成",
                    color = colorResource(R.color.brand_green),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (conversationId != null) {
                    Text(
                        text = "会话 #$conversationId，可在计划列表中查看",
                        color = colorResource(R.color.brand_green).copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            FilledTonalButton(
                onClick = onGoToPlans,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colorResource(R.color.brand_green),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "查看计划",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================
// 加载指示条
// ============================================================

@Composable
fun LoadingIndicatorBar(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.brand_primary_soft))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = colorResource(R.color.brand_primary),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message.ifEmpty { "处理中..." },
            color = colorResource(R.color.brand_primary),
            fontSize = 12.sp
        )
    }
}

// ============================================================
// 错误横幅
// ============================================================

@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(20.dp)
            ) {
                Text("✕", fontSize = 12.sp, color = Color(0xFFD32F2F))
            }
        }
    }
}


// ============================================================
// 聊天消息气泡
// ============================================================

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val isUser = message.role == "user"
    val isAssistant = message.role == "assistant"
    val isBotLoading = message.isLoading

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isUser) {
            // ── 用户消息（右对齐） ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(
                            color = colorResource(R.color.brand_primary),
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 6.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 18.dp
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTime(message.timestamp),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        } else if (isBotLoading) {
            // ── AI 思考中（动画占位） ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = colorResource(R.color.surface_white),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(
                            1.dp, colorResource(R.color.divider_soft),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 打字动画
                    repeat(3) { index ->
                        val delay by rememberInfiniteTransition().animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 600, delayMillis = index * 200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(colorResource(R.color.brand_primary).copy(alpha = delay))
                        )
                        if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        } else if (isAssistant) {
            // ── AI 回复（左对齐） ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    // AI 头像 + 名称
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colorResource(R.color.brand_primary_soft)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AI",
                                color = colorResource(R.color.brand_primary),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI 学习助手",
                            color = colorResource(R.color.text_secondary),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 消息正文气泡
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 6.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(R.color.surface_white)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, colorResource(R.color.divider_soft)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // 解析 Markdown 风格的文本
                            val segments = parseMarkdownText(message.content)
                            segments.forEach { segment ->
                                when (segment.type) {
                                    TextSegmentType.BOLD -> {
                                        Text(
                                            text = segment.text,
                                            color = colorResource(R.color.text_primary),
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    TextSegmentType.NORMAL -> {
                                        if (segment.text.contains("\n")) {
                                            segment.text.split("\n").forEach { line ->
                                                Text(
                                                    text = line,
                                                    color = colorResource(R.color.text_primary),
                                                    fontSize = 15.sp,
                                                    lineHeight = 22.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        } else {
                                            Text(
                                                text = segment.text,
                                                color = colorResource(R.color.text_primary),
                                                fontSize = 15.sp,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatTime(message.timestamp),
                                color = colorResource(R.color.text_muted),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // ---- 如果包含学习路径，显示路径卡片 ----
                    message.learningPath?.let { lp ->
                        Spacer(modifier = Modifier.height(10.dp))
                        LearningPathChatCard(plan = lp)
                    }
                }
            }
        }
    }
}

// ============================================================
// 文本段类型
// ============================================================

private enum class TextSegmentType { NORMAL, BOLD }

private data class TextSegment(
    val text: String,
    val type: TextSegmentType = TextSegmentType.NORMAL
)

/**
 * 简单解析 Markdown 风格的 **粗体** 文本
 */
private fun parseMarkdownText(text: String): List<TextSegment> {
    val segments = mutableListOf<TextSegment>()
    var remaining = text
    val boldRegex = Regex("\\*\\*(.+?)\\*\\*")

    while (remaining.isNotEmpty()) {
        val match = boldRegex.find(remaining)
        if (match == null) {
            segments.add(TextSegment(remaining))
            break
        }

        // 粗体之前的普通文本
        if (match.range.first > 0) {
            segments.add(TextSegment(remaining.substring(0, match.range.first)))
        }

        // 粗体文本
        segments.add(TextSegment(match.groupValues[1], TextSegmentType.BOLD))

        // 剩余文本
        remaining = remaining.substring(match.range.last + 1)
    }

    return segments
}

// ============================================================
// 学习路径卡片（聊天内嵌）
// ============================================================

@Composable
fun LearningPathChatCard(
    plan: LearningPathMeta
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 计划标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorResource(R.color.brand_primary_soft)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📚",
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "学习路径已生成",
                        color = colorResource(R.color.brand_primary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = plan.title,
                        color = colorResource(R.color.text_primary),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 节点列表
            plan.nodes.forEachIndexed { index, node ->
                val statusColor = when (node.status) {
                    "done" -> colorResource(R.color.brand_green)
                    "in_progress", "available" -> colorResource(R.color.brand_primary)
                    else -> colorResource(R.color.text_muted)
                }
                val statusIcon = when (node.status) {
                    "done" -> "✅"
                    "in_progress" -> "🔄"
                    "available" -> "🔓"
                    else -> "🔒"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 序号
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.title,
                                color = colorResource(R.color.text_primary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusIcon,
                                fontSize = 12.sp
                            )
                        }
                        if (node.description.isNotBlank()) {
                            Text(
                                text = node.description,
                                color = colorResource(R.color.text_secondary),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* 查看详情待实现 */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, colorResource(R.color.brand_primary)
                    )
                ) {
                    Text(
                        text = "查看详情",
                        color = colorResource(R.color.brand_primary),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}


// ============================================================
// 工具函数：解析学习路径
// ============================================================

/**
 * 从 AnswerMessageData 的 meta_json / content_text 中解析学习路径
 */
private fun parseLearningPath(answerMsg: AnswerMessageData): LearningPathMeta? {
    return try {
        // 优先从 meta_json 中解析
        val metaJson = answerMsg.meta_json
        if (metaJson != null && metaJson.containsKey("learning_path")) {
            @Suppress("UNCHECKED_CAST")
            val lpMap = metaJson["learning_path"] as? Map<String, Any> ?: return null
            val title = lpMap["title"] as? String ?: ""
            val goal = lpMap["goal"] as? String ?: ""
            val nodesRaw = lpMap["nodes"] as? List<Map<String, Any>> ?: emptyList()
            val nodes = nodesRaw.mapIndexed { index, nodeMap ->
                LearningPathNodeMeta(
                    id = (nodeMap["id"] as? Number)?.toInt() ?: (index + 1),
                    title = nodeMap["title"] as? String ?: "节点 ${index + 1}",
                    description = nodeMap["description"] as? String ?: "",
                    status = nodeMap["status"] as? String ?: "locked"
                )
            }
            LearningPathMeta(title = title, goal = goal, nodes = nodes)
        } else {
            // 尝试从 content_text 中解析 JSON
            val content = answerMsg.content_text
            if (content.isNotBlank() && content.contains("learning_path")) {
                val gson = com.google.gson.Gson()
                val map = gson.fromJson(content, Map::class.java) as? Map<String, Any>
                val lpMap = map?.get("learning_path") as? Map<String, Any> ?: return null
                val title = lpMap["title"] as? String ?: ""
                val goal = lpMap["goal"] as? String ?: ""
                val nodesRaw = lpMap["nodes"] as? List<Map<String, Any>> ?: emptyList()
                val nodes = nodesRaw.mapIndexed { index, nodeMap ->
                    LearningPathNodeMeta(
                        id = (nodeMap["id"] as? Number)?.toInt() ?: (index + 1),
                        title = nodeMap["title"] as? String ?: "节点 ${index + 1}",
                        description = nodeMap["description"] as? String ?: "",
                        status = nodeMap["status"] as? String ?: "locked"
                    )
                }
                LearningPathMeta(title = title, goal = goal, nodes = nodes)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

// ============================================================
// 工具函数：格式化时间
// ============================================================

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreatePlanScreenPreview() {
    CreatePlanScreen(onBack = {})
}

@Preview(showBackground = true)
@Composable
fun ChatMessageBubbleUserPreview() {
    ChatMessageBubble(
        message = ChatMessage(
            id = "1",
            role = "user",
            content = "我想学习 Python 编程基础，请生成学习路径",
            timestamp = System.currentTimeMillis()
        ),
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ChatMessageBubbleAiPreview() {
    ChatMessageBubble(
        message = ChatMessage(
            id = "2",
            role = "assistant",
            content = "好的！我已经为你生成了个性化的学习路径 📚\n\n路径名称：**Python 入门路径**\n\n这个学习路径包含 4 个阶段。",
            timestamp = System.currentTimeMillis(),
            learningPath = LearningPathMeta(
                title = "Python 入门路径",
                goal = "我想学习 Python 编程基础",
                nodes = listOf(
                    LearningPathNodeMeta(id = 1, title = "Python 基础语法", description = "学习变量、数据类型、控制流等基础知识", status = "available"),
                    LearningPathNodeMeta(id = 2, title = "函数与模块", description = "掌握函数定义和模块导入", status = "locked"),
                    LearningPathNodeMeta(id = 3, title = "面向对象编程", description = "理解类、对象、继承和多态", status = "locked"),
                    LearningPathNodeMeta(id = 4, title = "综合项目实战", description = "完成一个 CLI 小项目", status = "locked"),
                )
            )
        ),
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeSectionPreview() {
    WelcomeSection(onTopicClick = {})
}

@Preview(showBackground = true)
@Composable
fun ChatInputBarPreview() {
    ChatInputBar(
        inputText = "",
        onInputChange = {},
        isLoading = false,
        loadingMessage = "",
        onSend = {},
        enabled = true
    )
}
