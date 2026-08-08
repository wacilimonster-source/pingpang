# 乒乓训练助手 PingPang 🏓

个人使用的 Android 乒乓球训练学习工具：训练计划制定与记录、技术细节记录、训练视频本地存储与多视频比对回放、对战技战术分析与总结。**全部数据本地存储**，无账号体系。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言/UI | Kotlin + Jetpack Compose (Material 3) |
| 数据库 | Room (SQLite) |
| 视频播放 | Media3 ExoPlayer（多实例同屏播放） |
| 视频录制 | CameraX |
| 网络 | OkHttp（GitHub 更新检查 / AI 调用） |
| AI | 云端 LLM（OpenAI 兼容协议，用户自配 Key） |
| 分发 | APK 侧载 + GitHub Releases 更新 |

## 项目结构

```
pingpang/
├── app/
│   └── src/main/
│       ├── java/com/pingpang/app/
│       │   ├── PingPangApp.kt          # Application（初始化 Room）
│       │   ├── MainActivity.kt         # 底部导航（首页/计划/视频/我的）
│       │   ├── data/
│       │   │   ├── model/Entities.kt   # 7 张表实体
│       │   │   └── db/                 # AppDatabase + DAO
│       │   ├── ui/                     # Compose 页面（home/plan/video/mine + theme）
│       │   ├── ai/                     # AiPlanService（生成计划）+ AppPrefs
│       │   └── update/                 # UpdateChecker + UpdateInstaller
│       └── res/                        # 资源（strings/themes/图标/FileProvider 路径）
├── gradle/libs.versions.toml           # 版本目录
└── build.gradle.kts / settings.gradle.kts
```

## 构建运行

1. 安装 Android Studio（Ladybug 及以上）
2. 打开本目录，等待 Gradle 同步（首次需下载依赖）
3. 连接 Android 设备（Android 8.0+）或模拟器，运行 `app` 配置

> 命令行构建：`./gradlew assembleRelease`（需要本机配置 Android SDK 与 JDK 17）

## 检查更新机制（GitHub Releases）

App 内置检查更新：`我的 → 检查更新（GitHub）`。

- 请求 `https://api.github.com/repos/wacilimonster-source/pingpang/releases/latest`
- 解析 `tag_name`（如 `v0.1.1`）与本地 `versionName` 比对
- 有新版本 → 弹窗提示 → 下载 Release 资产中的 `.apk`（带进度条）→ 拉起系统安装器
- Android 8+ 首次安装需在系统设置中允许"安装未知应用"（App 会自动引导跳转）

## 发布新版本（发版必读）

1. **修改版本号**：`app/build.gradle.kts` 中同步更新
   ```kotlin
   versionCode = 2        // 每次 +1
   versionName = "0.1.1"  // 与 tag 一致（不含 v）
   ```
2. **构建 APK**：Android Studio `Build → Build App Bundle(s) / APK(s) → Build APK(s)`，产物在 `app/build/outputs/apk/release/`
3. **打 tag 并推送**：
   ```bash
   git add -A && git commit -m "v0.1.1"
   git tag v0.1.1
   git push origin main --tags
   ```
4. **创建 Release**：GitHub 仓库页面 `Releases → Draft a new release`，选择标签 `v0.1.1`，填写更新说明，**上传 APK 文件**（资产名随意，以 `.apk` 结尾即可）
5. 用户端点击"检查更新"即可发现新版本

> 注意：GitHub API 未认证时限 60 次/小时/IP，个人使用足够；若需更高额度可在 App 中增加 Token 配置（后续版本）。

## 功能状态

| 功能 | 状态 |
|---|---|
| 底部导航 + 四页面框架 | ✅ 骨架完成 |
| Room 数据层（7 表） | ✅ 完成 |
| GitHub 检查更新 + 下载 + 安装 | ✅ 完成 |
| 阶段计划 → 周计划（手动/AI 生成） | 🚧 计划页骨架，逻辑待实现 |
| 训练打卡与量化记录 | 🚧 待实现 |
| 视频导入/录制 + 播放器 | 🚧 待实现（Media3 已接入依赖） |
| 多视频比对工作台（缩放对齐） | 🚧 待实现（MVP 核心亮点） |
| 技术档案 / 对战分析 | 🚧 V1.1 |
| 一键导出备份（SAF） | 🚧 待实现 |
| AI 配置对话框 | 🚧 待实现 |

## 隐私

- 所有数据（数据库、视频、照片）存储在设备本地，无云端上传
- AI 功能仅在用户主动调用时发送必要文本；视频文件不会上传
- 无任何统计/追踪 SDK
