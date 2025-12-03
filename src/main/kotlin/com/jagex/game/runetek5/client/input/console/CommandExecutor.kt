package com.jagex.game.runetek5.client.input.console

import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

import javax.imageio.ImageIO

import com.darkan.bot.Bot
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.rendering.RenderInfo
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.CollisionMap
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.tools.DebugDisplay
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.FileOperations
import com.jagex.game.runetek5.utils.DirectoryPicker
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarProviders

object CommandExecutor {

    @JvmField var teleportCommandRequested: Boolean = false
    @JvmField var teleportCommandPositionZ: Int = 0
    @JvmField var teleportCommandPositionX: Int = 0
    @JvmField var teleportCommandPositionY: Int = 0

    @JvmStatic
    fun executeCommand(command: String, bool1: Boolean, save: Boolean) {
        try {
            label381@ run {
                try {
                    Bot.handleConsoleCommand(command.split(" ").toTypedArray())
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                if (!"commands".equals(command, ignoreCase = true) && !"help".equals(command, ignoreCase = true)) {
                    if ("cls".equals(command, ignoreCase = true)) {
                        Console.consoleMessageCount = 0
                        Console.anInt2223 = 0
                        return
                    }
                    if ("displayfps".equals(command, ignoreCase = true)) {
                        client.DRAW_DEBUG = !client.DRAW_DEBUG
                        if (client.DRAW_DEBUG) {
                            Console.writeLine("FPS on")
                        } else {
                            Console.writeLine("FPS off")
                        }
                        return
                    }
                    if (!"renderer".equals(command)) {
                        if ("heap".equals(command)) {
                            Console.writeLine("Heap: " + GameShell.maxMemory + "MB")
                            return
                        }
                        if ("getcamerapos".equals(command, ignoreCase = true)) {
                            val base = Resource.mapRegion!!.getBaseCoordinates()
                            Console.writeLine("Pos: " + PlayerUpdate.MY_PLAYER.plane + "," + ((Camera.cameraPositionAbsoluteX shr 9) + base.x.toInt() shr 6) + "," + ((Camera.cameraPositionAbsoluteY shr 9) + base.y.toInt() shr 6) + "," + ((Camera.cameraPositionAbsoluteX shr 9) + base.x.toInt() and 0x3f) + "," + ((Camera.cameraPositionAbsoluteY shr 9) + base.y.toInt() and 0x3f) + " Height: " + (MapRegion.tileheightForAbsolutePosition(Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER.plane.toInt()) - Camera.cameraPositionAbsoluteZ))
                            Console.writeLine("Look: " + PlayerUpdate.MY_PLAYER.plane + "," + (base.x.toInt() + Camera.lookX shr 6) + "," + (base.y.toInt() + Camera.lookY shr 6) + "," + (base.x.toInt() + Camera.lookX and 0x3f) + "," + (base.y.toInt() + Camera.lookY and 0x3f) + " Height: " + (MapRegion.tileheightForAbsolutePosition(Camera.lookX, Camera.lookY, PlayerUpdate.MY_PLAYER.plane.toInt()) - Camera.anInt1527))
                            return
                        }
                    }
                    val renderInfo = Renderers.activeToolkit!!.getRenderInfo()
                    Console.writeLine("Toolkit ID: " + PreferenceManager.preferences!!.activeToolkit.getValue())
                    Console.writeLine("Vendor: " + renderInfo.vendor)
                    Console.writeLine("Name: " + renderInfo.name)
                    Console.writeLine("Version: " + renderInfo.version)
                    Console.writeLine("Device: " + renderInfo.device)
                    Console.writeLine("Driver Version: " + renderInfo.driverVersion)
                    return
                }
                Console.writeLine("commands - This command")
                Console.writeLine("cls - Clear console")
                Console.writeLine("displayfps - Toggle FPS and other information")
                Console.writeLine("renderer - Print graphics renderer information")
                Console.writeLine("heap - Print java memory information")
                Console.writeLine("getcamerapos - Print location and direction of camera for use in bug reports")
                return
            }
        } catch (exception: Exception) {
            Console.writeLine(LocalizedText.ERROR_EXECUTING_COMMAND.translate(client.CURRENT_LANGUAGE!!)!!)
            return
        }
        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE || client.PLAYER_RIGHTS >= 2) {
            try {
                if ("wm1".equals(command, ignoreCase = true)) {
                    WindowMode.requestWindowMode(1, -1, -1, false)
                    if (WindowMode.getWindowMode() == 1) {
                        Console.writeLine("Success")
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if ("wm2".equals(command, ignoreCase = true)) {
                    WindowMode.requestWindowMode(2, -1, -1, false)
                    if (WindowMode.getWindowMode() == 2) {
                        Console.writeLine("Success")
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if (WindowMode.fullScreenSupported && "wm3".equals(command, ignoreCase = true)) {
                    WindowMode.requestWindowMode(3, 1024, 768, false)
                    if (WindowMode.getWindowMode() == 3) {
                        Console.writeLine("Success")
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if (command.startsWith("clipmask")) {
                    Console.writeLine("(" + PlayerUpdate.MY_PLAYER.endX + ", " + PlayerUpdate.MY_PLAYER.endZ + "): " + Resource.mapRegion!!.getCollisionMap(PlayerUpdate.MY_PLAYER.plane.toInt()).collisionFlags[PlayerUpdate.MY_PLAYER.endX + 1][PlayerUpdate.MY_PLAYER.endZ + 1].toInt() + " - " + CollisionMap.getFlags(Resource.mapRegion!!.getCollisionMap(PlayerUpdate.MY_PLAYER.plane.toInt()).collisionFlags[PlayerUpdate.MY_PLAYER.endX + 1][PlayerUpdate.MY_PLAYER.endZ + 1].toInt()))
                }
                if (command.startsWith("dumpitems")) {
                    for (itemId in 0 until 24806) {
                        val pixels = TypeLists.OBJ!!.list(itemId).getSpritePixels(Renderers.GPU_ITEM_RENDERER, Renderers.activeToolkit, 1, 1, -13623264, false, Component.StackMode.NONUM, Renderers.FONT_RENDERER, PlayerUpdate.MY_PLAYER.playerAppearance)
                        try {
                            val dimension = Math.sqrt(pixels.size.toDouble()).toInt() - 1
                            val itemImage = BufferedImage(36, 32, 2)
                            itemImage.setRGB(0, 0, dimension, dimension, pixels, 0, dimension)
                            for (x in 0 until dimension) {
                                for (y in 0 until dimension) {
                                    itemImage.setRGB(y, x, pixels[x * 36 + y])
                                }
                            }
                            val imageFile = File("./items/$itemId.png")
                            ImageIO.write(itemImage, "png", imageFile)
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                    }
                    return
                }
                if (command.startsWith("setlobby ")) {
                    var port = -1
                    var commandStr = command.substring(9)
                    val prefix = commandStr.indexOf(' ')
                    if (prefix == -1) {
                        Console.writeLine("Failure")
                        return
                    }
                    try {
                        port = commandStr.substring(0, prefix).toInt()
                    } catch (numberFormatException: NumberFormatException) {
                        // NOOP
                    }
                    if (port == -1) {
                        Console.writeLine("Failure")
                        return
                    }
                    val host = commandStr.substring(prefix + 1).trim() + ".runescape.com"
                    WorldConnection.setLobbyConnection(host, port)
                    Console.writeLine("Success")
                    return
                }
                if ("tk0".equals(command, ignoreCase = true)) { // Safe mode
                    client.loadAssets(0, false)
                    if (PreferenceManager.preferences!!.activeToolkit.getValue() == 0) {
                        Console.writeLine("Success")
                        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit, 0)
                        PreferenceManager.savePreferences()
                        client.preferencesSavedOnServer = false
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if ("tk1".equals(command, ignoreCase = true)) { // OpenGL - no shaders
                    client.loadAssets(1, false)
                    if (PreferenceManager.preferences!!.activeToolkit.getValue() == 1) {
                        Console.writeLine("Success")
                        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit, 1)
                        PreferenceManager.savePreferences()
                        client.preferencesSavedOnServer = false
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if ("tk2".equals(command, ignoreCase = true)) { // Sw3d
                    client.loadAssets(2, false)
                    if (PreferenceManager.preferences!!.activeToolkit.getValue() == 2) {
                        Console.writeLine("Success")
                        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit, 2)
                        PreferenceManager.savePreferences()
                        client.preferencesSavedOnServer = false
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if ("tk3".equals(command, ignoreCase = true)) { // DirectX
                    client.loadAssets(3, false)
                    if (PreferenceManager.preferences!!.activeToolkit.getValue() == 3) {
                        Console.writeLine("Success")
                        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit, 3)
                        PreferenceManager.savePreferences()
                        client.preferencesSavedOnServer = false
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if ("tk5".equals(command, ignoreCase = true)) { // OpenGL - with shaders
                    client.loadAssets(5, false)
                    if (PreferenceManager.preferences!!.activeToolkit.getValue() == 5) {
                        Console.writeLine("Success")
                        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit, 5)
                        PreferenceManager.savePreferences()
                        client.preferencesSavedOnServer = false
                    } else {
                        Console.writeLine("Failure")
                    }
                    return
                }
                if ("clientdrop".equals(command, ignoreCase = true)) {
                    if (client.gameState == GameState.LOGGED_INGAME) {
                        WorldConnection.dropClient()
                    } else if (client.gameState == GameState.LOADING_PLEASE_WAIT) {
                        client.GAME_CONNECTION_CONTEXT.dropped = true
                    }
                    return
                }
                if ("breakcon".equals(command, ignoreCase = true)) {
                    val connectionContexts = client.connectionContexts
                    for (index in connectionContexts.indices) {
                        val context = connectionContexts[index]
                        if (context.getConnection() != null) {
                            context.getConnection()!!.drop()
                        }
                    }
                    Resource.remoteFileRequesterJs5!!.dropConnection()
                    return
                }
                if (command.startsWith("getclientvarpbit")) {
                    val varpbitId = command.substring(17).toInt()
                    Console.writeLine("varpbit=" + VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(varpbitId))
                    return
                }
                if (command.startsWith("getclientvarp")) {
                    val varpId = command.substring(14).toInt()
                    Console.writeLine("varp=" + VarProviders.PLAYER_VAR_PROVIDER.getVarValue(varpId))
                    return
                }
                if (command.startsWith("directlogin")) {
                    val commandParts = Utils.split(command.substring(12), ' ')
                    if (commandParts.size == 2) {
                        LoginManager.directLogin(commandParts[0], commandParts[1])
                    }
                    return
                }
                if (command.startsWith("snlogin ")) {
                    val commandParts = Utils.split(command.substring(8), ' ')
                    val state = commandParts[0].toInt()
                    LoginManager.snLogin(state)
                    return
                }
                if (command.startsWith("setoutput ")) {
                    var file = File(command.substring(10))
                    if (file.exists()) {
                        file = File(command.substring(10) + "." + Utils.time() + ".log")
                        if (file.exists()) {
                            Console.writeLine("file already exists!")
                            return
                        }
                    }
                    if (Console.outputStream != null) {
                        Console.outputStream!!.close()
                        Console.outputStream = null
                    }
                    try {
                        Console.outputStream = FileOutputStream(file)
                    } catch (notFound: FileNotFoundException) {
                        Console.writeLine("Could not create " + file.name)
                    } catch (securityException: SecurityException) {
                        Console.writeLine("Cannot write to " + file.name)
                    }
                    return
                }
                if ("closeoutput".equals(command)) {
                    if (Console.outputStream != null) {
                        Console.outputStream!!.close()
                    }
                    Console.outputStream = null
                    return
                }
                if (command.startsWith("runscript ")) {
                    val file = File(command.substring(10))
                    if (!file.exists()) {
                        Console.writeLine("No such file")
                        return
                    }
                    val fileData = FileOperations.getDataFromFile(file)
                    if (fileData == null) {
                        Console.writeLine("Failed to read file")
                        return
                    }
                    val scriptLines = Utils.split(Utils.formatScript(Utils.decodeWithUnicodeTable(fileData), '\r', ""), '\n')
                    Console.runscript(scriptLines)
                }
                /*Added*/
                if (command.startsWith("debug ")) {
                    DebugDisplay.applyDebugOpcode(command.substring(6).toInt())
                    return
                }
                if (command.startsWith("checknpprio")) {
                    for (id in 0 until 100_000) {
                        val npctype = TypeLists.NPC!!.list(id)
                        if (npctype != null && npctype.highPriority) {
                            Console.writeLine("NPC: [$id] ${npctype.name}")
                        }
                    }
                    Console.writeLine("Done checking: 100000 npcs")
                    return
                }
                if (command.startsWith("npctoggle")) {
                    for (id in 0 until 100_000) {
                        val npctype = TypeLists.NPC!!.list(id)
                        if (npctype != null && !npctype.delayMovement) {
                            npctype.delayMovement = !npctype.delayMovement
                        }
                    }
                    Console.writeLine("Done toggling to: ")
                    return
                }
                if (command.startsWith("checklocop")) {
                    for (id in 0 until 100_000) {
                        val locType = TypeLists.LOC!!.list(id)
                        if (locType.requiresTextures) {
                            Console.writeLine("LOC: $id")
                        }
                    }
                    return
                }
                if (command.startsWith("checknpcop")) {
                    Console.writeLine("Executing $command")
                    for (id in 0 until 100_000) {
                        val npc = TypeLists.NPC!!.list(id)
                        if (npc.npcBasId > 0) {
                            val basType = TypeLists.BAS!!.list(npc.npcBasId)
                            if (basType.unused != -1) {
                                Console.writeLine("NPC: $id with name ${npc.name} - has anInt2826: ${basType.unused}")
                            }
                        }
                    }
                    return
                }
                if (command.startsWith("effect") || command.startsWith("e")) {
                    val effect = if (command.length > 5) command.substring(7).toInt() else command.substring(2).toInt()
                    Loader.MATERIAL_EFFECTS_DISABLED[effect] = !Loader.MATERIAL_EFFECTS_DISABLED[effect]
                    Console.writeLine("Set effect: $effect to " + (if (Loader.MATERIAL_EFFECTS_DISABLED[effect]) "disabled" else "enabled"))
                    return
                }
                if (command.startsWith("pickdir")) {
                    DirectoryPicker("FilePicker")
                    return
                }
                /**/
                if (client.gameState == GameState.LOGGED_INGAME) {
                    val clientPacket = PacketEncoder.createPacket(ClientProt.COMMAND, client.GAME_CONNECTION_CONTEXT.isaac)
                    clientPacket.buffer.writeByte(command.length + 3)
                    clientPacket.buffer.writeByte(if (bool1) 1 else 0)
                    clientPacket.buffer.writeByte(if (save) 1 else 0)
                    clientPacket.buffer.writeString(command)
                    client.GAME_CONNECTION_CONTEXT.queuePacket(clientPacket)
                }
            } catch (exception: Exception) {
                Console.writeLine(LocalizedText.ERROR_EXECUTING_COMMAND.translate(client.CURRENT_LANGUAGE!!)!!)
                return
            }
        }
        if (client.gameState != GameState.LOGGED_INGAME) {
            Console.writeLine(LocalizedText.UNKNOWN_DEV_COMMAND.translate(client.CURRENT_LANGUAGE) + command)
        }
    }

    @JvmStatic
    fun executeTeleportCommand(plane: Int, x: Int, y: Int) {
        val commandString = "tele $plane,${x shr 6},${y shr 6},${x and 0x3f},${y and 0x3f}"
        executeCommand(commandString, true, false)
    }

}
