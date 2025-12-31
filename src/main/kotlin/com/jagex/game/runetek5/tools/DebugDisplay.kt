package com.jagex.game.runetek5.tools

import com.jagex.game.runetek5.ExplicitRuntimeException
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.culling.Culling
import com.jagex.game.runetek5.utils.Utils
import java.io.*

object DebugDisplay {

    @JvmField
    var anInt3991: Int = 0

    @JvmStatic
    fun applyDebugOpcode(opcode: Int) {
        if (opcode == 17) {
            throw Error()
        } else if (opcode == 25) {
            throw OutOfMemoryError()
        }
        try {
            when (opcode) {
                8 -> Console.close()
                24 -> Console.writeLine("" + GameShell.FPS)
                28 -> {
                    val culling = Resource.mapRegion!!.getScene().culling
                    culling.cullingEnabled = !culling.cullingEnabled
                }
                7 -> client.DRAW_DEBUG = true
                4 -> client.DRAW_DEBUG = false
                14 -> client.TILE_MESSAGES.unlink()
                21 -> {
                    TypeLists.clear()
                    for (gc in 0..9) {
                        System.gc()
                    }
                    val runtime = Runtime.getRuntime()
                    val kbMemleft = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
                    Console.writeLine("" + kbMemleft)
                }
                9 -> {
                    TypeLists.clear()
                    for (gc in 0..9) {
                        System.gc()
                    }
                    val runtime = Runtime.getRuntime()
                    var kbMemleft = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
                    Console.writeLine("" + kbMemleft)
                    PlayerEntity.uploadScene()
                    TypeLists.clear()
                    for (gc in 0..9) {
                        System.gc()
                    }
                    kbMemleft = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
                    Console.writeLine("" + kbMemleft)
                }
                19 -> Console.writeLine(if (Resource.NATIVE_LIBRARY_LOADER!!.shutdown()) "Success" else "Failure")
                20 -> LoginManager.GAME_CONNECTION_INFO!!.swap()
                22 -> Resource.remoteFileRequesterJs5!!.closeStream()
                3 -> Resource.remoteFileRequesterJs5!!.requestTermination()
                13 -> GameShell.gameCanvas!!.setLocation(50, 50)
                1 -> GameShell.gameCanvas!!.setLocation(GameShell.gameCanvasX, GameShell.gameCanvasY)
                27 -> MapRegion.requestDebugMapUpdate()
                6 -> {
                    Resource.mapRegion!!.debugRegionLoadtimeStart = Utils.time()
                    Resource.mapRegion!!.debugRegionLoadtime = true
                    MapRegion.requestDebugMapUpdate()
                }
                26 -> {
                    val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                    Console.writeLine((abs.x.toInt() shr 9).toString() + " " + (abs.z.toInt() shr 9))
                }
                2 -> {
                    val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                    Console.writeLine("" + Resource.mapRegion!!.getScene().localHeightmap!![PlayerUpdate.MY_PLAYER!!.plane.toInt()]!!.getHeight(abs.x.toInt() shr 9, abs.z.toInt() shr 9))
                }
                23 -> {
                    Console.writeLine(Component.NATIVE_SPRITE_CACHE.getCapacityLeft().toString() + " " + Component.NATIVE_SPRITE_CACHE.getCapacity())
                    Console.writeLine(Component.MODEL_CACHE.getCapacityLeft().toString() + " " + Component.MODEL_CACHE.getCapacity())
                    Console.writeLine(TypeLists.OBJ!!.spriteDrawQueue.getCurrentCapacity().toString() + " " + TypeLists.OBJ!!.spriteDrawQueue.getMaxCapacity())
                }
                16 -> TypeLists.resetCachedData(false)
                15 -> {
                    client.DEBUG_MODE = !client.DEBUG_MODE
                    Renderers.activeToolkit!!.debugModeChanged()
                }
                10 -> {
                    client.sceneDummyOp = 0
                    Resource.mapRegion!!.reload()
                }
                5 -> {
                    client.sceneDummyOp = 1
                    Resource.mapRegion!!.reload()
                }
                29 -> {
                    client.sceneDummyOp = 2
                    Resource.mapRegion!!.reload()
                }
            }
        } catch (exception: Exception) {
            Console.writeLine(LocalizedText.ERROR_EXECUTING_COMMAND.translate(client.CURRENT_LANGUAGE)!!)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getStringFromThrowable(throwable: Throwable): String {
        var error: String
        var actualThrowable = throwable
        if (throwable is ExplicitRuntimeException) {
            error = throwable.error + "|"
            actualThrowable = throwable.throwable
        } else {
            error = ""
        }
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        actualThrowable.printStackTrace(printWriter)
        printWriter.close()
        val stackTrace = stringWriter.toString()
        val stackTraceReader = BufferedReader(StringReader(stackTrace))
        val firstTraceLine = stackTraceReader.readLine()
        while (true) {
            val stackTraceLine = stackTraceReader.readLine() ?: break
            val seekJavaVersionPosition = stackTraceLine.indexOf('(')
            val javaVersionLength = stackTraceLine.indexOf(')', seekJavaVersionPosition + 1)
            var string_11: String
            if (seekJavaVersionPosition != -1) {
                string_11 = stackTraceLine.substring(0, seekJavaVersionPosition)
            } else {
                string_11 = stackTraceLine
            }
            string_11 = string_11.trim()
            string_11 = string_11.substring(string_11.lastIndexOf(' ') + 1)
            string_11 = string_11.substring(string_11.lastIndexOf('\t') + 1)
            error += string_11
            if (seekJavaVersionPosition != -1 && javaVersionLength != -1) {
                val javaVersionStart = stackTraceLine.indexOf(".java:", seekJavaVersionPosition)
                if (javaVersionStart >= 0) {
                    error += stackTraceLine.substring(javaVersionStart + 5, javaVersionLength)
                }
            }
            error += ' '
        }
        error += "| $firstTraceLine"
        return error
    }
}
