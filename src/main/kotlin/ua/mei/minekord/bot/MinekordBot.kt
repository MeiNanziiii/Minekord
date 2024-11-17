package ua.mei.minekord.bot

import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.ensureWebhook
import dev.kordex.core.utils.loadModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.Minekord
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.event.AdvancementGrantEvent
import ua.mei.minekord.event.ChatMessageEvent
import ua.mei.minekord.utils.MinekordActivityType
import ua.mei.minekord.utils.toText
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

    val mentions: AllowedMentionsBuilder = AllowedMentionsBuilder()

    override fun onServerStarting(server: MinecraftServer) {
        runBlocking {
            bot = ExtensibleBot(MinekordConfig.token) {
                applicationCommands {
                    enabled = true
                }
                intents {
                    +Intents.NON_PRIVILEGED
                    +Intent.GuildMembers
                }
                members {
                    fill(MinekordConfig.guild)
                }
                hooks {
                    afterKoinSetup {
                        loadModule {
                            single { server }
                        }
                    }
                }
            }
            guild = bot.kordRef.getGuild(Snowflake(MinekordConfig.guild))
            channel = guild.getChannel(Snowflake(MinekordConfig.channel)) as TopGuildMessageChannel
            webhook = ensureWebhook(channel, MinekordConfig.webhookName)

            mentions.add(AllowedMentionType.UserMentions)
            mentions.roles.addAll(guild.roles.filter { it.mentionable }.map { it.id }.toList())

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

        ChatMessageEvent.EVENT.register { message, sender ->
            minekordExtensions.forEach {
                launch {
                    it.onChatMessage(message, sender)
                }
            }
        }

        AdvancementGrantEvent.EVENT.register { player, advancement ->
            minekordExtensions.forEach {
                launch {
                    it.onAdvancementGrant(player, advancement)
                }
            }
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
            if (server.ticks % MinekordConfig.updateTicks == 0 && MinekordConfig.activityType != MinekordActivityType.NONE) {
                launch {
                    bot.kordRef.editPresence {
                        val text: String = MinekordConfig.activityText.toText(server).string

                        when (MinekordConfig.activityType) {
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
