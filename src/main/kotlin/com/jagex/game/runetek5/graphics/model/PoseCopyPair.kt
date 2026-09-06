package com.jagex.game.runetek5.graphics.model

/**
 * A private pair of copy slots for one memoised pose owner.
 *
 * `Model.copy(type 1..7)` hands out the toolkit's SHARED per-type slot pair, so a
 * copy kept across frames gets overwritten by the next entity; `copy(0)` allocates
 * a fresh ModelGL and every array on each pose change, which at 20 poses a frame is
 * hundreds of MB/s of garbage and the 20-50 ms collection pauses seen while rotating
 * the camera. This is the slot mechanism again, owned by one entity: two ModelGL
 * objects alternate as destination and array donor exactly like copySrc/copyDst.
 */
class PoseCopyPair {
    private var a: ModelGL? = null
    private var b: ModelGL? = null
    private var flip = false
    private var failures = 0
    private var ownNxA: ShortArray? = null; private var ownNyA: ShortArray? = null; private var ownNzA: ShortArray? = null; private var ownMagA: ByteArray? = null
    /** Private vertex arrays per slot (x,y,z) x (A,B). */
    private val ownVerts = arrayOfNulls<IntArray>(6)
    private fun ownInts(axis: Int, slotA: Boolean, n: Int): IntArray {
        val i = axis * 2 + (if (slotA) 0 else 1)
        var a = ownVerts[i]
        if (a == null || a.size < n) { a = IntArray(n); ownVerts[i] = a }
        return a
    }
    private var ownNxB: ShortArray? = null; private var ownNyB: ShortArray? = null; private var ownNzB: ShortArray? = null; private var ownMagB: ByteArray? = null

    private var baseA: ModelGL? = null
    private var baseB: ModelGL? = null

    /**
     * copyModelData treats the source slot's face and billboard arrays as scratch it
     * may hand to the destination and overwrite. A slot that last copied from [base]
     * with a mask that did not alter one of those arrays still references the base's
     * own array, and the two slots swap roles on every copy, so without this the next
     * pose would animate the base model in place (a fade-out teleport left every later
     * pose invisible; a torch's billboard scale compounded every loop).
     */
    private fun releaseBaseAliases(slot: ModelGL, base: ModelGL) {
        if (slot.facesColor === base.facesColor) slot.facesColor = null
        if (slot.facesAlpha === base.facesAlpha) slot.facesAlpha = null
        if (slot.facesA === base.facesA) slot.facesA = null
        if (slot.facesB === base.facesB) slot.facesB = null
        if (slot.facesC === base.facesC) slot.facesC = null
        if (slot.facesMaterial === base.facesMaterial) slot.facesMaterial = null
        if (slot.billboardSurfaces === base.billboardSurfaces) slot.billboardSurfaces = null
    }

    /** Copies [base] into the next slot with [functionMask]; the returned object is owned by this pair. */
    fun copy(base: ModelGL, functionMask: Int): ModelGL {
        var slotA = a
        var slotB = b
        if (slotA == null || slotB == null || slotA.toolkit !== base.toolkit) {
            slotA = ModelGL(base.toolkit)
            slotB = ModelGL(base.toolkit)
            a = slotA
            b = slotB
        }
        flip = !flip
        val dst = if (flip) slotA else slotB
        val src = if (flip) slotB else slotA
        dst.ensureSlotBuffers()
        src.ensureSlotBuffers()
        (if (flip) baseB else baseA)?.let { releaseBaseAliases(src, it) }
        try {
            base.copyModelData(dst, src, functionMask, true)
        } catch (t: Throwable) {
            if (failures++ < 3) { System.err.println("[PoseCopyPair] copy failed: $t"); t.printStackTrace() }
            throw t
        }
        if (flip) baseA = base else baseB = base
        dst.privatePoseCopy = true
        // Vertex axes the mask does not declare as altered are SHARED with the base
        // (dst.verticesY = base.verticesY). The base is the toolkit's per-type slot,
        // overwritten by the next entity's pose, so a kept copy would read another
        // model's vertices later — the emitter positions of updateParticlePosition
        // (which reads verticesX/Y/Z every frame) then land under the character on
        // every memo-hit frame. Give the slot its own arrays.
        val nv = base.numVertices
        if (nv > 0) {
            if (dst.verticesX != null && dst.verticesX === base.verticesX) dst.verticesX = ownInts(0, flip, nv).also { System.arraycopy(base.verticesX!!, 0, it, 0, nv) }
            if (dst.verticesY != null && dst.verticesY === base.verticesY) dst.verticesY = ownInts(1, flip, nv).also { System.arraycopy(base.verticesY!!, 0, it, 0, nv) }
            if (dst.verticesZ != null && dst.verticesZ === base.verticesZ) dst.verticesZ = ownInts(2, flip, nv).also { System.arraycopy(base.verticesZ!!, 0, it, 0, nv) }
        }
        // A copy whose mask does not alter lighting shares the base's normal arrays;
        // a kept pose must own them (neighbour merging would otherwise drift the base)
        // and detachSharedNormals allocated fresh copies on every pose. Keep the
        // slot's own arrays and refill them instead.
        val baseNx = base.normalsX
        if (baseNx != null && dst.normalsX === baseNx) {
            val n = baseNx.size
            var ox = if (flip) ownNxA else ownNxB
            var oy = if (flip) ownNyA else ownNyB
            var oz = if (flip) ownNzA else ownNzB
            var om = if (flip) ownMagA else ownMagB
            if (ox == null || oy == null || oz == null || om == null || ox.size < n) {
                ox = ShortArray(n); oy = ShortArray(n); oz = ShortArray(n); om = ByteArray(n)
                if (flip) { ownNxA = ox; ownNyA = oy; ownNzA = oz; ownMagA = om } else { ownNxB = ox; ownNyB = oy; ownNzB = oz; ownMagB = om }
            }
            System.arraycopy(baseNx, 0, ox, 0, n)
            base.normalsY?.let { System.arraycopy(it, 0, oy, 0, minOf(n, it.size)) }
            base.normalsZ?.let { System.arraycopy(it, 0, oz, 0, minOf(n, it.size)) }
            base.magnitudes?.let { System.arraycopy(it, 0, om, 0, minOf(n, it.size)) }
            dst.normalsX = ox; dst.normalsY = oy; dst.normalsZ = oz
            if (base.magnitudes != null) dst.magnitudes = om
        }
        return dst
    }
}
