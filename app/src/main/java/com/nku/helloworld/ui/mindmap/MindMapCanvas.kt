package com.nku.helloworld.ui.mindmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nku.helloworld.ui.plan.model.LearningNodeOut
import com.nku.helloworld.ui.plan.model.PlanItem
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

// ═══════════════════════════════════════════
// 颜色常量
// ═══════════════════════════════════════════

private val C_Done = Color(0xFF16A34A)
private val C_Prog = Color(0xFFFF8B37)
private val C_Avail = Color(0xFF4D7CFE)
private val C_Lock = Color(0xFF95A0B2)

private fun stColor(s: String?) = when (s) {
    "done" -> C_Done; "in_progress" -> C_Prog; "available" -> C_Avail; else -> C_Lock
}
private fun stIcon(s: String?) = when (s) {
    "done" -> "\u2713"; "in_progress" -> "\u25CF"; "available" -> "\u25CB"; else -> "\uD83D\uDD12"
}
private fun typeIcon(t: String?) = when (t) {
    "lesson" -> "\uD83D\uDCD6"; "practice" -> "\uD83D\uDCBB"; "checkpoint" -> "\uD83D\uDCDD"; else -> "\uD83D\uDCCC"
}

private val Palette = listOf(
    Color(0xFF6B9CE4), Color(0xFF5BC0A0), Color(0xFFF4A261),
    Color(0xFFB583E4), Color(0xFFE483B5)
)

// ═══════════════════════════════════════════
// 树节点数据结构
// ═══════════════════════════════════════════

data class TreeNode(
    val data: LearningNodeOut,
    val children: MutableList<TreeNode> = mutableListOf()
) {
    val isRoot get() = data.parentNodeId == null
}

// ── 从 API 返回的 flat 列表构建树 ──
fun buildTree(nodes: List<LearningNodeOut>): List<TreeNode> {
    if (nodes.isEmpty()) return emptyList()
    val lookup = mutableMapOf<Long, TreeNode>()
    val roots = mutableListOf<TreeNode>()

    // 先创建所有节点
    for (n in nodes) {
        if (n.id != null) lookup[n.id] = TreeNode(n)
    }
    // 再链接父子
    for (t in lookup.values) {
        val pid = t.data.parentNodeId
        if (pid != null && lookup.containsKey(pid)) {
            lookup[pid]!!.children.add(t)
        } else {
            roots.add(t)
        }
    }
    // 按 sortNo 排序
    fun sortTree(node: TreeNode) {
        node.children.sortBy { it.data.sortNo ?: 0 }
        node.children.forEach { sortTree(it) }
    }
    roots.sortBy { it.data.sortNo ?: 0 }
    roots.forEach { sortTree(it) }
    return roots
}

// ═══════════════════════════════════════════
// 布局位置信息
// ═══════════════════════════════════════════

data class NodeLayout(
    val node: TreeNode,
    val x: Float,
    val y: Float,
    val level: Int
)

/**
 * 改进的径向树布局算法
 *
 * 布局策略：
 * - 根节点位于画布中心的下半部分（从中心向外辐射）
 * - 子节点从父节点方向以递减的角度范围延伸
 * - 树结构整体从中心向外展开
 */
