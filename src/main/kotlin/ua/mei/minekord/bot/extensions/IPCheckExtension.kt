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
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.cache.IPCache
import ua.mei.minekord.config.ExperimentalSpec
import ua.mei.minekord.config.config
import ua.mei.minekord.event.IPCheckEvent
import ua.mei.minekord.utils.ExperimentalUtils

class IPCheckExtension : Extension() {
    override val name: String = "IP Check Extension"

    override suspend fun setup() {
        IPCheckEvent.event.register { socketAddress, profile ->
            MinekordBot.launch {
                try {
                    if (!config[ExperimentalSpec.DiscordSpec.enabled]) return@launch

                    val discordId: ULong = ExperimentalUtils.uuidToDiscord(profile.id)
                    val user: User? = kord.getUser(Snowflake(discordId))

                    user?.getDmChannelOrNull()?.createMessage {
                        embed {
                            title = "This is your IP?"

                            field {
                                name = "> IP"
                                value = "> ${socketAddress.address}"
                                inline = true
                            }
                            field {
                                name = "> Time"
                                value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
                                inline = true
                            }
                        }
                        components {
                            publicButton {
                                label = "Yes"
                                style = ButtonStyle.Success

                                action {
                                    IPCache.putIntoCache(profile.name, socketAddress.address)

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
                                    IPCache.blockedIps += socketAddress.address

                                    edit {
                                        embed {
                                            title = "IP was blocked!"

                                            field {
                                                name = "> IP"
                                                value = "> ${socketAddress.address}"
                                                inline = true
                                            }
                                            field {
                                                name = "> Time"
                                                value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
                                                inline = true
                                            }
                                        }
                                        components {
                                            publicButton {
                                                label = "Unblock"
                                                style = ButtonStyle.Danger

                                                action {
                                                    IPCache.blockedIps.removeAll { it == socketAddress.address }

                                                    edit {
                                                        embed {
                                                            title = "IP was unblocked!"

                                                            field {
                                                                name = "> IP"
                                                                value = "> ${socketAddress.address}"
                                                                inline = true
                                                            }
                                                            field {
                                                                name = "> Time"
                                                                value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
                                                                inline = true
                                                            }
                                                        }
                                                        components {
                                                            disabledButton {
                                                                label = "Unblock"
                                                                style = ButtonStyle.Danger
                                                            }
                                                        }
                                                    }
                                                }
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