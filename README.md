# helloworld

一个基于 **Android 原生 (Kotlin + Jetpack Compose + Material3)** 的学习管理应用，
配合后端服务（Python FastAPI）提供完整的账号体系、会话管理、学习路径与打卡闭环。

## 当前功能

### 认证模块（Auth）

- **登录**：账号密码登录，支持 JWT 持久化
- **注册**：手机号 + 密码注册，含前端格式校验
- **忘记密码**：占位页面
- **会话管理**：基于 `SharedPreferences` 的本地登录态持久化

### 主页（Home）

- 顶部搜索栏（支持快捷搜索，可对接 AI 问答）
- 顶部问候与今日日期
- 可拖拽底部卡片（BottomSheet）：
  - 学习计划 & 打卡提醒统计卡片
  - AI 学习内容预览区域
  - 打卡提醒列表
- 背景学习文案

### 学习计划（Plan）

- **计划看板**：两列网格卡片布局，马卡龙色系
- **空状态与加载态**：友好的空提示与加载动画
- **数据来源**：支持 Mock 示例数据与后端 API 切换
- **API 对接**：
  - `GET /api/v1/conversations` — 获取会话列表
  - `GET /api/v1/learning-paths/conversations/{id}/current` — 获取当前学习路径
  - `GET /api/v1/learning-paths/{id}` — 获取学习路径详情
  - `PATCH /api/v1/learning-paths/{id}/nodes/{node_id}/state` — 更新节点状态
  - `POST /api/v1/learning-paths/{id}/checkins` — 学习打卡
  - `GET /api/v1/learning-paths/{id}/progress` — 查询学习进度
  - `POST /api/v1/conversations` — 创建新会话

### 统计（Stats）

- 当前学习进度条（打羽毛球、打乒乓球）
- 总学习进度环形图（Donut Chart）
- 学习日历（支持左右翻页、年月切换、学习时长着色热力图、今日高亮）

### 我的（Profile）

- **登录态**：用户头像、昵称编辑、用户 ID、已完成学习目标数
- **数据卡片**：累计学习天数、连续学习天数
- **通知开关**：推送提醒控制
- **退出登录**：确认对话框
- **未登录态**：头像占位、登录/注册按钮

## 项目结构

```
app/src/main/java/com/nku/helloworld/
├── MainActivity.kt                 # 主 Activity（Compose + 底部导航）
├── auth/                           # 认证模块
│   ├── LoginActivity.kt            # 登录页面（Compose）
│   ├── RegisterActivity.kt         # 注册页面（Compose）
│   ├── ForgotPasswordActivity.kt   # 忘记密码（占位）
│   ├── AuthViewModel.kt            # 认证状态管理
│   ├── SessionManager.kt           # 本地会话持久化
│   ├── api/AuthApiService.kt       # 登录/注册 API
│   ├── data/AuthRepository.kt      # 认证数据仓库
│   └── model/                      # 数据模型
├── ui/
│   ├── home/                       # 主页
│   │   ├── HomeFragment.kt         # 主页 Fragment + 全部 Compose UI
│   │   └── HomeViewModel.kt
│   ├── plan/                       # 学习计划
│   │   ├── PlanFragment.kt         # 计划 Fragment + 网格卡片
│   │   ├── PlanViewModel.kt        # 计划列表状态管理
│   │   ├── api/PlanApiService.kt   # 计划相关完整 API
│   │   └── model/PlanModels.kt     # 数据模型
│   ├── stats/                      # 统计
│   │   └── StatsFragment.kt        # 统计 Fragment + 日历 + 环形图
│   └── profile/                    # 我的
│       └── ProfileFragment.kt      # 个人中心 Fragment（含登录/未登录状态）
```

## 技术栈

| 技术 | 用途 |
|------|------|
| **Kotlin** | 开发语言 |
| **Jetpack Compose** | UI 框架 |
| **Material3** | 设计系统 |
| **OkHttp + Gson** | 网络层 |
| **ViewModel + StateFlow** | 状态管理 |
| **SharedPreferences** | 本地持久化 |
| **JWT Bearer Token** | API 鉴权 |

## 环境要求

- Android Studio（推荐最新稳定版）
- JDK 17+（按 Android Gradle Plugin 要求）
- Android SDK（以本地 `local.properties` 为准）

## 快速运行  

克隆到本地之后

```bash
cd ~/helloworld
./gradlew :app:assembleDebug --no-daemon
```

构建成功后，可在 Android Studio 中直接运行 `app` 模块到模拟器或真机。

> **注意**：后端服务默认地址为 `http://10.0.2.2:8000`（模拟器中指向宿主机 localhost），
> 如需连接远程服务，请修改 `AuthApiService.kt` 和 `PlanApiService.kt` 中的 `BASE_URL`。

## 常用命令

```bash
# 构建 Debug
./gradlew :app:assembleDebug --no-daemon

# 清理构建缓存
./gradlew clean
```

## 后端接口

完整的后端 API 文档见 [`api.md`](https://github.com/moonClll/aigc_services/blob/main/api.md)，涵盖：

- 账号体系（注册/登录/JWT）
- 会话管理（创建/查询/历史）
- 消息与问答（提问/反馈/重答）
- 任务分发（Claim/Heartbeat/结果轮询）
- 模型回调（成功/失败）
- 学习路径（节点状态/打卡/进度/事件时间线）

## 说明

- 仓库已配置 `.gitignore`，会忽略构建产物、IDE 本地文件与系统临时文件。
- 首页搜索栏的 AI 问答功能需要配置 API URL 和 Token（参考 `HomeFragment.kt` 中的 `sendAiRequest`）。
- 所有页面均采用 Compose 构建，无 XML 布局文件。
