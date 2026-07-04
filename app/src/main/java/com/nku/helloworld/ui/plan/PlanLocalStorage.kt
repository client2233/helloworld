package com.nku.helloworld.ui.plan

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nku.helloworld.ui.plan.model.PlanItem

/**
 * 学习计划本地存储
 *
 * 在 API 不可用或开发阶段，将通过 AI 对话生成的计划保存到本地，
 * 使得返回计划列表页面时仍能看到已生成的计划。
 */
object PlanLocalStorage {

    private const val PREFS_NAME = "helloworld_local_plans"
    private const val KEY_PLANS = "saved_plans"
    private const val KEY_COMPLETED_NODES_PREFIX = "completed_nodes_"
    private const val MAX_LOCAL_PLANS = 20

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private var _initialized = false

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _initialized = true
    }

    fun isInitialized(): Boolean = _initialized

    /**
     * 保存一个本地计划
     */
    fun savePlan(plan: PlanItem) {
        val plans = loadAllPlans().toMutableList()
        // 替换相同 ID 的计划，或追加
        val existingIndex = plans.indexOfFirst { it.id == plan.id }
        if (existingIndex >= 0) {
            plans[existingIndex] = plan
        } else {
            plans.add(0, plan) // 新计划放在最前面
        }
        // 限制数量
        val trimmed = plans.take(MAX_LOCAL_PLANS)
        val json = gson.toJson(trimmed)
        prefs.edit().putString(KEY_PLANS, json).commit()
    }

    /**
     * 加载所有本地计划
     */
    fun loadAllPlans(): List<PlanItem> {
        val json = prefs.getString(KEY_PLANS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PlanItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 清除所有本地计划
     */
    fun clearAll() {
        prefs.edit().remove(KEY_PLANS).commit()
    }

    /**
     * 保存已完成节点 ID（按会话 ID 分组）
     */
    fun saveCompletedNodes(conversationId: Long, nodeIds: Set<Long>) {
        val key = KEY_COMPLETED_NODES_PREFIX + conversationId
        prefs.edit().putString(key, nodeIds.joinToString(",")).commit()
    }

    /**
     * 加载已完成节点 ID
     */
    fun loadCompletedNodes(conversationId: Long): Set<Long> {
        val key = KEY_COMPLETED_NODES_PREFIX + conversationId
        val raw = prefs.getString(key, null) ?: return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    /**
     * 生成自增 ID（本地使用负值，避免与后端 ID 冲突）
     */
    private var nextId: Long = -1L
        get() {
            val plans = loadAllPlans()
            val minId = plans.minOfOrNull { it.id } ?: 0L
            field = minOf(field, minId - 1)
            return field--
        }

    /**
     * 生成一个新的本地计划 ID
     */
    fun generateLocalId(): Long {
        return nextId
    }
}
