package com.jagex.game.runetek5.graphics.loginscreen

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.LoadingStage
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.utils.Utils
import java.awt.*
import java.io.InputStream

/**
 * Draws the vertically centered loading bar in the first seconds of the client starting
 * as to give an indication the client is loading while the login screen assets are
 * yet to display
 * @author Steven - 01/10/2023
 */
class LegacyAssetLoadingScreen : LoginScreenComponent {
    var resourcesUnavailable = false
    var drawInCenterX = false
    var drawInCenterY = false
    var candyBarOffsetX = 0
    var candyBarOffsetY = 0
    var candyBarWidth = 0
    var candyBarHeight = 0
    var anInt3569 = 0
    var anInt3590 = 0
    var anInt3591 = 0
    var anInt3592 = 0
    var font: Font? = null
    var fontMetrics: FontMetrics? = null
    var color: Color? = null
    var candyBarMiddle: Image? = null
    var candyBarBack: Image? = null
    var candyBarOutlineLeft: Image? = null
    var candyBarOutlineRight: Image? = null
    var candyBarOutlineTop: Image? = null
    var candyBarOutlineBottom: Image? = null
    var loadbarBodyLeft: Image? = null
    var loadbarBodyRight: Image? = null
    var loadbarBodyFill: Image? = null
    var candyBarElementCount = 0
    var candyBarImage: Image? = null

    fun getDrawX(width: Int): Int {
        return if (drawInCenterX) (GameShell.screenCanvasWidth - width) / 2 else 0
    }

    fun getDrawY(height: Int): Int {
        return if (drawInCenterY) (GameShell.screenCanvasHeight * -969250379 - height) / 2 else 0
    }

    override fun cleanup() {
        cleanupPlainLoadingbarResources()
    }

    fun getImageFromResource(resourceName: String): Image? {
        return try {
            val buffer: ByteArray
            GameShell.suppliedApplet!!.javaClass.classLoader.getResourceAsStream(resourceName).use { inputstream ->
                buffer = ByteArray(4000)
                if (inputstream == null) return null
                var index = 0
                var data: Int
                while (inputstream.read().also { data = it } != -1) {
                    buffer[index++] = data.toByte()
                }
            }
            Toolkit.getDefaultToolkit().createImage(buffer)
        } catch (exception: Exception) {
            null
        }
    }

    override fun initialize() {
    }

    override fun calculateLoadingPercent(): Int {
        return 100
    }

    override fun finished(long1: Long): Boolean {
        return true
    }

    override fun timeBetweenUpdatesMs(): Int {
        return 0
    }

