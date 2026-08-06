package ua.bonfiremc.minekord.bot

import dev.kord.common.Color
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.execute
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.Webhook
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.component.section
import dev.kord.rest.builder.message.container
import dev.kordex.core.checks.inChannel
import dev.kordex.core.components.components
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.ensureWebhook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementType
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import ua.bonfiremc.minekord.Minekord
import ua.bonfiremc.minekord.config.BotSpec
import ua.bonfiremc.minekord.event.AdvancementGrantEvent
import kotlin.coroutines.CoroutineContext

class MessagesExtension : Extension(), ServerLifecycleEvents.ServerStarted, ServerLifecycleEvents.ServerStopped, ServerPlayerEvents.Join, ServerPlayerEvents.Leave, ServerMessageEvents.ChatMessage, ServerLivingEntityEvents.AllowDeath, AdvancementGrantEvent, CoroutineScope {
    override val name: String = "minekord:messages_extension"

    private lateinit var channel: TextChannel
    private lateinit var webhook: Webhook
    private lateinit var server: MinecraftServer

    override suspend fun setup() {
        val channel: Channel = bot.kordRef.getChannel(Minekord.config[BotSpec.channel]) ?: return

        if (channel is TextChannel) {
            this.channel = channel

            ServerLifecycleEvents.SERVER_STARTED.register(this)
            ServerLifecycleEvents.SERVER_STOPPED.register(this)

            ServerPlayerEvents.JOIN.register(this)
            ServerPlayerEvents.LEAVE.register(this)

            ServerLivingEntityEvents.ALLOW_DEATH.register(this)
            AdvancementGrantEvent.EVENT.register(this)

            ServerMessageEvents.CHAT_MESSAGE.register(this)

            webhook = channel.ensureWebhook("test")

            event<MessageCreateEvent> {
                check { failIf { event.message.author?.id == kord.selfId || event.message.webhookId != null } }
                check { inChannel(Minekord.config[BotSpec.channel]) }

                action {
                    if (this@MessagesExtension::server.isInitialized) {
                        val message: Message = event.message
                        val sender: Member = event.member ?: return@action

                        server.playerList.broadcastSystemMessage(Component.literal("${sender.effectiveName}: ${message.content}"), false)
                    }
                }
            }
        }
    }

    override fun onServerStarted(server: MinecraftServer) {
        this.server = server

        launch {
            channel.createMessage {
                flags = MessageFlags(MessageFlag.IsComponentsV2)

                components {
                    container {
                        accentColor = Color(0x2ECC71)

                        textDisplay("### :white_check_mark:   Сервер запущено")
                    }
                }
            }
        }
    }

    override fun onChatMessage(message: PlayerChatMessage, sender: ServerPlayer, boundChatType: ChatType.Bound) {
        launch {
            webhook.execute(webhook.token!!) {
                username = sender.plainTextName
                avatarUrl = "https://cravatar.eu/helmavatar/${sender.plainTextName}/256"

                content = message.signedContent()
            }
        }
    }

    override fun onJoin(player: ServerPlayer) {
        launch {
            channel.createMessage {
                flags = MessageFlags(MessageFlag.IsComponentsV2)

                components {
                    container {
                        accentColor = Color(0x2ECC71)

                        section {
                            textDisplay("### :wave:   ${player.plainTextName} приєднався до гри")
                            textDisplay("Бажаємо гарно провести час на сервері!")

                            thumbnailAccessory {
                                url = "https://cravatar.eu/helmavatar/${player.plainTextName}/256"
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAdvancementGrant(player: ServerPlayer, holder: AdvancementHolder) {
        launch {
            channel.createMessage {
                flags = MessageFlags(MessageFlag.IsComponentsV2)

                components {
                    container {
                        accentColor = Color(if (holder.value.display.get().type == AdvancementType.CHALLENGE) 0xAA00AA else 0x55FF55)

                        val text: String = when (holder.value.display.get().type) {
                            AdvancementType.TASK -> "отримав досягнення"
                            AdvancementType.CHALLENGE -> "виконав випробування"
                            AdvancementType.GOAL -> "досяг цілі"
                        }

                        section {
                            textDisplay("### :sparkles:   ${player.plainTextName} $text ${Advancement.name(holder).string}")
                            textDisplay(holder.value.display.get().description.string)

                            thumbnailAccessory {
                                url = "https://cravatar.eu/helmavatar/${player.plainTextName}/256"
                            }
                        }
                    }
                }
            }
        }
    }

    override fun allowDeath(entity: LivingEntity, damageSource: DamageSource, damageAmount: Float): Boolean {
        if (entity is ServerPlayer) {
            launch {
                channel.createMessage {
                    flags = MessageFlags(MessageFlag.IsComponentsV2)

                    components {
                        container {
                            accentColor = Color(0xF1C40F)

                            section {
                                textDisplay("### :skull_crossbones:   ${entity.combatTracker.deathMessage.string}")
                                textDisplay("Буває")

                                thumbnailAccessory {
                                    url = "https://cravatar.eu/helmavatar/${entity.plainTextName}/256"
                                }
                            }
                        }
                    }
                }
            }
        }

        return true
    }

    override fun onLeave(player: ServerPlayer) {
        launch {
            channel.createMessage {
                flags = MessageFlags(MessageFlag.IsComponentsV2)

                components {
                    container {
                        accentColor = Color(0xE74C3C)

                        section {
                            textDisplay("### :door:   ${player.plainTextName} покинув гру")
                            textDisplay("Сподіваємося, що Ви ще повернетеся!")

                            thumbnailAccessory {
                                url = "https://cravatar.eu/helmavatar/${player.plainTextName}/256"
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onServerStopped(server: MinecraftServer) {
        runBlocking {
            channel.createMessage {
                flags = MessageFlags(MessageFlag.IsComponentsV2)

                components {
                    container {
                        accentColor = Color(0xE74C3C)

                        textDisplay("### :octagonal_sign:   Сервер зупинено")
                    }
                }
            }

            bot.stop()
        }
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}