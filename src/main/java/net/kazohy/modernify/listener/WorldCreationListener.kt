package net.kazohy.modernify.listener

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.kazohy.modernify.util.ScreenshotTicker
import net.minecraft.client.MinecraftClient
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.Map
import java.util.function.Function
import java.util.function.ToLongFunction


object WorldCreationListener {
    var shouldScreenshotBeTaken: Boolean = false
    private var screenshotCountdownEndTime: Long = -1

    fun startCountdown(delayMillis: Int) {
        screenshotCountdownEndTime = System.currentTimeMillis() + delayMillis
    }

    val isCountdownFinished: Boolean
        get() = screenshotCountdownEndTime > 0 && System.currentTimeMillis() >= screenshotCountdownEndTime

    fun init() {
        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, sender, client ->
            onPlayerJoin()
        })
        ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, server ->
            onPlayerDisconnect()
        })
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient? ->
            if (shouldScreenshotBeTaken) {
                ScreenshotTicker.tick()
            }
        })
        WorldRenderEvents.END.register(WorldRenderEvents.End { context: WorldRenderContext? ->
            if (shouldScreenshotBeTaken && isCountdownFinished) {
                ScreenshotTicker.tick()
            }
        })
    }

    private fun onPlayerJoin() {
        shouldScreenshotBeTaken = true
    }

    private fun onPlayerDisconnect() {
        shouldScreenshotBeTaken = false
    }
}