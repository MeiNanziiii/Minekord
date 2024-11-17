package ua.mei.minekord.bot.extension

import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.embed
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.toKey
import ua.mei.minekord.bot.MinekordExtension
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.utils.toText

class PlayerListExtension : MinekordExtension() {
    override val name: String = "minekord.playerlist"

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = MinekordConfig.name.toKey()
            description = MinekordConfig.description.toKey()

            guild(Snowflake(MinekordConfig.guild))

            action {
                respond {
                    embed {
                        title = MinekordConfig.title.toText(server).string
                        color = MinekordConfig.green
                        description = server.playerManager.playerList.map {
                            MinekordConfig.format.toText(it).string
                        }.joinToString("\n")
                    }
                }
            }
        }
    }
}
