package com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.androidengineers.agent_quickstart_android.R
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.AttentionLabel
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.AttentionSignal
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.CoachRole
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.EmotionLabel
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
    currentEmotion: EmotionLabel? = null,
    activeCoachRole: CoachRole = CoachRole.DELIVERY,
    visualCoachingEnabled: Boolean = false,
    attentionSignal: AttentionSignal? = null,
    onToggleVisualCoaching: (Boolean) -> Unit = {},
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

            CoachRoleRow(
                activeRole = activeCoachRole,
                modifier = Modifier.padding(top = 12.dp),
            )

            EmotionChip(
                emotion = currentEmotion,
                modifier = Modifier.padding(top = 8.dp),
            )

            InterruptLatencyChip(
                latencyMs = stats.lastInterruptionLatencyMs,
                modifier = Modifier.padding(top = 6.dp),
            )

            VisualCoachingToggleRow(
                enabled = visualCoachingEnabled,
                onToggle = onToggleVisualCoaching,
                modifier = Modifier.padding(top = 10.dp),
            )

            if (visualCoachingEnabled) {
                AttentionChip(
                    signal = attentionSignal,
                    modifier = Modifier.padding(top = 6.dp),
                )
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

/**
 * Shows the 3 coach personas (point 3: "multiple AI coaches") as a row of
 * chips, with whichever one currently has the floor highlighted. Honest
 * about the underlying reality: only one is actually joined/speaking at a
 * time (see CoachRole.kt / AgoraConversationSessionManager.setCoachAgent),
 * but this makes the hand-off between them visible and legible — the part
 * that actually reads as "3 coaches" to the person using the app, more than
 * the audio itself would.
 */
@Composable
private fun CoachRoleRow(
    activeRole: CoachRole,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CoachRole.ALL.forEach { role ->
            val isActive = role == activeRole
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isActive) FillerFreeColors.signalGreen.copy(alpha = 0.16f)
                        else FillerFreeColors.surface,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = role.displayName,
                    style = FillerFreeType.counterLabel,
                    color = if (isActive) FillerFreeColors.signalGreen else FillerFreeColors.textMuted,
                )
            }
        }
    }
}

/**
 * Coarse, text-derived coaching read (see DetectEmotionSignalUseCase) shown
 * as a single quiet line under the topic title. Every EmotionLabel gets its
 * own bundled emoji image + line — including FLAT and UNKNOWN — so no
 * detected feeling is ever hidden from the user, even the "nothing much
 * going on" ones.
 *
 * Emoji are bundled as drawable resources (Noto Emoji, Apache 2.0) rather
 * than rendered as Unicode text. Unicode emoji fall back to whatever emoji
 * font each OEM ships (Samsung/MIUI/stock Android all draw them
 * differently, sometimes changing the read of the expression), so we lock
 * the artwork the same way WhatsApp does: bundle it, don't rely on the
 * system font.
 */
@Composable
private fun EmotionChip(
    emotion: EmotionLabel?,
    modifier: Modifier = Modifier,
) {
    val (emojiRes, label, textColor) = when (emotion) {
        EmotionLabel.CONFIDENT -> Triple(R.drawable.emoji_confident, "Sounding confident", FillerFreeColors.signalGreen)
        EmotionLabel.EXCITED -> Triple(R.drawable.emoji_excited, "Energy's up", FillerFreeColors.signalGreen)
        EmotionLabel.NERVOUS -> Triple(R.drawable.emoji_nervous, "Pace is climbing — slow down", FillerFreeColors.signalAmber)
        EmotionLabel.FRUSTRATED -> Triple(R.drawable.emoji_frustrated, "Long pauses — take a breath", FillerFreeColors.signalRed)
        EmotionLabel.FLAT -> Triple(R.drawable.emoji_flat, "Steady and even — okay to add energy", FillerFreeColors.textSecondary)
        EmotionLabel.UNKNOWN -> Triple(R.drawable.emoji_unknown, "Reading your pace…", FillerFreeColors.textMuted)
        null -> Triple(R.drawable.emoji_unknown, "Reading your pace…", FillerFreeColors.textMuted)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(id = emojiRes),
            contentDescription = label,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = FillerFreeType.counterLabel,
            color = textColor,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Surfaces Agora's real, measured barge-in latency (user speaks -> agent
 * actually stops) as a live, visible product signal rather than invisible
 * plumbing. Only shows once a real interruption has happened this session;
 * `stats.lastInterruptionLatencyMs` comes from
 * AgoraConversationSessionManager's own timestamps (see
 * requestAgentInterruptFromUserSpeech / updateAgentState), not a chunk-arrival
 * heuristic, so the number reflects the actual cut-in speed.
 */
@Composable
private fun InterruptLatencyChip(
    latencyMs: Long?,
    modifier: Modifier = Modifier,
) {
    if (latencyMs == null) return

    val color = when {
        latencyMs <= 400L -> FillerFreeColors.signalGreen
        latencyMs <= 900L -> FillerFreeColors.signalAmber
        else -> FillerFreeColors.textSecondary
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = "Cut in: ${latencyMs}ms",
            style = FillerFreeType.counterLabel,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * Point 4 (visual coaching, tier 1): opt-in toggle for the local camera +
 * face-detection attention read. Off by default — enabling it triggers
 * [onToggleVisualCoaching], which the caller (FillerFreeScreen) wires to
 * both a CAMERA permission check and VisualCoachManager.start/stop. No
 * camera preview is shown here on purpose: this is a coaching signal, not
 * a video call — see VisualCoachManager.kt for the full scope note.
 */
@Composable
private fun VisualCoachingToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Eye-contact coaching",
            style = FillerFreeType.counterLabel,
            color = FillerFreeColors.textSecondary,
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = FillerFreeColors.signalGreen,
                checkedThumbColor = FillerFreeColors.textPrimary,
            ),
        )
    }
}

/**
 * Shows the local, on-device attention read (see DetectAttentionSignalUseCase)
 * while visual coaching is on. Only flags LOOKING_AWAY once it's held for a
 * couple of seconds, so a quick natural glance away doesn't trigger a
 * distracting callout.
 */
@Composable
private fun AttentionChip(
    signal: AttentionSignal?,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when {
        signal == null -> "Starting camera…" to FillerFreeColors.textMuted
        signal.label == AttentionLabel.LOOKING_AWAY && signal.continuousDurationMs > 2_000L ->
            "Looking away — bring your eyes back to camera" to FillerFreeColors.signalAmber
        signal.label == AttentionLabel.LOOKING_AT_CAMERA -> "Good eye contact" to FillerFreeColors.signalGreen
        signal.label == AttentionLabel.NO_FACE_DETECTED -> "Face not visible" to FillerFreeColors.textMuted
        else -> "Reading eye contact…" to FillerFreeColors.textMuted
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = text,
            style = FillerFreeType.counterLabel,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
        )
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