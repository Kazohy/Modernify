package net.kazohy.modernify.util

import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.world.GameMode
import net.minecraft.world.level.storage.LevelSummary
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.function.Function

object WorldUtils {
    fun mostRecentSingleplayerWorldName(): String? {
            val client = MinecraftClient.getInstance()
            val storage = client.getLevelStorage()

            try {
                val levelList = storage.getLevelList()
                val summaries = storage.loadSummaries(levelList)
                    .thenApply<MutableList<LevelSummary?>>(Function { list: MutableList<LevelSummary?>? ->
                        list!!.stream()
                            .sorted(
                                Comparator.comparing<LevelSummary?, Long?>(Function { obj: LevelSummary? -> obj!!.getLastPlayed() })
                                    .reversed()
                            )
                            .toList()
                    }
                    )
                    .join() // block until ready

                if (!summaries.isEmpty()) {
                    return summaries.get(0)!!.getName()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }


            return "No worlds played"
    }


    @Throws(IOException::class)
    fun getWorldGameMode(worldName: String): GameMode {
        // Point to default saves folder (relative to run directory)
        val worldDirectory = File("saves", worldName)
        val levelDat = File(worldDirectory, "level.dat")

        if (!levelDat.exists()) {
            throw IOException("level.dat not found for world: " + worldName)
        }

        FileInputStream(levelDat).use { fis ->
            val nbt = NbtIo.readCompressed(fis, NbtSizeTracker.ofUnlimitedBytes())
            if (nbt == null || !nbt.contains("Data")) {
                throw IOException("Invalid level.dat structure in world: " + worldName)
            }

            val data = nbt.getCompound("Data")

            // Check for hardcore first
            if (data.contains("hardcore") && data.getBoolean("hardcore")) {
                return GameMode.SURVIVAL
            }

            val gameType = data.getInt("GameType")
            return when (gameType) {
                0 -> GameMode.SURVIVAL
                1 -> GameMode.CREATIVE
                2 -> GameMode.ADVENTURE
                3 -> GameMode.SPECTATOR
                else -> throw IOException("Unknown GameType: " + gameType)
            }
        }
    }
}