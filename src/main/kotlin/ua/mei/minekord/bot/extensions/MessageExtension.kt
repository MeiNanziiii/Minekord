package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.execute
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.embed
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
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.kyori.adventure.text.Component
import net.minecraft.advancement.AdvancementDisplay
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.PresenceSpec
import ua.mei.minekord.event.AdvancementGrantEvent
import ua.mei.minekord.utils.MinekordActivityType
import ua.mei.minekord.utils.MinekordColor
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
        .addRenderer(MinekordMinecraftRenderer)

    private lateinit var server: MinecraftServer

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

        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    embed {
                        author {
                            name = parse(config[ChatSpec.DiscordSpec.joinMessage], handler.player).string
                            icon = parse(config[ChatSpec.WebhookSpec.playerAvatar], PlaceholderContext.of(handler.player)) {
                                "nickname" to handler.player.gameProfile.name.literal()
                                "texture" to (handler.player.gameProfile.properties?.get("textures")?.firstOrNull()?.value ?: "").literal()
                            }.string
                        }
                        color = MinekordColor.GREEN
                    }
                    allowedMentions = mentions
                }
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    embed {
                        author {
                            name = parse(config[ChatSpec.DiscordSpec.leaveMessage], handler.player).string
                            icon = parse(config[ChatSpec.WebhookSpec.playerAvatar], PlaceholderContext.of(handler.player)) {
                                "nickname" to handler.player.gameProfile.name.literal()
                                "texture" to (handler.player.gameProfile.properties?.get("textures")?.firstOrNull()?.value ?: "").literal()
                            }.string
                        }
                        color = MinekordColor.RED
                    }
                    allowedMentions = mentions
                }
            }
        }

        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            if (entity is ServerPlayerEntity) {
                MinekordBot.launch {
                    MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                        embed {
                            author {
                                name = parse(config[ChatSpec.DiscordSpec.deathMessage], PlaceholderContext.of(entity)) {
                                    "message" to source.getDeathMessage(entity)
                                }.string
                                icon = parse(config[ChatSpec.WebhookSpec.playerAvatar], PlaceholderContext.of(entity)) {
                                    "nickname" to entity.gameProfile.name.literal()
                                    "texture" to (entity.gameProfile.properties?.get("textures")?.firstOrNull()?.value ?: "").literal()
                                }.string
                            }
                            color = MinekordColor.YELLOW
                        }
                        allowedMentions = mentions
                    }
                }
            }
            true
        }

        AdvancementGrantEvent.EVENT.register { player, advancement ->
            val display: AdvancementDisplay = advancement.comp_1913.get()

            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    embed {
                        author {
                            name = parse(
                                when (display.frame) {
                                    AdvancementFrame.CHALLENGE -> config[ChatSpec.DiscordSpec.challengeMessage]
                                    AdvancementFrame.GOAL -> config[ChatSpec.DiscordSpec.goalMessage]
                                    else -> config[ChatSpec.DiscordSpec.advancementMessage]
                                },
                                PlaceholderContext.of(player)
                            ) {
                                "advancement" to display.title
                            }.string
                            icon = parse(config[ChatSpec.WebhookSpec.playerAvatar], PlaceholderContext.of(player)) {
                                "nickname" to player.gameProfile.name.literal()
                                "texture" to (player.gameProfile.properties?.get("textures")?.firstOrNull()?.value ?: "").literal()
                            }.string
                        }
                        footer {
                            text = display.description.string
                        }
                        color = when (display.frame) {
                            AdvancementFrame.CHALLENGE -> MinekordColor.FUCHSIA
                            else -> MinekordColor.GREEN
                        }
                    }
                    allowedMentions = mentions
                }
            }
        }

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            this.server = server
            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    embed {
                        title = parse(config[ChatSpec.DiscordSpec.startMessage], server).string
                        color = MinekordColor.GREEN
                    }
                    allowedMentions = mentions
                }
            }
        }

        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    embed {
                        title = parse(config[ChatSpec.DiscordSpec.stopMessage], server).string
                        color = MinekordColor.RED
                    }
                    allowedMentions = mentions
                }
            }
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (config[PresenceSpec.activityType] != MinekordActivityType.NONE) {
                if (server.ticks % config[PresenceSpec.updateTicks] == 0) {
                    MinekordBot.launch {
                        kord.editPresence {
                            val text: String = parse(config[PresenceSpec.activityText], server).string

                            when (config[PresenceSpec.activityType]) {
                                MinekordActivityType.NONE -> Unit
                                MinekordActivityType.PLAYING -> playing(text)
                                MinekordActivityType.LISTENING -> listening(text)
                                MinekordActivityType.WATCHING -> watching(text)
                                MinekordActivityType.COMPETING -> competing(text)
                            }
                        }
                    }
                }
            }
        }
    }
}
