# 主页计划 (helloworld)

一个基于 **Android 原生 (Kotlin + Jetpack)** 的学习管理示例应用。

## 当前功能

- 主页：搜索栏、顶部问候、学习背景文案、可拖拽底部卡片
- 计划：中文计划看板与任务列表
- 统计：占位页面（后续可接入图表）
- 我的：占位页面（后续可接入个人中心）

## 项目结构

- `app/src/main/java/com/nku/helloworld/`：应用代码
- `app/src/main/res/`：布局、字符串、图标与主题资源
- `app/src/main/res/navigation/mobile_navigation.xml`：底部导航路由
- `app/src/main/res/menu/bottom_nav_menu.xml`：底部 Tab 菜单

## 环境要求

- Android Studio（推荐最新稳定版）
- JDK 17+（按 Android Gradle Plugin 要求）
- Android SDK（以本地 `local.properties` 为准）

## 快速运行

```bash
cd /home/LittleWatter/IdeaProjects/helloworld
./gradlew :app:assembleDebug --no-daemon
```

构建成功后，可在 Android Studio 中直接运行 `app` 模块到模拟器或真机。

## 常用命令

```bash
# 构建 Debug
./gradlew :app:assembleDebug --no-daemon

# 清理构建缓存
./gradlew clean
```

## 说明

- 仓库已配置 `.gitignore`，会忽略构建产物、IDE 本地文件与系统临时文件。
- `统计` 与 `我的` 页面当前为占位实现，后续可按业务继续扩展。

