package ua.mei.minekord.util

import dev.kord.core.entity.Message
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import ua.mei.minekord.config.MinekordConfig.Chat
import ua.mei.minekord.config.MinekordConfig.Colors
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO

object ImageUtils {
    fun attachmentsToText(message: Message): List<Text> {
        if (!Chat.Minecraft.appendImages) return emptyList()

        return message.attachments
            .filter { it.isImage && it.size < 8 * 1024 * 1024 }
            .map { attachment ->
                val image: BufferedImage = ImageIO.read(URI(attachment.proxyUrl).toURL())

                val maxWidth: Int = Chat.Minecraft.imageMaxWidth
                val maxHeight: Int = Chat.Minecraft.imageMaxHeight

                var width: Int = image.width
                var height: Int = image.height

                val aspectRatio: Float = width.toFloat() / height.toFloat()

                if (width > maxWidth) {
                    width = maxWidth
                    height = (width / aspectRatio).toInt()
                }

                if (height > maxHeight) {
                    height = maxHeight
                    width = (height * aspectRatio).toInt()
                }

                val resizedImage: BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply {
                    val graphics = createGraphics()
                    graphics.drawImage(
                        image.getScaledInstance(width, height, Chat.Minecraft.imageInterpolation),
                        0,
                        0,
                        null
                    )
                    graphics.dispose()
                }

                val result: List<Text> = (0 until resizedImage.height).map { y ->
                    (0 until resizedImage.width).fold(Text.empty().styled { it.withItalic(false) }) { rowText, x ->
                        rowText.append(Text.literal("█").styled { it.withColor(resizedImage.getRGB(x, y) and 0xFFFFFF) })
                    }
                }

                val stack: ItemStack = ItemStack(Items.PAPER).apply {
                    set(DataComponentTypes.CUSTOM_NAME, Text.empty())
                    set(DataComponentTypes.LORE, LoreComponent(result))
                }

                Text.literal("[${attachment.filename}]").styled { style ->
                    style
                        .withColor(Colors.link.value())
                        .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, attachment.proxyUrl))
                        .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackContent(stack)))
                }
            }
    }
}