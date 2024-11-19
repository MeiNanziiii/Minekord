package ua.mei.minekord.config.spec

import com.uchuhimo.konf.ConfigSpec

object AuthSpec : ConfigSpec() {
    val snowflakeBasedUuid by required<Boolean>()
    val requiredRoles by required<List<ULong>>()
    val ipBasedLogin by required<Boolean>()
}
