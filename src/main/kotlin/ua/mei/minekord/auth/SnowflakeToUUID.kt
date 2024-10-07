package ua.mei.minekord.auth

import dev.kord.common.annotation.KordExperimental
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.AuthSpec
import ua.mei.minekord.config.config
import java.util.UUID

object SnowflakeToUUID {
    fun enabled(): Boolean = config[AuthSpec.uuidFromSnowflake]

    @OptIn(KordExperimental::class)
    fun generateFromNickname(nickname: String): UUID? {
        return runBlocking {
            return@runBlocking try {
                val member: Member = MinekordBot.guild?.getMembers(nickname)?.filter {
                    it.roleIds.map { it.value }.all { it in config[AuthSpec.requiredRoles] }
                }?.firstOrNull() ?: return@runBlocking null

                generateFromId(member.id.value)
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun generateFromId(discordId: ULong): UUID {
        val mostSigBytes: ByteArray = ByteArray(8) { i -> (discordId shr ((7 - i) * 8) and 0xFFu).toByte() }
        val leastSigBytes: ByteArray = ByteArray(8) { i -> ((ULong.MAX_VALUE - discordId) shr ((7 - i) * 8) and 0xFFu).toByte() }

        val mostSigBits: Long = mostSigBytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
        val leastSigBits: Long = leastSigBytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }

        return UUID(mostSigBits, leastSigBits)
    }
}
