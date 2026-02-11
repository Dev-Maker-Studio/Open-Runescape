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
        fun execute_trigger(triggerType: ClientTriggerType, scriptId: Int, alternateId: Int, executor: CS2Executor) {
            val script = CS2Script.getScript(triggerType, scriptId, alternateId)
            if (script == null) {
                decrementCS2ExecIdx()
            } else {
                executor.intLocals = IntArray(script.intLocalsCount)
                executor.objectLocals = arrayOfNulls(script.stringLocalsCount)
                if (script.triggerType != ClientTriggerType.MEC_MOUSE_ENTER && script.triggerType != ClientTriggerType.MEC_MOUSE_OVER && script.triggerType != ClientTriggerType.MEC_MOUSE_LEAVE) {
                    if (script.triggerType == ClientTriggerType.IF_PLAYER_CYCLE) {
                        executor.intLocals!![0] = executor.playerIndex
                    }
                } else {
                    var worldMapOffsetX = 0
                    var worldMapOffsetY = 0
                    if (WorldMap.worldmapComponent != null) {
                        worldMapOffsetX = WorldMap.worldmapComponent!!.x
                        worldMapOffsetY = WorldMap.worldmapComponent!!.y
                    }
                    executor.intLocals!![0] = MouseRecord.recorder!!.getMouseX() - worldMapOffsetX
                    executor.intLocals!![1] = MouseRecord.recorder!!.getMouseY() - worldMapOffsetY
                }
                executeScript(script, 200000, executor)
            }
        }

        @JvmStatic
        fun executeScript(script: CS2Script, stackLimit: Int, executor: CS2Executor) {
            executor.intStackPtr = 0
            executor.stringStackPtr = 0
            executor.instrPtr = -1
            executor.current = script
            executor.operations = executor.current!!.operations
            executor.intOpValues = executor.current!!.intOpValues
            var currentOpcode: ClientScriptOpCode? = null
            executor.returnValuesPtr = 0
            try {
                try {
                    instructionCount = 0
                    while (true) {
                        ++instructionCount
                        if (instructionCount > stackLimit) {
                            throw RuntimeException("")
                        }
                        currentOpcode = executor.operations!![++executor.instrPtr]
                        if (isDebugTracing && (debugFilterScript == null || executor.current!!.scriptName != null && executor.current!!.scriptName!!.indexOf(debugFilterScript!!) != -1)) {
                            println(executor.current!!.scriptName + ": " + currentOpcode)
                        }
                        executor.hasIntOperand = executor.intOpValues!![executor.instrPtr] == 1
                        if (currentOpcode == ClientScriptOpCode.RETURN && executor.returnValuesPtr == 0) {
                            decrementCS2ExecIdx()
                            break
                        }
                        CS2Interpreter.executeOperation(currentOpcode, executor)
                    }
                } catch (scriptException: Exception) {
                    val errorBuilder = StringBuilder(30)
                    errorBuilder.append(executor.current!!.pointer).append(" ")
                    for (returnIdx in executor.returnValuesPtr - 1 downTo 0) {
                        errorBuilder.append(executor.returnValues[returnIdx]!!.savedScript!!.pointer).append(" ")
                    }
                    assert(currentOpcode != null)
                    errorBuilder.append(currentOpcode!!.opcode)
                    PacketEncoder.sendErrorToServer(errorBuilder.toString(), scriptException)
                    decrementCS2ExecIdx()
                }
            } catch (fatalException: Exception) {
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
                            var stringParam = params[paramIdx] as String
                            if ("event_opbase" == stringParam) {
                                stringParam = hook.optionName ?: ""
                            }

                            executor.objectLocals!![objectLocalCount++] = stringParam
                        }
                        is Long -> {
                            val longParam = (params[paramIdx] as Long).toLong()
                            executor.longLocals!![longLocalCount++] = longParam
                        }
                    }
                }

                executor.hookRequestCount = hook.requestId
                executeScript(script, stackLimit, executor)
            }
        }

        @JvmStatic
        fun executeCutsceneScript(cutsceneId: Int, scriptParam: String, intParam: Int) {
            val script = CS2Script.getScript(ClientTriggerType.RUN_CUTSCENE_SCRIPT, cutsceneId, -1)
            if (script != null) {
                val executor = getNextScriptExecutor()
                executor.intLocals = IntArray(script.intLocalsCount)
                executor.objectLocals = arrayOfNulls(script.stringLocalsCount)
                executor.objectLocals!![0] = scriptParam
                executor.intLocals!![0] = intParam
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
        fun executeIfTrigger(triggerType: ClientTriggerType, scriptId: Int, entity: PathingEntity, playerIdx: Int) {
            val executor = getNextScriptExecutor()
            executor.currentEntity = entity
            executor.playerIndex = playerIdx
            execute_trigger(triggerType, scriptId, -1, executor)
            executor.currentEntity = null
            executor.playerIndex = -1
        }

        @JvmStatic
        fun ifObjectTrigger(triggerType: ClientTriggerType, scriptId: Int, gameObject: GameObject) {
            val executor = getNextScriptExecutor()
            executor.currentLocation = gameObject
            execute_trigger(triggerType, scriptId, -1, executor)
            executor.currentLocation = null
        }

        @JvmStatic
        fun executeIfOnGitem(triggerType: ClientTriggerType, interfaceId: Int, groundItem: GroundItem) {
            val executor = getNextScriptExecutor()
            executor.currentGroundItem = groundItem
            execute_trigger(triggerType, interfaceId, -1, executor)
            executor.currentGroundItem = null
        }

        @JvmStatic
        fun queryItemParamString(itemName: String, tradeableOnly: Boolean, paramId: Int, paramString: String) {
            CS2Interpreter.queryItem(itemName, tradeableOnly, paramId, -1, paramString, true)
        }

        @JvmStatic
        fun executeTrigger(triggerType: ClientTriggerType, scriptId: Int, alternateId: Int) {
            val cs2Executor = getNextScriptExecutor()
            execute_trigger(triggerType, scriptId, alternateId, cs2Executor)
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
