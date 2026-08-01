package ca.hdclark.mtbsim.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(seed: Int = 0x4D5442) {
    private val random = Random(seed)
    private var mode = GameMode.READY
    private var speed = START_SPEED
    private var distance = 0f
    private var score = 0
    private var streak = 0
    private var elapsed = 0f
    private var featureId = 1L
    private var current: TrailFeature? = null
    private var nextFeatureGap = 42f
    private var feedback = "Tap anywhere to ride"
    private var feedbackAge = 0f
    private var runId = 0L
    private var lastRule: FeatureType? = null

    fun start() {
        mode = GameMode.PLAYING
        speed = START_SPEED
        distance = 0f
        score = 0
        streak = 0
        elapsed = 0f
        featureId = 1L
        current = null
        nextFeatureGap = 35f
        feedback = "Roll in easy — hints appear before every feature"
        feedbackAge = 0f
        runId++
        lastRule = null
    }

    fun pause() {
        if (mode == GameMode.PLAYING) mode = GameMode.PAUSED
    }

    fun resume() {
        if (mode == GameMode.PAUSED) mode = GameMode.PLAYING
    }

    fun update(deltaSeconds: Float) {
        if (mode != GameMode.PLAYING) return
        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        elapsed += dt
        feedbackAge += dt
        speed = min(MAX_SPEED, START_SPEED + elapsed * SPEED_GAIN_PER_SECOND)
        val travel = speed * dt
        distance += travel
        score += max(1, (travel * 2.5f).toInt())

        val feature = current
        if (feature == null) {
            nextFeatureGap -= travel
            if (nextFeatureGap <= 0f) spawnFeature()
            return
        }

        feature.distanceMeters -= travel
        if (!feature.activated && feature.distanceMeters <= feature.rule.interactionLeadMeters) {
            feature.activated = true
            feedback = feature.rule.steps.first().hint
            feedbackAge = 0f
        }

        if (!feature.completed && feature.distanceMeters < feature.rule.failBehindMeters) {
            mode = GameMode.GAME_OVER
            streak = 0
            feedback = "Missed ${feature.rule.type.displayName.lowercase()}"
            feedbackAge = 0f
        } else if (feature.completed && feature.distanceMeters < -11f) {
            current = null
            nextFeatureGap = random.nextInt(30, 46).toFloat()
        }
    }

    fun submitGesture(kind: GestureKind): Boolean {
        if (mode == GameMode.READY || mode == GameMode.GAME_OVER) {
            start()
            return true
        }
        if (mode != GameMode.PLAYING) return false

        val feature = current ?: return false
        if (!feature.activated || feature.completed) return false
        val expected = feature.rule.steps[feature.stepIndex]
        if (kind != expected.kind) {
            feedback = "Almost — ${expected.hint}"
            feedbackAge = 0f
            return false
        }

        feature.stepIndex++
        if (feature.stepIndex >= feature.rule.steps.size) {
            feature.completed = true
            streak++
            val bonus = 140 + streak * 25 + (speed * 8f).toInt()
            score += bonus
            feedback = "${feature.rule.type.cheer}  +$bonus"
            feedbackAge = 0f
        } else {
            feedback = feature.rule.steps[feature.stepIndex].hint
            feedbackAge = 0f
        }
        return true
    }

    fun snapshot(): GameSnapshot = GameSnapshot(
        mode = mode,
        speedMetersPerSecond = speed,
        distanceMeters = distance,
        score = score,
        streak = streak,
        currentFeature = current,
        feedback = feedback,
        feedbackAgeSeconds = feedbackAge,
        elapsedSeconds = elapsed,
        runId = runId,
    )

    private fun spawnFeature() {
        val candidates = FeatureRules.all.filter { it.type != lastRule }
        val rule = candidates[random.nextInt(candidates.size)]
        current = TrailFeature(
            id = featureId++,
            rule = rule,
            distanceMeters = random.nextInt(53, 66).toFloat(),
        )
        lastRule = rule.type
        nextFeatureGap = Float.POSITIVE_INFINITY
    }

    companion object {
        const val START_SPEED = 6.2f
        const val MAX_SPEED = 18.2f
        const val SPEED_GAIN_PER_SECOND = 0.075f
    }
}
