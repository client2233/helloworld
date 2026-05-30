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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
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
 * 色彩鲜明，信息层级清晰。
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
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = viewModel(),
    onPlanClick: ((PlanItem) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听生命周期变化，页面恢复时刷新计划列表（例如从登录页返回）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.app_bg))
    ) {
        // ── 1. 顶部导航栏 ──
        PlanTopBar()

        // ── 2. 计划卡片网格列表 ──
        if (uiState.isLoading) {
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
                }
            }
        } else {
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

    // 错误提示（可扩展为 Snackbar）
    uiState.error?.let { errorMsg ->
        LaunchedEffect(errorMsg) {
            // 这里可以显示 Snackbar，目前仅做预留
            // scaffoldState.snackbarHostState.showSnackbar(errorMsg)
        }
    }
}

// ============================================================
// 顶部导航栏
// ============================================================

@Composable
fun PlanTopBar() {
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
            // 返回按钮（左侧）
            IconButton(
                onClick = { /* 返回上一级，预留 */ },
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

            // 右侧占位（保持对称）
            Spacer(modifier = Modifier.size(48.dp))
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
            // 核心标题区（中部居上）
            // 特大号白色粗体字，支持自动换行与缩进
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
                    .padding(bottom = 40.dp)  // 撑开底部空间与日期区域隔离
            )

            // 时间/状态数据区（底部贴边）
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 动态日期项 1：最新执行/关联日期
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

                // 动态日期项 2：创建日期
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
