package ua.mei.minekord.extensions

import dev.kord.common.entity.Snowflake
import dev.kordex.core.extensions.Extension
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.config

class StartupExtension : Extension() {
    override val name: String = "Startup"

    override suspend fun setup() {
        MinekordBot.guild = kord.getGuildOrNull(Snowflake(config[BotSpec.guild]))
    }
}