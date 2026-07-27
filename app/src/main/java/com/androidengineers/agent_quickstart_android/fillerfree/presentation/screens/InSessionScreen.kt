package com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionStats
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEvent
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEventType
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechTopic
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme.FillerFreeColors
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme.FillerFreeType

@Composable
fun InSessionScreen(
    modifier: Modifier = Modifier,
    topic: SpeechTopic?,
    isConnecting: Boolean,
    liveTranscript: String,
    stats: SessionStats,
    recentEvents: List<SpeechEvent>,
    onEndSession: () -> Unit,
) {
    val lastEventIsInterruption = recentEvents.lastOrNull()?.type == SpeechEventType.AGENT_INTERRUPTION

    val backgroundColor by animateColorAsState(
        targetValue = if (lastEventIsInterruption) FillerFreeColors.signalAmber.copy(alpha = 0.10f)
        else FillerFreeColors.background,
        animationSpec = tween(220),
        label = "sessionBackground",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = topic?.title ?: "Free talk",
                    style = FillerFreeType.interruptionLine,
                    color = FillerFreeColors.textPrimary,
                )
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = FillerFreeColors.textSecondary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CounterChip(
                    label = "FILLERS",
                    value = stats.fillerCount,
                    modifier = Modifier.weight(1f),
                )
                CounterChip(
                    label = "REPEATS",
                    value = stats.repetitionCount,
                    modifier = Modifier.weight(1f),
                )
                CounterChip(
                    label = "CUT-INS",
                    value = stats.interruptionCount,
                    accent = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FillerFreeColors.surface)
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = liveTranscript.ifBlank { "Start talking. Silence means you're doing it right." },
                        style = FillerFreeType.body,
                        color = if (liveTranscript.isBlank()) FillerFreeColors.textMuted else FillerFreeColors.textPrimary,
                    )
                }
            }

            MicIndicator(
                isPulsing = !lastEventIsInterruption,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 20.dp),
            )

            Button(
                onClick = onEndSession,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FillerFreeColors.surfaceRaised,
                    contentColor = FillerFreeColors.textPrimary,
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "End session", style = FillerFreeType.interruptionLine)
            }
        }
    }
}

@Composable
private fun CounterChip(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FillerFreeColors.surface)
            .padding(vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = FillerFreeType.counterNumber,
                color = if (accent && value > 0) FillerFreeColors.signalAmber else FillerFreeColors.textPrimary,
            )
            Text(
                text = label,
                style = FillerFreeType.counterLabel,
                color = FillerFreeColors.textMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun MicIndicator(
    isPulsing: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPulsing) 1f else 1.15f,
        animationSpec = tween(180),
        label = "micScale",
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isPulsing) FillerFreeColors.signalGreen.copy(alpha = 0.15f)
                else FillerFreeColors.signalAmber.copy(alpha = 0.35f),
            )
            .padding(18.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isPulsing) FillerFreeColors.signalGreen else FillerFreeColors.signalAmber)
                .padding(14.dp),
        )
    }
    // NOTE: `scale` is computed for a future scaleX/scaleY graphicsLayer pass;
    // wire it in via Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    // on the outer Box once you're happy with the base visual.
}