package ca.hdclark.mtbsim.game

import kotlin.math.abs
import kotlin.math.hypot

data class TouchSample(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long,
    val pointerCount: Int,
    val viewWidth: Float,
    val viewHeight: Float,
)

object GestureClassifier {
    fun classify(sample: TouchSample): GestureKind? {
        val dx = sample.endX - sample.startX
        val dy = sample.endY - sample.startY
        val distance = hypot(dx, dy)
        val minSwipe = sample.viewWidth.coerceAtMost(sample.viewHeight) * 0.07f

        if (sample.pointerCount >= 2 && dy > minSwipe && abs(dy) > abs(dx) * 0.75f) {
            return GestureKind.TWO_FINGER_SWIPE_DOWN
        }
        if (sample.durationMs >= 340L && distance < minSwipe * 0.8f && isCenter(sample.startX, sample.viewWidth)) {
            return GestureKind.HOLD_CENTER
        }
        if (distance < minSwipe) {
            return when {
                sample.startX < sample.viewWidth * 0.34f -> GestureKind.TAP_LEFT
                sample.startX > sample.viewWidth * 0.66f -> GestureKind.TAP_RIGHT
                else -> GestureKind.TAP_CENTER
            }
        }
        return if (abs(dx) > abs(dy)) {
            if (dx < 0f) GestureKind.SWIPE_LEFT else GestureKind.SWIPE_RIGHT
        } else {
            if (dy < 0f) GestureKind.SWIPE_UP else GestureKind.SWIPE_DOWN
        }
    }

    fun isCenter(x: Float, width: Float): Boolean = x in width * 0.28f..width * 0.72f
}
