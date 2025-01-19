package ua.mei.minekord.bot.extension

import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kordex.core.extensions.event
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.util.LuckPermsUtils

class RoleSyncExtension : MinekordExtension() {
    override val name: String = "minekord.rolesync"

    override suspend fun setup() {
        event<MemberUpdateEvent> {
            action {
                val player: ServerPlayerEntity = server.playerManager.getPlayer(event.member.effectiveName) ?: return@action

                LuckPermsUtils.syncPlayer(player)
            }
        }
    }

    override suspend fun onPlayerJoin(player: ServerPlayerEntity) {
        LuckPermsUtils.syncPlayer(player)
    }
}
