package ua.bonfiremc.minekord.messages

import dev.kord.common.Color
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.execute
import dev.kord.rest.builder.component.ContainerBuilder
import dev.kord.rest.builder.component.section
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.container
import dev.kordex.core.components.components
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementType
import net.minecraft.advancements.DisplayInfo
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.server.level.ServerPlayer
import ua.bonfiremc.minekord.Minekord
import ua.bonfiremc.minekord.MinekordBot
import ua.bonfiremc.minekord.config.DiscordSpec
import ua.bonfiremc.minekord.event.AdvancementGrantEvent
import ua.bonfiremc.minekord.util.MessageUtils
import ua.bonfiremc.minekord.util.Placeholders

class M2DMessages(val ext: MessagesExtension) {
    val mentions: AllowedMentionsBuilder = AllowedMentionsBuilder()

    suspend fun onPlayerMassage(message: PlayerChatMessage, player: ServerPlayer) {
        ext.webhook.execute(ext.webhook.token!!) {
            allowedMentions = mentions

            username = player.plainTextName
            avatarUrl = getPlayerAvatar(player)

            content = MessageUtils.message2String(message)
        }
    }

    suspend fun onPlayerJoin(player: ServerPlayer) {
        sendPlayerContainer(Minekord.config[DiscordSpec.joinMessages], Colors.green, player)
    }

    suspend fun onPlayerLeave(player: ServerPlayer) {
        sendPlayerContainer(Minekord.config[DiscordSpec.leaveMessages], Colors.red, player)
    }

    suspend fun onPlayerDeath(player: ServerPlayer) {
        sendPlayerContainer(Minekord.config[DiscordSpec.deathMessages], Colors.orange, player) {
            "death_message" to MessageUtils.component2String(player.combatTracker.deathMessage)
        }
    }

    suspend fun onAdvancementGrant(player: ServerPlayer, holder: AdvancementHolder) {
        val display: DisplayInfo = holder.value.display.get()

        val messages: List<String> = when (display.type) {
            AdvancementType.TASK -> Minekord.config[DiscordSpec.advancementMessages]
            AdvancementType.CHALLENGE -> Minekord.config[DiscordSpec.challengeMessages]
            AdvancementType.GOAL -> Minekord.config[DiscordSpec.goalMessages]
        }

        sendPlayerContainer(messages, if (display.type == AdvancementType.CHALLENGE) Colors.purple else Colors.yellow, player) {
            "advancement_name" to MessageUtils.component2String(Advancement.name(holder))
            "advancement_description" to MessageUtils.component2String(display.description)
        }
    }

    suspend fun onServerMessage(message: PlayerChatMessage) {
        ext.channel.createMessage {
            allowedMentions = mentions

            content = MessageUtils.message2String(message)
        }
    }

    suspend fun onServerStart() {
        sendContainer(Colors.green) {
            textDisplay(Minekord.config[DiscordSpec.startMessage])
        }
    }

    suspend fun onServerStop() {
        sendContainer(Colors.red) {
            textDisplay(Minekord.config[DiscordSpec.stopMessage])
        }

        ext.bot.stop()
    }

    private fun getPlayerAvatar(player: ServerPlayer): String {
        return Minekord.config[DiscordSpec.playerAvatarUrl].replace("{player}", player.plainTextName)
    }

    private suspend fun sendContainer(color: Color, builder: ContainerBuilder.() -> Unit) {
        ext.channel.createMessage {
            flags = MessageFlags(MessageFlag.IsComponentsV2)

            components {
                container {
                    accentColor = color

                    builder()
                }
            }
        }
    }

    private suspend fun sendPlayerContainer(messages: List<String>, color: Color, player: ServerPlayer, builder: Placeholders<String>.() -> Unit = {}) {
        val placeholders: Placeholders<String> = Placeholders<String>().apply {
            map["player"] = player.plainTextName
            builder()
        }

        sendContainer(color) {
            section {
                messages.forEach { message ->
                    textDisplay(
                        placeholders.map.entries.fold(message) { result, (key, value) ->
                            result.replace("{$key}", value)
                        }
                    )
                }

                thumbnailAccessory {
                    url = getPlayerAvatar(player)
                }
            }
        }
    }

    fun registerMinecraftEvents() {
        // player events
        ServerMessageEvents.CHAT_MESSAGE.register { message, player, _ ->
            MinekordBot.launch {
                onPlayerMassage(message, player)
            }
        }

        ServerPlayerEvents.JOIN.register { player ->
            MinekordBot.launch {
                onPlayerJoin(player)
            }
        }
        ServerPlayerEvents.LEAVE.register { player ->
            MinekordBot.launch {
                onPlayerLeave(player)
            }
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, _, _ ->
            if (entity is ServerPlayer) {
                MinekordBot.launch {
                    onPlayerDeath(entity)
                }
            }
            true
        }

        AdvancementGrantEvent.EVENT.register { player, holder ->
            MinekordBot.launch {
                onAdvancementGrant(player, holder)
            }
        }

        // server events
        ServerMessageEvents.COMMAND_MESSAGE.register { message, stack, _ ->
            MinekordBot.launch {
                if (stack.isPlayer) {
                    onPlayerMassage(message, stack.player!!)
                } else {
                    onServerMessage(message)
                }
            }
        }

        ServerLifecycleEvents.SERVER_STARTED.register { _ ->
            MinekordBot.launch {
                onServerStart()
            }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { _ ->
            runBlocking {
                onServerStop()
            }
        }
    }

    object Colors {
        val red: Color = Color(0xE74C3C)
        val orange: Color = Color(0xE67E22)
        val yellow: Color = Color(0xF1C40F)
        val green: Color = Color(0x2ECC71)
        val purple: Color = Color(0x9B59B6)
    }
}