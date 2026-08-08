package com.pingpang.app.ui.mine

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pingpang.app.BuildConfig
import com.pingpang.app.ai.AppPrefs
import com.pingpang.app.update.UpdateChecker
import com.pingpang.app.update.UpdateInstaller
import kotlinx.coroutines.launch

/** 更新流程状态 */
private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateChecker.ReleaseInfo) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data object Installing : UpdateState
    data class Failed(val reason: String) : UpdateState
}

@Composable
fun MineScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall)

        // ---------- 版本与更新 ----------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                    is UpdateState.Downloading ->
                        Column {
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
                            val info = try {
                                UpdateChecker.checkLatest()
                            } catch (e: Exception) {
                                state = UpdateState.Failed(e.message ?: "网络错误")
                                return@launch
                            }
                            if (info == null) {
                                state = UpdateState.UpToDate
                            } else if (UpdateChecker.hasNewVersion(info.versionName, BuildConfig.VERSION_NAME)) {
                                state = UpdateState.Available(info)
                            } else {
                                state = UpdateState.UpToDate
                            }
                            AppPrefs.setLastCheckTime(context, System.currentTimeMillis())
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
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text(
                        "AI 设置（制定计划 / 复盘总结）",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    if (AppPrefs.isAiConfigured(context)) "已配置：${AppPrefs.getAiModel(context)}"
                    else "未配置。需填写 baseUrl / API Key / 模型名（DeepSeek、通义千问、智谱等均可）",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = { /* TODO V1.0：AI 配置对话框 */ }) {
                    Text("配置 AI")
                }
            }
        }

        // ---------- 备份 ----------
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, contentDescription = null)
                    Text(
                        "数据备份",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    "导出 SQLite 数据库 + 视频目录为 zip，可存网盘 / Documents 防止换机丢失",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = { /* TODO V1.0：SAF 导出备份 */ }) {
                    Text("一键导出备份")
                }
            }
        }

        HorizontalDivider()
        Text("数据全部存储在本地，无账号体系", style = MaterialTheme.typography.bodySmall)
    }

    // ---------- 更新确认与下载安装对话框 ----------
    val available = (state as? UpdateState.Available)?.info
    if (available != null) {
        AlertDialog(
            onDismissRequest = { state = UpdateState.Idle },
            title = { Text("发现新版本 ${available.versionName}") },
            text = {
                Column {
                    Text("更新说明：")
                    Text(available.notes.ifBlank { "（无说明）" }, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    state = UpdateState.Downloading(0)
                    scope.launch {
                        try {
                            val pm = context.packageManager
                            if (android.os.Build.VERSION.SDK_INT >= 26 &&
                                !pm.canRequestPackageInstalls()
                            ) {
                                // 引导授权"安装未知应用"
                                UpdateInstaller.openInstallPermissionSettings(
                                    context, context.packageName,
                                )
                                state = UpdateState.Idle
                                Toast.makeText(
                                    context,
                                    "请先在系统设置中允许本应用安装未知应用",
                                    Toast.LENGTH_LONG,
                                ).show()
                                return@launch
                            }
                            val apk = UpdateInstaller.downloadApk(
                                context,
                                available.apkUrl,
                                available.apkName,
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
