package ua.mei.minekord.utils

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import dev.kord.common.Color
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.node.EmptyNode
import eu.pb4.placeholders.api.node.TextNode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.parser.DynamicNode
import java.util.Base64

val discordOptions: DiscordSerializerOptions = DiscordSerializerOptions.defaults()
    .withEmbedLinks(false)
    .withEscapeMarkdown(MinekordConfig.convertMarkdown)
    .withKeybindProvider(SerializerUtils::translatableToString)
    .withTranslationProvider(SerializerUtils::translatableToString)

val minecraftOptions: MinecraftSerializerOptions<Component> = MinecraftSerializerOptions.defaults()
    .addRenderer(MinekordMinecraftRenderer)

fun String.literal(): MutableText = Text.literal(this)

fun Component.toNative(): MutableText {
    return Text.Serializer.fromJson(GsonComponentSerializer.gson().serialize(this)) ?: Text.empty()
}

fun Text.toAdventure(): Component {
    return GsonComponentSerializer.gson().deserialize(Text.Serializer.toJson(this))
}

fun String.toAdventure(): Component = Component.text(this)

fun String.summary(): String {
    return if (this.length <= MinekordConfig.summaryMaxLength) {
        this.trim()
    } else {
        this.take(MinekordConfig.summaryMaxLength).trim() + "..."
    }
}

fun GameProfile.texture(): String {
    return try {
        JsonParser.parseString(Base64.getDecoder().decode(this.properties.get("textures").firstOrNull()?.value ?: "").toString(Charsets.UTF_8))
            .getAsJsonObject()
            .getAsJsonObject("textures")
            .getAsJsonObject("SKIN")
            .getAsJsonPrimitive("url")
            .getAsString()
            .let { it.substring(it.lastIndexOf('/') + 1) }
            .takeIf { it.isNotBlank() } ?: ""
    } catch (_: Throwable) {
        return ""
    }
}

fun String.toColor(): Color = Color(this.removePrefix("#").toInt(16))

fun TextNode.toText(context: PlaceholderContext, placeholders: PlaceholderBuilder.() -> Unit): Text {
    if (this == EmptyNode.INSTANCE) return Text.empty()
    return this.toText(context.asParserContext().with(DynamicNode.NODES, PlaceholderBuilder().apply(placeholders).map))
}

fun TextNode.toText(player: ServerPlayerEntity, placeholders: PlaceholderBuilder.() -> Unit = {}): Text {
    return this.toText(PlaceholderContext.of(player), placeholders)
}

fun TextNode.toText(server: MinecraftServer, placeholders: PlaceholderBuilder.() -> Unit = {}): Text {
    return this.toText(PlaceholderContext.of(server), placeholders)
}

class PlaceholderBuilder {
    val map: MutableMap<String, Text> = mutableMapOf<String, Text>()

    infix fun String.to(value: Text) {
        map[this] = value
    }
}

val ServerPlayerEntity.avatar: String
    get() = MinekordConfig.playerAvatar.toText(this@avatar) {
        "nickname" to this@avatar.gameProfile.name.literal()
        "texture" to this@avatar.gameProfile.texture().literal()
    }.string
