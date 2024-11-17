package ua.mei.minekord.event

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.text.Text
import ua.mei.minekord.utils.MessageSender

fun interface ChatMessageEvent {
    fun message(message: Text, sender: MessageSender)

    companion object {
        val EVENT: Event<ChatMessageEvent> = EventFactory.createArrayBacked(ChatMessageEvent::class.java) { listeners ->
            ChatMessageEvent { message, sender ->
                listeners.forEach { listener ->
                    listener.message(message, sender)
                }
            }
        }
    }
}
