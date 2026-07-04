package com.nku.helloworld.ui.plan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import android.util.Log
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val context = LocalContext.current

    // ── Debug: 写入文件 + Logcat ──
    fun debugLog(tag: String, msg: String) {
        Log.e(tag, msg)
        try {
            val file = java.io.File(context.filesDir, "plan_debug.log")
            file.appendText("${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())} [$tag] $msg\n")
        } catch (_: Exception) {}
    }
    debugLog("PlanDetail", "进入详情页 — conversationId=$conversationId, pathId=$pathId, title=$planTitle")

    // ── 状态 ──
    var learningPath by remember { mutableStateOf<LearningPath?>(null) }
    var currentPathId by remember { mutableStateOf<Long?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nodeStates by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var retryTrigger by remember { mutableStateOf(0) }
    /** 标记是否使用 QA 树作为数据源（而非 learning-paths） */
    var isUsingQaTree by remember { mutableStateOf(false) }
    /** QA 树原始数据（用于展开/收回控制） */
    var qaTreeData by remember { mutableStateOf<QaTreeData?>(null) }
    /** 已展开的节点 ID 集合 */
    var expandedNodeIds by remember { mutableStateOf(setOf<Long>()) }

    // ── 可见树节点数据类 ──
    data class VisibleTreeNode(
        val node: QaNodeData,
        val depth: Int,
        val isLastSibling: Boolean,
        val ancestorHasMoreSiblings: List<Boolean>,
        val hasChildren: Boolean,
        val isExpanded: Boolean
    )

    /** 获取 QaTreeNode 的节点 ID（用 node.id 或 hashCode 回退） */
    fun nodeIdOf(tn: QaTreeNode): Long = tn.node?.id ?: tn.hashCode().toLong()

    /**
     * 递归扁平化可见节点（仅展开的子节点可见）
     * 按目录树结构遍历，保留展开/收回状态。
     * 不可见节点（用户追问）会被跳过，但其 planned 子节点会穿透到同层级显示。
     */
    fun flattenVisibleNodes(
        roots: List<QaTreeNode>,
        expandedIds: Set<Long>,
        depth: Int = 0,
        ancestorHasMore: List<Boolean> = emptyList()
    ): List<VisibleTreeNode> {
        val result = mutableListOf<VisibleTreeNode>()

        // 判断节点是否可显示（planned 或有具体类型的 done 学习节点）
        fun isDisplayable(tn: QaTreeNode): Boolean {
            val s = tn.node?.status
            val t = tn.node?.nodeType
            return s == "planned" || (s == "done" && !t.isNullOrBlank())
        }

        // 统计当前列表中在第 i 个元素之后还有几个可显示节点
        fun countAfter(list: List<QaTreeNode>, start: Int): Int {
            var c = 0
            for (j in start + 1 until list.size) {
                if (isDisplayable(list[j])) c++
            }
            return c
        }

        // 判断节点是否有任何可见后代（穿透不可见层）
        fun hasAnyVisibleDescendant(tn: QaTreeNode): Boolean {
            return tn.children?.any { child ->
                isDisplayable(child) || hasAnyVisibleDescendant(child)
            } ?: false
        }

        // 遍历全部节点，不可见节点穿透但不显示
        for (i in roots.indices) {
            val tn = roots[i]
            val node = tn.node ?: continue
            val children = tn.children ?: emptyList()
            val isLastVisible = countAfter(roots, i) == 0

            if (isDisplayable(tn)) {
                // 可见节点：正常加入结果
                val nid = nodeIdOf(tn)
                val hasChildren = hasAnyVisibleDescendant(tn)
                val isExpanded = expandedIds.contains(nid)
                result.add(VisibleTreeNode(node, depth, isLastVisible, ancestorHasMore, hasChildren, isExpanded))

                if (isExpanded) {
                    // 递归处理所有子节点（含不可见穿透）
                    result.addAll(flattenVisibleNodes(children, expandedIds, depth + 1, ancestorHasMore + !isLastVisible))
                }
            } else {
                // 不可见节点（用户追问）：不显示，但穿透其子节点到同一层级
                result.addAll(flattenVisibleNodes(children, expandedIds, depth, ancestorHasMore))
            }
        }
        return result
    }

    /** 收集树中所有有可见子节点的节点 ID（用于展开全部，穿透不可见节点） */
    fun collectAllParentIds(nodes: List<QaTreeNode>): Set<Long> {
        val ids = mutableSetOf<Long>()
        fun isDisplayable(tn: QaTreeNode): Boolean {
            val s = tn.node?.status
            val t = tn.node?.nodeType
            return s == "planned" || (s == "done" && !t.isNullOrBlank())
        }
        fun hasDisplayableDescendant(tn: QaTreeNode): Boolean {
            return tn.children?.any { child ->
                isDisplayable(child) || hasDisplayableDescendant(child)
            } ?: false
        }
        fun walk(list: List<QaTreeNode>) {
            for (tn in list) {
                if (isDisplayable(tn) && hasDisplayableDescendant(tn)) {
                    ids.add(nodeIdOf(tn))
                }
                walk(tn.children ?: emptyList())
            }
        }
        walk(nodes)
        return ids
    }

    /** 切换节点展开/收回 */
    fun toggleExpand(nodeId: Long) {
        expandedNodeIds = if (expandedNodeIds.contains(nodeId)) {
            expandedNodeIds - nodeId
        } else {
            expandedNodeIds + nodeId
        }
    }

    /** 展开全部节点 */
    fun expandAll() {
        qaTreeData?.let { tree ->
            val roots = tree.nodes ?: emptyList()
            val topNodes = roots.flatMap { it.children ?: emptyList() }
            expandedNodeIds = collectAllParentIds(topNodes)
        }
    }

    /** 全部收回 */
    fun collapseAll() {
        expandedNodeIds = emptySet()
    }

    // ── 将 API 返回的 LearningNodeOut 转换为 PathNode（learning-paths 备用） ──
    fun toPathNodes(apiNodes: List<LearningNodeOut>?): List<PathNode> {
        return apiNodes?.map { node ->
            PathNode(
                id = node.id ?: 0L,
                title = node.title ?: "",
                status = node.userState?.state ?: "locked",
                question = node.description ?: node.contentJson?.get("question") as? String,
                nodeType = node.nodeType ?: "",
                estMinutes = (node.estMinutes ?: 0).toInt()
            )
        } ?: emptyList()
    }

    // ── 从 QA 树中提取根节点标题 ──
    fun extractTitleFromQaTree(qaTree: QaTreeData, defaultTitle: String): String {
        val rootNode = qaTree.nodes?.firstOrNull()?.node
        return rootNode?.title ?: defaultTitle
    }

    // ── 从 QA 树中提取根节点 question_text 作为 goal ──
    fun extractGoalFromQaTree(qaTree: QaTreeData): String {
        val rootNode = qaTree.nodes?.firstOrNull()?.node
        return rootNode?.questionText ?: rootNode?.summaryText ?: ""
    }

    /**
     * 在标记完成时更新本地 plan 的最后活动日期
     */
    fun updateLocalPlanDateOnDone(nodeId: Long, newState: String) {
        if (newState == "done") {
            val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val plan = PlanItem(
                id = conversationId ?: 0L,
                title = learningPath?.title ?: planTitle,
                latestDate = now,
                createdDate = now,
                progress = 0f,
                conversationId = conversationId
            )
            PlanLocalStorage.savePlan(plan)
        }
    }

    /**
     * 回退到 learning-paths API
     */
    fun fallbackToLearningPath(
        token: String,
        convId: Long?,
        pId: Long?,
        defaultTitle: String
    ) {
        coroutineScope.launch {
            try {
                val result = if (convId != null) {
                    PlanApiService.getCurrentLearningPathV2(token, convId)
                } else if (pId != null) {
                    PlanApiService.getLearningPathDetailV2(token, pId)
                } else {
                    Result.failure(Exception("缺少 ID"))
                }

                result.fold(
                    onSuccess = { detailData ->
                        val pathOut = detailData.path
                        val resolvedPathId = pathOut?.id
                        currentPathId = resolvedPathId
                        val convertedNodes = toPathNodes(detailData.nodes)
                        learningPath = LearningPath(
                            id = resolvedPathId ?: 0L,
                            title = pathOut?.title ?: defaultTitle,
                            goal = pathOut?.goal ?: "",
                            nodes = convertedNodes,
                            created_at = pathOut?.createdAt
                        )
                        nodeStates = convertedNodes.associate { it.id to it.status }
                        isLoading = false
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "获取学习数据失败"
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "加载失败"
                isLoading = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ── 加载学习路径（仅一次 LaunchedEffect） ──
    // 流程：优先使用 QA 树 API 获取完整树形结构，learning-paths API 作为备选
    // ═══════════════════════════════════════════════════════════
    LaunchedEffect(conversationId, pathId, retryTrigger) {
        debugLog("PlanDetail", "LaunchedEffect 触发 — conversationId=$conversationId, pathId=$pathId")
        val token = SessionManager.getAccessToken() ?: ""
        debugLog("PlanDetail", "token 长度=${token.length}, 是否为空=${token.isEmpty()}")
        if (token.isEmpty()) {
            debugLog("PlanDetail", "token 为空，停止加载")
            errorMessage = "请先登录"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        isUsingQaTree = false
        qaTreeData = null

        debugLog("PlanDetail", "conversationId=$conversationId, 是否走QA树=${conversationId != null}")
        if (conversationId != null) {
            debugLog("PlanDetail", "开始调用 getQaTree, conversationId=$conversationId")
            // 优先使用 QA 树 API — 包含完整的树形层级
            try {
                val qaResult = PlanApiService.getQaTree(token, conversationId)
                qaResult.fold(
                    onSuccess = { qaTree ->
                        debugLog("PlanDetail", "getQaTree 成功, nodes count=${qaTree.nodes?.size ?: 0}")
                        isUsingQaTree = true
                        qaTreeData = qaTree
                        // 跳过根节点（原始问题），从子节点开始展示
                        val roots = qaTree.nodes ?: emptyList()
                        val topNodes = roots.flatMap { it.children ?: emptyList() }
                        // 初始展开：所有节点默认展开
                        val initialExpanded = collectAllParentIds(topNodes)
                        expandedNodeIds = initialExpanded

                        // 构建 learningPath 用于标题/进度展示
                        val title = extractTitleFromQaTree(qaTree, planTitle)
                        val goal = extractGoalFromQaTree(qaTree)
                        val visibleNodes = flattenVisibleNodes(topNodes, initialExpanded)
                        val pathNodes = visibleNodes.map { vn ->
                            PathNode(
                                id = vn.node.id ?: 0L,
                                title = vn.node.title ?: "节点",
                                status = if (vn.node.status == "done") "done" else "available",
                                question = vn.node.description ?: vn.node.questionText ?: "",
                                nodeType = vn.node.nodeType ?: "",
                                estMinutes = vn.node.estMinutes ?: 0,
                                depth = vn.depth
                            )
                        }
                        learningPath = LearningPath(
                            id = conversationId,
                            title = title,
                            goal = goal,
                            nodes = pathNodes,
                            created_at = null
                        )
                        nodeStates = pathNodes.associate { it.id to it.status }
                        // 合并本地已保存的完成状态
                        val savedCompleted = PlanLocalStorage.loadCompletedNodes(conversationId)
                        if (savedCompleted.isNotEmpty()) {
                            nodeStates = nodeStates + savedCompleted.associate { it to "done" }
                        }
                        isLoading = false
                    },
                    onFailure = { qaError ->
                        debugLog("PlanDetail", "getQaTree 失败: ${qaError.message}, 回退到 learning-paths")
                        fallbackToLearningPath(token, conversationId, null, planTitle)
                    }
                )
            } catch (e: Exception) {
                fallbackToLearningPath(token, conversationId, null, planTitle)
            }
        } else if (pathId != null) {
            // 只有 pathId 时使用 learning-paths API
            fallbackToLearningPath(token, null, pathId, planTitle)
        } else {
            errorMessage = "缺少会话 ID"
            isLoading = false
        }
    }

    /**
     * 标记完成时自动解锁下一个节点（本地状态）
     */
    fun autoUnlockNextNode(currentNodeId: Long) {
        val nodes = learningPath?.nodes ?: return
        val currentIndex = nodes.indexOfFirst { it.id == currentNodeId }
        if (currentIndex >= 0 && currentIndex + 1 < nodes.size) {
            val nextNode = nodes[currentIndex + 1]
            val nextNodeCurrentState = nodeStates[nextNode.id] ?: nextNode.status
            if (nextNodeCurrentState == "locked") {
                nodeStates = nodeStates + (nextNode.id to "available")

                // 非 QA 树模式时同步调用 API 更新服务器状态
                if (!isUsingQaTree) {
                    val token = SessionManager.getAccessToken() ?: return
                    val pid = currentPathId ?: return
                    coroutineScope.launch {
                        try {
                            PlanApiService.updateNodeState(
                                token = token,
                                pathId = pid,
                                nodeId = nextNode.id,
                                newState = "available"
                            )
                        } catch (e: Exception) {
                            nodeStates = nodeStates + (nextNode.id to "locked")
                        }
                    }
                }
            }
        }
    }

    // ── 更新节点状态 ──
    fun updateNodeState(nodeId: Long, newState: String) {
        // 乐观更新当前节点 UI（本地始终更新）
        nodeStates = nodeStates + (nodeId to newState)

        // 如果当前使用 QA 树数据，保存完成状态到本地
        if (isUsingQaTree) {
            if (newState == "done") {
                val completedIds = nodeStates.filter { it.value == "done" }.keys
                conversationId?.let { PlanLocalStorage.saveCompletedNodes(it, completedIds) }
            }
            updateLocalPlanDateOnDone(nodeId, newState)
            return
        }

        val token = SessionManager.getAccessToken() ?: return
        val pid = currentPathId ?: return

        coroutineScope.launch {
            try {
                PlanApiService.updateNodeState(
                    token = token,
                    pathId = pid,
                    nodeId = nodeId,
                    newState = newState
                )
                updateLocalPlanDateOnDone(nodeId, newState)
            } catch (e: Exception) {
                // 失败时回滚
                nodeStates = nodeStates + (nodeId to (learningPath?.nodes?.find { it.id == nodeId }?.status ?: "locked"))
            }
        }

        // 如果标记为完成，自动解锁下一个节点
        if (newState == "done") {
            autoUnlockNextNode(nodeId)
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
                // ── 预先计算 QA 树的可见节点（避免在 LazyColumn 内调用 remember） ──
                val visibleNodes = remember(expandedNodeIds, qaTreeData) {
                    if (isUsingQaTree && qaTreeData != null) {
                        val roots = qaTreeData!!.nodes ?: emptyList()
                        val topNodes = roots.flatMap { it.children ?: emptyList() }
                        flattenVisibleNodes(topNodes, expandedNodeIds)
                    } else {
                        emptyList()
                    }
                }

                // ── 级联完成：节点→子节点映射 ──
                val nodeChildrenMap = remember(qaTreeData) {
                    val map = mutableMapOf<Long, List<Long>>()
                    fun build(tn: QaTreeNode) {
                        val pid = tn.node?.id ?: return
                        val childIds = tn.children?.mapNotNull { it.node?.id } ?: emptyList()
                        if (childIds.isNotEmpty()) map[pid] = childIds
                        tn.children?.forEach { build(it) }
                    }
                    qaTreeData?.nodes?.forEach { build(it) }
                    map
                }
                fun isAllChildrenDone(nodeId: Long): Boolean {
                    val childIds = nodeChildrenMap[nodeId] ?: return true
                    if (childIds.isEmpty()) return true
                    return childIds.all { cid ->
                        val state = nodeStates[cid] ?: "available"
                        state == "done" && isAllChildrenDone(cid)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── 标题卡片 ──
                    item {
                        DetailHeaderCard(
                            path = path,
                            subtitle = if (isUsingQaTree) "🌳 问答树" else "📚 学习路径"
                        )
                    }

                    // ── 进度概览 ──
                    item {
                        ProgressOverview(
                            nodes = path.nodes,
                            nodeStates = nodeStates
                        )
                    }

                    // ── 节点列表标题 + 展开/收回工具栏（仅 QA 树模式） ──
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 学习节点 (${visibleNodes.size})",
                                color = colorResource(R.color.text_primary),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isUsingQaTree) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // 展开全部
                                    TextButton(
                                        onClick = { expandAll() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("📂 展开全部", fontSize = 12.sp, color = colorResource(R.color.brand_primary))
                                    }
                                    // 全部收回
                                    TextButton(
                                        onClick = { collapseAll() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("📁 收起全部", fontSize = 12.sp, color = colorResource(R.color.text_muted))
                                    }
                                }
                            }
                        }
                    }

                    // ── 树形节点列表（QA 树模式：动态展开/收回） ──
                    if (isUsingQaTree && visibleNodes.isNotEmpty()) {
                        items(
                            items = visibleNodes.withIndex().toList(),
                            key = { (_, vn) -> vn.node.id ?: vn.hashCode() }
                        ) { (_, vn) ->
                            val nodeId = vn.node.id ?: 0L
                            val baseState = nodeStates[nodeId] ?: if (vn.node.status == "done") "done" else "available"
                            // 目录节点：所有子节点完成则自身显示完成
                            val currentState = if (vn.hasChildren && isAllChildrenDone(nodeId)) "done" else baseState
                            TreeNodeCardV2(
                                node = vn.node,
                                depth = vn.depth,
                                isLastSibling = vn.isLastSibling,
                                ancestorHasMoreSiblings = vn.ancestorHasMoreSiblings,
                                hasChildren = vn.hasChildren,
                                isExpanded = vn.isExpanded,
                                onToggleExpand = { toggleExpand(nodeId) },
                                currentState = currentState,
                                onStateChange = { newState ->
                                    updateNodeState(nodeId, newState)
                                }
                            )
                        }
                    } else {
                        // 非 QA 树模式：使用普通节点卡片
                        items(
                            items = path.nodes.withIndex().toList(),
                            key = { (_, node) -> node.id }
                        ) { (index, node) ->
                            DetailNodeCard(
                                node = node,
                                index = index,
                                currentState = nodeStates[node.id] ?: node.status,
                                onStateChange = { newState ->
                                    updateNodeState(node.id, newState)
                                }
                            )
                        }
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
fun DetailHeaderCard(
    path: LearningPath,
    subtitle: String = "📚 学习路径"
) {
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
                text = subtitle,
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

            // 节点类型和时间信息
            if (node.nodeType.isNotBlank() || node.estMinutes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (node.nodeType.isNotBlank()) {
                        val typeLabel = when (node.nodeType) {
                            "lesson" -> "📖 课程"
                            "practice" -> "💻 练习"
                            "checkpoint" -> "🎯 测验"
                            "resource" -> "📎 资源"
                            else -> node.nodeType
                        }
                        val typeColor = when (node.nodeType) {
                            "lesson" -> Color(0xFF4A90D9)
                            "practice" -> Color(0xFFE67E22)
                            "checkpoint" -> Color(0xFF9B59B6)
                            "resource" -> Color(0xFF27AE60)
                            else -> colorResource(R.color.text_muted)
                        }
                        Text(
                            text = typeLabel,
                            color = typeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (node.estMinutes > 0) {
                        if (node.nodeType.isNotBlank()) {
                            Text(
                                text = " · ",
                                color = colorResource(R.color.text_muted),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "约 ${node.estMinutes} 分钟",
                            color = colorResource(R.color.text_muted),
                            fontSize = 12.sp
                        )
                    }
                }
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
// 树形节点卡片 V2（支持展开/收回、目录结构）
// ============================================================

@Composable
fun TreeNodeCardV2(
    node: com.nku.helloworld.ui.plan.model.QaNodeData,
    depth: Int,
    isLastSibling: Boolean,
    ancestorHasMoreSiblings: List<Boolean>,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currentState: String,
    onStateChange: (String) -> Unit
) {
    val statusColor = when (currentState) {
        "done" -> colorResource(R.color.brand_green)
        "in_progress" -> colorResource(R.color.brand_orange)
        "available" -> colorResource(R.color.brand_primary)
        else -> colorResource(R.color.text_muted)
    }
    val statusIcon = when (currentState) {
        "done" -> "✅"
        "in_progress" -> "🔄"
        "available" -> if (hasChildren) "📁" else "📄"
        else -> "🔒"
    }
    val nodeType = node.nodeType ?: ""
    val estMinutes = node.estMinutes ?: 0
    val description = node.description ?: node.questionText ?: ""
    val nodeStatus = node.status ?: ""
    val statusLabel = if (nodeStatus == "planned") "📋 待学习" else ""

    // 构建目录树连接线前缀
    val prefixBuilder = StringBuilder()
    for (i in 0 until depth) {
        if (i < ancestorHasMoreSiblings.size && ancestorHasMoreSiblings[i]) {
            prefixBuilder.append("│  ")
        } else {
            prefixBuilder.append("   ")
        }
    }
    if (depth > 0) {
        if (isLastSibling) {
            prefixBuilder.append("└── ")
        } else {
            prefixBuilder.append("├── ")
        }
    }
    val prefix = prefixBuilder.toString()

    val nodeTypeLabel = when (nodeType) {
        "lesson" -> "📖"
        "practice" -> "💻"
        "checkpoint" -> "🎯"
        "resource" -> "📎"
        else -> ""
    }
    val nodeTypeColor = when (nodeType) {
        "lesson" -> Color(0xFF4A90D9)
        "practice" -> Color(0xFFE67E22)
        "checkpoint" -> Color(0xFF9B59B6)
        "resource" -> Color(0xFF27AE60)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.surface_white)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, statusColor.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // ── 第一行：连接线 + 展开/收回图标 + 标题 + 状态 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 树连接线
                if (prefix.isNotBlank()) {
                    Text(
                        text = prefix,
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // 展开/收回图标（有子节点时显示）
                if (hasChildren) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "▼" else "▶",
                            fontSize = 10.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // 节点类型图标
                if (nodeTypeLabel.isNotBlank()) {
                    Text(text = nodeTypeLabel, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // 标题（有子节点时可点击展开/收回）
                Text(
                    text = node.title ?: "未命名节点",
                    color = colorResource(R.color.text_primary),
                    fontSize = 14.sp,
                    fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (hasChildren) {
                                Modifier.clickable { onToggleExpand() }
                            } else {
                                Modifier
                            }
                        )
                )

                // 状态图标 + planned 标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = " $statusIcon", fontSize = 12.sp)
                    if (statusLabel.isNotBlank()) {
                        Text(
                            text = statusLabel,
                            color = colorResource(R.color.brand_primary),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // ── 第二行：类型 + 时间（缩进对齐） ──
            if (nodeType.isNotBlank() || estMinutes > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.padding(
                        start = if (hasChildren) 28.dp else 4.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (nodeType.isNotBlank()) {
                        val typeLabel = when (nodeType) {
                            "lesson" -> "📖 课程"
                            "practice" -> "💻 练习"
                            "checkpoint" -> "🎯 测验"
                            "resource" -> "📎 资源"
                            else -> nodeType
                        }
                        Text(
                            text = typeLabel,
                            color = nodeTypeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (estMinutes > 0) {
                        if (nodeType.isNotBlank()) {
                            Text(
                                text = " · ",
                                color = colorResource(R.color.text_muted),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "约 ${estMinutes} 分钟",
                            color = colorResource(R.color.text_muted),
                            fontSize = 11.sp
                        )
                    }
                    // 展开后的子节点数量提示
                    if (isExpanded && hasChildren) {
                        Text(
                            text = " · ${if (isExpanded) "已展开" else "已收起"}",
                            color = colorResource(R.color.text_muted),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // ── 描述 ──
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = colorResource(R.color.text_secondary),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = if (hasChildren) 28.dp else 4.dp),
                    maxLines = 2
                )
            }

            // ── 操作按钮（仅叶子节点可操作，目录节点不显示） ──
            if (!hasChildren && (currentState == "available" || currentState == "in_progress")) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentState == "available") {
                        OutlinedButton(
                            onClick = { onStateChange("in_progress") },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("开始学习", fontSize = 12.sp)
                        }
                    }
                    if (currentState == "in_progress") {
                        Button(
                            onClick = { onStateChange("done") },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.brand_green)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("标记完成", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 树形节点卡片（旧版，保留兼容）
// ============================================================

@Composable
fun TreeNodeCard(
    node: PathNode,
    depth: Int,
    isLastSibling: Boolean,
    ancestorHasMoreSiblings: List<Boolean>,
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
        "done" -> "✅"
        "in_progress" -> "🔄"
        "available" -> "🔓"
        else -> "🔒"
    }

    // 构建连接线前缀
    val prefixBuilder = StringBuilder()
    for (i in 0 until depth) {
        if (i < ancestorHasMoreSiblings.size && ancestorHasMoreSiblings[i]) {
            prefixBuilder.append("│  ")
        } else {
            prefixBuilder.append("   ")
        }
    }
    if (depth > 0) {
        if (isLastSibling) {
            prefixBuilder.append("└── ")
        } else {
            prefixBuilder.append("├── ")
        }
    }
    val prefix = prefixBuilder.toString()

    val nodeTypeLabel = when (node.nodeType) {
        "lesson" -> "📖"
        "practice" -> "💻"
        "checkpoint" -> "🎯"
        "resource" -> "📎"
        else -> ""
    }
    val nodeTypeColor = when (node.nodeType) {
        "lesson" -> Color(0xFF4A90D9)
        "practice" -> Color(0xFFE67E22)
        "checkpoint" -> Color(0xFF9B59B6)
        "resource" -> Color(0xFF27AE60)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 20).dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.surface_white)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, statusColor.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prefix.isNotBlank()) {
                    Text(
                        text = prefix,
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1
                    )
                }
                if (nodeTypeLabel.isNotBlank()) {
                    Text(text = nodeTypeLabel, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = node.title,
                    color = colorResource(R.color.text_primary),
                    fontSize = 14.sp,
                    fontWeight = if (depth <= 1) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(text = " $statusText", fontSize = 12.sp)
            }

            if (node.nodeType.isNotBlank() || node.estMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(start = (depth * 14).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.nodeType.isNotBlank()) {
                        val typeLabel = when (node.nodeType) {
                            "lesson" -> "📖 课程"
                            "practice" -> "💻 练习"
                            "checkpoint" -> "🎯 测验"
                            "resource" -> "📎 资源"
                            else -> node.nodeType
                        }
                        Text(
                            text = typeLabel,
                            color = nodeTypeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (node.estMinutes > 0) {
                        if (node.nodeType.isNotBlank()) {
                            Text(
                                text = " · ",
                                color = colorResource(R.color.text_muted),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "约 ${node.estMinutes} 分钟",
                            color = colorResource(R.color.text_muted),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!node.question.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = node.question,
                    color = colorResource(R.color.text_secondary),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = (depth * 14).dp),
                    maxLines = 3
                )
            }

            if (currentState == "available" || currentState == "in_progress") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentState == "available") {
                        OutlinedButton(
                            onClick = { onStateChange("in_progress") },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("开始学习", fontSize = 12.sp)
                        }
                    }
                    if (currentState == "in_progress") {
                        Button(
                            onClick = { onStateChange("done") },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.brand_green)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("标记完成", fontSize = 12.sp, color = Color.White)
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
