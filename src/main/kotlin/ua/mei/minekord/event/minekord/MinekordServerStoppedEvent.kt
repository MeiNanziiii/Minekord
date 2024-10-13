package ua.mei.minekord.event.minekord

import dev.kordex.core.events.KordExEvent
import net.minecraft.server.MinecraftServer
import ua.mei.minekord.event.base.MinekordServerBaseEvent

data class MinekordServerStoppedEvent(
    override val server: MinecraftServer
) : KordExEvent, MinekordServerBaseEvent
