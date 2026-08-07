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
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kord.rest.builder.message.container
import dev.kordex.core.checks.inChannel
import dev.kordex.core.components.components
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.ensureWebhook
import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.node.DynamicTextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TagLikeParser
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
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import ua.bonfiremc.minekord.Minekord
import ua.bonfiremc.minekord.config.BotSpec
import ua.bonfiremc.minekord.config.DiscordSpec
import ua.bonfiremc.minekord.config.MinecraftSpec
import ua.bonfiremc.minekord.event.AdvancementGrantEvent
import ua.bonfiremc.minekord.util.MessageUtils
import kotlin.coroutines.CoroutineContext
import java.util.function.Function as JavaFunction

class MessagesExtension : Extension(), ServerLifecycleEvents.ServerStarted, ServerLifecycleEvents.ServerStopped, ServerPlayerEvents.Join, ServerPlayerEvents.Leave, ServerMessageEvents.ChatMessage, ServerLivingEntityEvents.AllowDeath, AdvancementGrantEvent, CoroutineScope {
    override val name: String = "minekord:messages_extension"

    private lateinit var channel: TextChannel
    private lateinit var webhook: Webhook
    private lateinit var server: MinecraftServer

    private val dynamicKey: ParserContext.Key<JavaFunction<String, Component?>> = DynamicTextNode.key(Minekord.MOD_ID)
    private val parser: NodeParser = NodeParser.builder()
        .simplifiedTextFormat()
        .quickText()
        .commonPlaceholders()
        .placeholders(TagLikeParser.PLACEHOLDER_ALTERNATIVE, dynamicKey)
        .markdown()
        .build()

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

            webhook = channel.ensureWebhook("Minekord")

            event<MessageCreateEvent> {
                check { failIf { event.message.author?.id == kord.selfId || event.message.webhookId != null } }
                check { inChannel(Minekord.config[BotSpec.channel]) }

                action {
                    if (!this@MessagesExtension::server.isInitialized) return@action

                    val message: Message = event.message
                    val sender: Member = event.member ?: return@action

                    if (message.content.isBlank() && !message.attachments.any { it.isImage }) return@action

                    val text: MutableComponent = Component.empty()

                    if (message.referencedMessage != null) {
                        val args: Map<String, Component> = mapOf(
                            "sender" to Component.literal(sender.effectiveName),
                            "message" to parser.parseComponent(
                                message.referencedMessage!!.content,
                                ParserContext.of()
                            ),
                            "summary" to parser.parseComponent(
                                summary(message.referencedMessage!!.content),
                                ParserContext.of()
                            )
                        )

                        text.append(
                            parser.parseComponent(
                                Minekord.config[MinecraftSpec.replyFormat],
                                ParserContext.of().with(dynamicKey, JavaFunction { args[it] })
                            )
                        ).append("\n")
                    }

                    val args: Map<String, Component> = mapOf(
                        "sender" to Component.literal(sender.effectiveName),
                        "message" to parser.parseComponent(
                            MessageUtils.getFormattedContent(message),
                            ParserContext.of()
                        )
                    )

                    text.append(
                        parser.parseComponent(
                            Minekord.config[MinecraftSpec.messageFormat],
                            ParserContext.of().with(dynamicKey, JavaFunction { args[it] })
                        )
                    )

                    val attachments: List<Component> = MessageUtils.getAttachmentsAsText(message)

                    server.playerList.broadcastSystemMessage(text, false)

                    attachments.forEach {
                        server.playerList.broadcastSystemMessage(it, false)
                    }
                }
            }
        }
    }

    override fun onChatMessage(message: PlayerChatMessage, sender: ServerPlayer, boundChatType: ChatType.Bound) {
        launch {
            webhook.execute(webhook.token!!) {
                allowedMentions = AllowedMentionsBuilder()

                username = sender.plainTextName
                avatarUrl = replacePlaceholders(Minekord.config[DiscordSpec.playerAvatarUrl], mapOf("player" to sender.plainTextName))

                content = message.signedContent()
            }
        }
    }

    override fun onJoin(player: ServerPlayer) {
        playerContainer(
            Minekord.config[DiscordSpec.joinMessages],
            mapOf("player" to player.plainTextName),
            Color(0x2ECC71)
        )
    }

    override fun onLeave(player: ServerPlayer) {
        playerContainer(
            Minekord.config[DiscordSpec.leaveMessages],
            mapOf("player" to player.plainTextName),
            Color(0xE74C3C)
        )
    }

    override fun allowDeath(entity: LivingEntity, damageSource: DamageSource, damageAmount: Float): Boolean {
        if (entity is ServerPlayer) {
            playerContainer(
                Minekord.config[DiscordSpec.deathMessages],
                mapOf(
                    "player" to entity.plainTextName,
                    "death_message" to entity.combatTracker.deathMessage.string
                ),
                Color(0xF1C40F)
            )
        }

        return true
    }

    override fun onServerStarted(server: MinecraftServer) {
        this.server = server

        launch {
            channel.createMessage {
                flags = MessageFlags(MessageFlag.IsComponentsV2)

                components {
                    container {
                        accentColor = Color(0x2ECC71)

                        Minekord.config[DiscordSpec.startMessages].forEach(::textDisplay)
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

                        Minekord.config[DiscordSpec.stopMessages].forEach(::textDisplay)
                    }
                }
            }

            bot.stop()
        }
    }

    override fun onAdvancementGrant(player: ServerPlayer, holder: AdvancementHolder) {
        val type: AdvancementType = holder.value.display.get().type

        val texts: List<String> = when (type) {
            AdvancementType.TASK -> Minekord.config[DiscordSpec.advancementMessages]
            AdvancementType.CHALLENGE -> Minekord.config[DiscordSpec.challengeMessages]
            AdvancementType.GOAL -> Minekord.config[DiscordSpec.goalMessages]
        }

        playerContainer(
            texts,
            mapOf(
                "player" to player.plainTextName,
                "advancement_name" to Advancement.name(holder).string,
                "advancement_description" to holder.value.display.get().description.string
            ),
            Color(if (type == AdvancementType.CHALLENGE) 0xAA00AA else 0x55FF55)
        )
    }

    private fun summary(text: String): String {
        val length: Int = Minekord.config[MinecraftSpec.summaryMaxLength]

        return if (text.replace("\n", " ").length <= length) {
            text.replace("\n", " ").trim()
        } else {
            text.replace("\n", " ").take(length).trim() + "…"
        }
    }

    private fun playerContainer(texts: List<String>, args: Map<String, String>, color: Color) {
        launch {
            channel.createMessage {
                flags = MessageFlags(MessageFlag.IsComponentsV2)

                components {
                    container {
                        accentColor = color

                        section {
                            texts.forEach {
                                textDisplay(replacePlaceholders(it, args))
                            }

                            thumbnailAccessory {
                                url = replacePlaceholders(Minekord.config[DiscordSpec.playerAvatarUrl], args)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun replacePlaceholders(text: String, args: Map<String, String>): String {
        var result: String = text

        args.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }

        return result
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default
}