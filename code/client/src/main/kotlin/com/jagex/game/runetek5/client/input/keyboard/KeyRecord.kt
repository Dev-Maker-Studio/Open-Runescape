package com.jagex.game.runetek5.client.input.keyboard

interface KeyRecord {
    fun getCharacter(): Char
    fun getType(): Int
    fun getCode(): Int
    fun getTimeSubmitted(): Long
    fun getFlags(): Int
}
