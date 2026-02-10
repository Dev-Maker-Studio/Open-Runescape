package com.jagex.game.runetek5.client.input.console

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.utils.Utils
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Date

/**
 * [J]
 * @author Steven
 */
object Console {

    @JvmField
    var outputStream: FileOutputStream? = null
    @JvmField
    var displayedOutput: Array<String>? = null
    @JvmField
    var consoleMessageCount: Int = 0

    @JvmField
    var inputLineHeight: Int = 0
    @JvmField
    var outputLineHeight: Int = 0
    @JvmField
    var scrollOffset: Int = 0

    @JvmField
    var clipboard: Clipboard? = null
    @JvmField
    var scriptArguments: Array<String>? = null
    @JvmField
    var commandString: String = ""
    @JvmField
    var commandLength: Int = 0
    @JvmField
    var isOpen: Boolean = false

    @JvmField
    var pausedAtCommandIndex: Int = -1
    @JvmField
    var historyIndex: Int = 0
    @JvmField
    var pauseEndsAt: Long = 0

    @JvmField
    var backgroundAlpha: Int = 0

    @JvmStatic
    fun open() {
        if (GameState.isIngame()) {
            if (displayedOutput == null) {
                initialize()
            }
            isOpen = true
            backgroundAlpha = 0
        }
    }

    @JvmStatic
    fun close() {
        isOpen = false
        GameShell.setComponentsOnTop()
    }

    @JvmStatic
    fun initialize() {
        inputLineHeight = FontMetrics.aClass4143438!!.topPadding + FontMetrics.aClass4143438!!.bottomPadding + 2
        outputLineHeight = FontManager.messageboxFont!!.topPadding + FontManager.messageboxFont!!.bottomPadding + 2
        displayedOutput = Array(500) { "" }
        writeLine(LocalizedText.DEVELOPER_CONSOLE_INTRO.translate(Client.CURRENT_LANGUAGE!!)!!)
    }

    @JvmStatic
    fun executeCommand(save: Boolean) {
        if (commandString.isNotEmpty()) {
            writeLine("--> $commandString")
            CommandExecutor.executeCommand(commandString, false, save)
            historyIndex = 0
            if (!save) {
                commandLength = 0
                commandString = ""
            }
        }
    }

    @JvmStatic
    fun writeLine(command: String) {
        if (displayedOutput == null) {
            initialize()
        }
        Client.ACTIVE_TIMEZONE_CALENDER!!.time = Date(Utils.time())
        val hour = Client.ACTIVE_TIMEZONE_CALENDER!![Calendar.HOUR_OF_DAY]
        val minute = Client.ACTIVE_TIMEZONE_CALENDER!![Calendar.MINUTE]
        val second = Client.ACTIVE_TIMEZONE_CALENDER!![Calendar.SECOND]
        val timeDisplay = "${hour / 10}${hour % 10}:${minute / 10}${minute % 10}:${second / 10}${second % 10}"
        val commandParts = Utils.split(command, '\n')
        for (index in commandParts.indices) {
            if (consoleMessageCount >= 0) {
                System.arraycopy(displayedOutput, 0, displayedOutput, 1, consoleMessageCount)
            }
            displayedOutput!![0] = "$timeDisplay: ${commandParts[index]}"
            if (outputStream != null) {
                try {
                    outputStream!!.write(Utils.characterSequenceToBytearray(displayedOutput!![0] + "\n"))
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }
            }
            if (consoleMessageCount < displayedOutput!!.size - 1) {
                consoleMessageCount++
                if (scrollOffset > 0) {
                    scrollOffset++
                }
            }
        }
    }

    @JvmStatic
    fun adjustCommandString() {
        if (historyIndex > 0) {
            var arrowCount = 0
            for (index in displayedOutput!!.indices) {
                if (displayedOutput!![index].indexOf("--> ") != -1) {
                    ++arrowCount
                    if (arrowCount == historyIndex) {
                        commandString = displayedOutput!![index].substring(displayedOutput!![index].indexOf('>') + 2)
                        break
                    }
                }
            }
        } else {
            commandString = ""
        }
    }

