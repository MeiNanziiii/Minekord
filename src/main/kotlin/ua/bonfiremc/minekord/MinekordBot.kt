package ua.bonfiremc.minekord

import dev.kordex.core.ExtensibleBot
import dev.kordex.data.api.DataCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ua.bonfiremc.minekord.Minekord.config
import ua.bonfiremc.minekord.bot.MessagesExtension
import ua.bonfiremc.minekord.config.BotSpec
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    var started: Boolean = false
        private set

    lateinit var instance: ExtensibleBot
        private set

    fun start() {
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
            }
        }

        launch {
            instance.start()
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}