package ua.mei.minekord.config

import com.uchuhimo.konf.ConfigSpec

object PresenceSpec : ConfigSpec() {
    val activityType by required<ActivityType>()
    val activityText by required<String>()
    val updateTicks by required<Int>()
}

enum class ActivityType {
    NONE,
    PLAYING,
    LISTENING,
    WATCHING,
    COMPETING,
}
