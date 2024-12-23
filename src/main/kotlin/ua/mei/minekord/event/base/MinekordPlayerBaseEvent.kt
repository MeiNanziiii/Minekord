package ua.mei.minekord.event.base

import dev.kordex.core.events.KordExEvent
import eu.pb4.placeholders.api.PlaceholderContext
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.utils.literal
import ua.mei.minekord.utils.parse
import ua.mei.minekord.utils.texture

interface MinekordPlayerBaseEvent : KordExEvent {
    val player: ServerPlayerEntity

    val playerAvatar: String
        get() = parse(config[ChatSpec.WebhookSpec.playerAvatar], PlaceholderContext.of(player)) {
            "nickname" to player.gameProfile.name.literal()
            "texture" to player.gameProfile.texture().literal()
        }.string
}
