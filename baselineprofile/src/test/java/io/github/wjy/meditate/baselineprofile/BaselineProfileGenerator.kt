package io.github.wjy.meditate.baselineprofile

import androidx.benchmark.macro.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 纸条 (paper-slip) Baseline Profile 生成器
 *
 * 通过模拟关键用户操作路径，帮助 AOT 编译器预编译热点代码路径，
 * 从而显著减少首次安装/升级后的启动和运行卡顿。
 *
 * 运行方式：
 * ```
 * ./gradlew :baselineprofile:generateBaselineProfile
 * ```
 * 需要连接的设备或模拟器（Android 9+）。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collectBaselineProfile(
            packageName = "io.github.wjy.meditate",
            maxIterations = 10
        ) {
            // 1. 冷启动 - 等待主屏幕完全呈现
            startActivityAndWait()
            device.waitForIdle()

            // 2. 等待主列表加载完成（Room 数据库预热）
            device.wait(
                until = {
                    device.findObject(
                        androidx.test.uiautomator.By.text("全部")
                    ).exists()
                },
                timeout = 5000
            )
            device.waitForIdle()

            // 3. 滚动日记列表（触发 LazyColumn 回收和复用）
            val listView = device.findObject(
                androidx.test.uiautomator.By.scrollable(true)
            )
            if (listView.exists()) {
                listView.setGestureMargin(
                    device.displayWidth / 5
                )
                // 向下滚动
                listView.flingForward()
                device.waitForIdle()
                // 向上滚动回顶部
                listView.flingBackward()
                device.waitForIdle()
            }

            // 4. 打开设置页面（Navigation Compose 导航）
            val settingsButton = device.findObject(
                androidx.test.uiautomator.By.desc("设置")
            )
            if (settingsButton.exists()) {
                settingsButton.click()
                device.waitForIdle()
            }

            // 5. 返回主页面
            device.pressBack()
            device.waitForIdle()
        }
    }
}
