package ua.mei.minekord.bot

import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.TextChannel
import dev.kordex.core.ExtensibleBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ua.mei.minekord.bot.extensions.IPCheckExtension
import ua.mei.minekord.bot.extensions.SetupExtension
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.config
import kotlin.coroutines.CoroutineContext

object MinekordBot : CoroutineScope {
    lateinit var bot: ExtensibleBot

    var guild: Guild? = null
    var chat: TextChannel? = null

    fun launchBot() {
        launch {
            bot = ExtensibleBot(config[BotSpec.token]) {
                extensions {
                    add(::SetupExtension)
                    add(::IPCheckExtension)
                }
            }

            bot.start()
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}
