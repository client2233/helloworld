package com.nku.helloworld.ui.plan.model

/**
 * 学习计划卡片数据模型
 */
data class PlanItem(
    val id: Long = 0,
    val title: String,
    /** 最新执行/关联日期，展示在卡片底部第一行 */
    val latestDate: String? = null,
    /** 创建日期，展示在卡片底部第二行 */
    val createdDate: String? = null,
    /** 进度 0.0 ~ 1.0 */
    val progress: Float = 0f,
    /** 卡片颜色索引（用于从预定义调色板中选择） */
    val colorIndex: Int = 0,
    /** 关联的会话 ID */
    val conversationId: Long? = null,
    /** 关联的学习路径 ID */
    val pathId: Long? = null
)

/**
 * 会话列表响应（来自 /api/v1/conversations）
 */
data class ConversationItem(
    val id: Long,
    val title: String,
    val created_at: String,
    val updated_at: String
)

/**
 * 学习路径详情（来自 /api/v1/learning-paths/{path_id}）
 */
data class LearningPath(
    val id: Long,
    val title: String,
    val goal: String = "",
    val nodes: List<PathNode> = emptyList(),
    val created_at: String? = null
)

/**
 * 学习路径节点
 */
data class PathNode(
    val id: Long,
    val title: String,
    val status: String = "locked",  // locked, available, in_progress, done
    val question: String? = null
)

/**
 * 学习进度汇总（来自 /api/v1/learning-paths/{path_id}/progress）
 */
data class LearningProgress(
    val totalNodes: Int = 0,
    val completedNodes: Int = 0,
    val completionRate: Float = 0f,
    val checkinDays: Int = 0
)
