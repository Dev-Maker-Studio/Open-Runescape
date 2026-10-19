package org.darkan.world

import com.rs.net.encoders.MapRegionKeys
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.darkan.core.model.RegionSize
import org.darkan.core.net.prot.packet.BuildInstancedRegion
import org.darkan.core.net.prot.packet.BuildRegion
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapRegionKeysTest {

    private companion object {
        const val BASE_CHUNK_X = 600
        const val BASE_CHUNK_Y = 600
        const val FIRST_SEEN_REGION = (52 shl 8) + 49
        const val SECOND_SEEN_REGION = (49 shl 8) + 50
        const val THIRD_SEEN_REGION = (50 shl 8) + 53
        const val PLANES = 4
    }

    private val layout = mapOf(
        Triple(BASE_CHUNK_X, BASE_CHUNK_Y, 0) to BuildInstancedRegion.chunkDescriptor(416, 392, 0, 0),
        Triple(BASE_CHUNK_X, BASE_CHUNK_Y + 1, 0) to BuildInstancedRegion.chunkDescriptor(392, 400, 1, 1),
        Triple(BASE_CHUNK_X + 1, BASE_CHUNK_Y - 2, 0) to BuildInstancedRegion.chunkDescriptor(417, 393, 0, 2),
        Triple(BASE_CHUNK_X - 3, BASE_CHUNK_Y, 1) to BuildInstancedRegion.chunkDescriptor(400, 424, 2, 3),
        Triple(BASE_CHUNK_X, BASE_CHUNK_Y, 3) to BuildInstancedRegion.chunkDescriptor(393, 401, 3, 0),
    )

    private fun descriptorAt(chunkX: Int, chunkY: Int, plane: Int) =
        layout[Triple(chunkX, chunkY, plane)] ?: BuildInstancedRegion.ABSENT_CHUNK

    private fun keyOf(regionId: Int) = intArrayOf(regionId, regionId * 31 + 7, regionId.inv(), regionId xor 0x5a5a5a5a)

    @Test
    fun `instanced keys follow the client's first-seen region order`() {
        val chunks = BuildInstancedRegion.chunks(BASE_CHUNK_X, BASE_CHUNK_Y, RegionSize.SIZE_104, ::descriptorAt)
        val regionIds = BuildInstancedRegion.regionIds(chunks)
        assertContentEquals(intArrayOf(FIRST_SEEN_REGION, SECOND_SEEN_REGION, THIRD_SEEN_REGION), regionIds)

        val keys = MapRegionKeys.forChunks(chunks, ::keyOf)
        assertEquals(regionIds.size, keys.size)
        regionIds.forEachIndexed { index, regionId -> assertContentEquals(keyOf(regionId), keys[index], "region $regionId") }
    }

    @Test
    fun `first-seen order differs from the sorted and hash orders the old sender could produce`() {
        val firstSeen = intArrayOf(FIRST_SEEN_REGION, SECOND_SEEN_REGION, THIRD_SEEN_REGION)
        val hashOrder = IntOpenHashSet().apply {
            BuildInstancedRegion.chunks(BASE_CHUNK_X, BASE_CHUNK_Y, RegionSize.SIZE_104, ::descriptorAt)
                .filter { it != BuildInstancedRegion.ABSENT_CHUNK }
                .forEach { add(((it shr 14 and 0x3ff) / 8 shl 8) + (it shr 3 and 0x7ff) / 8) }
        }.toIntArray()
        assertFalse(firstSeen.contentEquals(firstSeen.sortedArray()))
        assertFalse(firstSeen.contentEquals(hashOrder))
        assertFalse(firstSeen.sortedArray().contentEquals(hashOrder))
    }

    @Test
    fun `the instanced walk visits plane then x then y over every chunk of every map size`() {
        for (mapSize in RegionSize.entries) {
            val radius = mapSize.size shr 4
            val visited = mutableListOf<Triple<Int, Int, Int>>()
            val chunks = BuildInstancedRegion.chunks(BASE_CHUNK_X, BASE_CHUNK_Y, mapSize) { x, y, plane ->
                visited.add(Triple(plane, x, y))
                BuildInstancedRegion.ABSENT_CHUNK
            }
            val span = mapSize.size shr 3
            assertEquals(PLANES * span * span, chunks.size, "$mapSize chunk count")
            assertEquals(visited.sortedWith(compareBy({ it.first }, { it.second }, { it.third })), visited, "$mapSize walk order")
            assertEquals(visited.size, visited.toSet().size, "$mapSize visits each chunk once")
            assertEquals(BASE_CHUNK_X - radius, visited.minOf { it.second })
            assertEquals(BASE_CHUNK_X + radius, visited.maxOf { it.second })
            assertEquals(BASE_CHUNK_Y - radius, visited.minOf { it.third })
            assertEquals(BASE_CHUNK_Y + radius, visited.maxOf { it.third })
        }
    }

    @Test
    fun `a region without keys is sent zero keys in its own slot`() {
        val keys = MapRegionKeys.forRegions(intArrayOf(FIRST_SEEN_REGION, SECOND_SEEN_REGION)) { if (it == FIRST_SEEN_REGION) null else keyOf(it) }
        assertContentEquals(IntArray(4), keys[0])
        assertContentEquals(keyOf(SECOND_SEEN_REGION), keys[1])
    }

    @Test
    fun `standard keys pair with the client's x-outer y-inner region grid`() {
        for (mapSize in RegionSize.entries) {
            val regionIds = BuildRegion.regionIds(401, 403, mapSize)
            val keys = MapRegionKeys.forRegions(regionIds, ::keyOf)
            assertTrue(regionIds.contentEquals(regionIds.sortedArray()), "$mapSize grid is ascending away from the origin")
            regionIds.forEachIndexed { index, regionId -> assertContentEquals(keyOf(regionId), keys[index]) }
        }
    }
}
