package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.rest.Image
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.ensureWebhook
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.firstOrNull
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.ChatSpec
import ua.mei.minekord.config.config

class SetupExtension : Extension() {
    override val name: String = "Startup"

    override suspend fun setup() {
        MinekordBot.guild = kord.getGuild(Snowflake(config[BotSpec.guild]))
        MinekordBot.chat = MinekordBot.guild.getChannel(Snowflake(config[BotSpec.chat])) as TextChannel
        MinekordBot.webhook = MinekordBot.chat.webhooks.firstOrNull { it.name == config[ChatSpec.WebhookSpec.webhookName] }.let {
            if (it == null) {
                MinekordBot.chat.createWebhook(config[ChatSpec.WebhookSpec.webhookName]) {
                    avatar = Image.fromUrl(HttpClient(), config[ChatSpec.WebhookSpec.avatarUrl])
                }
            } else {
                it
            }
        }
    }
}