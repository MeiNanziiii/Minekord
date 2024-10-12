package ua.mei.minekord.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import net.fabricmc.loader.api.FabricLoader

const val CONFIG_PATH: String = "minekord.toml"

val config: Config = Config {
    addSpec(BotSpec)
    addSpec(ChatSpec)
    addSpec(PresenceSpec)
    addSpec(ExperimentalSpec)
}
    .from.toml.resource(CONFIG_PATH)
    .from.toml.watchFile(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH).toFile())
