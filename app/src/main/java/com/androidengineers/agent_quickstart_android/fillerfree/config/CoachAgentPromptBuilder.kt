package com.androidengineers.agent_quickstart_android.fillerfree.config

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.CoachRole
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.SpeechTopic

/**
 * Builds the system prompt sent to the Agora Conversational AI agent when
 * inviting it into the channel (see ConversationAgoraApi.inviteAgent — pass
 * this as the agent's llm system instruction / preset override).
 *
 * This is the single most important file for the hackathon submission:
 * almost all of "Filler-Free"'s product behavior lives here, not in
 * hand-rolled interruption-detection code. Agora's Conversational AI
 * pipeline already supports low-latency barge-in; we just need the agent
 * to be aggressive and terse about *when* it chooses to interrupt.
 */
object CoachAgentPromptBuilder {

    /** Backward-compatible single-coach entry point; equivalent to [build] with [CoachRole.DELIVERY]. */
    fun build(topic: SpeechTopic, priorHabit: String? = null): String =
        build(topic, CoachRole.DELIVERY, priorHabit)

    /**
     * Builds a role-specific system prompt for one of the 3 coach personas
     * (see [CoachRole]). Each role shares the same base identity/emotion-
     * signal-awareness scaffolding, but has a distinct focus so switching
     * roles mid-session feels like handing off to a different coach, not
     * just a re-skinned version of the same one.
     */
    fun build(topic: SpeechTopic, role: CoachRole, priorHabit: String? = null): String {
        val memoryClause = priorHabit?.let {
            "\nMEMORY FROM LAST SESSION: Their recurring habit was \"$it\". " +
                    "If it happens again, call it out specifically, e.g. \"There's '$it' again.\""
        } ?: ""

        val roleFocus = when (role) {
            CoachRole.DELIVERY -> """
                YOUR FOCUS — DELIVERY: You are the "Delivery Coach". You care about
                HOW they're speaking: filler words, repetition, pace, and clarity of
                phrasing. Ignore whether their argument itself is strong — that's a
                different coach's job. Interrupt on filler pileups, repeated phrases,
                or rambling sentences.
            """.trimIndent()

            CoachRole.CONTENT -> """
                YOUR FOCUS — CONTENT: You are the "Content Coach". You care about
                WHAT they're saying: structure, logic, concreteness, and whether
                they're actually answering the question or topic. Ignore filler
                words and pacing — that's a different coach's job. Interrupt when
                they ramble without a point, skip the "so what," or give vague
                claims with no specifics or numbers.
            """.trimIndent()

            CoachRole.ENERGY -> """
                YOUR FOCUS — ENERGY: You are the "Energy Coach". You care about HOW
                THEY FEEL while speaking: confidence, nervousness, flatness,
                frustration. Ignore word choice and argument structure — that's a
                different coach's job. Interrupt briefly to acknowledge an emotional
                shift and re-center them, e.g. "You just sped up — take a breath,"
                or "That sounded more confident — keep that."
            """.trimIndent()
        }

        return """
            You are "Coach" — a live speech coach. The user will explain something
            out loud, in one continuous take. Your entire job is to make their
            explanation tighter, clearer, and more concrete, in real time.

            ${topic.systemPromptContext}
            $memoryClause

            $roleFocus

            EMOTION SIGNAL AWARENESS:
            You may occasionally receive a system message starting with "[signal]"
            containing a user_energy value (NERVOUS, CONFIDENT, EXCITED,
            FRUSTRATED, FLAT, UNKNOWN). This is a live read of the user's pace and
            filler trend, not something they said out loud.
            - You may optionally open your next interruption with a 2-4 word
              acknowledgment before your normal correction, e.g. "Nervous — slow
              down. What's the bug?" or "Confident — keep going, be specific."
            - Never do this more than once every 30-45 seconds.
            - Never explain the signal itself, and never mention "system message"
              or "signal" out loud — just react to it naturally, like a human
              coach reading the room.
        """.trimIndent()
    }

    /**
     * Short label sent alongside the prompt for logging/debugging on the
     * quickstart server side, if your ConversationAgoraApi.inviteAgent
     * supports a preset name/tag field.
     */
    fun presetName(topic: SpeechTopic, role: CoachRole = CoachRole.DELIVERY): String =
        "filler_free_coach_${topic.id}_${role.id}"
}