package org.polyfrost.behindyou
import org.polyfrost.oneconfig.api.event.v1.EventDelay


import org.polyfrost.oneconfig.gui.animations.*
import org.polyfrost.universal.*
import org.polyfrost.universal.wrappers.UPlayer
import org.polyfrost.oneconfig.api.commands.v1.CommandManager
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.*
import club.sk1er.patcher.config.PatcherConfig
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.polyfrost.behindyou.config.BehindYouConfig
import org.polyfrost.behindyou.config.EaseOutQuart
import java.io.File
import kotlin.math.abs

val mc: Minecraft get() = UMinecraft.getMinecraft()

//TODO: until actual toggle is implemented in twoconfig
const val enabled: Boolean = true

@Mod(
    modid = BehindYou.MODID,
    name = BehindYou.NAME,
    version = BehindYou.VERSION,
    clientSideOnly = true,
    modLanguageAdapter = "cc.polyfrost.oneconfig.utils.KotlinLanguageAdapter"
)
object BehindYou {
    const val MODID = "@ID@"
    const val NAME = "@NAME@"
    const val VERSION = "@VER@"

    var previousPerspective = 0
    var vPerspective = 0
    var previousFOV = 0f
    var realPerspective = 0

    var previousBackKey = false
    var backToggled = false
    var previousFrontKey = false
    var frontToggled = false

    val oldModDir = File(File("./W-OVERFLOW"), "BehindYouV3")

    var end = 0f
    var distance = 0f
    //TODO: var animation: Animation = DummyAnimation(0f)
    private var lastParallaxFix = false
    private var isPatcher = false

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        isPatcher = Loader.isModLoaded("patcher")
        MinecraftForge.EVENT_BUS.register(this)
        CommandManager.registerCommand(BehindYouCommand())
    }

    @SubscribeEvent
    fun EventDelay.ticks(event: TickEvent.RenderTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        onTick()
        val thirdPersonView = mc.gameSettings.thirdPersonView
        if (enabled && thirdPersonView != realPerspective) {
            setPerspective(thirdPersonView)
        }
    }

    // TODO: Animation stuff
    /*
    fun level(): Float {
        val parallaxFix = isPatcher && PatcherConfig.parallaxFix
        if (realPerspective == 0) {
            if (mc.gameSettings.thirdPersonView != 0) {
                mc.gameSettings.thirdPersonView = 0
                mc.renderGlobal.setDisplayListEntitiesDirty()
            }
            if (animation !is DummyAnimation || !(animation.get(0f) == -0.05f || animation.get(0f) == 0.1f) || lastParallaxFix != parallaxFix) {
                lastParallaxFix = parallaxFix
                animation = DummyAnimation(if (parallaxFix) -0.05f else 0.1f)
            }
        } else {
            if (end != 0.3f) end = distance
            if (animation.get() > distance) {
                animation = DummyAnimation(distance)
            } else if (animation.end != end) {
                animation = EaseOutQuart(if (BehindYouConfig.animation) 100 * abs(animation.get() - end) / BehindYouConfig.speed else 0f, animation.get(), end, false)
            }
        }
        if (animation.isFinished && animation.end == 0.3f) {
            mc.gameSettings.thirdPersonView = 0
            realPerspective = 0
            mc.renderGlobal.setDisplayListEntitiesDirty()
        }
        return animation.get()
    }
     */

    private fun onTick() {
        if (!enabled) {
            resetAll()
            return
        }
        if (UScreen.currentScreen != null || mc.theWorld == null || !UPlayer.hasPlayer()) {
            if (!BehindYouConfig.frontKeybindMode || !BehindYouConfig.backKeybindMode) {
                resetAll()
            }
            return
        }
        if (BehindYouConfig.backToFirst) previousPerspective = 0

        val backDown = BehindYouConfig.backKeybind.keys { UKeyboard.isKeyDown(it) }
        val frontDown = BehindYouConfig.frontKeybind.keyBinds.any { UKeyboard.isKeyDown(it) }

        if (backDown && frontDown) return

        if (backDown != previousBackKey) {
            previousBackKey = backDown

            if (backDown) {
                if (backToggled) {
                    resetBack()
                } else {
                    if (frontToggled) {
                        resetFront()
                    }
                    if (vPerspective != 2) enterBack() else resetBack()
                }
            } else if (!BehindYouConfig.backKeybindMode) {
                resetBack()
            }

        } else if (frontDown != previousFrontKey) {
            previousFrontKey = frontDown

            if (frontDown) {
                if (frontToggled) {
                    resetFront()
                } else {
                    if (backToggled) {
                        resetBack()
                    }
                    if (vPerspective != 1) enterFront() else resetFront()
                }
            } else if (!BehindYouConfig.frontKeybindMode) {
                resetFront()
            }

        }
    }

    fun backDown(): Boolean {

    }

    fun frontDown(): Boolean {

    }

    fun enterBack() {
        backToggled = true
        previousFOV = getFOV()
        setPerspective(2)
        if (BehindYouConfig.changeFOV) {
            setFOV(BehindYouConfig.backFOV)
        }
    }

    fun enterFront() {
        frontToggled = true
        previousFOV = getFOV()
        setPerspective(1)
        if (BehindYouConfig.changeFOV) {
            setFOV(BehindYouConfig.frontFOV)
        }
    }

    fun resetBack() {
        backToggled = false
        setPerspective(
            previousPerspective
        )
        setFOV(previousFOV)
    }

    fun resetFront() {
        frontToggled = false
        setPerspective(
            previousPerspective
        )
        setFOV(previousFOV)
    }

    fun resetAll() {
        if (frontToggled) {
            resetFront()
        }
        if (backToggled) {
            resetBack()
        }
    }

    fun setPerspective(value: Int) {
        if (vPerspective == value) return
        previousPerspective = vPerspective
        vPerspective = value

        if (value == 0) {
            end = 0.3f
            if (!BehindYouConfig.animation) {
                mc.gameSettings.thirdPersonView = 0
                realPerspective = 0
                mc.renderGlobal.setDisplayListEntitiesDirty()
            }
        } else {
            end = distance
            mc.gameSettings.thirdPersonView = value
            realPerspective = value
            mc.renderGlobal.setDisplayListEntitiesDirty()
            //TODO: animation = DummyAnimation(0.3f)
        }
    }

    private fun getFOV() = mc.gameSettings.fovSetting

    private fun setFOV(value: Number) {
        mc.gameSettings.fovSetting = value.toFloat()
    }

    @Command(value = ["behindyou", "behindyouv3"], description = "Open the BehindYou config GUI.")
    class BehindYouCommand {
        @Command
        fun main() {
            //TODO: BehindYouConfig.openGui()
        }
    }
}