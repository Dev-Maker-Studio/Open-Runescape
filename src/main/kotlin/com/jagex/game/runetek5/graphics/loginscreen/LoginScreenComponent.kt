package com.jagex.game.runetek5.graphics.loginscreen

interface LoginScreenComponent {
    fun initialize()

    fun draw()

    fun cleanup()

    fun finished(initTime: Long): Boolean

    fun timeBetweenUpdatesMs(): Int

    fun calculateLoadingPercent(): Int
}
