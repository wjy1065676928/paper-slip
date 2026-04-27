package io.github.wjy.meditate.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import io.github.wjy.meditate.data.JournalEntry
import io.github.wjy.meditate.data.JournalRepository
import io.github.wjy.meditate.data.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 现代 MVI 架构：单一 UI 状态源
 * 标注为 @Immutable 告诉 Compose 编译器该对象一旦创建就不会改变其属性，
 * 从而在列表刷新时跳过不必要的重绘检查。
 */
@Immutable
data class HomeUiState(
    val entries: List<JournalEntry> = emptyList(),
    val deletedEntries: List<JournalEntry> = emptyList(),
    val dbTags: List<String> = emptyList(),
    val sessionTags: Set<String> = emptySet(),
    val blurEnabled: Boolean = false,
    val blurImplementation: String = SettingsManager.IMPL_HARDWARE,
    val blurIntensity: Float = 16f,
    val isLoading: Boolean = false
)

/**
 * MVI Actions：所有用户交互意图的唯一入口
 */
sealed interface HomeAction {
    data class AddEntry(val content: String, val moodTag: String, val advice: String?) : HomeAction
    data class SoftDeleteEntry(val entry: JournalEntry) : HomeAction
    data class SoftDeleteEntries(val entries: List<JournalEntry>) : HomeAction
    data class RestoreEntry(val entry: JournalEntry) : HomeAction
    data class PermanentlyDeleteEntry(val entry: JournalEntry) : HomeAction
    data class DeleteEntriesByTag(val tag: String) : HomeAction
    data class AddSessionTag(val tag: String) : HomeAction
    data class ImportEntries(val entries: List<JournalEntry>) : HomeAction
    data object EmptyTrash : HomeAction
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JournalRepository.getInstance(application)
    private val _sessionTags = MutableStateFlow(setOf<String>())

    private val daoFlow = repository.getDaoFlow()

    /**
     * 响应式状态流：使用 combine 实现多数据源的实时响应
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = daoFlow.flatMapLatest { dao ->
        combine(
            dao.getAllEntries(),
            dao.getDeletedEntries(),
            _sessionTags,
            repository.blurSettings
        ) { entries, deletedEntries, sessionTags, blur ->
            HomeUiState(
                entries = entries,
                deletedEntries = deletedEntries,
                dbTags = entries.map { it.moodTag }.distinct().filter { it.isNotBlank() },
                sessionTags = sessionTags,
                blurEnabled = blur.enabled,
                blurImplementation = blur.implementation,
                blurIntensity = blur.intensity,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    /**
     * 处理所有来自 UI 的 Action
     */
    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.AddEntry -> addEntry(action.content, action.moodTag, action.advice)
            is HomeAction.SoftDeleteEntry -> softDeleteEntry(action.entry)
            is HomeAction.SoftDeleteEntries -> softDeleteEntries(action.entries)
            is HomeAction.RestoreEntry -> restoreEntry(action.entry)
            is HomeAction.PermanentlyDeleteEntry -> permanentlyDeleteEntry(action.entry)
            is HomeAction.DeleteEntriesByTag -> deleteEntriesByTag(action.tag)
            is HomeAction.AddSessionTag -> addSessionTag(action.tag)
            is HomeAction.ImportEntries -> importEntries(action.entries)
            HomeAction.EmptyTrash -> emptyTrash()
        }
    }

    private fun importEntries(entries: List<JournalEntry>) {
        viewModelScope.launch {
            val dao = repository.getDao()
            entries.forEach { entry ->
                dao.insertEntry(entry.copy(id = 0))
            }
        }
    }

    private fun addEntry(content: String, moodTag: String, advice: String?) {
        viewModelScope.launch {
            val dao = repository.getDao()
            dao.insertEntry(
                JournalEntry(
                    content = content,
                    moodTag = moodTag,
                    selfAdvice = advice,
                    isRational = !advice.isNullOrBlank()
                )
            )
        }
    }

    private fun softDeleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            val dao = repository.getDao()
            dao.softDeleteEntry(entry.id)
        }
    }

    private fun softDeleteEntries(entries: List<JournalEntry>) {
        viewModelScope.launch {
            val dao = repository.getDao()
            entries.forEach { dao.softDeleteEntry(it.id) }
        }
    }

    private fun restoreEntry(entry: JournalEntry) {
        viewModelScope.launch {
            val dao = repository.getDao()
            dao.restoreEntry(entry.id)
        }
    }

    private fun permanentlyDeleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            val dao = repository.getDao()
            dao.permanentlyDeleteEntry(entry.id)
        }
    }

    private fun deleteEntriesByTag(tag: String) {
        viewModelScope.launch {
            val dao = repository.getDao()
            dao.deleteEntriesByTag(tag)
            _sessionTags.value -= tag
        }
    }

    private fun addSessionTag(tag: String) {
        _sessionTags.value += tag
    }

    private fun emptyTrash() {
        viewModelScope.launch {
            val dao = repository.getDao()
            dao.emptyTrash()
        }
    }
}
