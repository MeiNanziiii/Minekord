package ua.mei.minekord.bot

import dev.kordex.core.ExtensibleBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.config
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    lateinit var bot: ExtensibleBot

    fun launchBot() {
        launch {
            bot = ExtensibleBot(config[BotSpec.token]) {
                members {
                    fill(config[BotSpec.guild])
                }
            }

            bot.start()
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}