fun layoutTree(
    roots: List<TreeNode>,
    centerX: Float,
    centerY: Float,
    density: androidx.compose.ui.unit.Density
): List<NodeLayout> {
    if (roots.isEmpty()) return emptyList()

    val levelGap = with(density) { 160.dp.toPx() }  // 增大层间距
    val minNodeGap = with(density) { 60.dp.toPx() }  // 最小节点间距
    val result = mutableListOf<NodeLayout>()

    // 布局子树
    fun layoutSubTree(
        nodes: List<TreeNode>,
        level: Int,
        angleCenter: Float,
        angleSpread: Float,
        parentX: Float,
        parentY: Float
    ) {
        if (nodes.isEmpty()) return
        val count = nodes.size
        if (count == 0) return

        // 根据数量动态调整角度范围
        val minSpread = 0.4f
        val perNodeAngle = 0.35f
        val dynamicSpread = (minSpread + count * perNodeAngle).coerceIn(minSpread, PI.toFloat())
        val spread = if (level <= 1) dynamicSpread * 1.2f else angleSpread
        val startAngle = angleCenter - spread / 2f

        nodes.forEachIndexed { idx, node ->
            val angle = startAngle + (idx.toFloat() + 0.5f) * spread / count

            val x = parentX + cos(angle) * levelGap
            val y = parentY + sin(angle) * levelGap

            result.add(NodeLayout(node, x, y, level))

            // 子节点角度范围：按数量自适应，逐层小幅缩小
            val childCount = node.children.size
            val childSpread = if (childCount > 0) {
                (0.5f + childCount * perNodeAngle).coerceIn(0.4f, 2.2f)
            } else 0.5f
            layoutSubTree(node.children, level + 1, angle, childSpread, x, y)
        }
    }

    // 根节点从画布中心向外辐射，分布在 270° ~ 450°（即 -90° ~ 90°，底部半圆向左向右展开）
    // 这样根节点分布在中心的左右两侧，视觉上更均衡
    val rootCount = roots.size
    if (rootCount == 1) {
        val rootX = centerX
        val rootY = centerY - levelGap * 0.5f
        result.add(NodeLayout(roots[0], rootX, rootY, 1))
        val childCount = roots[0].children.size
        val childSpread = if (childCount > 3) 2.8f else if (childCount > 1) 1.8f else 1.2f
        layoutSubTree(roots[0].children, 2, -PI.toFloat() / 2f, childSpread, rootX, rootY)
    } else {
        val rootSpread = (rootCount * 0.7f).coerceIn(1.5f, PI.toFloat())
        val rootStart = -PI.toFloat() / 2f - rootSpread / 2f
        roots.forEachIndexed { idx, node ->
            val angle = rootStart + (idx.toFloat() + 0.5f) * rootSpread / rootCount
            val x = centerX + cos(angle) * levelGap * 0.7f
            val y = centerY + sin(angle) * levelGap * 0.7f
            result.add(NodeLayout(node, x, y, 1))
            val childSpread = 1.5f
            layoutSubTree(node.children, 2, angle, childSpread, x, y)
        }
    }

    return result
}

// ═══════════════════════════════════════════
// 思维导图画布——主入口 Composable
// ═══════════════════════════════════════════

/**
 * 主页画布——以思维导图形式展示学习路径节点及其关系
 *
 * 使用 API 返回的 [LearningNodeOut] 列表构建树形结构，
 * 以径向树布局展示节点间的父子依赖关系。
 * 支持缩放、平移、节点选择、展开/收回等交互。
 *
 * @param plans 所有学习计划列表
 * @param selectedPlanId 当前选中的计划 ID
 * @param learningNodes 当前计划的学习路径节点（含 user_state）
 * @param isQaMode 是否为 QA 树模式（影响默认展开行为）
 * @param onSelectPlan 计划选择回调
 * @param modifier 修饰符
 */
