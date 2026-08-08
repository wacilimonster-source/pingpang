package com.pingpang.app.ui.mine

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pingpang.app.BuildConfig
import com.pingpang.app.ai.AppPrefs
import com.pingpang.app.data.BackupManager
import com.pingpang.app.update.UpdateChecker
import com.pingpang.app.update.UpdateInstaller
import kotlinx.coroutines.launch

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateChecker.VersionInfo) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data object Installing : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/**
 * 我的（F11/F12/F13）：检查更新、AI 配置入口、备份导出、关于。
 */
@Composable
fun MineScreen(
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var backupProgress by remember { mutableStateOf<String?>(null) }

    // 备份：SAF 选择保存位置
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            backupProgress = "正在打包…"
            scope.launch {
                try {
                    val count = BackupManager.export(context, uri)
                    backupProgress = null
                    Toast.makeText(context, "备份完成（$count 个文件）", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    backupProgress = null
                    Toast.makeText(context, "备份失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall)

        // ---------- 版本与更新 ----------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Build, contentDescription = null)
                    Text(
                        "当前版本 ${BuildConfig.VERSION_NAME}",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                when (val s = state) {
                    is UpdateState.Idle -> Unit
                    is UpdateState.Checking -> Text("正在检查更新…")
                    is UpdateState.UpToDate -> Text("已是最新版本")
                    is UpdateState.Available -> Text("发现新版本 ${s.info.versionName}")
                    is UpdateState.Downloading -> Column {
                        Text("正在下载更新… ${s.percent}%")
                        LinearProgressIndicator(
                            progress = { s.percent / 100f },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                    is UpdateState.Installing -> Text("下载完成，正在启动安装…")
                    is UpdateState.Failed -> Text("检查更新失败：${s.reason}")
                }

                Button(
                    onClick = {
                        state = UpdateState.Checking
                        scope.launch {
                            val info = UpdateChecker.fetchVersion()
                            if (info == null) {
                                state = UpdateState.Failed("网络错误或版本信息不存在")
                            } else if (UpdateChecker.hasNewVersion(info, BuildConfig.VERSION_CODE)) {
                                state = UpdateState.Available(info)
                            } else {
                                state = UpdateState.UpToDate
                            }
                        }
                    },
                    enabled = state !is UpdateState.Checking &&
                        state !is UpdateState.Downloading &&
                        state !is UpdateState.Installing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("检查更新（GitHub）")
                }
            }
        }

        // ---------- AI 设置 ----------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text(
                        "AI 设置（制定计划 / 训练复盘）",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    if (AppPrefs.isAiConfigured(context)) "已配置：${AppPrefs.getAiModel(context)}"
                    else "未配置。需填写接口地址、API Key、模型名",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("配置 AI")
                }
            }
        }

        // ---------- 备份 ----------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, contentDescription = null)
                    Text(
                        "数据备份",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    backupProgress ?: "导出数据库 + 视频为 zip，可存网盘 / Documents 防丢失",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = { backupLauncher.launch(BackupManager.suggestFileName()) },
                    enabled = backupProgress == null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("一键导出备份")
                }
            }
        }

        // ---------- 关于 ----------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Text("关于", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
                Text("乒乓训练助手 · 数据全部本地存储 · 无账号体系", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wacilimonster-source/pingpang"))
                    context.startActivity(intent)
                }) {
                    Text("GitHub 仓库")
                }
            }
        }

        HorizontalDivider()
        Text("更新与备份说明", style = MaterialTheme.typography.bodySmall)
        Text(
            "更新：App 从 GitHub raw 读取 version.txt，versionCode 高于本机时提示下载安装。\n" +
                "发版：修改版本号 → 构建 APK 上传仓库 → 更新 version.txt → 推送 GitHub。",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    // ---------- 更新确认 ----------
    val available = (state as? UpdateState.Available)?.info
    if (available != null) {
        AlertDialog(
            onDismissRequest = { state = UpdateState.Idle },
            title = { Text("发现新版本 ${available.versionName}") },
            text = {
                Column {
                    Text("更新说明：")
                    Text(available.updateMessage.ifBlank { "（无说明）" }, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    state = UpdateState.Downloading(0)
                    scope.launch {
                        try {
                            val pm = context.packageManager
                            if (android.os.Build.VERSION.SDK_INT >= 26 && !pm.canRequestPackageInstalls()) {
                                UpdateInstaller.openInstallPermissionSettings(context, context.packageName)
                                state = UpdateState.Idle
                                Toast.makeText(
                                    context,
                                    "请先在系统设置中允许本应用安装未知应用，再重新点击检查更新",
                                    Toast.LENGTH_LONG,
                                ).show()
                                return@launch
                            }
                            val fileName = available.apkDownloadUrl.substringAfterLast('/').ifBlank { "pingpang.apk" }
                            val apk = UpdateInstaller.downloadApk(
                                context,
                                available.apkDownloadUrl,
                                fileName,
                                { p -> state = UpdateState.Downloading(p) },
                            )
                            state = UpdateState.Installing
                            UpdateInstaller.installApk(context, apk)
                            state = UpdateState.Idle
                        } catch (e: Exception) {
                            state = UpdateState.Failed(e.message ?: "下载失败")
                        }
                    }
                }) { Text("立即更新") }
            },
            dismissButton = {
                TextButton(onClick = { state = UpdateState.Idle }) { Text("稍后再说") }
            },
        )
    }
}
