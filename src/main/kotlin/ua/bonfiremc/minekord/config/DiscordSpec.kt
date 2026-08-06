package ua.bonfiremc.minekord.config

import com.uchuhimo.konf.ConfigSpec

object DiscordSpec : ConfigSpec() {
    val playerAvatarUrl by required<String>()

    val joinMessages by required<List<String>>()
    val leaveMessages by required<List<String>>()
    val deathMessages by required<List<String>>()

    val startMessages by required<List<String>>()
    val stopMessages by required<List<String>>()

    val advancementMessages by required<List<String>>()
    val goalMessages by required<List<String>>()
    val challengeMessages by required<List<String>>()
}