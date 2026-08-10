package ua.bonfiremc.minekord.util

import dev.kord.core.entity.Attachment
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import ua.bonfiremc.minekord.Minekord
import ua.bonfiremc.minekord.config.MinecraftSpec
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO

object AttachmentUtils {
    fun getAsComponents(attachments: Set<Attachment>): List<Component> {
        if (!Minekord.config[MinecraftSpec.appendImages]) return emptyList()

        return attachments.mapNotNull(::getAsComponent)
    }

    fun getAsComponent(attachment: Attachment): Component? {
        if (!Minekord.config[MinecraftSpec.appendImages] || !attachment.isImage || attachment.size >= 8 * 1024 * 1024) return null

        val image: BufferedImage = ImageIO.read(URI(attachment.url).toURL()) ?: return null

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

        return Component.literal("[${attachment.filename}]").withStyle { style ->
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