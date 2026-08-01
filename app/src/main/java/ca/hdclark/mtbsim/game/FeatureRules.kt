package ca.hdclark.mtbsim.game

object FeatureRules {
    val all: List<FeatureRule> = listOf(
        FeatureRule(
            FeatureType.WOODEN_SKINNY,
            listOf(
                GestureStep(GestureKind.TAP_LEFT, "Tap LEFT grip"),
                GestureStep(GestureKind.TAP_RIGHT, "Tap RIGHT grip"),
                GestureStep(GestureKind.TAP_LEFT, "Tap LEFT grip"),
            ),
            interactionLeadMeters = 29f,
        ),
        FeatureRule(
            FeatureType.DROP,
            listOf(
                GestureStep(GestureKind.SWIPE_DOWN, "Swipe DOWN to lean back"),
                GestureStep(GestureKind.SWIPE_UP, "Swipe UP to return to centre"),
            ),
        ),
        FeatureRule(
            FeatureType.JUMP,
            listOf(
                GestureStep(GestureKind.TAP_CENTER, "Tap the bike to preload"),
                GestureStep(GestureKind.SWIPE_DOWN, "Swipe DOWN to float the landing"),
            ),
        ),
        FeatureRule(
            FeatureType.TIGHT_LEFT,
            listOf(
                GestureStep(GestureKind.SWIPE_LEFT, "Drag the bars LEFT"),
                GestureStep(GestureKind.SWIPE_LEFT, "Hold the LEFT line"),
            ),
            interactionLeadMeters = 30f,
        ),
        FeatureRule(
            FeatureType.TIGHT_RIGHT,
            listOf(
                GestureStep(GestureKind.SWIPE_RIGHT, "Drag the bars RIGHT"),
                GestureStep(GestureKind.SWIPE_RIGHT, "Hold the RIGHT line"),
            ),
            interactionLeadMeters = 30f,
        ),
        FeatureRule(
            FeatureType.ROCK_ROLL,
            listOf(GestureStep(GestureKind.HOLD_CENTER, "Press and HOLD for gentle braking")),
            interactionLeadMeters = 28f,
        ),
        FeatureRule(
            FeatureType.TEETER_TOTTER,
            listOf(
                GestureStep(GestureKind.HOLD_CENTER, "Press and HOLD to stay centred"),
                GestureStep(GestureKind.RELEASE_CENTER, "RELEASE as the plank tips"),
            ),
            interactionLeadMeters = 30f,
        ),
        FeatureRule(
            FeatureType.CANOE_CHUTE,
            listOf(GestureStep(GestureKind.TWO_FINGER_SWIPE_DOWN, "Two fingers DOWN through the chute")),
            interactionLeadMeters = 31f,
        ),
        FeatureRule(
            FeatureType.A_FRAME,
            listOf(
                GestureStep(GestureKind.SWIPE_DOWN, "Swipe DOWN: shift back for the climb"),
                GestureStep(GestureKind.SWIPE_UP, "Swipe UP: move forward over the crest"),
            ),
            interactionLeadMeters = 31f,
        ),
    )
}
