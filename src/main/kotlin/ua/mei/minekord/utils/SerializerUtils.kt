package ua.mei.minekord.utils

import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.toList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import ua.mei.minekord.bot.MinekordBot
import java.util.*

object SerializerUtils {
    val pingRegex: Regex = Regex("@(\\S{1,32})")

    suspend fun convertMentions(message: String): String {
        val members: Map<String, Member> = MinekordBot.guild.members.toList().associateBy { it.effectiveName.lowercase() }

        return pingRegex.replace(message) { matchResult ->
            val effectiveName = matchResult.groupValues[1].lowercase()
            members[effectiveName]?.mention ?: matchResult.value
        }
    }

    fun translatableToString(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(component, Locale.ENGLISH))
    }
}
