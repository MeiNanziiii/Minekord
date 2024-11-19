package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object LuckPermsSpec : ConfigSpec() {
    val roles by required<Map<String, ULong>>()
}
