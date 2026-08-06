package ua.bonfiremc.minekord.config

import com.uchuhimo.konf.ConfigSpec

object BotSpec : ConfigSpec("") {
    val token by required<String>()
}