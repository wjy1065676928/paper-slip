@file:Suppress("DEPRECATION")

package io.github.wjy.meditate.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.wjy.meditate.data.AppDatabase
import io.github.wjy.meditate.data.SettingsManager
import io.github.wjy.meditate.data.WebDavConfig
import io.github.wjy.meditate.ui.home.components.FastTransferOverlay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDatabaseRestored: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val blurEnabled by viewModel.blurEnabled.collectAsStateWithLifecycle()
    val blurImplementation by viewModel.blurImplementation.collectAsStateWithLifecycle()
    val blurIntensity by viewModel.blurIntensity.collectAsStateWithLifecycle()
    val webDavConfig by viewModel.webDavConfig.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val restorePreview by viewModel.restorePreview.collectAsStateWithLifecycle()
    val activeSlot by viewModel.activeSlot.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showImplDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }
    var showScanOverlay by remember { mutableStateOf(false) }

    // 统一遮罩可见性判断
    val isOverlayVisible = showScanOverlay || showWebDavDialog || showImplDialog || (restorePreview != null)

    // 处理物理返回键
    BackHandler(enabled = isOverlayVisible) {
        when {
            showScanOverlay -> showScanOverlay = false
            showWebDavDialog -> showWebDavDialog = false
            showImplDialog -> showImplDialog = false
            restorePreview != null -> viewModel.cancelRestore()
        }
    }

    // 模糊动画逻辑同步
    val view = LocalView.current
    var blurredBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val blurAlpha by animateFloatAsState(
        targetValue = if (isOverlayVisible && blurredBitmap != null) 1f else 0f,
        animationSpec = tween(300),
        label = "SettingsBlurAlpha"
    )
    
    LaunchedEffect(isOverlayVisible, blurIntensity) {
        if (isOverlayVisible && blurEnabled && blurImplementation == SettingsManager.IMPL_RENDER_SCRIPT) {
            try {
                // 确保在截图前 UI 状态已更新，但动画尚未完全覆盖
                val screenshot = view.drawToBitmap()
                val rs = RenderScript.create(context)
                val input = Allocation.createFromBitmap(rs, screenshot)
                val output = Allocation.createTyped(rs, input.type)
                val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
                script.setRadius(blurIntensity.coerceIn(1f, 25f))
                script.setInput(input)
                script.forEach(output)
                output.copyTo(screenshot)
                blurredBitmap = screenshot.asImageBitmap()
                rs.destroy()
            } catch (e: Exception) {
                Log.e("SettingsBlur", "Blur failed", e)
            }
        } else if (!isOverlayVisible) {
            // 延迟清空，等待淡出动画结束
            kotlinx.coroutines.delay(300.milliseconds)
            blurredBitmap = null
        }
    }

    val blurRadius by animateFloatAsState(
        targetValue = if (isOverlayVisible && blurEnabled &&
            (blurImplementation == SettingsManager.IMPL_HARDWARE || 
             blurImplementation == SettingsManager.IMPL_RENDER_SCRIPT)) blurIntensity else 0f,
        animationSpec = tween(300),
        label = "SettingsBlurAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(blurRadius.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("设置", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Text(
                        "数据管理",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("WebDAV 同步") },
                        supportingContent = { 
                            Text(webDavConfig.url.ifBlank { "未配置" })
                        },
                        modifier = Modifier.clickable { showWebDavDialog = true }
                    )
                }
                
                if (webDavConfig.url.isNotBlank()) {
                    item {
                        ListItem(
                            headlineContent = { Text("立即同步到云端") },
                            supportingContent = { Text("上传本地数据库到 WebDAV") },
                            modifier = Modifier.clickable { viewModel.syncNow() }
                        )
                    }
                    item {
                        ListItem(
                            headlineContent = { Text("从云端恢复") },
                            supportingContent = { Text("从 WebDAV 下载并预览备份") },
                            modifier = Modifier.clickable { viewModel.downloadForRestore() }
                        )
                    }
                }
                
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                item {
                    Text(
                        "分享",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("扫码") },
                        supportingContent = { Text("扫描对方二维码预览记录") },
                        modifier = Modifier.clickable { showScanOverlay = true }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("全部分享") },
                        supportingContent = { Text("向对方展示所有") },
                        modifier = Modifier.clickable {
                            scope.launch {
                                try {
                                    // 导出前执行 checkpoint，确保所有 WAL 数据同步到主 .db 文件
                                    AppDatabase.checkpoint(context)

                                    val slot = activeSlot
                                    val dbFile = context.getDatabasePath("paper_database_$slot")
                                    if (dbFile.exists()) {
                                        val tempFile = java.io.File(context.cacheDir, "paper-slip-backup.db")
                                        dbFile.copyTo(tempFile, overwrite = true)
                                        
                                        val contentUri: Uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            tempFile
                                        )
                                        
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/octet-stream"
                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "发送数据库备份"))
                                    } else {
                                        Toast.makeText(context, "数据库文件不存在", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                item {
                    Text(
                        "模糊",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("模糊开关") },
                        supportingContent = { Text("在弹窗出现时模糊背景") },
                        trailingContent = {
                            Switch(
                                checked = blurEnabled,
                                onCheckedChange = { viewModel.setBlurEnabled(it) }
                            )
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("模糊实现") },
                        supportingContent = { Text(blurImplementation) },
                        modifier = Modifier.clickable { showImplDialog = true }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("模糊强度") },
                        supportingContent = {
                            Column {
                                Slider(
                                    value = blurIntensity,
                                    onValueChange = { viewModel.setBlurIntensity(it) },
                                    valueRange = 1f..25f,
                                    steps = 24
                                )
                                Text("当前强度: ${blurIntensity.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    )
                }
                
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                
                item {
                    Text(
                        "Debug 调试",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("生成测试数据") },
                        supportingContent = { Text("点击生成 10 条随机日记记录") },
                        modifier = Modifier.clickable {
                            viewModel.generateDebugEntries(10)
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("清空数据库") },
                        supportingContent = { Text("永久删除所有记录，请谨慎操作") },
                        modifier = Modifier.clickable {
                            viewModel.clearAllEntries()
                        }
                    )
                }
            }
        }

        // RenderScript Legacy 模糊层
        if (blurAlpha > 0f) {
            blurredBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = blurAlpha }
                        .zIndex(85f),
                    contentScale = ContentScale.FillBounds
                )
            }
        }

        // 弹窗层
        Box(modifier = Modifier.zIndex(120f)) {
            FastTransferOverlay(
                visible = showScanOverlay,
                qrBitmap = null,
                canSwitchMode = false,
                onClose = { showScanOverlay = false },
                onImport = { viewModel.importEntries(it) }
            )
        }
    }

    // 反馈与对话框逻辑
    LaunchedEffect(syncStatus) {
        syncStatus?.let {
            if (it.contains("失败") || it.contains("错误")) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("WebDAV Error", it)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "错误已复制到剪贴板，请粘贴查看", Toast.LENGTH_LONG).show()
            } else {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.clearSyncStatus()
        }
    }

    if (showWebDavDialog) {
        WebDavConfigDialog(
            config = webDavConfig,
            onDismiss = { showWebDavDialog = false },
            onSave = { viewModel.updateWebDavConfig(it); showWebDavDialog = false },
            onTest = { viewModel.testWebDavConnection(it) }
        )
    }

    if (showImplDialog) {
        val options = listOf(SettingsManager.IMPL_HARDWARE, SettingsManager.IMPL_RENDER_SCRIPT)
        AlertDialog(
            onDismissRequest = { showImplDialog = false },
            title = { Text("选择模糊实现") },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setBlurImplementation(option); showImplDialog = false }.padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = (option == blurImplementation), onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showImplDialog = false }) { Text("关闭") } }
        )
    }

    restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            title = { Text("恢复确认 (A/B 切换)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("发现云端备份，预览如下：", fontWeight = FontWeight.Bold)
                    Text("• 记录条数：${preview.entryCount}")
                    Text("• 最近日期：${preview.lastEntryDate}")
                    Text("• 最近内容：${preview.lastEntryContent}...")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("确认恢复将切换到新的数据槽位并重载应用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.confirmRestore(onDatabaseRestored) }) { Text("确认恢复并重载") } },
            dismissButton = { TextButton(onClick = { viewModel.cancelRestore() }) { Text("取消") } }
        )
    }
}

@Composable
fun WebDavConfigDialog(
    config: WebDavConfig,
    onDismiss: () -> Unit,
    onSave: (WebDavConfig) -> Unit,
    onTest: (WebDavConfig) -> Unit
) {
    var url by remember { mutableStateOf(config.url) }
    var user by remember { mutableStateOf(config.user) }
    var pass by remember { mutableStateOf(config.pass) }
    var ignoreCert by remember { mutableStateOf(config.ignoreCert) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebDAV 配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http(s)://example.com:port/dav") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { ignoreCert = !ignoreCert }
                ) {
                    Switch(
                        checked = ignoreCert,
                        onCheckedChange = { ignoreCert = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("忽略证书错误", style = MaterialTheme.typography.bodyMedium)
                }
                
                TextButton(
                    onClick = { onTest(WebDavConfig(url, user, pass, ignoreCert)) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("测试连接")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(WebDavConfig(url, user, pass, ignoreCert)) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
