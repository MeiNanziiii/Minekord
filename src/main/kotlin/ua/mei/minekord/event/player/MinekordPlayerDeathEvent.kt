package ua.mei.minekord.event.player

import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.event.base.MinekordPlayerBaseEvent

data class MinekordPlayerDeathEvent(
    override val player: ServerPlayerEntity,
    val source: DamageSource
) : MinekordPlayerBaseEvent
