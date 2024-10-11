package ua.mei.minekord.utils

import dev.kord.common.annotation.KordExperimental
import dev.kord.core.entity.Member
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.ExperimentalSpec
import ua.mei.minekord.config.config
import java.util.UUID
import kotlin.experimental.inv

object ExperimentalUtils {
    private val client: HttpClient = HttpClient()

    fun premiumPlayer(uuid: UUID): Boolean {
        return runBlocking {
            try {
                client.get("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")
                    .status == HttpStatusCode.OK
            } catch (_: Exception) {
                false
            }
        }
    }

    @OptIn(KordExperimental::class)
    fun getPlayer(nickname: String): Member? {
        return runBlocking {
            MinekordBot.guild.getMembers(nickname).filter { member ->
                val trueRoleIds: List<ULong> = member.roleIds.map { it.value }
                config[ExperimentalSpec.DiscordSpec.requiredRoles].all { it in trueRoleIds }
            }.firstOrNull()
        }
    }

    @OptIn(KordExperimental::class)
    suspend fun getPlayerSuspend(nickname: String): Member? {
        return MinekordBot.guild.getMembers(nickname).filter { member ->
            val trueRoleIds: List<ULong> = member.roleIds.map { it.value }
            config[ExperimentalSpec.DiscordSpec.requiredRoles].all { it in trueRoleIds }
        }.firstOrNull()
    }

    fun generateFromNickname(nickname: String): UUID? {
        return runBlocking {
            try {
                val member: Member = getPlayer(nickname) ?: return@runBlocking null

                discordToUuid(member.id.value)
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun discordToUuid(discordId: ULong): UUID {
        val mostSigBytes: ByteArray = ByteArray(8) { (discordId shr ((7 - it) * 8) and 0xFFu).toByte() }
        val leastSigBytes: ByteArray = mostSigBytes.map { it.inv() }.toByteArray()

        val mostSigBits: Long = mostSigBytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
        val leastSigBits: Long = leastSigBytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }

        return UUID(mostSigBits, leastSigBits)
    }

    fun uuidToDiscord(uuid: UUID): ULong {
        val mostSigBits: Long = uuid.mostSignificantBits
        val mostSigBytes: ByteArray = ByteArray(8) { i -> ((mostSigBits shr ((7 - i) * 8)) and 0xFF).toByte() }

        return mostSigBytes.fold(0u) { acc, byte -> (acc shl 8) or (byte.toULong() and 0xFFu) }
    }
}
