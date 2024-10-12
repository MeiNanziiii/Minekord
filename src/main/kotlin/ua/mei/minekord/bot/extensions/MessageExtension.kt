package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.AllowedMentionType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.execute
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kordex.core.checks.inChannel
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions
import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.node.DynamicTextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TagLikeParser
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import org.koin.core.component.inject
import ua.mei.minekord.Minekord
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.BotSpec
import ua.mei.minekord.config.ChatSpec
import ua.mei.minekord.config.config
import ua.mei.minekord.event.PlayerMessageEvent
import ua.mei.minekord.utils.MinekordMinecraftRenderer
import ua.mei.minekord.utils.SerializerUtils
import ua.mei.minekord.utils.asString
import ua.mei.minekord.utils.literal
import ua.mei.minekord.utils.summary
import ua.mei.minekord.utils.toAdventure
import ua.mei.minekord.utils.toNative

import java.util.function.Function as JavaFunction

class MessageExtension : Extension() {
    override val name: String = "Message Extension"

    private val mentions: AllowedMentionsBuilder = AllowedMentionsBuilder()
    private val discordOptions: DiscordSerializerOptions = DiscordSerializerOptions.defaults()
        .withEmbedLinks(false)
        .withEscapeMarkdown(config[ChatSpec.convertMarkdown])
        .withKeybindProvider(SerializerUtils::translatableToString)
        .withTranslationProvider(SerializerUtils::translatableToString)

    private val minecraftOptions: MinecraftSerializerOptions<Component> = MinecraftSerializerOptions.defaults()
        .addRenderer(MinekordMinecraftRenderer())

    private val server: MinecraftServer by inject()
    private val dynamicKey: ParserContext.Key<JavaFunction<String, Text?>> = DynamicTextNode.key(Minekord.MOD_ID)
    private val parser: NodeParser = NodeParser.builder()
        .simplifiedTextFormat()
        .quickText()
        .globalPlaceholders()
        .placeholders(TagLikeParser.PLACEHOLDER_ALTERNATIVE, dynamicKey)
        .build()

    override suspend fun setup() {
        mentions.add(AllowedMentionType.UserMentions)
        mentions.roles.addAll(MinekordBot.guild.roles.filter { it.mentionable }.map { it.id }.toList())

        event<MessageCreateEvent> {
            check { isNotBot() }
            check { inChannel(Snowflake(config[BotSpec.channel])) }

            action {
                val message: Message = event.message
                val sender: Member = message.getAuthorAsMember()

                server.submit {

                    var content: Text = if (config[ChatSpec.convertMarkdown])
                        MinecraftSerializer.INSTANCE.serialize(message.content, minecraftOptions).toNative(server.registryManager)
                    else
                        message.content.literal()

                    if (message.referencedMessage != null) {
                        val replyComponent: Component = MinecraftSerializer.INSTANCE.serialize(message.referencedMessage!!.content, minecraftOptions)

                        val replyPlaceholders: Map<String, Text> = mapOf(
                            "sender" to sender.effectiveName.literal(),
                            "message" to replyComponent.toNative(server.registryManager),
                            "summary" to replyComponent.asString().summary().literal()
                        )
                        val reply = parser.parseText(
                            config[ChatSpec.MinecraftSpec.replyFormat],
                            ParserContext.of(
                                dynamicKey,
                                JavaFunction { replyPlaceholders[it] }
                            )
                        )

                        content = Text.empty()
                            .append(reply)
                            .append("\n")
                            .append(content)
                    }

                    val contentPlaceholders: Map<String, Text> = mapOf(
                        "sender" to sender.effectiveName.literal(),
                        "message" to content,
                    )
                    content = parser.parseText(
                        config[ChatSpec.MinecraftSpec.messageFormat],
                        ParserContext.of(
                            dynamicKey,
                            JavaFunction { contentPlaceholders[it] }
                        )
                    )

                    server.playerManager.broadcast(content, false)
                }
            }
        }

        PlayerMessageEvent.event.register { profile, message, wrapperLookup ->
            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    allowedMentions = mentions
                    username = profile.name

                    content = DiscordSerializer.INSTANCE.serialize(
                        message.toAdventure(wrapperLookup), discordOptions
                    ).let { if (config[ChatSpec.convertMentions]) SerializerUtils.convertMentions(it) else it }.takeIf { it.isNotBlank() } ?: return@launch

                    avatarUrl = config[ChatSpec.WebhookSpec.playerAvatar].let {
                        val texture: String = profile.properties.get("textures").firstOrNull()?.value ?: ""
                        it.replace("nickname", profile.name).replace("texture", texture)
                    }
                }
            }
        }
    }
}