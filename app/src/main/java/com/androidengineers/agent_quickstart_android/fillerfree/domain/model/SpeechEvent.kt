package com.androidengineers.agent_quickstart_android.fillerfree.domain.model

/**
 * A single detected speech issue during a live session.
 * Emitted by [com.androidengineers.agent_quickstart_android.fillerfree.domain.usecase.AnalyzeTranscriptUseCase]
 * as transcript chunks stream in from Agora RTM.
 */
data class SpeechEvent(
    val id: String,
    val type: SpeechEventType,
    val text: String,
    val timestampMs: Long,
)

enum class SpeechEventType {
    FILLER_WORD,
    REPETITION,
    AGENT_INTERRUPTION,
}

/**
 * Aggregated live stats shown in the UI while a session is active.
 */
data class SessionStats(
    val fillerCount: Int = 0,
    val repetitionCount: Int = 0,
    val interruptionCount: Int = 0,
    val wordCount: Int = 0,
    val durationMs: Long = 0L,
) {
    val totalIssues: Int get() = fillerCount + repetitionCount

    fun issuesPerMinute(): Double {
        if (durationMs <= 0) return 0.0
        val minutes = durationMs / 60_000.0
        return if (minutes > 0) totalIssues / minutes else 0.0
    }
}

/**
 * Final summary shown on the end-of-session card.
 */
data class SessionSummary(
    val stats: SessionStats,
    val topOffender: String?,
    val closingTip: String,
)
