package com.androidengineers.agent_quickstart_android.fillerfree.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens.InSessionScreen
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens.SessionSummaryScreen
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens.TopicSelectScreen
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme.FillerFreeColors

/**
 * Entry point composable. Wire this into your NavHost or set it as the
 * app's start destination via setContent { FillerFreeScreen() }.
 */
@Composable
fun FillerFreeScreen(
    modifier: Modifier = Modifier,
    viewModel: FillerFreeViewModel = viewModel(),
    onRequestStart: () -> Unit = viewModel::startSession,
    // Returns true if the CAMERA permission is already granted; otherwise
    // it should kick off a permission request and return false. The
    // permission-request result itself doesn't flip the Switch — the user
    // just taps it again once granted, keeping this a simple synchronous
    // check rather than needing a callback-after-request round trip here.
    hasCameraPermission: () -> Boolean = { false },
    onRequestCameraPermission: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold(
        modifier = modifier,
        containerColor = FillerFreeColors.background,
    ) { padding ->
        val contentModifier = Modifier.padding(padding)

        when (uiState.screen) {
            FillerFreeUiState.Screen.TOPIC_SELECT -> TopicSelectScreen(
                modifier = contentModifier,
                topics = uiState.topics,
                selectedTopic = uiState.selectedTopic,
                errorMessage = uiState.errorMessage,
                onTopicSelected = viewModel::selectTopic,
                onStart = onRequestStart,
                dailyProgress = uiState.dailyProgress,
            )

            FillerFreeUiState.Screen.IN_SESSION -> InSessionScreen(
                modifier = contentModifier,
                topic = uiState.selectedTopic,
                isConnecting = uiState.isConnecting,
                liveTranscript = uiState.liveTranscript,
                stats = uiState.stats,
                recentEvents = uiState.recentEvents,
                currentEmotion = uiState.currentEmotion,
                activeCoachRole = uiState.activeCoachRole,
                visualCoachingEnabled = uiState.visualCoachingEnabled,
                attentionSignal = uiState.attentionSignal,
                onToggleVisualCoaching = { enabled ->
                    if (!enabled) {
                        viewModel.toggleVisualCoaching(false, lifecycleOwner)
                    } else if (hasCameraPermission()) {
                        viewModel.toggleVisualCoaching(true, lifecycleOwner)
                    } else {
                        onRequestCameraPermission()
                        // Permission dialog is now showing; the Switch stays
                        // off until the user re-taps it after granting, which
                        // avoids needing an async callback threaded back
                        // into this composable.
                    }
                },
                onEndSession = viewModel::endSession,
            )

            FillerFreeUiState.Screen.SUMMARY -> SessionSummaryScreen(
                modifier = contentModifier,
                summary = uiState.summary,
                onStartNewSession = viewModel::startNewSession,
            )
        }
    }
}