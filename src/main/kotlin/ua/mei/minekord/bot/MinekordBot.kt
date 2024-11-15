package ua.mei.minekord.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
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
import ua.mei.minekord.Minekord
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.PresenceSpec
import ua.mei.minekord.config.spec.PresenceSpec.MinekordActivityType
import ua.mei.minekord.event.AdvancementGrantEvent
import ua.mei.minekord.utils.parseText
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KCallable

@OptIn(PrivilegedIntent::class)
object MinekordBot : CoroutineScope, ServerLifecycleEvents.ServerStarting {
    private lateinit var bot: ExtensibleBot

    private val extensions: MutableList<() -> Extension> = mutableListOf()
    private var loaded: Boolean = false

    lateinit var guild: Guild
        private set
    lateinit var channel: TopGuildMessageChannel
        private set
    lateinit var webhook: Webhook
        private set

    override fun onServerStarting(server: MinecraftServer) {
        runBlocking {
            bot = ExtensibleBot(config[BotSpec.token]) {
                applicationCommands {
                    enabled = true
                }
                intents {
                    +Intent.GuildMembers
                }
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

            loaded = true

            setup()
        }
        launch { bot.start() }
    }

    fun registerExtension(extension: () -> Extension) {
        if (extension in extensions)
            throw IllegalArgumentException("Extension already registered!")

        if (loaded)
            throw IllegalStateException("Cannot register extension after bot startup!")

        extensions += extension
        Minekord.logger.info("Registered extension: ${(extension as KCallable<*>).returnType}")
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
            minekordExtensions.forEach {
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
