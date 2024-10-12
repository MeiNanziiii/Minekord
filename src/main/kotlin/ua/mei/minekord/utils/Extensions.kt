package ua.mei.minekord.utils

import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.node.DynamicTextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TagLikeParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.MinecraftServer
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import ua.mei.minekord.Minekord
import ua.mei.minekord.config.ChatSpec
import ua.mei.minekord.config.config
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
