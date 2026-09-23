package com.jagex.game.runetek5.client.window

import java.awt.*
import java.lang.reflect.Field
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.tools.Browser
import com.jagex.game.runetek5.utils.ArrayUtils

/**
 * [J]
 * @author Steven
 *
 */
class WindowMode @Throws(Exception::class) constructor() {

    var displayMode: DisplayMode? = null
    var graphicsDevice: GraphicsDevice

    init {
        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        graphicsDevice = environment.defaultScreenDevice
        if (!graphicsDevice.isFullScreenSupported) {
            graphicsDevice = environment.screenDevices.firstOrNull { it != null && it.isFullScreenSupported } ?: throw Exception()
        }
    }

    fun getDisplayModes(): IntArray {
        val displayModes = graphicsDevice.displayModes
        val displayModeDetails = IntArray(displayModes.size shl 2)
        for (displayModeIndex in displayModes.indices) {
            displayModeDetails[displayModeIndex shl 2] = displayModes[displayModeIndex].width
            displayModeDetails[(displayModeIndex shl 2) + 1] = displayModes[displayModeIndex].height
            displayModeDetails[(displayModeIndex shl 2) + 2] = displayModes[displayModeIndex].bitDepth
            displayModeDetails[(displayModeIndex shl 2) + 3] = displayModes[displayModeIndex].refreshRate
        }
        return displayModeDetails
    }

    fun setGraphicsDeviceDisplayMode(frame: Frame, displayWidth: Int, displayHeight: Int, currentBitDepth: Int) {
        var newRefreshRate = 0
        displayMode = graphicsDevice.displayMode
        if (displayMode == null) {
            throw NullPointerException()
        }
        frame.isUndecorated = true
        frame.enableInputMethods(false)
        setGraphicsDeviceFullscreen(frame)
        val refreshRate = displayMode!!.refreshRate
        val allDisplayModes = graphicsDevice.displayModes
        var replaceRefreshRate = false
        for (modeIndex in allDisplayModes.indices) {
            if (allDisplayModes[modeIndex].width == displayWidth && allDisplayModes[modeIndex].height == displayHeight && allDisplayModes[modeIndex].bitDepth == currentBitDepth) {
                val displayModeRefreshRate = allDisplayModes[modeIndex].refreshRate
                if (!replaceRefreshRate || Math.abs(displayModeRefreshRate - refreshRate) < Math.abs(newRefreshRate - refreshRate)) {
                    newRefreshRate = displayModeRefreshRate
                    replaceRefreshRate = true
                }
            }
        }
        if (!replaceRefreshRate) {
            newRefreshRate = refreshRate
        }
        graphicsDevice.displayMode = DisplayMode(displayWidth, displayHeight, currentBitDepth, newRefreshRate)
    }

