package ua.mei.minekord.bot.extension

import com.mojang.authlib.GameProfile
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Member
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.components.ComponentContainer
import dev.kordex.core.components.components
import dev.kordex.core.components.disabledButton
import dev.kordex.core.components.publicButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.i18n.toKey
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import io.ktor.util.network.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ua.mei.minekord.Minekord
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.cache.IPCache
import ua.mei.minekord.config.MinekordConfig.Auth
import ua.mei.minekord.config.MinekordConfig.Messages
import ua.mei.minekord.event.IPCheckEvent
import ua.mei.minekord.utils.AuthUtils
import java.net.SocketAddress

class IPCheckExtension : Extension() {
    override val name: String = "minekord.ipcheck"

    override suspend fun setup() {
        IPCheckEvent.EVENT.register { socketAddress, profile ->
            if (!Auth.ipBasedLogin) return@register

            MinekordBot.launch {
                try {
                    val member: Member? = AuthUtils.findMember(profile.name)

                    member?.getDmChannelOrNull()?.createMessage {
                        embed {
                            title = Messages.embedTitle
                            addIpField(socketAddress)
                            addTimeField()
                        }
                        components {
                            addYesButton(socketAddress, profile)
                            addNoButton(socketAddress, profile)
                        }
                    }
                } catch (e: Exception) {
                    Minekord.logger.info("Error handling IP check: ${e.message}")
                }
            }
        }
    }

    private fun EmbedBuilder.addIpField(socketAddress: SocketAddress) {
        field {
            name = "> IP"
            value = "> ${socketAddress.address}"
            inline = true
        }
    }

    private fun EmbedBuilder.addTimeField() {
        field {
            name = "> ${Messages.timeLabel}"
            value = "> ${Clock.System.now().toDiscord(TimestampType.Default)}"
            inline = true
        }
    }

    private suspend fun ComponentContainer.addYesButton(socketAddress: SocketAddress, profile: GameProfile) {
        publicButton {
            label = Messages.yesButton.toKey()
            style = ButtonStyle.Success

            action {
                IPCache.ipCache[profile.name] = socketAddress.address

                edit {
                    components {
                        disabledButton {
                            label = Messages.yesButton.toKey()
                            style = ButtonStyle.Success
                        }
                    }
                }
            }
        }
    }

    private suspend fun ComponentContainer.addNoButton(socketAddress: SocketAddress, profile: GameProfile) {
        publicButton {
            label = Messages.noButton.toKey()
            style = ButtonStyle.Danger

            action {
                IPCache.blockedIps += socketAddress.address

                edit {
                    embed {
                        title = Messages.ipBlockedTitle
                        addIpField(socketAddress)
                        addTimeField()
                    }
                    components {
                        addUnblockButton(socketAddress, profile)
                    }
                }
            }
        }
    }

    private suspend fun ComponentContainer.addUnblockButton(socketAddress: SocketAddress, profile: GameProfile) {
        publicButton {
            label = Messages.unblockButton.toKey()
            style = ButtonStyle.Danger

            action {
                IPCache.blockedIps.removeAll { it == socketAddress.address }
                IPCache.alreadyRequestedIps[profile.name]?.removeAll { it == socketAddress.address }

                edit {
                    embed {
                        title = Messages.ipUnblockedTitle
                        addIpField(socketAddress)
                        addTimeField()
                    }
                    components {
                        disabledButton {
                            label = Messages.unblockButton.toKey()
                            style = ButtonStyle.Danger
                        }
                    }
                }
            }
        }
    }
}
