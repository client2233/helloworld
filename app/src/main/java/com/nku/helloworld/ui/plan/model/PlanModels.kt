package com.nku.helloworld.ui.plan.model

import com.google.gson.annotations.SerializedName

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
    val question: String? = null,
    /** 节点类型：lesson, practice, checkpoint, resource */
    val nodeType: String = "",
    /** 预计学习分钟数 */
    val estMinutes: Int = 0,
    /** 在树中的深度层级（0=根） */
    val depth: Int = 0
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
// 学习路径 API 响应模型（匹配实际后端返回）
// ============================================================

/**
 * 学习路径详情 API 响应 data
 */
data class LearningPathDetailData(
    val path: LearningPathOut,
    val nodes: List<LearningNodeOut>
)

/**
 * 学习路径输出（来自 /api/v1/learning-paths/{path_id} 的 path 字段）
 *
 * 字段使用 snake_case 以匹配后端 JSON 响应。
 */
data class LearningPathOut(
    val id: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    @SerializedName("conversation_id")
    val conversationId: Long? = null,
    @SerializedName("source_task_id")
    val sourceTaskId: Long? = null,
    @SerializedName("source_message_id")
    val sourceMessageId: Long? = null,
    @SerializedName("version_no")
    val versionNo: Long? = null,
    val title: String? = null,
    val goal: String? = null,
    @SerializedName("summary_json")
    val summaryJson: Map<String, Any?>? = null,
    val status: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * 学习路径节点输出（来自 /api/v1/learning-paths/{path_id} 的 nodes 数组）
 */
data class LearningNodeOut(
    val id: Long? = null,
    @SerializedName("path_id")
    val pathId: Long? = null,
    @SerializedName("node_code")
    val nodeCode: String? = null,
    @SerializedName("parent_node_id")
    val parentNodeId: Long? = null,
    val title: String? = null,
    @SerializedName("node_type")
    val nodeType: String? = null,
    val description: String? = null,
    @SerializedName("est_minutes")
    val estMinutes: Long? = null,
    @SerializedName("sort_no")
    val sortNo: Long? = null,
    @SerializedName("unlock_rule_json")
    val unlockRuleJson: Map<String, Any?>? = null,
    @SerializedName("content_json")
    val contentJson: Map<String, Any?>? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("user_state")
    val userState: LearningNodeStateOut? = null
)

/**
 * 学习节点用户状态
 */
data class LearningNodeStateOut(
    val id: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    @SerializedName("path_id")
    val pathId: Long? = null,
    @SerializedName("node_id")
    val nodeId: Long? = null,
    val state: String? = null,  // locked | available | in_progress | done
    @SerializedName("progress_percent")
    val progressPercent: Long? = null,
    @SerializedName("started_at")
    val startedAt: String? = null,
    @SerializedName("completed_at")
    val completedAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
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
 *
 * 支持两种格式：
 * 1. learning_path_v1: { title, goal, nodes: [LearningPathNodeMeta] }
 * 2. decompose_v1:     { summary_text, key_points, sub_questions: [SubQuestionMeta] }
 */
data class LearningPathMeta(
    val title: String = "",
    val goal: String = "",
    val nodes: List<LearningPathNodeMeta> = emptyList(),
    val summaryText: String = "",
    val keyPoints: List<String> = emptyList()
)

/**
 * 学习路径节点元数据
 */
data class LearningPathNodeMeta(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val status: String = "locked",
    /** 节点类型：lesson, practice, checkpoint, resource */
    val nodeType: String = "",
    /** 预计学习分钟数 */
    val estMinutes: Int = 0
)

/**
 * decompose_v1 格式的子问题元数据
 */
data class SubQuestionMeta(
    @SerializedName("question_text")
    val questionText: String = "",
    val title: String = "",
    @SerializedName("node_type")
    val nodeType: String = "",
    val description: String = "",
    @SerializedName("est_minutes")
    val estMinutes: Int = 0
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
 * conversationId: 关联的会话 ID（用于跳转详情页）
 */
data class ChatMessage(
    val id: String = "",
    val role: String = "user",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val learningPath: LearningPathMeta? = null,
    val conversationId: Long? = null
)



// ============================================================
// QA 树节点模型（来自 api.txt 的 qa-nodes API）
// ============================================================

/**
 * 获取会话 QA 树的响应 data
 * GET /api/v1/qa-nodes/conversations/{conversation_id}/tree
 */
data class QaTreeData(
    @SerializedName("conversation_id")
    val conversationId: Long? = null,
    val nodes: List<QaTreeNode>? = null
)

/**
 * QA 树节点（递归结构，包含子节点）
 */
data class QaTreeNode(
    val node: QaNodeData? = null,
    val children: List<QaTreeNode>? = null
)

/**
 * QA 节点数据
 */
data class QaNodeData(
    val id: Long? = null,
    @SerializedName("user_id")
    val userId: Long? = null,
    @SerializedName("conversation_id")
    val conversationId: Long? = null,
    @SerializedName("parent_node_id")
    val parentNodeId: Long? = null,
    @SerializedName("root_node_id")
    val rootNodeId: Long? = null,
    val depth: Int? = null,
    @SerializedName("sort_no")
    val sortNo: Int? = null,
    val title: String? = null,
    @SerializedName("question_text")
    val questionText: String? = null,
    @SerializedName("answer_text")
    val answerText: String? = null,
    @SerializedName("summary_text")
    val summaryText: String? = null,
    val status: String? = null,
    @SerializedName("node_type")
    val nodeType: String? = null,
    val description: String? = null,
    @SerializedName("est_minutes")
    val estMinutes: Int? = null,
    @SerializedName("request_id")
    val requestId: String? = null,
    @SerializedName("generation_task_id")
    val generationTaskId: Long? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * QA 节点提交响应（包装 node + generation_task_id）
 * POST /api/v1/qa-nodes/question 的响应 data 格式
 */
data class QaNodeQuestionResponse(
    val node: QaNodeData? = null,
    @SerializedName("generation_task_id")
    val generationTaskId: Long? = null
)

/**
 * QA 节点提交请求（qa-nodes 专用）
 * POST /api/v1/qa-nodes/question
 */
data class QaNodeQuestionRequest(
    @SerializedName("conversation_id")
    val conversationId: Long,
    @SerializedName("content_text")
    val contentText: String,
    @SerializedName("parent_node_id")
    val parentNodeId: Long? = null,
    @SerializedName("node_title")
    val nodeTitle: String,
    @SerializedName("request_id")
    val requestId: String
)
