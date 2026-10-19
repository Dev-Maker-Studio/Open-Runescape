package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.entity.RenderedEntity
import java.util.Stack

object RenderedEntityCache {
    @JvmField var stack: Stack<RenderedEntity> = Stack()

    @JvmStatic
    fun generate(interactive: Boolean): RenderedEntity {
        synchronized(stack) {
            val entry: RenderedEntity = if (stack.isEmpty()) {
                RenderedEntity()
            } else {
                stack.pop()
            }
            entry.interactive = interactive
            return entry
        }
    }

    @JvmStatic
    fun clear(interaction: RenderedEntity) {
        interaction.entity = null
        synchronized(stack) {
            if (stack.size < 200) {
                stack.push(interaction)
            }
        }
    }

    @JvmStatic
    fun reset() {
        synchronized(stack) {
            stack = Stack()
        }
    }
}
