package com.darkan.bot.scripts

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

@OptIn(InternalCoroutinesApi::class)
class ClientPulseDispatcher : CoroutineDispatcher(), Delay {
    private val tasks = mutableListOf<ScheduledTask>()
    private val predicateContinuations = mutableListOf<PredicateContinuation>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        synchronized(this) {
            tasks.add(ScheduledTask(System.currentTimeMillis() + timeMillis, continuation))
        }
    }

    fun addPredicateContinuation(continuation: CancellableContinuation<Unit>, predicate: () -> Boolean) {
        synchronized(this) {
            predicateContinuations.add(PredicateContinuation(predicate, continuation))
        }
    }

    fun tick() {
        val readyContinuations = mutableListOf<CancellableContinuation<Unit>>()
        val currentTime = System.currentTimeMillis()

        synchronized(this) {
            tasks.removeAll { task ->
                if (task.scheduledTime <= currentTime) {
                    readyContinuations.add(task.continuation)
                    true
                } else {
                    false
                }
            }

            predicateContinuations.removeAll { predicateCont ->
                if (!predicateCont.predicate()) {
                    readyContinuations.add(predicateCont.continuation)
                    true
                } else {
                    false
                }
            }
        }

        readyContinuations.forEach { it.resume(Unit) }
    }

    private data class ScheduledTask(
        val scheduledTime: Long,
        val continuation: CancellableContinuation<Unit>
    )

    private data class PredicateContinuation(
        val predicate: () -> Boolean,
        val continuation: CancellableContinuation<Unit>
    )
}