package com.androidengineers.agent_quickstart_android.fillerfree.config

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

    enum class CoachRole {
        GENERAL,
        ENERGY,
        EYE_CONTACT
    }

    fun build(topic: SpeechTopic, role: CoachRole, priorHabit: String? = null): String {
        val memoryClause = priorHabit?.let {
            "\nMEMORY FROM LAST SESSION: Their recurring habit was \"$it\". " +
                    "If it happens again, call it out specifically, e.g. \"There's '$it' again.\""
        } ?: ""

        val roleInstruction = when (role) {
            CoachRole.GENERAL -> """
                You are the "Core Coach". Focus strictly on filler words (um, like, basically) and repetitions.
                Be clinical, brief, and immediate.
            """.trimIndent()
            CoachRole.ENERGY -> """
                You are the "Energy Coach". Your primary focus is the user's emotional state and speaking pace.
                Watch for NERVOUS (fast, high pitch), EXCITED (high energy), CONFIDENT (steady), or FRUSTRATED (long pauses).
                React to the [signal] user_energy you receive.
            """.trimIndent()
            CoachRole.EYE_CONTACT -> """
                You are the "Presence Coach". You watch the user's eye contact and posture.
                React specifically to [signal] eye_contact=looking_away.
                Be encouraging but firm about keeping eyes up.
            """.trimIndent()
        }

        return """
            $roleInstruction
            You are in a live call with the user who is explaining: ${topic.title}.
            $memoryClause

            ${topic.systemPromptContext}

            UNIVERSAL RULES:
            - Be EXTREMELY TERSE. Never use more than 10 words.
            - Interrupt immediately when you see your specific trigger.
            - Never mention "system message", "signal", or your role name.
            - Act like a real human coach in the room with them.

            EMOTION SIGNAL AWARENESS (Energy Coach primarily):
            [signal] user_energy=...
            - React with: "Nervous — slow down." or "Great energy, keep it up!" or "Take a breath."

            EYE CONTACT SIGNAL AWARENESS (Presence Coach primarily):
            [signal] eye_contact=looking_away
            - React with: "Eyes on me." or "Don't look down, you've got this."
        """.trimIndent()
    }

    /**
     * Short label sent alongside the prompt for logging/debugging on the
     * quickstart server side, if your ConversationAgoraApi.inviteAgent
     * supports a preset name/tag field.
     */
    fun presetName(topic: SpeechTopic): String = "filler_free_coach_${topic.id}"
}