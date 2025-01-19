package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object LuckPermsSpec : ConfigSpec() {
    val roles by required<Map<String, ULong>>()

    val startSpacer by required<String>()
    val middleSpacer by required<String>()
    val endSpacer by required<String>()
}
