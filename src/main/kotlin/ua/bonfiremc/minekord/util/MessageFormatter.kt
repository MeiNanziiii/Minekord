package ua.bonfiremc.minekord.util

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.network.chat.Style
import java.util.*

object MessageFormatter {
    val urlRegex: Regex = Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)")

    val userRegex: Regex = Regex("<@(\\d{17,19})>")
    val channelRegex: Regex = Regex("<#(\\d{17,19})>")
    val roleRegex: Regex = Regex("<@&(\\d{17,19})>")

    val emojiRegex: Regex = Regex("<a?(:\\w+:)\\d{17,19}>")

    suspend fun discordMessage(message: Message): String {
        val content: String = message.content.replace(urlRegex) { match -> "<underline><blue><url:'${match.value}'>${match.value}</url></blue></underline>" }

        val userIds: Set<Snowflake> = findSnowflakes(content, userRegex)
        val channelIds: Set<Snowflake> = findSnowflakes(content, channelRegex)
        val roleIds: Set<Snowflake> = findSnowflakes(content, roleRegex)

        val guild: Guild? = message.getGuildOrNull()

        val members: Map<Snowflake, String?> = userIds.associateWith { id ->
            guild?.getMemberOrNull(id)?.effectiveName
        }
        val channels: Map<Snowflake, String?> = channelIds.associateWith { id ->
            guild?.getChannelOrNull(id)?.data?.name?.value
        }
        val roles: Map<Snowflake, Role?> = roleIds.associateWith { id ->
            guild?.getRoleOrNull(id)
        }

        return content
            .replace(userRegex) { match ->
                val member: String = members[match.asSnowflake()] ?: "unknown-member"

                "<color:#5865F2>@$member</color>"
            }
            .replace(channelRegex) { match ->
                val channel: String = channels[match.asSnowflake()] ?: "unknown-channel"

                "<color:#5865F2>#$channel</color>"
            }
            .replace(roleRegex) { match ->
                val role: Role? = roles[match.asSnowflake()]

                if (role != null) {
                    "<color:${"#%06X".format(role.color.rgb and 0xFFFFFF)}>@${role.name}</color>"
                } else {
                    "<color:#5865F2>@unknown-role</color>"
                }
            }
            .replace(emojiRegex) { match ->
                match.groupValues[1]
            }
    }

    private fun findSnowflakes(content: String, regex: Regex): Set<Snowflake> {
        return regex
            .findAll(content)
            .map { match -> match.asSnowflake() }
            .toSet()
    }

    private fun MatchResult.asSnowflake() = Snowflake(groupValues[1].toULong())

    fun minecraftMessage(message: PlayerChatMessage): String {
        if (message.unsignedContent == null) return message.signedContent()

        return component(message.unsignedContent!!)
    }

    fun component(component: Component): String {
        val result: StringBuilder = StringBuilder()

        component.visit({ style, content ->
            var formatted: String = content

            if (style.isBold) {
                formatted = "**$formatted**"
            }
            if (style.isItalic) {
                formatted = "*$formatted*"
            }
            if (style.isUnderlined && style.clickEvent !is ClickEvent.OpenUrl) {
                formatted = "__${formatted}__"
            }

            result.append(formatted)

            Optional.empty()
        }, Style.EMPTY)

        return result.toString()
    }
}