package com.jagex.game.runetek5.staticmethods

import java.util.Random
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.CheckEmailRequest
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.menu.ColoredMiniMenuEntry
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.SHOW_FACE_HERE
import com.jagex.game.runetek5.client.menu.MiniMenuEntry
import com.jagex.game.runetek5.client.menu.MenuInteraction
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.*
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement
import com.jagex.game.runetek5.config.material.texture.operation.TextureShapeRasterizer
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpRasterizer
import com.jagex.game.runetek5.config.midi.SoundBankCache
import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.config.midi.VorbisNode
import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interpreter
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontCombo
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.hint.HintRenderer
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.network.packet.AbstractMousePacket
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.tile.TileMessage
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.trigger.sound.SoundTrigger
import com.jagex.game.runetek5.utils.*

object StaticMethods {

    @JvmField val unprojectionBuffer: FloatArray = FloatArray(4)
    @JvmField val storedViewProjectionMatrix: Matrix44 = Matrix44()
    @JvmField val INDEX36_FILE_CACHE: IterableNodeMap<VorbisNode> = IterableNodeMap(4)
    @JvmField var WORLD_LIST_VERSION: Int = 0
    @JvmField var menuFontIndex: Int = 0
    @JvmField var menuTopBorderSprite: NativeSprite? = null
    @JvmField var initializedFontcombos: FontCombo? = null
    @JvmField var worldMapViewWidth: Int = 0
    @JvmField var loadingBarTileSprite: NativeSprite? = null
    @JvmField var cameraTargetAbsY: Int = 0
    @JvmField var savedAspectFovMin: Short = 0
    @JvmField var savedAspectFovMax: Short = 0
    @JvmField var savedFovScaleMax: Short = 0
    @JvmField var cutsceneViewportWidth: Int = 0
    @JvmField var hasSavedFov: Boolean = false
    @JvmField var lobbyLoginResponse: Int = -2
    @JvmField var worldLoginResponse: Int = -2
    @JvmField var lastDisconnectResponse: Int = -2
    @JvmField var lastDisconnectArg1: Int = -1
    @JvmField var lastDisconnectArg2: Int = -1
    @JvmField var loginHandshakeCode: Int = 0
    @JvmField var hasStoredCredentials: Boolean = false
    @JvmField var selectedWorldId: Int = -1
    @JvmField var loginUsername: String = ""
    @JvmField var loginPassword: String = ""
    @JvmField var voronoiF2Distance: Int = 0
    @JvmField var initialApplet: Loader? = null
    @JvmField var errorReportUserId: Long = 0
    @JvmField var SPRITES_INDEX: Js5ResourceProvider? = null
    @JvmField var circleInnerRadiusBuffer: IntArray? = null
    @JvmField var pendingSoundBankCache: SoundBankCache? = null
    @JvmField var menuBackgroundAlpha: Int = 0
    @JvmField var menuHoverTextColor: Int = 0
    @JvmField var menuCornerSpriteId: Int = 0
    @JvmField var menuOpenX: Int = 0
    @JvmField var songFadeInRate: Int = 0
    @JvmField var songLooping: Boolean = false
    @JvmField var menuSideBorderSpriteId: Int = 0
    @JvmField var menuBottomRightCornerSprite: NativeSprite? = null
    @JvmField var menuOpenY: Int = 0
    @JvmField var pendingMusicTrack: MusicTrack? = null
    @JvmField var menuBottomLeftCornerSprite: NativeSprite? = null
    @JvmField var menuTopBorderSpriteId: Int = 0
    @JvmField var cameraUpdateCooldown: Int = 0
    @JvmField var loadingBarBackgroundSpriteId: Int = 0

    @JvmStatic
    fun resetSceneAndCaches() {
        Resource.mapRegion!!.resetInteractiveEntities()
        ComponentMinimap.reset()
        TypeLists.resetCachedData(true)
        resetAllVorbisStreams()
        System.gc()
        Renderers.activeToolkit!!.applyClearFlags(2, 0)
    }

