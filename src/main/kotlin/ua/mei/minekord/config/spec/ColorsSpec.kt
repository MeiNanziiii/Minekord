package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object ColorsSpec : ConfigSpec() {
    val red by required<String>()
    val orange by required<String>()
    val green by required<String>()
    val blue by required<String>()
    val purple by required<String>()

    val mention by required<String>()
    val link by required<String>()
}
