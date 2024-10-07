package ua.mei.minekord.event

import com.mojang.authlib.GameProfile
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import java.net.SocketAddress

fun interface IPCheckEvent {
    fun request(address: SocketAddress, profile: GameProfile)

    companion object {
        val event: Event<IPCheckEvent> = EventFactory.createArrayBacked(IPCheckEvent::class.java) { listeners ->
            IPCheckEvent { address, profile ->
                listeners.forEach { listener ->
                    listener.request(address, profile)
                }
            }
        }
    }
}