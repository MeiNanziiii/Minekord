package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.execute
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kordex.core.checks.inChannel
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions
import eu.pb4.placeholders.api.PlaceholderContext
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.kyori.adventure.text.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import org.koin.core.component.inject
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.ActivityType
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.ChatSpec
import ua.mei.minekord.config.PresenceSpec
import ua.mei.minekord.config.config
import ua.mei.minekord.utils.MinekordMinecraftRenderer
import ua.mei.minekord.utils.SerializerUtils
import ua.mei.minekord.utils.literal
import ua.mei.minekord.utils.parse
import ua.mei.minekord.utils.summary
import ua.mei.minekord.utils.toAdventure
import ua.mei.minekord.utils.toNative

class MessageExtension : Extension() {
    override val name: String = "Message Extension"

    private val mentions: AllowedMentionsBuilder = AllowedMentionsBuilder()
    private val discordOptions: DiscordSerializerOptions = DiscordSerializerOptions.defaults()
        .withEmbedLinks(false)
        .withEscapeMarkdown(config[ChatSpec.convertMarkdown])
        .withKeybindProvider(SerializerUtils::translatableToString)
        .withTranslationProvider(SerializerUtils::translatableToString)

    private val minecraftOptions: MinecraftSerializerOptions<Component> = MinecraftSerializerOptions.defaults()
        .addRenderer(MinekordMinecraftRenderer())

    private val server: MinecraftServer by inject()

    override suspend fun setup() {
        mentions.add(AllowedMentionType.UserMentions)
        mentions.roles.addAll(MinekordBot.guild.roles.filter { it.mentionable }.map { it.id }.toList())

        event<MessageCreateEvent> {
            check { isNotBot() }
            check { inChannel(Snowflake(config[BotSpec.channel])) }

            action {
                val message: Message = event.message
                val sender: Member = message.getAuthorAsMemberOrNull() ?: return@action

                server.submit {

                    var content: Text = if (config[ChatSpec.convertMarkdown])
                        MinecraftSerializer.INSTANCE.serialize(message.content, minecraftOptions).toNative(server.registryManager)
                    else
                        message.content.literal()

                    if (message.referencedMessage != null) {
                        val replyText: Text = MinecraftSerializer.INSTANCE.serialize(message.referencedMessage!!.content, minecraftOptions).toNative(server.registryManager)

                        val reply = parse(config[ChatSpec.MinecraftSpec.replyFormat], PlaceholderContext.of(server)) {
                            "sender" to sender.effectiveName.literal()
                            "message" to replyText
                            "summary" to replyText.string.summary().literal()
                        }

                        content = Text.empty()
                            .append(reply)
                            .append("\n")
                            .append(content)
                    }

                    content = parse(config[ChatSpec.MinecraftSpec.messageFormat], PlaceholderContext.of(server)) {
                        "sender" to sender.effectiveName.literal()
                        "message" to content
                    }

                    server.playerManager.broadcast(content, false)
                }
            }
        }

        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    allowedMentions = mentions
                    username = sender.gameProfile.name

                    content = DiscordSerializer.INSTANCE.serialize(
                        message.content.toAdventure(server.registryManager), discordOptions
                    ).let { if (config[ChatSpec.convertMentions]) SerializerUtils.convertMentions(it) else it }.takeIf { it.isNotBlank() } ?: return@launch

                    avatarUrl = parse(config[ChatSpec.WebhookSpec.playerAvatar], PlaceholderContext.of(sender)) {
                        "nickname" to sender.gameProfile.name.literal()
                        "texture" to (sender.gameProfile.properties?.get("textures")?.firstOrNull()?.value ?: "").literal()
                    }.string
                }
            }
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (config[PresenceSpec.activityType] != ActivityType.NONE) {
                if (server.ticks % config[PresenceSpec.updateTicks] == 0) {
                    MinekordBot.launch {
                        kord.editPresence {
                            val text: String = parse(config[PresenceSpec.activityText], server).string

                            when (config[PresenceSpec.activityType]) {
                                ActivityType.NONE -> Unit
                                ActivityType.PLAYING -> playing(text)
                                ActivityType.LISTENING -> listening(text)
                                ActivityType.WATCHING -> watching(text)
                                ActivityType.COMPETING -> competing(text)
                            }
                        }
                    }
                }
            }
        }
    }
}