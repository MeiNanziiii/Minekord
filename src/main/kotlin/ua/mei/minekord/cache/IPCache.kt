package ua.mei.minekord.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import net.fabricmc.loader.api.FabricLoader
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

object IPCache {
    private var cache: MutableMap<UUID, String> = mutableMapOf()
    val path: Path = FabricLoader.getInstance().gameDir.resolve("ip-cache.json")
    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun load() {
        if (!Files.exists(path)) {
            Files.createFile(path)
            Files.write(path, "{}".toByteArray())
        }
        val reader: JsonReader = JsonReader(FileReader(path.toFile()))
        cache = gson.fromJson(reader, MutableMap::class.java)
        reader.close()
    }

    fun save() {
        if (!Files.exists(path)) {
            Files.createFile(path)
        }
        Files.write(path, gson.toJson(cache).toByteArray())
    }

    fun putIntoCache(uuid: UUID, ip: String) {
        cache[uuid] = ip
        save()
    }

    fun getFromCache(uuid: UUID): String {
        return cache[uuid] ?: ""
    }
}