package ua.mei.minekord.util

import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.firstOrNull
import net.fabricmc.loader.api.FabricLoader
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.group.Group
import net.luckperms.api.model.user.User
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.InheritanceNode
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.MinekordConfig

object LuckPermsUtils {
    suspend fun prefixByNickname(nickname: String): String {
        if (!FabricLoader.getInstance().isModLoaded("luckperms") || MinekordConfig.LuckPerms.roles.isEmpty()) {
            return ""
        }

        val member: Member = MinekordBot.guild.members.firstOrNull { it.effectiveName == nickname && it.roleIds.map { it.value }.containsAll(MinekordConfig.Auth.requiredRoles) } ?: return ""
        val roles: List<ULong> = member.roleIds.map { it.value }

        val lp: LuckPerms = LuckPermsProvider.get()

        return MinekordConfig.LuckPerms.roles.filter { it.value in roles }.map {
            val group: Group = lp.groupManager.getGroup(it.key) ?: return@map ""

            group.getNodes(NodeType.PREFIX).firstOrNull()?.metaValue ?: ""
        }.takeIf { !it.isEmpty() }?.joinToString(
            separator = MinekordConfig.LuckPerms.middleSpacer,
            prefix = MinekordConfig.LuckPerms.startSpacer,
            postfix = MinekordConfig.LuckPerms.endSpacer
        ) ?: ""
    }

    suspend fun syncPlayer(player: ServerPlayerEntity) {
        if (!MinekordConfig.LuckPerms.roles.isEmpty() && FabricLoader.getInstance().isModLoaded("luckperms")) {
            val member: Member = MinekordBot.guild.members.firstOrNull { it.effectiveName == player.gameProfile.name && it.roleIds.map { it.value }.containsAll(MinekordConfig.Auth.requiredRoles) } ?: return

            val lp: LuckPerms = LuckPermsProvider.get()
            val user: User = lp.userManager.getUser(player.uuid) ?: return

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