package ua.mei.minekord.event.base

import net.minecraft.server.MinecraftServer

interface MinekordServerBaseEvent {
    val server: MinecraftServer
}
