package com.pingpang.app.ui.mine

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pingpang.app.ai.AppPrefs

/**
 * AI 设置（F13 增强）：
 * 默认使用内置 opencode MiMo-V2.5（密钥内置，界面不显示）；
 * 也可切换自定义 OpenAI 兼容源（baseUrl / API Key / 模型名）。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var source by remember { mutableStateOf(AppPrefs.getAiSource(context)) }
    var baseUrl by remember { mutableStateOf(AppPrefs.getAiBaseUrl(context)) }
    var apiKey by remember { mutableStateOf(AppPrefs.getAiApiKey(context)) }
    var model by remember { mutableStateOf(AppPrefs.getAiModel(context)) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("‹ 返回") }
            Text("AI 设置", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
        }

        // 内置模型（默认）
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(
                selected = source != "CUSTOM",
                onClick = { source = "BUILTIN" },
            )
            Column {
                Text("内置模型：${AppPrefs.BUILTIN_AI_LABEL}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "opencode Go 服务 · OpenAI 兼容 · 密钥已内置（界面不显示），开箱即用",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // 自定义 AI 源
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(
                selected = source == "CUSTOM",
                onClick = { source = "CUSTOM" },
            )
            Text("自定义 AI 源（OpenAI 兼容）", style = MaterialTheme.typography.bodyMedium)
        }

        if (source == "CUSTOM") {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("接口地址（chat/completions）") },
                placeholder = { Text("https://api.deepseek.com/v1/chat/completions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None
                else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    OutlinedButton(onClick = { showKey = !showKey }) { Text(if (showKey) "隐藏" else "显示") }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("模型名") },
                placeholder = { Text("deepseek-chat") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = {
                if (source == "CUSTOM") {
                    if (baseUrl.isBlank() || apiKey.isBlank()) {
                        Toast.makeText(context, "接口地址和 API Key 必填", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    AppPrefs.setCustomAiConfig(context, baseUrl.trim(), apiKey.trim(), model.trim().ifBlank { "deepseek-chat" })
                } else {
                    AppPrefs.useBuiltinAi(context)
                }
                Toast.makeText(context, "已保存 AI 配置", Toast.LENGTH_SHORT).show()
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存配置")
        }
    }
}
