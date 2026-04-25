package io.github.wjy.meditate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.wjy.meditate.ui.home.HomeScreen
import io.github.wjy.meditate.ui.settings.SettingsScreen
import io.github.wjy.meditate.ui.theme.AppStyle
import io.github.wjy.meditate.ui.theme.PaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PaperTheme(appStyle = AppStyle.MIUI) {
                var navigationKey by remember { mutableStateOf(0) }
                AppNavigation(
                    key = navigationKey,
                    onDatabaseRestored = { navigationKey++ }
                )
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
