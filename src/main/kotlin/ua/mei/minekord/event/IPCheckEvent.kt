package ua.mei.minekord.event

import com.mojang.authlib.GameProfile
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import java.net.SocketAddress

fun interface IPCheckEvent {
    fun request(socketAddress: SocketAddress, profile: GameProfile)

    companion object {
        val EVENT: Event<IPCheckEvent> = EventFactory.createArrayBacked(IPCheckEvent::class.java) { listeners ->
            IPCheckEvent { socketAddress, profile ->
                listeners.forEach { listener ->
                    listener.request(socketAddress, profile)
                }
            }
        }
    }
}
