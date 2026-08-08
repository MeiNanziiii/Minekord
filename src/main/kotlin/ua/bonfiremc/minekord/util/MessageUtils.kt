package ua.bonfiremc.minekord.util

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import ua.bonfiremc.minekord.Minekord
import ua.bonfiremc.minekord.config.MinecraftSpec
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.net.URI
import java.util.*
import javax.imageio.ImageIO

object MessageUtils {
    val userRegex: Regex = Regex("<@(\\d+)>")
    val channelRegex: Regex = Regex("<#(\\d+)>")
    val roleRegex: Regex = Regex("<@&(\\d+)>")

    val urlRegex: Regex = Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)")

    suspend fun getFormattedContent(message: Message): String {
        val content: String = message.content.replace(urlRegex) { match -> "<underline><blue><url:'${match.value}'>${match.value}</url></blue></underline>" }

        val userIds: Set<Snowflake> = findMatches(content, userRegex)
        val channelIds: Set<Snowflake> = findMatches(content, channelRegex)
        val roleIds: Set<Snowflake> = findMatches(content, roleRegex)

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
                val member: String = members[match.snowflake()] ?: "unknown-member"

                "<color:#5865F2>@$member</color>"
            }
            .replace(channelRegex) { match ->
                val channel: String = channels[match.snowflake()] ?: "unknown-channel"

                "<color:#5865F2>#$channel</color>"
            }
            .replace(roleRegex) { match ->
                val role: Role? = roles[match.snowflake()]

                if (role != null) {
                    "<color:${"#%06X".format(role.color.rgb and 0xFFFFFF)}>@${role.name}</color>"
                } else {
                    "<color:#5865F2>@unknown-role</color>"
                }
            }
    }

    private fun findMatches(content: String, regex: Regex): Set<Snowflake> {
        return regex
            .findAll(content)
            .map { match -> match.snowflake() }
            .toSet()
    }

    private fun MatchResult.snowflake() = Snowflake(groupValues[1].toULong())

    fun getAttachmentsAsText(message: Message): List<Component> {
        if (!Minekord.config[MinecraftSpec.appendImages]) return emptyList()

        return message.attachments
            .filter { attachment -> attachment.isImage && attachment.size < 8 * 1024 * 1024 }
            .mapNotNull { attachment ->
                val image: BufferedImage = ImageIO.read(URI(attachment.url).toURL()) ?: return@mapNotNull null

                val scale: Double = minOf(
                    1.0,
                    Minekord.config[MinecraftSpec.imageMaxWidth].toDouble() / image.width,
                    Minekord.config[MinecraftSpec.imageMaxHeight].toDouble() / image.height
                )

                val width: Int = (image.width * scale).toInt()
                val height: Int = (image.height * scale).toInt()

                val resizedImage: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply {
                    val graphics: Graphics2D = createGraphics()

                    graphics.drawImage(
                        image.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                        0, 0,
                        null
                    )

                    graphics.dispose()
                }

                val components: List<Component> = (0 until height).map { y ->
                    var component: MutableComponent? = null

                    var color: Int = resizedImage.getRGB(0, y) and 0xFFFFFF
                    var count: Int = 0

                    (0 until width).forEach { x ->
                        val pixel: Int = if (x == 0) color else (resizedImage.getRGB(x, y) and 0xFFFFFF)

                        if (color == pixel) {
                            count++
                        } else {
                            val child: MutableComponent = Component.literal("█".repeat(count))
                                .withStyle { style ->
                                    style.withColor(color)
                                        .withShadowColor(color or 0xFF000000.toInt())
                                        .withItalic(false)
                                }

                            if (component == null) {
                                component = child
                            } else {
                                component.append(child)
                            }

                            color = pixel
                            count = 1
                        }
                    }

                    val child: MutableComponent = Component.literal("█".repeat(count))
                        .withStyle { style ->
                            style.withItalic(false)
                                .withColor(color)
                                .withShadowColor(color or 0xFF000000.toInt())
                        }

                    if (component == null) {
                        component = child
                    } else {
                        component.append(child)
                    }

                    return@map component as Component
                }

                Component.literal("[${attachment.filename}]").withStyle { style ->
                    style.applyFormat(ChatFormatting.BLUE)
                        .withClickEvent(ClickEvent.OpenUrl(URI(attachment.proxyUrl)))
                        .withHoverEvent(
                            HoverEvent.ShowItem(
                                ItemStackTemplate(
                                    Items.PAPER,
                                    DataComponentPatch.builder()
                                        .set(DataComponents.ITEM_NAME, Component.literal(attachment.filename))
                                        .set(DataComponents.LORE, ItemLore(components))
                                        .build()
                                )
                            )
                        )
                }
            }
    }

    fun message2String(message: PlayerChatMessage): String {
        if (message.unsignedContent == null) return message.signedContent()

        return component2String(message.unsignedContent!!)
    }

    fun component2String(component: Component): String {
        val text: StringBuilder = StringBuilder()

        component.visit({ style, content ->
            var formatted: String = content

            if (style.isBold) {
                formatted = "**$formatted**"
            }
            if (style.isItalic) {
                formatted = "*$formatted*"
            }
            if (style.isUnderlined) {
                formatted = "__${formatted}__"
            }

            text.append(formatted)

            Optional.empty()
        }, Style.EMPTY)

        return text.toString()
    }
}