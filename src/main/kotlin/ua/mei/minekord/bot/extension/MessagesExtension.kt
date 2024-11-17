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
import net.minecraft.advancement.Advancement
import net.minecraft.advancement.AdvancementDisplay
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.utils.MessageSender
import ua.mei.minekord.utils.SerializerUtils
import ua.mei.minekord.utils.adventure
import ua.mei.minekord.utils.avatar
import ua.mei.minekord.utils.literal
import ua.mei.minekord.utils.native
import ua.mei.minekord.utils.summary
import ua.mei.minekord.utils.toText

class MessagesExtension : MinekordExtension() {
    override val name: String = "minekord.messages"

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { isNotBot() }
            check { inChannel(Snowflake(MinekordConfig.channel)) }

            action {
                val message: Message = event.message
                val sender: Member = event.member ?: return@action

                var content: Text = if (MinekordConfig.convertMarkdown) {
                    MinecraftSerializer.INSTANCE.serialize(message.content, MinekordBot.minecraftOptions).native()
                } else {
                    message.content.literal()
                }

                if (message.referencedMessage != null) {
                    val replyContent: Text = MinecraftSerializer.INSTANCE.serialize(message.referencedMessage!!.content, MinekordBot.minecraftOptions).native()

                    val reply: Text = MinekordConfig.replyFormat.toText(server) {
                        "sender" to (message.referencedMessage!!.author?.effectiveName ?: message.referencedMessage!!.data.author.username).literal()
                        "message" to replyContent
                        "summary" to replyContent.string.summary().literal()
                    }

                    content = Text.empty().append(reply).append("\n").append(content)
                }

                content = MinekordConfig.messageFormat.toText(server) {
                    "sender" to sender.effectiveName.literal()
                    "message" to content
                }

                server.playerManager.broadcast(content, false)
            }
        }
    }

    override suspend fun onChatMessage(message: Text, sender: MessageSender) {
        webhookMessage {
            username = sender.name
            avatarUrl = sender.avatar

            content = DiscordSerializer.INSTANCE.serialize(message.adventure(), MinekordBot.discordOptions).let {
                if (MinekordConfig.convertMentions) {
                    SerializerUtils.convertMentions(it)
                } else {
                    it
                }
            }.takeIf { it.isNotBlank() } ?: return@webhookMessage
        }
    }

    override suspend fun onAdvancementGrant(player: ServerPlayerEntity, advancement: Advancement) {
        val display: AdvancementDisplay = advancement.display ?: return
        val frame: AdvancementFrame = display.frame

        val message = when (frame) {
            AdvancementFrame.CHALLENGE -> MinekordConfig.challengeMessage
            AdvancementFrame.GOAL -> MinekordConfig.goalMessage
            AdvancementFrame.TASK -> MinekordConfig.advancementMessage
        }.toText(player) { "advancement" to display.title }.string

        webhookEmbed {
            author {
                icon = player.avatar
                name = message
            }
            footer {
                text = display.description.string
            }
            color = if (frame == AdvancementFrame.CHALLENGE) MinekordConfig.purple else MinekordConfig.blue
        }
    }

    override suspend fun onPlayerJoin(player: ServerPlayerEntity) {
        webhookEmbed {
            author {
                name = MinekordConfig.joinMessage.toText(player).string
                icon = player.avatar
            }
            color = MinekordConfig.green
        }
    }

    override suspend fun onPlayerLeave(player: ServerPlayerEntity) {
        webhookEmbed {
            author {
                name = MinekordConfig.leaveMessage.toText(player).string
                icon = player.avatar
            }
            color = MinekordConfig.red
        }
    }

    override suspend fun onPlayerDeath(player: ServerPlayerEntity, source: DamageSource) {
        webhookEmbed {
            author {
                icon = player.avatar
                name = MinekordConfig.deathMessage.toText(player) {
                    "message" to source.getDeathMessage(player)
                }.string
            }
            color = MinekordConfig.orange
        }
    }

    override suspend fun onServerStart() {
        webhookEmbed {
            title = MinekordConfig.startMessage.toText(server).string
            color = MinekordConfig.green
        }
    }

    override suspend fun onServerStop() {
        webhookEmbed {
            title = MinekordConfig.stopMessage.toText(server).string
            color = MinekordConfig.red
        }
    }
}
