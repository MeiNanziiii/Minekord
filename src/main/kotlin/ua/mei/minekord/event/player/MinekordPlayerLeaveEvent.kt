package ua.mei.minekord.event.player

import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.event.base.MinekordPlayerBaseEvent

data class MinekordPlayerLeaveEvent(
    override val player: ServerPlayerEntity
) : MinekordPlayerBaseEvent
