package ua.mei.minekord.util

import com.mojang.authlib.GameProfile
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.firstOrNull
import net.fabricmc.loader.api.FabricLoader
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import net.luckperms.api.node.types.InheritanceNode
import net.luckperms.api.query.QueryOptions
import net.minecraft.server.MinecraftServer
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.MinekordConfig
import kotlin.jvm.optionals.getOrNull

object LuckPermsUtils {
    fun prefixByNickname(nickname: String, server: MinecraftServer): String {
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            return ""
        }

        val lp: LuckPerms = LuckPermsProvider.get()
        val profile: GameProfile = server.userCache?.findByName(nickname)?.getOrNull() ?: return ""

        val user: User = lp.userManager.loadUser(profile.id).get()

        return user.cachedData.getMetaData(QueryOptions.defaultContextualOptions()).prefix ?: ""
    }

    suspend fun syncPlayer(nickname: String) {
        if (!MinekordConfig.LuckPerms.roles.isEmpty() && FabricLoader.getInstance().isModLoaded("luckperms")) {
            val member: Member = MinekordBot.guild.members.firstOrNull {
                it.effectiveName == nickname && it.roleIds.map { it.value }
                    .containsAll(MinekordConfig.Auth.requiredRoles)
            } ?: return

            val lp: LuckPerms = LuckPermsProvider.get()
            val user: User = lp.userManager.getUser(nickname) ?: return

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