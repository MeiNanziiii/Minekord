package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.embed
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import net.minecraft.server.MinecraftServer
import org.koin.core.component.inject
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.CommandsSpec
import ua.mei.minekord.utils.parse
import kotlin.getValue

class PlayerListExtension : Extension() {
    override val name: String = "Player List Extension"

    private val server: MinecraftServer by inject()

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = config[CommandsSpec.PlayerListSpec.name]
            description = config[CommandsSpec.PlayerListSpec.description]

            guild(Snowflake(config[BotSpec.guild]))

            action {
                respond {
                    embed {
                        title = parse(config[CommandsSpec.PlayerListSpec.title], server).string
                        description = server.playerManager.playerList.map {
                            parse(config[CommandsSpec.PlayerListSpec.format], it).string
                        }.joinToString("\n")
                    }
                }
            }
        }
    }
}
