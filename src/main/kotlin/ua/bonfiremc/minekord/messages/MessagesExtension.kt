package ua.bonfiremc.minekord.messages

import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.ensureWebhook
import net.minecraft.server.MinecraftServer
import org.koin.core.component.inject
import ua.bonfiremc.minekord.Minekord
import ua.bonfiremc.minekord.MinekordReloadable
import ua.bonfiremc.minekord.config.BotSpec

class MessagesExtension : Extension(), MinekordReloadable {
    override val name: String = "minekord:messages_extension"

    private val d2m: D2MMessages = D2MMessages(this)
    private val m2d: M2DMessages = M2DMessages(this)

    lateinit var channel: TextChannel
    lateinit var webhook: Webhook

    val server: MinecraftServer by inject()

    override suspend fun setup() {
        setupVariables()

        d2m.registerDiscordEvents()
        m2d.registerMinecraftEvents()
    }

    override suspend fun onMinekordReload() {
        setupVariables()
    }

    private suspend fun setupVariables() {
        val channel: Channel = bot.kordRef.getChannel(Minekord.config[BotSpec.channel]) ?: return

        if (channel is TextChannel) {
            this.channel = channel

            webhook = channel.ensureWebhook("Minekord")
        }
    }
}