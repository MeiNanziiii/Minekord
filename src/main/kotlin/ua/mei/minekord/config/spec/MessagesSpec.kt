package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object MessagesSpec : ConfigSpec() {
    val ipKickMessage by required<String>()
    val embedTitle by required<String>()
    val timeLabel by required<String>()
    val yesButton by required<String>()
    val noButton by required<String>()
    val unblockButton by required<String>()
    val ipBlockedTitle by required<String>()
    val ipUnblockedTitle by required<String>()
}
