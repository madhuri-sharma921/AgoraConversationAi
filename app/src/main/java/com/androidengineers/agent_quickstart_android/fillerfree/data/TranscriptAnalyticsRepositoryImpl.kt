package com.androidengineers.agent_quickstart_android.fillerfree.data

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionStats
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionSummary
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEvent
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEventType
import com.androidengineers.agent_quickstart_android.fillerfree.domain.repository.TranscriptAnalyticsRepository
import com.androidengineers.agent_quickstart_android.fillerfree.domain.usecase.AnalyzeTranscriptUseCase
import com.androidengineers.agent_quickstart_android.fillerfree.domain.usecase.BuildSessionSummaryUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation: no persistence needed for the hackathon MVP.
 * Everything lives for the duration of one session and resets on [reset].
 *
 * Thread-safety note: this is expected to be driven from a single
 * coroutine/collector (the ViewModel's viewModelScope), matching how
 * ConversationViewModel already collects from AgoraConversationSessionManager.
 */
class TranscriptAnalyticsRepositoryImpl(
    private val analyzeTranscript: AnalyzeTranscriptUseCase = AnalyzeTranscriptUseCase(),
    private val buildSummary: BuildSessionSummaryUseCase = BuildSessionSummaryUseCase(),
) : TranscriptAnalyticsRepository {

    private val _events = MutableSharedFlow<SpeechEvent>(replay = 0, extraBufferCapacity = 32)
    override val events = _events

    private val _stats = MutableStateFlow(SessionStats())
    override val stats: StateFlow<SessionStats> = _stats.asStateFlow()

    private val eventHistory = mutableListOf<SpeechEvent>()
    private val recentSentences = ArrayDeque<String>()
    private var sessionStartMs: Long? = null

    override fun onUserTranscriptChunk(text: String, timestampMs: Long) {
        if (sessionStartMs == null) sessionStartMs = timestampMs

        val newEvents = analyzeTranscript(
            newText = text,
            recentSentences = recentSentences.toList(),
            timestampMs = timestampMs,
        )

        recentSentences.addLast(text)
        if (recentSentences.size > MAX_RECENT_SENTENCES) recentSentences.removeFirst()

        newEvents.forEach { event ->
            eventHistory += event
            _events.tryEmit(event)
        }

        val wordDelta = text.split(Regex("\\s+")).count { it.isNotBlank() }
        _stats.update(sessionStartMs, timestampMs) { current ->
            current.copy(
                fillerCount = current.fillerCount + newEvents.count { it.type == SpeechEventType.FILLER_WORD },
                repetitionCount = current.repetitionCount + newEvents.count { it.type == SpeechEventType.REPETITION },
                wordCount = current.wordCount + wordDelta,
            )
        }
    }

    override fun onAgentTranscriptChunk(text: String, timestampMs: Long, userWasSpeaking: Boolean) {
        if (!userWasSpeaking) return

        val event = SpeechEvent(
            id = "${timestampMs}_interrupt",
            type = SpeechEventType.AGENT_INTERRUPTION,
            text = text,
            timestampMs = timestampMs,
        )
        eventHistory += event
        _events.tryEmit(event)

        _stats.update(sessionStartMs, timestampMs) { current ->
            current.copy(interruptionCount = current.interruptionCount + 1)
        }
    }

    override fun reset() {
        eventHistory.clear()
        recentSentences.clear()
        sessionStartMs = null
        _stats.value = SessionStats()
    }

    override fun buildSummary(): SessionSummary = buildSummary(_stats.value, eventHistory.toList())

    private fun MutableStateFlow<SessionStats>.update(
        startMs: Long?,
        nowMs: Long,
        block: (SessionStats) -> SessionStats,
    ) {
        val duration = if (startMs != null) nowMs - startMs else 0L
        value = block(value).copy(durationMs = duration)
    }

    companion object {
        private const val MAX_RECENT_SENTENCES = 5
    }
}
