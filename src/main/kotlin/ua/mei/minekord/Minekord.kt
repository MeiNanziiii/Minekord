package ua.mei.minekord

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.CONFIG_PATH
import ua.mei.minekord.config.config
import java.nio.file.Files

object Minekord : ModInitializer {
    const val MOD_ID: String = "minekord"

    val logger: Logger = LogManager.getLogger("Minekord")

    override fun onInitialize() {
        logger.info("Initializing Minekord")

        val loader: FabricLoader = FabricLoader.getInstance()

        if (!Files.exists(loader.configDir.resolve(CONFIG_PATH))) {
            Files.copy(
                loader.getModContainer(MOD_ID).get().findPath(CONFIG_PATH).get(),
                loader.configDir.resolve(CONFIG_PATH)
            )
        }

        config.validateRequired()

        ServerLifecycleEvents.SERVER_STARTING.register(MinekordBot)
    }
}
