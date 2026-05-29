package com.nku.helloworld.auth.model

/**
 * 用户信息数据模型
 */
data class UserProfile(
    val id: Int = 0,
    val nickname: String = "用户",
    val phone: String = "",
    val accessToken: String = "",
    /** 已完成学习目标数量 */
    val completedGoals: Int = 12,
    /** 累计学习天数 */
    val totalDays: Int = 68,
    /** 连续学习天数 */
    val streakDays: Int = 7
)
