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
import dev.kordex.core.i18n.toKey
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import io.ktor.util.network.address
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.cache.IPCache
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.ExperimentalSpec
import ua.mei.minekord.config.spec.MessagesSpec
import ua.mei.minekord.event.IPCheckEvent
import ua.mei.minekord.utils.ExperimentalUtils

class IPCheckExtension : Extension() {
    override val name: String = "IP Check Extension"

    override suspend fun setup() {
        IPCheckEvent.EVENT.register { socketAddress, profile ->
            MinekordBot.launch {
                try {
                    if (!config[ExperimentalSpec.DiscordSpec.enabled]) return@launch

                    val discordId: ULong = ExperimentalUtils.uuidToDiscord(profile.id)
                    val user: User? = kord.getUser(Snowflake(discordId))

                    user?.getDmChannelOrNull()?.createMessage {
                        embed {
                            title = config[MessagesSpec.embedTitle]

                            field {
                                name = "> IP"
                                value = "> ${socketAddress.address}"
                                inline = true
                            }
                            field {
                                name = "> ${config[MessagesSpec.timeLabel]}"
                                value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
                                inline = true
                            }
                        }
                        components {
                            publicButton {
                                label = config[MessagesSpec.yesButton].toKey()
                                style = ButtonStyle.Success

                                action {
                                    IPCache.putIntoCache(profile.name, socketAddress.address)

                                    edit {
                                        components {
                                            disabledButton {
                                                label = config[MessagesSpec.yesButton].toKey()
                                                style = ButtonStyle.Success
                                            }
                                        }
                                    }
                                }
                            }
                            publicButton {
                                label = config[MessagesSpec.noButton].toKey()
                                style = ButtonStyle.Danger

                                action {
                                    IPCache.blockedIps += socketAddress.address

                                    edit {
                                        embed {
                                            title = config[MessagesSpec.ipBlockedTitle]

                                            field {
                                                name = "> IP"
                                                value = "> ${socketAddress.address}"
                                                inline = true
                                            }
                                            field {
                                                name = "> ${config[MessagesSpec.timeLabel]}"
                                                value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
                                                inline = true
                                            }
                                        }
                                        components {
                                            publicButton {
                                                label = config[MessagesSpec.unblockButton].toKey()
                                                style = ButtonStyle.Danger

                                                action {
                                                    IPCache.blockedIps.removeAll { it == socketAddress.address }
                                                    IPCache.alreadyRequestedIps[profile.name]?.removeAll { it == socketAddress.address }

                                                    edit {
                                                        embed {
                                                            title = config[MessagesSpec.ipUnblockedTitle]

                                                            field {
                                                                name = "> IP"
                                                                value = "> ${socketAddress.address}"
                                                                inline = true
                                                            }
                                                            field {
                                                                name = "> ${config[MessagesSpec.timeLabel]}"
                                                                value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
                                                                inline = true
                                                            }
                                                        }
                                                        components {
                                                            disabledButton {
                                                                label = config[MessagesSpec.unblockButton].toKey()
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
