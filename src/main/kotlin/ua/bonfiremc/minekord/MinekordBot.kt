package ua.bonfiremc.minekord

import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.loadModule
import dev.kordex.data.api.DataCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.server.MinecraftServer
import ua.bonfiremc.minekord.Minekord.config
import ua.bonfiremc.minekord.config.BotSpec
import ua.bonfiremc.minekord.messages.MessagesExtension
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    var started: Boolean = false
        private set

    lateinit var instance: ExtensibleBot
        private set

    fun start(server: MinecraftServer) {
        if (started) return

        started = true

        runBlocking {
            instance = ExtensibleBot(config[BotSpec.token]) {
                applicationCommands {
                    enabled = false
                }

                dataCollectionMode = DataCollection.None

                extensions {
                    add(::MessagesExtension)
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
            instance.start()
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}