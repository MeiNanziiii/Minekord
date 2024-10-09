package ua.mei.minekord.config

import com.uchuhimo.konf.ConfigSpec

object ExperimentalSpec : ConfigSpec() {
    object DiscordSpec : ConfigSpec() {
        val enabled by required<Boolean>()
        val allowOfflinePlayers by required<Boolean>()
        val requiredRoles by required<List<ULong>>()
        val loginByIp by required<Boolean>()
    }
}
