package ua.mei.minekord.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mojang.authlib.GameProfile
import io.ktor.util.network.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import java.io.FileReader
import java.io.FileWriter
import java.net.SocketAddress
import java.nio.file.Files
import java.nio.file.Path

object IPCache : ServerLifecycleEvents.ServerStarting, ServerLifecycleEvents.ServerStopped {
    var ipCache: MutableMap<String, String> = mutableMapOf()
    val alreadyRequestedIps: MutableMap<String, MutableList<String>> = mutableMapOf()
    val blockedIps: MutableList<String> = mutableListOf()
    val path: Path = FabricLoader.getInstance().gameDir.resolve("minekord/ip-cache.json")
    val type: TypeToken<MutableMap<String, String>> = object : TypeToken<MutableMap<String, String>>() {}
    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    override fun onServerStarting(server: MinecraftServer) {
        if (!Files.exists(path)) {
            Files.createFile(path)
            Files.write(path, "{}".toByteArray(Charsets.UTF_8))
        }

        FileReader(path.toFile()).use { reader ->
            ipCache = gson.fromJson(reader, type)
        }
    }

    override fun onServerStopped(server: MinecraftServer) {
        FileWriter(path.toFile()).use { writer ->
            writer.write(gson.toJson(ipCache))
        }
    }

    fun isBlocked(socketAddress: SocketAddress): Boolean {
        return blockedIps.contains(socketAddress.address)
    }

    fun isRequested(address: SocketAddress, profile: GameProfile): Boolean {
        if (alreadyRequestedIps[profile.name]?.contains(address.address) == true) {
            return true
        }
        alreadyRequestedIps.getOrPut(profile.name) { mutableListOf() }.add(address.address)
        return false
    }

    fun containsInCache(address: SocketAddress, profile: GameProfile): Boolean {
        return ipCache[profile.name] == address.address
    }
}