    fun setGraphicsDeviceFullscreen(frame: Frame?) {
        var invalidated = false
        var isValid: Field? = null
        try {
            isValid = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("valid")
            isValid.isAccessible = true
            val setInvalid = (isValid.get(graphicsDevice) as Boolean)
            if (setInvalid) {
                isValid.set(graphicsDevice, java.lang.Boolean.FALSE)
                invalidated = true
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
        try {
            graphicsDevice.fullScreenWindow = frame
            if (invalidated) {
                try {
                    isValid = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("valid")
                    isValid!!.set(graphicsDevice, java.lang.Boolean.TRUE)
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
        } catch (exception: Exception) {
            if (invalidated) {
                try {
                    val valid = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("valid")
                    valid.set(graphicsDevice, java.lang.Boolean.TRUE)
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
        }
    }

    fun destroy() {
        if (displayMode != null) {
            val displayModes = graphicsDevice.displayModes
            var modeAdjusted = false
            for (modeIndex in displayModes.indices) {
                val mode = displayModes[modeIndex]
                if (mode == displayMode) {
                    graphicsDevice.displayMode = displayMode!!
                    modeAdjusted = true
                    break
                }
            }
            if (!modeAdjusted) {
                try {
                    val defaultDisplayMode = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("defaultDisplayMode")
                    defaultDisplayMode.isAccessible = true
                    defaultDisplayMode.set(graphicsDevice, null)
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
            displayMode = null
        }
        setGraphicsDeviceFullscreen(null)
    }

    companion object {
        @JvmField
        var resizableScreenWidth: Int = 0

        @JvmField
        var fullScreenSupported: Boolean = true

        @JvmField
        var fullScreenFrame: Frame? = null

        @JvmField
        var nextDimensionUpdateCheck: Long = 0L

        @JvmField
        var resizableScreenHeight: Int = 0

        @JvmField
        var fullScreenWidth: Int = 0

        @JvmField
        var fullScreenHeight: Int = 0

        @JvmField
        var activeWindowMode: WindowMode? = null

        @JvmField
        @Volatile
        var hasFocus: Boolean = true

        @JvmField
        var fullScreenWindowMode: Boolean = false

        @JvmStatic
        internal var activeDisplayModeDetails: Array<DisplayModeDetails>? = null

        @JvmField
        @Volatile
        var redrawWindowBackground: Boolean = true

        @JvmStatic
        fun calculateFullscreenFrame(windowMode: WindowMode, displayWidth: Int, displayHeight: Int, bitDepth: Int): Frame? {
            var bitDepthLocal = bitDepth
            if (bitDepthLocal == 0) {
                val totalDetails = DisplayModeDetails.forWindowMode(windowMode)
                var found = false
                for (index in totalDetails.indices) {
                    if (totalDetails[index].width == displayWidth && displayHeight == totalDetails[index].height && (!found || totalDetails[index].bitDepth > bitDepthLocal)) {
                        bitDepthLocal = totalDetails[index].bitDepth
                        found = true
                    }
                }
                if (!found) {
                    return null
                }
            }
            val frame = Frame("Jagex Full Screen")
            frame.isResizable = false
            windowMode.setGraphicsDeviceDisplayMode(frame, displayWidth, displayHeight, bitDepthLocal)
            return frame
        }

        @JvmStatic
        fun getActiveDisplayModeDetails(): Array<DisplayModeDetails> {
            if (activeDisplayModeDetails == null) {
                val allDetails = DisplayModeDetails.forWindowMode(activeWindowMode!!)
                val allDetailsRebuilt = arrayOfNulls<DisplayModeDetails>(allDetails.size)
                var detailCount = 0
                val screenSizeType = PreferenceManager.preferences!!.maxScreenSize!!.value
                Cache_Into@ for (detailIndex in allDetails.indices) {
                    val displayDetails = allDetails[detailIndex]
                    if ((displayDetails.bitDepth <= 0 || displayDetails.bitDepth >= 24)
                            && displayDetails.width >= 800 && displayDetails.height >= 600
                            && (screenSizeType != 2 || displayDetails.width <= 800 && displayDetails.height <= 600)
                            && (screenSizeType != 1 || displayDetails.width <= 1024 && displayDetails.height <= 768)) {
                        for (index in 0 until detailCount) {
                            val copy = allDetailsRebuilt[index]!!
                            if (displayDetails.width == copy.width && displayDetails.height == copy.height) {
                                if (displayDetails.bitDepth > copy.bitDepth) {
                                    allDetailsRebuilt[index] = displayDetails
                                }
                                continue@Cache_Into
                            }
                        }
                        allDetailsRebuilt[detailCount] = displayDetails
                        detailCount++
                    }
                }
                activeDisplayModeDetails = Array<DisplayModeDetails>(detailCount) { allDetailsRebuilt[it]!! }
                // System.arraycopy(allDetailsRebuilt, 0, activeDisplayModeDetails, 0, detailCount)
                val detailsArray = activeDisplayModeDetails!!
                val displayDimensions = IntArray(detailsArray.size)
                for (index in detailsArray.indices) {
                    val details = detailsArray[index]
                    displayDimensions[index] = details.height * details.width
                }
                ArrayUtils.quicksort(displayDimensions, detailsArray)
            }
            return activeDisplayModeDetails!!
        }

        /**
         * requested_window_mode
         * 1 = fixed
         * 2 = resizable
         * 3 = fullscreen
         */
        @JvmStatic
        fun requestWindowMode(activeCurrentMode: Int, requestedWindowMode: Int, width: Int, height: Int, isFixed: Boolean) {
            if (fullScreenSupported && fullScreenFrame != null && (requestedWindowMode != 3 || width != fullScreenWidth || height != fullScreenHeight)) {
                disposeFrame(activeWindowMode!!, fullScreenFrame!!)
                fullScreenFrame = null
            }
            println("Mode: $requestedWindowMode")
            if (fullScreenSupported && requestedWindowMode == 3 && fullScreenFrame == null) {
                fullScreenFrame = calculateFullscreenFrame(activeWindowMode!!, width, height, 0)
                if (fullScreenFrame != null) {
                    fullScreenWidth = width
                    fullScreenHeight = height
                    PreferenceManager.savePreferences()
                }
            }
            if (requestedWindowMode == 3 && (!fullScreenSupported || fullScreenFrame == null)) {
                requestWindowMode(activeCurrentMode, PreferenceManager.preferences!!.fixedScreen!!.value, -1, -1, true)
            } else {
                val container = getActiveContainer()
                if (fullScreenFrame != null) {
                    resizableScreenWidth = width
                    resizableScreenHeight = height
                } else if (GameShell.engineFrame != null) {
                    val insets = GameShell.engineFrame!!.insets
                    val widthDelta = insets.left + insets.right
                    resizableScreenWidth = GameShell.engineFrame!!.size.width - widthDelta
                    val heightDelta = insets.bottom + insets.top
                    resizableScreenHeight = GameShell.engineFrame!!.size.height - heightDelta
                } else {
                    resizableScreenWidth = container.size.width
                    resizableScreenHeight = container.size.height
                }
                if (resizableScreenWidth <= 0) {
                    resizableScreenWidth = 1
                }
                if (resizableScreenHeight <= 0) {
                    resizableScreenHeight = 1
                }
                if (requestedWindowMode != 1) {
                    canvasAdjustment()
                } else {
                    GameShell.screenCanvasWidth = Client.GAME_WIDTH
                    GameShell.gameCanvasX = (resizableScreenWidth - Client.GAME_WIDTH) / 2
                    GameShell.screenCanvasHeight = Client.GAME_HEIGHT * -1929118563
                    GameShell.gameCanvasY = 0
                }
                if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE && GameShell.screenCanvasWidth < 1024) {
                    val canvasH = GameShell.screenCanvasHeight
                }
                if (!isFixed) {
                    GameShell.gameCanvas!!.setSize(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                    Renderers.activeToolkit!!.resizeGameCanvas(GameShell.gameCanvas!!, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                    if (container == GameShell.engineFrame) {
                        val insets = GameShell.engineFrame!!.insets
                        GameShell.gameCanvas!!.setLocation(insets.left + GameShell.gameCanvasX, insets.top + GameShell.gameCanvasY)
                    } else {
                        GameShell.gameCanvas!!.setLocation(GameShell.gameCanvasX, GameShell.gameCanvasY)
                    }
                } else {
                    exitFullscreen()
                }
                fullScreenWindowMode = requestedWindowMode >= 2
                if (Client.BASE_WINDOW_ID != -1) {
                    interfaceResize(true)
                }
                if (Client.GAME_CONNECTION_CONTEXT.connection != null && GameState.isRenderingScene(Client.gameState)) {
                    PacketEncoder.sendWindowmodePreferenceToServer()
                }
                for (componentIndex in 0 until 107) {
                    Client.IS_COMPONENT_ON_TOP[componentIndex] = true
                }
                redrawWindowBackground = true
            }
        }

        @JvmStatic
        fun interfaceResize(hasBaseWindowId: Boolean) {
            InterfaceRenderer.updateInterfaceDimensions(Client.BASE_WINDOW_ID, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379, hasBaseWindowId)
        }

        @JvmStatic
        fun getActiveContainer(): Container {
            return if (fullScreenFrame != null) fullScreenFrame!! else if (GameShell.engineFrame != null) GameShell.engineFrame!! else GameShell.suppliedApplet!!
        }

        @JvmStatic
        fun notifyError() {
            if (fullScreenSupported && fullScreenFrame != null) {
                requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.value, -1, -1, false)
            }
            if (GameShell.getApplicationType() == ApplicationType.APPLET) {
                PacketEncoder.writeVarcPreferences()
                println("[WindowMode] Exit")
                System.exit(0)
            } else {
                Browser.openBrowserErrorscreenOpenjs()
            }
        }

        @JvmStatic
        fun requestWindowMode(requestedMode: Int, width: Int, height: Int, isFixed: Boolean) {
            nextDimensionUpdateCheck = 0L
            var isFixedLocal = isFixed
            val activeMode = getWindowMode()
            if (requestedMode == 3 || activeMode == 3) {
                isFixedLocal = true
            }
            if (!Renderers.activeToolkit!!.forceFixedScreenmode()) {
                isFixedLocal = true
            }
            requestWindowMode(activeMode, requestedMode, width, height, isFixedLocal)
        }

        @JvmStatic
        fun disposeFrame(windowMode: WindowMode, frame: Frame) {
            windowMode.destroy()
            frame.isVisible = false
            frame.dispose()
        }

        @JvmStatic
        fun getWindowMode(): Int {
            return if (fullScreenSupported && fullScreenFrame != null) 3 else if (fullScreenWindowMode) 2 else 1
        }

        @JvmStatic
        fun exitFullscreen() {
            if (Renderers.activeToolkit!!.replaceCanvasOnDisplayUpdate()) {
                Renderers.activeToolkit!!.removeCanvas(GameShell.gameCanvas!!)
                Client.buildCanvasComponents()
                val canvasDimension = GameShell.gameCanvas!!.size
                Renderers.activeToolkit!!.ensureImageproducerCreated(GameShell.gameCanvas!!, canvasDimension.width, canvasDimension.height)
                Renderers.activeToolkit!!.setActiveCanvas(GameShell.gameCanvas!!)
            } else {
                Client.loadAssets(PreferenceManager.preferences!!.activeToolkit!!.value, false)
            }
            GameShell.setComponentsOnTop()
        }

        @JvmStatic
        fun fillWindowRect() {
            if (fullScreenFrame == null) {
                val canvasX = GameShell.gameCanvasX
                val canvasY = GameShell.gameCanvasY
                val deltaX = resizableScreenWidth - GameShell.screenCanvasWidth - canvasX
                val deltaY = resizableScreenHeight - GameShell.screenCanvasHeight * -969250379 - canvasY
                if (canvasX > 0 || deltaX > 0 || canvasY > 0 || deltaY > 0) {
                    try {
                        val activeContainer = getActiveContainer()
                        var x = 0
                        var y = 0
                        if (activeContainer == GameShell.engineFrame) {
                            val frameInsets = GameShell.engineFrame!!.insets
                            x = frameInsets.left
                            y = frameInsets.top
                        }
                        val activeGraphics = activeContainer.graphics
                        activeGraphics.color = Color.black
                        if (canvasX > 0) {
                            activeGraphics.fillRect(x, y, canvasX, resizableScreenHeight)
                        }
                        if (canvasY > 0) {
                            activeGraphics.fillRect(x, y, resizableScreenWidth, canvasY)
                        }
                        if (deltaX > 0) {
                            activeGraphics.fillRect(x + resizableScreenWidth - deltaX, y, deltaX, resizableScreenHeight)
                        }
                        if (deltaY > 0) {
                            activeGraphics.fillRect(x, y + resizableScreenHeight - deltaY, resizableScreenWidth, deltaY)
                        }
                    } catch (exception: Exception) {
                        // NOOP
                    }
                }
            }
        }

        @JvmStatic
        fun adjustActiveWindow() {
            synchronized(ClientStartup.activeClient!!) {
                if (fullScreenFrame == null) {
                    val container = getActiveContainer()
                    if (container != null) {
                        resizableScreenWidth = container.size.width
                        resizableScreenHeight = container.size.height
                        if (container == GameShell.engineFrame) {
                            val insets = GameShell.engineFrame!!.insets
                            resizableScreenWidth -= insets.left + insets.right
                            resizableScreenHeight -= insets.bottom + insets.top
                        }
                        if (resizableScreenWidth <= 0) {
                            resizableScreenWidth = 1
                        }
                        if (resizableScreenHeight <= 0) {
                            resizableScreenHeight = 1
                        }
                        if (getWindowMode() != 1) {
                            canvasAdjustment()
                        } else {
                            GameShell.screenCanvasWidth = Client.GAME_WIDTH
                            GameShell.gameCanvasX = (resizableScreenWidth - Client.GAME_WIDTH) / 2
                            GameShell.screenCanvasHeight = Client.GAME_HEIGHT * -1929118563
                            GameShell.gameCanvasY = 0
                        }
                        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE && GameShell.screenCanvasWidth < 1024) {
                            val canvasHeight = GameShell.screenCanvasHeight
                        }
                        GameShell.gameCanvas!!.setSize(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                        val toolkit = Renderers.activeToolkit
                        if (toolkit != null) {
                            toolkit.resizeGameCanvas(GameShell.gameCanvas!!, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                        }
                        if (container == GameShell.engineFrame) {
                            val insets = GameShell.engineFrame!!.insets
                            GameShell.gameCanvas!!.setLocation(insets.left + GameShell.gameCanvasX, insets.top + GameShell.gameCanvasY)
                        } else {
                            GameShell.gameCanvas!!.setLocation(GameShell.gameCanvasX, GameShell.gameCanvasY)
                        }
                        if (Client.BASE_WINDOW_ID != -1) {
                            interfaceResize(true)
                        }
                        fillWindowRect()
                    }
                }
            }
        }

        @JvmStatic
        fun canvasAdjustment() {
            val preferences = PreferenceManager.preferences
            var screensizePreference = 0
            if (preferences != null) {
                screensizePreference = preferences.maxScreenSize!!.value
            }
            if (screensizePreference == 2) {
                val preferedWidth = Math.min(800, resizableScreenWidth)
                val preferedHeight = Math.min(600, resizableScreenHeight)
                GameShell.screenCanvasWidth = preferedWidth
                GameShell.gameCanvasX = (resizableScreenWidth - preferedWidth) / 2
                GameShell.screenCanvasHeight = preferedHeight * -1929118563
                GameShell.gameCanvasY = 0
            } else if (screensizePreference == 1) {
                val preferedWidth = Math.min(1024, resizableScreenWidth)
                val preferedHeight = Math.min(768, resizableScreenHeight)
                GameShell.screenCanvasWidth = preferedWidth
                GameShell.gameCanvasX = (resizableScreenWidth - preferedWidth) / 2
                GameShell.screenCanvasHeight = preferedHeight * -1929118563
                GameShell.gameCanvasY = 0
            } else {
                GameShell.screenCanvasWidth = resizableScreenWidth
                GameShell.gameCanvasX = 0
                GameShell.screenCanvasHeight = resizableScreenHeight * -1929118563
                GameShell.gameCanvasY = 0
            }
        }
    }
}
