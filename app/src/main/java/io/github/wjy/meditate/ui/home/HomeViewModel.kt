package io.github.wjy.meditate.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wjy.meditate.data.AppDatabase
import io.github.wjy.meditate.data.JournalEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).journalDao()

    val entries: StateFlow<List<JournalEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 动态计算所有已存在的标签
    val tags: StateFlow<List<String>> = entries.map { list ->
        list.map { it.moodTag }.distinct().filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(content: String, moodTag: String, advice: String?) {
        viewModelScope.launch {
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

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

    fun deleteEntriesByTag(tag: String) {
        viewModelScope.launch {
            dao.deleteEntriesByTag(tag)
        }
    }
}