    @JvmStatic
    fun processConsoleInput() {
        if (backgroundAlpha < 102) {
            backgroundAlpha += 6
        }

        if (pausedAtCommandIndex != -1 && pauseEndsAt < Utils.time()) {
            for (argIndex in pausedAtCommandIndex until scriptArguments!!.size) {
                if (scriptArguments!![argIndex].startsWith("pause")) {
                    var pauseSeconds = 5
                    try {
                        pauseSeconds = scriptArguments!![argIndex].substring(6).toInt()
                    } catch (ignored: Exception) {
                        // NOOP
                    }
                    writeLine("Pausing for $pauseSeconds seconds...")
                    pausedAtCommandIndex = argIndex + 1
                    pauseEndsAt = Utils.time() + (pauseSeconds * 1000L)
                    return
                }
                commandString = scriptArguments!![argIndex]
                executeCommand(false)
            }
            pausedAtCommandIndex = -1
        }
        if (Client.mouseWheelDelta != 0) {
            scrollOffset -= Client.mouseWheelDelta * 5
            if (scrollOffset >= consoleMessageCount) {
                scrollOffset = consoleMessageCount - 1
            }
            if (scrollOffset < 0) {
                scrollOffset = 0
            }
            Client.mouseWheelDelta = 0
        }
        for (index in 0 until Client.keyCachePointerPoss) {
            val keyRecord = Client.KEYS_PRESSED[index]
            val code = keyRecord!!.getCode()
            val character = keyRecord!!.getCharacter()
            val flags = keyRecord!!.getFlags()
            if (code == 84) {
                executeCommand(false)
            }
            if (code == 80) {
                executeCommand(true)
            } else if (code == 66 && (flags and 0x4) != 0) {
                if (clipboard != null) {
                    var line = ""
                    for (lineIndex in displayedOutput!!.size - 1 downTo 0) {
                        if (displayedOutput!![lineIndex].isNotEmpty()) {
                            line = line + displayedOutput!![lineIndex] + '\n'
                        }
                    }
                    clipboard!!.setContents(StringSelection(line), null)
                }
            } else if (code == 67 && (flags and 0x4) != 0) {
                if (clipboard != null) {
                    try {
                        val transferable = clipboard!!.getContents(null)
                        if (transferable != null) {
                            val data = transferable.getTransferData(DataFlavor.stringFlavor) as String
                            val script = Utils.split(data, '\n')
                            runscript(script)
                        }
                    } catch (ignored: Exception) {
                        // NOOP
                    }
                }
            } else if (code == 85 && commandLength > 0) {
                commandString = commandString.substring(0, commandLength - 1) + commandString.substring(commandLength)
                commandLength--
            } else if (code == 101 && commandLength < commandString.length) {
                commandString = commandString.substring(0, commandLength) + commandString.substring(commandLength + 1)
            } else if (code == 96 && commandLength > 0) {
                commandLength--
            } else if (code == 97 && commandLength < commandString.length) {
                commandLength++
            } else if (code == 102) {
                commandLength = 0
            } else if (code == 103) {
                commandLength = commandString.length
            } else if (code == 104 && historyIndex < displayedOutput!!.size) {
                historyIndex++
                adjustCommandString()
                commandLength = commandString.length
            } else if (code == 105 && historyIndex > 0) {
                historyIndex--
                adjustCommandString()
                commandLength = commandString.length
            } else if (TextUtils.isAlphabeticalOrNumeric(character) || "\\/.:, _-+[]~@".indexOf(character) != -1) {
                commandString = commandString.substring(0, commandLength) + Client.KEYS_PRESSED[index]!!.getCharacter() + commandString.substring(commandLength)
                commandLength++
            }
        }
        Client.keyCachePointerPoss = 0
        Client.maximumHeldKeys = 0
        GameShell.setComponentsOnTop()
    }

    @JvmStatic
    fun isOpen(): Boolean {
        return isOpen
    }

    @JvmStatic
    fun runscript(arguments: Array<String>) {
        if (arguments.size > 1) {
            for (index in arguments.indices) {
                if (arguments[index].startsWith("pause")) {
                    var waitDuration = 5
                    try {
                        waitDuration = arguments[index].substring(6).toInt()
                    } catch (exception: Exception) {
                        // NOOP
                    }
                    writeLine("Pausing for $waitDuration seconds...")
                    scriptArguments = arguments
                    pausedAtCommandIndex = index + 1
                    pauseEndsAt = Utils.time() + (waitDuration * 1000L)
                    break
                }
                commandString = arguments[index]
                executeCommand(false)
            }
        } else {
            commandString += arguments[0]
            commandLength += arguments[0].length
        }
    }

