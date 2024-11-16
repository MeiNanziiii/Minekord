package ua.mei.minekord.bot

import dev.kord.core.behavior.execute
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.WebhookMessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.extensions.Extension
import net.minecraft.advancement.Advancement
import net.minecraft.entity.damage.DamageSource
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.koin.core.component.inject
import ua.mei.minekord.config.MinekordConfig

abstract class MinekordExtension : Extension() {
    abstract override val name: String

    val server: MinecraftServer by inject()

    suspend fun webhookMessage(builder: suspend WebhookMessageCreateBuilder.() -> Unit) {
        MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
            allowedMentions = MinekordBot.mentions
            avatarUrl = MinekordConfig.webhookAvatar
            builder()
        }
    }

    suspend fun webhookEmbed(builder: suspend EmbedBuilder.() -> Unit) {
        webhookMessage {
            embed {
                builder()
            }
        }
    }

    open suspend fun onChatMessage(player: ServerPlayerEntity, message: Text) {}

    open suspend fun onAdvancementGrant(player: ServerPlayerEntity, advancement: Advancement) {}

    open suspend fun onPlayerJoin(player: ServerPlayerEntity) {}
    open suspend fun onPlayerLeave(player: ServerPlayerEntity) {}
    open suspend fun onPlayerDeath(player: ServerPlayerEntity, source: DamageSource) {}

    open suspend fun onServerStart() {}
    open suspend fun onServerStop() {}
}
