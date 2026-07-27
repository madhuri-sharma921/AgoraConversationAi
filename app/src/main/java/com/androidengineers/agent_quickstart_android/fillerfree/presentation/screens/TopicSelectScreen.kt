package com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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