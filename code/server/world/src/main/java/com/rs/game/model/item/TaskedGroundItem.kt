package com.rs.game.model.item

import com.rs.game.World
import com.rs.game.World.getPlayerByUsername
import com.rs.game.model.entity.async.AsyncTaskScheduler
import com.rs.game.model.entity.async.ScheduledTask
import org.darkan.core.model.GroundItem
import kotlinx.coroutines.CoroutineScope

/**
 * A GroundItem wrapper that supports async task scheduling.
 * Uses composition because GroundItem is final, where legacy's TaskedGroundItem subclassed it.
 */
class TaskedGroundItem(val groundItem: GroundItem) {
    val asyncTasks = AsyncTaskScheduler()

    // Delegate common GroundItem properties
    val id get() = groundItem.id
    val amount get() = groundItem.amount
    val item get() = groundItem.item
    val tile get() = groundItem.tile
    val ownerUsername get() = groundItem.ownerUsername
    val type get() = groundItem.type
    val definitions get() = groundItem.definitions
    val metaData get() = groundItem.metaData

    fun tick() {
        groundItem.tick()
        asyncTasks.tick()
    }

    fun getMetaData(key: String): Any? = groundItem.getMetaData(key)
    fun getMetaDataI(key: String, default: Int = -1) = groundItem.getMetaDataI(key, default)
    fun addMetaData(key: String, value: Any) = groundItem.addMetaData(key, value)

    /** Allow TaskedGroundItem to be used where GroundItem is expected */
    fun asGroundItem(): GroundItem = groundItem
}

fun TaskedGroundItem.schedule(mapping: String, task: suspend ScheduledTask.(CoroutineScope) -> Unit) {
    this.asyncTasks.schedule(mapping, task)
}

fun TaskedGroundItem.schedule(task: suspend ScheduledTask.(CoroutineScope) -> Unit) {
    this.asyncTasks.schedule(null, task)
}

fun TaskedGroundItem.cancel(mapping: String) {
    this.asyncTasks.cancel(mapping)
}

fun GroundItem.getOwner() = ownerUsername?.let { getPlayerByUsername(it) }