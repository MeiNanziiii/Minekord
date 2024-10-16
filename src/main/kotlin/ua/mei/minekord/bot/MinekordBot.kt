package ua.mei.minekord.bot

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TextChannel
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
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import ua.mei.minekord.bot.extensions.IPCheckExtension
import ua.mei.minekord.bot.extensions.MessageExtension
import ua.mei.minekord.bot.extensions.PlayerListExtension
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.CommandsSpec
import ua.mei.minekord.config.spec.ExperimentalSpec
import ua.mei.minekord.event.AdvancementGrantEvent
import ua.mei.minekord.event.player.MinekordAdvancementGrantEvent
import ua.mei.minekord.event.player.MinekordPlayerDeathEvent
import ua.mei.minekord.event.player.MinekordPlayerJoinEvent
import ua.mei.minekord.event.player.MinekordPlayerLeaveEvent
import ua.mei.minekord.event.player.MinekordPlayerMessageEvent
import ua.mei.minekord.event.server.MinekordServerStartedEvent
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    internal lateinit var bot: ExtensibleBot

    val extensions: MutableList<() -> Extension> = mutableListOf()

    lateinit var guild: Guild
    lateinit var channel: TextChannel
    lateinit var webhook: Webhook

    fun launchBot(server: MinecraftServer) {
        runBlocking {
            bot = ExtensibleBot(config[BotSpec.token]) {
                applicationCommands {
                    enabled = true
                }

                extensions {
                    if (config[CommandsSpec.PlayerListSpec.enabled])
                        add(::PlayerListExtension)

                    if (config[ExperimentalSpec.DiscordSpec.loginByIp])
                        add(::IPCheckExtension)

                    MinekordBot.extensions.forEach { add(it) }
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
            setupVariables()
            setupEvents()

            bot.addExtension(::MessageExtension)
        }
        launch {
            bot.start()
        }
    }

    fun setupEvents() {
        AdvancementGrantEvent.EVENT.register { player, advancement ->
            launch { bot.send(MinekordAdvancementGrantEvent(player, advancement)) }
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            if (entity is ServerPlayerEntity) {
                launch { bot.send(MinekordPlayerDeathEvent(entity, source)) }
            }
            true
        }
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            launch { bot.send(MinekordPlayerJoinEvent(handler.player)) }
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            launch { bot.send(MinekordPlayerLeaveEvent(handler.player)) }
        }
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            launch { bot.send(MinekordPlayerMessageEvent(sender, message.content)) }
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            launch { bot.send(MinekordServerStartedEvent(server)) }
        }
    }

    suspend fun setupVariables() {
        guild = bot.kordRef.getGuild(Snowflake(config[BotSpec.guild]))
        channel = guild.getChannel(Snowflake(config[BotSpec.channel])) as TextChannel
        webhook = ensureWebhook(channel as TopGuildMessageChannel, config[ChatSpec.WebhookSpec.webhookName])
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}
