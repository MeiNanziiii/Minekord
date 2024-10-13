package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.inChannel
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.event
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer
import eu.pb4.placeholders.api.PlaceholderContext
import net.minecraft.advancement.AdvancementDisplay
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.text.Text
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.PresenceSpec
import ua.mei.minekord.event.minekord.MinekordAdvancementGrantEvent
import ua.mei.minekord.event.minekord.MinekordPlayerDeathEvent
import ua.mei.minekord.event.minekord.MinekordPlayerJoinEvent
import ua.mei.minekord.event.minekord.MinekordPlayerLeaveEvent
import ua.mei.minekord.event.minekord.MinekordPlayerMessageEvent
import ua.mei.minekord.event.minekord.MinekordServerEndTickEvent
import ua.mei.minekord.event.minekord.MinekordServerStartedEvent
import ua.mei.minekord.event.minekord.MinekordServerStoppedEvent
import ua.mei.minekord.extension.MinekordExtension
import ua.mei.minekord.utils.MinekordActivityType
import ua.mei.minekord.utils.MinekordColor
import ua.mei.minekord.utils.SerializerUtils
import ua.mei.minekord.utils.literal
import ua.mei.minekord.utils.parse
import ua.mei.minekord.utils.summary
import ua.mei.minekord.utils.toAdventure
import ua.mei.minekord.utils.toNative

class MessageExtension : MinekordExtension() {
    override val name: String = "Message Extension"

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { isNotBot() }
            check { inChannel(Snowflake(config[BotSpec.channel])) }

            action {
                val message: Message = event.message
                val sender: Member = event.member ?: return@action

                var content: Text = if (config[ChatSpec.convertMarkdown]) {
                    MinecraftSerializer.INSTANCE.serialize(message.content, minecraftOptions).toNative(server.registryManager)
                } else {
                    message.content.literal()
                }

                if (message.referencedMessage != null) {
                    val replyText: Text = MinecraftSerializer.INSTANCE.serialize(message.referencedMessage!!.content, minecraftOptions).toNative(server.registryManager)

                    val reply = parse(config[ChatSpec.MinecraftSpec.replyFormat], PlaceholderContext.of(server)) {
                        "sender" to sender.effectiveName.literal()
                        "message" to replyText
                        "summary" to replyText.string.summary().literal()
                    }

                    content = Text.empty().append(reply).append("\n").append(content)
                }

                content = parse(config[ChatSpec.MinecraftSpec.messageFormat], PlaceholderContext.of(server)) {
                    "sender" to sender.effectiveName.literal()
                    "message" to content
                }

                server.playerManager.broadcast(content, false)
            }
        }

        event<MinekordPlayerMessageEvent> {
            action {
                createWebhookMessage {
                    username = event.player.gameProfile.name
                    avatarUrl = event.playerAvatar

                    content = DiscordSerializer.INSTANCE.serialize(
                        event.message.toAdventure(server.registryManager), discordOptions
                    ).let { if (config[ChatSpec.convertMentions]) SerializerUtils.convertMentions(it) else it }.takeIf { it.isNotBlank() } ?: return@createWebhookMessage
                }
            }
        }

        event<MinekordPlayerJoinEvent> {
            action {
                createWebhookEmbed {
                    author {
                        name = parse(config[ChatSpec.DiscordSpec.joinMessage], event.player).string
                        icon = event.playerAvatar
                    }
                    color = MinekordColor.GREEN
                }
            }
        }

        event<MinekordPlayerLeaveEvent> {
            action {
                createWebhookEmbed {
                    author {
                        name = parse(config[ChatSpec.DiscordSpec.leaveMessage], event.player).string
                        icon = event.playerAvatar
                    }
                    color = MinekordColor.RED
                }
            }
        }

        event<MinekordPlayerDeathEvent> {
            action {
                createWebhookEmbed {
                    author {
                        icon = event.playerAvatar
                        name = parse(config[ChatSpec.DiscordSpec.deathMessage], PlaceholderContext.of(event.player)) {
                            "message" to event.source.getDeathMessage(event.player)
                        }.string
                    }
                    color = MinekordColor.YELLOW
                }
            }
        }

        event<MinekordAdvancementGrantEvent> {
            action {
                val display: AdvancementDisplay = event.advancement.comp_1913.get()

                createWebhookEmbed {
                    author {
                        icon = event.playerAvatar
                        name = parse(
                            when (display.frame) {
                                AdvancementFrame.CHALLENGE -> config[ChatSpec.DiscordSpec.challengeMessage]
                                AdvancementFrame.GOAL -> config[ChatSpec.DiscordSpec.goalMessage]
                                else -> config[ChatSpec.DiscordSpec.advancementMessage]
                            },
                            PlaceholderContext.of(event.player)
                        ) {
                            "advancement" to display.title
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
            }
        }

        event<MinekordServerStartedEvent> {
            action {
                createWebhookEmbed {
                    title = parse(config[ChatSpec.DiscordSpec.startMessage], event.server).string
                    color = MinekordColor.GREEN
                }
            }
        }

        event<MinekordServerStoppedEvent> {
            action {
                createWebhookEmbed {
                    title = parse(config[ChatSpec.DiscordSpec.stopMessage], server).string
                    color = MinekordColor.RED
                }
            }
        }

        event<MinekordServerEndTickEvent> {
            action {
                if (config[PresenceSpec.activityType] != MinekordActivityType.NONE) {
                    if (server.ticks % config[PresenceSpec.updateTicks] == 0) {
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
