package ua.mei.minekord.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.Channel
import dev.vankka.mcdiscordreserializer.renderer.implementation.DefaultMinecraftRenderer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.MinekordConfig.Chat
import ua.mei.minekord.config.MinekordConfig.Colors

object MinekordMinecraftRenderer : DefaultMinecraftRenderer() {
    override fun link(part: Component, link: String): Component {
        return super.link(part, link).color(Colors.link).decorate(TextDecoration.UNDERLINED)
    }

    override fun appendChannelMention(component: Component, id: String): Component {
        return runBlocking {
            val channel: Channel? = MinekordBot.guild.getChannelOrNull(Snowflake(id))
            val name: String = channel?.data?.name?.value ?: "unknown-channel"

            component.append("#$name".adventure().color(Colors.mention))
        }
    }

    override fun appendUserMention(component: Component, id: String): Component {
        return runBlocking {
            val member: Member? = MinekordBot.guild.getMemberOrNull(Snowflake(id))
            val name: String = member?.effectiveName ?: "unknown-member"

            component.append("@$name".adventure().color(Colors.link))
        }
    }

    override fun appendRoleMention(component: Component, id: String): Component {
        return runBlocking {
            val role: Role? = MinekordBot.guild.getRoleOrNull(Snowflake(id))
            val name: String = role?.name ?: "unknown-role"
            val color: TextColor = if (role != null && Chat.Minecraft.coloredRoles) TextColor.color(role.color.rgb) else Colors.mention

            component.append("@$name".adventure().color(color))
        }
    }
}