    @JvmStatic
    fun renderConsole(toolkit: AbstractToolkit) {
        toolkit.setClipping(0, 0, GameShell.screenCanvasWidth, 350)
        toolkit.drawFilledRectangle(0, 0, GameShell.screenCanvasWidth, 350, backgroundAlpha shl 24 or 0x332277, 1)
        val i_2 = 350 / outputLineHeight
        if (consoleMessageCount > 0) {
            val i_3 = 346 - outputLineHeight - 4
            val i_4 = i_2 * i_3 / (i_2 + consoleMessageCount - 1)
            var i_5 = 4
            if (consoleMessageCount > 1) {
                i_5 += (i_3 - i_4) * (consoleMessageCount - 1 - scrollOffset) / (consoleMessageCount - 1)
            }
            toolkit.drawFilledRectangle(GameShell.screenCanvasWidth - 16, i_5, 12, i_4, backgroundAlpha shl 24 or 0x332277, 2)
            for (i_6 in scrollOffset until i_2 + scrollOffset) {
                if (i_6 >= consoleMessageCount) break
                val arr_7 = Utils.split(displayedOutput!![i_6], '\b')
                val i_8 = (GameShell.screenCanvasWidth - 8 - 16) / arr_7.size
                for (i_9 in arr_7.indices) {
                    val i_10 = i_9 * i_8 + 8
                    toolkit.setClipping(i_10, 0, i_8 + i_10 - 8, 350)
                    FontManager.debugOverlayFont!!.renderPlain(formatCommand(arr_7[i_9]), i_10, 350 - inputLineHeight - 2 - FontManager.messageboxFont!!.bottomPadding - (i_6 - scrollOffset) * outputLineHeight, -1, 0xffffff.inv())
                }
            }
        }
        Renderers.FONT_RENDERER!!.renderLeftAligned("727 1", GameShell.screenCanvasWidth - 25, 330, -1, 0xffffff.inv())
        toolkit.setClipping(0, 0, GameShell.screenCanvasWidth, 350)
        toolkit.drawOpaqueHorizontalLine(0, 350 - inputLineHeight, GameShell.screenCanvasWidth, -1)
        MiniMenu.minimenuHeader!!.renderPlain("--> ${formatCommand(commandString)}", 10, 350 - FontMetrics.aClass4143438!!.bottomPadding - 1, -1, 0xffffff.inv())
        if (GameShell.appletHasFocus) {
            var lineColor = -1
            if (Client.cycles % 30 > 15) {
                lineColor = 16777215
            }
            toolkit.drawOpaqueVerticalLine(10 + FontMetrics.aClass4143438!!.getWidthNoSprites("--> ${formatCommand(commandString).substring(0, commandLength)}"), 350 - FontMetrics.aClass4143438!!.bottomPadding - 11, 12, lineColor)
        }
        toolkit.resetClipping()
    }

    @JvmStatic
    fun setConsoleText(string_0: String) {
        commandString = string_0
        commandLength = commandString.length
    }

    @JvmStatic
    fun shutdown() {
        if (outputStream != null) {
            try {
                outputStream!!.close()
            } catch (ignored: IOException) {
                // NOOP
            }
        }
        outputStream = null
    }

    @JvmStatic
    fun formatCommand(command: String): String {
        var prefix: String? = null
        val arrowIndex = command.indexOf("--> ")
        var commandVar = command
        if (arrowIndex >= 0) {
            prefix = command.substring(0, arrowIndex + 4)
            commandVar = command.substring(arrowIndex + 4)
        }

        if (commandVar.startsWith("directlogin ")) {
            val directLoginIndex = commandVar.indexOf(' ', "directlogin ".length)
            if (directLoginIndex >= 0) {
                val end = commandVar.length
                commandVar = commandVar.substring(0, directLoginIndex) + " "

                for (start in directLoginIndex + 1 until end) {
                    commandVar += "*"
                }
            }
        }

        return if (prefix != null) prefix + commandVar else commandVar
    }
}
