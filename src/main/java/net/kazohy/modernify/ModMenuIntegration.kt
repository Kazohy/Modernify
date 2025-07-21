package net.kazohy.modernify

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screen.Screen

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen?> {
        return ConfigScreenFactory { parent: Screen? -> null } // return null if no config screen yet
    }
}