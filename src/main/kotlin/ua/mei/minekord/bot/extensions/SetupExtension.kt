package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.channel.TextChannel
import dev.kordex.core.extensions.Extension
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.config

class SetupExtension : Extension() {
    override val name: String = "Startup"

    override suspend fun setup() {
        MinekordBot.guild = kord.getGuildOrNull(Snowflake(config[BotSpec.guild]))
        MinekordBot.chat = MinekordBot.guild?.getChannel(Snowflake(config[BotSpec.chat])) as? TextChannel
    }
}