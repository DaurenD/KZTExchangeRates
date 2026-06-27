package com.boxing.analysis.domain.model

enum class PunchType(val displayName: String, val labelIndex: Int) {
    JAB("Jab", 0),
    CROSS("Cross", 1),
    LEFT_HOOK("Left Hook", 2),
    RIGHT_HOOK("Right Hook", 3),
    LEFT_UPPERCUT("Left Uppercut", 4),
    RIGHT_UPPERCUT("Right Uppercut", 5);

    companion object {
        fun fromLabelIndex(index: Int): PunchType =
            entries.firstOrNull { it.labelIndex == index } ?: JAB
    }
}

enum class SessionType(val displayName: String) {
    SHADOWBOXING("Shadowboxing"),
    BAG_WORK("Bag Work"),
    PAD_WORK("Pad Work"),
    SPARRING("Sparring"),
    DRILL("Drill");
}

enum class Stance { ORTHODOX, SOUTHPAW }
