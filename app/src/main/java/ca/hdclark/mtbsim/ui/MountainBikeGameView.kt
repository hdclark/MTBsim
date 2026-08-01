package ca.hdclark.mtbsim.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import ca.hdclark.mtbsim.game.FeatureType
import ca.hdclark.mtbsim.game.GameEngine
import ca.hdclark.mtbsim.game.GameMode
import ca.hdclark.mtbsim.game.GameSnapshot
import ca.hdclark.mtbsim.game.GestureClassifier
import ca.hdclark.mtbsim.game.GestureKind
import ca.hdclark.mtbsim.game.TouchSample
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class MountainBikeGameView(context: Context) : View(context) {
    private val engine = GameEngine()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private var running = true
    private var lastFrameNanos = 0L
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downTime = 0L
    private var maxPointerCount = 1
    private var holdSent = false
    private var steeringVisual = 0f
    private var leanVisual = 0f
    private var touchGlow = PointF(-1000f, -1000f)
    private var touchGlowAge = 10f
    private var snapshot = engine.snapshot()

    init {
        isFocusable = true
        keepScreenOn = true
        paint.typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
    }

    fun resume() {
        running = true
        engine.resume()
        lastFrameNanos = 0L
        postInvalidateOnAnimation()
    }

    fun pause() {
        running = false
        engine.pause()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrameNanos == 0L) 0f else ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrameNanos = now
        if (running) {
            engine.update(dt)
            snapshot = engine.snapshot()
            updateTouchHold()
            steeringVisual *= (1f - dt * 3.4f).coerceAtLeast(0f)
            leanVisual *= (1f - dt * 3.0f).coerceAtLeast(0f)
            touchGlowAge += dt
        }

        drawWorld(canvas, snapshot)
        drawHud(canvas, snapshot)
        drawBike(canvas, snapshot)
        drawTouchGlow(canvas)
        drawOverlay(canvas, snapshot)

        if (running) postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
                downTime = SystemClock.uptimeMillis()
                maxPointerCount = 1
                holdSent = false
                touchGlow = PointF(event.x, event.y)
                touchGlowAge = 0f
                if (snapshot.mode == GameMode.READY || snapshot.mode == GameMode.GAME_OVER) {
                    engine.start()
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                maxPointerCount = max(maxPointerCount, event.pointerCount)
            }
            MotionEvent.ACTION_MOVE -> {
                lastX = event.getX(0)
                lastY = event.getY(0)
                maxPointerCount = max(maxPointerCount, event.pointerCount)
                steeringVisual = ((lastX - downX) / (width * 0.22f)).coerceIn(-1f, 1f)
                leanVisual = ((lastY - downY) / (height * 0.18f)).coerceIn(-1f, 1f)
                touchGlow = PointF(lastX, lastY)
                touchGlowAge = 0f
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastX = event.x
                lastY = event.y
                val duration = SystemClock.uptimeMillis() - downTime
                if (holdSent && GestureClassifier.isCenter(downX, width.toFloat())) {
                    submit(GestureKind.RELEASE_CENTER)
                } else {
                    GestureClassifier.classify(
                        TouchSample(
                            startX = downX,
                            startY = downY,
                            endX = lastX,
                            endY = lastY,
                            durationMs = duration,
                            pointerCount = maxPointerCount,
                            viewWidth = width.toFloat(),
                            viewHeight = height.toFloat(),
                        ),
                    )?.let(::submit)
                }
                holdSent = false
                downTime = 0L
            }
        }
        return true
    }

    private fun updateTouchHold() {
        if (holdSent || downTime == 0L) return
        val age = SystemClock.uptimeMillis() - downTime
        val moved = kotlin.math.hypot(lastX - downX, lastY - downY)
        if (age >= 360L && moved < min(width, height) * 0.055f && GestureClassifier.isCenter(downX, width.toFloat())) {
            holdSent = true
            submit(GestureKind.HOLD_CENTER)
        }
    }

    private fun submit(kind: GestureKind) {
        val success = engine.submitGesture(kind)
        performHapticFeedback(if (success) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.CLOCK_TICK)
        when (kind) {
            GestureKind.SWIPE_LEFT -> steeringVisual = -1f
            GestureKind.SWIPE_RIGHT -> steeringVisual = 1f
            GestureKind.SWIPE_UP -> leanVisual = -1f
            GestureKind.SWIPE_DOWN -> leanVisual = 1f
            else -> Unit
        }
        snapshot = engine.snapshot()
    }

    private fun drawWorld(canvas: Canvas, state: GameSnapshot) {
        val w = width.toFloat()
        val h = height.toFloat()
        val horizon = h * 0.34f
        paint.shader = LinearGradient(0f, 0f, 0f, horizon, Color.rgb(113, 194, 235), Color.rgb(236, 247, 220), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, horizon, paint)
        paint.shader = null

        drawSun(canvas, w * 0.78f, h * 0.12f, min(w, h) * 0.055f)
        drawMountains(canvas, horizon, state.elapsedSeconds)
        drawForest(canvas, horizon, state)
        drawTrail(canvas, horizon, state)
        drawFeature(canvas, horizon, state)
        drawBirds(canvas, state)
        drawBranches(canvas, state)
    }

    private fun drawSun(canvas: Canvas, x: Float, y: Float, r: Float) {
        paint.color = Color.argb(80, 255, 238, 145)
        canvas.drawCircle(x, y, r * 1.8f, paint)
        paint.color = Color.rgb(255, 233, 142)
        canvas.drawCircle(x, y, r, paint)
    }

    private fun drawMountains(canvas: Canvas, horizon: Float, t: Float) {
        val w = width.toFloat()
        val layers = listOf(
            Pair(Color.rgb(116, 154, 149), 0.10f),
            Pair(Color.rgb(75, 121, 112), 0.18f),
            Pair(Color.rgb(46, 91, 73), 0.27f),
        )
        for ((index, layer) in layers.withIndex()) {
            val (color, amp) = layer
            path.reset()
            path.moveTo(0f, horizon)
            val segments = 9
            for (i in 0..segments) {
                val x = w * i / segments
                val wave = sin(i * 1.7f + index * 0.8f + t * 0.015f) * height * amp * 0.18f
                val peak = horizon - height * amp - wave - (i % 2) * height * amp * 0.2f
                path.lineTo(x, peak)
            }
            path.lineTo(w, horizon)
            path.close()
            paint.color = color
            canvas.drawPath(path, paint)
        }
    }

    private fun drawForest(canvas: Canvas, horizon: Float, state: GameSnapshot) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = Color.rgb(34, 76, 48)
        canvas.drawRect(0f, horizon, w, h, paint)
        val scroll = state.distanceMeters * 0.65f
        for (side in listOf(-1, 1)) {
            for (i in 0 until 14) {
                val phase = ((i * 73f + scroll * (1f + i * 0.03f)) % 930f) / 930f
                val depth = phase.coerceAtLeast(0.04f)
                val xBase = if (side < 0) w * (0.42f - depth * 0.55f) else w * (0.58f + depth * 0.55f)
                val yBase = horizon + depth * (h - horizon)
                val treeH = h * (0.08f + depth * 0.42f)
                val trunkW = max(2f, w * 0.004f * depth)
                paint.color = Color.rgb(74, 63, 43)
                canvas.drawRoundRect(xBase - trunkW, yBase - treeH * 0.55f, xBase + trunkW, yBase, trunkW, trunkW, paint)
                paint.color = if (i % 2 == 0) Color.rgb(24, 92, 55) else Color.rgb(42, 115, 66)
                drawPine(canvas, xBase, yBase - treeH * 0.42f, treeH * 0.68f, treeH * 0.25f)
            }
        }
    }

    private fun drawPine(canvas: Canvas, x: Float, y: Float, h: Float, halfW: Float) {
        path.reset()
        path.moveTo(x, y - h)
        path.lineTo(x - halfW * 0.72f, y - h * 0.45f)
        path.lineTo(x - halfW * 0.38f, y - h * 0.47f)
        path.lineTo(x - halfW, y)
        path.lineTo(x + halfW, y)
        path.lineTo(x + halfW * 0.38f, y - h * 0.47f)
        path.lineTo(x + halfW * 0.72f, y - h * 0.45f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawTrail(canvas: Canvas, horizon: Float, state: GameSnapshot) {
        val w = width.toFloat()
        val h = height.toFloat()
        path.reset()
        path.moveTo(w * 0.46f, horizon)
        path.lineTo(w * 0.54f, horizon)
        path.lineTo(w * 0.88f, h)
        path.lineTo(w * 0.12f, h)
        path.close()
        paint.shader = LinearGradient(0f, horizon, 0f, h, Color.rgb(143, 115, 78), Color.rgb(94, 67, 44), Shader.TileMode.CLAMP)
        canvas.drawPath(path, paint)
        paint.shader = null

        val offset = state.distanceMeters % 7f
        for (i in 0 until 14) {
            val d = ((i * 7f + offset) % 98f) / 98f
            val perspective = d * d
            val y = horizon + perspective * (h - horizon)
            val half = w * (0.04f + perspective * 0.34f)
            stroke.color = Color.argb((40 + perspective * 80).toInt(), 60, 44, 31)
            stroke.strokeWidth = 1f + perspective * 5f
            canvas.drawLine(w / 2f - half, y, w / 2f + half, y + perspective * 8f, stroke)
        }

        // Roots and friendly trail texture.
        for (i in 0 until 11) {
            val p = ((i * 0.193f + state.distanceMeters * 0.009f) % 1f)
            val y = horizon + p * p * (h - horizon)
            val half = w * (0.05f + p * p * 0.28f)
            val x = w / 2f + sin(i * 2.3f) * half * 0.55f
            stroke.color = Color.argb((30 + p * 85).toInt(), 55, 39, 28)
            stroke.strokeWidth = 1f + p * 4f
            canvas.drawLine(x - 18f * p, y, x + 24f * p, y + 5f * p, stroke)
        }
    }

    private fun drawFeature(canvas: Canvas, horizon: Float, state: GameSnapshot) {
        val feature = state.currentFeature ?: return
        val d = feature.distanceMeters
        if (d > 72f || d < -15f) return
        val t = (1f - (d / 72f)).coerceIn(0f, 1.12f)
        val p = t * t
        val w = width.toFloat()
        val h = height.toFloat()
        val y = horizon + p * (h - horizon) * 0.84f
        val scale = 0.18f + p * 1.1f
        val centerX = w / 2f + when (feature.rule.type) {
            FeatureType.TIGHT_LEFT -> -w * p * 0.16f
            FeatureType.TIGHT_RIGHT -> w * p * 0.16f
            else -> 0f
        }
        paint.color = if (feature.completed) Color.rgb(102, 190, 111) else Color.rgb(174, 122, 67)
        stroke.strokeWidth = 3f * scale
        stroke.color = Color.rgb(67, 45, 31)

        when (feature.rule.type) {
            FeatureType.WOODEN_SKINNY -> {
                val fw = w * 0.10f * scale
                val fh = h * 0.20f * scale
                canvas.drawRoundRect(centerX - fw / 2f, y - fh, centerX + fw / 2f, y + fh * 0.15f, 8f, 8f, paint)
                repeat(6) { i ->
                    val yy = y - fh + i * fh / 5f
                    canvas.drawLine(centerX - fw / 2f, yy, centerX + fw / 2f, yy, stroke)
                }
            }
            FeatureType.DROP -> {
                val fw = w * 0.36f * scale
                val fh = h * 0.13f * scale
                canvas.drawRect(centerX - fw / 2f, y - fh, centerX + fw / 2f, y, paint)
                paint.color = Color.rgb(72, 54, 38)
                canvas.drawRect(centerX - fw / 2f, y, centerX + fw / 2f, y + fh * 0.45f, paint)
            }
            FeatureType.JUMP -> {
                val fw = w * 0.40f * scale
                val fh = h * 0.20f * scale
                path.reset()
                path.moveTo(centerX - fw / 2f, y)
                path.quadTo(centerX, y - fh * 1.25f, centerX + fw / 2f, y - fh * 0.75f)
                path.lineTo(centerX + fw / 2f, y)
                path.close()
                canvas.drawPath(path, paint)
            }
            FeatureType.TIGHT_LEFT, FeatureType.TIGHT_RIGHT -> {
                val dir = if (feature.rule.type == FeatureType.TIGHT_LEFT) -1f else 1f
                stroke.color = Color.rgb(203, 153, 86)
                stroke.strokeWidth = 13f * scale
                val turn = Path()
                turn.moveTo(centerX - dir * w * 0.18f * scale, y + h * 0.10f * scale)
                turn.quadTo(centerX, y - h * 0.12f * scale, centerX + dir * w * 0.25f * scale, y - h * 0.05f * scale)
                canvas.drawPath(turn, stroke)
            }
            FeatureType.ROCK_ROLL -> {
                repeat(5) { i ->
                    val rx = centerX + (i - 2) * w * 0.055f * scale
                    val ry = y - abs(i - 2) * h * 0.018f * scale
                    paint.color = if (feature.completed) Color.rgb(108, 169, 105) else Color.rgb(104 + i * 8, 105 + i * 6, 99)
                    canvas.drawOval(RectF(rx - 27f * scale, ry - 23f * scale, rx + 29f * scale, ry + 19f * scale), paint)
                }
            }
            FeatureType.TEETER_TOTTER -> {
                canvas.save()
                canvas.rotate(if (feature.stepIndex == 0) -7f else 9f, centerX, y)
                val fw = w * 0.42f * scale
                canvas.drawRoundRect(centerX - fw / 2f, y - 10f * scale, centerX + fw / 2f, y + 10f * scale, 5f, 5f, paint)
                canvas.restore()
                paint.color = Color.rgb(80, 64, 43)
                path.reset()
                path.moveTo(centerX, y)
                path.lineTo(centerX - 20f * scale, y + 38f * scale)
                path.lineTo(centerX + 20f * scale, y + 38f * scale)
                path.close()
                canvas.drawPath(path, paint)
            }
            FeatureType.CANOE_CHUTE -> {
                stroke.color = Color.rgb(84, 92, 83)
                stroke.strokeWidth = 14f * scale
                val fw = w * 0.18f * scale
                canvas.drawLine(centerX - fw, y - h * 0.13f * scale, centerX - fw * 0.45f, y + h * 0.10f * scale, stroke)
                canvas.drawLine(centerX + fw, y - h * 0.13f * scale, centerX + fw * 0.45f, y + h * 0.10f * scale, stroke)
                paint.color = Color.rgb(67, 116, 136)
                canvas.drawOval(RectF(centerX - fw * 0.38f, y - 10f * scale, centerX + fw * 0.38f, y + 18f * scale), paint)
            }
            FeatureType.A_FRAME -> {
                val fw = w * 0.43f * scale
                val fh = h * 0.24f * scale
                path.reset()
                path.moveTo(centerX - fw / 2f, y)
                path.lineTo(centerX, y - fh)
                path.lineTo(centerX + fw / 2f, y)
                path.lineTo(centerX + fw / 2f - 15f * scale, y)
                path.lineTo(centerX, y - fh + 16f * scale)
                path.lineTo(centerX - fw / 2f + 15f * scale, y)
                path.close()
                canvas.drawPath(path, paint)
                repeat(5) { i ->
                    val q = i / 5f
                    canvas.drawLine(centerX - fw / 2f * (1f - q), y - fh * q, centerX + fw / 2f * (1f - q), y - fh * q, stroke)
                }
            }
        }
    }

    private fun drawBirds(canvas: Canvas, state: GameSnapshot) {
        val cycle = state.elapsedSeconds % 19f
        if (cycle !in 2f..8f) return
        val p = (cycle - 2f) / 6f
        val x = width * (-0.08f + p * 1.16f)
        val y = height * (0.14f + sin(p * PI).toFloat() * 0.05f)
        stroke.color = Color.rgb(44, 54, 52)
        stroke.strokeWidth = max(2f, width * 0.0022f)
        val flap = sin(state.elapsedSeconds * 9f) * height * 0.012f
        path.reset()
        path.moveTo(x - width * 0.025f, y + flap.toFloat())
        path.quadTo(x - width * 0.008f, y - height * 0.018f, x, y)
        path.quadTo(x + width * 0.008f, y - height * 0.018f, x + width * 0.025f, y - flap.toFloat())
        canvas.drawPath(path, stroke)
    }

    private fun drawBranches(canvas: Canvas, state: GameSnapshot) {
        val cycle = state.elapsedSeconds % 23f
        if (cycle !in 13f..17f) return
        val p = (cycle - 13f) / 4f
        val slide = sin(p * PI).toFloat()
        stroke.color = Color.rgb(68, 53, 36)
        stroke.strokeWidth = width * 0.018f
        canvas.drawLine(width * 1.04f, height * 0.02f, width * (0.75f - slide * 0.12f), height * 0.27f, stroke)
        paint.color = Color.argb(220, 41, 104, 58)
        repeat(5) { i ->
            val q = i / 4f
            canvas.drawOval(
                RectF(
                    width * (0.80f + q * 0.05f - slide * 0.1f),
                    height * (0.06f + q * 0.035f),
                    width * (0.87f + q * 0.05f - slide * 0.1f),
                    height * (0.15f + q * 0.035f),
                ),
                paint,
            )
        }
    }

    private fun drawHud(canvas: Canvas, state: GameSnapshot) {
        val margin = width * 0.025f
        val top = height * 0.035f
        paint.color = Color.argb(175, 15, 38, 31)
        canvas.drawRoundRect(margin, top, width * 0.31f, height * 0.155f, 22f, 22f, paint)
        drawText(canvas, "${(state.speedMetersPerSecond * 3.6f).toInt()} km/h", margin * 1.55f, height * 0.087f, height * 0.045f, Color.WHITE, true)
        drawText(canvas, "${state.distanceMeters.toInt()} m  •  ${state.score} pts", margin * 1.55f, height * 0.133f, height * 0.027f, Color.rgb(224, 239, 221), false)

        if (state.streak > 1) {
            paint.color = Color.argb(205, 247, 179, 72)
            canvas.drawRoundRect(width * 0.79f, top, width - margin, height * 0.125f, 20f, 20f, paint)
            drawText(canvas, "FLOW ×${state.streak}", width * 0.895f, height * 0.087f, height * 0.036f, Color.rgb(47, 54, 35), true, centered = true)
        }

        val feature = state.currentFeature
        if (feature != null && feature.activated && !feature.completed && state.mode == GameMode.PLAYING) {
            val cardW = width * 0.48f
            val left = (width - cardW) / 2f
            paint.color = Color.argb(218, 255, 247, 218)
            canvas.drawRoundRect(left, height * 0.045f, left + cardW, height * 0.20f, 28f, 28f, paint)
            drawText(canvas, feature.rule.type.displayName.uppercase(), width / 2f, height * 0.095f, height * 0.031f, Color.rgb(66, 81, 59), true, centered = true)
            val hint = feature.rule.steps[feature.stepIndex.coerceAtMost(feature.rule.steps.lastIndex)].hint
            drawText(canvas, hint, width / 2f, height * 0.153f, height * 0.038f, Color.rgb(34, 53, 43), true, centered = true)
            drawProgressDots(canvas, feature.rule.steps.size, feature.stepIndex, width / 2f, height * 0.183f)
        } else if (state.feedbackAgeSeconds < 2.2f && state.mode == GameMode.PLAYING) {
            paint.color = Color.argb(185, 21, 51, 40)
            canvas.drawRoundRect(width * 0.31f, height * 0.055f, width * 0.69f, height * 0.135f, 20f, 20f, paint)
            drawText(canvas, state.feedback, width / 2f, height * 0.105f, height * 0.030f, Color.WHITE, true, centered = true)
        }
    }

    private fun drawProgressDots(canvas: Canvas, count: Int, completed: Int, cx: Float, y: Float) {
        val gap = height * 0.028f
        for (i in 0 until count) {
            paint.color = if (i < completed) Color.rgb(55, 143, 80) else Color.rgb(190, 183, 151)
            canvas.drawCircle(cx + (i - (count - 1) / 2f) * gap, y, height * 0.007f, paint)
        }
    }

    private fun drawBike(canvas: Canvas, state: GameSnapshot) {
        val w = width.toFloat()
        val h = height.toFloat()
        val bob = if (state.mode == GameMode.PLAYING) sin(state.distanceMeters * 0.58f) * h * 0.004f else 0f
        val cx = w / 2f + steeringVisual * w * 0.025f
        val barY = h * 0.79f + bob.toFloat() + leanVisual * h * 0.018f
        canvas.save()
        canvas.rotate(steeringVisual * 7f, cx, barY)

        stroke.color = Color.rgb(40, 45, 44)
        stroke.strokeWidth = h * 0.034f
        canvas.drawLine(cx - w * 0.18f, barY, cx + w * 0.18f, barY, stroke)
        stroke.color = Color.rgb(229, 162, 59)
        stroke.strokeWidth = h * 0.014f
        canvas.drawLine(cx - w * 0.16f, barY - h * 0.004f, cx + w * 0.16f, barY - h * 0.004f, stroke)

        // Stem and top tube.
        stroke.color = Color.rgb(48, 54, 52)
        stroke.strokeWidth = h * 0.025f
        canvas.drawLine(cx, barY, cx, h * 0.96f, stroke)
        stroke.color = Color.rgb(242, 178, 65)
        stroke.strokeWidth = h * 0.020f
        canvas.drawLine(cx, barY + h * 0.035f, cx, h * 0.96f, stroke)

        drawArm(canvas, cx - w * 0.17f, barY, -1f)
        drawArm(canvas, cx + w * 0.17f, barY, 1f)

        paint.color = Color.rgb(32, 37, 36)
        canvas.drawRoundRect(cx - w * 0.21f, barY - h * 0.025f, cx - w * 0.14f, barY + h * 0.025f, 18f, 18f, paint)
        canvas.drawRoundRect(cx + w * 0.14f, barY - h * 0.025f, cx + w * 0.21f, barY + h * 0.025f, 18f, 18f, paint)
        canvas.restore()
    }

    private fun drawArm(canvas: Canvas, handX: Float, handY: Float, side: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val elbowX = handX + side * w * 0.07f
        val elbowY = handY + h * 0.13f
        val shoulderX = handX + side * w * 0.14f
        val shoulderY = h * 1.03f
        stroke.color = Color.rgb(222, 167, 121)
        stroke.strokeWidth = h * 0.075f
        canvas.drawLine(shoulderX, shoulderY, elbowX, elbowY, stroke)
        canvas.drawLine(elbowX, elbowY, handX, handY, stroke)
        stroke.color = Color.rgb(61, 111, 118)
        stroke.strokeWidth = h * 0.095f
        canvas.drawLine(shoulderX, shoulderY, elbowX + side * w * 0.008f, elbowY + h * 0.025f, stroke)
        paint.color = Color.rgb(40, 47, 47)
        canvas.drawCircle(handX, handY, h * 0.037f, paint)
        paint.color = Color.argb(100, 255, 236, 202)
        canvas.drawCircle(handX - side * h * 0.01f, handY - h * 0.012f, h * 0.010f, paint)
    }

    private fun drawTouchGlow(canvas: Canvas) {
        if (touchGlowAge > 0.45f) return
        val alpha = ((1f - touchGlowAge / 0.45f) * 140).toInt().coerceIn(0, 140)
        paint.color = Color.argb(alpha, 255, 242, 170)
        val r = min(width, height) * (0.035f + touchGlowAge * 0.12f)
        canvas.drawCircle(touchGlow.x, touchGlow.y, r, paint)
    }

    private fun drawOverlay(canvas: Canvas, state: GameSnapshot) {
        if (state.mode == GameMode.PLAYING || state.mode == GameMode.PAUSED) return
        paint.color = Color.argb(178, 8, 29, 22)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        val title = if (state.mode == GameMode.READY) "MTBsim" else "Trail nap!"
        val subtitle = if (state.mode == GameMode.READY) {
            "Ride the rhythm of the mountain"
        } else {
            "${state.distanceMeters.toInt()} m  •  ${state.score} points"
        }
        drawText(canvas, title, width / 2f, height * 0.30f, height * 0.105f, Color.rgb(255, 238, 177), true, centered = true)
        drawText(canvas, subtitle, width / 2f, height * 0.40f, height * 0.042f, Color.WHITE, false, centered = true)

        paint.color = Color.argb(225, 255, 246, 210)
        canvas.drawRoundRect(width * 0.25f, height * 0.49f, width * 0.75f, height * 0.65f, 34f, 34f, paint)
        drawText(canvas, if (state.mode == GameMode.READY) "TAP TO DROP IN" else "TAP TO RIDE AGAIN", width / 2f, height * 0.585f, height * 0.045f, Color.rgb(38, 71, 54), true, centered = true)
        drawText(canvas, "Follow each generous gesture hint • speed rises gradually", width / 2f, height * 0.74f, height * 0.030f, Color.rgb(226, 238, 225), false, centered = true)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        centered: Boolean = false,
    ) {
        paint.shader = null
        paint.color = color
        paint.textSize = size
        paint.typeface = android.graphics.Typeface.create("sans", if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        paint.textAlign = if (centered) Paint.Align.CENTER else Paint.Align.LEFT
        canvas.drawText(text, x, y, paint)
    }
}
