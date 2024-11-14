package ua.mei.minekord.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.ensureWebhook
import dev.kordex.core.utils.loadModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.bot.extension.MessagesExtension
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.PresenceSpec
import ua.mei.minekord.utils.AdvancementGrantEvent
import ua.mei.minekord.utils.MinekordActivityType
import ua.mei.minekord.utils.parseText
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope, ServerLifecycleEvents.ServerStarting {
    val extensions: MutableList<() -> Extension> = mutableListOf()

    lateinit var bot: ExtensibleBot
    lateinit var guild: Guild
    lateinit var channel: TopGuildMessageChannel
    lateinit var webhook: Webhook

    override fun onServerStarting(server: MinecraftServer) {
        extensions += ::MessagesExtension

        runBlocking {
            bot = ExtensibleBot(config[BotSpec.token]) {
                members {
                    fill(config[BotSpec.guild])
                }
                hooks {
                    afterKoinSetup {
                        loadModule {
                            single { server }
                        }
                    }
                }
            }
            guild = bot.kordRef.getGuild(Snowflake(config[BotSpec.guild]))
            channel = guild.getChannel(Snowflake(config[BotSpec.channel])) as TopGuildMessageChannel
            webhook = ensureWebhook(channel, config[ChatSpec.WebhookSpec.webhookName])

            extensions.forEach { bot.addExtension(it) }

            setup()
        }
        launch {
            bot.start()
        }
    }

    fun setup() {
        val minekordExtensions: List<MinekordExtension> = bot.findExtensions()

        AdvancementGrantEvent.EVENT.register { player, advancement ->
            minekordExtensions.forEach {
                launch {
                    it.onAdvancementGrant(player, advancement)
                }
            }
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            if (entity is ServerPlayerEntity) {
                minekordExtensions.forEach {
                    launch {
                        it.onPlayerDeath(entity, source)
                    }
                }
            }
            true
        }
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            minekordExtensions.forEach {
                launch {
                    it.onPlayerJoin(handler.player)
                }
            }
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            minekordExtensions.forEach {
                if (server.isStopping) {
                    runBlocking {
                        it.onPlayerLeave(handler.player)
                    }
                } else {
                    launch {
                        it.onPlayerLeave(handler.player)
                    }
                }
            }
        }
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            println("test")
            minekordExtensions.forEach {
                println("test")
                launch {
                    it.onChatMessage(sender, message.content)
                }
            }
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            minekordExtensions.forEach {
                launch {
                    it.onServerStart()
                }
            }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            minekordExtensions.forEach {
                runBlocking {
                    it.onServerStop()
                }
            }
        }

        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (server.ticks % config[PresenceSpec.updateTicks] == 0 && config[PresenceSpec.activityType] != MinekordActivityType.NONE) {
                launch {
                    bot.kordRef.editPresence {
                        val text: String = parseText(config[PresenceSpec.activityText], server).string

                        when (config[PresenceSpec.activityType]) {
                            MinekordActivityType.NONE -> Unit
                            MinekordActivityType.PLAYING -> playing(text)
                            MinekordActivityType.LISTENING -> listening(text)
                            MinekordActivityType.WATCHING -> watching(text)
                            MinekordActivityType.COMPETING -> competing(text)
                        }
                    }
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}
