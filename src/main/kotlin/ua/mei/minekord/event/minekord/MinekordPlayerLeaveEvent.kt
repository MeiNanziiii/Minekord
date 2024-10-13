package ua.mei.minekord.event.minekord

import dev.kordex.core.events.KordExEvent
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.event.base.MinekordPlayerBaseEvent

data class MinekordPlayerLeaveEvent(
    override val player: ServerPlayerEntity
) : KordExEvent, MinekordPlayerBaseEvent
