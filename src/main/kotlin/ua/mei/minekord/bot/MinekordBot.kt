package ua.mei.minekord.bot

import dev.kord.core.entity.Guild
import dev.kordex.core.ExtensibleBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.config
import ua.mei.minekord.extensions.StartupExtension
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    lateinit var bot: ExtensibleBot

    var guild: Guild? = null

    fun launchBot() {
        launch {
            bot = ExtensibleBot(config[BotSpec.token]) {
                extensions {
                    add(::StartupExtension)
                }
            }

            bot.start()
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}
