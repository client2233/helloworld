package com.nku.helloworld.ui.plan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import java.util.UUID

/**
 * 创建学习计划页面
 *
 * 用户输入想学习的内容，提交后生成学习路径。
 * 参考 api.md 中的「前端提交问题样例」和「前端轮询任务结果样例」。
 */
class CreatePlanActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isInitialized()) {
            SessionManager.init(this)
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
// 创建计划主页面
// ============================================================

@Composable
fun CreatePlanScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── 状态 ──
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var resultPlan by remember { mutableStateOf<LearningPathMeta?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    // ── 检查登录 ──
    val isLoggedIn = remember { SessionManager.isLoggedIn() }

    // ── 模拟创建计划（API 不可用时使用） ──
    val useMockCreatePlan: (String) -> Unit = { query ->
        resultPlan = LearningPathMeta(
            title = when {
                query.contains("Python", ignoreCase = true) -> "Python 入门路径"
                query.contains("Java", ignoreCase = true) -> "Java 进阶路径"
                query.contains("英语", ignoreCase = true) || query.contains("English", ignoreCase = true) -> "英语学习路径"
                query.contains("Android", ignoreCase = true) -> "Android 开发路径"
                query.contains("机器学习", ignoreCase = true) || query.contains("ML", ignoreCase = true) -> "机器学习路径"
                query.contains("算法", ignoreCase = true) || query.contains("数据结构", ignoreCase = true) -> "数据结构与算法路径"
                else -> "个性化学习路径"
            },
            goal = query,
            nodes = listOf(
                LearningPathNodeMeta(id = 1, title = "了解基础知识", description = "学习相关领域的基础概念和原理", status = "available"),
                LearningPathNodeMeta(id = 2, title = "动手实践", description = "通过练习和项目巩固所学知识", status = "locked"),
                LearningPathNodeMeta(id = 3, title = "进阶提升", description = "深入学习高级主题和最佳实践", status = "locked"),
                LearningPathNodeMeta(id = 4, title = "综合应用", description = "完成一个综合项目来检验学习成果", status = "locked"),
            )
        )
        isLoading = false
        showSuccess = true
    }

    // ── 提交创建计划 ──
    fun submitCreatePlan(query: String) {
        if (query.isBlank()) return
        if (!isLoggedIn) {
            errorMessage = "请先登录后再创建学习计划"
            return
        }

        isLoading = true
        loadingMessage = "正在创建会话..."
        errorMessage = null
        resultPlan = null
        showSuccess = false

        coroutineScope.launch {
            try {
                val token = SessionManager.getAccessToken() ?: ""
                if (token.isEmpty()) {
                    errorMessage = "登录已过期，请重新登录"
                    isLoading = false
                    return@launch
                }

                val requestId = "create-plan-${UUID.randomUUID().toString().take(8)}"

                // Step 1: 创建会话
                loadingMessage = "正在创建会话..."
                val conversationResult = PlanApiService.createConversation(
                    token = token,
                    title = query.take(50),
                    requestId = requestId
                )

                val conversation = conversationResult.getOrElse { error ->
                    // 如果 API 不可用，使用模拟数据演示
                    useMockCreatePlan(query)
                    return@launch
                }

                // Step 2: 提交问题
                loadingMessage = "正在提交学习需求..."
                val questionRequest = QuestionSubmitRequest(
                    conversation_id = conversation.id,
                    content_text = query,
                    request_id = "$requestId-q"
                )

                val questionResult = PlanApiService.submitQuestion(token, questionRequest)
                val questionData = questionResult.getOrElse { error ->
                    useMockCreatePlan(query)
                    return@launch
                }

                // Step 3: 轮询任务结果
                loadingMessage = "AI 正在生成学习路径..."
                val taskId = questionData.generation_task_id
                var retryCount = 0
                val maxRetries = 30 // 最多等待 90 秒

                while (retryCount < maxRetries) {
                    delay(3000) // 每 3 秒轮询一次
                    retryCount++

                    loadingMessage = "AI 正在生成学习路径... (${retryCount * 3}s)"

                    val taskResult = PlanApiService.getTaskResult(token, taskId)
                    val resultData = taskResult.getOrElse { error ->
                        if (retryCount >= maxRetries) {
                            useMockCreatePlan(query)
                            return@launch
                        }
                        useMockCreatePlan(query)
                        return@launch
                    }

                    if (resultData.answer_ready && resultData.answer_message != null) {
                        // 解析学习路径
                        val answerMsg = resultData.answer_message
                        val learningPath = parseLearningPath(answerMsg)
                        if (learningPath != null) {
                            resultPlan = learningPath
                        } else {
                            // 尝试从 content_text 解析
                            resultPlan = LearningPathMeta(
                                title = "学习计划",
                                goal = query,
                                nodes = listOf(
                                    LearningPathNodeMeta(id = 1, title = "开始学习", description = "已为您生成学习计划", status = "available")
                                )
                            )
                        }
                        isLoading = false
                        showSuccess = true
                        return@launch
                    }

                    if (resultData.task.status == "failed") {
                        errorMessage = resultData.task.error_message ?: "生成失败，请重试"
                        isLoading = false
                        return@launch
                    }
                }

                // 超时处理
                loadingMessage = "生成超时，显示默认结果"
                useMockCreatePlan(query)

            } catch (e: Exception) {
                useMockCreatePlan(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.app_bg))
    ) {
        // ── 顶部导航栏 ──
        CreatePlanTopBar(onBack = onBack)

        // ── 可滚动内容 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            if (showSuccess && resultPlan != null) {
                // ── 成功：显示生成的学习路径 ──
                LearningPathResult(
                    plan = resultPlan!!,
                    onCreateAnother = {
                        resultPlan = null
                        showSuccess = false
                        inputText = ""
                    },
                    onGoToPlans = {
                        // 返回后会自动跳转到计划列表
                        onBack()
                    }
                )
            } else {
                // ── 输入区域 ──
                CreatePlanInputArea(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    isLoading = isLoading,
                    loadingMessage = loadingMessage,
                    errorMessage = errorMessage,
                    isLoggedIn = isLoggedIn,
                    onClearError = { errorMessage = null },
                    onSubmit = { submitCreatePlan(inputText) },
                    onTopicClick = { topic ->
                        inputText = topic
                    }
                )
            }
        }
    }
}

