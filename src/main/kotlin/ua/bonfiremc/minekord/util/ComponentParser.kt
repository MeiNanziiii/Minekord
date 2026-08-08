package ua.bonfiremc.minekord.util

import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.node.DynamicTextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TagLikeParser
import net.minecraft.network.chat.Component
import ua.bonfiremc.minekord.Minekord

import java.util.function.Function as JavaFunction

object ComponentParser {
    private val dynamicKey: ParserContext.Key<JavaFunction<String, Component?>> = DynamicTextNode.key(Minekord.MOD_ID)
    private val parser: NodeParser = NodeParser.builder()
        .simplifiedTextFormat()
        .quickText()
        .commonPlaceholders()
        .placeholders(TagLikeParser.PLACEHOLDER_ALTERNATIVE, dynamicKey)
        .markdown()
        .build()

    fun parse(content: String): Component {
        return parser.parseComponent(content, ParserContext.of())
    }

    fun parse(content: String, builder: Placeholders<Component>.() -> Unit): Component {
        val placeholders: Placeholders<Component> = Placeholders<Component>().apply(builder)

        return parser.parseComponent(
            content,
            ParserContext.of().with(dynamicKey, JavaFunction { placeholders.map[it] })
        )
    }
}