package ua.mei.minekord.utils

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import dev.kord.common.Color
import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.node.DynamicTextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TagLikeParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import ua.mei.minekord.Minekord
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.ChatSpec
import java.util.Base64
import kotlin.collections.firstOrNull
import java.util.function.Function as JavaFunction

val dynamicKey: ParserContext.Key<JavaFunction<String, Text?>> = DynamicTextNode.key(Minekord.MOD_ID)

val parser: NodeParser = NodeParser.builder()
    .simplifiedTextFormat()
    .quickText()
    .globalPlaceholders()
    .placeholders(TagLikeParser.PLACEHOLDER_ALTERNATIVE, dynamicKey)
    .build()

fun String.literal(): MutableText = Text.literal(this)

fun Component.toNative(wrapperLookup: RegistryWrapper.WrapperLookup): MutableText {
    return Text.Serialization.fromJson(GsonComponentSerializer.gson().serialize(this), wrapperLookup) ?: Text.empty()
}

fun Text.toAdventure(wrapperLookup: RegistryWrapper.WrapperLookup): Component {
    return GsonComponentSerializer.gson().deserialize(Text.Serialization.toJsonString(this, wrapperLookup))
}

fun String.toAdventure(): Component = Component.text(this)

fun String.summary(): String {
    return if (this.length <= config[ChatSpec.MinecraftSpec.summaryMaxLength]) {
        this.trim()
    } else {
        this.take(config[ChatSpec.MinecraftSpec.summaryMaxLength]).trim() + "..."
    }
}

class PlaceholderBuilder {
    private val map: MutableMap<String, Text> = mutableMapOf<String, Text>()

    infix fun String.to(value: Text) {
        map[this] = value
    }

    fun build(): Map<String, Text> = map.toMap()
}

fun parse(input: String, context: PlaceholderContext, placeholders: PlaceholderBuilder.() -> Unit): Text {
    val map: Map<String, Text> = PlaceholderBuilder().apply(placeholders).build()

    return parser.parseText(
        input,
        context.asParserContext().with(dynamicKey, JavaFunction { map[it] })
    )
}

fun parse(input: String, server: MinecraftServer): Text {
    return parser.parseText(
        input,
        PlaceholderContext.of(server).asParserContext()
    )
}

fun parse(input: String, player: ServerPlayerEntity): Text {
    return parser.parseText(
        input,
        PlaceholderContext.of(player).asParserContext()
    )
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

fun colorFromString(hex: String): Color = Color(hex.removePrefix("#").toInt(16))
