package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createWebhook
import dev.kord.core.entity.channel.TextChannel
import dev.kordex.core.extensions.Extension
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
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
import ua.mei.minekord.event.server.MinekordServerStartedEvent

class SetupExtension : Extension() {
    override val name: String = "Setup Extension"

    override suspend fun setup() {
        MinekordBot.guild = kord.getGuild(Snowflake(config[BotSpec.guild]))
        MinekordBot.channel = MinekordBot.guild.getChannel(Snowflake(config[BotSpec.channel])) as TextChannel
        MinekordBot.webhook = MinekordBot.channel.webhooks.firstOrNull { it.name == config[ChatSpec.WebhookSpec.webhookName] } ?: MinekordBot.channel.createWebhook(config[ChatSpec.WebhookSpec.webhookName])

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
            MinekordBot.launch { MinekordBot.bot?.send(MinekordPlayerLeaveEvent(handler.player)) }
        }
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            MinekordBot.launch { MinekordBot.bot?.send(MinekordPlayerMessageEvent(sender, message.content)) }
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            MinekordBot.launch { MinekordBot.bot?.send(MinekordServerStartedEvent(server)) }
        }
    }
}
