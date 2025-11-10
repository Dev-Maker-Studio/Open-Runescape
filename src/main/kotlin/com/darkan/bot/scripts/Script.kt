package com.darkan.bot.scripts

import com.darkan.bot.gaussian
import com.darkan.bot.scripts.event.Chat
import com.darkan.bot.scripts.event.Event
import com.darkan.bot.scripts.event.XPDrop
import com.darkan.bot.utils.Skill
import com.jagex.game.runetek5.client.ChatMessageType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.function.Predicate
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

abstract class Script {
    private var pendingEventWaitCompleted = false
    private var pendingEventPredicate: Predicate<Event>? = null
    private val parallelScripts = mutableListOf<Script>()

    var started = false
        private set

    var stopped = false
        private set

    private val dispatcher = ClientPulseDispatcher()

    fun tick() {
        if (stopped) return

        if (!started) {
            started = true
            startCoroutineLoop()
        } else
            dispatcher.tick()
    }

    private fun startCoroutineLoop() {
        val suspendLambda: suspend () -> Unit = {
            onStart()
            while (!stopped) {
                loop()
                delay(40)
            }
            onStop()
        }
        suspendLambda.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext = dispatcher
            override fun resumeWith(result: Result<Unit>) {
                result.onFailure { it.printStackTrace() }
            }
        })
    }

    abstract suspend fun loop()

    fun stop() {
        stopped = true
        ScriptExecutor.deactivate(this)
        parallelScripts.forEach { ScriptExecutor.deactivate(it) }
    }

    fun addParallelScript(script: Script) {
        parallelScripts.add(script)
        ScriptExecutor.activate(script)
    }

    open fun onStart() = println("${this.javaClass.simpleName} started.")
    open fun onStop() = println("${this.javaClass.simpleName} stopped.")

    open fun onEvent(event: Event) {}

    fun _processEvent(event: Event) {
        onEvent(event)
        if (pendingEventPredicate?.test(event) == true)
            pendingEventWaitCompleted = true
    }

    private suspend fun waitForCondition(
        predicate: () -> Boolean,
        timeoutMillis: Long? = null,
        shouldWaitWhile: Boolean = true,
        pollingDelayMillis: Int = 100
    ) {
        val wrappedPredicate = {
            if (shouldWaitWhile) predicate() else !predicate()
        }

        if (timeoutMillis != null) {
            withTimeoutOrNull(timeoutMillis) {
                while (wrappedPredicate()) {
                    delay(pollingDelayMillis)
                    suspendCancellableCoroutine<Unit> { cont ->
                        dispatcher.addPredicateContinuation(cont, wrappedPredicate)
                    }
                }
            }
        } else {
            while (wrappedPredicate()) {
                delay(pollingDelayMillis)
                suspendCancellableCoroutine<Unit> { cont ->
                    dispatcher.addPredicateContinuation(cont, wrappedPredicate)
                }
            }
        }
    }

    suspend fun delayWhile(timeoutMillis: Long? = null, predicate: () -> Boolean) = waitForCondition(predicate, timeoutMillis, shouldWaitWhile = true)

    suspend fun waitThenDelayWhile(waitFor: Long, timeoutMillis: Long? = null, predicate: () -> Boolean) {
        delay(waitFor.toInt(), 1000)
        waitForCondition(predicate, timeoutMillis, shouldWaitWhile = true)
    }

    suspend fun delayUntil(timeoutMillis: Long? = null, pollingDelayMillis: Int = 100, predicate: () -> Boolean) = waitForCondition(predicate, timeoutMillis, pollingDelayMillis = pollingDelayMillis, shouldWaitWhile = false)

    suspend fun waitThenDelayUntil(waitFor: Long, timeoutMillis: Long? = null, pollingDelayMillis: Int = 100, predicate: () -> Boolean) {
        delay(waitFor.toInt(), 1000)
        waitForCondition(predicate, timeoutMillis, shouldWaitWhile = false, pollingDelayMillis = pollingDelayMillis)
    }

    suspend fun waitForEvent(timeoutMillis: Long = 15000, predicate: Predicate<Event>) {
        pendingEventPredicate = predicate
        pendingEventWaitCompleted = false
        delayUntil(timeoutMillis) { pendingEventWaitCompleted }
        pendingEventPredicate = null
    }

    suspend fun waitForXPDrop(skill: Skill, timeoutMillis: Long = 15000) {
        waitForEvent(timeoutMillis) { event -> event is XPDrop && event.skill == skill }
    }

    suspend fun waitForChatContaining(type: ChatMessageType, text: String, timeoutMillis: Long = 15000) {
        waitForEvent(timeoutMillis) { event ->
            event is Chat && event.messageType == type && event.message.contains(text, ignoreCase = true)
        }
    }

    suspend fun delay(time: Int) {
        kotlinx.coroutines.delay(time.toLong())
    }

    suspend fun delay(mean: Int, variance: Int) {
        delay(gaussian(mean, variance))
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScriptDescription(
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    /**
     * TODO
     * possible requirements field that scripters can attach level requirements,
     * varbit requirements, etc to and the script will not populate into the script
     * dropdown without them being met on the account
     */
)