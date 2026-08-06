package ua.bonfiremc.minekord.event

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.server.level.ServerPlayer

fun interface AdvancementGrantEvent {
    fun onAdvancementGrant(player: ServerPlayer, holder: AdvancementHolder)

    companion object {
        @JvmField
        val EVENT: Event<AdvancementGrantEvent> = EventFactory.createArrayBacked(AdvancementGrantEvent::class.java) { listeners ->
            AdvancementGrantEvent { player, holder ->
                listeners.forEach { listener ->
                    listener.onAdvancementGrant(player, holder)
                }
            }
        }
    }
}