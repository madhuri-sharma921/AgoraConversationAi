package com.androidengineers.agent_quickstart_android.fillerfree.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionHistoryDao {

    @Insert
    suspend fun insert(entity: SessionHistoryEntity)

    @Query("SELECT * FROM session_history ORDER BY completedAtEpochMs DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<SessionHistoryEntity>

    @Query("SELECT * FROM session_history ORDER BY completedAtEpochMs DESC LIMIT 1")
    suspend fun getMostRecentSession(): SessionHistoryEntity?

    @Query("DELETE FROM session_history")
    suspend fun deleteAll()
}
