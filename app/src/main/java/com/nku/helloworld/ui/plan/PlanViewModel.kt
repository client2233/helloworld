package com.nku.helloworld.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nku.helloworld.auth.SessionManager
import com.nku.helloworld.ui.plan.api.PlanApiService
import com.nku.helloworld.ui.plan.model.PlanItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 学习计划页面状态
 */
data class PlanUiState(
    val plans: List<PlanItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 学习计划页面 ViewModel
 *
 * 负责管理计划列表数据，支持从 API 获取或使用本地 Mock 数据。
 * 后端接口位置参考 PlanApiService 中的注释。
 */
class PlanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        // 初始化时判断登录状态，已登录则调 API，否则加载示例数据
        if (SessionManager.isLoggedIn()) {
            fetchPlansFromApi()
        } else {
            loadMockData()
        }
    }

    /**
     * 加载示例数据（用于 UI 开发阶段）
     *
     * 展示不同长度的标题、不同的日期状态（含 "暂无" 缺省值），
     * 覆盖色彩索引的多种取值。
     */
    private fun loadMockData() {
        _uiState.value = PlanUiState(
            plans = listOf(
                PlanItem(
                    id = 1,
                    title = "Python 入门",
                    latestDate = "2026-05-28",
                    createdDate = "2026-05-01",
                    progress = 0.6f,
                    colorIndex = 0
                ),
                PlanItem(
                    id = 2,
                    title = "Java 进阶学习",
                    latestDate = "2026-05-27",
                    createdDate = "2026-04-15",
                    progress = 0.3f,
                    colorIndex = 1
                ),
                PlanItem(
                    id = 3,
                    title = "数据结构与算法",
                    latestDate = "2026-05-25",
                    createdDate = "2026-03-10",
                    progress = 0.8f,
                    colorIndex = 2
                ),
                PlanItem(
                    id = 4,
                    title = "Android 开发实战",
                    latestDate = "2026-05-20",
                    createdDate = "2026-04-01",
                    progress = 0.4f,
                    colorIndex = 3
                ),
                PlanItem(
                    id = 5,
                    title = "英语口语练习",
                    latestDate = "2026-05-29",
                    createdDate = "2026-05-05",
                    progress = 0.2f,
                    colorIndex = 4
                ),
                PlanItem(
                    id = 6,
                    title = "机器学习基础",
                    latestDate = null,  // 缺省状态：暂无
                    createdDate = "2026-05-30",
                    progress = 0.0f,
                    colorIndex = 0
                ),
                // 新增一个长标题卡片，测试自动换行
                PlanItem(
                    id = 7,
                    title = "计算机组成原理与体系结构",
                    latestDate = "2026-05-26",
                    createdDate = "2026-02-20",
                    progress = 0.5f,
                    colorIndex = 1
                ),
                PlanItem(
                    id = 8,
                    title = "高等数学",
                    latestDate = "暂无",
                    createdDate = "2026-05-30",
                    progress = 0.0f,
                    colorIndex = 2
                )
            )
        )
    }

    /**
     * 刷新计划列表 — 供外部在登录状态变化时调用
     *
     * 已登录 → 调 API
     * 未登录 → 清空列表
     */
    fun refresh() {
        if (SessionManager.isLoggedIn()) {
            fetchPlansFromApi()
        } else {
            _uiState.value = PlanUiState()
        }
    }

    /**
     * 从 API 获取学习计划列表
     *
     * 调用链路：
     * 1. GET /api/v1/conversations → 获取用户的所有会话列表
     * 2. 对于每个会话（可选），可进一步调用
     *    GET /api/v1/learning-paths/conversations/{id}/current 获取学习路径
     *
     * 会自动处理未登录状态并给出提示。
     */
    fun fetchPlansFromApi() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
val token = SessionManager.getAccessToken() ?: ""
                if (token.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "请先登录"
                    )
                    return@launch
                }

                val result = PlanApiService.getConversations(token)
                result.fold(
                    onSuccess = { conversations ->
                        val plans = conversations.mapIndexed { index, conv ->
                            PlanItem(
                                id = conv.id,
                                title = conv.title,
                                latestDate = conv.updated_at.take(10),
                                createdDate = conv.created_at.take(10),
                                colorIndex = index % 5,
                                conversationId = conv.id
                            )
                        }
                        _uiState.value = PlanUiState(
                            plans = if (plans.isNotEmpty()) plans else _uiState.value.plans,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "获取计划失败"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "网络错误"
                )
            }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
