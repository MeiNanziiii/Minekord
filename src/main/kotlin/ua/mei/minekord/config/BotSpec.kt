package ua.mei.minekord.config

import com.uchuhimo.konf.ConfigSpec

object BotSpec : ConfigSpec() {
    val token by required<String>()
    val guild by required<ULong>()
    val chat by required<ULong>()
}
