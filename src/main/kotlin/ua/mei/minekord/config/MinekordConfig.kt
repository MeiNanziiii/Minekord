package ua.mei.minekord.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import dev.kord.common.Color
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.node.EmptyNode
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser
import eu.pb4.placeholders.api.parsers.StaticPreParser
import eu.pb4.placeholders.api.parsers.TextParserV1
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.text.format.TextColor
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.ColorsSpec
import ua.mei.minekord.config.spec.CommandsSpec
import ua.mei.minekord.config.spec.MainSpec
import ua.mei.minekord.config.spec.PresenceSpec
import ua.mei.minekord.parser.DynamicNode
import ua.mei.minekord.utils.MinekordActivityType
import ua.mei.minekord.utils.toColor

object MinekordConfig {
    const val CONFIG_PATH: String = "minekord.toml"

    private val parser: NodeParser = NodeParser.merge(
        TextParserV1.DEFAULT,
        Placeholders.DEFAULT_PLACEHOLDER_PARSER,
        PatternPlaceholderParser(PatternPlaceholderParser.ALT_PLACEHOLDER_PATTERN_CUSTOM, DynamicNode.Companion::of),
        StaticPreParser.INSTANCE
    )
    private lateinit var config: Config

    fun load() {
        config = Config {
            addSpec(MainSpec)
            addSpec(ChatSpec)
            addSpec(PresenceSpec)
            addSpec(CommandsSpec)
            addSpec(ColorsSpec)
        }.from.toml.file(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH).toFile())

        config.validateRequired()

        Main.load()
        Chat.load()
        Presence.load()
        Commands.load()
        Colors.load()
    }

    private fun parseNode(text: String): TextNode {
        return if (text.isEmpty()) EmptyNode.INSTANCE else parser.parseNode(text)
    }

    object Main {
        lateinit var token: String
            private set
        var guild: ULong = 0u
            private set
        var channel: ULong = 0u
            private set

        internal fun load() {
            token = config[MainSpec.token]
            guild = config[MainSpec.guild]
            channel = config[MainSpec.channel]
        }
    }

    object Chat {
        var convertMentions: Boolean = true
            private set
        var convertMarkdown: Boolean = true
            private set

        fun load() {
            convertMentions = config[ChatSpec.convertMentions]
            convertMarkdown = config[ChatSpec.convertMarkdown]

            Minecraft.load()
            Discord.load()
            Webhook.load()
        }

        object Minecraft {
            lateinit var messageFormat: TextNode
                private set
            lateinit var replyFormat: TextNode
                private set
            var summaryMaxLength: Int = 20
                private set

            var coloredRoles: Boolean = true
                private set

            fun load() {
                messageFormat = parseNode(config[ChatSpec.MinecraftSpec.messageFormat])
                replyFormat = parseNode(config[ChatSpec.MinecraftSpec.replyFormat])
                summaryMaxLength = config[ChatSpec.MinecraftSpec.summaryMaxLength]

                coloredRoles = config[ChatSpec.MinecraftSpec.coloredRoles]
            }
        }

        object Discord {
            lateinit var advancementMessage: TextNode
                private set
            lateinit var goalMessage: TextNode
                private set
            lateinit var challengeMessage: TextNode
                private set

            lateinit var joinMessage: TextNode
                private set
            lateinit var leaveMessage: TextNode
                private set
            lateinit var deathMessage: TextNode
                private set

            lateinit var startMessage: TextNode
                private set
            lateinit var stopMessage: TextNode
                private set

            fun load() {
                advancementMessage = parseNode(config[ChatSpec.DiscordSpec.advancementMessage])
                goalMessage = parseNode(config[ChatSpec.DiscordSpec.goalMessage])
                challengeMessage = parseNode(config[ChatSpec.DiscordSpec.challengeMessage])

                joinMessage = parseNode(config[ChatSpec.DiscordSpec.joinMessage])
                leaveMessage = parseNode(config[ChatSpec.DiscordSpec.leaveMessage])
                deathMessage = parseNode(config[ChatSpec.DiscordSpec.deathMessage])

                startMessage = parseNode(config[ChatSpec.DiscordSpec.startMessage])
                stopMessage = parseNode(config[ChatSpec.DiscordSpec.stopMessage])
            }
        }

        object Webhook {
            lateinit var webhookName: String
                private set
            lateinit var webhookAvatar: String
                private set
            lateinit var playerAvatar: TextNode
                private set

            fun load() {
                webhookName = config[ChatSpec.WebhookSpec.webhookName]
                webhookAvatar = config[ChatSpec.WebhookSpec.webhookAvatar]
                playerAvatar = parseNode(config[ChatSpec.WebhookSpec.playerAvatar])
            }
        }
    }

    object Presence {
        lateinit var activityType: MinekordActivityType
            private set
        lateinit var activityText: TextNode
            private set
        var updateTicks: Int = 1200
            private set

        fun load() {
            activityType = config[PresenceSpec.activityType]
            activityText = parseNode(config[PresenceSpec.activityText])
            updateTicks = config[PresenceSpec.updateTicks]
        }
    }

    object Commands {
        fun load() {
            PlayerList.load()
        }

        object PlayerList {
            var enabled: Boolean = true
                private set
            lateinit var name: String
                private set
            lateinit var description: String
                private set
            lateinit var title: TextNode
                private set
            lateinit var format: TextNode
                private set

            fun load() {
                enabled = config[CommandsSpec.PlayerListSpec.enabled]
                name = config[CommandsSpec.PlayerListSpec.name]
                description = config[CommandsSpec.PlayerListSpec.description]
                title = parseNode(config[CommandsSpec.PlayerListSpec.title])
                format = parseNode(config[CommandsSpec.PlayerListSpec.format])
            }
        }
    }

    object Colors {
        lateinit var red: Color
            private set
        lateinit var orange: Color
            private set
        lateinit var green: Color
            private set
        lateinit var blue: Color
            private set
        lateinit var purple: Color
            private set

        lateinit var mention: TextColor
            private set
        lateinit var link: TextColor
            private set

        fun load() {
            red = config[ColorsSpec.red].toColor()
            orange = config[ColorsSpec.orange].toColor()
            green = config[ColorsSpec.green].toColor()
            blue = config[ColorsSpec.blue].toColor()
            purple = config[ColorsSpec.purple].toColor()

            mention = TextColor.fromHexString(config[ColorsSpec.mention])!!
            link = TextColor.fromHexString(config[ColorsSpec.link])!!
        }
    }
}
