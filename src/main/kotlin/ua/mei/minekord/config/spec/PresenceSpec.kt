package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object PresenceSpec : ConfigSpec() {
    val activityType by required<MinekordActivityType>()
    val activityText by required<String>()
    val updateTicks by required<Int>()

    enum class MinekordActivityType {
        NONE,
        PLAYING,
        LISTENING,
        WATCHING,
        COMPETING,
    }
}
