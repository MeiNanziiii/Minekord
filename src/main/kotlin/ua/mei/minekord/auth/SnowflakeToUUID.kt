package ua.mei.minekord.auth

import dev.kord.common.annotation.KordExperimental
import dev.kord.core.entity.Member
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.AuthSpec
import ua.mei.minekord.config.config
import java.util.UUID
import kotlin.experimental.inv

object SnowflakeToUUID {
    private val client: HttpClient = HttpClient(OkHttp)

    fun enabled(): Boolean = config[AuthSpec.uuidFromSnowflake]
    fun allowOfflinePlayers(): Boolean = config[AuthSpec.allowOfflinePlayers]

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
    fun generateFromNickname(nickname: String): UUID? {
        return runBlocking {
            try {
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
        val mostSigBytes: ByteArray = ByteArray(8) { (discordId shr ((7 - it) * 8) and 0xFFu).toByte() }
        val leastSigBytes: ByteArray = mostSigBytes.map { it.inv() }.toByteArray()

        val mostSigBits: Long = mostSigBytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
        val leastSigBits: Long = leastSigBytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }

        return UUID(mostSigBits, leastSigBits)
    }
}
