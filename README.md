# 乒乓训练助手 PingPang 🏓

个人使用的 Android 乒乓球训练学习工具：训练计划制定与记录、技术细节记录、训练视频本地存储与多视频比对回放、对战技战术分析与总结。**全部数据本地存储**，无账号体系。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言/UI | Kotlin + Jetpack Compose (Material 3) |
| 数据库 | Room (SQLite)，v2（AI 复盘草稿 + 查询索引），含损坏自愈 |
| 视频播放 | Media3 ExoPlayer（多实例同屏播放） |
| 视频录制 | CameraX（带录制计时） |
| 网络 | OkHttp（GitHub 更新检查 / AI 调用） |
| AI | 云端 LLM（OpenAI 兼容协议，用户自配 Key） |
| 分发 | APK 侧载 + GitHub Releases 更新 |

## 项目结构

```
pingpang/
├── app/
│   └── src/
│       ├── main/java/com/pingpang/app/
│       │   ├── PingPangApp.kt          # Application（初始化 Room + 坏库自愈）
│       │   ├── MainActivity.kt         # 底部导航（首页/计划/视频/我的）
│       │   ├── data/
│       │   │   ├── model/Entities.kt   # 7 张表实体
│       │   │   ├── db/                 # AppDatabase(v2) + DAO + Migration + 自愈
│       │   │   ├── BackupManager.kt    # 备份导出（zip + manifest 清单）
│       │   │   ├── JsonUtils.kt / TrainingTemplates.kt / PlanParser.kt
│       │   ├── ui/                     # Compose 页面（home/plan/video/mine + theme）
│       │   ├── ai/                     # AiPlanService（生成计划/复盘）+ AppPrefs
│       │   └── update/                 # UpdateChecker + UpdateInstaller
│       ├── test/                       # JVM 单元测试（JsonUtils / TrainingTemplates / PlanParser）
│       └── res/                        # 资源（strings/themes/图标/FileProvider 路径）
├── gradle/libs.versions.toml           # 版本目录
├── gradle/wrapper/ + gradlew.bat       # Gradle Wrapper（8.6）
└── build.gradle.kts / settings.gradle.kts
```

## 构建运行

1. 安装 Android Studio（Ladybug 及以上）
2. 打开本目录，等待 Gradle 同步（首次需下载依赖）
3. 连接 Android 设备（Android 8.0+）或模拟器，运行 `app` 配置

命令行构建（本机需配置 Android SDK 与 JDK 17）：

```bash
./gradlew assembleDebug     # 调试包
./gradlew assembleRelease   # 发布包（debug 签名，可直接安装）
./gradlew test              # 运行 JVM 单元测试
./gradlew lint              # 静态检查
```

发布包产物：`app/build/outputs/apk/release/app-release.apk`（R8 裁剪 + 资源收缩）。

## 检查更新机制（GitHub Releases）

App 内置检查更新：`我的 → 检查更新（GitHub）`。

- 请求 `https://api.github.com/repos/wacilimonster-source/pingpang/releases/latest`
- 解析 `tag_name`（如 `v0.2.0`）与本地 `versionName` 比对
- 有新版本 → 弹窗提示 → 下载 Release 资产中的 `.apk`（带进度条）→ 拉起系统安装器
- Android 8+ 首次安装需在系统设置中允许"安装未知应用"（App 会自动引导跳转）

## 发布新版本（发版必读）

1. **修改版本号**：`app/build.gradle.kts` 中同步更新
   ```kotlin
   versionCode = 3        // 每次 +1
   versionName = "0.2.1"  // 与 tag 一致（不含 v）
   ```
2. **构建 APK**：`./gradlew assembleRelease`，产物在 `app/build/outputs/apk/release/`
3. **打 tag 并推送**：
   ```bash
   git add -A && git commit -m "v0.2.1"
   git tag v0.2.1
   git push origin main --tags
   ```
4. **创建 Release**：GitHub 仓库页面 `Releases → Draft a new release`，选择标签 `v0.2.1`，填写更新说明，**上传 APK 文件**（资产名随意，以 `.apk` 结尾即可）
5. 用户端点击"检查更新"即可发现新版本

> 注意：GitHub API 未认证时限 60 次/小时/IP，个人使用足够。

## 功能状态

| 功能 | 状态 |
|---|---|
| 底部导航 + 四页面框架 | ✅ 完成 |
| Room 数据层（7 表，v2：AI 复盘草稿 + 索引 + Migration） | ✅ 完成 |
| 数据库损坏自愈（quick_check → 备份坏库 → 重建） | ✅ 完成 |
| 阶段计划 → 周计划（手动/AI 生成） | ✅ 完成 |
| 训练打卡与量化记录 + 照片/视频关联 + 编辑/删除 | ✅ 完成 |
| AI 训练复盘（生成 + 草稿持久化 + 采纳为笔记） | ✅ 完成 |
| 首页今日训练卡片 + 阶段进度 | ✅ 完成 |
| 视频导入（含重复检测 + 存储检查）/ 录制（计时）/ 播放器 | ✅ 完成 |
| 多视频比对工作台（独立缩放、逐路移除、日期叠加） | ✅ 完成 |
| 视频库搜索 + 标签筛选 + 照片全屏预览 | ✅ 完成 |
| 一键导出备份（SAF zip + manifest 进度） | ✅ 完成 |
| GitHub 检查更新 + 下载 + 安装 | ✅ 完成 |
| JVM 单元测试（数据工具层） | ✅ 完成 |
| 技术档案 / 对战分析 | 🚧 V1.1 规划 |
| 备份恢复（zip → 还原） | 🚧 V1.1 规划 |

## 隐私

- 所有数据（数据库、视频、照片）存储在设备本地，无云端上传
- AI 功能仅在用户主动调用时发送必要文本；视频文件不会上传
- 无任何统计/追踪 SDK
