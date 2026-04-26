package io.github.wjy.meditate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.wjy.meditate.network.RestorePreview
import io.github.wjy.meditate.network.SyncManager
import io.github.wjy.meditate.ui.home.HomeScreen
import io.github.wjy.meditate.ui.settings.SettingsScreen
import io.github.wjy.meditate.ui.theme.AppStyle
import io.github.wjy.meditate.ui.theme.PaperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val syncManager by lazy { SyncManager(this) }
    private var pendingImportPreview by mutableStateOf<RestorePreview?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        setContent {
            PaperTheme(appStyle = AppStyle.MIUI) {
                var navigationKey by remember { mutableIntStateOf(0) }
                val scope = rememberCoroutineScope()

                // 处理外部文件导入对话框
                pendingImportPreview?.let { preview ->
                    AlertDialog(
                        onDismissRequest = { pendingImportPreview = null },
                        title = { Text("外部数据库导入", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("识别到外部数据库文件，预览如下：")
                                Text("• 记录条数：${preview.entryCount}")
                                Text("• 最近日期：${preview.lastEntryDate}")
                                Text("• 最近内容：${preview.lastEntryContent}...")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "确认导入将切换到新的数据槽位。原数据依然保留在旧槽位。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    syncManager.switchActiveSlot().onSuccess {
                                        pendingImportPreview = null
                                        navigationKey++ // 触发导航重载
                                        Toast.makeText(this@MainActivity, "导入成功", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) { Text("确认导入") }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingImportPreview = null }) { Text("取消") }
                        }
                    )
                }

                AppNavigation(
                    key = navigationKey,
                    onDatabaseRestored = { navigationKey++ }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            else -> null
        }

        uri?.let {
            importDatabase(it)
        }
    }

    private fun importDatabase(uri: Uri) {
        lifecycleScope.launch {
            syncManager.importFromFile(uri).onSuccess { preview ->
                pendingImportPreview = preview
            }.onFailure {
                Toast.makeText(this@MainActivity, "文件解析失败: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun AppNavigation(
    key: Int,
    onDatabaseRestored: () -> Unit
) {
    // 使用 key 强制销毁并重建整个导航栈和控制器，从而强制所有 ViewModel 重新初始化并连接新数据库
    androidx.compose.runtime.key(key) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(onNavigateToSettings = { navController.navigate("settings") })
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onDatabaseRestored = onDatabaseRestored
                )
            }
        }
    }
}
