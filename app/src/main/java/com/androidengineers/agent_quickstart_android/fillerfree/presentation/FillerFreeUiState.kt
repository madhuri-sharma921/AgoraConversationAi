package com.androidengineers.agent_quickstart_android.fillerfree.presentation

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.AttentionSignal
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.CoachRole
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.EmotionLabel
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
    val currentEmotion: EmotionLabel? = null,
    val activeCoachRole: CoachRole = CoachRole.DELIVERY,
    // Point 4 (visual coaching, tier 1): local-only, opt-in. Off by default
    // since it needs the CAMERA permission, which RECORD_AUDIO alone doesn't
    // cover — the user explicitly turns this on per session.
    val visualCoachingEnabled: Boolean = false,
    val attentionSignal: AttentionSignal? = null,
    // Point 5 (daily improvement memory): trend line across recent sessions,
    // loaded once at app start / topic-select time, not during a live session.
    val dailyProgress: DailyProgressUiModel? = null,
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
    data class CoachSwitched(val role: com.androidengineers.agent_quickstart_android.fillerfree.domain.model.CoachRole) : FillerFreeUiEvent
}

/**
 * UI-ready projection of the domain-layer DailyProgress (point 5). Kept
 * separate from the domain model so the presentation layer doesn't need to
 * import BuildDailyProgressUseCase's math directly — [improvementText] is
 * pre-formatted, ready to drop into a Text composable.
 */
data class DailyProgressUiModel(
    val sessionCount: Int,
    val issuesPerMinuteTrend: List<Double>,
    val mostCommonFillerWord: String?,
    val improvementText: String?,
)