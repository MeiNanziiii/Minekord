package ua.mei.minekord.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import java.util.Locale

object SerializerUtils {
    val pingRegex: Regex = Regex("@([a-zA-Z0-9_]{3,16})")

    suspend fun convertMentions(message: String): String {
        return pingRegex.findAll(message).fold(message) { acc, matchResult ->
            val username: String = matchResult.groupValues[1]
            val mention: String? = ExperimentalUtils.getPlayerSuspend(username)?.mention
            if (mention != null) acc.replace(matchResult.value, mention) else acc
        }
    }

    fun translatableToString(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(component, Locale.ENGLISH))
    }
}
