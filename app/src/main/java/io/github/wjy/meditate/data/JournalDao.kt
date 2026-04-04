package io.github.wjy.meditate.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeletedEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE isRational = 1 AND isDeleted = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomAdvice(): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Query("UPDATE journal_entries SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteEntry(id: Long)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun permanentlyDeleteEntry(id: Long)

    @Query("UPDATE journal_entries SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreEntry(id: Long)

    @Query("DELETE FROM journal_entries WHERE moodTag = :tag AND isDeleted = 0")
    suspend fun deleteEntriesByTag(tag: String)

    @Query("DELETE FROM journal_entries WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM journal_entries")
    suspend fun clearAll()
}
