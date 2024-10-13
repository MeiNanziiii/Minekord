package ua.mei.minekord.event.minekord

import dev.kordex.core.events.KordExEvent
import net.minecraft.advancement.Advancement
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.event.base.MinekordPlayerBaseEvent

data class MinekordPlayerAdvancementGrantEvent(
    override val player: ServerPlayerEntity,
    val advancement: Advancement
) : KordExEvent, MinekordPlayerBaseEvent
