package ua.mei.minekord.utils

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.node.EmptyNode
import eu.pb4.placeholders.api.node.TextNode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.config.MinekordConfig.Chat
import java.util.Base64

import java.util.function.Function as JavaFunction

fun String.literal(): MutableText = Text.literal(this)

fun Component.native(wrapperLookup: RegistryWrapper.WrapperLookup): MutableText {
    return Text.Serialization.fromJson(GsonComponentSerializer.gson().serialize(this), wrapperLookup) ?: Text.empty()
}

fun Text.adventure(wrapperLookup: RegistryWrapper.WrapperLookup): Component {
    return GsonComponentSerializer.gson().deserialize(Text.Serialization.toJsonString(this, wrapperLookup))
}

fun String.adventure(): Component = Component.text(this)

fun String.summary(): String {
    return if (this.length <= Chat.Minecraft.summaryMaxLength) {
        this.trim()
    } else {
        this.take(Chat.Minecraft.summaryMaxLength).trim() + "..."
    }
}

fun GameProfile.texture(): String {
    return try {
        JsonParser.parseString(Base64.getDecoder().decode(this.properties.get("textures").firstOrNull()?.value ?: "").toString(Charsets.UTF_8))
            .asJsonObject
            .getAsJsonObject("textures")
            .getAsJsonObject("SKIN")
            .getAsJsonPrimitive("url")
            .asString
            .let { it.substring(it.lastIndexOf('/') + 1) }
            .takeIf { it.isNotBlank() } ?: ""
    } catch (_: Throwable) {
        return ""
    }
}

fun String.toColor(): Color = Color(this.removePrefix("#").toInt(16))

fun TextNode.toText(context: PlaceholderContext, placeholders: PlaceholderBuilder.() -> Unit): Text {
    if (this == EmptyNode.INSTANCE) return Text.empty()

    val map = PlaceholderBuilder().apply(placeholders).map

    return this.toText(context.asParserContext().with(MinekordConfig.dynamicKey, JavaFunction { map[it] }))
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

val ServerPlayerEntity.avatarUrl: String
    get() = Chat.Webhook.playerAvatar.toText(this@avatarUrl) {
        "nickname" to this@avatarUrl.gameProfile.name.literal()
        "texture" to this@avatarUrl.gameProfile.texture().literal()
    }.string

val ULong.asSnowflake: Snowflake
    get() = Snowflake(this@asSnowflake)

val String.asSnowflake: Snowflake
    get() = Snowflake(this@asSnowflake)
