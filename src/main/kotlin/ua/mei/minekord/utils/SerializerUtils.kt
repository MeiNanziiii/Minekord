package ua.mei.minekord.utils

import kotlinx.coroutines.flow.firstOrNull
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import ua.mei.minekord.bot.MinekordBot
import java.util.Locale

object SerializerUtils {
    val pingRegex: Regex = Regex("@([a-zA-Z0-9_]{3,16})")

    suspend fun convertMentions(message: String): String {
        return pingRegex.findAll(message).fold(message) { acc, matchResult ->
            val mention: String? = MinekordBot.guild.members.firstOrNull { it.effectiveName.equals(matchResult.groupValues[1], true) }?.mention

            if (mention != null) acc.replace(matchResult.value, mention) else acc
        }
    }

    fun translatableToString(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(component, Locale.ENGLISH))
    }
}
