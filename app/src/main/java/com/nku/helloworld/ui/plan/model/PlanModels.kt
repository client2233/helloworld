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


// ============================================================
// 创建学习计划相关模型
// ============================================================

/**
 * 问题提交请求体
 * POST /api/v1/messages/question
 */
data class QuestionSubmitRequest(
    val conversation_id: Long,
    val content_text: String,
    val request_id: String
)

/**
 * 问题提交响应数据
 */
data class QuestionSubmitData(
    val id: Long,
    val conversation_id: Long,
    val role: String = "user",
    val message_type: String = "question",
    val content_text: String,
    val request_id: String? = null,
    val parent_message_id: Long? = null,
    val meta_json: Any? = null,
    val created_at: String = "",
    val assets: List<Any> = emptyList(),
    val generation_task_id: Long = 0
)

/**
 * 任务结果响应 data
 * GET /api/v1/tasks/{task_id}/result
 */
data class TaskResultData(
    val task: TaskInfoData = TaskInfoData(),
    val answer_ready: Boolean = false,
    val answer_message: AnswerMessageData? = null
)

data class TaskInfoData(
    val id: Long = 0,
    val conversation_id: Long = 0,
    val status: String = "",
    val error_message: String? = null
)

data class AnswerMessageData(
    val id: Long = 0,
    val conversation_id: Long = 0,
    val role: String = "",
    val message_type: String = "",
    val content_text: String = "",
    val meta_json: Map<String, Any>? = null,
    val assets: List<Any> = emptyList()
)

/**
 * 学习路径元数据（从 meta_json 中解析）
 */
data class LearningPathMeta(
    val title: String = "",
    val goal: String = "",
    val nodes: List<LearningPathNodeMeta> = emptyList()
)

data class LearningPathNodeMeta(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val status: String = "locked"
)



// ============================================================
// 聊天对话模型（AI 对话创建学习计划）
// ============================================================

/**
 * 聊天消息，用于 AI 对话式创建学习计划
 *
 * role: "user" 为用户消息，"assistant" 为 AI 回复
 * isLoading: 用于显示 AI 正在输入的占位消息
 * learningPath: 当 AI 回复包含学习路径时，附带解析后的路径数据
 */
data class ChatMessage(
    val id: String = "",
    val role: String = "user",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val learningPath: LearningPathMeta? = null
)
