package com.jagex.game.runetek5.graphics.modern.dump

internal enum class CoverageFallback(val suffix: String) {
    RASTER("Raster"), INDEX_TYPE("IndexType"), INSTANCES("Instances"),
    POSITION_DIVISOR("PositionDivisor"), POSITION_TYPE("PositionType"), POSITION_SIZE("PositionSize"),
    CLIP_ATTRIBUTE("ClipAttribute"), PROJECTION("Projection"), LAYOUT("Layout"), INDEX("Index"),
    CLIP_FINITE("ClipFinite"), CLIP_VARYING("ClipVarying"), TRANSFORM("Transform"), CLIP_ORDER("ClipOrder"), BUILD("Build")
}

internal enum class CoverageOperation(val key: String) {
    CLEAR("coverageFirstFullClear"), NATIVE_DRAW("coverageFirstFullNativeDraw"),
    DRAW_BOUNDS("coverageFirstFullDrawBounds"), AFFINE_2D("coverageFirstFullAffine2d"),
    FALLBACK_2D("coverageFirstFullFallback2d"), DRAW_ERROR("coverageFirstFullDrawError"), BLIT("coverageFirstFullBlit")
}

internal class CoverageDiagnostics(private val coverage: FrameCoverage, private val profile: CaptureProfile) {
    private var full = false

    fun tight(pass: Pass) {
        prefix(pass)?.let { profile.add(it + "Tight", 1) }
    }

    fun fallback(pass: Pass?, reason: CoverageFallback, previous: CoverageFallback? = null) {
        if (reason != previous) prefix(pass)?.let { profile.add(it + "Fallback" + reason.suffix, 1) }
    }

    fun drawArguments(mode: Int, type: Int, instances: Int, known: Boolean) {
        profile.add("coverageFirstFullArgumentsKnown", if (known) 1 else 0)
        if (known) {
            profile.add("coverageFirstFullMode", mode.toLong())
            profile.add("coverageFirstFullIndexType", type.toLong())
            profile.add("coverageFirstFullInstances", instances.toLong())
        }
    }

    fun viewport(viewport: IntArray) {
        profile.add("coverageFirstFullViewportX", viewport[0].toLong())
        profile.add("coverageFirstFullViewportY", coverage.height.toLong() - viewport[1] - viewport[3])
        profile.add("coverageFirstFullViewportWidth", viewport[2].toLong())
        profile.add("coverageFirstFullViewportHeight", viewport[3].toLong())
    }

    fun firstFull(operation: CoverageOperation, details: () -> Unit = {}) {
        if (full || !coverage.fullyCovered()) return
        full = true
        profile.add(operation.key, 1)
        details()
    }

    fun history() {
        profile.add("historyEligiblePixels", coverage.eligiblePixels().toLong())
    }

    fun blit(source: Int, target: Int, mask: Int, filter: Int, sourceBounds: IntArray, targetBounds: IntArray, scissor: IntArray?) {
        firstFull(CoverageOperation.BLIT) {
            profile.add("coverageFirstFullReadFbo", source.toLong())
            profile.add("coverageFirstFullDrawFbo", target.toLong())
            profile.add("coverageFirstFullMask", mask.toLong())
            profile.add("coverageFirstFullFilter", filter.toLong())
            profile.add("coverageFirstFullSourceX0", sourceBounds[0].toLong())
            profile.add("coverageFirstFullSourceY0", sourceBounds[1].toLong())
            profile.add("coverageFirstFullSourceX1", sourceBounds[2].toLong())
            profile.add("coverageFirstFullSourceY1", sourceBounds[3].toLong())
            profile.add("coverageFirstFullTargetX0", targetBounds[0].toLong())
            profile.add("coverageFirstFullTargetY0", targetBounds[1].toLong())
            profile.add("coverageFirstFullTargetX1", targetBounds[2].toLong())
            profile.add("coverageFirstFullTargetY1", targetBounds[3].toLong())
            clip(scissor)
        }
    }

    fun clip(scissor: IntArray?) {
        profile.add("coverageFirstFullScissorEnabled", if (scissor == null) 0 else 1)
        if (scissor != null) {
            profile.add("coverageFirstFullScissorX", scissor[0].toLong())
            profile.add("coverageFirstFullScissorY", scissor[1].toLong())
            profile.add("coverageFirstFullScissorWidth", scissor[2].toLong())
            profile.add("coverageFirstFullScissorHeight", scissor[3].toLong())
        }
    }

    private fun prefix(pass: Pass?): String? = when (pass) {
        Passes.sprite -> "coverageSprite"
        Passes.surfaceMask -> "coverageMask"
        else -> null
    }
}
