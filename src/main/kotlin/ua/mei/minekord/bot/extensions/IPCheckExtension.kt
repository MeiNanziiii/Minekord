package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import dev.kordex.core.components.components
import dev.kordex.core.components.publicButton
import dev.kordex.core.extensions.Extension
import io.ktor.util.network.address
import kotlinx.coroutines.launch
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
                            title = "Join with new IP"
                            description = "This is your IP?\n> ${address.address}"
                        }
                        components {
                            publicButton {
                                label = "Yes"
                                style = ButtonStyle.Success

                                action {
                                    IPCache.putIntoCache(profile.id, address.address)
                                    respond {
                                        content = "ІДІ НАХУЙ"
                                    }
                                }
                            }
                            publicButton {
                                label = "No"
                                style = ButtonStyle.Danger

                                action {
                                    respond {
                                        content = "ІДІ НАХУЙ"
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