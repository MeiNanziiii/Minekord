package ua.mei.minekord.bot.extension

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.inChannel
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.event
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer
import eu.pb4.placeholders.api.PlaceholderContext
import net.minecraft.advancement.Advancement
import net.minecraft.advancement.AdvancementDisplay
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.utils.MinekordColor
import ua.mei.minekord.utils.SerializerUtils
import ua.mei.minekord.utils.avatar
import ua.mei.minekord.utils.discordOptions
import ua.mei.minekord.utils.literal
import ua.mei.minekord.utils.minecraftOptions
import ua.mei.minekord.utils.parseText
import ua.mei.minekord.utils.summary
import ua.mei.minekord.utils.toAdventure
import ua.mei.minekord.utils.toNative

class MessagesExtension : MinekordExtension() {
    override val name: String = "Messages Extension"

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { isNotBot() }
            check { inChannel(Snowflake(config[BotSpec.channel])) }

            action {
                val message: Message = event.message
                val sender: Member = event.member ?: return@action

                var content: Text = if (config[ChatSpec.convertMarkdown]) {
                    MinecraftSerializer.INSTANCE.serialize(message.content, minecraftOptions).toNative()
                } else {
                    message.content.literal()
                }

                if (message.referencedMessage != null) {
                    val replyContent: Text = MinecraftSerializer.INSTANCE.serialize(message.referencedMessage!!.content, minecraftOptions).toNative()

                    val reply: Text = parseText(config[ChatSpec.MinecraftSpec.replyFormat], PlaceholderContext.of(server)) {
                        "sender" to (message.referencedMessage!!.author?.effectiveName ?: message.referencedMessage!!.data.author.username).literal()
                        "message" to replyContent
                        "summary" to replyContent.string.summary().literal()
                    }

                    content = Text.empty().append(reply).append("\n").append(content)
                }

                content = parseText(config[ChatSpec.MinecraftSpec.messageFormat], PlaceholderContext.of(server)) {
                    "sender" to sender.effectiveName.literal()
                    "message" to content
                }

                server.playerManager.broadcast(content, false)
            }
        }
    }

    override suspend fun onAdvancementGrant(player: ServerPlayerEntity, advancement: Advancement) {
        val display: AdvancementDisplay = advancement.display ?: return
        val frame: AdvancementFrame = display.frame

        val message: String = parseText(
            when (frame) {
                AdvancementFrame.CHALLENGE -> config[ChatSpec.DiscordSpec.challengeMessage]
                AdvancementFrame.GOAL -> config[ChatSpec.DiscordSpec.goalMessage]

                else -> config[ChatSpec.DiscordSpec.advancementMessage]
            },
            PlaceholderContext.of(player)
        ) {
            "advancement" to display.title
        }.string

        webhookEmbed {
            author {
                icon = player.avatar
                name = message
            }
            footer {
                text = display.description.string
            }
            color = if (frame == AdvancementFrame.CHALLENGE) MinekordColor.PURPLE else MinekordColor.BLUE
        }
    }

    override suspend fun onPlayerDeath(player: ServerPlayerEntity, source: DamageSource) {
        webhookEmbed {
            author {
                icon = player.avatar
                name = parseText(config[ChatSpec.DiscordSpec.deathMessage], PlaceholderContext.of(player)) {
                    "message" to source.getDeathMessage(player)
                }.string
            }
            color = MinekordColor.ORANGE
        }
    }

    override suspend fun onPlayerJoin(player: ServerPlayerEntity) {
        webhookEmbed {
            author {
                name = parseText(config[ChatSpec.DiscordSpec.joinMessage], player).string
                icon = player.avatar
            }
            color = MinekordColor.GREEN
        }
    }

    override suspend fun onPlayerLeave(player: ServerPlayerEntity) {
        webhookEmbed {
            author {
                name = parseText(config[ChatSpec.DiscordSpec.leaveMessage], player).string
                icon = player.avatar
            }
            color = MinekordColor.RED
        }
    }

    override suspend fun onChatMessage(player: ServerPlayerEntity, message: Text) {
        webhookMessage {
            username = player.gameProfile.name
            avatarUrl = player.avatar

            content = DiscordSerializer.INSTANCE.serialize(
                message.toAdventure(), discordOptions
            ).let {
                if (config[ChatSpec.convertMentions]) {
                    SerializerUtils.convertMentions(it)
                } else {
                    it
                }
            }.takeIf { it.isNotBlank() } ?: return@webhookMessage
        }
    }

    override suspend fun onServerStart() {
        webhookEmbed {
            title = parseText(config[ChatSpec.DiscordSpec.startMessage], server).string
            color = MinekordColor.GREEN
        }
    }

    override suspend fun onServerStop() {
        webhookEmbed {
            title = parseText(config[ChatSpec.DiscordSpec.stopMessage], server).string
            color = MinekordColor.RED
        }
    }
}