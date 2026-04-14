package com.replock.app.ml.rep

import com.replock.app.domain.model.RepPhase
import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Counts push-up repetitions from a stream of [LandmarkFrame]s using
 * elbow-angle geometry.
 *
 * ## Algorithm
 * For each frame:
 * 1. Compute the elbow angle on each arm (angle at the elbow joint formed
 *    by the shoulder→elbow and wrist→elbow vectors).
 * 2. Average visible sides; prefer both arms when available for robustness.
 * 3. Classify the angle into a [RepPhase]:
 *    - `DOWN`       → angle < [DOWN_ANGLE_DEG] (~80°)  — low position
 *    - `UP`         → angle > [UP_ANGLE_DEG]   (~155°) — arms extended
 *    - `TRANSITION` → angle in between
 * 4. Count a rep on the **DOWN → UP** transition (concentric phase complete).
 *
 * A rep is only counted once per descent, preventing double-counts for
 * noisy frames near the threshold.
 */
class PushUpCounter {

    // ── Thresholds ────────────────────────────────────────────────────────────
    companion object {
        /** Elbow angle (degrees) at or below which we classify as DOWN. */
        const val DOWN_ANGLE_DEG = 80f

        /** Elbow angle (degrees) at or above which we classify as UP. */
        const val UP_ANGLE_DEG  = 155f
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private var currentPhase = RepPhase.UP   // assume starting position is UP

    /** Total rep count for the current session. */
    var repCount = 0
        private set

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Feed a landmark frame and receive the updated [RepState].
     *
     * Returns the **previous** state wrapped in [RepState] if no valid elbow
     * angles can be computed (e.g. the person is off-screen).
     */
    fun process(frame: LandmarkFrame): RepState {
        val angleDeg = bestElbowAngle(frame)
            ?: return RepState(currentPhase, confidence = 0f)

        val newPhase = classifyAngle(angleDeg)

        // ── Rep counting state machine ──────────────────────────────────────
        if (currentPhase == RepPhase.DOWN && newPhase == RepPhase.UP) {
            repCount++
        }
        currentPhase = newPhase

        return RepState(
            phase      = newPhase,
            confidence = computeConfidence(newPhase, angleDeg)
        )
    }

    /** Reset counter and state (call at session start / stop). */
    fun reset() {
        repCount     = 0
        currentPhase = RepPhase.UP
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun classifyAngle(angleDeg: Float): RepPhase = when {
        angleDeg <= DOWN_ANGLE_DEG -> RepPhase.DOWN
        angleDeg >= UP_ANGLE_DEG   -> RepPhase.UP
        else                       -> RepPhase.TRANSITION
    }

    /**
     * How strongly the angle is at each phase's extreme (0 = boundary, 1 = deep).
     * Used for UI feedback (e.g. progress rings or colour intensity).
     */
    private fun computeConfidence(phase: RepPhase, angleDeg: Float): Float = when (phase) {
        RepPhase.DOWN       -> ((DOWN_ANGLE_DEG - angleDeg) / DOWN_ANGLE_DEG).coerceIn(0f, 1f)
        RepPhase.UP         -> ((angleDeg - UP_ANGLE_DEG) / (180f - UP_ANGLE_DEG)).coerceIn(0f, 1f)
        RepPhase.TRANSITION -> 0f
    }

    /**
     * Returns the averaged elbow angle across all visible arms.
     * Averaging left and right reduces asymmetry noise.
     */
    private fun bestElbowAngle(frame: LandmarkFrame): Float? {
        val leftAngle  = elbowAngle(frame.leftShoulder,  frame.leftElbow,  frame.leftWrist)
        val rightAngle = elbowAngle(frame.rightShoulder, frame.rightElbow, frame.rightWrist)
        return when {
            leftAngle != null && rightAngle != null -> (leftAngle + rightAngle) / 2f
            leftAngle  != null                      -> leftAngle
            rightAngle != null                      -> rightAngle
            else                                    -> null
        }
    }

    /**
     * Computes the angle (in degrees) at [elbow] formed by the
     * [shoulder]–[elbow]–[wrist] joint triplet using the dot-product formula.
     *
     * Only uses x/y image-plane coordinates; z depth from ML Kit is less
     * reliable on standard front cameras so we deliberately ignore it.
     *
     * Returns `null` if any joint is missing or the vectors have zero length.
     */
    private fun elbowAngle(shoulder: Joint?, elbow: Joint?, wrist: Joint?): Float? {
        if (shoulder == null || elbow == null || wrist == null) return null

        // Vector from elbow to shoulder
        val v1x = shoulder.x - elbow.x
        val v1y = shoulder.y - elbow.y

        // Vector from elbow to wrist
        val v2x = wrist.x - elbow.x
        val v2y = wrist.y - elbow.y

        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)

        // Guard against zero-length vectors (landmark jitter / same position)
        if (mag1 < 1e-6f || mag2 < 1e-6f) return null

        val cosAngle = ((v1x * v2x + v1y * v2y) / (mag1 * mag2))
            .coerceIn(-1.0, 1.0)   // clamp for floating-point safety

        return Math.toDegrees(acos(cosAngle)).toFloat()
    }
}
