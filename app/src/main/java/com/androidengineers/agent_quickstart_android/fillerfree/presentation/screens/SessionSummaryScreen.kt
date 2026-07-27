package com.androidengineers.agent_quickstart_android.fillerfree.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SessionSummary
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme.FillerFreeColors
import com.androidengineers.agent_quickstart_android.fillerfree.presentation.theme.FillerFreeType

@Composable
fun SessionSummaryScreen(
    summary: SessionSummary?,
    onStartNewSession: () -> Unit,
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
                text = "Session done",
                style = FillerFreeType.screenTitle,
                color = FillerFreeColors.textPrimary,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FillerFreeColors.surface)
                    .padding(20.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SummaryStat(label = "FILLERS", value = summary?.stats?.fillerCount ?: 0)
                        SummaryStat(label = "REPEATS", value = summary?.stats?.repetitionCount ?: 0)
                        SummaryStat(label = "CUT-INS", value = summary?.stats?.interruptionCount ?: 0)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FillerFreeColors.surfaceRaised)
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "COACH'S NOTE",
                        style = FillerFreeType.counterLabel,
                        color = FillerFreeColors.signalAmber,
                    )
                    Text(
                        text = summary?.closingTip ?: "No data captured this round.",
                        style = FillerFreeType.body,
                        color = FillerFreeColors.textPrimary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            Button(
                onClick = onStartNewSession,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FillerFreeColors.textPrimary,
                    contentColor = FillerFreeColors.background,
                ),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                Text(text = "Try again, tighter", style = FillerFreeType.interruptionLine)
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = FillerFreeType.counterNumber,
            color = FillerFreeColors.textPrimary,
        )
        Text(
            text = label,
            style = FillerFreeType.counterLabel,
            color = FillerFreeColors.textMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}