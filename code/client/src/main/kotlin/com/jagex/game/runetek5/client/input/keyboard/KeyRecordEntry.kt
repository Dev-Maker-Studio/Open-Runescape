package com.jagex.game.runetek5.client.input.keyboard

import com.jagex.game.runetek5.cache.node.Node

/**
 *
 * @author Brainpower - 18/04/2022
 *
 */
class KeyRecordEntry : Node<KeyRecordEntry>(), KeyRecord {

    @JvmField var timeSubmitted: Long = 0
    @JvmField var keyCharacter: Char = '\u0000'
    @JvmField var keyCode: Int = 0
    @JvmField var type: Int = 0
    @JvmField var flags: Int = 0

    override fun getType(): Int {
        return this.type
    }

    override fun getCharacter(): Char {
        return this.keyCharacter
    }

    override fun getCode(): Int {
        return this.keyCode
    }

    override fun getFlags(): Int {
        return this.flags
    }

    override fun getTimeSubmitted(): Long {
        return this.timeSubmitted
    }
}
