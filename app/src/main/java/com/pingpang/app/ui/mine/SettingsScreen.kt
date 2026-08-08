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
 * AI 设置（F13）：baseUrl / API Key / 模型名，本地存储。
 * 兼容 OpenAI 协议（DeepSeek / 通义千问 / 智谱 等）。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

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

        Text(
            "用于「AI 制定计划」和「AI 训练复盘」。兼容 DeepSeek / 通义千问 / 智谱等 OpenAI 协议接口。Key 只保存在本机。",
            style = MaterialTheme.typography.bodySmall,
        )

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

        Button(
            onClick = {
                if (baseUrl.isBlank() || apiKey.isBlank()) {
                    Toast.makeText(context, "接口地址和 API Key 必填", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                AppPrefs.setAiConfig(context, baseUrl.trim(), apiKey.trim(), model.trim().ifBlank { "deepseek-chat" })
                Toast.makeText(context, "已保存 AI 配置", Toast.LENGTH_SHORT).show()
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存配置")
        }
    }
}
