package ua.mei.minekord.bot.extensions

import dev.kord.common.entity.AllowedMentionType
import dev.kord.core.behavior.execute
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.kordex.core.extensions.Extension
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.text.Text
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.ChatSpec
import ua.mei.minekord.config.config
import ua.mei.minekord.event.PlayerMessageEvent
import ua.mei.minekord.utils.SerializerUtils

class MessageExtension : Extension() {
    override val name: String = "Message Extension"

    private val mentions: AllowedMentionsBuilder = AllowedMentionsBuilder()
    private val discordOptions: DiscordSerializerOptions = DiscordSerializerOptions.defaults()
        .withEmbedLinks(false)
        .withEscapeMarkdown(config[ChatSpec.convertMarkdown])
        .withKeybindProvider(SerializerUtils::translatableToString)
        .withTranslationProvider(SerializerUtils::translatableToString)

    override suspend fun setup() {
        mentions.add(AllowedMentionType.UserMentions)
        mentions.roles.addAll(MinekordBot.guild.roles.filter { it.mentionable }.map { it.id }.toList())

        PlayerMessageEvent.event.register { profile, message, wrapperLookup ->
            MinekordBot.launch {
                MinekordBot.webhook.execute(MinekordBot.webhook.token!!) {
                    allowedMentions = mentions
                    username = profile.name
                    content = DiscordSerializer.INSTANCE.serialize(
                        GsonComponentSerializer.gson().deserialize(
                            Text.Serialization.toJsonString(message, wrapperLookup)
                        ), discordOptions
                    ).let {
                        if (config[ChatSpec.allowMentions]) SerializerUtils.convertMentions(it) else it
                    }
                    avatarUrl = "https://api.bonfiremc.site/attachments/avatars/${profile.name}.png?size=128"
                }
            }
        }
    }
}