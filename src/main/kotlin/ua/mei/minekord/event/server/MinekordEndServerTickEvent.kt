package ua.mei.minekord.event.server

import dev.kordex.core.events.KordExEvent
import net.minecraft.server.MinecraftServer
import ua.mei.minekord.event.base.MinekordServerBaseEvent

data class MinekordEndServerTickEvent(
    override val server: MinecraftServer
) : KordExEvent, MinekordServerBaseEvent
