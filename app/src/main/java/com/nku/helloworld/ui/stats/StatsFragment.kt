package com.nku.helloworld.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.nku.helloworld.R
import java.util.Calendar

class StatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                StatsScreen()
            }
        }
    }
}

@Composable
fun StatsScreen() {
    val brandBlue = colorResource(R.color.brand_primary)
    val lightBg = colorResource(R.color.app_bg)
    val whiteBg = colorResource(R.color.surface_white)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val textMuted = colorResource(R.color.text_muted)
    val dividerSoft = colorResource(R.color.divider_soft)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBg)
    ) {
        // ── 顶部导航栏 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(whiteBg)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.stats_title),
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── 可滚动内容区域 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp)
        ) {
            // ── 1. 当前学习进度 ──
            Text(
                text = stringResource(R.string.stats_current_progress),
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProgressItem(
                title = stringResource(R.string.stats_badminton),
                progress = 0.20f,
                brandBlue = brandBlue,
                textSecondary = textSecondary,
                dividerSoft = dividerSoft
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProgressItem(
                title = stringResource(R.string.stats_table_tennis),
                progress = 0.10f,
                brandBlue = brandBlue,
                textSecondary = textSecondary,
                dividerSoft = dividerSoft
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. 总学习进度卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = whiteBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.stats_total_progress),
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "15%",
                            color = brandBlue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 环形图
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            progress = 0.50f,
                            brandBlue = brandBlue,
                            strokeWidthDp = 36.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. 学习日历按钮 ──
            Button(
                onClick = { /* TODO: open calendar */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandBlue)
            ) {
                Text(
                    text = stringResource(R.string.stats_learning_calendar),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 4. 日历预览 ──
            CalendarPreview(
                brandBlue = brandBlue,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                textMuted = textMuted,
                whiteBg = whiteBg
            )
        }
    }
}

// ── 单个进度条目（名称 + 进度条 + 百分比） ──
@Composable
private fun ProgressItem(
    title: String,
    progress: Float,
    brandBlue: Color,
    textSecondary: Color,
    dividerSoft: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = textSecondary,
                fontSize = 14.sp
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = brandBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 进度条背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(color = dividerSoft, shape = RoundedCornerShape(4.dp))
        ) {
            // 进度条填充
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color = brandBlue, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

// ── 环形图（Donut Chart） ──
@Composable
private fun DonutChart(
    progress: Float,
    brandBlue: Color,
    strokeWidthDp: androidx.compose.ui.unit.Dp
) {
    val grayColor = Color(0xFFE8EDF5)

    Canvas(modifier = Modifier.size(160.dp)) {
        val strokePx = strokeWidthDp.toPx()
        val canvasSize = size.minDimension
        val arcSize = canvasSize - strokePx

        // 灰色背景环
        drawArc(
            color = grayColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(strokePx / 2, strokePx / 2),
            size = Size(arcSize, arcSize),
            style = Stroke(width = strokePx, cap = StrokeCap.Butt)
        )

        // 蓝色前景环（50% = 180°）
        drawArc(
            color = brandBlue,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(strokePx / 2, strokePx / 2),
            size = Size(arcSize, arcSize),
            style = Stroke(width = strokePx, cap = StrokeCap.Butt)
        )
    }
}

// ── 日历预览组件（左右翻页，学习时长着色，当日下划线） ──
@Composable
private fun CalendarPreview(
    brandBlue: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    whiteBg: Color
) {
    // ── 系统当前日期（用于高亮「今天」） ──
    val systemNow = remember { Calendar.getInstance() }
    val systemYear = systemNow.get(Calendar.YEAR)
    val systemMonth = systemNow.get(Calendar.MONTH) + 1
    val systemDay = systemNow.get(Calendar.DAY_OF_MONTH)

    // ── 当前显示的年份／月份（状态，可翻页） ──
    var displayYear by remember { mutableStateOf(systemYear) }
    var displayMonth by remember { mutableStateOf(systemMonth) }

    // ── 辅助 Calendar（每次计算时重新设置） ──
    val cal = remember { Calendar.getInstance() }

    // ── 根据 displayYear/displayMonth 计算当月天数 & 首日偏移 ──
    val daysInMonth = remember(displayYear, displayMonth) {
        cal.set(displayYear, displayMonth - 1, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val firstDayIndex = remember(displayYear, displayMonth) {
        cal.set(displayYear, displayMonth - 1, 1)
        (cal.get(Calendar.DAY_OF_WEEK) + 6) % 7 // 0=星期一 … 6=星期日
    }

    // ── 模拟学习时长数据（分钟/天），当天之后的日期不显示 ──
    val studyMinutes = remember(displayYear, displayMonth) {
        val seed = displayYear * 10000 + displayMonth * 100
        val data = mutableMapOf<Int, Int>()

        // 判断所显示的月份相对于系统当前月份的位置
        val isPastMonth = displayYear < systemYear ||
                (displayYear == systemYear && displayMonth < systemMonth)
        val isFutureMonth = displayYear > systemYear ||
                (displayYear == systemYear && displayMonth > systemMonth)

        if (isFutureMonth) {
            // 未来月份：完全不显示学习时长
        } else {
            val rng = object {
                var s = seed.toLong()
                fun next(): Int { s = (s * 1103515245 + 12345) % (1L shl 31); return s.toInt() }
            }
            val maxDay = if (isPastMonth) daysInMonth else systemDay.coerceAtMost(daysInMonth)
            for (d in 1..maxDay) {
                val minutes = (rng.next() % 181).let { if (it < 0) -it else it }
                cal.set(displayYear, displayMonth - 1, d)
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val adjusted = if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
                    (minutes * 1.5).toInt().coerceAtMost(180)
                else
                    minutes
                if (adjusted > 5) data[d] = adjusted
            }
        }
        data
    }

    // ── 学习时长 → 蓝色背景 alpha ──
    fun intensity(minutes: Int): Float = when {
        minutes >= 150 -> 0.70f
        minutes >= 120 -> 0.55f
        minutes >= 90  -> 0.40f
        minutes >= 60  -> 0.28f
        minutes >= 30  -> 0.15f
        minutes >= 10  -> 0.08f
        else           -> 0f
    }

    // ── 构建日期网格 ──
    val weeks = remember(displayYear, displayMonth) {
        val list = mutableListOf<MutableList<Int?>>()
        var week = mutableListOf<Int?>()
        repeat(firstDayIndex) { week.add(null) }
        for (d in 1..daysInMonth) {
            week.add(d)
            if (week.size == 7) { list.add(week); week = mutableListOf() }
        }
        if (week.isNotEmpty()) {
            while (week.size < 7) week.add(null)
            list.add(week)
        }
        list
    }

    // ── 翻页逻辑 ──
    fun goPrevMonth() {
        if (displayMonth == 1) {
            displayYear--
            displayMonth = 12
        } else {
            displayMonth--
        }
    }
    fun goNextMonth() {
        if (displayMonth == 12) {
            displayYear++
            displayMonth = 1
        } else {
            displayMonth++
        }
    }
    fun goPrevYear() {
        displayYear--
    }
    fun goNextYear() {
        displayYear++
    }

    val isCurrentMonth = displayYear == systemYear && displayMonth == systemMonth
    val cellHeightDp = 36.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = whiteBg, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // ── 年／月切换栏 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayYear.toString(),
                    color = textSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%02d", displayMonth),
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 《 前一年
                Text(
                    text = "\u300A",
                    color = textMuted,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clickable { goPrevYear() }
                )
                // ‹ 前一月
                Text(
                    text = "\u2039",
                    color = textMuted,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clickable { goPrevMonth() }
                )
                // › 下一月
                Text(
                    text = "\u203A",
                    color = textMuted,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clickable { goNextMonth() }
                )
                // 》 下一年
                Text(
                    text = "\u300B",
                    color = textMuted,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clickable { goNextYear() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 星期表头 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    color = textMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── 日期网格 ──
        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cellHeightDp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val mins = studyMinutes[day] ?: 0
                            val alpha = intensity(mins)
                            val bgColor = if (alpha > 0f) brandBlue.copy(alpha = alpha) else Color.Transparent
                            val isToday = isCurrentMonth && day == systemDay

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 当日蓝色下划线（仅当前月份显示）
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 2.dp)
                                        .width(16.dp)
                                        .height(2.dp)
                                        .background(brandBlue, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    StatsScreen()
}
