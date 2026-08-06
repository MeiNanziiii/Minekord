package ua.bonfiremc.minekord.config

import com.uchuhimo.konf.ConfigSpec
import dev.kord.common.entity.Snowflake

object BotSpec : ConfigSpec("") {
    val token by required<String>()
    val channel by required<Snowflake>()
}