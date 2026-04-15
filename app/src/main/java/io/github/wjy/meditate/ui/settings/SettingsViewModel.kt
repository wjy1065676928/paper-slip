package io.github.wjy.meditate.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wjy.meditate.data.AppDatabase
import io.github.wjy.meditate.data.JournalEntry
import kotlinx.coroutines.launch
import java.util.Random

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).journalDao()

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
            repeat(count) {
                val entry = JournalEntry(
                    content = contents[random.nextInt(contents.size)] + " (Debug #${it + 1})",
                    moodTag = tags[random.nextInt(tags.size)],
                    timestamp = System.currentTimeMillis() - random.nextInt(1000 * 60 * 60 * 24 * 7),
                    selfAdvice = if (random.nextBoolean()) "保持这个状态。" else null
                )
                dao.insertEntry(entry)
            }
        }
    }

    fun clearAllEntries() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}
