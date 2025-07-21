package net.kazohy.modernify

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.serializer.ConfigSerializer
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.kazohy.modernify.listener.WorldCreationListener
import net.kazohy.modernify.util.ScreenshotTicker
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class Modernify : ClientModInitializer {
    override fun onInitializeClient() {
        AutoConfig.register<ModConfig?>(
            ModConfig::class.java,
            ConfigSerializer.Factory { definition: Config?, configClass: Class<ModConfig?>? ->
                return@Factory Toml4jConfigSerializer(
                    definition,
                    configClass
                )
            }
        )

        config = AutoConfig.getConfigHolder<ModConfig?>(ModConfig::class.java).getConfig()

        val gameDir = MinecraftClient.getInstance().runDirectory
        val myModFolder = File(gameDir, "higherResScreenshots")

        if (!myModFolder.exists()) {
            myModFolder.mkdirs()
        }
        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { handler, sender, client ->
            WorldCreationListener.shouldScreenshotBeTaken = true
            val serverInfo = client!!.currentServerEntry
            ScreenshotTicker.tick()
            if (serverInfo != null) {
                saveRecentServer(serverInfo.name, serverInfo.address)
            }
        })

        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayConnectionEvents.Disconnect { handler, client ->
            WorldCreationListener.shouldScreenshotBeTaken = false
        })
    }

    private fun saveRecentServer(name: String?, address: String?) {
        val server = RecentServer(name, address)

        try {
            Files.createDirectories(RECENT_SERVER_PATH.getParent())

            val json: String = GSON.toJson(server)
            Files.writeString(RECENT_SERVER_PATH, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

            LOGGER.info("[RecentServerTracker] Saved server: " + name + " (" + address + ")")
        } catch (e: IOException) {
            System.err.println("[RecentServerTracker] Failed to save recent server: " + e.message)
        }
    }

    class RecentServer(var name: String?, var address: String?)
    companion object {
        var shouldTakeScreenshot: Boolean = false
        private val RECENT_SERVER_PATH: Path = MinecraftClient.getInstance()
            .runDirectory.toPath()
            .resolve("config/recentServer.json")

        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
        val LOGGER: Logger = LoggerFactory.getLogger("")
        var config: ModConfig? = null

        fun loadRecentServer(): RecentServer? {
            val path = MinecraftClient.getInstance()
                .runDirectory.toPath()
                .resolve("config/recentServer.json")

            if (!Files.exists(path)) {
                return null
            }

            try {
                val json = Files.readString(path)
                return Gson().fromJson<RecentServer?>(json, RecentServer::class.java)
            } catch (e: IOException) {
                System.err.println("[RecentServerTracker] Failed to read recent_server.json: " + e.message)
            } catch (e: Exception) {
                System.err.println("[RecentServerTracker] JSON is malformed: " + e.message)
            }

            return null
        }
    }
}
