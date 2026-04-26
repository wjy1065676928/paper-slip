package io.github.wjy.meditate.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wjy.meditate.data.AppDatabase
import io.github.wjy.meditate.data.JournalEntry
import io.github.wjy.meditate.data.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 现代 MVI 架构：单一 UI 状态源
 */
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
    private val settingsManager = SettingsManager(application)
    
    private val _sessionTags = MutableStateFlow(setOf<String>())

    private val daoFlow = flow {
        emit(AppDatabase.getDatabase(getApplication()).journalDao())
    }

    /**
     * 响应式状态流：使用 combine 实现多数据源的实时响应
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = daoFlow.flatMapLatest { dao ->
        @Suppress("UNCHECKED_CAST")
        combine(
            dao.getAllEntries(),
            dao.getDeletedEntries(),
            _sessionTags,
            settingsManager.blurEnabled,
            settingsManager.blurImplementation,
            settingsManager.blurIntensity
        ) { args: Array<Any> ->
            val entries = args[0] as List<JournalEntry>
            val deletedEntries = args[1] as List<JournalEntry>
            val sessionTags = args[2] as Set<String>
            val blurEnabled = args[3] as Boolean
            val blurImplementation = args[4] as String
            val blurIntensity = args[5] as Float

            HomeUiState(
                entries = entries,
                deletedEntries = deletedEntries,
                dbTags = entries.map { it.moodTag }.distinct().filter { it.isNotBlank() },
                sessionTags = sessionTags,
                blurEnabled = blurEnabled,
                blurImplementation = blurImplementation,
                blurIntensity = blurIntensity,
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
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
            entries.forEach { entry ->
                dao.insertEntry(entry.copy(id = 0)) // 确保作为新记录插入
            }
        }
    }

    private fun addEntry(content: String, moodTag: String, advice: String?) {
        viewModelScope.launch {
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
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
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
            dao.softDeleteEntry(entry.id)
        }
    }

    private fun softDeleteEntries(entries: List<JournalEntry>) {
        viewModelScope.launch {
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
            entries.forEach { dao.softDeleteEntry(it.id) }
        }
    }

    private fun restoreEntry(entry: JournalEntry) {
        viewModelScope.launch {
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
            dao.restoreEntry(entry.id)
        }
    }

    private fun permanentlyDeleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
            dao.permanentlyDeleteEntry(entry.id)
        }
    }

    private fun deleteEntriesByTag(tag: String) {
        viewModelScope.launch {
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
            dao.deleteEntriesByTag(tag)
            _sessionTags.value -= tag
        }
    }

    private fun addSessionTag(tag: String) {
        _sessionTags.value += tag
    }

    private fun emptyTrash() {
        viewModelScope.launch {
            val dao = AppDatabase.getDatabase(getApplication()).journalDao()
            dao.emptyTrash()
        }
    }
}
