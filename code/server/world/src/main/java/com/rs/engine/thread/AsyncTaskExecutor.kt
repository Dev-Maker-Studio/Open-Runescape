package com.rs.engine.thread

import com.rs.Settings
import org.darkan.core.Logger
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object AsyncTaskExecutor {
    private const val VIRTUAL_POOL_SIZE = 16

    private lateinit var worldThreadExecutor: ScheduledExecutorService
    private lateinit var asyncExecutor: ScheduledExecutorService

    @JvmStatic
    fun initExecutors() {
        Logger.info(AsyncTaskExecutor::class.java, "initExecutors", "Initializing world threads...")
        worldThreadExecutor = Executors.newSingleThreadScheduledExecutor(WorldThreadFactory())
        asyncExecutor = Executors.newScheduledThreadPool(VIRTUAL_POOL_SIZE, Thread.ofVirtual().factory())
    }

    @JvmStatic
    fun execute(command: Runnable) {
        asyncExecutor.submit(CatchExceptionRunnable(command))
    }

    @JvmStatic
    fun schedule(command: Runnable, startDelay: Int, delay: Int) {
        asyncExecutor.scheduleWithFixedDelay(CatchExceptionRunnable(command), startDelay.toLong() * Settings.WORLD_CYCLE_MS, delay.toLong() * Settings.WORLD_CYCLE_MS, TimeUnit.MILLISECONDS)
    }

    @JvmStatic
    fun shutdown() = asyncExecutor.shutdown()

    @JvmStatic
    fun getWorldThreadExecutor(): ScheduledExecutorService = worldThreadExecutor
}
