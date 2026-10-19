package com.jagex.game.runetek5.config.rsinterface.cs2.ops

import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode

/**
 * Two-level opcode dispatch.
 *
 * A single 1006-branch `when` compiles to roughly 14,000 bytecodes, well past
 * HotSpot's 8000-bytecode `HugeMethodLimit`. With `DontCompileHugeMethods` on by
 * default that method is never JIT-compiled, so the client's hottest dispatch runs
 * interpreted forever. Routing on [ClientScriptOpCode.familyId] first keeps this
 * method and every per-family `when` small enough to compile.
 *
 * Switch on the `Int` family id rather than an enum: `when` over an enum adds a
 * `WhenMappings` lookup plus an `ordinal()` call, while an `Int` compiles straight
 * to a `tableswitch`.
 */
object CS2Dispatch {

    @JvmStatic
    fun execute(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation.familyId) {
            0 -> CoreVmOps.dispatch(operation, exec)
            1 -> ComponentLifecycleOps.dispatch(operation, exec)
            2 -> ComponentSetterOps.dispatch(operation, exec)
            3 -> ComponentQueryOps.dispatch(operation, exec)
            4 -> InterfaceSetterOps.dispatch(operation, exec)
            5 -> InterfaceQueryOps.dispatch(operation, exec)
            6 -> DialogOps.dispatch(operation, exec)
            7 -> PlayerWorldOps.dispatch(operation, exec)
            8 -> EnumEmailFriendOps.dispatch(operation, exec)
            9 -> ClanStockOps.dispatch(operation, exec)
            10 -> MathStringOps.dispatch(operation, exec)
            11 -> ConfigTypeChatOps.dispatch(operation, exec)
            12 -> QuickChatDisplayOps.dispatch(operation, exec)
            13 -> CameraLoginPrefOps.dispatch(operation, exec)
            14 -> WorldMapUserDetailOps.dispatch(operation, exec)
            15 -> DetailCapabilityOps.dispatch(operation, exec)
            16 -> QuestOps.dispatch(operation, exec)
            else -> throw IllegalStateException("unrouted opcode $operation")
        }
    }
}
