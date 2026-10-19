package com.rs.net.encoders

import org.darkan.core.net.prot.packet.BuildInstancedRegion

object MapRegionKeys {

    fun forRegions(regionIds: IntArray, keysFor: (Int) -> IntArray?): Array<IntArray> =
        Array(regionIds.size) { keysFor(regionIds[it]) ?: IntArray(4) }

    fun forChunks(chunks: IntArray, keysFor: (Int) -> IntArray?): Array<IntArray> =
        forRegions(BuildInstancedRegion.regionIds(chunks), keysFor)
}
