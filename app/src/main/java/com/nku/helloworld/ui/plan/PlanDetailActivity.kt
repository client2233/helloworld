package com.nku.helloworld.ui.plan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

/**
 * 学习路径详情页
 *
 * 根据 services.md：
 * - GET /api/v1/learning-paths/conversations/{conversation_id}/current  获取当前学习路径
 * - GET /api/v1/learning-paths/{path_id}         获取学习路径详情
 * - PATCH /api/v1/learning-paths/{path_id}/nodes/{node_id}/state  更新节点状态
 */
class PlanDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_PATH_ID = "path_id"
        const val EXTRA_PLAN_TITLE = "plan_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isInitialized()) {
            SessionManager.init(this)
        }

        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L)
        val pathId = intent.getLongExtra(EXTRA_PATH_ID, -1L)
        val planTitle = intent.getStringExtra(EXTRA_PLAN_TITLE) ?: "学习路径"

        setContent {
            PlanDetailScreen(
                conversationId = if (conversationId > 0) conversationId else null,
                pathId = if (pathId > 0) pathId else null,
                planTitle = planTitle,
                onBack = { finish() }
            )
        }
    }
}

// ============================================================
// 学习路径详情页主页面
// ============================================================

@Composable
fun PlanDetailScreen(
    conversationId: Long?,
    pathId: Long?,
    planTitle: String,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // ── 状态 ──
    var learningPath by remember { mutableStateOf<LearningPath?>(null) }
    var currentPathId by remember { mutableStateOf<Long?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nodeStates by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var retryTrigger by remember { mutableStateOf(0) }

    // ── 将 API 返回的 LearningNodeOut 转换为 PathNode ──
    fun toPathNodes(apiNodes: List<LearningNodeOut>?): List<PathNode> {
        return apiNodes?.map { node ->
            PathNode(
                id = node.id ?: 0L,
                title = node.title ?: "",
                status = node.userState?.state ?: "locked",
                question = node.description
            )
        } ?: emptyList()
    }

    // ── 加载学习路径 ──
    // 流程：conversationId → getCurrentLearningPathV2 → 拿 pathId → 后续操作
    // 如果只有 pathId 则直接用 getLearningPathDetailV2 兜底
    LaunchedEffect(conversationId, pathId, retryTrigger) {
        val token = SessionManager.getAccessToken() ?: ""
        if (token.isEmpty()) {
            errorMessage = "请先登录"
            isLoading = false
            return@LaunchedEffect
        }

        try {
            // 优先通过 conversationId 获取当前学习路径（包含 pathId）
            val result = if (conversationId != null) {
                PlanApiService.getCurrentLearningPathV2(token, conversationId)
            } else if (pathId != null) {
                PlanApiService.getLearningPathDetailV2(token, pathId)
            } else {
                Result.failure(Exception("缺少会话 ID"))
            }

            result.fold(
                onSuccess = { detailData ->
                    val pathOut = detailData.path
                    val resolvedPathId = pathOut?.id
                    currentPathId = resolvedPathId

                    val convertedNodes = toPathNodes(detailData.nodes)
                    learningPath = LearningPath(
                        id = resolvedPathId ?: 0L,
                        title = pathOut?.title ?: planTitle,
                        goal = pathOut?.goal ?: "",
                        nodes = convertedNodes,
                        created_at = pathOut?.createdAt
                    )
                    nodeStates = convertedNodes.associate { it.id to it.status }
                    isLoading = false
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "获取学习路径失败"
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "加载失败"
            isLoading = false
        }
    }

    // ── 更新节点状态 ──
    fun updateNodeState(nodeId: Long, newState: String) {
        val token = SessionManager.getAccessToken() ?: return
        val pid = currentPathId ?: return

        // 乐观更新 UI
        nodeStates = nodeStates + (nodeId to newState)

        coroutineScope.launch {
            try {
                PlanApiService.updateNodeState(
                    token = token,
                    pathId = pid,
                    nodeId = nodeId,
                    newState = newState
                )
            } catch (e: Exception) {
                // 失败时回滚
                nodeStates = nodeStates + (nodeId to (learningPath?.nodes?.find { it.id == nodeId }?.status ?: "locked"))
            }
        }
    }

    // ── 布局 ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.app_bg))
    ) {
        // ── 顶部导航栏 ──
        DetailTopBar(
            title = planTitle,
            onBack = onBack
        )

        // ── 内容 ──
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(R.color.brand_primary))
            }
        } else if (errorMessage != null && learningPath == null) {
            // 错误状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "😵",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "加载失败",
                        color = colorResource(R.color.text_primary),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "未知错误",
                        color = colorResource(R.color.text_secondary),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FilledTonalButton(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            retryTrigger++
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("重试", fontSize = 15.sp)
                    }
                }
            }
        } else {
            learningPath?.let { path ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── 标题卡片 ──
                    item {
                        DetailHeaderCard(path = path)
                    }

                    // ── 进度概览 ──
                    item {
                        ProgressOverview(
                            nodes = path.nodes,
                            nodeStates = nodeStates
                        )
                    }

                    // ── 节点列表标题 ──
                    item {
                        Text(
                            text = "📋 学习节点 (${path.nodes.size})",
                            color = colorResource(R.color.text_primary),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // ── 节点列表 ──
                    items(
                        items = path.nodes,
                        key = { it.id }
                    ) { node ->
                        val nodeIndex = path.nodes.indexOf(node)
                        DetailNodeCard(
                            node = node,
                            index = nodeIndex,
                            currentState = nodeStates[node.id] ?: node.status,
                            onStateChange = { newState ->
                                updateNodeState(node.id, newState)
                            }
                        )
                    }

                    // 底部留白
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// ============================================================
// 顶部导航栏
// ============================================================

@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit
) {
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
                text = title,
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
// 标题卡片
// ============================================================

@Composable
fun DetailHeaderCard(path: LearningPath) {
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
                text = "📚 学习路径",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = path.title.ifBlank { "未命名路径" },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (path.goal.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = path.goal,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            if (path.created_at != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "创建于 ${path.created_at.take(10)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ============================================================
// 进度概览
// ============================================================

@Composable
fun ProgressOverview(
    nodes: List<PathNode>,
    nodeStates: Map<Long, String>
) {
    val total = nodes.size
    val doneCount = nodes.count { nodeStates[it.id] == "done" }
    val progress = if (total > 0) doneCount.toFloat() / total else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.surface_white)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 学习进度",
                    color = colorResource(R.color.text_primary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$doneCount / $total",
                    color = colorResource(R.color.brand_primary),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colorResource(R.color.brand_green),
                trackColor = colorResource(R.color.divider_soft)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(progress * 100).toInt()}% 已完成",
                color = colorResource(R.color.text_muted),
                fontSize = 12.sp
            )
        }
    }
}

// ============================================================
// 节点详情卡片
// ============================================================

@Composable
fun DetailNodeCard(
    node: PathNode,
    index: Int,
    currentState: String,
    onStateChange: (String) -> Unit
) {
    val statusColor = when (currentState) {
        "done" -> colorResource(R.color.brand_green)
        "in_progress" -> colorResource(R.color.brand_orange)
        "available" -> colorResource(R.color.brand_primary)
        else -> colorResource(R.color.text_muted)
    }
    val statusText = when (currentState) {
        "done" -> "已完成"
        "in_progress" -> "进行中"
        "available" -> "未开始"
        else -> "未解锁"
    }
    val statusBg = when (currentState) {
        "done" -> colorResource(R.color.brand_green_soft)
        "in_progress" -> colorResource(R.color.brand_orange_soft)
        "available" -> colorResource(R.color.brand_primary_soft)
        else -> Color(0xFFF5F5F5)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.surface_white)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, statusColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 序号圆
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = node.title,
                    color = colorResource(R.color.text_primary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // 状态标签
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 问题/描述
            if (!node.question.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = node.question,
                    color = colorResource(R.color.text_secondary),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // 操作按钮（仅 available 和 in_progress 状态可操作）
            if (currentState == "available" || currentState == "in_progress") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentState == "available") {
                        OutlinedButton(
                            onClick = { onStateChange("in_progress") },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("开始学习", fontSize = 13.sp)
                        }
                    }
                    if (currentState == "in_progress") {
                        Button(
                            onClick = { onStateChange("done") },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.brand_green)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("标记完成", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlanDetailScreenPreview() {
    PlanDetailScreen(
        conversationId = 1,
        pathId = null,
        planTitle = "Python 学习路径",
        onBack = {}
    )
}
