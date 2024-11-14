package ua.mei.minekord.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import net.fabricmc.loader.api.FabricLoader
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.ColorsSpec
import ua.mei.minekord.config.spec.PresenceSpec

const val CONFIG_PATH: String = "minekord.toml"

val config: Config = Config {
    addSpec(BotSpec)
    addSpec(ChatSpec)
    addSpec(ColorsSpec)
    addSpec(PresenceSpec)
}
    .from.toml.resource(CONFIG_PATH)
    .from.toml.watchFile(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH).toFile())
