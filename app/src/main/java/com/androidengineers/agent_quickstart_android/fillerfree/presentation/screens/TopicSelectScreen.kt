package com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechTopic
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.DailyProgressUiModel
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme.FillerFreeColors
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme.FillerFreeType

@Composable
fun TopicSelectScreen(
    topics: List<SpeechTopic>,
    selectedTopic: SpeechTopic?,
    errorMessage: String?,
    onTopicSelected: (SpeechTopic) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    dailyProgress: DailyProgressUiModel? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FillerFreeColors.background)
            .padding(24.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Filler-Free",
                style = FillerFreeType.screenTitle,
                color = FillerFreeColors.textPrimary,
            )
            Text(
                text = "It interrupts you the moment you ramble. Pick a topic.",
                style = FillerFreeType.body,
                color = FillerFreeColors.textSecondary,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            )

            dailyProgress?.let {
                DailyProgressCard(
                    progress = it,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(topics) { topic ->
                    TopicCard(
                        topic = topic,
                        isSelected = topic.id == selectedTopic?.id,
                        onClick = { onTopicSelected(topic) },
                    )
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    style = FillerFreeType.body,
                    color = FillerFreeColors.signalRed,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Button(
                onClick = onStart,
                enabled = selectedTopic != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FillerFreeColors.textPrimary,
                    contentColor = FillerFreeColors.background,
                    disabledContainerColor = FillerFreeColors.surfaceRaised,
                    disabledContentColor = FillerFreeColors.textMuted,
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Start talking",
                    style = FillerFreeType.interruptionLine,
                )
            }
        }
    }
}

/**
 * Point 5 (daily improvement memory): a small trend card shown above the
 * topic list once at least one past session exists locally. Everything
 * here comes from SessionHistoryRepository (on-device Room storage) via
 * BuildDailyProgressUseCase — nothing is fetched from the server.
 */
@Composable
private fun DailyProgressCard(
    progress: DailyProgressUiModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FillerFreeColors.surface)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = "YOUR PROGRESS",
                style = FillerFreeType.counterLabel,
                color = FillerFreeColors.textMuted,
            )

            if (progress.issuesPerMinuteTrend.size >= 2) {
                TrendSparkline(
                    values = progress.issuesPerMinuteTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }

            progress.improvementText?.let {
                Text(
                    text = it,
                    style = FillerFreeType.body,
                    color = FillerFreeColors.textPrimary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            progress.mostCommonFillerWord?.let {
                Text(
                    text = "Most common filler across recent sessions: \"$it\"",
                    style = FillerFreeType.body,
                    color = FillerFreeColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Text(
                text = "Based on your last ${progress.sessionCount} session(s), on this device.",
                style = FillerFreeType.counterLabel,
                color = FillerFreeColors.textMuted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/**
 * Minimal Canvas sparkline for issues/minute across recent sessions,
 * oldest-first (left to right). Deliberately simple — no axes, no
 * tooltips — this is a glance-level trend indicator, not a full chart.
 */
@Composable
private fun TrendSparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    val maxValue = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    androidx.compose.foundation.Canvas(
        modifier = modifier.height(48.dp),
    ) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value / maxValue).toFloat().coerceIn(0f, 1f)
            val y = size.height - (normalized * size.height)
            androidx.compose.ui.geometry.Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = FillerFreeColors.signalGreen,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f,
            )
        }
        points.forEach { point ->
            drawCircle(
                color = FillerFreeColors.signalGreen,
                radius = 5f,
                center = point,
            )
        }
    }
}

@Composable
private fun TopicCard(
    topic: SpeechTopic,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) FillerFreeColors.surfaceRaised else FillerFreeColors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = topic.title,
                style = FillerFreeType.interruptionLine,
                color = if (isSelected) FillerFreeColors.signalAmber else FillerFreeColors.textPrimary,
            )
            Text(
                text = topic.description,
                style = FillerFreeType.body,
                color = FillerFreeColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}