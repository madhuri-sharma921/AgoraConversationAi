package com.androidengineers.agent_quickstart_android.fillerfree.domain.repository

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.PastSessionRecord
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionSummary

/**
 * Clean-architecture boundary for local (on-device) session history —
 * point 5's "daily improvement memory." Nothing behind this interface ever
 * leaves the device; there's no server-side persistence involved.
 */
interface SessionHistoryRepository {

    /** Persists a completed session's summary for future trend tracking. */
    suspend fun recordSession(topicId: String, summary: SessionSummary)

    /** Most recent sessions, newest first, capped at [limit]. */
    suspend fun recentSessions(limit: Int): List<PastSessionRecord>

    /** The single most recent session, if any — used to seed CoachAgentPromptBuilder's memory clause. */
    suspend fun mostRecentSession(): PastSessionRecord?

    /** Wipes all local session history. */
    suspend fun clearHistory()
}