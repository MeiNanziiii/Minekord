package ua.mei.minekord.event.player

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import ua.mei.minekord.event.base.MinekordPlayerBaseEvent

data class MinekordPlayerMessageEvent(
    override val player: ServerPlayerEntity,
    val message: Text
) : MinekordPlayerBaseEvent
