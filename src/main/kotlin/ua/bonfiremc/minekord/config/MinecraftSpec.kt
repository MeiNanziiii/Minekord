package ua.bonfiremc.minekord.config

import com.uchuhimo.konf.ConfigSpec

object MinecraftSpec : ConfigSpec() {
    val messageFormat by required<String>()
    val replyFormat by required<String>()
    val summaryMaxLength by required<Int>()

    val appendImages by required<Boolean>()
    val imageMaxWidth by required<Int>()
    val imageMaxHeight by required<Int>()
}