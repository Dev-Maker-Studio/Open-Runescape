package com.jagex.game.runetek5.graphics.modern.dump

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33.*

object Uniforms {
    private fun comps(type: Int) = when (type) {
        0x8B50, 0x8B53 -> 2
        0x8B51, 0x8B54 -> 3
        0x8B52, 0x8B55 -> 4
        0x8B5A -> 4
        0x8B5B -> 9
        0x8B5C -> 16
        else -> 1
    }

    private fun isInt(type: Int) = type == 0x1404 || type == 0x8B56 || type in 0x8B53..0x8B55 || type in 0x8B5E..0x8B67 || type in 0x8DC0..0x8DD0

    fun read(program: Int): Map<String, FloatArray> {
        val out = HashMap<String, FloatArray>()
        val size = BufferUtils.createIntBuffer(1)
        val type = BufferUtils.createIntBuffer(1)
        val floats = BufferUtils.createFloatBuffer(16)
        val ints = BufferUtils.createIntBuffer(16)
        for (i in 0 until glGetProgrami(program, GL_ACTIVE_UNIFORMS)) {
            val raw = glGetActiveUniform(program, i, 128, size, type)
            val name = raw.removeSuffix("[0]")
            val (n, t) = size.get(0) to type.get(0)
            val c = comps(t)
            val values = FloatArray(n * c)
            for (e in 0 until n) {
                val loc = glGetUniformLocation(program, if (n > 1 || raw.endsWith("[0]")) "$name[$e]" else name)
                if (loc < 0) continue
                if (isInt(t)) {
                    glGetUniformiv(program, loc, ints.clear())
                    for (k in 0 until c) values[e * c + k] = ints.get(k).toFloat()
                } else {
                    glGetUniformfv(program, loc, floats.clear())
                    for (k in 0 until c) values[e * c + k] = floats.get(k)
                }
            }
            out[name] = values
        }
        return out
    }

    fun isSampler(type: Int) = type in 0x8B5E..0x8B67
}
