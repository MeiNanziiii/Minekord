package ua.bonfiremc.minekord.config

import com.uchuhimo.konf.ConfigSpec

object MinecraftSpec : ConfigSpec() {
    val messageFormat by required<String>()
    val replyFormat by required<String>()
    val summaryMaxLength by required<Int>()

    val coloredRoles by required<Boolean>()
}