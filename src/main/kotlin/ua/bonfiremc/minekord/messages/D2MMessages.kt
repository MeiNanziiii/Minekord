package ua.bonfiremc.minekord.messages

import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.inChannel
import dev.kordex.core.extensions.event
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import ua.bonfiremc.minekord.Minekord
import ua.bonfiremc.minekord.config.BotSpec
import ua.bonfiremc.minekord.config.MinecraftSpec
import ua.bonfiremc.minekord.util.ComponentParser
import ua.bonfiremc.minekord.util.MessageUtils

class D2MMessages(val ext: MessagesExtension) {
    suspend fun onDiscordMessage(message: Message, member: Member) {
        val formattedContent: String = MessageUtils.getFormattedContent(message)

        val content: Component = ComponentParser.parse(Minekord.config[MinecraftSpec.messageFormat]) {
            "sender" to Component.literal(member.effectiveName)
            "message" to ComponentParser.parse(formattedContent)
        }

        val reply: Component? = message.referencedMessage?.let { replyMessage ->
            val referencedContent: String = MessageUtils.getFormattedContent(replyMessage)
            val sender: String = replyMessage.getAuthorAsMemberOrNull()?.effectiveName ?: "unknown-member"

            ComponentParser.parse(Minekord.config[MinecraftSpec.replyFormat]) {
                "sender" to Component.literal(sender)
                "message" to ComponentParser.parse(referencedContent)
                "summary" to ComponentParser.parse(getSummary(referencedContent))
            }
        }

        val component: MutableComponent = reply?.copy()?.append(content) ?: content.copy()

        val attachments: List<Component> = MessageUtils.getAttachmentsAsText(message).let { list ->
            if (message.content.isBlank() && list.isNotEmpty()) {
                component.append(list[0])

                list.drop(1)
            } else {
                list
            }
        }

        ext.server.playerList.broadcastSystemMessage(component, false)

        attachments.forEach { attachment ->
            ext.server.playerList.broadcastSystemMessage(attachment, false)
        }
    }

    private fun getSummary(content: String): String {
        val maxLength: Int = Minekord.config[MinecraftSpec.summaryMaxLength]
        val trimmed: String = content.replace("\n", " ").trim()

        return if (trimmed.length <= maxLength) {
            trimmed
        } else {
            trimmed.take(maxLength - 1).trimEnd() + "…"
        }
    }

    suspend fun registerDiscordEvents() {
        ext.event<MessageCreateEvent> {
            check { inChannel(Minekord.config[BotSpec.channel]) }
            check {
                failIf {
                    event.member == null
                            || event.message.content.isBlank() && event.message.attachments.none { attachment -> attachment.isImage }
                            || event.message.author?.id == kord.selfId
                            || event.message.webhookId == ext.webhook.id
                }
            }

            action {
                onDiscordMessage(event.message, event.member!!)
            }
        }
    }
}