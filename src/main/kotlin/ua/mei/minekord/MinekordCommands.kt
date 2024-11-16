package ua.mei.minekord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import ua.mei.minekord.config.MinekordConfig

object MinekordCommands : CommandRegistrationCallback {
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>, access: CommandRegistryAccess, environment: CommandManager.RegistrationEnvironment) {
        dispatcher.register(
            literal<ServerCommandSource>("minekord")
                .requires { source -> source.hasPermissionLevel(4) }
                .then(
                    literal<ServerCommandSource>("reload")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .executes { context ->
                            MinekordConfig.load()

                            context.source.sendFeedback({ Text.literal("Config reloaded!") }, false)

                            return@executes 1
                        }
                )
        )
    }
}