package ua.mei.minekord

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.cache.IPCache
import ua.mei.minekord.config.CONFIG_PATH
import ua.mei.minekord.config.ExperimentalSpec
import ua.mei.minekord.config.config
import ua.mei.minekord.event.PlayerMessageEvent
import java.nio.file.Files

object Minekord : ModInitializer {
    const val MOD_ID: String = "minekord"

    val logger: Logger = LogManager.getLogger("Minekord")

    override fun onInitialize() {
        logger.info("Initializing Minekord")

        if (!Files.exists(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH))) {
            Files.copy(
                FabricLoader.getInstance().getModContainer(MOD_ID).get().findPath(CONFIG_PATH).get(),
                FabricLoader.getInstance().configDir.resolve(CONFIG_PATH)
            )
        }

        config.validateRequired()

        if (config[ExperimentalSpec.DiscordSpec.loginByIp]) IPCache.load()

        ServerLifecycleEvents.SERVER_STARTING.register(MinekordBot::launchBot)

        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, type ->
            PlayerMessageEvent.event.invoker().message(sender.gameProfile, message.content, sender.server.registryManager)
        }
    }
}
