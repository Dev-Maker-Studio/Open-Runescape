package com.jagex.game.runetek5.config.rsinterface.cs2

import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.CoreVmOps
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class CS2ArrayInvariantTest {
    private data class Instruction(val opcode: ClientScriptOpCode, val operand: Int = 0, val text: String = "", val long: Long = 0)

    private fun script(
        id: Int,
        vararg instructions: Instruction,
        ints: Int = 0,
        strings: Int = 0,
        longs: Int = 0,
        intArgs: Int = 0,
        stringArgs: Int = 0,
        longArgs: Int = 0
    ): CS2Script {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeByte(0)
            for (instruction in instructions) {
                out.writeShort(instruction.opcode.opcode)
                when (instruction.opcode) {
                    ClientScriptOpCode.PUSH_CONSTANT_STRING -> {
                        out.writeBytes(instruction.text)
                        out.writeByte(0)
                    }
                    ClientScriptOpCode.PUSH_LONG_CONSTANT -> out.writeLong(instruction.long)
                    else -> if (instruction.opcode.hasIntConstant) out.writeInt(instruction.operand) else out.writeByte(instruction.operand)
                }
            }
            out.writeInt(instructions.size)
            for (count in intArrayOf(ints, strings, longs, intArgs, stringArgs, longArgs)) out.writeShort(count)
            out.writeByte(0)
            out.writeShort(1)
        }
        return CS2Script.decodeScript(bytes.toByteArray()).apply { pointer = id.toLong() }
    }

    private fun executor(operand: Int) = CS2Executor().apply {
        intOpValues = intArrayOf(operand)
        instrPtr = 0
    }

    private fun push(executor: CS2Executor, vararg values: Int) {
        for (value in values) executor.intStack[executor.intStackPtr++] = value
    }

    private fun define(executor: CS2Executor, id: Int, length: Int, type: Int = 105) {
        executor.intOpValues = intArrayOf((id shl 16) or type)
        executor.instrPtr = 0
        push(executor, length)
        CoreVmOps.arrayNew(executor)
    }

    @After
    fun clearExecutors() {
        CS2Executor.CS2_CACHE.reset()
        CS2Executor.CS2_EXECUTORS.clear()
        CS2Executor.CURRENT_CS2_EXEC_IDX = 0
    }

    @Test
    fun arrayDefinitionUsesPackedSlotTypeAndLogicalLength() {
        val executor = executor(0)
        define(executor, 4, 5000)
        assertEquals(5000, executor.globalArrayLengths[4])
        assertTrue(executor.globalArrays[4].all { it == 0 })
        define(executor, 4, 2, 115)
        assertArrayEquals(intArrayOf(-1, -1, 0), executor.globalArrays[4].copyOf(3))
        assertEquals(2, executor.globalArrayLengths[4])
        for (length in intArrayOf(-1, 5001)) {
            assertThrows(RuntimeException::class.java) { define(executor, 4, length) }
            assertEquals(2, executor.globalArrayLengths[4])
        }
        assertEquals(0, executor.intStackPtr)
    }

    @Test
    fun loadsAndStoresEnforceLogicalBoundsRatherThanBackingStorage() {
        val executor = executor(0)
        define(executor, 0, 2)
        executor.intOpValues = intArrayOf(0)
        push(executor, 777, 1, 42)
        CoreVmOps.arrayStore(executor)
        assertEquals(1, executor.intStackPtr)
        push(executor, 1)
        CoreVmOps.arrayLoad(executor)
        assertArrayEquals(intArrayOf(777, 42), executor.intStack.copyOf(executor.intStackPtr))
        executor.intStackPtr = 0
        for (index in intArrayOf(-1, 2, 4999)) {
            push(executor, index)
            assertThrows(RuntimeException::class.java) { CoreVmOps.arrayLoad(executor) }
            assertEquals(0, executor.intStackPtr)
            push(executor, index, 55)
            assertThrows(RuntimeException::class.java) { CoreVmOps.arrayStore(executor) }
            assertEquals(0, executor.intStackPtr)
        }
        define(executor, 0, 0)
        executor.intOpValues = intArrayOf(0)
        push(executor, 0)
        assertThrows(RuntimeException::class.java) { CoreVmOps.arrayLoad(executor) }
    }

    @Test
    fun gosubMarshalsTypedArgumentsInStackOrderAndSharesArraysOnReturn() {
        val caller = script(900001, Instruction(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900002))
        val child = script(900002, Instruction(ClientScriptOpCode.RETURN), ints = 3, strings = 2, longs = 1, intArgs = 2, stringArgs = 2, longArgs = 1)
        CS2Executor.CS2_CACHE.put(child, 900002)
        val executor = executor(900002).apply {
            current = caller
            operations = caller.operations
            intLocals = intArrayOf(71)
            objectLocals = arrayOf("caller")
            longLocals = longArrayOf(72)
            stringStack[0] = "lower"
            stringStack[1] = "first"
            stringStack[2] = "second"
            stringStackPtr = 3
            longStack[0] = 73
            longStack[1] = 74
            longStackPtr = 2
        }
        define(executor, 0, 4)
        executor.intOpValues = caller.intOpValues
        push(executor, 70, 11, 22)
        CoreVmOps.callCS2Script(executor)
        assertArrayEquals(intArrayOf(11, 22, 0), executor.intLocals)
        assertArrayEquals(arrayOf<Any?>("first", "second"), executor.objectLocals)
        assertArrayEquals(longArrayOf(74), executor.longLocals)
        assertEquals(1, executor.intStackPtr)
        assertEquals(1, executor.stringStackPtr)
        assertEquals(1, executor.longStackPtr)
        assertEquals(4, executor.globalArrayLengths[0])
        define(executor, 0, 2)
        executor.globalArrays[0][1] = 91
        CoreVmOps.returnInstr(executor)
        assertSame(caller, executor.current)
        assertArrayEquals(intArrayOf(71), executor.intLocals)
        assertArrayEquals(arrayOf<Any?>("caller"), executor.objectLocals)
        assertArrayEquals(longArrayOf(72), executor.longLocals)
        assertEquals(2, executor.globalArrayLengths[0])
        assertEquals(91, executor.globalArrays[0][1])
        assertEquals(0, executor.returnValuesPtr)
    }

    @Test
    fun parsedNestedScriptsShareArrayRedefinitionsThroughTheDispatchLoop() {
        val child = script(900007,
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_INT, 2),
            Instruction(ClientScriptOpCode.PUSH_ARRAY_INT, 0),
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_INT, 1),
            Instruction(ClientScriptOpCode.DEFINE_ARRAY, 105),
            Instruction(ClientScriptOpCode.RETURN))
        val root = script(900006,
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_INT, 3),
            Instruction(ClientScriptOpCode.DEFINE_ARRAY, 105),
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_INT, 2),
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_INT, 81),
            Instruction(ClientScriptOpCode.POP_ARRAY_INT, 0),
            Instruction(ClientScriptOpCode.GOSUB_WITH_PARAMS, 900007),
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_INT, 0),
            Instruction(ClientScriptOpCode.PUSH_ARRAY_INT, 0),
            Instruction(ClientScriptOpCode.RETURN))
        CS2Executor.CS2_CACHE.put(child, 900007)
        val executor = CS2Executor.getNextScriptExecutor()
        CS2Executor.executeScript(root, 100, executor)
        assertArrayEquals(intArrayOf(81, 0), executor.intStack.copyOf(executor.intStackPtr))
        assertEquals(1, executor.globalArrayLengths[0])
        assertEquals(0, CS2Executor.CURRENT_CS2_EXEC_IDX)
        CS2Executor.getNextScriptExecutor()
        CS2Executor.executeScript(script(900008, Instruction(ClientScriptOpCode.RETURN)), 100, executor)
        assertEquals(1, executor.globalArrayLengths[0])
        assertEquals(0, executor.intStackPtr)
    }

    @Test
    fun hookMarshalsMixedParametersIntoIndependentTypedLocals() {
        val script = script(900003, Instruction(ClientScriptOpCode.RETURN), ints = 2, strings = 2, longs = 1, intArgs = 2, stringArgs = 2, longArgs = 1)
        CS2Executor.CS2_CACHE.put(script, 900003)
        CS2Executor.executeHookInner(HookRequest().apply { params = arrayOf(900003, 11, "first", 22, "second", 33L) })
        val executor = CS2Executor.CS2_EXECUTORS.single()
        assertArrayEquals(intArrayOf(11, 22), executor.intLocals)
        assertArrayEquals(arrayOf<Any?>("first", "second"), executor.objectLocals)
        assertArrayEquals(longArrayOf(33), executor.longLocals)
        assertEquals(0, CS2Executor.CURRENT_CS2_EXEC_IDX)
    }

    @Test
    fun parserKeepsWideArrayOperandsAndTypedConstantsAligned() {
        val script = script(900004,
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_INT, 5000),
            Instruction(ClientScriptOpCode.DEFINE_ARRAY, (4 shl 16) or 105),
            Instruction(ClientScriptOpCode.PUSH_CONSTANT_STRING, text = "fixture"),
            Instruction(ClientScriptOpCode.PUSH_LONG_CONSTANT, long = 0x123456789abcdefL),
            Instruction(ClientScriptOpCode.PUSH_ARRAY_INT, 4),
            Instruction(ClientScriptOpCode.POP_ARRAY_INT, 4),
            Instruction(ClientScriptOpCode.RETURN),
            ints = 3, strings = 2, longs = 1, intArgs = 2, stringArgs = 1, longArgs = 1)
        assertEquals(7, script.operations!!.size)
        assertEquals((4 shl 16) or 105, script.intOpValues!![1])
        assertEquals("fixture", script.stringOpValues!![2])
        assertEquals(0x123456789abcdefL, script.longOpValues!![3])
        assertEquals(4, script.intOpValues!![4])
        assertEquals(4, script.intOpValues!![5])
        assertArrayEquals(intArrayOf(3, 2, 1, 2, 1, 1), intArrayOf(script.intLocalsCount, script.stringLocalsCount, script.longLocalsCount, script.intArgsCount, script.stringArgsCount, script.longArgsCount))
        assertEquals(859, ClientScriptOpCode.PUSH_ARRAY_INT.opcode)
        assertArrayEquals(IntArray(1006) { it }, ClientScriptOpCode.entries.map { it.opcode }.filter { it < 1006 }.sorted().toIntArray())
    }

    @Test
    fun diagnosticsAreBoundedNumericOnlyAndPreserveDefinitionAcrossRuns() {
        val diagnostics = CS2ArrayDiagnostics()
        val executor = executor(105).apply {
            current = script(900005, Instruction(ClientScriptOpCode.RETURN)).apply { scriptName = "private script name" }
            stringStack[0] = "private player text"
            intStack[0] = 987654321
        }
        define(executor, 0, 3)
        diagnostics.defined(executor)
        diagnostics.begin()
        executor.intOpValues = intArrayOf(0)
        push(executor, -1)
        repeat(100) { diagnostics.record(executor, ClientScriptOpCode.PUSH_CONSTANT_STRING) }
        diagnostics.record(executor, ClientScriptOpCode.PUSH_ARRAY_INT)
        val report = diagnostics.report(executor)
        assertEquals(64, report.lineSequence().count { it.startsWith("[CS2 array trace]") })
        assertTrue(report.contains("opcode=859 array=0 index=-1 length=3"))
        assertTrue(report.contains("[CS2 array definition] script=900005 instruction=0 opcode=235 array=0 length=3"))
        assertFalse(report.contains("private"))
        assertFalse(report.contains("987654321"))
        assertEquals(1, executor.intStackPtr)
        assertEquals(-1, executor.intStack[0])
        assertEquals(3, executor.globalArrayLengths[0])
    }
}
