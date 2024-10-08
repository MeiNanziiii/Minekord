package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.embed
import dev.kordex.core.components.components
import dev.kordex.core.components.disabledButton
import dev.kordex.core.components.publicButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import io.ktor.util.network.address
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ua.mei.minekord.bot.DiscordUtils
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.cache.IPCache
import ua.mei.minekord.event.IPCheckEvent

class IPCheckExtension : Extension() {
    override val name: String = "IP Check Extension"

    override suspend fun setup() {
        IPCheckEvent.event.register { address, profile ->
            MinekordBot.launch {
                try {
                    val discordId: ULong = DiscordUtils.uuidToDiscord(profile.id)
                    val user: User? = kord.getUser(Snowflake(discordId))

                    user?.getDmChannelOrNull()?.createMessage {
                        embed {
                            title = "This is your IP?"

                            field {
                                name = "> IP"
                                value = "> ${address.address}"
                            }
                            field {
                                name = "> Time"
                                value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
                            }
                        }
                        components {
                            publicButton {
                                label = "Yes"
                                style = ButtonStyle.Success

                                action {
                                    IPCache.putIntoCache(profile.name, address.address)

                                    edit {
                                        components {
                                            disabledButton {
                                                label = "Yes"
                                                style = ButtonStyle.Success
                                            }
                                        }
                                    }
                                }
                            }
                            publicButton {
                                label = "No"
                                style = ButtonStyle.Danger

                                action {
                                    edit {
                                        components {
                                            disabledButton {
                                                label = "No"
                                                style = ButtonStyle.Danger
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {

                }
            }
        }
    }
}