package ua.mei.minekord.event.minekord

import dev.kordex.core.events.KordExEvent
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import ua.mei.minekord.event.base.MinekordPlayerBaseEvent

data class MinekordPlayerMessageEvent(
    override val player: ServerPlayerEntity,
    val message: Text
) : KordExEvent, MinekordPlayerBaseEvent
