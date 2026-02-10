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

    @JvmField val aFloatArray194: FloatArray = FloatArray(4)
    @JvmField val aClass384185: Matrix44 = Matrix44()
    @JvmField val INDEX36_FILE_CACHE: IterableNodeMap<VorbisNode> = IterableNodeMap(4)
    @JvmField var WORLD_LIST_VERSION: Int = 0
    @JvmField var anInt2982: Int = 0
    @JvmField var aNativeSprite4110: NativeSprite? = null
    @JvmField var initializedFontcombos: FontCombo? = null
    @JvmField var anInt5760: Int = 0
    @JvmField var aNativeSprite612: NativeSprite? = null
    @JvmField var anInt833: Int = 0
    @JvmField var aShort828: Short = 0
    @JvmField var aShort829: Short = 0
    @JvmField var aShort830: Short = 0
    @JvmField var anInt824: Int = 0
    @JvmField var aBool826: Boolean = false
    @JvmField var anInt72: Int = -2
    @JvmField var anInt106: Int = -2
    @JvmField var anInt107: Int = -2
    @JvmField var anInt108: Int = -1
    @JvmField var anInt109: Int = -1
    @JvmField var anInt113: Int = 0
    @JvmField var aBool74: Boolean = false
    @JvmField var anInt76: Int = -1
    @JvmField var aString99: String = ""
    @JvmField var aString102: String = ""
    @JvmField var anInt118: Int = 0
    @JvmField var initialApplet: Loader? = null
    @JvmField var aLong10459: Long = 0
    @JvmField var SPRITES_INDEX: Js5ResourceProvider? = null
    @JvmField var anIntArray36: IntArray? = null
    @JvmField var aClass2505450: SoundBankCache? = null
    @JvmField var anInt2262: Int = 0
    @JvmField var anInt1005: Int = 0
    @JvmField var anInt148: Int = 0
    @JvmField var anInt3555: Int = 0
    @JvmField var anInt4664: Int = 0
    @JvmField var aBool1962: Boolean = false
    @JvmField var anInt5396: Int = 0
    @JvmField var aNativeSprite9315: NativeSprite? = null
    @JvmField var anInt2300: Int = 0
    @JvmField var aNodeSub79563: MusicTrack? = null
    @JvmField var aNativeSprite1571: NativeSprite? = null
    @JvmField var anInt3145: Int = 0
    @JvmField var anInt2912: Int = 0
    @JvmField var anInt5438: Int = 0

    @JvmStatic
    fun method6907() {
        Resource.mapRegion!!.resetInteractiveEntities()
        ComponentMinimap.reset()
        TypeLists.resetCachedData(true)
        method4250()
        System.gc()
        Renderers.activeToolkit!!.applyClearFlags(2, 0)
    }

    @JvmStatic
    fun method813(i_0: Int, i_1: Int, i_2: Int, i_3: Int) {
        var i_5 = 0
        var i_6 = i_2
        var i_7 = -i_2
        var i_8 = -1
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_1], i_0 - i_2, i_0 + i_2, i_3)
        while (i_6 > i_5) {
            i_8 += 2
            i_7 += i_8
            ++i_5
            if (i_7 >= 0) {
                --i_6
                i_7 -= i_6 shl 1
                val ints_9 = TextureShapeRasterizer.anIntArrayArray7072!![i_6 + i_1]
                val ints_10 = TextureShapeRasterizer.anIntArrayArray7072!![i_1 - i_6]
                val i_11 = i_0 + i_5
                val i_12 = i_0 - i_5
                ArrayUtils.setArrayValuesTo(ints_9, i_12, i_11, i_3)
                ArrayUtils.setArrayValuesTo(ints_10, i_12, i_11, i_3)
            }
            val i_13 = i_0 + i_6
            val i_14 = i_0 - i_6
            val ints_15 = TextureShapeRasterizer.anIntArrayArray7072!![i_5 + i_1]
            val ints_16 = TextureShapeRasterizer.anIntArrayArray7072!![i_1 - i_5]
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
    fun method6731(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        if (i_0 >= TextureOpRasterizer.anInt7071 && i_1 <= TextureOpRasterizer.anInt7069 && i_2 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068) {
            ArrayUtils.method3230(i_0, i_1, i_2, i_3, i_4, i_5, i_6)
        } else {
            ArrayUtils.method4034(i_0, i_1, i_2, i_3, i_4, i_5, i_6)
        }
    }

    @JvmStatic
    fun method3936(ints_0: Array<IntArray>?) {
        TextureShapeRasterizer.anIntArrayArray7072 = ints_0
    }

    @JvmStatic
    fun method7914(long_0: Long): Int {
        return ((long_0 / 86400000L) - 11745).toInt()
    }

    @JvmStatic
    fun method7039() {
        InterfaceComponentRenderer.aClass118Array3772 = null
        Component.method3629(Client.BASE_WINDOW_ID, 0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379, 0, 0, -1)
        if (InterfaceComponentRenderer.aClass118Array3772 != null) {
            val arr_1 = InterfaceComponentRenderer.aClass118Array3772
            val i_2 = -1412584499
            val b_3: Int = 0
            val b_4: Int = 0
            val i_5 = GameShell.screenCanvasWidth
            val i_6 = GameShell.screenCanvasHeight * -969250379
            val i_7 = InterfaceComponentRenderer.anInt4822
            val i_8 = InterfaceComponentRenderer.anInt5157
            val i_9: Int = if (Client.selectedComponent == Client.frameComponent) {
                -1
            } else {
                Client.selectedComponent!!.cacheReference
            }

            InterfaceComponentRenderer.drawComponents(arr_1!!, i_2, b_3, b_4, i_5, i_6, i_7, i_8, i_9, true)
            InterfaceComponentRenderer.aClass118Array3772 = null
        }
    }

    @JvmStatic
    fun method755(var_3: Char): Int {
        return 68 or 22528 or 5505024 or ((var_3.code and 0xff) shl 24)
    }

    @JvmStatic
    fun method4824(bytes_0: ByteArray, i_1: Int, i_2: Int): String {
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
    fun method5360(randomInteger: Int, limit: Int): Int {
        val offset = randomInteger shr 31 and (limit - 1)
        return (randomInteger + (randomInteger ushr 31)) % limit + offset
    }

    @JvmStatic
    fun method6176(i_0: Int): Boolean {
        return i_0 == 44 || i_0 == 45 || i_0 == 46 || i_0 == 47 || i_0 == 48 || i_0 == 49 || i_0 == 50 || i_0 == 51 || i_0 == 52 || i_0 == 53 || i_0 == 15
    }

    @JvmStatic
    fun method6772(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        if (i_0 >= TextureOpRasterizer.anInt7071 && i_1 <= TextureOpRasterizer.anInt7069 && i_2 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068) {
            method1564(i_0, i_1, i_2, i_3, i_4)
        } else {
            ArrayUtils.method7728(i_0, i_1, i_2, i_3, i_4)
        }
    }

    @JvmStatic
    fun method8209(toolkit: AbstractToolkit, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        if ((aNativeSprite4110 == null || StaticMedia.aNativeSprite5339 == null || aNativeSprite1571 == null) && Resource.SPRITES!!.isFileCached(anInt3145) && Resource.SPRITES!!.isFileCached(anInt5396) && Resource.SPRITES!!.isFileCached(anInt148)) {
            val spriteType = SpriteType.listSingle(Resource.SPRITES, anInt5396, 0)
            StaticMedia.aNativeSprite5339 = toolkit.generateSprite(spriteType, true)
            spriteType!!.flipHorizontal()
            StaticMedia.aNativeSprite3452 = toolkit.generateSprite(spriteType, true)
            aNativeSprite4110 = toolkit.generateSprite(SpriteType.listSingle(Resource.SPRITES, anInt3145, 0), true)
            val class9111 = SpriteType.listSingle(Resource.SPRITES, anInt148, 0)
            aNativeSprite1571 = toolkit.generateSprite(class9111, true)
            class9111!!.flipHorizontal()
            aNativeSprite9315 = toolkit.generateSprite(class9111, true)
        }
        if (aNativeSprite4110 != null && StaticMedia.aNativeSprite5339 != null && aNativeSprite1571 != null) {
            val i_7 = (i_3 - aNativeSprite1571!!.getWidth() * 2) / aNativeSprite4110!!.getWidth()
            var i_8: Int
            i_8 = 0
            while (i_8 < i_7) {
                aNativeSprite4110!!.draw(i_1 + aNativeSprite1571!!.getWidth() + i_8 * aNativeSprite4110!!.getWidth(), i_2 + i_4 - aNativeSprite4110!!.getHeight())
                i_8++
            }
            i_8 = (i_4 - i_5 - aNativeSprite1571!!.getHeight()) / StaticMedia.aNativeSprite5339!!.getHeight()
            for (i_9 in 0 until i_8) {
                StaticMedia.aNativeSprite5339!!.draw(i_1, i_5 + i_2 + i_9 * StaticMedia.aNativeSprite5339!!.getHeight())
                StaticMedia.aNativeSprite3452!!.draw(i_3 + i_1 - StaticMedia.aNativeSprite3452!!.getWidth(), i_5 + i_2 + i_9 * StaticMedia.aNativeSprite5339!!.getHeight())
            }
            aNativeSprite1571!!.draw(i_1, i_2 + i_4 - aNativeSprite1571!!.getHeight())
            aNativeSprite9315!!.draw(i_3 + i_1 - aNativeSprite1571!!.getWidth(), i_2 + i_4 - aNativeSprite1571!!.getHeight())
        }
    }

    @JvmStatic
    fun method14572(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        var i_2Var = i_2
        var i_3Var = i_3
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_2Var++], i_0, i_1, i_4)
        ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_3Var--], i_0, i_1, i_4)

        var i_6 = i_2Var
        while (i_6 <= i_3Var) {
            val ints_7 = TextureShapeRasterizer.anIntArrayArray7072!![i_6]
            ints_7[i_0] = i_4
            ints_7[i_1] = i_4
            i_6++
        }
    }

    @JvmStatic
    fun method14501(i_0: Int, i_1: Int) {
        SongPlayer.method2580(i_0, i_1)
    }

    @JvmStatic
    fun method1169(var_0: Char): Boolean {
        return var_0 >= 'A' && var_0 <= 'Z' || var_0 >= 'a' && var_0 <= 'z'
    }

    @JvmStatic
    fun method14668(): CheckEmailRequest {
        return AccountCreationStage.checkemailRequeststatus ?: CheckEmailRequest.DEFAULT
    }

    @JvmStatic
    fun method1564(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        for (i_6 in i_2..i_3) {
            ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.anIntArrayArray7072!![i_6], i_0, i_1, i_4)
        }
    }

    @JvmStatic
    fun method304(): Int {
        return if (WorldMap.requestedMapZoom == 3.0f) 37 else if (WorldMap.requestedMapZoom == 4.0f) 50 else if (WorldMap.requestedMapZoom == 6.0f) 75 else if (WorldMap.requestedMapZoom == 8.0f) 100 else 200
    }

    @JvmStatic
    fun method3850(matrix440: Matrix44, centerX: Float, centerY: Float, f_4: Float, f_5: Float, width: Int, length: Int) {
        val viewRangeEnd = Resource.mapRegion!!.viewRangeEnd
        val viewRangeStart = Resource.mapRegion!!.viewRangeStart
        matrix440.method6531(centerX, centerY, f_4, f_5, viewRangeStart.toFloat(), viewRangeEnd.toFloat(), width.toFloat(), length.toFloat())
    }

    @JvmStatic
    fun method488(i_0: Int, i_1: Int): Int {
        return if (i_1 < 0) i_0 else (i_0 * Math.sqrt(1.220703125E-4 * (16384 - i_1)) + 0.5).toInt()
    }

    @JvmStatic
    fun method458(i_0: Int) {
        WorldMapSub1.someMecId = i_0
        WorldMapSub1.anInt8026 = -1
        JSObjectManager.anInt4185 = 3
        ServerEnvironment.anInt9282 = 100
    }

    @JvmStatic
    fun method8020() {
        MusicSample.method7042()
        method15420()
        SoundEffectPlayer.updatePlayers()
        AreaSoundPlayer.method5961()
    }

    @JvmStatic
    fun method12402(var_0: Char): Boolean {
        return var_0 >= ' ' && var_0 <= '~' || (var_0.code >= 160 && var_0.code <= 255 || (var_0.code == 8364 || var_0.code == 338 || var_0.code == 8212 || var_0.code == 339 || var_0.code == 376))
    }

    @JvmStatic
    fun method8146() {
        if (!Client.cameraYawVelocityApplied) {
            Client.cameraYawVelocity += (-24.0f - Client.cameraYawVelocity) / 2.0f
            Client.sendCameraAngleToServer = true
            Client.cameraYawVelocityApplied = true
        }
    }

    @JvmStatic
    fun method15430(i_0: Int) {
        if (GameState.method6693()) {
            if (i_0 != anInt76) {
                LoginManager.aLong77 = -1L
            }

            anInt76 = i_0
            Client.LOBBY_CONNECTION_CONTEXT!!.closeConnection()
            Client.setGameState(GameState.UNK_14)
        }
    }

    @JvmStatic
    fun method16114() {
        if (!Client.cameraPitchVelocityApplied) {
            Client.cameraPitchVelocity += (-12.0f - Client.cameraPitchVelocity) / 2.0f
            Client.sendCameraAngleToServer = true
            Client.cameraPitchVelocityApplied = true
        }
    }

    @JvmStatic
    fun method6292(i_0: Int, i_1: Int, i_2: Int, i_3: Int) {
        if (i_0 - i_2 >= TextureOpRasterizer.anInt7071 && i_0 + i_2 <= TextureOpRasterizer.anInt7069 && i_1 - i_2 >= TextureOpRasterizer.anInt7070 && i_2 + i_1 <= TextureOpRasterizer.anInt7068) {
            method813(i_0, i_1, i_2, i_3)
        } else {
            ArrayUtils.method4866(i_0, i_1, i_2, i_3)
        }
    }

    @JvmStatic
    fun method14263(i_0: Int, i_1: Int) {
        WorldMapSub1.anInt8014 = i_0 - WorldMap.mapSquareAbsX
        WorldMapSub1.anInt8016 = i_1 - WorldMap.mapSquareAbsY
    }

    @JvmStatic
    fun queryItemNoParams(name: String, tradeable: Boolean) {
        queryItemParamInt(name, tradeable, -1, -1)
    }

    @JvmStatic
    fun method15420() {
        try {
            if (SoundResourceManager.anInt1730 == 1) {
                var volume = SoundResourceManager.aNodeSub15Sub21735!!.getMusicVolume()
                if (volume > 0 && SoundResourceManager.aNodeSub15Sub21735!!.dataLoaded()) {
                    volume -= CutsceneBasicMovement.anInt9440
                    if (volume < 0) {
                        volume = 0
                    }
                    SoundResourceManager.aNodeSub15Sub21735!!.setMusicVolume(volume)
                    return
                }
                SoundResourceManager.aNodeSub15Sub21735!!.turnOffSound()
                SoundResourceManager.aNodeSub15Sub21735!!.unlinkMusicPatches()
                if (AreaSoundPlayer.MUSIC_RESOURCE != null) {
                    SoundResourceManager.anInt1730 = 2
                } else {
                    SoundResourceManager.anInt1730 = 0
                }
                aNodeSub79563 = null
                aClass2505450 = null
            }
            if (SoundResourceManager.anInt1730 == 3) {
                var volume = SoundResourceManager.aNodeSub15Sub21735!!.getMusicVolume()
                if (volume < AreaSoundPlayer.SOUND_LEVEL && SoundResourceManager.aNodeSub15Sub21735!!.dataLoaded()) {
                    volume += anInt4664
                    if (volume > AreaSoundPlayer.SOUND_LEVEL) {
                        volume = AreaSoundPlayer.SOUND_LEVEL
                    }
                    SoundResourceManager.aNodeSub15Sub21735!!.setMusicVolume(volume)
                } else {
                    anInt4664 = 0
                    SoundResourceManager.anInt1730 = 0
                }
            }
        } catch (exception3: Exception) {
            exception3.printStackTrace()
            SoundResourceManager.aNodeSub15Sub21735!!.turnOffSound()
            SongReference.reset()
        }
    }

    @JvmStatic
    fun method15390(i_0: Int, i_1: Int): Int {
        return if (i_1 < 0) -i_0 else (i_0 * Math.sqrt(1.220703125E-4 * i_1) + 0.5).toInt()
    }

    @JvmStatic
    fun method3953() {
        aShort828 = Client.aspectFovMin
        aShort829 = Client.aspectFovMax
        AbstractMousePacket.aShort7079 = Client.fovScaleMin
        aShort830 = Client.fovScaleMax
        aBool826 = true
        if (anInt824 != 0 && CutsceneTypeList.anInt825 != 0) {
            Client.fovScaleMin = 334
            Client.fovScaleMax = 334
            Client.aspectFovMax = (anInt824 * 512 / CutsceneTypeList.anInt825).toShort()
            Client.aspectFovMin = Client.aspectFovMax
        }
    }

    @JvmStatic
    fun method6140(): Boolean {
        return SoundResourceManager.anInt1730 != 0
    }

    @JvmStatic
    fun method4164(i_0: Int, i_1: Int) {
        if (MiniMenu.anInt167 == 1) {
            MenuInteraction.processMenuclick(MiniMenu.aCacheableNodeSub7438, i_0, i_1)
        } else if (MiniMenu.anInt167 == 2) {
            method4653(i_0, i_1)
        }
        MiniMenu.anInt167 = 0
        MiniMenu.aCacheableNodeSub7438 = null
    }

    @JvmStatic
    fun method4166() {
        if (!Client.cameraPitchVelocityApplied) {
            Client.cameraPitchVelocity += (12.0f - Client.cameraPitchVelocity) / 2.0f
            Client.sendCameraAngleToServer = true
            Client.cameraPitchVelocityApplied = true
        }
    }

    @JvmStatic
    fun method4180(i_0: GameState): Boolean {
        return i_0 == GameState.UNK_7 || i_0 == GameState.UNK_6
    }

    @JvmStatic
    fun method1630(i_0: Int, i_1: Int, i_2: Int, i_4: Int, i_6: Int, class282Sub50Sub157: ColoredMiniMenuEntry, fontrenderer8: AbstractFontRenderer, fontmetrics9: FontMetrics, i_10: Int, i_11: Int) {
        var i_101 = i_10
        if (i_0 > i_2 && i_0 < i_2 + i_4 && i_1 > i_6 - fontmetrics9.topPadding - 1 && i_1 < fontmetrics9.bottomPadding + i_6) {
            i_101 = i_11
        }
        val string_13 = MiniMenu.formatTag(class282Sub50Sub157)
        @Suppress("UNCHECKED_CAST")
        fontrenderer8.method386(string_13, i_2 + 3, i_6, i_101, Client.RANDOMIZER, Client.randomSeed, StaticMedia.chatIcons as Array<NativeSprite>?, StaticMedia.chatScaleHeights)
    }

    @JvmStatic
    fun method4653(mouseX: Int, mouseY: Int) {
        if (Client.ENTITY_DEFAULTS!!.aBool7044 || MiniMenu.entryCount != 1 && (!SHOW_FACE_HERE || MiniMenu.entryCount != 2 || !MiniMenu.aCacheableNodeSub712!!.action.equals(LocalizedText.FACE_HERE.translate(Client.CURRENT_LANGUAGE!!)))) {
            val fontMetrics = FontMetrics.getForMinimenu()
            var i_4 = fontMetrics!!.getWidthNoSprites(LocalizedText.CHOOSE_OPTION.translate(Client.CURRENT_LANGUAGE!!)!!)
            val i_5: Int
            var i_7: Int
            if (!MiniMenu.aBool162) {
                var class282Sub50Sub79 = MiniMenu.aClass482171!!.getFirst() as MiniMenuEntry?
                while (class282Sub50Sub79 != null) {
                    i_7 = MiniMenu.getWidthForEntryWithQuests(class282Sub50Sub79, fontMetrics)
                    if (i_7 > i_4) {
                        i_4 = i_7
                    }
                    class282Sub50Sub79 = MiniMenu.aClass482171!!.next() as MiniMenuEntry?
                }
                i_4 += 8
                i_5 = MiniMenu.entryCount * MiniMenu.anInt178 + 21
                MiniMenu.anInt1104 = (if (FontMetrics.useBoldFont) 26 else 22) + MiniMenu.entryCount * MiniMenu.anInt178
            } else {
                var menuEntry = MiniMenu.aClass477182!!.getNext() as ColoredMiniMenuEntry?
                while (menuEntry != null) {
                    i_7 = if (menuEntry.queueSize == 1) {
                        MiniMenu.getWidthForEntryWithQuests(menuEntry.nodeQueue!!.current!!.next as MiniMenuEntry, fontMetrics)
                    } else {
                        MiniMenu.getPrefixTextWidth(menuEntry, fontMetrics)
                    }
                    if (i_7 > i_4) {
                        i_4 = i_7
                    }
                    menuEntry = MiniMenu.aClass477182!!.getDirectNext() as ColoredMiniMenuEntry?
                }
                i_4 += 8
                i_5 = MiniMenu.anInt178 * MiniMenu.anInt170 + 21
                MiniMenu.anInt1104 = MiniMenu.anInt178 * MiniMenu.anInt170 + (if (FontMetrics.useBoldFont) 26 else 22)
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
            anInt3555 = i_8
            anInt2300 = i_7
            MiniMenu.anInt8975 = i_4
            NativeSprite.anInt156 = (Math.random() * 24.0).toInt()
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
    fun method7931(random: Random, limit: Int): Int {
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
        return method5360(randomInteger, limit)
    }

    @JvmStatic
    fun method4250() {
        var vorbis = INDEX36_FILE_CACHE.first()
        while (vorbis != null) {
            method1563(vorbis.fileId)
            vorbis = INDEX36_FILE_CACHE.next()
        }
    }

    @JvmStatic
    fun setVorbisSound(fileId: Int) {
        val vorbis = INDEX36_FILE_CACHE.get(fileId.toLong())
        if (vorbis != null) {
            vorbis.aBool7995 = !vorbis.aBool7995
            vorbis.vfReference!!.method4924(vorbis.aBool7995)
        }
    }

    @JvmStatic
    fun method1563(fileId: Int) {
        val varbisNode = INDEX36_FILE_CACHE.get(fileId.toLong())
        if (varbisNode != null) {
            varbisNode.vfReference!!.method4947()
            SoundTrigger.playVorbis(varbisNode.fileId, varbisNode.sendPacket)
            varbisNode.unlink()
        }
    }

    @JvmStatic
    fun queryItemParamInt(name: String, tradeableOnly: Boolean, paramId: Int, paramIntQuery: Int) {
        CS2Interpreter.queryItem(name, tradeableOnly, paramId, paramIntQuery, null, false)
    }
}
