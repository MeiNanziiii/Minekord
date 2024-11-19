package ua.mei.minekord

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.bot.extension.MessagesExtension
import ua.mei.minekord.bot.extension.PlayerListExtension
import ua.mei.minekord.cache.IPCache
import ua.mei.minekord.config.MinekordConfig
import ua.mei.minekord.config.MinekordConfig.CONFIG_PATH
import ua.mei.minekord.config.MinekordConfig.Chat
import ua.mei.minekord.config.MinekordConfig.Commands
import ua.mei.minekord.event.ChatMessageEvent
import ua.mei.minekord.utils.MessageSender
import ua.mei.minekord.utils.avatarUrl
import java.nio.file.Files

object Minekord : ModInitializer {
    const val MOD_ID: String = "minekord"

    val logger: Logger = LogManager.getLogger("Minekord")

    override fun onInitialize() {
        logger.info("Initializing Minekord")

        val loader: FabricLoader = FabricLoader.getInstance()

        if (!Files.exists(loader.gameDir.resolve("minekord"))) {
            Files.createDirectories(loader.gameDir.resolve("minekord"))
        }
        if (!Files.exists(loader.configDir.resolve(CONFIG_PATH))) {
            Files.copy(
                loader.getModContainer(MOD_ID).get().findPath(CONFIG_PATH).get(),
                loader.configDir.resolve(CONFIG_PATH)
            )
        }

        MinekordConfig.load()

        MinekordBot.registerExtension(::MessagesExtension)
        if (Commands.PlayerList.enabled) {
            MinekordBot.registerExtension(::PlayerListExtension)
        }

        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            ChatMessageEvent.EVENT.invoker().message(message.content, MessageSender(sender.gameProfile.name, sender.avatarUrl))
        }
        ServerMessageEvents.COMMAND_MESSAGE.register { message, source, parameters ->
            if (source.isExecutedByPlayer) {
                ChatMessageEvent.EVENT.invoker().message(message.content, MessageSender(source.player!!.gameProfile.name, source.player!!.avatarUrl))
            } else {
                ChatMessageEvent.EVENT.invoker().message(message.content, MessageSender(Chat.Webhook.webhookName, Chat.Webhook.webhookAvatar))
            }
        }
        ServerLifecycleEvents.SERVER_STARTING.register(IPCache)
        ServerLifecycleEvents.SERVER_STOPPED.register(IPCache)
        ServerLifecycleEvents.SERVER_STARTING.register(MinekordBot)
        CommandRegistrationCallback.EVENT.register(MinekordCommands)
    }
}
