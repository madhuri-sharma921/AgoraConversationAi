package com.androidengineers.agent_quickstart_android.fillerfree.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.androidengineers.agent_quickstart_android.data.ConversationRepository
import com.androidengineers.agent_quickstart_android.fillerfree.config.CoachAgentPromptBuilder
import com.androidengineers.agent_quickstart_android.fillerfree.data.SessionHistoryRepositoryImpl
import com.androidengineers.agent_quickstart_android.fillerfree.data.TranscriptAnalyticsRepositoryImpl
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.CoachRole
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEventType
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechTopic
import com.androidengineers.agent_quickstart_android.fillerfree.domain.repository.SessionHistoryRepository
import com.androidengineers.agent_quickstart_android.fillerfree.domain.repository.TranscriptAnalyticsRepository
import com.androidengineers.agent_quickstart_android.fillerfree.domain.usecase.RouteCoachRoleUseCase
import com.androidengineers.agent_quickstart_android.fillerfree.visual.VisualCoachManager
import com.androidengineers.agent_quickstart_android.model.TranscriptSpeaker
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
    private val sessionHistoryRepository: SessionHistoryRepository = SessionHistoryRepositoryImpl(
        application
    ),
) : AndroidViewModel(application) {

    private val conversationRepository = ConversationRepository()
    private val sessionManager = AgoraConversationSessionManager(application)
    private val routeCoachRole = RouteCoachRoleUseCase()


    // Point 4 (visual coaching, tier 1): local-only camera + face detection.
    // Lazily constructed since it touches CameraX APIs that only make sense
    // once the user has actually opted in (see toggleVisualCoaching).
    private val visualCoachManager = VisualCoachManager(application)

    private val _uiState = MutableStateFlow(FillerFreeUiState())
    val uiState: StateFlow<FillerFreeUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<FillerFreeUiEvent>(Channel.BUFFERED)
    val uiEvents = eventChannel.receiveAsFlow()

    private var activeAgentId: String? = null
    private var currentTopic: SpeechTopic? = null

    // Tracks the last-seen text length per turn key, so we only feed the
    // *new* substring of a growing IN_PROGRESS turn into the analyzer,
    // rather than re-analyzing the whole turn on every partial update.
    private val analyzedLengthByTurnKey = mutableMapOf<String, Int>()

    // Multi-coach routing state (point 3). Recent-event timestamps in a
    // rolling window feed RouteCoachRoleUseCase; role switches are
    // throttled since each one is a real server-side leave+join, not a
    // free client-side toggle.
    private val recentFillerTimestampsMs = ArrayDeque<Long>()
    private val recentRepetitionTimestampsMs = ArrayDeque<Long>()
    private var lastRoleSwitchAtMs = 0L
    private var isSwitchingRole = false

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

                when (event.type) {
                    SpeechEventType.FILLER_WORD -> recentFillerTimestampsMs.addLast(event.timestampMs)
                    SpeechEventType.REPETITION -> recentRepetitionTimestampsMs.addLast(event.timestampMs)
                    SpeechEventType.AGENT_INTERRUPTION -> Unit
                }
                pruneWindow(recentFillerTimestampsMs, event.timestampMs)
                pruneWindow(recentRepetitionTimestampsMs, event.timestampMs)
                maybeRouteCoachRole(event.timestampMs)
            }
        }
        viewModelScope.launch {
            analyticsRepository.emotionSignal.collect { signal ->
                _uiState.update { it.copy(currentEmotion = signal.label) }
                sessionManager.pushCoachSignal(
                    "user_energy=${signal.label} wpm=${signal.wordsPerMinute.toInt()}"
                )
                maybeRouteCoachRole(signal.computedAtMs)
            }
        }

        // Real, measured interrupt latency (user speaks -> agent actually
        // stops), sourced from AgoraConversationSessionManager's own
        // timestamps rather than derived from transcript-chunk arrival —
        // see requestAgentInterruptFromUserSpeech / updateAgentState there.
        sessionManager.setOnInterruptLatencyMeasured { latencyMs ->
            analyticsRepository.recordInterruptLatency(latencyMs, System.currentTimeMillis())
            eventChannel.trySend(FillerFreeUiEvent.InterruptionFlash)
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
                            val userWasSpeaking = previousLength == 0 // only count the first chunk of each agent turn
                            analyticsRepository.onAgentTranscriptChunk(newChunk, nowMs, userWasSpeaking)
                        }
                    }
                }

                _uiState.update { it.copy(liveTranscript = liveTranscriptText) }
            }
        }

        // Point 4 (visual coaching): surface the local attention signal in
        // UI state when the feature is on. When it's off, VisualCoachManager
        // is simply never started (see toggleVisualCoaching), so this flow
        // just idles at null.
        viewModelScope.launch {
            visualCoachManager.attentionSignal.collect { signal ->
                _uiState.update { it.copy(attentionSignal = signal) }
            }
        }

        // Point 5 (daily improvement memory): load the trend across recent
        // local sessions once at startup, so it's ready to show on the topic
        // select screen without the user having to ask for it.
        viewModelScope.launch {
            refreshDailyProgress()
        }
    }

    fun selectTopic(topic: SpeechTopic) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }

    fun startSession() {
        val topic = _uiState.value.selectedTopic ?: SpeechTopic.FREE_TALK
        currentTopic = topic
        analyticsRepository.reset()
        analyzedLengthByTurnKey.clear()
        recentFillerTimestampsMs.clear()
        recentRepetitionTimestampsMs.clear()
        lastRoleSwitchAtMs = 0L
        _uiState.update {
            it.copy(
                screen = FillerFreeUiState.Screen.IN_SESSION,
                isConnecting = true,
                liveTranscript = "",
                recentEvents = emptyList(),
                summary = null,
                errorMessage = null,
                activeCoachRole = CoachRole.DELIVERY,
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

                // Every session starts with the Delivery coach holding the
                // floor; RouteCoachRoleUseCase may hand off to Content/Energy
                // as the conversation develops (see maybeRouteCoachRole).
                //
                // Point 5: if a prior session logged a recurring filler habit,
                // pass it as CoachAgentPromptBuilder's memory clause so the
                // agent can reference it ("There's 'um' again") — this is
                // what makes the coach feel like it remembers the user
                // across sessions without any server-side storage.
                val priorHabit = runCatching { sessionHistoryRepository.mostRecentSession() }
                    .getOrNull()
                    ?.topOffender
                val prompt = CoachAgentPromptBuilder.build(topic, CoachRole.DELIVERY, priorHabit)
                val inviteResult = conversationRepository.inviteAgent(
                    channelName = bootstrap.channel,
                    requesterRtcUid = requesterRtcUid,
                    systemPrompt = prompt,
                    role = CoachRole.DELIVERY.id,
                )
                activeAgentId = inviteResult.agentId
                sessionManager.setActiveAgentId(activeAgentId)
                sessionManager.setCoachAgent(
                    role = inviteResult.role,
                    agentId = inviteResult.agentId,
                    rtcUid = inviteResult.rtcUid,
                )
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
            val topicIdForHistory = currentTopic?.id ?: SpeechTopic.FREE_TALK.id
            activeAgentId = null
            currentTopic = null
            recentFillerTimestampsMs.clear()
            recentRepetitionTimestampsMs.clear()
            sessionManager.setActiveAgentId(null)
            sessionManager.disconnect(resetSnapshot = true)
            stopVisualCoaching()

            val summary = analyticsRepository.buildSummary()

            // Point 5: persist this session locally so future sessions can
            // reference it (memory clause) and the daily-progress trend
            // reflects it. Best-effort — a persistence failure shouldn't
            // block showing the summary screen.
            runCatching { sessionHistoryRepository.recordSession(topicIdForHistory, summary) }
            refreshDailyProgress()

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

    /**
     * Point 4 (visual coaching): user-initiated opt-in/opt-out. Requires the
     * caller (see MainActivity) to have already obtained the CAMERA
     * permission — this does not request it, matching how RECORD_AUDIO is
     * handled for [startSession]. Safe to call whether or not a session is
     * currently active; if a session is active, the camera binds/unbinds
     * immediately against the given [lifecycleOwner].
     */
    fun toggleVisualCoaching(enabled: Boolean, lifecycleOwner: LifecycleOwner) {
        _uiState.update { it.copy(visualCoachingEnabled = enabled) }
        if (enabled) {
            visualCoachManager.start(lifecycleOwner)
        } else {
            visualCoachManager.stop()
        }
    }

    private fun stopVisualCoaching() {
        visualCoachManager.stop()
        _uiState.update { it.copy(visualCoachingEnabled = false, attentionSignal = null) }
    }

    private suspend fun refreshDailyProgress() {
        runCatching { sessionHistoryRepository.recentSessions(limit = DAILY_PROGRESS_SESSION_WINDOW) }
            .getOrNull()
            ?.let { recentSessions ->
               // val progress = buildDailyProgress(recentSessions)
             //   _uiState.update { it.copy(dailyProgress = progress?.toUiModel()) }
            }
    }

    /** Drops timestamps older than the routing window from a rolling deque. */
    private fun pruneWindow(timestamps: ArrayDeque<Long>, nowMs: Long) {
        while (timestamps.isNotEmpty() && nowMs - timestamps.first() > ROUTING_WINDOW_MS) {
            timestamps.removeFirst()
        }
    }

    /**
     * Asks [RouteCoachRoleUseCase] who should have the floor right now and,
     * if it disagrees with the current role and we're not mid-switch or
     * still inside the cooldown, kicks off a real server-side switch. Each
     * switch is a genuine leave+join (see AgoraConversationSessionManager /
     * server routes.py switch-role), so this is throttled deliberately —
     * unlike a client-side UI toggle, this has real network/session cost.
     */
    private fun maybeRouteCoachRole(nowMs: Long) {
        if (!_uiState.value.isSessionActive || isSwitchingRole) return
        if (nowMs - lastRoleSwitchAtMs < ROLE_SWITCH_COOLDOWN_MS) return

        val currentRole = _uiState.value.activeCoachRole
        val nextRole = routeCoachRole(
            emotion = _uiState.value.currentEmotion,
            recentFillerCount = recentFillerTimestampsMs.size,
            recentRepetitionCount = recentRepetitionTimestampsMs.size,
            currentRole = currentRole,
        )
        if (nextRole != currentRole) {
            switchToRole(nextRole, nowMs)
        }
    }

    private fun switchToRole(role: CoachRole, nowMs: Long) {
        val channelName = sessionManager.snapshot.value.channelName ?: return
        val requesterRtcUid = sessionManager.snapshot.value.localRtcUid
            .takeIf { it > 0 }
            ?.toString()
            ?: return
        val topic = currentTopic ?: SpeechTopic.FREE_TALK

        isSwitchingRole = true
        lastRoleSwitchAtMs = nowMs
        viewModelScope.launch {
            runCatching {
                val prompt = CoachAgentPromptBuilder.build(topic, role)
                conversationRepository.switchCoachRole(
                    channelName = channelName,
                    requesterRtcUid = requesterRtcUid,
                    role = role.id,
                    systemPrompt = prompt,
                )
            }.onSuccess { result ->
                activeAgentId = result.agentId
                sessionManager.setActiveAgentId(activeAgentId)
                sessionManager.setCoachAgent(
                    role = result.role,
                    agentId = result.agentId,
                    rtcUid = result.rtcUid,
                )
                _uiState.update { it.copy(activeCoachRole = role) }
                eventChannel.trySend(FillerFreeUiEvent.CoachSwitched(role))
            }.onFailure {
                // Non-fatal: stay on the current coach rather than tearing
                // down the session over a failed hand-off. The next routing
                // check will simply try again once conditions still call for it.
            }
            isSwitchingRole = false
        }
    }

    override fun onCleared() {
        sessionManager.setOnInterruptLatencyMeasured(null)
        sessionManager.release()
        visualCoachManager.release()
        super.onCleared()
    }
}

private inline fun MutableStateFlow<FillerFreeUiState>.update(
    block: (FillerFreeUiState) -> FillerFreeUiState,
) {
    value = block(value)
}

private const val ROUTING_WINDOW_MS = 10_000L
private const val ROLE_SWITCH_COOLDOWN_MS = 20_000L
private const val DAILY_PROGRESS_SESSION_WINDOW = 10

/** Maps the domain [com.androidengineers.agent_quickstart_android.fillerfree.domain.model.DailyProgress] to a UI-ready model. */
private fun com.androidengineers.agent_quickstart_android.fillerfree.domain.model.DailyProgress.toUiModel(): DailyProgressUiModel {
    return DailyProgressUiModel(
        sessionCount = sessionCount,
        issuesPerMinuteTrend = issuesPerMinuteTrend,
        mostCommonFillerWord = mostCommonFillerWord,
        improvementText = buildImprovementText(),
    )
}

private fun com.androidengineers.agent_quickstart_android.fillerfree.domain.model.DailyProgress.buildImprovementText(): String? {
    val improvement = issuesPerMinuteImprovement ?: return null
    return when {
        improvement > 0.5 -> "Down %.1f issues/min from your recent average — nice progress.".format(improvement)
        improvement < -0.5 -> "Up %.1f issues/min from your recent average.".format(-improvement)
        else -> "About steady with your recent average."
    }
}