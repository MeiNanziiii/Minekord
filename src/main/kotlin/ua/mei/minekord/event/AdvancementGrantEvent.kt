package ua.mei.minekord.event

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.advancement.Advancement
import net.minecraft.server.network.ServerPlayerEntity

fun interface AdvancementGrantEvent {
    fun grant(player: ServerPlayerEntity, advancement: Advancement)

    companion object {
        val EVENT: Event<AdvancementGrantEvent> = EventFactory.createArrayBacked(AdvancementGrantEvent::class.java) { listeners ->
            AdvancementGrantEvent { player, advancement ->
                listeners.forEach { listener ->
                    listener.grant(player, advancement)
                }
            }
        }
    }
}
