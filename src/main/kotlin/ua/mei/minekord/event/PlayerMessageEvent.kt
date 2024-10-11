package ua.mei.minekord.event

import com.mojang.authlib.GameProfile
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.registry.RegistryWrapper
import net.minecraft.text.Text

fun interface PlayerMessageEvent {
    fun message(profile: GameProfile, message: Text, wrapperLookup: RegistryWrapper.WrapperLookup)

    companion object {
        val event: Event<PlayerMessageEvent> = EventFactory.createArrayBacked(PlayerMessageEvent::class.java) { listeners ->
            PlayerMessageEvent { profile, message, wrapperLookup ->
                listeners.forEach { listener ->
                    listener.message(profile, message, wrapperLookup)
                }
            }
        }
    }
}