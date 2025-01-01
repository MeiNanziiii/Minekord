package ua.mei.minekord.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import dev.kord.common.Color
import eu.pb4.placeholders.api.ParserContext
import eu.pb4.placeholders.api.node.DynamicTextNode
import eu.pb4.placeholders.api.node.EmptyNode
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.NodeParser
import eu.pb4.placeholders.api.parsers.TagLikeParser
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.text.format.TextColor
import net.minecraft.text.Text
import ua.mei.minekord.Minekord
import ua.mei.minekord.config.spec.*
import ua.mei.minekord.utils.MinekordActivityType
import ua.mei.minekord.utils.toColor
import java.util.function.Function as JavaFunction

object MinekordConfig {
    const val CONFIG_PATH: String = "minekord.toml"

    val dynamicKey: ParserContext.Key<JavaFunction<String, Text?>> = DynamicTextNode.key(Minekord.MOD_ID)
    val parser: NodeParser = NodeParser.builder()
        .simplifiedTextFormat()
        .quickText()
        .globalPlaceholders()
        .placeholders(TagLikeParser.PLACEHOLDER_ALTERNATIVE, dynamicKey)
        .staticPreParsing()
        .build()

    private lateinit var config: Config

    fun load() {
        config = Config {
            addSpec(MainSpec)
            addSpec(ChatSpec)
            addSpec(PresenceSpec)
            addSpec(CommandsSpec)
            addSpec(ColorsSpec)
            addSpec(AuthSpec)
            addSpec(LuckPermsSpec)
            addSpec(MessagesSpec)
        }.from.toml.file(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH).toFile())

        config.validateRequired()

        Main.load()
        Chat.load()
        Presence.load()
        Commands.load()
        Colors.load()
        Auth.load()
        LuckPerms.load()
        Messages.load()
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

    object Auth {
        var snowflakeBasedUuid: Boolean = false
            private set
        var requiredRoles: List<ULong> = emptyList()
            private set
        var ipBasedLogin: Boolean = false
            private set

        fun load() {
            snowflakeBasedUuid = config[AuthSpec.snowflakeBasedUuid]
            requiredRoles = config[AuthSpec.requiredRoles]
            ipBasedLogin = config[AuthSpec.ipBasedLogin]
        }
    }

    object LuckPerms {
        lateinit var roles: Map<String, ULong>
            private set

        fun load() {
            roles = config[LuckPermsSpec.roles]
        }
    }

    object Messages {
        lateinit var ipKickMessage: String
            private set
        lateinit var embedTitle: String
            private set
        lateinit var timeLabel: String
            private set
        lateinit var yesButton: String
            private set
        lateinit var noButton: String
            private set
        lateinit var unblockButton: String
            private set
        lateinit var ipBlockedTitle: String
            private set
        lateinit var ipUnblockedTitle: String
            private set

        fun load() {
            ipKickMessage = config[MessagesSpec.ipKickMessage]
            embedTitle = config[MessagesSpec.embedTitle]
            timeLabel = config[MessagesSpec.timeLabel]
            yesButton = config[MessagesSpec.yesButton]
            noButton = config[MessagesSpec.noButton]
            unblockButton = config[MessagesSpec.unblockButton]
            ipBlockedTitle = config[MessagesSpec.ipBlockedTitle]
            ipUnblockedTitle = config[MessagesSpec.ipUnblockedTitle]
        }
    }
}
