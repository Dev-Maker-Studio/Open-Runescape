package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.Node

class LocOnWorldMap(
    @JvmField val locIds: IntArray,
    @JvmField val shapeAndRotation: ByteArray
) : Node<LocOnWorldMap>()
