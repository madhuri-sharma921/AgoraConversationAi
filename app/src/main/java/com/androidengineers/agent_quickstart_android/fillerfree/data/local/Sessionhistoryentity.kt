package com.androidengineers.agent_quickstart_android.fillerfree.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Point 5 (daily improvement memory): one row per completed session.
 *
 * This is a flattened, persistence-friendly mirror of
 * [com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionSummary]
 * — Room entities need to be simple/flat, so nested data classes from the
 * domain layer aren't stored directly. See SessionHistoryRepository for the
 * mapping between the two.
 *
 * Stored entirely on-device via Room/SQLite — nothing here is sent to the
 * quickstart server or anywhere else.
 */
@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedAtEpochMs: Long,
    val topicId: String,
    val fillerCount: Int,
    val repetitionCount: Int,
    val interruptionCount: Int,
    val wordCount: Int,
    val durationMs: Long,
    val avgInterruptLatencyMs: Long?,
    val topOffender: String?,
)