    @JvmStatic
    fun drawUnclippedFilledCircle(centerX: Int, centerY: Int, radius: Int, color: Int) {
        var x = 0
        var y = radius
        var error = -radius
        var delta = -1
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![centerY], centerX - radius, centerX + radius, color)
        while (y > x) {
            delta += 2
            error += delta
            ++x
            if (error >= 0) {
                --y
                error -= y shl 1
                val upperRow = TextureShapeRasterizer.pixelBuffer!![y + centerY]
                val lowerRow = TextureShapeRasterizer.pixelBuffer!![centerY - y]
                val right = centerX + x
                val left = centerX - x
                ArrayUtils.setArrayValuesTo(upperRow, left, right, color)
                ArrayUtils.setArrayValuesTo(lowerRow, left, right, color)
            }
            val outerRight = centerX + y
            val outerLeft = centerX - y
            val upperScanline = TextureShapeRasterizer.pixelBuffer!![x + centerY]
            val lowerScanline = TextureShapeRasterizer.pixelBuffer!![centerY - x]
            ArrayUtils.setArrayValuesTo(upperScanline, outerLeft, outerRight, color)
            ArrayUtils.setArrayValuesTo(lowerScanline, outerLeft, outerRight, color)
        }
    }

    @JvmStatic
    fun drawTileMessages(x: Int, y: Int) {
        var msg = Client.TILE_MESSAGES.getFirst() as TileMessage?
        while (msg != null) {
            if (Client.cycles >= msg.lastCycle) {
                msg.unlinkNode()
            } else {
                HintRenderer.calculateHintArrowPlacement(msg.plane, (msg.absoluteX shl 9) + 256, (msg.absoluteY shl 9) + 256, msg.tileHeight * 2)
                MiniMenu.minimenuHeader!!.renderCentered(msg.minimenuText, (x + Client.HINT_POSITION[0]).toInt(), (y + Client.HINT_POSITION[1]).toInt(), 0xffffff.inv() or msg.minimenuColor, 0)
            }
            msg = Client.TILE_MESSAGES.getNext() as TileMessage?
        }
    }

    @JvmStatic
    fun drawStrokedAndFilledRectangle(left: Int, right: Int, top: Int, bottom: Int, fillColor: Int, strokeColor: Int, borderWidth: Int) {
        if (left >= TextureOpRasterizer.clipMinX && right <= TextureOpRasterizer.clipMaxX && top >= TextureOpRasterizer.clipMinY && bottom <= TextureOpRasterizer.clipMaxY) {
            ArrayUtils.drawUnclippedStrokedAndFilledRectangle(left, right, top, bottom, fillColor, strokeColor, borderWidth)
        } else {
            ArrayUtils.drawClippedStrokedAndFilledRectangle(left, right, top, bottom, fillColor, strokeColor, borderWidth)
        }
    }

    @JvmStatic
    fun setPixelBuffer(pixelBuffer: Array<IntArray>?) {
        TextureShapeRasterizer.pixelBuffer = pixelBuffer
    }

    @JvmStatic
    fun millisToJagexDays(millis: Long): Int {
        return ((millis / 86400000L) - 11745).toInt()
    }

    @JvmStatic
    fun drawInterfaces() {
        InterfaceComponentRenderer.deferredDragComponents = null
        Component.drawInterface(Client.BASE_WINDOW_ID, 0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379, 0, 0, -1)
        if (InterfaceComponentRenderer.deferredDragComponents != null) {
            val components = InterfaceComponentRenderer.deferredDragComponents
            val hash = -1412584499
            val originX: Int = 0
            val originY: Int = 0
            val width = GameShell.screenCanvasWidth
            val height = GameShell.screenCanvasHeight * -969250379
            val dragOffsetX = InterfaceComponentRenderer.deferredDragOffsetX
            val dragOffsetY = InterfaceComponentRenderer.deferredDragOffsetY
            val excludeComponentRef: Int = if (Client.selectedComponent == Client.frameComponent) {
                -1
            } else {
                Client.selectedComponent!!.cacheReference
            }

            InterfaceComponentRenderer.drawComponents(components!!, hash, originX, originY, width, height, dragOffsetX, dragOffsetY, excludeComponentRef, true)
            InterfaceComponentRenderer.deferredDragComponents = null
        }
    }

    @JvmStatic
    fun createShadowColorFromAlpha(alpha: Char): Int {
        return 68 or 22528 or 5505024 or ((alpha.code and 0xff) shl 24)
    }

    @JvmStatic
    fun decodeUtf8(data: ByteArray, offset: Int, length: Int): String {
        val chars = CharArray(length)
        var charIndex = 0
        var byteIndex = offset
        val endIndex = length + offset
        while (byteIndex < endIndex) {
            val byte1 = data[byteIndex++].toInt() and 0xff
            val codePoint: Int = if (byte1 < 128) {
                if (byte1 == 0) {
                    65533
                } else {
                    byte1
                }
            } else if (byte1 < 192) {
                65533
            } else if (byte1 < 224) {
                if (byteIndex < endIndex && (data[byteIndex].toInt() and 0xc0) == 128) {
                    val temp = (byte1 and 0x1f) shl 6 or (data[byteIndex++].toInt() and 0x3f)
                    if (temp < 128) {
                        65533
                    } else {
                        temp
                    }
                } else {
                    65533
                }
            } else if (byte1 < 240) {
                if (byteIndex + 1 < endIndex && (data[byteIndex].toInt() and 0xc0) == 128 && (data[byteIndex + 1].toInt() and 0xc0) == 128) {
                    val temp = (byte1 and 0xf) shl 12 or ((data[byteIndex++].toInt() and 0x3f) shl 6) or (data[byteIndex++].toInt() and 0x3f)
                    if (temp < 2048) {
                        65533
                    } else {
                        temp
                    }
                } else {
                    65533
                }
            } else if (byte1 < 248) {
                if (byteIndex + 2 < endIndex && (data[byteIndex].toInt() and 0xc0) == 128 && (data[byteIndex + 1].toInt() and 0xc0) == 128 && (data[byteIndex + 2].toInt() and 0xc0) == 128) {
                    byteIndex++
                    byteIndex++
                    byteIndex++
                    65533
                } else {
                    65533
                }
            } else {
                65533
            }
            chars[charIndex++] = codePoint.toChar()
        }
        return String(chars, 0, charIndex)
    }

    @JvmStatic
    fun flooredMod(randomInteger: Int, limit: Int): Int {
        val offset = randomInteger shr 31 and (limit - 1)
        return (randomInteger + (randomInteger ushr 31)) % limit + offset
    }

    @JvmStatic
    fun isPlayerOption(opcode: Int): Boolean {
        return opcode == 44 || opcode == 45 || opcode == 46 || opcode == 47 || opcode == 48 || opcode == 49 || opcode == 50 || opcode == 51 || opcode == 52 || opcode == 53 || opcode == 15
    }

    @JvmStatic
    fun drawFilledRectangleToPixelBuffer(left: Int, right: Int, top: Int, bottom: Int, color: Int) {
        if (left >= TextureOpRasterizer.clipMinX && right <= TextureOpRasterizer.clipMaxX && top >= TextureOpRasterizer.clipMinY && bottom <= TextureOpRasterizer.clipMaxY) {
            fillRectanglePixels(left, right, top, bottom, color)
        } else {
            ArrayUtils.fillClippedRectangle(left, right, top, bottom, color)
        }
    }

    @JvmStatic
    fun drawMenuBorderSprites(toolkit: AbstractToolkit, x: Int, y: Int, width: Int, height: Int, headerHeight: Int) {
        if ((menuTopBorderSprite == null || StaticMedia.menuLeftSideBorderSprite == null || menuBottomLeftCornerSprite == null) && Resource.SPRITES!!.isFileCached(menuTopBorderSpriteId) && Resource.SPRITES!!.isFileCached(menuSideBorderSpriteId) && Resource.SPRITES!!.isFileCached(menuCornerSpriteId)) {
            val spriteType = SpriteType.listSingle(Resource.SPRITES, menuSideBorderSpriteId, 0)
            StaticMedia.menuLeftSideBorderSprite = toolkit.generateSprite(spriteType, true)
            spriteType!!.flipHorizontal()
            StaticMedia.menuRightSideBorderSprite = toolkit.generateSprite(spriteType, true)
            menuTopBorderSprite = toolkit.generateSprite(SpriteType.listSingle(Resource.SPRITES, menuTopBorderSpriteId, 0), true)
            val class9111 = SpriteType.listSingle(Resource.SPRITES, menuCornerSpriteId, 0)
            menuBottomLeftCornerSprite = toolkit.generateSprite(class9111, true)
            class9111!!.flipHorizontal()
            menuBottomRightCornerSprite = toolkit.generateSprite(class9111, true)
        }
        if (menuTopBorderSprite != null && StaticMedia.menuLeftSideBorderSprite != null && menuBottomLeftCornerSprite != null) {
            val topBorderCount = (width - menuBottomLeftCornerSprite!!.getWidth() * 2) / menuTopBorderSprite!!.getWidth()
            var i: Int
            i = 0
            while (i < topBorderCount) {
                menuTopBorderSprite!!.draw(x + menuBottomLeftCornerSprite!!.getWidth() + i * menuTopBorderSprite!!.getWidth(), y + height - menuTopBorderSprite!!.getHeight())
                i++
            }
            i = (height - headerHeight - menuBottomLeftCornerSprite!!.getHeight()) / StaticMedia.menuLeftSideBorderSprite!!.getHeight()
            for (j in 0 until i) {
                StaticMedia.menuLeftSideBorderSprite!!.draw(x, headerHeight + y + j * StaticMedia.menuLeftSideBorderSprite!!.getHeight())
                StaticMedia.menuRightSideBorderSprite!!.draw(width + x - StaticMedia.menuRightSideBorderSprite!!.getWidth(), headerHeight + y + j * StaticMedia.menuLeftSideBorderSprite!!.getHeight())
            }
            menuBottomLeftCornerSprite!!.draw(x, y + height - menuBottomLeftCornerSprite!!.getHeight())
            menuBottomRightCornerSprite!!.draw(width + x - menuBottomLeftCornerSprite!!.getWidth(), y + height - menuBottomLeftCornerSprite!!.getHeight())
        }
    }

    @JvmStatic
    fun drawStrokedRectangleToPixelBuffer(left: Int, right: Int, top: Int, bottom: Int, color: Int) {
        var topRow = top
        var bottomRow = bottom
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![topRow++], left, right, color)
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![bottomRow--], left, right, color)

        var y = topRow
        while (y <= bottomRow) {
            val scanline = TextureShapeRasterizer.pixelBuffer!![y]
            scanline[left] = color
            scanline[right] = color
            y++
        }
    }

    @JvmStatic
    fun setSongVolume(channel: Int, volume: Int) {
        SongPlayer.setChannelVolume(channel, volume)
    }

    @JvmStatic
    fun isAlphabetic(ch: Char): Boolean {
        return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z'
    }

    @JvmStatic
    fun getCheckEmailStatus(): CheckEmailRequest {
        return AccountCreationStage.checkemailRequeststatus ?: CheckEmailRequest.DEFAULT
    }

    @JvmStatic
    fun fillRectanglePixels(left: Int, right: Int, top: Int, bottom: Int, color: Int) {
        for (y in top..bottom) {
            ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![y], left, right, color)
        }
    }

    @JvmStatic
    fun getWorldMapZoomLevel(): Int {
        return if (WorldMap.requestedMapZoom == 3.0f) 37 else if (WorldMap.requestedMapZoom == 4.0f) 50 else if (WorldMap.requestedMapZoom == 6.0f) 75 else if (WorldMap.requestedMapZoom == 8.0f) 100 else 200
    }

    @JvmStatic
    fun setupOrthoProjection(matrix: Matrix44, centerX: Float, centerY: Float, nearPlane: Float, farPlane: Float, width: Int, length: Int) {
        val viewRangeEnd = Resource.mapRegion!!.viewRangeEnd
        val viewRangeStart = Resource.mapRegion!!.viewRangeStart
        matrix.setPerspective(centerX, centerY, nearPlane, farPlane, viewRangeStart.toFloat(), viewRangeEnd.toFloat(), width.toFloat(), length.toFloat())
    }

    @JvmStatic
    fun calculateSoundAttenuation(baseVolume: Int, distance: Int): Int {
        return if (distance < 0) baseVolume else (baseVolume * Math.sqrt(1.220703125E-4 * (16384 - distance)) + 0.5).toInt()
    }

    @JvmStatic
    fun selectWorldMapElement(elementId: Int) {
        WorldMapSub1.someMecId = elementId
        WorldMapSub1.highlightedCategoryId = -1
        JSObjectManager.highlightFlashCycles = 3
        ServerEnvironment.flashCycleCounter = 100
    }

    @JvmStatic
    fun updateSoundSystems() {
        MusicSample.processAndTrimSamples()
        updateSongFade()
        SoundEffectPlayer.updatePlayers()
        AreaSoundPlayer.updateSpatialMusicVolume()
    }

    @JvmStatic
    fun isPrintableChar(ch: Char): Boolean {
        return ch >= ' ' && ch <= '~' || (ch.code >= 160 && ch.code <= 255 || (ch.code == 8364 || ch.code == 338 || ch.code == 8212 || ch.code == 339 || ch.code == 376))
    }

    @JvmStatic
    fun applyCameraYawLeft() {
        if (!Client.cameraYawVelocityApplied) {
            Client.cameraYawVelocity += (-24.0f - Client.cameraYawVelocity) / 2.0f
            Client.sendCameraAngleToServer = true
            Client.cameraYawVelocityApplied = true
        }
    }

    @JvmStatic
    fun switchToWorldLogin(worldId: Int) {
        if (GameState.isReadyForLogin()) {
            if (worldId != selectedWorldId) {
                LoginManager.socialNetworkToken = -1L
            }

            selectedWorldId = worldId
            Client.LOBBY_CONNECTION_CONTEXT!!.closeConnection()
            Client.setGameState(GameState.UNK_14)
        }
    }

    @JvmStatic
    fun applyCameraPitchDown() {
        if (!Client.cameraPitchVelocityApplied) {
            Client.cameraPitchVelocity += (-12.0f - Client.cameraPitchVelocity) / 2.0f
            Client.sendCameraAngleToServer = true
            Client.cameraPitchVelocityApplied = true
        }
    }

    @JvmStatic
    fun drawFilledCircle(centerX: Int, centerY: Int, radius: Int, color: Int) {
        if (centerX - radius >= TextureOpRasterizer.clipMinX && centerX + radius <= TextureOpRasterizer.clipMaxX && centerY - radius >= TextureOpRasterizer.clipMinY && radius + centerY <= TextureOpRasterizer.clipMaxY) {
            drawUnclippedFilledCircle(centerX, centerY, radius, color)
        } else {
            ArrayUtils.drawClippedFilledCircle(centerX, centerY, radius, color)
        }
    }

    @JvmStatic
    fun setWorldMapScrollTarget(targetX: Int, targetY: Int) {
        WorldMapSub1.scrollTargetX = targetX - WorldMap.mapSquareAbsX
        WorldMapSub1.scrollTargetY = targetY - WorldMap.mapSquareAbsY
    }

    @JvmStatic
    fun queryItemNoParams(name: String, tradeable: Boolean) {
        queryItemParamInt(name, tradeable, -1, -1)
    }

    @JvmStatic
    fun updateSongFade() {
        try {
            if (SoundResourceManager.songLoadState == 1) {
                var volume = SoundResourceManager.midiPcmStream!!.getMusicVolume()
                if (volume > 0 && SoundResourceManager.midiPcmStream!!.dataLoaded()) {
                    volume -= CutsceneBasicMovement.musicFadeStep
                    if (volume < 0) {
                        volume = 0
                    }
                    SoundResourceManager.midiPcmStream!!.setMusicVolume(volume)
                    return
                }
                SoundResourceManager.midiPcmStream!!.turnOffSound()
                SoundResourceManager.midiPcmStream!!.unlinkMusicPatches()
                if (AreaSoundPlayer.MUSIC_RESOURCE != null) {
                    SoundResourceManager.songLoadState = 2
                } else {
                    SoundResourceManager.songLoadState = 0
                }
                pendingMusicTrack = null
                pendingSoundBankCache = null
            }
            if (SoundResourceManager.songLoadState == 3) {
                var volume = SoundResourceManager.midiPcmStream!!.getMusicVolume()
                if (volume < AreaSoundPlayer.SOUND_LEVEL && SoundResourceManager.midiPcmStream!!.dataLoaded()) {
                    volume += songFadeInRate
                    if (volume > AreaSoundPlayer.SOUND_LEVEL) {
                        volume = AreaSoundPlayer.SOUND_LEVEL
                    }
                    SoundResourceManager.midiPcmStream!!.setMusicVolume(volume)
                } else {
                    songFadeInRate = 0
                    SoundResourceManager.songLoadState = 0
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            SoundResourceManager.midiPcmStream!!.turnOffSound()
            SongReference.reset()
        }
    }

    @JvmStatic
    fun calculateSoundPan(basePan: Int, distance: Int): Int {
        return if (distance < 0) -basePan else (basePan * Math.sqrt(1.220703125E-4 * distance) + 0.5).toInt()
    }

    @JvmStatic
    fun saveFovForCutscene() {
        savedAspectFovMin = Client.aspectFovMin
        savedAspectFovMax = Client.aspectFovMax
        AbstractMousePacket.previousFovScaleMin = Client.fovScaleMin
        savedFovScaleMax = Client.fovScaleMax
        hasSavedFov = true
        if (cutsceneViewportWidth != 0 && CutsceneTypeList.cutsceneViewportHeight != 0) {
            Client.fovScaleMin = 334
            Client.fovScaleMax = 334
            Client.aspectFovMax = (cutsceneViewportWidth * 512 / CutsceneTypeList.cutsceneViewportHeight).toShort()
            Client.aspectFovMin = Client.aspectFovMax
        }
    }

    @JvmStatic
    fun isSongTransitioning(): Boolean {
        return SoundResourceManager.songLoadState != 0
    }

    @JvmStatic
    fun processMenuSelection(mouseX: Int, mouseY: Int) {
        if (MiniMenu.deferredClickType == 1) {
            MenuInteraction.processMenuclick(MiniMenu.deferredEntry, mouseX, mouseY)
        } else if (MiniMenu.deferredClickType == 2) {
            openContextMenu(mouseX, mouseY)
        }
        MiniMenu.deferredClickType = 0
        MiniMenu.deferredEntry = null
    }

    @JvmStatic
    fun applyCameraPitchUp() {
        if (!Client.cameraPitchVelocityApplied) {
            Client.cameraPitchVelocity += (12.0f - Client.cameraPitchVelocity) / 2.0f
            Client.sendCameraAngleToServer = true
            Client.cameraPitchVelocityApplied = true
        }
    }

    @JvmStatic
    fun isInLobbyState(state: GameState): Boolean {
        return state == GameState.UNK_7 || state == GameState.UNK_6
    }

    @JvmStatic
    fun drawColoredMenuEntry(mouseX: Int, mouseY: Int, entryX: Int, entryWidth: Int, entryY: Int, coloredEntry: ColoredMiniMenuEntry, fontRenderer: AbstractFontRenderer, fontMetrics: FontMetrics, textColor: Int, hoverColor: Int) {
        var color = textColor
        if (mouseX > entryX && mouseX < entryX + entryWidth && mouseY > entryY - fontMetrics.topPadding - 1 && mouseY < fontMetrics.bottomPadding + entryY) {
            color = hoverColor
        }
        val formattedText = MiniMenu.formatTag(coloredEntry)
        @Suppress("UNCHECKED_CAST")
        fontRenderer.renderAntiMacroText(formattedText, entryX + 3, entryY, color, Client.RANDOMIZER, Client.randomSeed, StaticMedia.chatIcons as Array<NativeSprite>?, StaticMedia.chatScaleHeights)
    }

    @JvmStatic
    fun openContextMenu(mouseX: Int, mouseY: Int) {
        if (Client.ENTITY_DEFAULTS!!.alwaysShowContextMenu || MiniMenu.entryCount != 1 && (!SHOW_FACE_HERE || MiniMenu.entryCount != 2 || !MiniMenu.topEntry!!.action.equals(LocalizedText.FACE_HERE.translate(Client.CURRENT_LANGUAGE!!)))) {
            val fontMetrics = FontMetrics.getForMinimenu()
            var menuWidth = fontMetrics!!.getWidthNoSprites(LocalizedText.CHOOSE_OPTION.translate(Client.CURRENT_LANGUAGE!!)!!)
            val menuHeight: Int
            var entryWidth: Int
            if (!MiniMenu.isCollapsed) {
                var entry = MiniMenu.entryList!!.getFirst() as MiniMenuEntry?
                while (entry != null) {
                    entryWidth = MiniMenu.getWidthForEntryWithQuests(entry, fontMetrics)
                    if (entryWidth > menuWidth) {
                        menuWidth = entryWidth
                    }
                    entry = MiniMenu.entryList!!.next() as MiniMenuEntry?
                }
                menuWidth += 8
                menuHeight = MiniMenu.entryCount * MiniMenu.entryRowHeight + 21
                MiniMenu.menuHeight = (if (FontMetrics.useBoldFont) 26 else 22) + MiniMenu.entryCount * MiniMenu.entryRowHeight
            } else {
                var menuEntry = MiniMenu.sortedColoredEntries!!.getNext() as ColoredMiniMenuEntry?
                while (menuEntry != null) {
                    entryWidth = if (menuEntry.queueSize == 1) {
                        MiniMenu.getWidthForEntryWithQuests(menuEntry.nodeQueue!!.current!!.next as MiniMenuEntry, fontMetrics)
                    } else {
                        MiniMenu.getPrefixTextWidth(menuEntry, fontMetrics)
                    }
                    if (entryWidth > menuWidth) {
                        menuWidth = entryWidth
                    }
                    menuEntry = MiniMenu.sortedColoredEntries!!.getDirectNext() as ColoredMiniMenuEntry?
                }
                menuWidth += 8
                menuHeight = MiniMenu.entryRowHeight * MiniMenu.coloredEntryCount + 21
                MiniMenu.menuHeight = MiniMenu.entryRowHeight * MiniMenu.coloredEntryCount + (if (FontMetrics.useBoldFont) 26 else 22)
            }
            menuWidth += 10
            var menuX = mouseX - menuWidth / 2
            if (menuX + menuWidth > GameShell.screenCanvasWidth) {
                menuX = GameShell.screenCanvasWidth - menuWidth
            }
            if (menuX < 0) {
                menuX = 0
            }
            entryWidth = mouseY
            if (mouseY + menuHeight > GameShell.screenCanvasHeight * -969250379) {
                entryWidth = GameShell.screenCanvasHeight * -969250379 - menuHeight
            }
            if (entryWidth < 0) {
                entryWidth = 0
            }
            menuOpenX = menuX
            menuOpenY = entryWidth
            MiniMenu.menuWidth = menuWidth
            NativeSprite.alphaJitter = (Math.random() * 24.0).toInt()
            MiniMenu.isOpen = true
        }
    }

    @JvmStatic
    fun decodeFriendchatName(input: CharSequence): String {
        var decoded = TextUtils.decodeUidName(Utils.charseqTouid(input))
        if (decoded == null) {
            decoded = ""
        }
        return decoded
    }

    @JvmStatic
    fun boundedRandom(random: Random, limit: Int): Int {
        if (limit <= 0) {
            throw IllegalArgumentException()
        }
        if (OpenGlTexture.isPowerOfTwo(limit)) {
            return ((random.nextInt().toLong() and 0xffffffffL) * limit shr 32).toInt()
        }
        val rejectionThreshold = Integer.MIN_VALUE - (4294967296L % limit).toInt()
        var randomInteger: Int
        do {
            randomInteger = random.nextInt()
        } while (randomInteger >= rejectionThreshold)
        return flooredMod(randomInteger, limit)
    }

    @JvmStatic
    fun resetAllVorbisStreams() {
        var vorbis = INDEX36_FILE_CACHE.first()
        while (vorbis != null) {
            stopVorbisStream(vorbis.fileId)
            vorbis = INDEX36_FILE_CACHE.next()
        }
    }

    @JvmStatic
    fun setVorbisSound(fileId: Int) {
        val vorbis = INDEX36_FILE_CACHE.get(fileId.toLong())
        if (vorbis != null) {
            vorbis.isPaused = !vorbis.isPaused
            vorbis.vfReference!!.togglePause(vorbis.isPaused)
        }
    }

    @JvmStatic
    fun stopVorbisStream(fileId: Int) {
        val varbisNode = INDEX36_FILE_CACHE.get(fileId.toLong())
        if (varbisNode != null) {
            varbisNode.vfReference!!.shutdown()
            SoundTrigger.playVorbis(varbisNode.fileId, varbisNode.sendPacket)
            varbisNode.unlink()
        }
    }

    @JvmStatic
    fun queryItemParamInt(name: String, tradeableOnly: Boolean, paramId: Int, paramIntQuery: Int) {
        CS2Interpreter.queryItem(name, tradeableOnly, paramId, paramIntQuery, null, false)
    }
}
