package com.androidengineers.agent_quickstart_android.fillerfree.presentation

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionStats
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionSummary
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEvent
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechTopic

/**
 * Single immutable state object for the Filler-Free screen (MVI style,
 * consistent with the existing ConversationUiState pattern in the quickstart).
 */
data class FillerFreeUiState(
    val screen: Screen = Screen.TOPIC_SELECT,
    val selectedTopic: SpeechTopic? = null,
    val topics: List<SpeechTopic> = SpeechTopic.ALL,
    val isConnecting: Boolean = false,
    val isSessionActive: Boolean = false,
    val liveTranscript: String = "",
    val recentEvents: List<SpeechEvent> = emptyList(),
    val stats: SessionStats = SessionStats(),
    val summary: SessionSummary? = null,
    val errorMessage: String? = null,
) {
    enum class Screen {
        TOPIC_SELECT,
        IN_SESSION,
        SUMMARY,
    }
}

/** One-off UI events, distinct from persisted state (e.g. a brief flash/haptic on interruption). */
sealed interface FillerFreeUiEvent {
    data object InterruptionFlash : FillerFreeUiEvent
    data class ShowError(val message: String) : FillerFreeUiEvent
}
