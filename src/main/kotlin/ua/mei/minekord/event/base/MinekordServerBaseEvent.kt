package ua.mei.minekord.event.base

import dev.kordex.core.events.KordExEvent
import net.minecraft.server.MinecraftServer

interface MinekordServerBaseEvent : KordExEvent {
    val server: MinecraftServer
}
