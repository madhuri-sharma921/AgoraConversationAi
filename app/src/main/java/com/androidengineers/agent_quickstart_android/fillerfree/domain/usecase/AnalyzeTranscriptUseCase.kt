package com.androidengineers.agent_quickstart_android.fillerfree.domain.usecase

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEvent
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechEventType
import java.util.UUID

/**
 * Pure function-style use case: takes a raw transcript chunk and the recent
 * history, returns any newly detected [SpeechEvent]s. No Android/Agora
 * dependencies here on purpose — fully unit testable.
 */
class AnalyzeTranscriptUseCase(
    private val fillerWords: Set<String> = DEFAULT_FILLER_WORDS,
) {

    /**
     * @param newText the latest chunk of user speech transcript
     * @param recentSentences last few sentences already spoken, oldest first,
     *   used to detect repetition (naive substring/overlap check)
     * @param timestampMs when this chunk arrived
     */
    operator fun invoke(
        newText: String,
        recentSentences: List<String>,
        timestampMs: Long,
    ): List<SpeechEvent> {
        if (newText.isBlank()) return emptyList()

        val events = mutableListOf<SpeechEvent>()
        val normalized = newText.lowercase().trim()
        val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

        // Filler word detection: count occurrences of known filler tokens.
        words.forEach { word ->
            val cleaned = word.trim(',', '.', '!', '?')
            if (cleaned in fillerWords) {
                events += SpeechEvent(
                    id = UUID.randomUUID().toString(),
                    type = SpeechEventType.FILLER_WORD,
                    text = cleaned,
                    timestampMs = timestampMs,
                )
            }
        }

        // Repetition detection: naive word-overlap ratio against recent sentences.
        val newWordSet = words.toSet()
        if (newWordSet.size >= 4) {
            for (prior in recentSentences.takeLast(3)) {
                val priorWords = prior.lowercase()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .toSet()
                if (priorWords.isEmpty()) continue

                val overlap = newWordSet.intersect(priorWords).size
                val overlapRatio = overlap.toDouble() / newWordSet.size
                if (overlapRatio >= REPETITION_OVERLAP_THRESHOLD) {
                    events += SpeechEvent(
                        id = UUID.randomUUID().toString(),
                        type = SpeechEventType.REPETITION,
                        text = newText,
                        timestampMs = timestampMs,
                    )
                    break
                }
            }
        }

        return events
    }

    companion object {
        private const val REPETITION_OVERLAP_THRESHOLD = 0.7

        val DEFAULT_FILLER_WORDS = setOf(
            "um", "umm", "uh", "uhh", "like", "basically", "actually",
            "literally", "so", "kind", "sort", "you know", "i mean",
            "right", "okay so", "yeah so",
        )
    }
}
