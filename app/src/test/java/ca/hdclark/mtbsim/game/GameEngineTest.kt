package ca.hdclark.mtbsim.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameEngineTest {
    @Test
    fun `speed scales gradually and remains capped`() {
        val engine = GameEngine(123)
        engine.start()
        repeat(4000) { engine.update(0.05f) }
        val speed = engine.snapshot().speedMetersPerSecond
        assertTrue(speed > GameEngine.START_SPEED)
        assertTrue(speed <= GameEngine.MAX_SPEED)
    }

    @Test
    fun `tap starts or restarts a run`() {
        val engine = GameEngine(123)
        assertEquals(GameMode.READY, engine.snapshot().mode)
        assertTrue(engine.submitGesture(GestureKind.TAP_CENTER))
        assertEquals(GameMode.PLAYING, engine.snapshot().mode)
    }
}