@Composable
fun MindMapCanvas(
    plans: List<PlanItem>,
    selectedPlanId: Long,
    learningNodes: List<LearningNodeOut> = emptyList(),
    isQaMode: Boolean = false,
    onSelectPlan: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val selectedPlan = plans.find { it.id == selectedPlanId }
    val isInit = remember { mutableStateOf(true) }

    // ── 展开/收回状态 ──
    var expandedNodeIds by remember { mutableStateOf(setOf<Long>()) }
    var isFirstLoad by remember { mutableStateOf(true) }

    // 构建完整树
    val fullTree = remember(learningNodes) {
        if (learningNodes.isEmpty()) emptyList() else buildTree(learningNodes)
    }

    // 根据展开状态过滤树（只保留可见节点，不修改原树）
    fun filterExpanded(nodes: List<TreeNode>): List<TreeNode> {
        return nodes.mapNotNull { node ->
            val nid = node.data.id ?: return@mapNotNull node
            val hasChildren = node.children.isNotEmpty()
            val expanded = expandedNodeIds.contains(nid)
            // 创建新节点，children 递归过滤
            val filteredChildren = if (hasChildren && expanded) {
                filterExpanded(node.children)
            } else {
                emptyList()
            }
            TreeNode(
                data = node.data,
                children = filteredChildren.toMutableList()
            )
        }
    }

    // 初始展开：全部展开
    if (isFirstLoad && fullTree.isNotEmpty()) {
        isFirstLoad = false
        fun collectAll(nodes: List<TreeNode>) {
            for (n in nodes) {
                n.data.id?.let { 
                    if (n.children.isNotEmpty()) expandedNodeIds = expandedNodeIds + it
                }
                collectAll(n.children)
            }
        }
        collectAll(fullTree)
    }

    val displayTree = remember(fullTree, expandedNodeIds) {
        if (isQaMode) filterExpanded(fullTree) else fullTree
    }

    // 从完整树计算每个节点是否有子节点（不受展开/收回影响）
    val hasChildrenMap = remember(fullTree) {
        val map = mutableMapOf<Long, Boolean>()
        fun walk(nodes: List<TreeNode>) {
            for (n in nodes) {
                n.data.id?.let { map[it] = n.children.isNotEmpty() }
                walk(n.children)
            }
        }
        walk(fullTree)
        map
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { canvasSize = it }
            .background(Color(0xFFF0F4F8))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    isInit.value = false
                    val ns = (scale * zoom).coerceIn(0.3f, 3.0f)
                    val sc = ns / scale
                    offsetX = centroid.x - sc * (centroid.x - offsetX)
                    offsetY = centroid.y - sc * (centroid.y - offsetY)
                    scale = ns
                    offsetX += pan.x; offsetY += pan.y
                }
            }
    ) {
        if (selectedPlan != null) {
            val cw = canvasSize.width.toFloat()
            val ch = canvasSize.height.toFloat()
            val cx = if (isInit.value) cw / 2f else offsetX
            val cy = if (isInit.value) ch / 2f else offsetY

            // 计算节点布局（仅在树或画布尺寸变化时重新计算）
            val layouts = remember(displayTree, cw, ch, cx, cy) {
                layoutTree(displayTree, cx, cy, density)
            }

            // 统计数据（基于完整树）
            val totalNodes = learningNodes.size
            val doneCount = learningNodes.count { it.userState?.state == "done" }
            val progCount = learningNodes.count { it.userState?.state == "in_progress" }
            val availCount = learningNodes.count { it.userState?.state == "available" }
            val lockCount = totalNodes - doneCount - progCount - availCount
            val progress = if (totalNodes > 0) doneCount.toFloat() / totalNodes else 0f

            // 构建布局映射（用于绘制连线）
            val layoutMap = remember(layouts) {
                layouts.associateBy { it.node.data.id }
            }

            // ── 画连线层 ──
            val nodeW = with(density) { 140.dp.toPx() }
            val nodeH = with(density) { 48.dp.toPx() }

            // 将所有内容放在同一个 graphicsLayer 中，保证缩放平移时线节点同步
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = if (isInit.value) 0f else offsetX - cx,
                        translationY = if (isInit.value) 0f else offsetY - cy
                    )
            ) {
                // ── 连线 Canvas ──
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineColor = Color(0xFFCCD7E6)
                    val doneLine = C_Done.copy(alpha = 0.4f)

                    for (l in layouts) {
                        val fromX: Float?
                        val fromY: Float?

                        if (l.node.isRoot) {
                            fromX = cx; fromY = cy
                        } else {
                            val pid = l.node.data.parentNodeId
                            val pl = if (pid != null) layoutMap[pid] else null
                            if (pl != null) {
                                fromX = pl.x; fromY = pl.y
                            } else {
                                fromX = cx; fromY = cy
                            }
                        }

                        if (fromX != null && fromY != null) {
                            val isNodeDone = l.node.data.userState?.state == "done"
                            val color = if (isNodeDone) doneLine else lineColor
                            val strokeW = if (l.node.isRoot) 2.5f else 1.8f
                            drawLine(
                                color = color,
                                start = Offset(fromX, fromY),
                                end = Offset(l.x, l.y),
                                strokeWidth = with(density) { strokeW.dp.toPx() },
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // 统计面板
                StatsPanel(
                    done = doneCount, total = totalNodes,
                    inProgress = progCount, available = availCount,
                    locked = lockCount, progress = progress,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (cx + with(density) { 100.dp.toPx() }).roundToInt(),
                                (cy - with(density) { 80.dp.toPx() }).roundToInt()
                            )
                        }
                )

                // 中心节点（选中的计划）
                CenterNode(
                    title = selectedPlan.title,
                    color = Palette[selectedPlan.colorIndex % Palette.size],
                    progress = progress,
                    doneCount = doneCount,
                    totalCount = totalNodes,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (cx - with(density) { 90.dp.toPx() }).roundToInt(),
                                (cy - with(density) { 28.dp.toPx() }).roundToInt()
                            )
                        }
                        .size(with(density) { 180.dp }, with(density) { 56.dp })
                )

                // 树节点卡片
                for (l in layouts) {
                    val n = l.node.data
                    val state = n.userState?.state ?: "locked"
                    val nid = n.id
                    val hasChildren = nid != null && (hasChildrenMap[nid] ?: false)
                    val isExpanded = nid != null && expandedNodeIds.contains(nid)

                    MindMapNodeCard(
                        title = n.title ?: "",
                        status = state,
                        nodeType = n.nodeType,
                        estMinutes = n.estMinutes,
                        progressPercent = n.userState?.progressPercent,
                        hasChildren = hasChildren,
                        isExpanded = isExpanded,
                        onToggle = {
                            if (nid != null) {
                                expandedNodeIds = if (isExpanded) expandedNodeIds - nid
                                else expandedNodeIds + nid
                            }
                        },
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (l.x - nodeW / 2).roundToInt(),
                                    (l.y - nodeH / 2).roundToInt()
                                )
                            }
                            .size(with(density) { 140.dp }, with(density) { 48.dp })
                    )
                }

                // 无节点提示
                if (totalNodes == 0) {
                    EmptyHint(
                        title = selectedPlan.title,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (cx - with(density) { 100.dp.toPx() }).roundToInt(),
                                    (cy + with(density) { 50.dp.toPx() }).roundToInt()
                                )
                            }
                    )
                }
            }

            // ── 缩放控制 ──
            ZoomBar(
                scale = scale,
                onIn = { scale = (scale * 1.25f).coerceAtMost(3f); isInit.value = false },
                onOut = { scale = (scale / 1.25f).coerceAtLeast(0.3f); isInit.value = false },
                onReset = { scale = 1f; offsetX = 0f; offsetY = 0f; isInit.value = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
            )
        } else {
            // 无计划时
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("\uD83D\uDCD6 暂无学习计划", color = Color(0xFF95A0B2), fontSize = 16.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════
// 子组件
// ═══════════════════════════════════════════

@Composable
private fun CenterNode(
    title: String, color: Color, progress: Float,
    doneCount: Int, totalCount: Int, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .background(color, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, color = Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (totalCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$doneCount/$totalCount",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPanel(
    done: Int, total: Int, inProgress: Int, available: Int,
    locked: Int, progress: Float, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${(progress * 100).toInt()}%",
            color = Color(0xFF4D7CFE), fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatDot(C_Done, "$done")
            StatDot(C_Prog, "$inProgress")
            StatDot(C_Avail, "$available")
            StatDot(C_Lock, "$locked")
        }
        Spacer(Modifier.height(2.dp))
        Text("${total} 节点", color = Color(0xFF6D7787), fontSize = 9.sp)
    }
}

@Composable
private fun StatDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(2.dp))
        Text(label, color = Color(0xFF6D7787), fontSize = 9.sp)
    }
}

