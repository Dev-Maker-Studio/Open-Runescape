package com.jagex.game.runetek5.graphics.modern.dump

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lwjgl.opengl.GL33.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL33.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL33.GL_LINEAR
import org.lwjgl.opengl.GL33.GL_NEAREST

class FrameAliasesTest {
    private val whole = intArrayOf(0, 0, 20, 10)

    @Test
    fun rootInitializationIsRequiredBeforeAnAliasCanResolve() {
        val aliases = FrameAliases(0, 20, 10)
        assertTrue(aliases.register(0, 7, 20, 10))
        assertFalse(aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        assertTrue(aliases.identityCopy(0, 7, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        assertTrue(aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
    }

    @Test
    fun sameSizeDistinctExplicitRegistrationIsRequired() {
        val aliases = FrameAliases(0, 20, 10)
        assertFalse(aliases.register(99, 7, 20, 10))
        assertFalse(aliases.register(0, 0, 20, 10))
        assertFalse(aliases.register(0, 7, 19, 10))
        assertFalse(aliases.register(0, 7, 20, 11))
        assertFalse(aliases.identityCopy(0, 7, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        assertFalse(aliases.identityCopy(0, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
    }

    @Test
    fun reregistrationAndResizeRequireFreshInitialization() {
        val aliases = FrameAliases(0, 20, 10)
        assertTrue(aliases.register(0, 7, 20, 10))
        assertTrue(aliases.identityCopy(0, 7, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        assertTrue(aliases.register(0, 7, 20, 10))
        assertFalse(aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        assertTrue(aliases.identityCopy(0, 7, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        assertFalse(aliases.register(0, 7, 21, 10))
        assertFalse(aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
    }

    @Test
    fun transformedFilteredScissoredAndOtherMasksNeverInitializeAnAlias() {
        fun rejected(source: IntArray = whole, target: IntArray = whole, mask: Int = GL_COLOR_BUFFER_BIT, filter: Int = GL_NEAREST, scissored: Boolean = false) {
            val aliases = FrameAliases(0, 20, 10)
            assertTrue(aliases.register(0, 7, 20, 10))
            assertFalse(aliases.identityCopy(0, 7, source, target, mask, filter, scissored))
            assertFalse(aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        }
        rejected(source = intArrayOf(0, 10, 20, 0))
        rejected(source = intArrayOf(0, 0, 19, 10))
        rejected(target = intArrayOf(1, 0, 20, 10))
        rejected(target = intArrayOf(0, 0, 10, 10))
        rejected(mask = GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        rejected(mask = GL_DEPTH_BUFFER_BIT)
        rejected(filter = GL_LINEAR)
        rejected(scissored = true)
    }

    @Test
    fun identityInitializationAndResolveNeverUnmarkUnknownGeometry() {
        val aliases = FrameAliases(0, 20, 10)
        val coverage = FrameCoverage(20, 10)
        assertTrue(aliases.register(0, 7, 20, 10))
        assertTrue(aliases.identityCopy(0, 7, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        coverage.cover(intArrayOf(1, 0, 18, 10))
        assertTrue(aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        val regions = coverage.inherited()
        assertEquals(20, regions.sumOf { it[2] * it[3] })
        assertTrue(regions.all { it[0] == 0 || it[0] == 19 })
    }

    @Test
    fun uninitializedResolveRemainsAConservativeWholeDestinationWrite() {
        val aliases = FrameAliases(0, 20, 10)
        val coverage = FrameCoverage(20, 10)
        assertTrue(aliases.register(0, 7, 20, 10))
        if (!aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false)) coverage.cover(whole)
        assertTrue(coverage.inherited().isEmpty())
    }
}
