package ua.mei.minekord.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minecraft.registry.RegistryWrapper
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import ua.mei.minekord.config.ChatSpec
import ua.mei.minekord.config.config

fun String.literal(): MutableText = Text.literal(this)

fun Component.toNative(wrapperLookup: RegistryWrapper.WrapperLookup): MutableText {
    return Text.Serialization.fromJson(GsonComponentSerializer.gson().serialize(this), wrapperLookup) ?: Text.empty()
}

fun Component.asString(): String {
    return PlainTextComponentSerializer.plainText().serialize(this)
}

fun Text.toAdventure(wrapperLookup: RegistryWrapper.WrapperLookup): Component {
    return GsonComponentSerializer.gson().deserialize(Text.Serialization.toJsonString(this, wrapperLookup))
}

fun String.toAdventure(): Component = Component.text(this)

fun String.summary(): String {
    return if (this.length <= config[ChatSpec.MinecraftSpec.summaryMaxLength]) {
        this.trim()
    } else {
        this.take(config[ChatSpec.MinecraftSpec.summaryMaxLength]).trim() + "..."
    }
}
