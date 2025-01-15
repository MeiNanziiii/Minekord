package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object AuthSpec : ConfigSpec() {
    val requiredRoles by required<List<ULong>>()
    val ipBasedLogin by required<Boolean>()
}
