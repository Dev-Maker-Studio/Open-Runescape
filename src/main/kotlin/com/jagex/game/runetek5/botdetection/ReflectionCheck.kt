package com.jagex.game.runetek5.botdetection

import com.jagex.game.runetek5.cache.node.Node
import java.lang.reflect.Field
import java.lang.reflect.Method

@Suppress("UNCHECKED_CAST")
class ReflectionCheck : Node<Any?>() {
    @JvmField
    var operationCount: Int = 0

    @JvmField
    var methodsToCheck: Array<Method>? = null

    @JvmField
    var operations: IntArray? = null

    @JvmField
    var exceptionsEncountered: IntArray? = null

    @JvmField
    var fieldsToCheck: Array<Field>? = null

    @JvmField
    var header: Int = 0

    @JvmField
    var fieldInput: IntArray? = null

    @JvmField
    var argumentBuffers: Array<Array<ByteArray?>>? = null
}
