package ua.mei.minekord.bot

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TextChannel
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.extensions.Extension
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
import ua.mei.minekord.bot.extensions.IPCheckExtension
import ua.mei.minekord.bot.extensions.MessageExtension
import ua.mei.minekord.bot.extensions.PlayerListExtension
import ua.mei.minekord.bot.extensions.SetupExtension
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.CommandsSpec
import ua.mei.minekord.config.spec.ExperimentalSpec
import ua.mei.minekord.event.AdvancementGrantEvent
import ua.mei.minekord.event.minekord.MinekordAdvancementGrantEvent
import ua.mei.minekord.event.minekord.MinekordPlayerDeathEvent
import ua.mei.minekord.event.minekord.MinekordPlayerJoinEvent
import ua.mei.minekord.event.minekord.MinekordPlayerMessageEvent
import ua.mei.minekord.event.minekord.MinekordServerEndTickEvent
import ua.mei.minekord.event.minekord.MinekordServerStartedEvent
import ua.mei.minekord.event.minekord.MinekordServerStoppedEvent
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    private lateinit var bot: ExtensibleBot

    val extensions: MutableList<() -> Extension> = mutableListOf()

    lateinit var guild: Guild
    lateinit var channel: TextChannel
    lateinit var webhook: Webhook

    fun launchBot(server: MinecraftServer) {
        launch {
            bot = ExtensibleBot(config[BotSpec.token]) {
                applicationCommands {
                    enabled = true
                }

                extensions {
                    add(::SetupExtension)
                    add(::MessageExtension)

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

            bot.start()
        }

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
            if (server.isStopping) {
                runBlocking { bot.send(MinekordPlayerJoinEvent(handler.player)) }
            } else {
                launch { bot.send(MinekordPlayerJoinEvent(handler.player)) }
            }
        }
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            launch { bot.send(MinekordPlayerMessageEvent(sender, message.content)) }
        }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            launch { bot.send(MinekordServerEndTickEvent(server)) }
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            launch { bot.send(MinekordServerStartedEvent(server)) }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            runBlocking { bot.send(MinekordServerStoppedEvent(server)) }
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}
