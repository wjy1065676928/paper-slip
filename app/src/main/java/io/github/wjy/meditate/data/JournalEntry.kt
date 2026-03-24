package io.github.wjy.meditate.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val moodTag: String,
    val selfAdvice: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isRational: Boolean = false // Whether it's a "self-rescue" advice
)