    override fun draw() {
        if (!resourcesUnavailable && font == null) {
            try {
                val resourceNames = Utils.split(client.loginScreenImageResourceNames!!, ',')
                if (resourceNames.size != 23) {
                    resourcesUnavailable = true
                }
                drawInCenterX = resourceNames[0].toBoolean()
                drawInCenterY = resourceNames[1].toBoolean()
                candyBarOffsetX = Utils.parseInt(resourceNames[2])
                candyBarOffsetY = Utils.parseInt(resourceNames[3])
                candyBarWidth = Utils.parseInt(resourceNames[4])
                candyBarHeight = Utils.parseInt(resourceNames[5])
                anInt3569 = Utils.parseInt(resourceNames[6])
                anInt3590 = Utils.parseInt(resourceNames[7])
                anInt3591 = Utils.parseInt(resourceNames[8])
                anInt3592 = Utils.parseInt(resourceNames[9])
                val fontName = resourceNames[10]
                val size = Utils.parseInt(resourceNames[11])
                val colorValue = Utils.parse_int(resourceNames[12].substring(2))
                font = Font(fontName, Font.PLAIN, size)
                fontMetrics = GameShell.gameCanvas!!.getFontMetrics(font)
                color = Color(colorValue)
                val mediatracker = MediaTracker(GameShell.gameCanvas!!)
                candyBarMiddle = getImageFromResource(resourceNames[13])
                mediatracker.addImage(candyBarMiddle, 0)
                candyBarBack = getImageFromResource(resourceNames[14])
                mediatracker.addImage(candyBarBack, 1)
                candyBarOutlineLeft = getImageFromResource(resourceNames[15])
                mediatracker.addImage(candyBarOutlineLeft, 2)
                candyBarOutlineRight = getImageFromResource(resourceNames[16])
                mediatracker.addImage(candyBarOutlineRight, 3)
                candyBarOutlineTop = getImageFromResource(resourceNames[17])
                mediatracker.addImage(candyBarOutlineTop, 4)
                candyBarOutlineBottom = getImageFromResource(resourceNames[18])
                mediatracker.addImage(candyBarOutlineBottom, 5)
                loadbarBodyLeft = getImageFromResource(resourceNames[19])
                mediatracker.addImage(loadbarBodyLeft, 6)
                loadbarBodyRight = getImageFromResource(resourceNames[20])
                mediatracker.addImage(loadbarBodyRight, 7)
                loadbarBodyFill = getImageFromResource(resourceNames[21])
                mediatracker.addImage(loadbarBodyFill, 8)
                mediatracker.waitForAll()
                candyBarElementCount = Utils.parseInt(resourceNames[22])
            } catch (exception: Exception) {
                resourcesUnavailable = true
            }
        }
        if (resourcesUnavailable) {
            drawPlainLoadingBar()
        } else {
            val graphics = GameShell.gameCanvas!!.graphics
            if (graphics != null) {
                try {
                    val loadPercentage = LoadingStage.plainLoginScreenRenderer!!.getLoadingPercentage()
                    val loadText = LoadingStage.plainLoginScreenRenderer!!.getLoadingText()
                    if (canvasImage == null) {
                        canvasImage = GameShell.gameCanvas!!.createImage(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                    }
                    val canvasImgGraphics = canvasImage!!.graphics
                    canvasImgGraphics.color = Color.BLACK
                    canvasImgGraphics.fillRect(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                    val loadbarLeftWidth = loadbarBodyLeft!!.getWidth(null)
                    val loadbarRightWidth = loadbarBodyRight!!.getWidth(null)
                    val loadbarFillWidth = loadbarBodyFill!!.getWidth(null)
                    val loadbarLeftHeight = loadbarBodyLeft!!.getHeight(null)
                    val loadbarRightHeight = loadbarBodyRight!!.getHeight(null)
                    val loadbarFillHeight = loadbarBodyFill!!.getHeight(null)
                    canvasImgGraphics.drawImage(loadbarBodyLeft, getDrawX(loadbarLeftWidth) + anInt3569 - anInt3591 / 2, getDrawY(loadbarLeftHeight) + anInt3590, null)
                    val i13 = anInt3569 - anInt3591 / 2 + loadbarLeftWidth
                    val i14 = anInt3591 / 2 + anInt3569
                    var i15 = i13
                    while (i15 <= i14) {
                        canvasImgGraphics.drawImage(loadbarBodyFill, getDrawX(loadbarLeftWidth) + anInt3569 + i15, getDrawY(loadbarFillHeight) + anInt3590, null)
                        i15 += loadbarFillWidth
                    }
                    canvasImgGraphics.drawImage(loadbarBodyRight, getDrawX(loadbarRightWidth) + anInt3569 + anInt3591 / 2, getDrawY(loadbarRightHeight) + anInt3590, null)
                    val candyLeftWidth = candyBarOutlineLeft!!.getWidth(null)
                    val candyLeftHeight = candyBarOutlineLeft!!.getHeight(null)
                    val candyRightWidth = candyBarOutlineRight!!.getWidth(null)
                    val candyRightHeight = candyBarOutlineRight!!.getHeight(null)
                    val candyBottomHeight = candyBarOutlineBottom!!.getHeight(null)
                    val candyTopWidth = candyBarOutlineTop!!.getWidth(null)
                    val candyTopHeight = candyBarOutlineTop!!.getHeight(null)
                    val candyBottomWidth = candyBarOutlineBottom!!.getWidth(null)
                    val candyMiddleWidth = candyBarMiddle!!.getWidth(null)
                    val candyBackWidth = candyBarBack!!.getWidth(null)
                    val candyBarX = getDrawX(candyBarWidth) + candyBarOffsetX
                    val candyBarY = getDrawY(candyBarHeight) + candyBarOffsetY
                    canvasImgGraphics.drawImage(candyBarOutlineLeft, candyBarX, candyBarY + (candyBarHeight - candyLeftHeight) / 2, null)
                    canvasImgGraphics.drawImage(candyBarOutlineRight, candyBarX + candyBarWidth - candyRightWidth, candyBarY + (candyBarHeight - candyRightHeight) / 2, null)
                    if (candyBarImage == null) {
                        candyBarImage = GameShell.gameCanvas!!.createImage(candyBarWidth - candyLeftWidth - candyRightWidth, candyBarHeight)
                    }
                    val candyGraphics = candyBarImage!!.graphics
                    var x = 0
                    while (x < candyBarWidth - candyLeftWidth - candyRightWidth) {
                        candyGraphics.drawImage(candyBarOutlineTop, x, 0, null)
                        x += candyTopWidth
                    }
                    x = 0
                    while (x < candyBarWidth - candyLeftWidth - candyRightWidth) {
                        candyGraphics.drawImage(candyBarOutlineBottom, x, candyBarHeight - candyBottomHeight, null)
                        x += candyBottomWidth
                    }
                    val completionWidth = loadPercentage * (candyBarWidth - candyLeftWidth - candyRightWidth) / 100

                    if (completionWidth > 0) {
                        val image29 = GameShell.gameCanvas!!.createImage(completionWidth, candyBarHeight - candyTopHeight - candyBottomHeight)
                        val i30 = image29.getWidth(null)
                        val graphics31 = image29.graphics
                        val i32 = candyBarElementCount * method8740() / 10 % candyMiddleWidth
                        x = i32 - candyMiddleWidth
                        while (x < i30) {
                            graphics31.drawImage(candyBarMiddle, x, 0, null)
                            x += candyMiddleWidth
                        }
                        candyGraphics.drawImage(image29, 0, candyTopHeight, null)
                    }
                    val i38 = completionWidth
                    val remainingWidth = candyBarWidth - candyLeftWidth - candyRightWidth - completionWidth
                    if (remainingWidth > 0) {
                        val completionImage = GameShell.gameCanvas!!.createImage(remainingWidth, candyBarHeight - candyTopHeight - candyBottomHeight)
                        val completionImageWidth = completionImage.getWidth(null)
                        val completionGraphics = completionImage.graphics
                        x = 0
                        while (x < completionImageWidth) {
                            completionGraphics.drawImage(candyBarBack, x, 0, null)
                            x += candyBackWidth
                        }
                        candyGraphics.drawImage(completionImage, i38, candyTopHeight, null)
                    }
                    canvasImgGraphics.drawImage(candyBarImage, candyBarX + candyLeftWidth, candyBarY, null)
                    canvasImgGraphics.font = font
                    canvasImgGraphics.color = color
                    canvasImgGraphics.drawString(loadText, candyBarX + (candyBarWidth - fontMetrics!!.stringWidth(loadText)) / 2, candyBarHeight / 2 + candyBarY + anInt3592 + 4)
                    graphics.drawImage(canvasImage, 0, 0, null)
                } catch (exception43: Exception) {
                    resourcesUnavailable = true
                }
            } else {
                GameShell.gameCanvas!!.repaint()
            }
        }
    }

    /**
     * Draws the loading bar when the login screen image resources are unavailable
     */
    fun drawPlainLoadingBar() {
        drawPlainLoadingBar(
            LoadingStage.plainLoginScreenRenderer!!.getLoadingPercentage(),
            LoadingStage.plainLoginScreenRenderer!!.getLoadingText() ?: "",
            client.DEBUG_LOAD_COLOR[client.DEBUG_DISPLAY_COLOR_INDEX],
            client.DEBUG_BG_COLOR[client.DEBUG_DISPLAY_COLOR_INDEX],
            client.aColorArray7200[client.DEBUG_DISPLAY_COLOR_INDEX]
        )
    }

    companion object {
        @JvmField
        var canvasImage: Image? = null

        @JvmStatic
        fun method8740(): Int {
            return LoadingStage.plainLoginScreenRenderer!!.getCycle()
        }

        @JvmStatic
        fun cleanupPlainLoadingbarResources() {
            canvasImage = null
            FontManager.helvetaBold13Font = null
        }

        /**
         * Draws the loading bar when the login screen image resources are unavailable
         */
        @JvmStatic
        fun drawPlainLoadingBar(loadingPercentage: Int, rectText: String, innerRectColor: Color?, rectBackgroundColor: Color?, textColor: Color?) {
            try {
                val canvasGraphics = GameShell.gameCanvas!!.graphics
                if (FontManager.helvetaBold13Font == null) {
                    FontManager.helvetaBold13Font = Font("Helvetica", Font.BOLD, 13)
                }

                val innerColor = innerRectColor ?: Color(140, 17, 17)
                val bgColor = rectBackgroundColor ?: Color(140, 17, 17)
                val txtColor = textColor ?: Color(255, 255, 255)

                try {
                    if (canvasImage == null) {
                        canvasImage = GameShell.gameCanvas!!.createImage(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                    }

                    val imageGraphics = canvasImage!!.graphics
                    imageGraphics.color = Color.black
                    imageGraphics.fillRect(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                    val x = GameShell.screenCanvasWidth / 2 - 152
                    val y = GameShell.screenCanvasHeight * -969250379 / 2 - 18
                    imageGraphics.color = bgColor
                    imageGraphics.drawRect(x, y, 303, 33)
                    imageGraphics.color = innerColor
                    imageGraphics.fillRect(x + 2, y + 2, loadingPercentage * 3, 30)
                    imageGraphics.color = Color.black
                    imageGraphics.drawRect(x + 1, y + 1, 301, 31)
                    imageGraphics.fillRect(x + loadingPercentage * 3 + 2, y + 2, 300 - loadingPercentage * 3, 30)
                    imageGraphics.font = FontManager.helvetaBold13Font
                    imageGraphics.color = txtColor
                    imageGraphics.drawString(rectText, x + (304 - rectText.length * 6) / 2, y + 22)
                    if (GameShell.translatedLoadingText != null) {
                        imageGraphics.font = FontManager.helvetaBold13Font
                        imageGraphics.color = txtColor
                        imageGraphics.drawString(GameShell.translatedLoadingText, GameShell.screenCanvasWidth / 2 - GameShell.translatedLoadingText!!.length * 6 / 2, GameShell.screenCanvasHeight * -969250379 / 2 - 26)
                    }

                    canvasGraphics.drawImage(canvasImage, 0, 0, null)
                } catch (exception10: Exception) {
                    canvasGraphics.color = Color.black
                    canvasGraphics.fillRect(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                    val x = GameShell.screenCanvasWidth / 2 - 152
                    val y = GameShell.screenCanvasHeight * -969250379 / 2 - 18
                    canvasGraphics.color = bgColor
                    canvasGraphics.drawRect(x, y, 303, 33)
                    canvasGraphics.color = innerColor
                    canvasGraphics.fillRect(x + 2, y + 2, loadingPercentage * 3, 30)
                    canvasGraphics.color = Color.black
                    canvasGraphics.drawRect(x + 1, y + 1, 301, 31)
                    canvasGraphics.fillRect(x + loadingPercentage * 3 + 2, y + 2, 300 - loadingPercentage * 3, 30)
                    canvasGraphics.font = FontManager.helvetaBold13Font
                    canvasGraphics.color = txtColor
                    if (GameShell.translatedLoadingText != null) {
                        canvasGraphics.font = FontManager.helvetaBold13Font
                        canvasGraphics.color = txtColor
                        canvasGraphics.drawString(GameShell.translatedLoadingText, GameShell.screenCanvasWidth / 2 - GameShell.translatedLoadingText!!.length * 6 / 2, GameShell.screenCanvasHeight * -969250379 / 2 - 26)
                    }

                    canvasGraphics.drawString(rectText, x + (304 - rectText.length * 6) / 2, y + 22)
                }
            } catch (exception: Exception) {
                GameShell.gameCanvas!!.repaint()
            }
        }
    }
}
