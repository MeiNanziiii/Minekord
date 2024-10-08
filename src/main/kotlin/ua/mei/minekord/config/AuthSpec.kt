package ua.mei.minekord.config

import com.uchuhimo.konf.ConfigSpec

object AuthSpec : ConfigSpec() {
    val uuidFromSnowflake by required<Boolean>()
    val allowOfflinePlayers by required<Boolean>()
    val requiredRoles by required<List<ULong>>()
    val loginByIp by required<Boolean>()
}
