package net.kazohy.modernify

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.Config.Gui.Background
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = "modernify-title-screen")
@Background("textures/block/blue_concrete_powder.png")
class ModConfig : ConfigData {
    @ConfigEntry.Gui.Tooltip(count = 2)
    var showSplashText: Boolean = true
}