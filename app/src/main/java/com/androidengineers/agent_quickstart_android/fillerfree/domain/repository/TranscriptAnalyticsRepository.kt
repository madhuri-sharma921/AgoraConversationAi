package com.androidengineers.agent_quickstart_android.fillerfree.domain.repository

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionStats
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionSummary
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEvent
import kotlinx.coroutines.flow.Flow

/**
 * Clean-architecture boundary: the presentation layer only depends on this
 * interface, never on the concrete transcript-parsing implementation.
 */
interface TranscriptAnalyticsRepository {

    /** Live stream of detected speech events as transcript text arrives. */
    val events: Flow<SpeechEvent>

    /** Live aggregated stats, updated as events come in. */
    val stats: Flow<SessionStats>

    /** Feed a new transcript chunk (user speech) into the analyzer. */
    fun onUserTranscriptChunk(text: String, timestampMs: Long)

    /** Feed a new agent transcript chunk in, used to detect interruptions. */
    fun onAgentTranscriptChunk(text: String, timestampMs: Long, userWasSpeaking: Boolean)

    /** Reset all counters for a new session. */
    fun reset()

    /** Build the final summary shown at session end. */
    fun buildSummary(): SessionSummary
}
