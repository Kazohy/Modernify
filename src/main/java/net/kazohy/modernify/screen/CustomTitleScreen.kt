package net.kazohy.modernify.screen

import java.io.File
import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.systems.RenderSystem
import com.terraformersmc.modmenu.api.ModMenuApi
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.kazohy.modernify.Modernify
import net.kazohy.modernify.Modernify.RecentServer
import net.kazohy.modernify.util.ServerIconUtil
import net.kazohy.modernify.util.WorldUtils
import net.kazohy.modernify.listener.WorldCreationListener
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.LogoDrawer
import net.minecraft.client.gui.screen.ButtonTextures
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.SplashTextRenderer
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.PlayerSkinWidget
import net.minecraft.client.gui.widget.PressableTextWidget
import net.minecraft.client.gui.widget.TexturedButtonWidget
import net.minecraft.client.network.CookieStorage
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.realms.gui.screen.RealmsMainScreen
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis
import org.lwjgl.opengl.GL11
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@Environment(EnvType.CLIENT)
class CustomTitleScreen() :
    Screen(Text.translatable("narrator.screen.title")) {

    private var splashText: SplashTextRenderer? = null

    override fun shouldPause(): Boolean {
        return false
    }

    override fun shouldCloseOnEsc(): Boolean {
        return false
    }

    override fun init() {
        if (this.splashText == null) {
            checkNotNull(this.client)
            this.splashText = this.client!!.splashTextLoader.get()
        }

        screenWidth = this.width
        screenHeight = this.height

        val playerWidgetX = this.width / 2 + this.width * (59 / 2) / 1800
        val playerWidgetY = this.height / 2 - this.height * 298 / 1080 + 279 * this.height / 1080
        val playerWidgetWidth = 409 * this.width / 1800
        val playerWidgetHeight = playerWidgetWidth * 280 / 409

        val profile: GameProfile?
        checkNotNull(client)
        if (client!!.player != null) {
            profile = client!!.player!!.gameProfile
        } else {
            val username = client!!.session.username
            profile = GameProfile(
                UUID.nameUUIDFromBytes(("OfflinePlayer:$username").toByteArray(StandardCharsets.UTF_8)),
                username
            )
        }

        val skinProvider = MinecraftClient.getInstance().skinProvider
        val skinSupplier = skinProvider.getSkinTexturesSupplier(profile)
        val models = MinecraftClient.getInstance().loadedEntityModels
        val playerSkin = PlayerSkinWidget(64, 88, models, skinSupplier)

        this.addDrawableChild(playerSkin)
        playerSkin.setPosition(playerWidgetX + playerWidgetWidth / 2 - 32, playerWidgetY + playerWidgetHeight / 2 - 16)

        RenderSystem.setShaderTexture(0, NORMAL_BUTTON_TEXTURE)
        RenderSystem.setShaderTexture(1, NORMAL_BUTTON_TEXTURE)
        RenderSystem.setShaderTexture(2, NORMAL_BUTTON_TEXTURE)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        val normalButtonTextures = ButtonTextures(
            NORMAL_BUTTON_TEXTURE,
            NORMAL_BUTTON_TEXTURE,
            NORMAL_BUTTON_TEXTURE
        )

        val buttonWidth = 408 * this.width / 1800
        val buttonHeight = buttonWidth * 68 / 408
        val buttonX = this.width / 2 - buttonWidth - this.width * (59 / 2) / 1800
        val buttonY = this.height / 2 - this.height * 298 / 1080

        this.addDrawableChild(
            TexturedButtonWidget(
                buttonX,
                buttonY,
                buttonWidth,
                buttonHeight,
                normalButtonTextures,
                { button: ButtonWidget? -> this.client!!.setScreen(SelectWorldScreen(this)) },
                Text.literal("Options")
            )
        )

        this.addDrawableChild(
            TexturedButtonWidget(
                buttonX,
                buttonY + this.height * 17 / 1080 + buttonHeight,
                buttonWidth,
                buttonHeight,
                normalButtonTextures,
                { button: ButtonWidget? -> this.client!!.setScreen(MultiplayerScreen(this)) },
                Text.literal("Options")
            )
        )

        val modsButtonActive = FabricLoader.getInstance().isModLoaded("modmenu")

        if (modsButtonActive) {LOGGER.info("Mod Menu is active")}

        this.addDrawableChild(
            TexturedButtonWidget(
                buttonX,
                buttonY + 3 * (this.height * 17 / 1080 + buttonHeight),
                buttonWidth,
                buttonHeight,
                normalButtonTextures,
                { button: ButtonWidget? ->
                    this.client!!.setScreen(
                        OptionsScreen(
                            this,
                            this.client!!.options
                        )
                    )
                },
                Text.literal("Options")
            )
        )

        this.addDrawableChild(
            TexturedButtonWidget(
                playerWidgetX,
                buttonY + 43 * this.height / 1080,
                buttonWidth,
                108 * this.height / 1080,
                normalButtonTextures,
                { button: ButtonWidget? -> playWorld(WorldUtils.mostRecentSingleplayerWorldName().toString()) },
                Text.literal("Options")
            )
        )

        val last: RecentServer? = Modernify.Companion.loadRecentServer()
        val lastJoinedServerAddress: String?
        if (last != null) {
            lastJoinedServerAddress = last.address
            LOGGER.info(lastJoinedServerAddress)
            this.addDrawableChild(
                PressableTextWidget(
                    playerWidgetX,
                    buttonY + 43 * this.height / 1080 + 122 * this.height / 1080,
                    buttonWidth,
                    108 * this.height / 1080,
                    Text.literal(""),
                    { button: ButtonWidget? -> joinServer(lastJoinedServerAddress) },
                    textRenderer
                )
            )
        } else {
            this.addDrawableChild(
                PressableTextWidget(
                    playerWidgetX,
                    buttonY + 43 * this.height / 1080 + 122 * this.height / 1080,
                    buttonWidth,
                    115 * this.height / 1080,
                    Text.literal(""),
                    { button: ButtonWidget? -> this.client!!.setScreen(MultiplayerScreen(this)) },
                    textRenderer
                )
            )
        }


        this.addDrawableChild(
            PressableTextWidget(
                buttonX,
                buttonY + 2 * (this.height * 17 / 1080 + buttonHeight),
                buttonWidth,
                buttonHeight,
                Text.literal(""),
                { button: ButtonWidget? -> this.client!!.setScreen(RealmsMainScreen(this)) },
                textRenderer
            )
        )

        this.addDrawableChild(
            TexturedButtonWidget(
                buttonX,
                buttonY + 5 * (this.height * 17 / 1080 + buttonHeight),
                buttonWidth,
                buttonHeight,
                normalButtonTextures,
                { button: ButtonWidget? ->
                    this.client!!.setScreen(
                        AccessibilityOptionsScreen(
                            this,
                            this.client!!.options
                        )
                    )
                },
                Text.translatable("menu.modernify.accessibility")
            )
        )

        this.addDrawableChild(
            TexturedButtonWidget(
                buttonX,
                buttonY + 4 * (this.height * 17 / 1080 + buttonHeight),
                buttonWidth,
                buttonHeight,
                normalButtonTextures,
                { button: ButtonWidget? ->
                    this.client!!.setScreen(ModMenuApi.createModsScreen(this))
                },
                Text.translatable("menu.modernify.accessibility")
            )
        )

        this.addDrawableChild(
            PressableTextWidget(
                buttonX,
                buttonY + 6 * (this.height * 17 / 1080 + buttonHeight),
                buttonWidth,
                buttonHeight,
                Text.literal(""),
                { button: ButtonWidget? -> this.client!!.scheduleStop() },
                textRenderer
            )
        )
    }

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val matrices = ctx.matrices

        val buttonWidth = 408 * this.width / 1800
        val logoXMiddle = this.width / 2 + 400
        val gradientWidth = (this.width * 0.7).toInt()

        matrices.push()
        matrices.translate(gradientWidth / 2f, this.height / 2f, 0f)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90f))
        ctx.fillGradient(
            -floor((this.height / 2f).toDouble()).toInt(),
            -floor((gradientWidth / 2f).toDouble()).toInt() - 1,
            ceil((this.height / 2f).toDouble()).toInt(),
            ceil((gradientWidth / 2f).toDouble()).toInt(),
            0x7b000000, 0x00000000, 0
        )
        matrices.pop()

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX)
        RenderSystem.setShaderTexture(0, AESTHETIC_ELEMENT_DOWN)
        RenderSystem.setShaderTexture(1, AESTHETIC_ELEMENT_UP)
        RenderSystem.setShaderTexture(2, NORMAL_BUTTON_TEXTURE)
        RenderSystem.setShaderTexture(3, REALMS_BUTTON_TEXTURE)
        RenderSystem.setShaderTexture(4, NEWS_BUTTON_TEXTURE)
        RenderSystem.setShaderTexture(5, PLAYER_WIDGET_BUTTON)
        RenderSystem.setShaderTexture(5, RECENTS_BUTTONS)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        super.render(ctx, mouseX, mouseY, delta)

        val buttonHeight = buttonWidth * 68 / 408
        val buttonX = this.width / 2 - buttonWidth - this.width * (59 / 2) / 1800
        var buttonY = this.height / 2 - this.height * 298 / 1080
        val recentsButtonY = buttonY
        val playerWidgetX = this.width / 2 + this.width * (59 / 2) / 1800
        val playerWidgetY: Int
        val playerWidgetWidth = 409 * this.width / 1800
        val playerWidgetHeight = playerWidgetWidth * 280 / 409
        val spacing = this.height * 17 / 1080 + buttonHeight
        val buttonRenderWidth = 408 * this.width / 1800
        val buttonBaseX = this.width / 2 - buttonRenderWidth - this.width * (59 / 2) / 1800
        var buttonCurrentY = this.height / 2 - this.height * 298 / 1080

        for (i in 0..6) {
            if ((i + 1) % 3 != 0) {
                ctx.drawTexture(
                    { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
                    NORMAL_BUTTON_TEXTURE,
                    buttonX,
                    buttonY,
                    0f,
                    0f,
                    buttonWidth,
                    buttonHeight,
                    buttonWidth,
                    buttonHeight
                )
            } else if (i == 2) {
                ctx.drawTexture(
                     { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
                    REALMS_BUTTON_TEXTURE,
                    buttonX,
                    buttonY,
                    0f,
                    0f,
                    buttonWidth,
                    buttonHeight,
                    buttonWidth,
                    buttonHeight
                )
            } else {
                ctx.drawTexture(
                     { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
                    NEWS_BUTTON_TEXTURE,
                    buttonX,
                    buttonY,
                    0f,
                    0f,
                    buttonWidth,
                    buttonHeight,
                    buttonWidth,
                    buttonHeight
                )
            }

            val labels = arrayOf<Text>(
                Text.translatable("menu.modernify.singleplayer"),
                Text.translatable("menu.modernify.multiplayer"),
                Text.translatable("menu.modernify.realms"),
                Text.translatable("menu.modernify.options"),
                Text.translatable("menu.modernify.modmenu"),
                Text.translatable("menu.modernify.accessibility"),
                Text.translatable("menu.modernify.quit")
            )

            val label = labels[i]
            val labelWidth = textRenderer.getWidth(label)
            val centeredX = buttonBaseX + (buttonRenderWidth - labelWidth) / 2
            val centeredY = buttonCurrentY + (buttonHeight - textRenderer.fontHeight) / 2
            ctx.drawText(textRenderer, label, centeredX, centeredY, 0xFFFFFF, true)

            buttonCurrentY += spacing
            buttonY += this.height * 17 / 1080 + buttonHeight
        }

        playerWidgetY = buttonY - playerWidgetHeight
        ctx.drawTexture(
             { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
            PLAYER_WIDGET_BUTTON,
            playerWidgetX,
            playerWidgetY,
            0f,
            0f,
            playerWidgetWidth,
            playerWidgetHeight,
            playerWidgetWidth,
            playerWidgetHeight
        )

        ctx.drawTexture(
             { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
            RECENTS_BUTTONS,
            playerWidgetX, recentsButtonY,
            0f, 0f, playerWidgetWidth, playerWidgetWidth * 262 / 409,
            playerWidgetWidth, playerWidgetWidth * 262 / 409
        )

        val textureWidth = this.width
        val textureHeight = textureWidth * 330 / 1800
        val y = this.height - textureHeight

        var x = 0
        while (x < this.width) {
            ctx.drawTexture(
                { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
                AESTHETIC_ELEMENT_DOWN,
                x, y,
                0f, 0f, textureWidth, textureHeight,
                textureWidth, textureHeight
            )
            x += textureWidth
        }
        ctx.drawTexture(
            { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
            AESTHETIC_ELEMENT_UP,
            0, 0,
            0f, 0f, textureWidth, textureHeight,
            textureWidth, textureHeight
        )

        val tr = MinecraftClient.getInstance().textRenderer
        val username = MinecraftClient.getInstance().session.username
        val textWidth = tr.getWidth(username)
        ctx.drawText(
            client!!.textRenderer,
            Text.literal(username),
            playerWidgetX + playerWidgetWidth / 2 - textWidth / 2,
            playerWidgetY + 16 - 4,
            0xFFFFFF,
            true
        )

        lastLoadedWorld = WorldUtils.mostRecentSingleplayerWorldName().toString()

        val latestTextX = playerWidgetX + 189 * this.width / 1800
        val lastestWorldTextY =
            (recentsButtonY + getFromFigmaHeight(39 + 100 / 2 - 24 / 2) - textRenderer.fontHeight / 2).toFloat()
                .roundToInt()
        val lastestServerTextY =
            (lastestWorldTextY + getFromFigmaHeight(115) + textRenderer.fontHeight / 2).toFloat().roundToInt()
        val lastestWorldGamemodeTextY =
            (recentsButtonY + getFromFigmaHeight(39 + 100 / 2 + 24 / 2) + textRenderer.fontHeight / 2).toFloat()
                .roundToInt()
        val lastestServerIPAddress =
            (lastestWorldGamemodeTextY + getFromFigmaHeight(115) + textRenderer.fontHeight / 2).toFloat().roundToInt()
        val last: RecentServer? = Modernify.Companion.loadRecentServer()
        var lastJoinedServerName: String
        var lastJoinedServerAddress: String
        if (last != null) {
            lastJoinedServerName = last.name!!
            lastJoinedServerAddress = last.address!!
        } else {
            lastJoinedServerAddress = "Null"
            lastJoinedServerName = "No servers/realms joined"
        }

        lastLoadedWorld = ScrollingTextUtil.getScrollingText(lastLoadedWorld, 17)

        ctx.drawText(
            client!!.textRenderer,
            Text.literal(lastLoadedWorld),
            latestTextX,
            lastestWorldTextY,
            0xFFFFFF,
            true
        )

        val gameModeText = try {
            Text.literal(
                WorldUtils.getWorldGameMode(lastLoadedWorld).toString().substring(0, 1).uppercase(
                    Locale.getDefault()
                ) + WorldUtils.getWorldGameMode(lastLoadedWorld).toString().substring(1).lowercase(
                    Locale.getDefault()
                )
            )
        } catch (_: IOException) {
            Text.literal("Gamemode: Error")
        }

        ctx.drawText(
            client!!.textRenderer,
            gameModeText,
            latestTextX,
            lastestWorldGamemodeTextY,
            -0x41000001,
            true
        )

        lastJoinedServerAddress = ScrollingTextUtil.getScrollingText(lastJoinedServerAddress, 17)

        ctx.drawText(
            client!!.textRenderer,
            Text.literal(lastJoinedServerAddress),
            latestTextX,
            lastestServerIPAddress,
            -0x41000001,
            true
        )

        val iconX = (getFromFigmaWidth(6) + playerWidgetX).toFloat().roundToInt()
        val iconY = (getFromFigmaHeight(39 + 8) + recentsButtonY).toFloat().roundToInt()
        val iconHeight: Int = 99 * screenHeight / 1080
        val iconWidth = (iconHeight * 16) / 9

        lastJoinedServerName = ScrollingTextUtil.getScrollingText(lastJoinedServerName, 17)

        ctx.drawText(client!!.textRenderer, lastJoinedServerName, latestTextX, lastestServerTextY, 0xFFFFFF, true)

        val lastJoinedServerIcon = ServerIconUtil.getServerIconIdentifier(lastJoinedServerName, "modernify")
        LOGGER.info(lastJoinedServerIcon.toString())

        val bl: Path = MinecraftClient.getInstance()
            .runDirectory
            .toPath()
            .resolve("saves")
            .resolve(lastLoadedWorld)

        LOGGER.info(bl.toString())
        val iconPath = bl.resolve("screenshots/world_icon.png")

        try {
            Files.newInputStream(iconPath).use { stream ->
                val image = NativeImage.read(stream)
                val texture = NativeImageBackedTexture(image)

                // Use a safe path for the Identifier
                val safePath = "world_icons/" + lastLoadedWorld.lowercase(Locale.getDefault())
                    .replace("[^a-z0-9/._-]".toRegex(), "_")

                LATEST_WORLD_ICON = Identifier.of("modernify", safePath)
                MinecraftClient.getInstance().textureManager.registerTexture(LATEST_WORLD_ICON, texture)
                LOGGER.info("Successfully loaded world icon: $LATEST_WORLD_ICON")
            }
        } catch (e: IOException) {
            LOGGER.error("Failed to load world icon image", e)
        }
        ctx.drawTexture(
             { texture: Identifier? -> RenderLayer.getGuiTextured(texture) },
            LATEST_WORLD_ICON,
            iconX,
            iconY,
            0f,
            0f,
            iconWidth,
            iconHeight,
            iconWidth,
            iconHeight
        )


        ctx.drawText(
            client!!.textRenderer,
            Text.translatable("menu.modernify.continue_playing"),
            playerWidgetX + getFromFigmaWidth(14),
            recentsButtonY + getFromFigmaHeight(34) / 2 - textRenderer.fontHeight / 2,
            -0x1,
            true
        )

        val logoDrawer = LogoDrawer(true) // true = use Mojang logo
        val logoHeight = LogoDrawer.LOGO_REGION_HEIGHT
        logoDrawer.draw(ctx, this.width, delta, recentsButtonY - logoHeight - 60 * this.height / 1080)

        this.splashText!!.render(ctx, logoXMiddle, this.textRenderer, -0x1000000)
        WorldCreationListener.init()
    }

    private fun joinServer(ipAddress: String?) {
        val client = MinecraftClient.getInstance()

        if (client == null) {
            System.err.println("MinecraftClient instance is null. Cannot connect.")
            return
        }

        val address = ServerAddress.parse(ipAddress)

        // For direct IP connections, you typically don't have existing server info
        // or specific cookies to pass. So, null is usually appropriate here.
        val serverInfo = ServerInfo(ipAddress, ipAddress, ServerInfo.ServerType.OTHER) // name, address, local
        val cookieStorage: CookieStorage? = null // No cookies needed for a fresh connection

        client.execute {
            ConnectScreen.connect(
                TitleScreen(),  // Parent screen
                client,  // MinecraftClient instance
                address,  // ServerAddress (parsed IP/domain)
                serverInfo,  // ServerInfo (null for fresh connect by IP)
                false,  // quickPlay: false for a manual connection
                cookieStorage // CookieStorage: null unless you have specific cookie data
            )
            client.inGameHud.chatHud.addMessage(Text.literal("Attempting to connect to: $ipAddress"))
        }
    }


    object ScrollingTextUtil {
        private var scrollIndex = 0
        private var tickCounter = 0
        private var pauseTicks = 0
        private var isPaused = false

        // Settings
        private const val SCROLL_SPEED_TICKS = 10 // Slower scroll
        private const val PAUSE_DURATION_TICKS = 80

        /**
         * Get the cropped, scrolling version of fullText with optional trailing spaces.
         *
         * @param baseText The main message to scroll.
         * @param visibleLength The number of characters to show.
         * @return A cropped, scrolling segment of the text.
         */
        fun getScrollingText(baseText: String, visibleLength: Int): String {
            var visibleLength = visibleLength
            if (baseText.length > visibleLength) {
                if (visibleLength <= 0 || baseText.isEmpty()) return ""

                // Add padding spaces at the end of the string
                val fullText = baseText + " ".repeat(max(0, 7))

                tickCounter++

                if (isPaused) {
                    pauseTicks++
                    if (pauseTicks >= PAUSE_DURATION_TICKS) {
                        isPaused = false
                        pauseTicks = 0
                    }
                } else if (tickCounter % SCROLL_SPEED_TICKS == 0) {
                    scrollIndex = (scrollIndex + 1) % fullText.length
                    if (scrollIndex == 0) {
                        isPaused = true
                    }
                }

                // Scroll the text
                val scrolled: String = fullText.substring(scrollIndex) + fullText.substring(0, scrollIndex)
                if (visibleLength > scrolled.length) {
                    visibleLength = scrolled.length
                }

                return scrolled.substring(0, visibleLength)
            } else {
                return baseText
            }
        }
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(FabricLoaderImpl.MOD_ID)

        private var lastLoadedWorld = ""
        var screenWidth: Int = 0
        var screenHeight: Int = 0

        private val AESTHETIC_ELEMENT_DOWN: Identifier =
            Identifier.of("modernify-gui-textures", "textures/gui/title-screen/aesthetic_element_down.png")
        private val AESTHETIC_ELEMENT_UP: Identifier =
            Identifier.of("modernify-gui-textures", "textures/gui/title-screen/aesthetic_element_up.png")
        private val NORMAL_BUTTON_TEXTURE: Identifier =
            Identifier.of("modernify-gui-textures", "textures/gui/title-screen/buttons/normal_button.png")
        private val REALMS_BUTTON_TEXTURE: Identifier =
            Identifier.of("modernify-gui-textures", "textures/gui/title-screen/buttons/realms_button.png")
        private val NEWS_BUTTON_TEXTURE: Identifier =
            Identifier.of("modernify-gui-textures", "textures/gui/title-screen/buttons/news_button.png")
        private val PLAYER_WIDGET_BUTTON: Identifier =
            Identifier.of("modernify-gui-textures", "textures/gui/title-screen/widgets/player_widget.png")
        private val RECENTS_BUTTONS: Identifier =
            Identifier.of("modernify-gui-textures", "textures/gui/title-screen/widgets/recents_buttons.png")
        private var LATEST_WORLD_ICON: Identifier = Identifier.of("")

        fun playWorld(worldName: String) {
            val client = MinecraftClient.getInstance()

            val savesDir = client.runDirectory.toPath().resolve("saves")
            val worldDir = savesDir.resolve(worldName)

            if (!Files.exists(worldDir)) {
                LOGGER.info("World folder doesn't exist: $worldDir")
                return
            }

            client.execute {
                client.disconnect()
                client.setScreen(null) // clear any lingering screen
                client.createIntegratedServerLoader().start(worldName) {
                    LOGGER.info("World loaded.")
                }
            }
        }

        fun getFromFigmaHeight(measure: Int): Int {
            return (measure * screenHeight / 1080)
        }

        fun getFromFigmaWidth(measure: Int): Int {
            return (measure * screenWidth / 1800)
        }
    }
}