package io.github.wjy.meditate.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wjy.meditate.data.JournalEntry
import io.github.wjy.meditate.data.JournalRepository
import io.github.wjy.meditate.data.SettingsManager
import io.github.wjy.meditate.data.WebDavConfig
import io.github.wjy.meditate.network.RestorePreview
import io.github.wjy.meditate.network.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Random

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JournalRepository.getInstance(application)
    private val syncManager = SyncManager(application)

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    private val _restorePreview = MutableStateFlow<RestorePreview?>(null)
    val restorePreview = _restorePreview.asStateFlow()

    val blurEnabled = repository.settingsManager.blurEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    val blurImplementation = repository.settingsManager.blurImplementation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = SettingsManager.IMPL_HARDWARE)

    val blurIntensity = repository.settingsManager.blurIntensity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = 16f)

    val webDavConfig = repository.settingsManager.webDavConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = WebDavConfig())

    val activeSlot = repository.settingsManager.activeSlot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = "a")

    fun setBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsManager.setBlurEnabled(enabled)
        }
    }

    fun setBlurImplementation(implementation: String) {
        viewModelScope.launch {
            repository.settingsManager.setBlurImplementation(implementation)
        }
    }

    fun setBlurIntensity(intensity: Float) {
        viewModelScope.launch {
            repository.settingsManager.setBlurIntensity(intensity)
        }
    }

    fun updateWebDavConfig(config: WebDavConfig) {
        viewModelScope.launch {
            repository.settingsManager.updateWebDavConfig(config)
        }
    }

    fun testWebDavConnection(config: WebDavConfig) {
        viewModelScope.launch {
            _syncStatus.value = "正在测试连接..."
            syncManager.testConnection(config)
                .onSuccess { _syncStatus.value = "连接成功！" }
                .onFailure { _syncStatus.value = "连接失败: ${it.message ?: "未知错误"}" }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _syncStatus.value = "正在同步..."
            val config = repository.settingsManager.webDavConfig.stateIn(viewModelScope).value
            syncManager.syncToWebDav(config)
                .onSuccess { _syncStatus.value = "同步成功" }
                .onFailure { _syncStatus.value = "同步失败: ${it.message ?: "未知错误"}" }
        }
    }

    fun downloadForRestore() {
        viewModelScope.launch {
            _syncStatus.value = "正在下载备份..."
            val config = repository.settingsManager.webDavConfig.stateIn(viewModelScope).value
            syncManager.downloadAndPreview(config)
                .onSuccess { 
                    _restorePreview.value = it
                    _syncStatus.value = null
                }
                .onFailure { _syncStatus.value = "下载失败: ${it.message ?: "未知错误"}" }
        }
    }

    fun confirmRestore(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = "正在切换数据槽位..."
            syncManager.switchActiveSlot()
                .onSuccess {
                    _restorePreview.value = null
                    _syncStatus.value = "数据恢复成功"
                    onSuccess()
                }
                .onFailure { _syncStatus.value = "切换失败: ${it.message ?: "未知错误"}" }
        }
    }

    fun cancelRestore() {
        _restorePreview.value = null
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun generateDebugEntries(count: Int) {
        viewModelScope.launch {
            val tags = listOf("感悟", "生活", "工作", "情绪", "灵感")
            val contents = listOf(
                "今天的天气真不错，心情也跟着变好了。",
                "读书笔记：生活不在于你呼吸了多少次，而在于那些令你屏息的时刻。",
                "完成了一个具有挑战性的项目，很有成就感。",
                "路边看到一朵不知名的小花，生命力真顽强。",
                "冥想 10 分钟，感觉思路清晰了很多。",
                "有时候，慢下来就是快。"
            )
            
            val random = Random()
            val dao = repository.getDao()
            repeat(count) {
                val entry = JournalEntry(
                    content = contents[random.nextInt(contents.size)] + " (Debug #${it + 1})",
                    moodTag = tags[random.nextInt(tags.size)],
                    timestamp = System.currentTimeMillis() - random.nextInt(1000 * 60 * 60 * 24 * 7),
                    selfAdvice = if (random.nextBoolean()) "保持这个状态。" else null,
                )
                dao.insertEntry(entry)
            }
        }
    }

    fun clearAllEntries() {
        viewModelScope.launch {
            repository.getDao().clearAll()
        }
    }

    fun importEntries(entries: List<JournalEntry>) {
        viewModelScope.launch {
            val dao = repository.getDao()
            entries.forEach { entry ->
                dao.insertEntry(entry.copy(id = 0))
            }
        }
    }
}
