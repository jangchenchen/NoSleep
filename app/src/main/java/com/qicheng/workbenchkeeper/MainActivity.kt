package com.qicheng.workbenchkeeper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qicheng.workbenchkeeper.data.PreferenceStore
import com.qicheng.workbenchkeeper.model.AppSettings
import com.qicheng.workbenchkeeper.model.KeepAwakeDuration
import com.qicheng.workbenchkeeper.model.WorkbenchPreset
import com.qicheng.workbenchkeeper.security.SignatureVerifier
import com.qicheng.workbenchkeeper.ui.theme.WorkbenchKeeperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!SignatureVerifier.isOfficialBuild(this)) {
            Toast.makeText(this, "非官方版本，已停止运行", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val preferenceStore = PreferenceStore(applicationContext)

        setContent {
            WorkbenchKeeperTheme {
                HomeRoute(preferenceStore = preferenceStore)
            }
        }
    }
}

private fun normalizeAccessUrl(rawUrl: String): String {
    val normalized = rawUrl.trim()
    require(normalized.isNotEmpty()) { "访问URL不能为空" }

    val uri = Uri.parse(normalized)
    require(uri.scheme == "http" || uri.scheme == "https") {
        "访问URL必须以 http:// 或 https:// 开头"
    }
    require(!uri.host.isNullOrBlank()) { "访问URL缺少有效主机名" }

    return normalized
}

