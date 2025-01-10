package ua.mei.minekord.bot.extension

import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kordex.core.extensions.event
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.utils.LuckPermsUtils

class RoleSyncExtension : MinekordExtension() {
    override val name: String = "minekord.rolesync"

    override suspend fun setup() {
        event<MemberUpdateEvent> {
            action {
                LuckPermsUtils.syncPlayer(event.member.effectiveName)
            }
        }
    }

    override suspend fun onPlayerJoin(player: ServerPlayerEntity) {
        LuckPermsUtils.syncPlayer(player.gameProfile.name)
    }
}
