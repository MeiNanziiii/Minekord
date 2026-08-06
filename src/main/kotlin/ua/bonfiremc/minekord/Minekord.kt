package ua.bonfiremc.minekord

import com.mojang.logging.LogUtils
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import dev.kordex.core.ExtensibleBot
import dev.kordex.data.api.DataCollection
import kotlinx.coroutines.runBlocking
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.Commands
import net.minecraft.server.MinecraftServer
import net.minecraft.server.permissions.Permissions
import org.slf4j.Logger
import ua.bonfiremc.minekord.bot.MessagesExtension
import ua.bonfiremc.minekord.config.BotSpec
import ua.bonfiremc.minekord.config.DiscordSpec
import ua.bonfiremc.minekord.config.MinecraftSpec
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.notExists

object Minekord : ModInitializer, ServerLifecycleEvents.ServerStarting {
    const val MOD_ID: String = "minekord"
    const val CONFIG_NAME: String = "$MOD_ID.config.toml"

    val logger: Logger = LogUtils.getLogger()

    lateinit var config: Config
        private set

    private lateinit var bot: ExtensibleBot

    fun loadConfig() {
        val loader: FabricLoader = FabricLoader.getInstance()
        val configFile: Path = loader.configDir.resolve(CONFIG_NAME)

        if (configFile.notExists()) {
            loader.getModContainer(MOD_ID).get()
                .findPath(CONFIG_NAME).get()
                .copyTo(configFile)
        }

        config = Config {
            addSpec(BotSpec)
            addSpec(DiscordSpec)
            addSpec(MinecraftSpec)
        }.from.toml.file(configFile.toFile()).validateRequired()
    }

    override fun onInitialize() {
        loadConfig()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("minekord")
                    .requires { it.permissions().hasPermission(Permissions.COMMANDS_ADMIN) }
                    .then(Commands.literal("reload").executes { loadConfig(); 1 })
            )
        }

        ServerLifecycleEvents.SERVER_STARTING.register(this)
    }

    override fun onServerStarting(server: MinecraftServer) {
        if (config[BotSpec.token].isBlank()) {
            logger.warn("Config field \"token\" is empty, change it in \"$CONFIG_NAME\" to start bot!")
        } else {
            runBlocking {
                bot = ExtensibleBot(config[BotSpec.token]) {
                    applicationCommands {
                        enabled = false
                    }

                    dataCollectionMode = DataCollection.None

                    extensions {
                        add(::MessagesExtension)
                    }
                }
            }

            bot.startAsync()
        }
    }
}