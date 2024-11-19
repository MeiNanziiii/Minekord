package ua.mei.minekord.utils

import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.MinekordConfig
import java.util.UUID

object AuthUtils {
    fun findMember(name: String): Member? {
        return runBlocking {
            MinekordBot.guild.members.firstOrNull { it.effectiveName == name && it.roleIds.map { it.value }.containsAll(MinekordConfig.Auth.requiredRoles) }
        }
    }

    fun uuidFromMember(member: Member): UUID {
        return snowflakeToUuid(member.id.value)
    }

    fun snowflakeToUuid(snowflake: ULong): UUID {
        val mostSigBits: Long = snowflake.toLong()
        val leastSigBits: Long = mostSigBits.inv()
        return UUID(mostSigBits, leastSigBits)
    }

    fun uuidToSnowflake(uuid: UUID): ULong {
        return uuid.mostSignificantBits.toULong()
    }
}
