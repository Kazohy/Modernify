package net.kazohy.modernify.mixin

import net.kazohy.modernify.screen.CustomTitleScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import org.objectweb.asm.Opcodes
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

@Mixin(MinecraftClient::class)
class TitleScreenMixin {
    @Redirect(
        method = ["setScreen"],
        at = At(
            value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private fun injected(instance: MinecraftClient, value: Screen?) {
        if (value is TitleScreen) {
            instance.setScreen(CustomTitleScreen())
        } else {
            instance.currentScreen = value
        }
    }
}