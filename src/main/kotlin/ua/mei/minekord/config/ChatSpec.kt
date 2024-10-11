package ua.mei.minekord.config

import com.uchuhimo.konf.ConfigSpec

object ChatSpec : ConfigSpec() {
    val allowMentions by required<Boolean>()
    val convertMarkdown by required<Boolean>()

    object WebhookSpec : ConfigSpec() {
        val webhookName by required<String>()
        val avatarUrl by required<String>()
    }
}