package net.kazohy.modernify

import net.fabricmc.api.ModInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MainMod : ModInitializer {
    override fun onInitialize() {
        LOGGER.info("Successfully initialized Modernify")
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("")
    }
}
