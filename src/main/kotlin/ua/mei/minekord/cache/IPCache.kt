package ua.mei.minekord.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.mojang.authlib.GameProfile
import io.ktor.util.network.address
import net.fabricmc.loader.api.FabricLoader
import java.io.FileReader
import java.net.SocketAddress
import java.nio.file.Files
import java.nio.file.Path

object IPCache {
    private var cache: MutableMap<String, String> = mutableMapOf()
    val path: Path = FabricLoader.getInstance().gameDir.resolve("minekord/ip-cache.json")
    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    val blockedIps: MutableList<String> = mutableListOf()
    val alreadyRequestedIps: MutableMap<String, MutableList<String>> = mutableMapOf()

    fun load() {
        if (!Files.exists(path)) {
            Files.createFile(path)
            Files.write(path, "{}".toByteArray())
        }
        val reader: JsonReader = JsonReader(FileReader(path.toFile()))
        cache = gson.fromJson(reader, Map::class.java)
        reader.close()
    }

    fun isIpRequested(address: SocketAddress, profile: GameProfile): Boolean {
        if (alreadyRequestedIps[profile.name]?.contains(address.address) == true) {
            return true
        }
        alreadyRequestedIps.getOrPut(profile.name) { mutableListOf() }.add(address.address)
        return false
    }

    fun save() {
        if (!Files.exists(path)) {
            Files.createFile(path)
        }
        Files.write(path, gson.toJson(cache).toByteArray())
    }

    fun putIntoCache(nickname: String, ip: String) {
        cache[nickname] = ip
        save()
    }

    fun containsInCache(nickname: String, socketAddress: SocketAddress): Boolean {
        return cache[nickname] == socketAddress.address
    }

    fun isBlockedIp(socketAddress: SocketAddress): Boolean {
        return blockedIps.contains(socketAddress.address)
    }
}
