package ua.mei.minekord.bot.extension

import com.mojang.authlib.GameProfile
import dev.kord.core.entity.Member
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kordex.core.extensions.event
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import net.fabricmc.loader.api.FabricLoader
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.types.InheritanceNode
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.Minekord
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.utils.AuthUtils
import kotlin.jvm.optionals.getOrNull

class RoleSyncExtension : MinekordExtension() {
    override val name: String = "minekord.rolesync"

    override suspend fun setup() {
        event<MemberUpdateEvent> {
            action {
                syncPlayer(event.member.effectiveName)
            }
        }
    }

    override suspend fun onPlayerJoin(player: ServerPlayerEntity) {
        syncPlayer(player.gameProfile.name)
    }

    override suspend fun onServerStart() {
        if (!MinekordConfig.LuckPerms.roles.isEmpty() && FabricLoader.getInstance().isModLoaded("luckperms")) {
            val startTime = System.currentTimeMillis()

            val members: List<Member> = MinekordBot.guild.members.filter { it.roleIds.map { it.value }.containsAll(MinekordConfig.Auth.requiredRoles) }.toList()

            members.forEach { member ->
                syncPlayer(member.effectiveName)
            }

            val endTime = System.currentTimeMillis()
            Minekord.logger.info("Roles successfully synced! Sync time: ${endTime - startTime}ms")
        }
    }

    fun syncPlayer(nickname: String) {
        if (!MinekordConfig.LuckPerms.roles.isEmpty() && FabricLoader.getInstance().isModLoaded("luckperms")) {
            val lp: LuckPerms = LuckPermsProvider.get()
            val member: Member = AuthUtils.findMember(nickname) ?: return
            val profile: GameProfile = server.userCache?.findByName(nickname)?.getOrNull() ?: return

            lp.userManager.loadUser(profile.id).thenAcceptAsync { user ->
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
