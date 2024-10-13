package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object ChatSpec : ConfigSpec() {
    val convertMentions by required<Boolean>()
    val convertMarkdown by required<Boolean>()

    object MinecraftSpec : ConfigSpec() {
        val messageFormat by required<String>()
        val replyFormat by required<String>()
        val summaryMaxLength by required<Int>()
        val mentionColor by required<String>()
        val linkColor by required<String>()
    }

    object DiscordSpec : ConfigSpec() {
        val joinMessage by required<String>()
        val leaveMessage by required<String>()
        val deathMessage by required<String>()
        val advancementMessage by required<String>()
        val goalMessage by required<String>()
        val challengeMessage by required<String>()
        val startMessage by required<String>()
        val stopMessage by required<String>()
    }

    object WebhookSpec : ConfigSpec() {
        val webhookName by required<String>()
        val webhookAvatar by required<String>()
        val playerAvatar by required<String>()
    }
}
