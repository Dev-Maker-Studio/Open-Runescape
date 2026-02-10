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
    fun drawUnclippedFilledCircle(i_0: Int, i_1: Int, i_2: Int, i_3: Int) {
        var i_5 = 0
        var i_6 = i_2
        var i_7 = -i_2
        var i_8 = -1
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_1], i_0 - i_2, i_0 + i_2, i_3)
        while (i_6 > i_5) {
            i_8 += 2
            i_7 += i_8
            ++i_5
            if (i_7 >= 0) {
                --i_6
                i_7 -= i_6 shl 1
                val ints_9 = TextureShapeRasterizer.pixelBuffer!![i_6 + i_1]
                val ints_10 = TextureShapeRasterizer.pixelBuffer!![i_1 - i_6]
                val i_11 = i_0 + i_5
                val i_12 = i_0 - i_5
                ArrayUtils.setArrayValuesTo(ints_9, i_12, i_11, i_3)
                ArrayUtils.setArrayValuesTo(ints_10, i_12, i_11, i_3)
            }
            val i_13 = i_0 + i_6
            val i_14 = i_0 - i_6
            val ints_15 = TextureShapeRasterizer.pixelBuffer!![i_5 + i_1]
            val ints_16 = TextureShapeRasterizer.pixelBuffer!![i_1 - i_5]
            ArrayUtils.setArrayValuesTo(ints_15, i_14, i_13, i_3)
            ArrayUtils.setArrayValuesTo(ints_16, i_14, i_13, i_3)
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
    fun drawStrokedAndFilledRectangle(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        if (i_0 >= TextureOpRasterizer.clipMinX && i_1 <= TextureOpRasterizer.clipMaxX && i_2 >= TextureOpRasterizer.clipMinY && i_3 <= TextureOpRasterizer.clipMaxY) {
            ArrayUtils.drawUnclippedStrokedAndFilledRectangle(i_0, i_1, i_2, i_3, i_4, i_5, i_6)
        } else {
            ArrayUtils.drawClippedStrokedAndFilledRectangle(i_0, i_1, i_2, i_3, i_4, i_5, i_6)
        }
    }

    @JvmStatic
    fun setPixelBuffer(ints_0: Array<IntArray>?) {
        TextureShapeRasterizer.pixelBuffer = ints_0
    }

    @JvmStatic
    fun millisToJagexDays(long_0: Long): Int {
        return ((long_0 / 86400000L) - 11745).toInt()
    }

    @JvmStatic
    fun drawInterfaces() {
        InterfaceComponentRenderer.deferredDragComponents = null
        Component.drawInterface(Client.BASE_WINDOW_ID, 0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379, 0, 0, -1)
        if (InterfaceComponentRenderer.deferredDragComponents != null) {
            val arr_1 = InterfaceComponentRenderer.deferredDragComponents
            val i_2 = -1412584499
            val b_3: Int = 0
            val b_4: Int = 0
            val i_5 = GameShell.screenCanvasWidth
            val i_6 = GameShell.screenCanvasHeight * -969250379
            val i_7 = InterfaceComponentRenderer.deferredDragOffsetX
            val i_8 = InterfaceComponentRenderer.deferredDragOffsetY
            val i_9: Int = if (Client.selectedComponent == Client.frameComponent) {
                -1
            } else {
                Client.selectedComponent!!.cacheReference
            }

            InterfaceComponentRenderer.drawComponents(arr_1!!, i_2, b_3, b_4, i_5, i_6, i_7, i_8, i_9, true)
            InterfaceComponentRenderer.deferredDragComponents = null
        }
    }

    @JvmStatic
    fun createShadowColorFromAlpha(var_3: Char): Int {
        return 68 or 22528 or 5505024 or ((var_3.code and 0xff) shl 24)
    }

    @JvmStatic
    fun decodeUtf8(bytes_0: ByteArray, i_1: Int, i_2: Int): String {
        val arr_4 = CharArray(i_2)
        var i_5 = 0
        var i_6 = i_1
        val i_7 = i_2 + i_1
        while (i_6 < i_7) {
            val i_8 = bytes_0[i_6++].toInt() and 0xff
            val i_9: Int = if (i_8 < 128) {
                if (i_8 == 0) {
                    65533
                } else {
                    i_8
                }
            } else if (i_8 < 192) {
                65533
            } else if (i_8 < 224) {
                if (i_6 < i_7 && (bytes_0[i_6].toInt() and 0xc0) == 128) {
                    val temp = (i_8 and 0x1f) shl 6 or (bytes_0[i_6++].toInt() and 0x3f)
                    if (temp < 128) {
                        65533
                    } else {
                        temp
                    }
                } else {
                    65533
                }
            } else if (i_8 < 240) {
                if (i_6 + 1 < i_7 && (bytes_0[i_6].toInt() and 0xc0) == 128 && (bytes_0[i_6 + 1].toInt() and 0xc0) == 128) {
                    val temp = (i_8 and 0xf) shl 12 or ((bytes_0[i_6++].toInt() and 0x3f) shl 6) or (bytes_0[i_6++].toInt() and 0x3f)
                    if (temp < 2048) {
                        65533
                    } else {
                        temp
                    }
                } else {
                    65533
                }
            } else if (i_8 < 248) {
                if (i_6 + 2 < i_7 && (bytes_0[i_6].toInt() and 0xc0) == 128 && (bytes_0[i_6 + 1].toInt() and 0xc0) == 128 && (bytes_0[i_6 + 2].toInt() and 0xc0) == 128) {
                    i_6++
                    i_6++
                    i_6++
                    65533
                } else {
                    65533
                }
            } else {
                65533
            }
            arr_4[i_5++] = i_9.toChar()
        }
        return String(arr_4, 0, i_5)
    }

    @JvmStatic
    fun flooredMod(randomInteger: Int, limit: Int): Int {
        val offset = randomInteger shr 31 and (limit - 1)
        return (randomInteger + (randomInteger ushr 31)) % limit + offset
    }

    @JvmStatic
    fun isPlayerOption(i_0: Int): Boolean {
        return i_0 == 44 || i_0 == 45 || i_0 == 46 || i_0 == 47 || i_0 == 48 || i_0 == 49 || i_0 == 50 || i_0 == 51 || i_0 == 52 || i_0 == 53 || i_0 == 15
    }

    @JvmStatic
    fun drawFilledRectangleToPixelBuffer(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        if (i_0 >= TextureOpRasterizer.clipMinX && i_1 <= TextureOpRasterizer.clipMaxX && i_2 >= TextureOpRasterizer.clipMinY && i_3 <= TextureOpRasterizer.clipMaxY) {
            fillRectanglePixels(i_0, i_1, i_2, i_3, i_4)
        } else {
            ArrayUtils.fillClippedRectangle(i_0, i_1, i_2, i_3, i_4)
        }
    }

    @JvmStatic
    fun drawMenuBorderSprites(toolkit: AbstractToolkit, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        if ((menuTopBorderSprite == null || StaticMedia.aNativeSprite5339 == null || menuBottomLeftCornerSprite == null) && Resource.SPRITES!!.isFileCached(menuTopBorderSpriteId) && Resource.SPRITES!!.isFileCached(menuSideBorderSpriteId) && Resource.SPRITES!!.isFileCached(menuCornerSpriteId)) {
            val spriteType = SpriteType.listSingle(Resource.SPRITES, menuSideBorderSpriteId, 0)
            StaticMedia.aNativeSprite5339 = toolkit.generateSprite(spriteType, true)
            spriteType!!.flipHorizontal()
            StaticMedia.aNativeSprite3452 = toolkit.generateSprite(spriteType, true)
            menuTopBorderSprite = toolkit.generateSprite(SpriteType.listSingle(Resource.SPRITES, menuTopBorderSpriteId, 0), true)
            val class9111 = SpriteType.listSingle(Resource.SPRITES, menuCornerSpriteId, 0)
            menuBottomLeftCornerSprite = toolkit.generateSprite(class9111, true)
            class9111!!.flipHorizontal()
            menuBottomRightCornerSprite = toolkit.generateSprite(class9111, true)
        }
        if (menuTopBorderSprite != null && StaticMedia.aNativeSprite5339 != null && menuBottomLeftCornerSprite != null) {
            val i_7 = (i_3 - menuBottomLeftCornerSprite!!.getWidth() * 2) / menuTopBorderSprite!!.getWidth()
            var i_8: Int
            i_8 = 0
            while (i_8 < i_7) {
                menuTopBorderSprite!!.draw(i_1 + menuBottomLeftCornerSprite!!.getWidth() + i_8 * menuTopBorderSprite!!.getWidth(), i_2 + i_4 - menuTopBorderSprite!!.getHeight())
                i_8++
            }
            i_8 = (i_4 - i_5 - menuBottomLeftCornerSprite!!.getHeight()) / StaticMedia.aNativeSprite5339!!.getHeight()
            for (i_9 in 0 until i_8) {
                StaticMedia.aNativeSprite5339!!.draw(i_1, i_5 + i_2 + i_9 * StaticMedia.aNativeSprite5339!!.getHeight())
                StaticMedia.aNativeSprite3452!!.draw(i_3 + i_1 - StaticMedia.aNativeSprite3452!!.getWidth(), i_5 + i_2 + i_9 * StaticMedia.aNativeSprite5339!!.getHeight())
            }
            menuBottomLeftCornerSprite!!.draw(i_1, i_2 + i_4 - menuBottomLeftCornerSprite!!.getHeight())
            menuBottomRightCornerSprite!!.draw(i_3 + i_1 - menuBottomLeftCornerSprite!!.getWidth(), i_2 + i_4 - menuBottomLeftCornerSprite!!.getHeight())
        }
    }

    @JvmStatic
    fun drawStrokedRectangleToPixelBuffer(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        var i_2Var = i_2
        var i_3Var = i_3
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_2Var++], i_0, i_1, i_4)
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_3Var--], i_0, i_1, i_4)

        var i_6 = i_2Var
        while (i_6 <= i_3Var) {
            val ints_7 = TextureShapeRasterizer.pixelBuffer!![i_6]
            ints_7[i_0] = i_4
            ints_7[i_1] = i_4
            i_6++
        }
    }

    @JvmStatic
    fun setSongVolume(i_0: Int, i_1: Int) {
        SongPlayer.method2580(i_0, i_1)
    }

    @JvmStatic
    fun isAlphabetic(var_0: Char): Boolean {
        return var_0 >= 'A' && var_0 <= 'Z' || var_0 >= 'a' && var_0 <= 'z'
    }

    @JvmStatic
    fun getCheckEmailStatus(): CheckEmailRequest {
        return AccountCreationStage.checkemailRequeststatus ?: CheckEmailRequest.DEFAULT
    }

    @JvmStatic
    fun fillRectanglePixels(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        for (i_6 in i_2..i_3) {
            ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![i_6], i_0, i_1, i_4)
        }
    }

    @JvmStatic
    fun getWorldMapZoomLevel(): Int {
        return if (WorldMap.requestedMapZoom == 3.0f) 37 else if (WorldMap.requestedMapZoom == 4.0f) 50 else if (WorldMap.requestedMapZoom == 6.0f) 75 else if (WorldMap.requestedMapZoom == 8.0f) 100 else 200
    }

    @JvmStatic
    fun setupOrthoProjection(matrix440: Matrix44, centerX: Float, centerY: Float, f_4: Float, f_5: Float, width: Int, length: Int) {
        val viewRangeEnd = Resource.mapRegion!!.viewRangeEnd
        val viewRangeStart = Resource.mapRegion!!.viewRangeStart
        matrix440.setPerspective(centerX, centerY, f_4, f_5, viewRangeStart.toFloat(), viewRangeEnd.toFloat(), width.toFloat(), length.toFloat())
    }

    @JvmStatic
    fun calculateSoundAttenuation(i_0: Int, i_1: Int): Int {
        return if (i_1 < 0) i_0 else (i_0 * Math.sqrt(1.220703125E-4 * (16384 - i_1)) + 0.5).toInt()
    }

    @JvmStatic
    fun selectWorldMapElement(i_0: Int) {
        WorldMapSub1.someMecId = i_0
        WorldMapSub1.highlightedCategoryId = -1
        JSObjectManager.anInt4185 = 3
        ServerEnvironment.flashCycleCounter = 100
    }

    @JvmStatic
    fun updateSoundSystems() {
        MusicSample.method7042()
        updateSongFade()
        SoundEffectPlayer.updatePlayers()
        AreaSoundPlayer.updateSpatialMusicVolume()
    }

    @JvmStatic
    fun isPrintableChar(var_0: Char): Boolean {
        return var_0 >= ' ' && var_0 <= '~' || (var_0.code >= 160 && var_0.code <= 255 || (var_0.code == 8364 || var_0.code == 338 || var_0.code == 8212 || var_0.code == 339 || var_0.code == 376))
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
    fun switchToWorldLogin(i_0: Int) {
        if (GameState.method6693()) {
            if (i_0 != selectedWorldId) {
                LoginManager.socialNetworkToken = -1L
            }

            selectedWorldId = i_0
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
    fun drawFilledCircle(i_0: Int, i_1: Int, i_2: Int, i_3: Int) {
        if (i_0 - i_2 >= TextureOpRasterizer.clipMinX && i_0 + i_2 <= TextureOpRasterizer.clipMaxX && i_1 - i_2 >= TextureOpRasterizer.clipMinY && i_2 + i_1 <= TextureOpRasterizer.clipMaxY) {
            drawUnclippedFilledCircle(i_0, i_1, i_2, i_3)
        } else {
            ArrayUtils.drawClippedFilledCircle(i_0, i_1, i_2, i_3)
        }
    }

    @JvmStatic
    fun setWorldMapScrollTarget(i_0: Int, i_1: Int) {
        WorldMapSub1.scrollTargetX = i_0 - WorldMap.mapSquareAbsX
        WorldMapSub1.scrollTargetY = i_1 - WorldMap.mapSquareAbsY
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
        } catch (exception3: Exception) {
            exception3.printStackTrace()
            SoundResourceManager.midiPcmStream!!.turnOffSound()
            SongReference.reset()
        }
    }

    @JvmStatic
    fun calculateSoundPan(i_0: Int, i_1: Int): Int {
        return if (i_1 < 0) -i_0 else (i_0 * Math.sqrt(1.220703125E-4 * i_1) + 0.5).toInt()
    }

    @JvmStatic
    fun saveFovForCutscene() {
        savedAspectFovMin = Client.aspectFovMin
        savedAspectFovMax = Client.aspectFovMax
        AbstractMousePacket.aShort7079 = Client.fovScaleMin
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
    fun processMenuSelection(i_0: Int, i_1: Int) {
        if (MiniMenu.deferredClickType == 1) {
            MenuInteraction.processMenuclick(MiniMenu.deferredEntry, i_0, i_1)
        } else if (MiniMenu.deferredClickType == 2) {
            openContextMenu(i_0, i_1)
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
    fun isInLobbyState(i_0: GameState): Boolean {
        return i_0 == GameState.UNK_7 || i_0 == GameState.UNK_6
    }

    @JvmStatic
    fun drawColoredMenuEntry(i_0: Int, i_1: Int, i_2: Int, i_4: Int, i_6: Int, class282Sub50Sub157: ColoredMiniMenuEntry, fontrenderer8: AbstractFontRenderer, fontmetrics9: FontMetrics, i_10: Int, i_11: Int) {
        var i_101 = i_10
        if (i_0 > i_2 && i_0 < i_2 + i_4 && i_1 > i_6 - fontmetrics9.topPadding - 1 && i_1 < fontmetrics9.bottomPadding + i_6) {
            i_101 = i_11
        }
        val string_13 = MiniMenu.formatTag(class282Sub50Sub157)
        @Suppress("UNCHECKED_CAST")
        fontrenderer8.renderAntiMacroText(string_13, i_2 + 3, i_6, i_101, Client.RANDOMIZER, Client.randomSeed, StaticMedia.chatIcons as Array<NativeSprite>?, StaticMedia.chatScaleHeights)
    }

    @JvmStatic
    fun openContextMenu(mouseX: Int, mouseY: Int) {
        if (Client.ENTITY_DEFAULTS!!.aBool7044 || MiniMenu.entryCount != 1 && (!SHOW_FACE_HERE || MiniMenu.entryCount != 2 || !MiniMenu.topEntry!!.action.equals(LocalizedText.FACE_HERE.translate(Client.CURRENT_LANGUAGE!!)))) {
            val fontMetrics = FontMetrics.getForMinimenu()
            var i_4 = fontMetrics!!.getWidthNoSprites(LocalizedText.CHOOSE_OPTION.translate(Client.CURRENT_LANGUAGE!!)!!)
            val i_5: Int
            var i_7: Int
            if (!MiniMenu.isCollapsed) {
                var class282Sub50Sub79 = MiniMenu.entryList!!.getFirst() as MiniMenuEntry?
                while (class282Sub50Sub79 != null) {
                    i_7 = MiniMenu.getWidthForEntryWithQuests(class282Sub50Sub79, fontMetrics)
                    if (i_7 > i_4) {
                        i_4 = i_7
                    }
                    class282Sub50Sub79 = MiniMenu.entryList!!.next() as MiniMenuEntry?
                }
                i_4 += 8
                i_5 = MiniMenu.entryCount * MiniMenu.entryRowHeight + 21
                MiniMenu.menuHeight = (if (FontMetrics.useBoldFont) 26 else 22) + MiniMenu.entryCount * MiniMenu.entryRowHeight
            } else {
                var menuEntry = MiniMenu.sortedColoredEntries!!.getNext() as ColoredMiniMenuEntry?
                while (menuEntry != null) {
                    i_7 = if (menuEntry.queueSize == 1) {
                        MiniMenu.getWidthForEntryWithQuests(menuEntry.nodeQueue!!.current!!.next as MiniMenuEntry, fontMetrics)
                    } else {
                        MiniMenu.getPrefixTextWidth(menuEntry, fontMetrics)
                    }
                    if (i_7 > i_4) {
                        i_4 = i_7
                    }
                    menuEntry = MiniMenu.sortedColoredEntries!!.getDirectNext() as ColoredMiniMenuEntry?
                }
                i_4 += 8
                i_5 = MiniMenu.entryRowHeight * MiniMenu.coloredEntryCount + 21
                MiniMenu.menuHeight = MiniMenu.entryRowHeight * MiniMenu.coloredEntryCount + (if (FontMetrics.useBoldFont) 26 else 22)
            }
            i_4 += 10
            var i_8 = mouseX - i_4 / 2
            if (i_8 + i_4 > GameShell.screenCanvasWidth) {
                i_8 = GameShell.screenCanvasWidth - i_4
            }
            if (i_8 < 0) {
                i_8 = 0
            }
            i_7 = mouseY
            if (mouseY + i_5 > GameShell.screenCanvasHeight * -969250379) {
                i_7 = GameShell.screenCanvasHeight * -969250379 - i_5
            }
            if (i_7 < 0) {
                i_7 = 0
            }
            menuOpenX = i_8
            menuOpenY = i_7
            MiniMenu.menuWidth = i_4
            NativeSprite.alphaJitter = (Math.random() * 24.0).toInt()
            MiniMenu.isOpen = true
        }
    }

    @JvmStatic
    fun decodeFriendchatName(charsequence_0: CharSequence): String {
        var string_2 = TextUtils.decodeUidName(Utils.charseqTouid(charsequence_0))
        if (string_2 == null) {
            string_2 = ""
        }
        return string_2
    }

    @JvmStatic
    fun boundedRandom(random: Random, limit: Int): Int {
        if (limit <= 0) {
            throw IllegalArgumentException()
        }
        if (OpenGlTexture.isPowerOfTwo(limit)) {
            return ((random.nextInt().toLong() and 0xffffffffL) * limit shr 32).toInt()
        }
        val i_3 = Integer.MIN_VALUE - (4294967296L % limit).toInt()
        var randomInteger: Int
        do {
            randomInteger = random.nextInt()
        } while (randomInteger >= i_3)
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
            vorbis.aBool7995 = !vorbis.aBool7995
            vorbis.vfReference!!.togglePause(vorbis.aBool7995)
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
