package com.androidengineers.agent_quickstart_android.fillerfree.data

import android.app.Application
import com.androidengineers.agent_quickstart_android.fillerfree.data.local.FillerFreeDatabase
import com.androidengineers.agent_quickstart_android.fillerfree.data.local.SessionHistoryEntity
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.PastSessionRecord
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionStats
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionSummary
import com.androidengineers.agent_quickstart_android.fillerfree.domain.repository.SessionHistoryRepository

class SessionHistoryRepositoryImpl(application: Application) : SessionHistoryRepository {

    private val dao = FillerFreeDatabase.getInstance(application).sessionHistoryDao()

    override suspend fun recordSession(
        topicId: String,
        summary: SessionSummary,
    ) {
        val entity = SessionHistoryEntity(
            completedAtEpochMs = System.currentTimeMillis(),
            topicId = topicId,
            fillerCount = summary.stats.fillerCount,
            repetitionCount = summary.stats.repetitionCount,
            interruptionCount = summary.stats.interruptionCount,
            wordCount = summary.stats.wordCount,
            durationMs = summary.stats.durationMs,
            avgInterruptLatencyMs = summary.avgInterruptLatencyMs,
            topOffender = summary.topOffender,
        )
        dao.insert(entity)
    }

    override suspend fun recentSessions(limit: Int): List<PastSessionRecord> {
        return dao.getRecentSessions(limit).map { it.toDomain() }
    }

    override suspend fun mostRecentSession(): PastSessionRecord? {
        return dao.getMostRecentSession()?.toDomain()
    }

    override suspend fun clearHistory() {
        dao.deleteAll()
    }

    private fun SessionHistoryEntity.toDomain() = PastSessionRecord(
        completedAtEpochMs = completedAtEpochMs,
        topicId = topicId,
        stats = SessionStats(
            fillerCount = fillerCount,
            repetitionCount = repetitionCount,
            interruptionCount = interruptionCount,
            wordCount = wordCount,
            durationMs = durationMs,
        ),
        avgInterruptLatencyMs = avgInterruptLatencyMs,
        topOffender = topOffender,
    )
}