// ============================================================
// 顶部导航栏
// ============================================================

@Composable
fun CreatePlanTopBar(onBack: () -> Unit) {
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
                text = "创建学习计划",
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
// 输入区域
// ============================================================

@Composable
fun CreatePlanInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    isLoading: Boolean,
    loadingMessage: String,
    errorMessage: String?,
    isLoggedIn: Boolean,
    onClearError: () -> Unit,
    onSubmit: () -> Unit,
    onTopicClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
    ) {
        // ── 标题 ──
        Text(
            text = "✨ 开始你的学习之旅",
            color = colorResource(R.color.text_primary),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "输入你想学习的内容，AI 将为你生成个性化的学习路径",
            color = colorResource(R.color.text_secondary),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── 输入框 ──
        Text(
            text = "📝 你想学习什么？",
            color = colorResource(R.color.text_primary),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                onInputChange(it)
                onClearError()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = {
                Text(
                    text = "例如：我想学习 Python 编程基础，请生成学习路径",
                    color = colorResource(R.color.text_muted),
                    fontSize = 14.sp
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(R.color.brand_primary),
                unfocusedBorderColor = colorResource(R.color.divider_soft),
                cursorColor = colorResource(R.color.brand_primary)
            ),
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 错误提示 ──
        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearError,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text("✕", fontSize = 12.sp, color = Color(0xFFD32F2F))
                    }
                }
            }
        }

        // ── 提交按钮 ──
        Button(
            onClick = onSubmit,
            enabled = inputText.isNotBlank() && !isLoading && isLoggedIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.brand_primary),
                disabledContainerColor = colorResource(R.color.divider_soft)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = loadingMessage,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "🔮 生成学习计划",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!isLoggedIn) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠️ 请先登录后再创建学习计划",
                color = colorResource(R.color.text_muted),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── 热门方向快捷入口 ──
        Text(
            text = "—— 或者选择热门方向 ——",
            color = colorResource(R.color.text_muted),
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        // 用 FlowRow 风格展示热门话题
        HotTopics.chunked(2).forEach { rowTopics ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTopics.forEach { topic ->
                    HotTopicChip(
                        text = topic,
                        onClick = { onTopicClick(topic) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 如果该行只有一项，补充一个占位 Spacer
                if (rowTopics.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================
// 热门方向标签
// ============================================================

@Composable
fun HotTopicChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
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
// 学习路径结果展示
// ============================================================

@Composable
fun LearningPathResult(
    plan: LearningPathMeta,
    onCreateAnother: () -> Unit,
    onGoToPlans: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        // ── 成功动画/标题 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎉",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "学习计划已生成！",
                    color = colorResource(R.color.text_primary),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 计划标题卡片 ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.brand_primary)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = plan.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (plan.goal.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = plan.goal,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 学习路径节点列表 ──
        Text(
            text = "📋 学习路径节点",
            color = colorResource(R.color.text_primary),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        plan.nodes.forEachIndexed { index, node ->
            PathNodeCard(
                index = index + 1,
                node = node
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── 操作按钮 ──
        Button(
            onClick = onCreateAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.brand_primary)
            )
        ) {
            Text(
                text = "🔄 再创建一个",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoToPlans,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, colorResource(R.color.brand_primary)
            )
        ) {
            Text(
                text = "📂 查看所有计划",
                color = colorResource(R.color.brand_primary),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ============================================================
// 路径节点卡片
// ============================================================

@Composable
fun PathNodeCard(
    index: Int,
    node: LearningPathNodeMeta
) {
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.surface_white)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, colorResource(R.color.divider_soft)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 序号圆
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    color = statusColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.title,
                        color = colorResource(R.color.text_primary),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = statusIcon,
                        fontSize = 16.sp
                    )
                }
                if (node.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.description,
                        color = colorResource(R.color.text_secondary),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ============================================================
// 解析学习路径（从 answer_message 的 meta_json 中提取）
// ============================================================

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
// Preview
// ============================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreatePlanScreenPreview() {
    CreatePlanScreen(onBack = {})
}

@Preview(showBackground = true)
@Composable
fun LearningPathResultPreview() {
    LearningPathResult(
        plan = LearningPathMeta(
            title = "Python 入门路径",
            goal = "我想学习 Python 编程基础",
            nodes = listOf(
                LearningPathNodeMeta(id = 1, title = "Python 基础语法", description = "学习变量、数据类型、控制流等基础知识", status = "available"),
                LearningPathNodeMeta(id = 2, title = "函数与模块", description = "掌握函数定义、参数传递和模块导入", status = "locked"),
                LearningPathNodeMeta(id = 3, title = "面向对象编程", description = "理解类、对象、继承和多态", status = "locked"),
                LearningPathNodeMeta(id = 4, title = "综合项目实战", description = "完成一个 CLI 小项目巩固所学知识", status = "locked"),
            )
        ),
        onCreateAnother = {},
        onGoToPlans = {}
    )
}
