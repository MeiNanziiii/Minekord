package ua.mei.minekord.bot.extension

import dev.kord.core.entity.Member
import net.fabricmc.loader.api.FabricLoader
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.types.InheritanceNode
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.utils.AuthUtils

class RoleSyncExtension : MinekordExtension() {
    override val name: String = "minekord.rolesync"

    override suspend fun setup() {

    }

    override suspend fun onPlayerJoin(player: ServerPlayerEntity) {
        if (FabricLoader.getInstance().isModLoaded("luckperms") && !MinekordConfig.LuckPerms.roles.isEmpty()) {
            val lp: LuckPerms = LuckPermsProvider.get()

            lp.userManager.loadUser(player.gameProfile.id).thenAcceptAsync { user ->
                val member: Member = AuthUtils.findMember(player.gameProfile.name) ?: return@thenAcceptAsync
                val roles: List<ULong> = member.roleIds.map { it.value }

                user.data().toCollection()
                    .filterIsInstance<InheritanceNode>()
                    .filter { groupNode -> groupNode.groupName != "default" && MinekordConfig.LuckPerms.roles[groupNode.groupName] !in roles }
                    .forEach { user.data().remove(it) }

                MinekordConfig.LuckPerms.roles.forEach { entry ->
                    if (entry.value in roles) {
                        user.data().add(InheritanceNode.builder(entry.key).build())
                    }
                }

                lp.userManager.saveUser(user)
            }
        }
    }
}
