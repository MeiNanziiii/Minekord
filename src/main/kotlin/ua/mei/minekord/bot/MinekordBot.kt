package ua.mei.minekord.bot

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.TextChannel
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.loadModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.server.MinecraftServer
import ua.mei.minekord.bot.extensions.IPCheckExtension
import ua.mei.minekord.bot.extensions.MessageExtension
import ua.mei.minekord.bot.extensions.SetupExtension
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.ExperimentalSpec
import ua.mei.minekord.config.config
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    lateinit var bot: ExtensibleBot

    lateinit var guild: Guild
    lateinit var chat: TextChannel
    lateinit var webhook: Webhook

    fun launchBot(server: MinecraftServer) {
        launch {
            println("не ліх")

            bot = ExtensibleBot(config[BotSpec.token]) {
                extensions {
                    add(::SetupExtension)
                    add(::MessageExtension)

                    if (config[ExperimentalSpec.DiscordSpec.enabled])
                        add(::IPCheckExtension)
                }

                hooks {
                    afterKoinSetup {
                        loadModule {
                            single { server }
                        }
                    }
                }
            }

            try {
                bot.start()
            } catch (_: Throwable) {
                println("ліх чи шо?")
            }
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}
