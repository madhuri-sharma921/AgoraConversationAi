package com.androidengineers.agent_quickstart_android.fillerfree.domain.usecase

import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.AttentionLabel
import com.androidengineers.agent_quickstart_android.fillerfree.domain.model.AttentionSignal

/**
 * Pure function, same style as DetectEmotionSignalUseCase: takes raw
 * per-frame face-detection output and turns it into a stable
 * [AttentionSignal], smoothing over single-frame noise so a momentary
 * blink or fast head-turn doesn't flip the label every 30ms.
 *
 * Not an ML model itself — ML Kit does the actual face/pose detection;
 * this just classifies its output into a coaching-relevant label and
 * tracks how long the user has held that state.
 */
class DetectAttentionSignalUseCase {

    private var currentLabel: AttentionLabel = AttentionLabel.UNKNOWN
    private var labelStartedAtMs: Long = 0L

    /**
     * @param headYawDegrees ML Kit's Face.headEulerAngleY (positive = turned
     *   toward the device's right from the camera's point of view), or null
     *   if no face was detected this frame.
     * @param timestampMs frame timestamp
     */
    operator fun invoke(headYawDegrees: Float?, timestampMs: Long): AttentionSignal {
        val rawLabel = when {
            headYawDegrees == null -> AttentionLabel.NO_FACE_DETECTED
            kotlin.math.abs(headYawDegrees) <= LOOKING_AWAY_YAW_THRESHOLD_DEGREES -> AttentionLabel.LOOKING_AT_CAMERA
            else -> AttentionLabel.LOOKING_AWAY
        }

        if (rawLabel != currentLabel) {
            currentLabel = rawLabel
            labelStartedAtMs = timestampMs
        }

        return AttentionSignal(
            label = currentLabel,
            headYawDegrees = headYawDegrees,
            continuousDurationMs = (timestampMs - labelStartedAtMs).coerceAtLeast(0L),
            computedAtMs = timestampMs,
        )
    }

    fun reset() {
        currentLabel = AttentionLabel.UNKNOWN
        labelStartedAtMs = 0L
    }

    companion object {
        // Beyond this yaw, the user is turned far enough from the camera
        // that they're very unlikely to be making eye contact with whoever
        // they're practicing in front of (an interviewer, an audience, etc).
        private const val LOOKING_AWAY_YAW_THRESHOLD_DEGREES = 25f
    }
}