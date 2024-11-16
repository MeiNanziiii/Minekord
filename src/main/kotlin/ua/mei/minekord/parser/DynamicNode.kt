package ua.mei.minekord.parser

import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.node.TextNode
import net.minecraft.text.Text

data class DynamicNode(val key: String, val text: Text) : TextNode {
    companion object {
        fun of(key: String): DynamicNode {
            return DynamicNode(key, Text.literal("{$key}"))
        }

        val NODES = ParserContext.Key<Map<String, Text>>("minekord:dynamic", null)
    }

    override fun toText(context: ParserContext, removeBackslashes: Boolean): Text {
        return context.get(NODES)?.getOrDefault(this.key, text) ?: text
    }

    override fun isDynamic(): Boolean {
        return true
    }
}
