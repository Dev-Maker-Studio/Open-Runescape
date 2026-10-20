package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.toolkit.ModernGLToolkit

internal class PoseKey {
    private var h = 0L

    fun reset(): PoseKey {
        h = 0x243F6A8885A308D3L
        return this
    }

    fun add(v: Int): PoseKey {
        h = (h xor (v.toLong() and 0xffffffffL)) * -7046029254386353131L
        h = (h shl 31) or (h ushr 33)
        return this
    }

    fun add(v: Long): PoseKey = add(v.toInt()).add((v ushr 32).toInt())

    fun add(v: Boolean): PoseKey = add(if (v) 1 else 0)

    fun add(s: Sequence?): PoseKey {
        if (s == null) return add(-1)
        return add(s.type?.id ?: -2).add(s.currentFrameId).add(s.nextFrameId).add(s.currentFrameDuration).add(s.speed)
    }

    fun add(a: IntArray?): PoseKey {
        if (a == null) return add(-1)
        add(a.size)
        for (v in a) add(v)
        return this
    }

    fun add(a: Array<out Sequence?>?): PoseKey {
        if (a == null) return add(-1)
        add(a.size)
        for (s in a) add(s)
        return this
    }

    fun value(): Long = if (h == 0L) 1L else h
}

internal class ShadowMemo {
    private var model: Model? = null
    private var key = 0L

    inline fun get(poseKey: Long, extra: PoseKey, build: () -> Model?): Model? {
        if (poseKey == 0L) {
            model = null; key = 0L
            PoseMemo.shadowIneligible++
            return build()
        }
        val k = extra.add(poseKey).value()
        val m = model
        if (m != null && k == key) { PoseMemo.shadowHits++; return m }
        PoseMemo.shadowMisses++
        val fresh = build()
        if (fresh != null) PoseMemo.carryGeometry(model, fresh)
        model = fresh
        key = if (fresh != null) k else 0L
        return fresh
    }
}

internal object PoseMemo {
    @JvmStatic
    val ENABLED: Boolean = System.getProperty("render.posememo")?.equals("false", true) != true

    @JvmStatic
    fun enabledFor(toolkit: AbstractToolkit): Boolean = ENABLED && toolkit is ModernGLToolkit

    @JvmField var poseHits = 0L
    @JvmField var poseMisses = 0L
    @JvmField var poseIneligible = 0L
    @JvmField var inelEntityTint = 0L
    @JvmField var inelTypeTint = 0L
    @JvmField var shadowHits = 0L
    @JvmField var shadowMisses = 0L
    @JvmField var shadowIneligible = 0L

    @JvmStatic
    fun statsAndReset(): String {
        val s = "poseMemo[hit=$poseHits miss=$poseMisses inel=$poseIneligible(entityTint=$inelEntityTint typeTint=$inelTypeTint)] shadowMemo[hit=$shadowHits miss=$shadowMisses inel=$shadowIneligible]"
        poseHits = 0; poseMisses = 0; poseIneligible = 0; shadowHits = 0; shadowMisses = 0; shadowIneligible = 0; inelEntityTint = 0; inelTypeTint = 0
        return s
    }

    @JvmStatic
    fun carryGeometry(previous: Model?, fresh: Model?) {
        val prev = previous as? ModelGL ?: return
        val next = fresh as? ModelGL ?: return
        if (prev === next) return
        if (prev.modernStreamKey != 0L) next.modernStreamKey = prev.modernStreamKey
        if (next.modernStreamGeometry == null && prev.modernStreamGeometry != null) {
            next.modernStreamGeometry = prev.modernStreamGeometry
            next.modernStreamValid = false
            prev.modernStreamGeometry = null
        }
    }
}
