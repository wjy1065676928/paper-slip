package io.github.wjy.meditate.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val tag: String,
    val timestamp: Long = System.currentTimeMillis()
)
