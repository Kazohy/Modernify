package net.kazohy.modernify.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.ScreenshotRecorder
import net.minecraft.text.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.function.Consumer


object ScreenshotTicker {
    val LOGGER: Logger = LoggerFactory.getLogger("")
    private var tickCounter = 0
    private const val INTERVAL_TICKS = 20000 // 10 seconds (20 ticks/sec)

    fun tick() {
        tickCounter++

        if (tickCounter >= INTERVAL_TICKS) {
            tickCounter = 0
            val worldName = MinecraftClient.getInstance()
                .getServer()!!
                .getSaveProperties()
                .getLevelName()
            takeScreenshot(worldName) // or pass dynamically if needed
        }
    }

    fun takeScreenshot(worldName: String) {
        val client = MinecraftClient.getInstance()

        client.execute {
            try {
                // Path to .minecraft/saves/<worldName>/
                val worldDir = client.runDirectory.toPath().resolve("saves").resolve(worldName)
                Files.createDirectories(worldDir)
                val worldFolder = worldDir.toFile()

                val folder = File(worldFolder, "screenshots")
                if (!folder.exists()) {
                    folder.mkdirs()
                }

                RenderSystem.recordRenderCall {
                    ScreenshotRecorder.saveScreenshot(
                        worldFolder,
                        "world_icon.png",
                        client.framebuffer
                    ) { text: Text? -> }
                    LOGGER.info("Screenshot created for $worldName")
                }
            } catch (e: Exception) {
                LOGGER.error("Failed to save world_icon for world '{}'", worldName, e)
            }
        }
    }
}