package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object CommandsSpec : ConfigSpec() {
    object PlayerListSpec : ConfigSpec() {
        val enabled by required<Boolean>()
        val name by required<String>()
        val description by required<String>()
        val title by required<String>()
        val format by required<String>()
    }
}
