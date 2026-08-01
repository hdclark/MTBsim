package ca.hdclark.mtbsim.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GestureClassifierTest {
    private val width = 1000f
    private val height = 600f

    @Test
    fun `classifies broad directional swipes`() {
        assertEquals(GestureKind.SWIPE_LEFT, classify(700f, 300f, 480f, 310f))
        assertEquals(GestureKind.SWIPE_RIGHT, classify(300f, 300f, 540f, 290f))
        assertEquals(GestureKind.SWIPE_UP, classify(500f, 430f, 510f, 220f))
        assertEquals(GestureKind.SWIPE_DOWN, classify(500f, 180f, 490f, 390f))
    }

    @Test
    fun `classifies taps by wide screen zones`() {
        assertEquals(GestureKind.TAP_LEFT, classify(100f, 300f, 105f, 304f, 90))
        assertEquals(GestureKind.TAP_CENTER, classify(500f, 300f, 503f, 302f, 90))
        assertEquals(GestureKind.TAP_RIGHT, classify(900f, 300f, 899f, 303f, 90))
    }

    @Test
    fun `classifies forgiving hold and two finger chute gesture`() {
        assertEquals(GestureKind.HOLD_CENTER, classify(500f, 300f, 510f, 305f, 420))
        assertEquals(GestureKind.TWO_FINGER_SWIPE_DOWN, classify(500f, 150f, 520f, 400f, 250, 2))
    }

    private fun classify(
        sx: Float,
        sy: Float,
        ex: Float,
        ey: Float,
        duration: Long = 180,
        pointers: Int = 1,
    ): GestureKind? = GestureClassifier.classify(
        TouchSample(sx, sy, ex, ey, duration, pointers, width, height),
    )
}
