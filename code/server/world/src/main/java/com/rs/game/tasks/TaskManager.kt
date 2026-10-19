package com.rs.game.tasks

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.darkan.core.Logger
import java.util.function.Function

private const val HALF_HOUR_MILLIS = 30 * 60_000L
private const val HOUR_MILLIS = 60 * 60_000L
private const val ONE_SHOT = -1

fun nextClockBoundary(nowMillis: Long, periodMillis: Long) = (nowMillis / periodMillis + 1) * periodMillis

class TaskManager {
    private val tasks = ObjectArrayList<TaskInformation>()
    private val mappedTasks = HashMap<String, TaskInformation>()

    val size get() = tasks.size

    fun processTasks() = synchronized(tasks) {
        try {
            val finished = ArrayList<TaskInformation>()
            for (task in tasks) {
                try {
                    if (task.currDelay > 0) {
                        task.currDelay--
                        continue
                    }
                    try {
                        task.task.run()
                    } catch (e: Throwable) {
                        Logger.handle(WorldTasks::class.java, "processTasksRun:${source(task)}", task.toString(), e)
                    }
                    if (task.task.needRemove) finished += task else task.currDelay = task.loopDelay
                } catch (e: Throwable) {
                    Logger.handle(WorldTasks::class.java, "processTasks:${source(task)}", task.toString(), e)
                }
            }
            for (task in finished) {
                tasks.remove(task)
                task.mapping?.let(mappedTasks::remove)
            }
        } catch (e: Throwable) {
            Logger.handle(WorldTasks::class.java, "processTasks", e)
        }
    }

    private fun source(task: TaskInformation) = task.javaClass.declaringClass?.simpleName ?: "UnknownSource"

    fun scheduleLooping(mapping: String?, task: Task?, startDelay: Int, loopDelay: Int): TaskInformation? =
        if (task == null || startDelay < 0 || loopDelay < 0) null else add(mapping, task, startDelay, loopDelay)

    fun scheduleLooping(task: Task?, startDelay: Int, loopDelay: Int) = scheduleLooping(null, task, startDelay, loopDelay)

    fun scheduleLooping(mapping: String?, startDelay: Int, loopDelay: Int, task: Runnable?): TaskInformation? =
        if (task == null || startDelay < 0 || loopDelay < 0) null else add(mapping, TaskLambda(task), startDelay, loopDelay)

    fun scheduleLooping(startDelay: Int, loopDelay: Int, task: Runnable?) = scheduleLooping(null, startDelay, loopDelay, task)

    fun schedule(mapping: String?, task: Task?, delayCount: Int): TaskInformation? =
        if (task == null || delayCount < 0) null else add(mapping, task, delayCount, ONE_SHOT)

    fun schedule(task: Task?, delayCount: Int) = schedule(null, task, delayCount)

    fun schedule(mapping: String?, task: Task?) = schedule(mapping, task, 0)

    fun schedule(task: Task?) = schedule(null, task, 0)

    fun schedule(mapping: String?, startDelay: Int, task: Runnable?): TaskInformation? =
        if (task == null || startDelay < 0) null else add(mapping, TaskLambda(task), startDelay, ONE_SHOT)

    fun schedule(startDelay: Int, task: Runnable?) = schedule(null, startDelay, task)

    fun schedule(mapping: String?, task: Runnable?) = schedule(mapping, 0, task)

    fun schedule(task: Runnable?) = schedule(null, 0, task)

    fun scheduleTimer(mapping: String?, startDelay: Int, loopDelay: Int, task: Function<Int, Boolean>?): TaskInformation? =
        if (task == null || startDelay < 0 || loopDelay < 0) null else add(mapping, TaskTimerLambda(task), startDelay, loopDelay)

    fun scheduleTimer(startDelay: Int, loopDelay: Int, task: Function<Int, Boolean>?) = scheduleTimer(null, startDelay, loopDelay, task)

    fun scheduleTimer(mapping: String?, task: Function<Int, Boolean>?) = scheduleTimer(mapping, 0, 0, task)

    fun scheduleTimer(task: Function<Int, Boolean>?) = scheduleTimer(null, 0, 0, task)

    fun scheduleTimer(mapping: String?, startDelay: Int, task: Function<Int, Boolean>?) = scheduleTimer(mapping, startDelay, 0, task)

    fun scheduleTimer(startDelay: Int, task: Function<Int, Boolean>?) = scheduleTimer(null, startDelay, 0, task)

    fun scheduleHalfHourly(task: Runnable) = scheduleOnTheClock(HALF_HOUR_MILLIS, task)

    fun scheduleNthHourly(hour: Int, task: Runnable) = scheduleOnTheClock(hour * HOUR_MILLIS, task)

    fun scheduleHourly(task: Runnable) = scheduleNthHourly(1, task)

    private fun scheduleOnTheClock(periodMillis: Long, task: Runnable): TaskInformation? {
        var next = nextClockBoundary(System.currentTimeMillis(), periodMillis)
        return scheduleLooping(0, 0) {
            val now = System.currentTimeMillis()
            if (now >= next) {
                next = nextClockBoundary(now, periodMillis)
                task.run()
            }
        }
    }

    fun remove(task: TaskInformation?) = synchronized(tasks) {
        if (task == null) return@synchronized
        tasks.remove(task)
        task.mapping?.let(mappedTasks::remove)
    }

    fun remove(mapping: String) = synchronized(tasks) {
        mappedTasks.remove(mapping)?.let(tasks::remove)
    }

    fun hasTask(mapping: String) = synchronized(tasks) { mapping in mappedTasks }

    fun getRemainingTicks(mapping: String) = synchronized(tasks) { mappedTasks[mapping]?.currDelay ?: -1 }

    fun listAllMappedTasks(): Map<String, Int> = synchronized(tasks) { mappedTasks.mapValues { it.value.currDelay } }

    private fun add(mapping: String?, task: Task, startDelay: Int, loopDelay: Int): TaskInformation = synchronized(tasks) {
        val info = TaskInformation(mapping, task, startDelay, loopDelay)
        tasks.add(info)
        if (mapping != null) mappedTasks.put(mapping, info)?.let(tasks::remove)
        info
    }
}
