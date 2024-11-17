package ua.mei.minekord.bot.extension

import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.embed
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.toKey
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.config.MinekordConfig.Bot
import ua.mei.minekord.config.MinekordConfig.Colors
import ua.mei.minekord.config.MinekordConfig.Commands
import ua.mei.minekord.utils.toText

class PlayerListExtension : MinekordExtension() {
    override val name: String = "minekord.playerlist"

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = Commands.PlayerList.name.toKey()
            description = Commands.PlayerList.description.toKey()

            guild(Snowflake(Bot.guild))

            action {
                respond {
                    embed {
                        title = Commands.PlayerList.title.toText(server).string
                        color = Colors.green
                        description = server.playerManager.playerList.map {
                            Commands.PlayerList.format.toText(it).string
                        }.joinToString("\n")
                    }
                }
            }
        }
    }
}
