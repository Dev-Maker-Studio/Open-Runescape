package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelGL

/**
 * Order-sensitive 64-bit digest of everything that determines a pathing entity's
 * posed body model. Two draws with the same digest would produce byte-identical
 * models, so the previous one (and the renderer geometry attached to it) can be
 * handed back instead of copying and re-animating the base model.
 */
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

    /** The parts of a sequence that [Sequence.animateRotatedModel] reads: type, frames, tween progress. */
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

/**
 * Per-entity reuse of the posed body model across render frames.
 *
 * With the render loop decoupled from the 50 Hz game logic, several frames in a
 * row see identical animation state for every player and NPC. Legacy re-copied and
 * re-animated the body model on each of them; the renderer then rebuilt and
 * re-uploaded the geometry. Reusing the previous model when nothing that feeds it
 * changed skips all of that. Disabled with -Drender.posememo=false (the same switch
 * as the animated-loc memo).
 */
/**
 * Memo for the entity drop-shadow disc, which is re-copied, scaled to the body
 * pose's bounds and re-animated on every draw. Keyed by the body pose key plus the
 * inputs of [PathingEntity.getAnimatedShadowModel].
 */
internal class ShadowMemo {
    private var model: Model? = null
    private var key = 0L

    /** Returns the memoised disc for [poseKey] (0 = not memoisable) or builds and remembers it. */
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

    // Diagnostics (read + reset by the modern toolkit's perf print).
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

    /**
     * Carries the renderer's per-instance stream key and geometry buffer from the
     * model being replaced to its replacement so the animated store refreshes one
     * entry in place instead of appending a new one every pose change.
     */
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
