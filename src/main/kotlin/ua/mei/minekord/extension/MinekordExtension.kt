package ua.mei.minekord.extension

import dev.kord.common.entity.AllowedMentionType
import dev.kord.core.behavior.execute
import dev.kord.core.entity.Webhook
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.WebhookMessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.extensions.Extension
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.minecraft.server.MinecraftServer
import org.koin.core.component.inject
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.utils.MinekordMinecraftRenderer
import ua.mei.minekord.utils.SerializerUtils
import kotlin.getValue

abstract class MinekordExtension : Extension() {
    open val mentions: AllowedMentionsBuilder = AllowedMentionsBuilder().apply {
        MinekordBot.launch {
            add(AllowedMentionType.UserMentions)
            roles.addAll(MinekordBot.guild.roles.filter { it.mentionable }.map { it.id }.toList())
        }
    }

    open val discordOptions: DiscordSerializerOptions = DiscordSerializerOptions.defaults()
        .withEmbedLinks(false)
        .withEscapeMarkdown(config[ChatSpec.convertMarkdown])
        .withKeybindProvider(SerializerUtils::translatableToString)
        .withTranslationProvider(SerializerUtils::translatableToString)

    open val minecraftOptions: MinecraftSerializerOptions<Component> = MinecraftSerializerOptions.defaults()
        .addRenderer(MinekordMinecraftRenderer)

    open val server: MinecraftServer by inject()

    open val webhook: Webhook = MinekordBot.webhook

    open suspend fun createWebhookMessageSync(builder: suspend WebhookMessageCreateBuilder.() -> Unit) {
        webhook.execute(webhook.token!!, null) {
            allowedMentions = mentions
            builder()
        }
    }

    open fun createWebhookMessage(builder: suspend WebhookMessageCreateBuilder.() -> Unit) {
        launch {
            createWebhookMessageSync(builder)
        }
    }

    open suspend fun createWebhookEmbedSync(builder: EmbedBuilder.() -> Unit) {
        createWebhookMessageSync {
            embed(builder)
        }
    }

    open fun createWebhookEmbed(builder: EmbedBuilder.() -> Unit) {
        launch {
            createWebhookMessageSync {
                embed(builder)
            }
        }
    }

    fun launch(block: suspend CoroutineScope.() -> Unit) {
        MinekordBot.launch { block() }
    }
}
