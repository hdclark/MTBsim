package ca.hdclark.mtbsim.game

enum class GameMode { READY, PLAYING, GAME_OVER, PAUSED }

enum class GestureKind {
    TAP_LEFT,
    TAP_RIGHT,
    TAP_CENTER,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    HOLD_CENTER,
    RELEASE_CENTER,
    TWO_FINGER_SWIPE_DOWN,
}

enum class FeatureType(
    val displayName: String,
    val cheer: String,
) {
    WOODEN_SKINNY("Wooden skinny", "Laser focus!"),
    DROP("Trail drop", "Send it!"),
    JUMP("Tabletop jump", "Air time!"),
    TIGHT_LEFT("Tight left berm", "Rail that turn!"),
    TIGHT_RIGHT("Tight right berm", "Rail that turn!"),
    ROCK_ROLL("Rock roll", "Smooth operator!"),
    TEETER_TOTTER("Teeter-totter", "Perfect pivot!"),
    CANOE_CHUTE("Canoe chute", "Threaded it!"),
    A_FRAME("A-frame", "Up and over!"),
}

data class GestureStep(
    val kind: GestureKind,
    val hint: String,
)

data class FeatureRule(
    val type: FeatureType,
    val steps: List<GestureStep>,
    val interactionLeadMeters: Float = 27f,
    val failBehindMeters: Float = -7f,
)

data class TrailFeature(
    val id: Long,
    val rule: FeatureRule,
    var distanceMeters: Float,
    var stepIndex: Int = 0,
    var activated: Boolean = false,
    var completed: Boolean = false,
)

data class GameSnapshot(
    val mode: GameMode,
    val speedMetersPerSecond: Float,
    val distanceMeters: Float,
    val score: Int,
    val streak: Int,
    val currentFeature: TrailFeature?,
    val feedback: String,
    val feedbackAgeSeconds: Float,
    val elapsedSeconds: Float,
    val runId: Long,
)
