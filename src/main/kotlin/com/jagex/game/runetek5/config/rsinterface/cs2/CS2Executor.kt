package com.jagex.game.runetek5.config.rsinterface.cs2

import com.jagex.game.runetek5.cache.node.CacheableNodeQueue
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.entity.grounditem.GroundItem
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.trigger.ClientTriggerType
import com.jagex.game.runetek5.utils.DirectoryPicker

class CS2Executor {

    @JvmField
    var intLocals: IntArray? = null

    @JvmField
    var longLocals: LongArray? = null

    @JvmField
    var playerIndex: Int = 0

    @JvmField
    var operations: Array<ClientScriptOpCode>? = null

    @JvmField
    var currentEntity: PathingEntity? = null

    @JvmField
    var filePicker: DirectoryPicker? = null

    @JvmField
    var currentQuickChatMessage: QuickChatMessage? = null

    @JvmField
    var currentClanSettings: ClanSettings? = null

    @JvmField
    var clanChannel: ClanChannel? = null

    @JvmField
    var currentLocation: GameObject? = null

    @JvmField
    var currentGroundItem: GroundItem? = null

    @JvmField
    var objectLocals: Array<Any?>? = null

    @JvmField
    var intOpValues: IntArray? = null

    @JvmField
    var current: CS2Script? = null

    @JvmField
    var hasIntOperand: Boolean = false

    @JvmField
    val globalArrayLengths: IntArray = IntArray(5)

    @JvmField
    val globalArrays: Array<IntArray> = Array(5) { IntArray(5000) }

    @JvmField
    val intStack: IntArray = IntArray(1000)

    @JvmField
    var intStackPtr: Int = 0

    @JvmField
    val longStack: LongArray = LongArray(1000)

    @JvmField
    var longStackPtr: Int = 0

    @JvmField
    val stringStack: Array<Any?> = arrayOfNulls(1000)

    @JvmField
    var stringStackPtr: Int = 0

    @JvmField
    var returnValuesPtr: Int = 0

    @JvmField
    val returnValues: Array<CS2ReturnValue?> = arrayOfNulls(50)

    @JvmField
    val hookedInterface1: CS2Interface = CS2Interface()

    @JvmField
    val hookedInterface2: CS2Interface = CS2Interface()

    @JvmField
    var hookRequestCount: Int = 0

    @JvmField
    var instrPtr: Int = -1

