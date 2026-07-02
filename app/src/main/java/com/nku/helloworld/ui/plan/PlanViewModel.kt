package com.nku.helloworld.ui.plan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/**
 * 学习计划页面 ViewModel
 *
 * 负责管理计划列表数据，支持从 API 获取或使用本地存储的计划。
 * 后端接口位置参考 PlanApiService 中的注释。
 */
class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        if (!PlanLocalStorage.isInitialized()) {
            PlanLocalStorage.init(application)
        }
        // 加载本地保存的计划
        loadLocalPlans()
        // 已登录则从 API 获取最新数据
        if (SessionManager.isLoggedIn()) {
            fetchPlansFromApi()
        }
    }

    /**
     * 从本地存储加载计划
     */
    private fun loadLocalPlans() {
        val localPlans = PlanLocalStorage.loadAllPlans()
        if (localPlans.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                plans = localPlans,
                isLoading = false
            )
        }
    }

    /**
     * 保存一个计划到本地存储（由 CreatePlanActivity 调用）
     */
    fun saveLocalPlan(plan: PlanItem) {
        PlanLocalStorage.savePlan(plan)
        // 刷新 UI 状态
        val currentPlans = _uiState.value.plans.toMutableList()
        val existingIndex = currentPlans.indexOfFirst { it.id == plan.id }
        if (existingIndex >= 0) {
            currentPlans[existingIndex] = plan
        } else {
            currentPlans.add(0, plan)
        }
        _uiState.value = _uiState.value.copy(plans = currentPlans)
    }

    /**
     * 刷新计划列表 — 供外部在登录状态变化时调用
     *
     * 已登录 → 调 API + 加载本地计划
     * 未登录 → 仅加载本地计划
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

        // 总是加载本地计划
        loadLocalPlans()

        if (SessionManager.isLoggedIn()) {
            fetchPlansFromApi(isRefresh = true)
        } else {
            _uiState.value = _uiState.value.copy(isRefreshing = false)
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
     * 如果 API 不可用，保留本地已保存的计划。
     */
    fun fetchPlansFromApi(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            try {
                val token = SessionManager.getAccessToken() ?: ""
                if (token.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "请先登录"
                    )
                    return@launch
                }

                val result = PlanApiService.getConversations(token)
                result.fold(
                    onSuccess = { conversations ->
                        val apiPlans = conversations.mapIndexed { index, conv ->
                            PlanItem(
                                id = conv.id,
                                title = conv.title,
                                latestDate = conv.updated_at.take(10),
                                createdDate = conv.created_at.take(10),
                                colorIndex = index % 5,
                                conversationId = conv.id
                            )
                        }
                        // 合并 API 计划和本地计划（本地计划优先展示）
                        val localPlans = PlanLocalStorage.loadAllPlans()
                        val mergedPlans = (localPlans + apiPlans).distinctBy { it.id }
                        _uiState.value = PlanUiState(
                            plans = mergedPlans,
                            isLoading = false,
                            isRefreshing = false
                        )
                    },
                    onFailure = { error ->
                        // API 不可用时，保留本地计划
                        val localPlans = PlanLocalStorage.loadAllPlans()
                        _uiState.value = _uiState.value.copy(
                            plans = if (localPlans.isNotEmpty()) localPlans else _uiState.value.plans,
                            isLoading = false,
                            isRefreshing = false,
                            error = if (localPlans.isEmpty()) (error.message ?: "获取计划失败") else null
                        )
                    }
                )
            } catch (e: Exception) {
                val localPlans = PlanLocalStorage.loadAllPlans()
                _uiState.value = _uiState.value.copy(
                    plans = if (localPlans.isNotEmpty()) localPlans else _uiState.value.plans,
                    isLoading = false,
                    isRefreshing = false,
                    error = if (localPlans.isEmpty()) (e.message ?: "网络错误") else null
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
