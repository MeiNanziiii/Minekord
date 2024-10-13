package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.Image
import dev.kordex.core.extensions.Extension
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.event.AdvancementGrantEvent
import ua.mei.minekord.event.player.MinekordAdvancementGrantEvent
import ua.mei.minekord.event.player.MinekordPlayerDeathEvent
import ua.mei.minekord.event.player.MinekordPlayerJoinEvent
import ua.mei.minekord.event.player.MinekordPlayerLeaveEvent
import ua.mei.minekord.event.player.MinekordPlayerMessageEvent
import ua.mei.minekord.event.server.MinekordServerEndTickEvent
import ua.mei.minekord.event.server.MinekordServerStartedEvent
import ua.mei.minekord.event.server.MinekordServerStoppedEvent

class SetupExtension : Extension() {
    override val name: String = "Setup Extension"

    override suspend fun setup() {
        MinekordBot.guild = kord.getGuild(Snowflake(config[BotSpec.guild]))
        MinekordBot.channel = MinekordBot.guild.getChannel(Snowflake(config[BotSpec.channel])) as TextChannel
        MinekordBot.webhook = MinekordBot.channel.webhooks.firstOrNull { it.name == config[ChatSpec.WebhookSpec.webhookName] } ?: MinekordBot.channel.createWebhook(config[ChatSpec.WebhookSpec.webhookName]) {
            avatar = Image.fromUrl(HttpClient(), config[ChatSpec.WebhookSpec.webhookAvatar])
        }

        AdvancementGrantEvent.EVENT.register { player, advancement ->
            MinekordBot.launch { MinekordBot.bot?.send(MinekordAdvancementGrantEvent(player, advancement)) }
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            if (entity is ServerPlayerEntity) {
                MinekordBot.launch { MinekordBot.bot?.send(MinekordPlayerDeathEvent(entity, source)) }
            }
            true
        }
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            MinekordBot.launch { MinekordBot.bot?.send(MinekordPlayerJoinEvent(handler.player)) }
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            val event: MinekordPlayerLeaveEvent = MinekordPlayerLeaveEvent(handler.player)

            if (server.isStopping) {
                runBlocking { MinekordBot.bot?.send(event) }
            } else {
                MinekordBot.launch { MinekordBot.bot?.send(event) }
            }
        }
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            MinekordBot.launch { MinekordBot.bot?.send(MinekordPlayerMessageEvent(sender, message.content)) }
        }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            MinekordBot.launch { MinekordBot.bot?.send(MinekordServerEndTickEvent(server)) }
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            MinekordBot.launch { MinekordBot.bot?.send(MinekordServerStartedEvent(server)) }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            runBlocking { MinekordBot.bot?.send(MinekordServerStoppedEvent(server)) }
        }
    }
}
