package com.androidengineers.agent_quickstart_android.fillerfree.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androidengineers.agent_quickstart_android.data.ConversationRepository
import com.androidengineers.agent_quickstart_android.fillerfree.config.CoachAgentPromptBuilder
import com.androidengineers.agent_quickstart_android.fillerfree.data.TranscriptAnalyticsRepositoryImpl
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEventType
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechTopic
import com.androidengineers.agent_quickstart_android.fillerfree.domain.repository.TranscriptAnalyticsRepository
import com.androidengineers.agent_quickstart_android.model.TranscriptSpeaker
import com.androidengineers.agent_quickstart_android.model.TranscriptTurnStatus
import com.androidengineers.agent_quickstart_android.rtc.AgoraConversationSessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Presentation-layer ViewModel for the Filler-Free feature.
 *
 * This composes the quickstart's existing [AgoraConversationSessionManager]
 * + [ConversationRepository] as the single source of truth for the RTC/RTM
 * session, and layers the Filler-Free analytics domain logic on top by
 * observing `sessionManager.snapshot.transcriptTurns` — the real transcript
 * model exposed by the quickstart (see model/ConversationModels.kt).
 *
 * Each [com.androidengineers.agent_quickstart_android.model.TranscriptTurn]
 * carries a stable `key`, a `speaker` (USER/AGENT), free-form `text`, and a
 * `status` (IN_PROGRESS / END / INTERRUPTED). We track which turn keys we've
 * already analyzed so partial (IN_PROGRESS) turns get re-analyzed as they
 * grow, without double-counting finished ones.
 */
class FillerFreeViewModel(
    application: Application,
    private val analyticsRepository: TranscriptAnalyticsRepository = TranscriptAnalyticsRepositoryImpl(),
) : AndroidViewModel(application) {

    private val conversationRepository = ConversationRepository()
    private val sessionManager = AgoraConversationSessionManager(application)

    private val _uiState = MutableStateFlow(FillerFreeUiState())
    val uiState: StateFlow<FillerFreeUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<FillerFreeUiEvent>(Channel.BUFFERED)
    val uiEvents = eventChannel.receiveAsFlow()

    private var activeAgentId: String? = null

    // Tracks the last-seen text length per turn key, so we only feed the
    // *new* substring of a growing IN_PROGRESS turn into the analyzer,
    // rather than re-analyzing the whole turn on every partial update.
    private val analyzedLengthByTurnKey = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch {
            analyticsRepository.stats.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
        viewModelScope.launch {
            analyticsRepository.events.collect { event ->
                _uiState.update { current ->
                    current.copy(recentEvents = (current.recentEvents + event).takeLast(20))
                }
                if (event.type == SpeechEventType.AGENT_INTERRUPTION) {
                    eventChannel.trySend(FillerFreeUiEvent.InterruptionFlash)
                }
            }
        }

        viewModelScope.launch {
            sessionManager.snapshot.collect { snapshot ->
                val nowMs = System.currentTimeMillis()
                var liveTranscriptText = _uiState.value.liveTranscript

                for (turn in snapshot.transcriptTurns) {
                    val previousLength = analyzedLengthByTurnKey[turn.key] ?: 0
                    if (turn.text.length <= previousLength) continue
                    val newChunk = turn.text.substring(previousLength)
                    analyzedLengthByTurnKey[turn.key] = turn.text.length

                    when (turn.speaker) {
                        TranscriptSpeaker.USER -> {
                            analyticsRepository.onUserTranscriptChunk(newChunk, nowMs)
                            liveTranscriptText = "$liveTranscriptText $newChunk".trim()
                        }
                        TranscriptSpeaker.AGENT -> {
                            // A user turn is considered interrupted mid-speech when
                            // the agent starts speaking while the most recent user
                            // turn is still IN_PROGRESS (i.e. barge-in happened).
                            val userWasSpeaking = snapshot.transcriptTurns
                                .filter { it.speaker == TranscriptSpeaker.USER }
                                .maxByOrNull { it.createdAtMillis }
                                ?.status == TranscriptTurnStatus.IN_PROGRESS ||
                                turn.status == TranscriptTurnStatus.INTERRUPTED
                            analyticsRepository.onAgentTranscriptChunk(newChunk, nowMs, userWasSpeaking)
                        }
                    }
                }

                _uiState.update { it.copy(liveTranscript = liveTranscriptText) }
            }
        }
    }

    fun selectTopic(topic: SpeechTopic) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }

    fun startSession() {
        val topic = _uiState.value.selectedTopic ?: SpeechTopic.FREE_TALK
        analyticsRepository.reset()
        analyzedLengthByTurnKey.clear()
        _uiState.update {
            it.copy(
                screen = FillerFreeUiState.Screen.IN_SESSION,
                isConnecting = true,
                liveTranscript = "",
                recentEvents = emptyList(),
                summary = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                val bootstrap = conversationRepository.requestSessionBootstrap()
                sessionManager.connect(bootstrap) { channel, rtcUid, rtmUserId ->
                    conversationRepository.renewTokens(
                        channel = channel,
                        rtcUid = rtcUid,
                        rtmUserId = rtmUserId,
                    )
                }

                val requesterRtcUid = sessionManager.snapshot.value.localRtcUid
                    .takeIf { it > 0 }
                    ?.toString()
                    ?: bootstrap.uid

                val prompt = CoachAgentPromptBuilder.build(topic)
                val inviteResult = conversationRepository.inviteAgent(
                    channelName = bootstrap.channel,
                    requesterRtcUid = requesterRtcUid,
                    systemPrompt = prompt,
                )
                activeAgentId = inviteResult.agentId
                sessionManager.setActiveAgentId(activeAgentId)
            }.onSuccess {
                _uiState.update { it.copy(isConnecting = false, isSessionActive = true) }
            }.onFailure { error ->
                sessionManager.disconnect(resetSnapshot = true)
                activeAgentId = null
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isSessionActive = false,
                        screen = FillerFreeUiState.Screen.TOPIC_SELECT,
                        errorMessage = error.message ?: "Could not start the session.",
                    )
                }
            }
        }
    }

    fun endSession() {
        viewModelScope.launch {
            runCatching {
                activeAgentId?.let { agentId ->
                    val channelName = sessionManager.snapshot.value.channelName
                    if (channelName != null) {
                        conversationRepository.stopConversation(agentId, channelName)
                    }
                }
            }
            activeAgentId = null
            sessionManager.setActiveAgentId(null)
            sessionManager.disconnect(resetSnapshot = true)

            val summary = analyticsRepository.buildSummary()
            _uiState.update {
                it.copy(
                    screen = FillerFreeUiState.Screen.SUMMARY,
                    isSessionActive = false,
                    summary = summary,
                )
            }
        }
    }

    fun startNewSession() {
        analyticsRepository.reset()
        analyzedLengthByTurnKey.clear()
        _uiState.update {
            FillerFreeUiState(topics = it.topics)
        }
    }

    override fun onCleared() {
        sessionManager.release()
        super.onCleared()
    }
}

private inline fun MutableStateFlow<FillerFreeUiState>.update(
    block: (FillerFreeUiState) -> FillerFreeUiState,
) {
    value = block(value)
}
