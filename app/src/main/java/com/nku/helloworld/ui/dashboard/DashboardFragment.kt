package com.nku.helloworld.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.nku.helloworld.R

data class PlanTaskItem(
    val title: String,
    val meta: String,
    val status: String,
    val colorRes: Int,
)

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DashboardScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    val tasks = listOf(
        PlanTaskItem(stringResource(R.string.plan_task_1_title), stringResource(R.string.plan_task_1_meta), stringResource(R.string.plan_status_todo), R.color.status_todo),
        PlanTaskItem(stringResource(R.string.plan_task_2_title), stringResource(R.string.plan_task_2_meta), stringResource(R.string.plan_status_pending), R.color.status_pending),
        PlanTaskItem(stringResource(R.string.plan_task_3_title), stringResource(R.string.plan_task_3_meta), stringResource(R.string.plan_status_todo), R.color.status_todo),
        PlanTaskItem(stringResource(R.string.plan_task_4_title), stringResource(R.string.plan_task_4_meta), stringResource(R.string.plan_status_done), R.color.status_done),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Using typical app_bg color, here hardcoded as nearly white or use AppTheme
            .background(colorResource(R.color.app_bg))
            .padding(top = 52.dp, start = 24.dp, end = 24.dp, bottom = 24.dp) // space_lg happens to be ~24dp
    ) {
        Text(
            text = stringResource(R.string.plan_title),
            color = colorResource(R.color.text_primary),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.plan_subtitle),
            color = colorResource(R.color.text_secondary),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp) // space_xs
        )

        // Plan Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorResource(R.color.brand_primary)) // using simplified background vs XML drawable gradient
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = stringResource(R.string.plan_card_title),
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(R.string.plan_card_value),
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.plan_card_desc),
                    color = Color(0xE5FFFFFF),
                    fontSize = 13.sp
                )
            }
        }

        Text(
            text = stringResource(R.string.plan_section),
            color = colorResource(R.color.text_primary),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp) // space_xl
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(tasks) { task ->
                PlanTaskItemView(task)
            }
        }
    }
}

@Composable
fun PlanTaskItemView(item: PlanTaskItem) {
    val statusColor = colorResource(item.colorRes)
    val bgColor = when (item.colorRes) {
        R.color.status_done -> colorResource(R.color.brand_green_soft)
        R.color.status_pending -> colorResource(R.color.brand_orange_soft)
        else -> colorResource(R.color.brand_primary_soft)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = colorResource(R.color.surface_white)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Bar
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = 44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = item.title,
                    color = colorResource(R.color.text_primary),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.meta,
                    color = colorResource(R.color.text_secondary),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = item.status,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}