package com.replock.app.ml.rep

import com.replock.app.domain.model.RepPhase
import com.replock.app.domain.model.RepState
import com.replock.app.ml.pose.Joint
import com.replock.app.ml.pose.LandmarkFrame
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpCounter {

    companion object {
        // Elbow angle thresholds
        private const val DOWN_THRESHOLD = 90f   // ≤ this  → bottom of push-up
        private const val UP_THRESHOLD   = 150f  // ≥ this  → arms extended (was 160 — too strict)

        // Body-straight validation
        private const val BODY_MIN_ANGLE = 150.0 // hip–knee–ankle angle

        // EMA smoothing: lower α = smoother signal, higher = more responsive
        // 0.25 kills per-frame jitter while staying <2 frames behind the real angle
        private const val EMA_ALPHA = 0.25f

        // Minimum ms between phase transitions — prevents noise double-counting
        private const val DEBOUNCE_MS = 350L
    }

    // ── State ─────────────────────────────────────────────────────────────────
    //
    // currentPhase        — where we are right now (UP / TRANSITION / DOWN)
    // lastDefinitivePhase — last time we were *actually* at an extreme (UP or DOWN)
    //
    // The rep count fires when currentPhase transitions into UP and
    // lastDefinitivePhase was DOWN.  TRANSITION never blocks this — it is just
    // a passthrough zone.  Previously the code did:
    //
    //   if (currentPhase == DOWN && newPhase == UP) repCount++
    //
    // which required jumping from DOWN straight to UP in a single frame.
    // That's essentially impossible: every real push-up passes through TRANSITION
    // on the way up, so the counter was permanently stuck at 0.
    //
    private var currentPhase        = RepPhase.UP
    private var lastDefinitivePhase = RepPhase.UP
    private var lastPhaseChangeTime = 0L
    private var smoothedAngle: Float? = null

    var repCount = 0
        private set

    // ─────────────────────────────────────────────────────────────────────────

    data class Analysis(
        val state: RepState,
        val isFormValid: Boolean,
        val feedback: String
    )

    fun process(frame: LandmarkFrame): Analysis {
        val rawAngle  = bestElbowAngle(frame)
        val isStraight = isBodyStraight(frame)

        // EMA smoothing — stabilises jittery MLKit landmark outputs
        if (rawAngle != null) {
            smoothedAngle = smoothedAngle
                ?.let { prev -> prev + EMA_ALPHA * (rawAngle - prev) }
                ?: rawAngle
        }

        val angle = smoothedAngle
        val now   = System.currentTimeMillis()

        if (angle != null && (now - lastPhaseChangeTime) > DEBOUNCE_MS) {
            val newPhase = classifyAngle(angle)
            if (newPhase != currentPhase) {
                // Count when we return to UP after a confirmed DOWN
                if (newPhase == RepPhase.UP && lastDefinitivePhase == RepPhase.DOWN) {
                    repCount++
                }
                // Only anchor the "last definitive" on actual extremes, not mid-range
                if (newPhase != RepPhase.TRANSITION) {
                    lastDefinitivePhase = newPhase
                }
                currentPhase        = newPhase
                lastPhaseChangeTime = now
            }
        }

        val isFormValid = angle != null && isStraight
        val feedback    = buildFeedback(angle, isStraight, currentPhase, lastDefinitivePhase)

        return Analysis(
            state = RepState(
                phase      = currentPhase,
                confidence = if (angle != null) confidence(currentPhase, angle) else 0f
            ),
            isFormValid = isFormValid,
            feedback    = feedback
        )
    }

    fun reset() {
        repCount            = 0
        currentPhase        = RepPhase.UP
        lastDefinitivePhase = RepPhase.UP
        smoothedAngle       = null
        lastPhaseChangeTime = 0L
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun classifyAngle(deg: Float): RepPhase = when {
        deg <= DOWN_THRESHOLD -> RepPhase.DOWN
        deg >= UP_THRESHOLD   -> RepPhase.UP
        else                  -> RepPhase.TRANSITION
    }

    private fun buildFeedback(
        angle: Float?,
        isStraight: Boolean,
        phase: RepPhase,
        lastDefinitive: RepPhase
    ): String = when {
        angle == null                                       -> "ADJUST CAMERA"
        !isStraight                                         -> "KEEP BACK STRAIGHT"
        phase == RepPhase.UP && lastDefinitive == RepPhase.UP -> "LOWER YOURSELF"
        phase == RepPhase.DOWN                              -> "PUSH UP"
        else                                                -> "KEEP GOING"
    }

    private fun confidence(phase: RepPhase, deg: Float): Float = when (phase) {
        RepPhase.DOWN       -> ((DOWN_THRESHOLD - deg) / DOWN_THRESHOLD).coerceIn(0f, 1f)
        RepPhase.UP         -> ((deg - UP_THRESHOLD) / (180f - UP_THRESHOLD)).coerceIn(0f, 1f)
        RepPhase.TRANSITION -> {
            val mid = (DOWN_THRESHOLD + UP_THRESHOLD) / 2f
            (1f - abs(deg - mid) / (mid - DOWN_THRESHOLD)).coerceIn(0f, 1f)
        }
    }

    /**
     * Prefer whichever side has all three joints visible; average both sides when
     * both are detected.  This prevents a partially-occluded limb from corrupting
     * the reading.
     */
    private fun bestElbowAngle(frame: LandmarkFrame): Float? {
        val left  = calculateAngle(frame.leftShoulder,  frame.leftElbow,  frame.leftWrist)
        val right = calculateAngle(frame.rightShoulder, frame.rightElbow, frame.rightWrist)
        return when {
            left != null && right != null -> (left + right) / 2f
            left  != null                 -> left
            right != null                 -> right
            else                          -> null
        }
    }

    /**
     * Falls back from ankle to knee when the ankle is off-camera (common in
     * push-up setups).  Returns true (form OK) when hips aren't visible at all —
     * we can't penalise what we can't see.
     */
    private fun isBodyStraight(frame: LandmarkFrame): Boolean {
        val left  = calculateAngle(frame.leftShoulder,  frame.leftHip,  frame.leftAnkle)
                 ?: calculateAngle(frame.leftShoulder,  frame.leftHip,  frame.leftKnee)
        val right = calculateAngle(frame.rightShoulder, frame.rightHip, frame.rightAnkle)
                 ?: calculateAngle(frame.rightShoulder, frame.rightHip, frame.rightKnee)
        val samples = listOfNotNull(left, right)
        return samples.isEmpty() || samples.average() >= BODY_MIN_ANGLE
    }

    private fun calculateAngle(a: Joint?, b: Joint?, c: Joint?): Float? {
        if (a == null || b == null || c == null) return null
        val v1x = a.x - b.x;  val v1y = a.y - b.y
        val v2x = c.x - b.x;  val v2y = c.y - b.y
        val m1  = sqrt(v1x * v1x + v1y * v1y)
        val m2  = sqrt(v2x * v2x + v2y * v2y)
        if (m1 < 1e-6f || m2 < 1e-6f) return null
        val cos = ((v1x * v2x + v1y * v2y) / (m1 * m2)).toDouble().coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos)).toFloat()
    }
}
