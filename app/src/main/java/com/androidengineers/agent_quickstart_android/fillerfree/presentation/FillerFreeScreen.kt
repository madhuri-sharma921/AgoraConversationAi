package com.androidengineers.agent_quickstart_android.fillerfree.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens.InSessionScreen
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens.ProgressScreen
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
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = FillerFreeColors.background,
    ) { padding ->
        val contentModifier = Modifier.padding(padding)

        when (uiState.screen) {
            FillerFreeUiState.Screen.TOPIC_SELECT -> TopicSelectScreen(
                modifier = contentModifier,
                userName = uiState.userName,
                onNameChanged = viewModel::setUserName,
                topics = uiState.topics,
                selectedTopic = uiState.selectedTopic,
                errorMessage = uiState.errorMessage,
                onTopicSelected = viewModel::selectTopic,
                onStart = onRequestStart,
            )

            FillerFreeUiState.Screen.IN_SESSION -> InSessionScreen(
                modifier = contentModifier,
                userName = uiState.userName,
                topic = uiState.selectedTopic,
                isConnecting = uiState.isConnecting,
                isSessionActive = uiState.isSessionActive,
                liveTranscript = uiState.liveTranscript,
                stats = uiState.stats,
                recentEvents = uiState.recentEvents,
                currentEmotion = uiState.currentEmotion,
                agentState = uiState.agentState,
                eyeContactCoachingEnabled = uiState.eyeContactCoachingEnabled,
                hasCameraPermission = uiState.hasCameraPermission,
                eyeContactState = uiState.eyeContactState,
                eyeContactAnalyzer = viewModel.eyeContactAnalyzer,
                onEyeContactCoachingToggled = viewModel::setEyeContactCoachingEnabled,
                onEndSession = viewModel::endSession,
            )

            FillerFreeUiState.Screen.SUMMARY -> SessionSummaryScreen(
                modifier = contentModifier,
                summary = uiState.summary,
                onStartNewSession = viewModel::startNewSession,
                onViewProgress = viewModel::openProgressScreen,
            )

            FillerFreeUiState.Screen.PROGRESS -> ProgressScreen(
                modifier = contentModifier,
                summary = uiState.progressSummary,
                onBack = viewModel::closeProgressScreen,
            )
        }
    }
}
