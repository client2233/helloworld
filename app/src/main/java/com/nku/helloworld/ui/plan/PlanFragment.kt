package com.nku.helloworld.ui.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nku.helloworld.R
import com.nku.helloworld.ui.plan.model.PlanItem

/**
 * 学习计划页面 Fragment
 *
 * 展示用户的所有学习计划，采用两列卡片化网格布局，
 * 支持下拉刷新、本地缓存和从 AI 对话页面返回后自动刷新。
 */
class PlanFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PlanScreen()
            }
        }
    }
}

// ============================================================
// 马卡龙色系调色板（高饱和度、低明度，柔和鲜明）
// ============================================================
private val MacaronPalette = listOf(
    Color(0xFF6B9CE4),  // 柔和蓝
    Color(0xFF5BC0A0),  // 薄荷绿
    Color(0xFFF4A261),  // 暖杏橙
    Color(0xFFB583E4),  // 薰衣草紫
    Color(0xFFE483B5),  // 樱花粉
)

/**
 * 学习计划页面主入口 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = viewModel(),
    onPlanClick: ((PlanItem) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 下拉刷新状态
    val pullToRefreshState = rememberPullToRefreshState()
    // 标记本次刷新是否由用户下拉触发（而非代码自动刷新）
    val isRefreshByPull = remember { mutableStateOf(false) }

    // 监听生命周期变化，页面恢复时静默刷新（不触发下拉动画）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 当下拉触发刷新时
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            isRefreshByPull.value = true
            viewModel.refresh()
        }
    }

    // 持续监听 isRefreshing 状态，变为 false 时结束下拉动画
    LaunchedEffect(Unit) {
        snapshotFlow { uiState.isRefreshing }
            .collect { refreshing ->
                if (!refreshing && isRefreshByPull.value) {
                    pullToRefreshState.endRefresh()
                    isRefreshByPull.value = false
                }
            }
    }

    // 最外层 Box：nestedScroll 包裹整个页面，刷新圆出现在屏幕最顶部
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.app_bg))
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── 1. 顶部导航栏 ──
            PlanTopBar(
                onRefresh = { viewModel.refresh() },
                isLoading = uiState.isLoading
            )

            // ── 2. 内容区域 ──
            if (uiState.isLoading && uiState.plans.isEmpty()) {
                // 首次加载中
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(R.color.brand_primary)
                    )
                }
            } else if (uiState.plans.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyPlanState(
                        error = uiState.error,
                        onRefresh = { viewModel.refresh() }
                    )
                }
            } else {
                // 计划卡片网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.plans,
                        key = { it.id }
                    ) { plan ->
                        PlanCard(
                            plan = plan,
                            onClick = { onPlanClick?.invoke(plan) }
                        )
                    }
                }
            }

            // ── 3. 底部提示语 ──
            PlanFooter()

            // ── 底部留白 ──
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 下拉刷新指示器（放在最外层 Box 顶部，盖住所有内容）
        // Material3 内部根据 progress / isRefreshing 自动控制显隐
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = colorResource(R.color.surface_white),
            contentColor = colorResource(R.color.brand_primary)
        )
    }

    // 错误提示
    uiState.error?.let { errorMsg ->
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(errorMsg) {
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short
            )
        }
    }
}

// ============================================================
// 空计划状态
// ============================================================

@Composable
fun EmptyPlanState(
    error: String?,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "暂无学习计划",
                color = colorResource(R.color.text_secondary),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.plan_empty_hint),
                color = colorResource(R.color.text_muted),
                fontSize = 13.sp
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = colorResource(R.color.brand_red),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("重试", fontSize = 14.sp)
                }
            }
        }
    }
}

// ============================================================
// 顶部导航栏（带刷新按钮）
// ============================================================

@Composable
fun PlanTopBar(
    onRefresh: () -> Unit = {},
    isLoading: Boolean = false
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
            // 返回按钮（左侧，预留）
            IconButton(
                onClick = { },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_auth_back),
                    contentDescription = stringResource(R.string.plan_back),
                    tint = colorResource(R.color.text_primary),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 页面标题（居中）
            Text(
                text = stringResource(R.string.plan_page_title),
                color = colorResource(R.color.text_primary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // 右侧刷新按钮
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(48.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colorResource(R.color.brand_primary),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "刷新",
                        tint = colorResource(R.color.text_secondary),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// 单个计划卡片
// ============================================================

@Composable
fun PlanCard(
    plan: PlanItem,
    onClick: () -> Unit = {}
) {
    val cardColor = MacaronPalette[plan.colorIndex % MacaronPalette.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 核心标题区
            Text(
                text = plan.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            )

            // 时间/状态数据区
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (!plan.latestDate.isNullOrBlank() && plan.latestDate != "暂无")
                        "最近: ${plan.latestDate}"
                    else
                        "最近: 暂无",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "创建: ${plan.createdDate ?: "未知"}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================
// 底部提示语
// ============================================================

@Composable
fun PlanFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.plan_footer_hint),
            color = colorResource(R.color.text_muted),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PlanScreenPreview() {
    PlanScreen()
}

@Preview(showBackground = true)
@Composable
fun PlanCardPreview() {
    PlanCard(
        plan = PlanItem(
            id = 1,
            title = "Python 入门",
            latestDate = "2026-05-28",
            createdDate = "2026-05-01",
            colorIndex = 0
        )
    )
}