    companion object {
        @JvmField
        val CS2_CACHE: CacheableNodeQueue = CacheableNodeQueue(128)

        @JvmField
        var CURRENT_CS2_EXEC_IDX: Int = 0

        @JvmField
        val CS2_EXECUTORS: ArrayList<CS2Executor> = ArrayList()

        @JvmField
        var instructionCount: Int = 0

        @JvmField
        var debugFilterScript: String? = null

        @JvmField
        var isDebugTracing: Boolean = false

        @JvmField
        val coordConversionBuffer: IntArray = IntArray(3)

        @JvmField
        var nextUniqueId: Int = 0

        @JvmStatic
        fun logError(errorString: String) {
            println("\u001B[31m" + "CS2 Error: " + formatCs2Error(errorString, " ", "\n     ") + "\u001B[0m")
        }

        @JvmStatic
        fun execute_trigger(triggerType: ClientTriggerType, scriptId: Int, i_2: Int, cs2executor3: CS2Executor) {
            val cs2script5 = CS2Script.getScript(triggerType, scriptId, i_2)
            if (cs2script5 == null) {
                decrementCS2ExecIdx()
            } else {
                cs2executor3.intLocals = IntArray(cs2script5.intLocalsCount)
                cs2executor3.objectLocals = arrayOfNulls(cs2script5.stringLocalsCount)
                if (cs2script5.triggerType != ClientTriggerType.MEC_MOUSE_ENTER && cs2script5.triggerType != ClientTriggerType.MEC_MOUSE_OVER && cs2script5.triggerType != ClientTriggerType.MEC_MOUSE_LEAVE) {
                    if (cs2script5.triggerType == ClientTriggerType.IF_PLAYER_CYCLE) {
                        cs2executor3.intLocals!![0] = cs2executor3.playerIndex
                    }
                } else {
                    var i_6 = 0
                    var i_7 = 0
                    if (WorldMap.worldmapComponent != null) {
                        i_6 = WorldMap.worldmapComponent!!.x
                        i_7 = WorldMap.worldmapComponent!!.y
                    }
                    cs2executor3.intLocals!![0] = MouseRecord.recorder!!.getMouseX() - i_6
                    cs2executor3.intLocals!![1] = MouseRecord.recorder!!.getMouseY() - i_7
                }
                executeScript(cs2script5, 200000, cs2executor3)
            }
        }

        @JvmStatic
        fun executeScript(cs2script0: CS2Script, stackLimit: Int, cs2executor2: CS2Executor) {
            cs2executor2.intStackPtr = 0
            cs2executor2.stringStackPtr = 0
            cs2executor2.instrPtr = -1
            cs2executor2.current = cs2script0
            cs2executor2.operations = cs2executor2.current!!.operations
            cs2executor2.intOpValues = cs2executor2.current!!.intOpValues
            var cs2opinfo4: ClientScriptOpCode? = null
            cs2executor2.returnValuesPtr = 0
            try {
                try {
                    instructionCount = 0
                    while (true) {
                        ++instructionCount
                        if (instructionCount > stackLimit) {
                            throw RuntimeException("")
                        }
                        cs2opinfo4 = cs2executor2.operations!![++cs2executor2.instrPtr]
                        if (isDebugTracing && (debugFilterScript == null || cs2executor2.current!!.scriptName != null && cs2executor2.current!!.scriptName!!.indexOf(debugFilterScript!!) != -1)) {
                            println(cs2executor2.current!!.scriptName + ": " + cs2opinfo4)
                        }
                        cs2executor2.hasIntOperand = cs2executor2.intOpValues!![cs2executor2.instrPtr] == 1
                        if (cs2opinfo4 == ClientScriptOpCode.RETURN && cs2executor2.returnValuesPtr == 0) {
                            decrementCS2ExecIdx()
                            break
                        }
                        CS2Interpreter.executeOperation(cs2opinfo4, cs2executor2)
                    }
                } catch (exception8: Exception) {
                    val stringbuilder6 = StringBuilder(30)
                    stringbuilder6.append(cs2executor2.current!!.pointer).append(" ")
                    for (i_7 in cs2executor2.returnValuesPtr - 1 downTo 0) {
                        stringbuilder6.append(cs2executor2.returnValues[i_7]!!.savedScript!!.pointer).append(" ")
                    }
                    assert(cs2opinfo4 != null)
                    stringbuilder6.append(cs2opinfo4!!.opcode)
                    PacketEncoder.sendErrorToServer(stringbuilder6.toString(), exception8)
                    decrementCS2ExecIdx()
                }
            } catch (exception9: Exception) {
                decrementCS2ExecIdx()
            }
        }

        @JvmStatic
        fun executeHookInner(hook: HookRequest) {
            execute_hook_inner(hook, 200000)
        }

        @JvmStatic
        fun execute_hook_inner(hook: HookRequest, stackLimit: Int) {
            val params = hook.params!!
            val scriptId = (params[0] as Int).toInt()
            val script = CS2Script.getCs2Script(scriptId)
            if (script != null) {
                val executor = getNextScriptExecutor()
                executor.intLocals = IntArray(script.intLocalsCount)
                var intLocalCount = 0
                executor.objectLocals = arrayOfNulls(script.stringLocalsCount)
                var objectLocalCount = 0
                executor.longLocals = LongArray(script.longLocalsCount)
                var longLocalCount = 0

                for (paramIdx in 1 until params.size) {
                    when (params[paramIdx]) {
                        is Int -> {
                            var intLocal = (params[paramIdx] as Int).toInt()
                            if (intLocal == -2147483647) {
                                intLocal = hook.mouseX
                            }

                            if (intLocal == -2147483646) {
                                intLocal = hook.mouseY
                            }

                            if (intLocal == -2147483645) {
                                intLocal = if (hook.source != null) hook.source!!.idHash else -1
                            }

                            if (intLocal == -2147483644) {
                                intLocal = hook.clickedOption
                            }

                            if (intLocal == -2147483643) {
                                intLocal = if (hook.source != null) hook.source!!.slotId else -1
                            }

                            if (intLocal == -2147483642) {
                                intLocal = if (hook.targetComponent != null) hook.targetComponent!!.idHash else -1
                            }

                            if (intLocal == -2147483641) {
                                intLocal = if (hook.targetComponent != null) hook.targetComponent!!.slotId else -1
                            }

                            if (intLocal == -2147483640) {
                                intLocal = hook.typedKeyCode
                            }

                            if (intLocal == -2147483639) {
                                intLocal = hook.typedKeyChar
                            }

                            executor.intLocals!![intLocalCount++] = intLocal
                        }
                        is String -> {
                            var string_13 = params[paramIdx] as String
                            if ("event_opbase" == string_13) {
                                string_13 = hook.optionName ?: ""
                            }

                            executor.objectLocals!![objectLocalCount++] = string_13
                        }
                        is Long -> {
                            val long_14 = (params[paramIdx] as Long).toLong()
                            executor.longLocals!![longLocalCount++] = long_14
                        }
                    }
                }

                executor.hookRequestCount = hook.requestId
                executeScript(script, stackLimit, executor)
            }
        }

        @JvmStatic
        fun executeCutsceneScript(cutsceneId: Int, string_1: String, i_2: Int) {
            val script = CS2Script.getScript(ClientTriggerType.RUN_CUTSCENE_SCRIPT, cutsceneId, -1)
            if (script != null) {
                val executor = getNextScriptExecutor()
                executor.intLocals = IntArray(script.intLocalsCount)
                executor.objectLocals = arrayOfNulls(script.stringLocalsCount)
                executor.objectLocals!![0] = string_1
                executor.intLocals!![0] = i_2
                executeScript(script, 200000, executor)
            }
        }

        @JvmStatic
        fun decrementCS2ExecIdx() {
            --CURRENT_CS2_EXEC_IDX
        }

        @JvmStatic
        fun getNextScriptExecutor(): CS2Executor {
            if (CURRENT_CS2_EXEC_IDX == CS2_EXECUTORS.size) {
                CS2_EXECUTORS.add(CS2Executor())
            }
            return CS2_EXECUTORS[CURRENT_CS2_EXEC_IDX++]
        }

        @JvmStatic
        fun generateUniqueId(): Long {
            return (++nextUniqueId - 1).toLong() shl 32 or 0xffffffffL
        }

        @JvmStatic
        fun executeIfTrigger(class3970: ClientTriggerType, i_1: Int, animable3: PathingEntity, i_4: Int) {
            val cs2executor6 = getNextScriptExecutor()
            cs2executor6.currentEntity = animable3
            cs2executor6.playerIndex = i_4
            execute_trigger(class3970, i_1, -1, cs2executor6)
            cs2executor6.currentEntity = null
            cs2executor6.playerIndex = -1
        }

        @JvmStatic
        fun ifObjectTrigger(class3970: ClientTriggerType, i_1: Int, sceneobject3: GameObject) {
            val cs2executor5 = getNextScriptExecutor()
            cs2executor5.currentLocation = sceneobject3
            execute_trigger(class3970, i_1, -1, cs2executor5)
            cs2executor5.currentLocation = null
        }

        @JvmStatic
        fun executeIfOnGitem(triggerType: ClientTriggerType, interfaceId: Int, groundItem: GroundItem) {
            val cs2executor6 = getNextScriptExecutor()
            cs2executor6.currentGroundItem = groundItem
            execute_trigger(triggerType, interfaceId, -1, cs2executor6)
            cs2executor6.currentGroundItem = null
        }

        @JvmStatic
        fun queryItemParamString(itemName: String, tradeableOnly: Boolean, paramId: Int, paramString: String) {
            CS2Interpreter.queryItem(itemName, tradeableOnly, paramId, -1, paramString, true)
        }

        @JvmStatic
        fun executeTrigger(triggerType: ClientTriggerType, i_1: Int, i_2: Int) {
            val cs2Executor = getNextScriptExecutor()
            execute_trigger(triggerType, i_1, i_2, cs2Executor)
        }

        @JvmStatic
        fun resetCs2Cache() {
            CS2_CACHE.reset()
        }

        @JvmStatic
        fun formatCs2Error(errorString: String, errorPrevious: String, errorNext: String): String {
            var errorStringMod = errorString
            var exceptionStr = ""
            if (errorStringMod.contains("|")) {
                exceptionStr = errorStringMod.substring(errorStringMod.indexOf("|") + 1, errorStringMod.indexOf("|", errorStringMod.indexOf("|") + 1))
                errorStringMod = errorStringMod.replaceFirst(exceptionStr, "")
                exceptionStr = exceptionStr.replace(" ", "")
                errorStringMod = "$exceptionStr $errorStringMod"
            }
            var i = errorStringMod.indexOf(errorPrevious)
            while (i != -1) {
                errorStringMod = errorStringMod.substring(0, i) + errorNext + errorStringMod.substring(i + errorPrevious.length)
                i = errorStringMod.indexOf(errorPrevious, i + errorNext.length)
            }
            return errorStringMod
        }
    }
}