@Composable
private fun HomeRoute(
    preferenceStore: PreferenceStore,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by preferenceStore.settingsFlow.collectAsState(initial = AppSettings())
    val presets by preferenceStore.presetsFlow.collectAsState(initial = emptyList())

    var initialized by rememberSaveable { mutableStateOf(false) }
    var accessUrl by rememberSaveable { mutableStateOf("") }
    var presetLabel by rememberSaveable { mutableStateOf("") }
    var keepAwakeDuration by rememberSaveable { mutableStateOf(KeepAwakeDuration.default) }
    var hasAcceptedUsageNotice by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings) {
        if (!initialized) {
            keepAwakeDuration = settings.keepAwakeDuration
            hasAcceptedUsageNotice = settings.hasAcceptedUsageNotice
            initialized = true
        }
    }

    LaunchedEffect(
        initialized,
        keepAwakeDuration,
        hasAcceptedUsageNotice,
    ) {
        if (!initialized) return@LaunchedEffect
        preferenceStore.saveSettings(
            AppSettings(
                keepAwakeDuration = keepAwakeDuration,
                hasAcceptedUsageNotice = hasAcceptedUsageNotice,
            ),
        )
    }

    HomeScreen(
        accessUrl = accessUrl,
        onAccessUrlChange = { accessUrl = it },
        presetLabel = presetLabel,
        onPresetLabelChange = { presetLabel = it },
        keepAwakeDuration = keepAwakeDuration,
        onKeepAwakeDurationChange = { keepAwakeDuration = it },
        hasAcceptedUsageNotice = hasAcceptedUsageNotice,
        onAcceptedUsageNoticeChange = { hasAcceptedUsageNotice = it },
        presets = presets,
        onOpenWorkbench = {
            runCatching {
                require(hasAcceptedUsageNotice) { "请先确认你有权访问输入的URL" }
                normalizeAccessUrl(accessUrl)
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "参数不完整", Toast.LENGTH_SHORT).show()
            }.onSuccess { targetUrl ->
                WorkbenchActivity.start(
                    context = context,
                    accessUrl = targetUrl,
                    keepAwakeDuration = keepAwakeDuration,
                )
            }
        },
        onSavePreset = {
            runCatching {
                normalizeAccessUrl(accessUrl)
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "参数不完整", Toast.LENGTH_SHORT).show()
            }.onSuccess { targetUrl ->
                val host = Uri.parse(targetUrl).host.orEmpty()
                val finalLabel = presetLabel.trim().ifBlank {
                    host.ifBlank { "自定义URL" }
                }
                val preset = WorkbenchPreset(
                    label = finalLabel,
                    accessUrl = targetUrl,
                    keepAwakeDuration = keepAwakeDuration,
                )
                scope.launch {
                    preferenceStore.upsertPreset(preset)
                    presetLabel = ""
                    Toast.makeText(context, "预设已保存", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onApplyPreset = { preset ->
            accessUrl = preset.accessUrl
            keepAwakeDuration = preset.keepAwakeDuration
            presetLabel = preset.label
        },
        onDeletePreset = { preset ->
            scope.launch {
                preferenceStore.deletePreset(preset.id)
                Toast.makeText(context, "预设已删除", Toast.LENGTH_SHORT).show()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    accessUrl: String,
    onAccessUrlChange: (String) -> Unit,
    presetLabel: String,
    onPresetLabelChange: (String) -> Unit,
    keepAwakeDuration: KeepAwakeDuration,
    onKeepAwakeDurationChange: (KeepAwakeDuration) -> Unit,
    hasAcceptedUsageNotice: Boolean,
    onAcceptedUsageNoticeChange: (Boolean) -> Unit,
    presets: List<WorkbenchPreset>,
    onOpenWorkbench: () -> Unit,
    onSavePreset: () -> Unit,
    onApplyPreset: (WorkbenchPreset) -> Unit,
    onDeletePreset: (WorkbenchPreset) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("网页常亮工具") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "输入你有权访问的网页地址，App 仅在本机 WebView 中打开该地址。访问URL不会自动保存，只有点击保存预设时才会写入本机。",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = accessUrl,
                    onValueChange = onAccessUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("访问URL") },
                    placeholder = { Text("https://your-domain.example/path") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    minLines = 1,
                    maxLines = 3,
                )
            }

            item {
                OutlinedTextField(
                    value = presetLabel,
                    onValueChange = onPresetLabelChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("预设名称（可选）") },
                    placeholder = { Text("不填写则使用域名命名") },
                )
            }

            item {
                UsageNoticeCard(
                    checked = hasAcceptedUsageNotice,
                    onCheckedChange = onAcceptedUsageNoticeChange,
                )
            }

            item {
                KeepAwakeDurationCard(
                    selectedDuration = keepAwakeDuration,
                    onDurationSelected = onKeepAwakeDurationChange,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onOpenWorkbench,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("打开网页")
                    }
                    OutlinedButton(
                        onClick = onSavePreset,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("保存预设", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            if (presets.isNotEmpty()) {
                item {
                    Text(
                        text = "已保存预设",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(items = presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset = preset,
                        onApply = { onApplyPreset(preset) },
                        onDelete = { onDeletePreset(preset) },
                    )
                }
            }

            item {
                AboutCard()
            }
        }
    }
}

@Composable
private fun UsageNoticeCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("使用确认") },
            supportingContent = {
                Text("请确认你对输入的网页地址及相关内容拥有合法访问权限，并遵守适用法律法规及平台规则。本应用仅提供本机网页打开与屏幕常亮辅助功能，不参与网页内容提供、账号授权或权限控制。")
            },
            trailingContent = {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeepAwakeDurationCard(
    selectedDuration: KeepAwakeDuration,
    onDurationSelected: (KeepAwakeDuration) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "工作页常亮时长",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        Text(
            text = "可选 30 分钟到 12 小时。超过选定时长后，App 会撤销常亮控制，系统恢复自己的息屏策略。",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeepAwakeDuration.entries.forEach { option ->
                FilterChip(
                    selected = option == selectedDuration,
                    onClick = { onDurationSelected(option) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: WorkbenchPreset,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    text = preset.label,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(preset.accessUrl)
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onApply) {
                        Text("应用")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "删除预设",
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "关于",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )
        Text(
            text = "网页常亮工具 v${BuildConfig.VERSION_NAME}\n© 2026 Qicheng. All rights reserved.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun WorkbenchActivity.Companion.start(
    context: Context,
    accessUrl: String,
    keepAwakeDuration: KeepAwakeDuration,
) {
    context.startActivity(
        Intent(context, WorkbenchActivity::class.java).apply {
            putExtra(WorkbenchActivity.EXTRA_ACCESS_URL, accessUrl)
            putExtra(WorkbenchActivity.EXTRA_KEEP_AWAKE_DURATION, keepAwakeDuration.name)
        },
    )
}
