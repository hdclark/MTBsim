package ca.hdclark.mtbsim.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureRulesTest {
    @Test
    fun `every requested trail feature has a playable rule`() {
        assertEquals(FeatureType.entries.toSet(), FeatureRules.all.map { it.type }.toSet())
        assertTrue(FeatureRules.all.all { it.steps.isNotEmpty() })
        assertTrue(FeatureRules.all.all { it.interactionLeadMeters >= 27f })
        assertTrue(FeatureRules.all.all { it.failBehindMeters <= -7f })
    }

    @Test
    fun `a frame and berms use directional rider inputs`() {
        val aFrame = FeatureRules.all.single { it.type == FeatureType.A_FRAME }
        assertEquals(listOf(GestureKind.SWIPE_DOWN, GestureKind.SWIPE_UP), aFrame.steps.map { it.kind })

        val left = FeatureRules.all.single { it.type == FeatureType.TIGHT_LEFT }
        val right = FeatureRules.all.single { it.type == FeatureType.TIGHT_RIGHT }
        assertTrue(left.steps.all { it.kind == GestureKind.SWIPE_LEFT })
        assertTrue(right.steps.all { it.kind == GestureKind.SWIPE_RIGHT })
    }
}
