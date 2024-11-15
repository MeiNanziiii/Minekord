package ua.mei.minekord.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import net.fabricmc.loader.api.FabricLoader
import ua.mei.minekord.config.spec.BotSpec
import ua.mei.minekord.config.spec.ChatSpec
import ua.mei.minekord.config.spec.ColorsSpec
import ua.mei.minekord.config.spec.PresenceSpec

const val CONFIG_PATH: String = "minekord.toml"

var config: Config = loadConfig()
    private set

fun loadConfig(): Config {
    return Config {
        addSpec(BotSpec)
        addSpec(ChatSpec)
        addSpec(ColorsSpec)
        addSpec(PresenceSpec)
    }.from.toml.file(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH).toFile())
}

fun reloadConfig() {
    config = loadConfig()
    config.validateRequired()
}
