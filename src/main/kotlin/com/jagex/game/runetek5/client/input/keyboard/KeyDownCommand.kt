package com.jagex.game.runetek5.client.input.keyboard

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.inputsubscriber.InputSubscriber
import com.jagex.game.runetek5.config.inputsubscriber.KeyHoldInputSubscriber

object KeyDownCommand {
    @JvmStatic
    fun isActive(subscriber: InputSubscriber?, mouseRecord: MouseRecord?): Boolean {
        return subscriber != null && subscriber.valid(mouseRecord, client.KEY_RECORDS, client.maximumHeldKeys, KeyRecorder.keyRecorder)
    }

    @JvmStatic
    fun isActive(keyHoldSubscriber: KeyHoldInputSubscriber?): Boolean {
        return isActive(keyHoldSubscriber, null)
    }

    @JvmStatic
    fun minimenuKeyDown(): Boolean {
        return isActive(TypeLists.INTERACTION_DEFAULTS!!.minimenuKey)
    }

    @JvmStatic
    fun teleportKeyDown(): Boolean {
        return isActive(TypeLists.INTERACTION_DEFAULTS!!.teleportKey)
    }

    @JvmStatic
    fun runKeyDown(): Boolean {
        return isActive(TypeLists.INTERACTION_DEFAULTS!!.toggleRunKey)
    }
}
