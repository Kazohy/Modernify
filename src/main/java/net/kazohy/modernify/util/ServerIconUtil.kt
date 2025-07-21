package net.kazohy.modernify.util

import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.util.Identifier
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.Locale

object ServerIconUtil {
    fun getServerIconIdentifier(serverName: String?, modId: String): Identifier? {
        try {
            val serversDat = MinecraftClient.getInstance().runDirectory.toPath().resolve("servers.dat")
            val rootTag = NbtIo.read(serversDat)
            val servers = rootTag!!.getList("servers", NbtElement.COMPOUND_TYPE.toInt())

            for (i in servers.indices) {
                val server = servers.getCompound(i)
                if (server.getString("name").equals(serverName, ignoreCase = true) && server.contains(
                        "icon",
                        NbtElement.STRING_TYPE.toInt()
                    )
                ) {
                    var base64 = server.getString("icon")
                    if (base64.startsWith("data:image/png;base64,")) {
                        base64 = base64.substring("data:image/png;base64,".length)
                    }

                    val iconBytes = Base64.getDecoder().decode(base64)
                    val image = NativeImage.read(ByteArrayInputStream(iconBytes))
                    val texture = NativeImageBackedTexture(image)

                    val sanitizedName =
                        serverName!!.lowercase(Locale.getDefault()).replace("[^a-z0-9_\\-]".toRegex(), "_")
                    val id = Identifier.of(modId, "server_icon/" + sanitizedName)

                    MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture)
                    return id
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to load icon for server: " + serverName)
            e.printStackTrace()
        }

        return null
    }
}