package com.jagex.game.runetek5.scene.tile

import com.jagex.game.runetek5.entity.InteractiveEntityContainer
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration
import com.jagex.game.runetek5.entity.gameobject.AbstractWall
import com.jagex.game.runetek5.entity.gameobject.AbstractWallDecoration
import com.jagex.game.runetek5.entity.grounditem.AbstractGroundItem

class Tile(plane: Int) {
    @JvmField var groundDecoration: AbstractGroundDecoration? = null
    @JvmField var secondaryWallDecoration: AbstractWallDecoration? = null

    @JvmField var secondaryWall: AbstractWall? = null

    @JvmField var primaryWallDecoration: AbstractWallDecoration? = null
    @JvmField var interactives: InteractiveEntityContainer? = null
    @JvmField var primaryWall: AbstractWall? = null
    @JvmField var bridge: Tile? = null
    @JvmField var plane: Byte = plane.toByte()
    @JvmField var cullX: Short = 0
    @JvmField var groundItem: AbstractGroundItem? = null
    @JvmField var cullY: Short = 0
    @JvmField var interactiveSideViewHeightOffset: Short = 0
    @JvmField var cullZ: Short = 0
}
