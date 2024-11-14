package ua.mei.minekord.utils

import dev.kord.common.Color
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.ColorsSpec

object MinekordColor {
    val RED: Color = config[ColorsSpec.red].toColor()
    val ORANGE: Color = config[ColorsSpec.orange].toColor()
    val GREEN: Color = config[ColorsSpec.green].toColor()
    val BLUE: Color = config[ColorsSpec.blue].toColor()
    val PURPLE: Color = config[ColorsSpec.purple].toColor()
}
