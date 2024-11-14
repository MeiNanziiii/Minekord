package ua.mei.minekord.utils

import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import dev.kord.common.Color
import dev.kord.common.entity.AllowedMentionType
import dev.kord.rest.builder.message.AllowedMentionsBuilder
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.TextParserUtils
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import ua.mei.minekord.bot.MinekordBot
import ua.mei.minekord.config.config
import ua.mei.minekord.config.spec.ChatSpec
import java.util.Base64

val mentions: AllowedMentionsBuilder = AllowedMentionsBuilder().apply {
    MinekordBot.launch {
        add(AllowedMentionType.UserMentions)
        roles.addAll(MinekordBot.guild.roles.filter { it.mentionable }.map { it.id }.toList())
    }
}

val discordOptions: DiscordSerializerOptions = DiscordSerializerOptions.defaults()
    .withEmbedLinks(false)
    .withEscapeMarkdown(config[ChatSpec.convertMarkdown])
    .withKeybindProvider(SerializerUtils::translatableToString)
    .withTranslationProvider(SerializerUtils::translatableToString)

val minecraftOptions: MinecraftSerializerOptions<Component> = MinecraftSerializerOptions.defaults()
    .addRenderer(MinekordMinecraftRenderer)

fun String.literal(): MutableText = Text.literal(this)

fun Component.toNative(): MutableText {
    return Text.Serializer.fromJson(GsonComponentSerializer.gson().serialize(this)) ?: Text.empty()
}

fun Text.toAdventure(): Component {
    return GsonComponentSerializer.gson().deserialize(Text.Serializer.toJson(this))
}

fun String.toAdventure(): Component = Component.text(this)

fun String.summary(): String {
    return if (this.length <= config[ChatSpec.MinecraftSpec.summaryMaxLength]) {
        this.trim()
    } else {
        this.take(config[ChatSpec.MinecraftSpec.summaryMaxLength]).trim() + "..."
    }
}

fun GameProfile.texture(): String {
    return try {
        JsonParser.parseString(Base64.getDecoder().decode(this.properties.get("textures").firstOrNull()?.value ?: "").toString(Charsets.UTF_8))
            .getAsJsonObject()
            .getAsJsonObject("textures")
            .getAsJsonObject("SKIN")
            .getAsJsonPrimitive("url")
            .getAsString()
            .let { it.substring(it.lastIndexOf('/') + 1) }
            .takeIf { it.isNotBlank() } ?: ""
    } catch (_: Throwable) {
        return ""
    }
}

class PlaceholderBuilder {
    val map: MutableMap<String, Text> = mutableMapOf<String, Text>()

    infix fun String.to(value: Text) {
        map[this] = value
    }
}

fun parseText(input: String, context: PlaceholderContext, placeholders: PlaceholderBuilder.() -> Unit): Text {
    return Placeholders.parseText(
        Placeholders.parseText(
            TextParserUtils.formatText(input),
            Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM,
            PlaceholderBuilder().apply(placeholders).map
        ),
        context
    )
}

fun parseText(input: String, server: MinecraftServer): Text {
    return Placeholders.parseText(
        TextParserUtils.formatText(input),
        PlaceholderContext.of(server)
    )
}

fun parseText(input: String, player: ServerPlayerEntity): Text {
    return Placeholders.parseText(
        TextParserUtils.formatText(input),
        PlaceholderContext.of(player)
    )
}

fun String.toColor(): Color = Color(this.removePrefix("#").toInt(16))

val ServerPlayerEntity.avatar: String
    get() = parseText(config[ChatSpec.WebhookSpec.playerAvatar], PlaceholderContext.of(this@avatar)) {
        "nickname" to this@avatar.gameProfile.name.literal()
        "texture" to this@avatar.gameProfile.texture().literal()
    }.string
