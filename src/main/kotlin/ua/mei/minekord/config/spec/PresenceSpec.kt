package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec
import ua.mei.minekord.utils.MinekordActivityType

object PresenceSpec : ConfigSpec() {
    val activityType by required<MinekordActivityType>()
    val activityText by required<String>()
    val updateTicks by required<Int>()
}
