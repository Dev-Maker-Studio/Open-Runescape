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
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
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
            // Handle bot commands first
            try {
                Bot.handleConsoleCommand(command.split(" ").toTypedArray())
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            // Basic commands available to everyone
            when {
                command.equals("commands", ignoreCase = true) || command.equals("help", ignoreCase = true) -> {
                    Console.writeLine("commands - This command")
                    Console.writeLine("cls - Clear console")
                    Console.writeLine("displayfps - Toggle FPS and other information")
                    Console.writeLine("renderer - Print graphics renderer information")
                    Console.writeLine("heap - Print java memory information")
                    Console.writeLine("getcamerapos - Print location and direction of camera for use in bug reports")
                    return
                }
                command.equals("cls", ignoreCase = true) -> {
                    Console.consoleMessageCount = 0
                    Console.anInt2223 = 0
                    return
                }
                command.equals("displayfps", ignoreCase = true) -> {
                    client.DRAW_DEBUG = !client.DRAW_DEBUG
                    Console.writeLine(if (client.DRAW_DEBUG) "FPS on" else "FPS off")
                    return
                }
                command.equals("renderer", ignoreCase = true) -> {
                    val renderInfo = Renderers.activeToolkit!!.getRenderInfo()
                    Console.writeLine("Toolkit ID: ${PreferenceManager.preferences!!.activeToolkit!!.getValue()}")
                    Console.writeLine("Vendor: ${renderInfo.vendor}")
                    Console.writeLine("Name: ${renderInfo.name}")
                    Console.writeLine("Version: ${renderInfo.version}")
                    Console.writeLine("Device: ${renderInfo.device}")
                    Console.writeLine("Driver Version: ${renderInfo.driverVersion}")
                    return
                }
                command.equals("heap", ignoreCase = true) -> {
                    Console.writeLine("Heap: ${GameShell.maxMemory}MB")
                    return
                }
                command.equals("getcamerapos", ignoreCase = true) -> {
                    val base = Resource.mapRegion!!.getBaseCoordinates()
                    val cameraX = (Camera.cameraPositionAbsoluteX shr 9) + base.x.toInt()
                    val cameraY = (Camera.cameraPositionAbsoluteY shr 9) + base.y.toInt()
                    val lookTotalX = base.x.toInt() + Camera.lookX
                    val lookTotalY = base.y.toInt() + Camera.lookY

                    Console.writeLine("Pos: ${PlayerUpdate.MY_PLAYER!!.plane},${cameraX shr 6},${cameraY shr 6},${cameraX and 0x3f},${cameraY and 0x3f} Height: ${MapRegion.tileheightForAbsolutePosition(Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER!!.plane.toInt()) - Camera.cameraPositionAbsoluteZ}")
                    Console.writeLine("Look: ${PlayerUpdate.MY_PLAYER!!.plane},${lookTotalX shr 6},${lookTotalY shr 6},${lookTotalX and 0x3f},${lookTotalY and 0x3f} Height: ${MapRegion.tileheightForAbsolutePosition(Camera.lookX, Camera.lookY, PlayerUpdate.MY_PLAYER!!.plane.toInt()) - Camera.anInt1527}")
                    return
                }
            }

            // Privileged commands (dev environment or staff)
            if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE || client.PLAYER_RIGHTS >= 2) {
                when {
                    command.equals("wm1", ignoreCase = true) -> {
                        WindowMode.requestWindowMode(1, -1, -1, false)
                        Console.writeLine(if (WindowMode.getWindowMode() == 1) "Success" else "Failure")
                        return
                    }
                    command.equals("wm2", ignoreCase = true) -> {
                        WindowMode.requestWindowMode(2, -1, -1, false)
                        Console.writeLine(if (WindowMode.getWindowMode() == 2) "Success" else "Failure")
                        return
                    }
                    WindowMode.fullScreenSupported && command.equals("wm3", ignoreCase = true) -> {
                        WindowMode.requestWindowMode(3, 1024, 768, false)
                        Console.writeLine(if (WindowMode.getWindowMode() == 3) "Success" else "Failure")
                        return
                    }
                    command.startsWith("clipmask") -> {
                        val player = PlayerUpdate.MY_PLAYER!!
                        val collisionMap = Resource.mapRegion!!.getCollisionMap(player.plane.toInt())
                        val flags = collisionMap.collisionFlags[player.endX + 1][player.endZ + 1].toInt()
                        Console.writeLine("(${player.endX}, ${player.endZ}): $flags - ${CollisionMap.getFlags(flags)}")
                        return
                    }
                    command.startsWith("dumpitems") -> {
                        for (itemId in 0 until 24806) {
                            val pixels = TypeLists.OBJ!!.list(itemId).getSpritePixels(
                                Renderers.GPU_ITEM_RENDERER!!, Renderers.activeToolkit!!, 1, 1, -13623264,
                                false, Component.StackMode.NONUM, Renderers.FONT_RENDERER!!,
                                PlayerUpdate.MY_PLAYER!!.playerAppearance!!
                            )
                            try {
                                val dimension = Math.sqrt(pixels!!.size.toDouble()).toInt() - 1
                                val itemImage = BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB)
                                for (x in 0 until dimension) {
                                    for (y in 0 until dimension) {
                                        itemImage.setRGB(y, x, pixels[x * 36 + y])
                                    }
                                }
                                val imageFile = File("./items/$itemId.png")
                                imageFile.parentFile?.mkdirs()
                                ImageIO.write(itemImage, "png", imageFile)
                            } catch (exception: Exception) {
                                exception.printStackTrace()
                            }
                        }
                        return
                    }
                    command.startsWith("setlobby ") -> {
                        val commandStr = command.substring(9)
                        val spaceIndex = commandStr.indexOf(' ')
                        if (spaceIndex == -1) {
                            Console.writeLine("Failure")
                            return
                        }
                        val port = commandStr.substring(0, spaceIndex).toIntOrNull()
                        if (port == null) {
                            Console.writeLine("Failure")
                            return
                        }
                        val host = "${commandStr.substring(spaceIndex + 1).trim()}.runescape.com"
                        WorldConnection.setLobbyConnection(host, port)
                        Console.writeLine("Success")
                        return
                    }
                    command.equals("tk0", ignoreCase = true) -> {
                        handleToolkitSwitch(0)
                        return
                    }
                    command.equals("tk1", ignoreCase = true) -> {
                        handleToolkitSwitch(1)
                        return
                    }
                    command.equals("tk2", ignoreCase = true) -> {
                        handleToolkitSwitch(2)
                        return
                    }
                    command.equals("tk3", ignoreCase = true) -> {
                        handleToolkitSwitch(3)
                        return
                    }
                    command.equals("tk5", ignoreCase = true) -> {
                        handleToolkitSwitch(5)
                        return
                    }
                    command.equals("clientdrop", ignoreCase = true) -> {
                        when (client.gameState) {
                            GameState.LOGGED_INGAME -> WorldConnection.dropClient()
                            GameState.LOADING_PLEASE_WAIT -> client.GAME_CONNECTION_CONTEXT.dropped = true
                            else -> {}
                        }
                        return
                    }
                    command.equals("breakcon", ignoreCase = true) -> {
                        client.connectionContexts.forEach { context ->
                            context.getConnection()?.drop()
                        }
                        Resource.remoteFileRequesterJs5!!.dropConnection()
                        return
                    }
                    command.startsWith("getclientvarpbit ") -> {
                        val varpbitId = command.substring(17).toIntOrNull()
                        if (varpbitId != null) {
                            Console.writeLine("varpbit=${VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(varpbitId)}")
                        }
                        return
                    }
                    command.startsWith("getclientvarp ") -> {
                        val varpId = command.substring(14).toIntOrNull()
                        if (varpId != null) {
                            Console.writeLine("varp=${VarProviders.PLAYER_VAR_PROVIDER.getVarValue(varpId)}")
                        }
                        return
                    }
                    command.startsWith("directlogin ") -> {
                        val commandParts = Utils.split(command.substring(12), ' ')
                        if (commandParts.size == 2) {
                            LoginManager.directLogin(commandParts[0], commandParts[1])
                        }
                        return
                    }
                    command.startsWith("snlogin ") -> {
                        val commandParts = Utils.split(command.substring(8), ' ')
                        val state = commandParts[0].toIntOrNull()
                        if (state != null) {
                            LoginManager.snLogin(state)
                        }
                        return
                    }
                    command.startsWith("setoutput ") -> {
                        handleSetOutput(command.substring(10))
                        return
                    }
                    command.equals("closeoutput", ignoreCase = true) -> {
                        Console.outputStream?.close()
                        Console.outputStream = null
                        return
                    }
                    command.startsWith("runscript ") -> {
                        handleRunScript(command.substring(10))
                        return
                    }
                    command.startsWith("debug ") -> {
                        command.substring(6).toIntOrNull()?.let {
                            DebugDisplay.applyDebugOpcode(it)
                        }
                        return
                    }
                    command.startsWith("checknpprio") -> {
                        (0 until 100_000).forEach { id ->
                            TypeLists.NPC!!.list(id).let { npctype ->
                                if (npctype.highPriority) {
                                    Console.writeLine("NPC: [$id] ${npctype.name}")
                                }
                            }
                        }
                        Console.writeLine("Done checking: 100000 npcs")
                        return
                    }
                    command.startsWith("npctoggle") -> {
                        (0 until 100_000).forEach { id ->
                            TypeLists.NPC!!.list(id).let { npctype ->
                                if (!npctype.delayMovement) {
                                    npctype.delayMovement = true
                                }
                            }
                        }
                        Console.writeLine("Done toggling")
                        return
                    }
                    command.startsWith("checklocop") -> {
                        (0 until 100_000).forEach { id ->
                            val locType = TypeLists.LOC!!.list(id)
                            if (locType.requiresTextures) {
                                Console.writeLine("LOC: $id")
                            }
                        }
                        return
                    }
                    command.startsWith("checknpcop") -> {
                        Console.writeLine("Executing $command")
                        (0 until 100_000).forEach { id ->
                            TypeLists.NPC!!.list(id).let { npc ->
                                if (npc.npcBasId > 0) {
                                    val basType = TypeLists.BAS!!.list(npc.npcBasId)
                                    if (basType.unused != -1) {
                                        Console.writeLine("NPC: $id with name ${npc.name} - has anInt2826: ${basType.unused}")
                                    }
                                }
                            }
                        }
                        return
                    }
                    command.startsWith("effect") || command.startsWith("e ") -> {
                        val effect = when {
                            command.startsWith("effect ") -> command.substring(7).toIntOrNull()
                            command.startsWith("e ") -> command.substring(2).toIntOrNull()
                            else -> null
                        }
                        if (effect != null && effect in Loader.MATERIAL_EFFECTS_DISABLED.indices) {
                            Loader.MATERIAL_EFFECTS_DISABLED[effect] = !Loader.MATERIAL_EFFECTS_DISABLED[effect]
                            Console.writeLine("Set effect: $effect to ${if (Loader.MATERIAL_EFFECTS_DISABLED[effect]) "disabled" else "enabled"}")
                        }
                        return
                    }
                    command.startsWith("pickdir") -> {
                        DirectoryPicker("FilePicker")
                        return
                    }
                }

                // Send command to server if in-game
                if (client.gameState == GameState.LOGGED_INGAME) {
                    val clientPacket = PacketEncoder.createPacket(ClientProt.COMMAND, client.GAME_CONNECTION_CONTEXT.isaac)
                    clientPacket.buffer.writeByte(command.length + 3)
                    clientPacket.buffer.writeByte(if (bool1) 1 else 0)
                    clientPacket.buffer.writeByte(if (save) 1 else 0)
                    clientPacket.buffer.writeString(command)
                    client.GAME_CONNECTION_CONTEXT.queuePacket(clientPacket)
                    return
                }
            }

            // Unknown command
            if (client.gameState != GameState.LOGGED_INGAME) {
                Console.writeLine("${LocalizedText.UNKNOWN_DEV_COMMAND.translate(client.CURRENT_LANGUAGE)}$command")
            }
        } catch (exception: Exception) {
            Console.writeLine(LocalizedText.ERROR_EXECUTING_COMMAND.translate(client.CURRENT_LANGUAGE!!)!!)
            exception.printStackTrace()
        }
    }

    private fun handleToolkitSwitch(toolkit: Int) {
        client.loadAssets(toolkit, false)
        if (PreferenceManager.preferences!!.activeToolkit!!.getValue() == toolkit) {
            Console.writeLine("Success")
            PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit!!, toolkit)
            PreferenceManager.savePreferences()
            client.preferencesSavedOnServer = false
        } else {
            Console.writeLine("Failure")
        }
    }

    private fun handleSetOutput(filepath: String) {
        var file = File(filepath)
        if (file.exists()) {
            file = File("$filepath.${Utils.time()}.log")
            if (file.exists()) {
                Console.writeLine("file already exists!")
                return
            }
        }
        Console.outputStream?.close()
        Console.outputStream = null

        try {
            Console.outputStream = FileOutputStream(file)
        } catch (_: FileNotFoundException) {
            Console.writeLine("Could not create ${file.name}")
        } catch (_: SecurityException) {
            Console.writeLine("Cannot write to ${file.name}")
        }
    }

    private fun handleRunScript(filepath: String) {
        val file = File(filepath)
        if (!file.exists()) {
            Console.writeLine("No such file")
            return
        }
        val fileData = FileOperations.getDataFromFile(file)
        if (fileData == null) {
            Console.writeLine("Failed to read file")
            return
        }
        val scriptLines = Utils.split(
            Utils.formatScript(Utils.decodeWithUnicodeTable(fileData), '\r', ""),
            '\n'
        )
        Console.runscript(scriptLines)
    }

    @JvmStatic
    fun executeTeleportCommand(plane: Int, x: Int, y: Int) {
        val commandString = "tele $plane,${x shr 6},${y shr 6},${x and 0x3f},${y and 0x3f}"
        executeCommand(commandString, true, false)
    }

}
