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
import net.minecraft.server.MinecraftServer
import ua.mei.minekord.bot.extensions.IPCheckExtension
import ua.mei.minekord.bot.extensions.MessageExtension
import ua.mei.minekord.bot.extensions.PlayerListExtension
import ua.mei.minekord.bot.extensions.SetupExtension
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.CommandsSpec
import ua.mei.minekord.config.spec.ExperimentalSpec
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    internal var bot: ExtensibleBot? = null

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
        }
        launch {
            bot!!.start()
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}