@Composable
private fun MindMapNodeCard(
    title: String,
    status: String,
    nodeType: String?,
    estMinutes: Long?,
    progressPercent: Long? = null,
    hasChildren: Boolean = false,
    isExpanded: Boolean = false,
    onToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val color = stColor(status)
    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .then(if (hasChildren) Modifier.clickable { onToggle() } else Modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 展开/收回图标（目录节点）
            if (hasChildren) {
                Text(
                    if (isExpanded) "▼" else "▶",
                    fontSize = 8.sp,
                    color = Color(0xFF95A0B2),
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
            // 状态图标
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (hasChildren) (if (isExpanded) "📂" else "📁") else stIcon(status),
                    color = color, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(typeIcon(nodeType), fontSize = 9.sp)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        title, color = Color(0xFF151A22), fontSize = 10.sp,
                        fontWeight = if (status == "done") FontWeight.Medium
                                    else FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (estMinutes != null && estMinutes > 0) {
                        Text(
                            "${estMinutes}min",
                            color = Color(0xFF95A0B2), fontSize = 8.sp
                        )
                    }
                    if (progressPercent != null && progressPercent > 0 && progressPercent < 100) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${progressPercent}%",
                            color = C_Prog, fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "\uD83D\uDD0E \u300C${title}\u300D",
                color = Color(0xFF95A0B2), fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "暂无学习节点\n请在详情页生成学习路径",
                color = Color(0xFF95A0B2), fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ZoomBar(
    scale: Float,
    onIn: () -> Unit,
    onOut: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp).clip(CircleShape)
                .background(Color(0xFFE8EDF5))
                .clickable(onClick = onOut),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2212", color = Color(0xFF151A22), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${(scale * 100).toInt()}%",
            color = Color(0xFF151A22), fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp).clip(CircleShape)
                .background(Color(0xFFE8EDF5))
                .clickable(onClick = onIn),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color(0xFF151A22), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .height(32.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE8EDF5))
                .clickable(onClick = onReset)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("\u91CD\u7F6E", color = Color(0xFF151A22), fontSize = 12.sp)
        }
    }
}

// ═══════════════════════════════════════════
// 计划选择器组件
// ═══════════════════════════════════════════

@Composable
fun PlanSelector(
    plans: List<PlanItem>,
    selectedPlanId: Long,
    onSelectPlan: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        plans.forEach { plan ->
            val sel = plan.id == selectedPlanId
            val c = Palette[plan.colorIndex % Palette.size]
            Box(
                modifier = Modifier
                    .weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) c else c.copy(alpha = 0.2f))
                    .clickable { onSelectPlan(plan.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    plan.title,
                    color = if (sel) Color.White else c,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
