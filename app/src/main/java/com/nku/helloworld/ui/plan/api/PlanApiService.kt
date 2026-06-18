package com.nku.helloworld.ui.plan.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nku.helloworld.AppConfig
import com.nku.helloworld.auth.model.ApiResponse
import com.nku.helloworld.ui.plan.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 学习计划 API 服务
 *
 * 对应后端接口文件：
 * - conversations.py：会话列表、创建会话
 * - learning.py：学习路径相关
 *
 * 所有业务接口均使用统一响应格式 {code, message, data}，
 * 前端通过 Authorization: Bearer <token> 鉴权。
 *
 * 参考 api.md 中的接口定义：
 * - GET    /api/v1/conversations                   分页查询会话列表
 * - POST   /api/v1/conversations                   创建会话
 * - GET    /api/v1/learning-paths/conversations/{conversation_id}/current  获取当前学习路径
 * - GET    /api/v1/learning-paths/{path_id}         获取学习路径详情
 * - PATCH  /api/v1/learning-paths/{path_id}/nodes/{node_id}/state  更新节点状态
 * - POST   /api/v1/learning-paths/{path_id}/checkins  学习打卡
 * - GET    /api/v1/learning-paths/{path_id}/progress   查询学习进度汇总
 */
object PlanApiService {

    /** 从配置文件中读取的后端服务器基础地址 */
    private val BASE_URL get() = AppConfig.baseUrl

    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.connectTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(AppConfig.readTimeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(AppConfig.writeTimeout.toLong(), TimeUnit.SECONDS)
            .build()
    }
    /**
     * 获取会话列表（用于展示学习计划列表）
     *
     * GET /api/v1/conversations
     *
     * 响应 data 为会话数组，每个会话可作为一条学习计划卡片展示。
     * 前端可通过 conversation_id 进一步调用 current-learning-path 获取详情。
     */
    suspend fun getConversations(token: String): Result<List<ConversationItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/conversations")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<List<ConversationItem>>>() {}.type
                val apiResponse: ApiResponse<List<ConversationItem>> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(apiResponse.data ?: emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    /**
     * 获取当前学习路径（基于会话）
     *
     * GET /api/v1/learning-paths/conversations/{conversation_id}/current
     *
     * 用于点击卡片后进入详情页时获取该会话的最新学习路径。
     */
    suspend fun getCurrentLearningPath(
        token: String,
        conversationId: Long
    ): Result<LearningPath> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/learning-paths/conversations/$conversationId/current")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<LearningPath>>() {}.type
                val apiResponse: ApiResponse<LearningPath> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(
                    apiResponse.data ?: throw Exception("学习路径数据为空")
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 获取学习路径详情
     *
     * GET /api/v1/learning-paths/{path_id}
     *
     * 直接按 path_id 获取完整的学习路径信息（含节点列表）。
     */
    suspend fun getLearningPathDetail(
        token: String,
        pathId: Long
    ): Result<LearningPath> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/learning-paths/$pathId")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<LearningPath>>() {}.type
                val apiResponse: ApiResponse<LearningPath> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(
                    apiResponse.data ?: throw Exception("学习路径详情为空")
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 更新节点状态
     *
     * PATCH /api/v1/learning-paths/{path_id}/nodes/{node_id}/state
     *
     * 用于用户在详情页中标记节点为 "in_progress" 或 "done"。
     * 请求体: {"state": "done"}
     */
    suspend fun updateNodeState(
        token: String,
        pathId: Long,
        nodeId: Long,
        newState: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(mapOf("state" to newState))
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/learning-paths/$pathId/nodes/$nodeId/state")
                    .patch(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<Unit>>() {}.type
                val apiResponse: ApiResponse<Unit> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 学习打卡
     *
     * POST /api/v1/learning-paths/{path_id}/checkins
     *
     * 请求体支持 request_id 实现幂等，避免重复打卡。
     */
    suspend fun checkIn(
        token: String,
        pathId: Long,
        requestId: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(mapOf("request_id" to requestId))
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/learning-paths/$pathId/checkins")
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<Unit>>() {}.type
                val apiResponse: ApiResponse<Unit> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 查询学习进度汇总
     *
     * GET /api/v1/learning-paths/{path_id}/progress
     *
     * 获取已完成节点数、完成率、打卡天数等统计数据。
     */
    suspend fun getProgress(
        token: String,
        pathId: Long
    ): Result<LearningProgress> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/learning-paths/$pathId/progress")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<LearningProgress>>() {}.type
                val apiResponse: ApiResponse<LearningProgress> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(apiResponse.data ?: LearningProgress())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 创建会话（用于新建学习计划）
     *
     * POST /api/v1/conversations
     *
     * 当用户从首页或快捷入口创建新学习计划时调用。
     * 请求体: {"title": "...", "request_id": "..."}
     */
    suspend fun createConversation(
        token: String,
        title: String,
        requestId: String
    ): Result<ConversationItem> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(
                    mapOf("title" to title, "request_id" to requestId)
                )
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/conversations")
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<ConversationItem>>() {}.type
                val apiResponse: ApiResponse<ConversationItem> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(
                    apiResponse.data ?: throw Exception("创建会话失败")
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ============================================================
    // 创建学习计划相关 API
    // ============================================================

    /**
     * 提交问题（用于创建学习计划）
     *
     * POST /api/v1/messages/question
     *
     * 用户输入想学习的内容，提交为问题消息。
     * 服务端会创建生成任务，返回 generation_task_id 供前端轮询。
     *
     * 参考 api.md 中的「前端提交问题样例」
     */
    suspend fun submitQuestion(
        token: String,
        request: QuestionSubmitRequest
    ): Result<QuestionSubmitData> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(request)
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v1/messages/question")
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(requestBuilder).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<QuestionSubmitData>>() {}.type
                val apiResponse: ApiResponse<QuestionSubmitData> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(
                    apiResponse.data ?: throw Exception("提交问题返回数据为空")
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 查询任务结果（轮询用）
     *
     * GET /api/v1/tasks/{task_id}/result
     *
     * 返回任务状态 + answer_ready + 最终答案。
     * 当 answer_ready 为 true 时，answer_message 包含学习路径数据。
     *
     * 参考 api.md 中的「前端轮询任务结果样例」
     */
    suspend fun getTaskResult(
        token: String,
        taskId: Long
    ): Result<TaskResultData> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v1/tasks/$taskId/result")
                    .get()
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(requestBuilder).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("HTTP ${response.code}: $responseBody")
                    )
                }

                val type = object : TypeToken<ApiResponse<TaskResultData>>() {}.type
                val apiResponse: ApiResponse<TaskResultData> =
                    gson.fromJson(responseBody, type)

                if (apiResponse.code != 0) {
                    return@withContext Result.failure(Exception(apiResponse.message))
                }

                Result.success(
                    apiResponse.data ?: throw Exception("任务结果数据为空")
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



}
