package ua.mei.minekord.utils

import dev.kord.common.Color
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.ColorsSpec

object MinekordColor {
    val RED: Color = colorFromString(config[ColorsSpec.red])
    val ORANGE: Color = colorFromString(config[ColorsSpec.orange])
    val GREEN: Color = colorFromString(config[ColorsSpec.green])
    val BLUE: Color = colorFromString(config[ColorsSpec.blue])
    val PURPLE: Color = colorFromString(config[ColorsSpec.purple])
}
