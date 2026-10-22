package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class CoverageDiagnosticsTest {
    @Test fun eligiblePixelsAreReportedEvenWhenHistoryIsEmpty() {
        val coverage = FrameCoverage(20, 10)
        val profile = CaptureProfile()
        val diagnostics = CoverageDiagnostics(coverage, profile)
        coverage.cover(intArrayOf(0, 0, 20, 10))
        diagnostics.history()
        assertEquals(0L, profile.snapshot()["historyEligiblePixels"])
        assertTrue(coverage.inherited().isEmpty())
        assertTrue(coverage.fullyCovered())
    }

    @Test fun eligiblePixelsAreReportedWithoutChangingSparseRegions() {
        val coverage = FrameCoverage(20, 10)
        val profile = CaptureProfile()
        coverage.cover(intArrayOf(1, 0, 18, 10))
        CoverageDiagnostics(coverage, profile).history()
        assertEquals(20L, profile.snapshot()["historyEligiblePixels"])
        val regions = coverage.inherited()
        assertEquals(2, regions.size)
        assertArrayEquals(intArrayOf(0, 0, 1, 10), regions[0])
        assertArrayEquals(intArrayOf(19, 0, 1, 10), regions[1])
        assertFalse(coverage.fullyCovered())
    }

    @Test fun observingOverBudgetHistoryDoesNotAuthorizeIt() {
        val coverage = FrameCoverage(20, 10)
        val profile = CaptureProfile()
        CoverageDiagnostics(coverage, profile).history()
        assertEquals(200L, profile.snapshot()["historyEligiblePixels"])
        assertThrows(IllegalArgumentException::class.java) { coverage.inherited() }
        assertEquals(200, coverage.eligiblePixels())
    }

    @Test fun firstFullDetailsAreLazyAndNeverReplacedByLaterOperations() {
        val coverage = FrameCoverage(20, 10)
        val profile = CaptureProfile()
        val diagnostics = CoverageDiagnostics(coverage, profile)
        var details = 0
        diagnostics.firstFull(CoverageOperation.DRAW_BOUNDS) { details++ }
        coverage.cover(intArrayOf(0, 0, 19, 10))
        diagnostics.firstFull(CoverageOperation.FALLBACK_2D) { details++ }
        assertEquals(0, details)
        coverage.cover(intArrayOf(19, 0, 1, 10))
        diagnostics.firstFull(CoverageOperation.CLEAR) { details++ }
        diagnostics.firstFull(CoverageOperation.DRAW_ERROR) { details++ }
        diagnostics.blit(3, 4, 0x4100, 0x2600, intArrayOf(0, 0, 20, 10), intArrayOf(0, 0, 20, 10), null)
        assertEquals(1, details)
        assertEquals(mapOf("coverageFirstFullClear" to 1L), profile.snapshot().filterKeys { it.startsWith("coverage") })
    }

    @Test fun eachOperationCategoryHasOneBoundedStaticKey() {
        for (operation in CoverageOperation.entries) {
            val coverage = FrameCoverage(1, 1)
            val profile = CaptureProfile()
            val diagnostics = CoverageDiagnostics(coverage, profile)
            coverage.cover(intArrayOf(0, 0, 1, 1))
            repeat(10) { diagnostics.firstFull(operation) }
            assertEquals(mapOf(operation.key to 1L), profile.snapshot().filterKeys { it.startsWith("coverage") })
        }
    }

    @Test fun firstFullBlitPreservesNativeIdsMasksRectanglesAndScissorExactly() {
        val coverage = FrameCoverage(20, 10)
        val profile = CaptureProfile()
        val diagnostics = CoverageDiagnostics(coverage, profile)
        coverage.cover(intArrayOf(0, 0, 20, 10))
        diagnostics.blit(17, 31, 0x4100, 0x2600, intArrayOf(-2, 3, 26, 19), intArrayOf(20, 10, 0, 0), intArrayOf(-3, 4, 29, 13))
        val data = profile.snapshot()
        assertEquals(1L, data["coverageFirstFullBlit"])
        assertEquals(17L, data["coverageFirstFullReadFbo"])
        assertEquals(31L, data["coverageFirstFullDrawFbo"])
        assertEquals(0x4100L, data["coverageFirstFullMask"])
        assertEquals(0x2600L, data["coverageFirstFullFilter"])
        assertEquals(-2L, data["coverageFirstFullSourceX0"])
        assertEquals(3L, data["coverageFirstFullSourceY0"])
        assertEquals(26L, data["coverageFirstFullSourceX1"])
        assertEquals(19L, data["coverageFirstFullSourceY1"])
        assertEquals(20L, data["coverageFirstFullTargetX0"])
        assertEquals(10L, data["coverageFirstFullTargetY0"])
        assertEquals(0L, data["coverageFirstFullTargetX1"])
        assertEquals(0L, data["coverageFirstFullTargetY1"])
        assertEquals(1L, data["coverageFirstFullScissorEnabled"])
        assertEquals(-3L, data["coverageFirstFullScissorX"])
        assertEquals(4L, data["coverageFirstFullScissorY"])
        assertEquals(29L, data["coverageFirstFullScissorWidth"])
        assertEquals(13L, data["coverageFirstFullScissorHeight"])
    }

    @Test fun disabledScissorIsDistinctFromAnEnabledEmptyScissor() {
        for (scissor in listOf(null, intArrayOf(0, 0, 0, 0))) {
            val coverage = FrameCoverage(1, 1)
            val profile = CaptureProfile()
            coverage.cover(intArrayOf(0, 0, 1, 1))
            CoverageDiagnostics(coverage, profile).blit(0, 7, 0x4000, 0x2600, intArrayOf(0, 0, 1, 1), intArrayOf(0, 0, 1, 1), scissor)
            assertEquals(if (scissor == null) 0L else 1L, profile.snapshot()["coverageFirstFullScissorEnabled"])
            assertEquals(scissor != null, profile.snapshot().containsKey("coverageFirstFullScissorWidth"))
        }
    }

    @Test fun blitDiagnosticsNeverModifyCoverage() {
        val coverage = FrameCoverage(20, 10)
        val profile = CaptureProfile()
        CoverageDiagnostics(coverage, profile).blit(0, 7, 0x4000, 0x2600, intArrayOf(0, 0, 20, 10), intArrayOf(0, 0, 20, 10), null)
        assertEquals(200, coverage.eligiblePixels())
        assertFalse(profile.snapshot().containsKey("coverageFirstFullBlit"))
    }

    @Test fun tightAndFallbackCountersUseOnlyAuditedPassAndEnumKeys() {
        val profile = CaptureProfile()
        val diagnostics = CoverageDiagnostics(FrameCoverage(1, 1), profile)
        diagnostics.tight(Passes.sprite)
        diagnostics.tight(Passes.surfaceMask)
        diagnostics.tight(Passes.model)
        for (reason in CoverageFallback.entries) {
            repeat(3) { diagnostics.fallback(Passes.sprite, reason) }
            diagnostics.fallback(Passes.surfaceMask, reason)
            diagnostics.fallback(Passes.model, reason)
            diagnostics.fallback(null, reason)
        }
        val data = profile.snapshot().filterKeys { it.startsWith("coverage") }
        assertEquals(2 + 2 * CoverageFallback.entries.size, data.size)
        assertEquals(1L, data["coverageSpriteTight"])
        assertEquals(1L, data["coverageMaskTight"])
        for (reason in CoverageFallback.entries) {
            assertEquals(3L, data["coverageSpriteFallback" + reason.suffix])
            assertEquals(1L, data["coverageMaskFallback" + reason.suffix])
        }
    }

    @Test fun affineFailureReasonsPreserveConservativeFallback() {
        val projection = ColorClear.projection(100, 80)
        val indices = intArrayOf(0, 1, 2, 0, 2, 3)
        val viewport = intArrayOf(0, 0, 100, 80)
        fun rejected(reason: CoverageFallback, vertices: ByteArray = sprite(), index: IntArray = indices, matrix: FloatArray = projection) {
            val coverage = FrameCoverage(100, 80)
            assertFalse(coverage.affine2d(Passes.sprite, vertices, index, matrix, viewport, null))
            assertEquals(reason, coverage.affineFailure)
            coverage.cover(viewport)
            assertTrue(coverage.fullyCovered())
            assertTrue(coverage.inherited().isEmpty())
        }
        rejected(CoverageFallback.PROJECTION, matrix = FloatArray(0))
        rejected(CoverageFallback.PROJECTION, matrix = projection.copyOf().also { it[0] = Float.NaN })
        rejected(CoverageFallback.PROJECTION, matrix = projection.copyOf().also { it[3] = 1f })
        rejected(CoverageFallback.LAYOUT, index = intArrayOf(0, 1))
        rejected(CoverageFallback.LAYOUT, vertices = ByteArray(39))
        rejected(CoverageFallback.INDEX, index = intArrayOf(0, 1, 4))
        rejected(CoverageFallback.CLIP_FINITE, vertices = sprite().also { bytes(it).putFloat(24, Float.POSITIVE_INFINITY) })
        rejected(CoverageFallback.CLIP_VARYING, vertices = sprite().also { bytes(it).putFloat(40 + 24, 1f) })
        rejected(CoverageFallback.TRANSFORM, vertices = sprite().also { bytes(it).putFloat(0, Float.NaN) })
        rejected(CoverageFallback.CLIP_ORDER, vertices = sprite().also { data -> repeat(4) { bytes(data).putFloat(it * 40 + 32, 0f) } })
    }

    @Test fun successfulAffineAttemptClearsThePreviousFailureReason() {
        val coverage = FrameCoverage(100, 80)
        val viewport = intArrayOf(0, 0, 100, 80)
        val indices = intArrayOf(0, 1, 2, 0, 2, 3)
        assertFalse(coverage.affine2d(Passes.sprite, sprite(), indices, FloatArray(0), viewport, null))
        assertNotNull(coverage.affineFailure)
        assertTrue(coverage.affine2d(Passes.sprite, sprite(), indices, ColorClear.projection(100, 80), viewport, null))
        assertNull(coverage.affineFailure)
        assertTrue(coverage.eligiblePixels() > 0)
    }

    @Test fun monotonicFullQueriesNeverSkipAnUntouchedGapOrChangeHistory() {
        val coverage = FrameCoverage(100, 80)
        coverage.cover(intArrayOf(0, 0, 100, 79))
        repeat(100) { assertFalse(coverage.fullyCovered()) }
        assertEquals(100, coverage.eligiblePixels())
        assertArrayEquals(intArrayOf(0, 79, 100, 1), coverage.inherited().single())
        coverage.cover(intArrayOf(99, 79, 1, 1))
        repeat(100) { assertFalse(coverage.fullyCovered()) }
        coverage.cover(intArrayOf(0, 79, 98, 1))
        repeat(100) { assertFalse(coverage.fullyCovered()) }
        assertEquals(1, coverage.eligiblePixels())
        assertArrayEquals(intArrayOf(98, 79, 1, 1), coverage.inherited().single())
        coverage.cover(intArrayOf(98, 79, 1, 1))
        repeat(100) { assertTrue(coverage.fullyCovered()) }
        assertEquals(0, coverage.eligiblePixels())
        assertTrue(coverage.inherited().isEmpty())
    }

    @Test fun repeatedInstanceGateCountsOnceButDistinctFallbackReasonsRemainVisible() {
        for (pass in listOf(Passes.sprite, Passes.surfaceMask)) {
            val profile = CaptureProfile()
            val diagnostics = CoverageDiagnostics(FrameCoverage(1, 1), profile)
            diagnostics.fallback(pass, CoverageFallback.INSTANCES)
            diagnostics.fallback(pass, CoverageFallback.INSTANCES, CoverageFallback.INSTANCES)
            diagnostics.fallback(pass, CoverageFallback.BUILD, CoverageFallback.INSTANCES)
            val prefix = if (pass === Passes.sprite) "coverageSprite" else "coverageMask"
            assertEquals(1L, profile.snapshot()[prefix + "FallbackInstances"])
            assertEquals(1L, profile.snapshot()[prefix + "FallbackBuild"])
        }
    }

    @Test fun unknownNativeDrawArgumentsAreOmittedInsteadOfLookingLikeGlPoints() {
        for (known in listOf(false, true)) {
            val profile = CaptureProfile()
            CoverageDiagnostics(FrameCoverage(1, 1), profile).drawArguments(0, 0x1405, 1, known)
            val data = profile.snapshot()
            assertEquals(if (known) 1L else 0L, data["coverageFirstFullArgumentsKnown"])
            assertEquals(known, data.containsKey("coverageFirstFullMode"))
            assertEquals(known, data.containsKey("coverageFirstFullIndexType"))
            assertEquals(known, data.containsKey("coverageFirstFullInstances"))
            if (known) {
                assertEquals(0L, data["coverageFirstFullMode"])
                assertEquals(0x1405L, data["coverageFirstFullIndexType"])
                assertEquals(1L, data["coverageFirstFullInstances"])
            }
        }
    }

    @Test fun viewportAndScissorTelemetryUseTheSameNativeBottomLeftCoordinates() {
        val profile = CaptureProfile()
        val diagnostics = CoverageDiagnostics(FrameCoverage(100, 80), profile)
        diagnostics.viewport(intArrayOf(7, 10, 40, 20))
        diagnostics.clip(intArrayOf(7, 50, 40, 20))
        val data = profile.snapshot()
        assertEquals(7L, data["coverageFirstFullViewportX"])
        assertEquals(50L, data["coverageFirstFullViewportY"])
        assertEquals(data["coverageFirstFullScissorX"], data["coverageFirstFullViewportX"])
        assertEquals(data["coverageFirstFullScissorY"], data["coverageFirstFullViewportY"])
        assertEquals(data["coverageFirstFullScissorWidth"], data["coverageFirstFullViewportWidth"])
        assertEquals(data["coverageFirstFullScissorHeight"], data["coverageFirstFullViewportHeight"])
    }

    private fun bytes(data: ByteArray) = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    private fun sprite(): ByteArray = ColorClear.vertices(intArrayOf(10, 10, 20, 20), floatArrayOf(1f, 1f, 1f, 0f)).also { data ->
        val buffer = bytes(data)
        repeat(4) {
            buffer.putFloat(it * 40 + 24, 0f)
            buffer.putFloat(it * 40 + 28, 0f)
            buffer.putFloat(it * 40 + 32, 100f)
            buffer.putFloat(it * 40 + 36, 80f)
        }
    }
}
