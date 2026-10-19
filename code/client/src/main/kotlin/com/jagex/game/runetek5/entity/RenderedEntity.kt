package com.jagex.game.runetek5.entity

import com.jagex.game.runetek5.scene.InteractiveEntityCache
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class RenderedEntity {
    /**
     * Only set for interactives
     */
    @JvmField var entity: SceneGraphEntity? = null
    @JvmField var interactive: Boolean = false
    @JvmField var parent: InteractiveEntityCache? = null

    fun hovered(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int): Boolean {
        val sizeShift = this.entity!!.getMaxSizeShift()
        if (this.entity!!.boundingBoxPolygons != null) {
            for (index in this.entity!!.boundingBoxPolygons!!.indices) {
                this.entity!!.boundingBoxPolygons!![index].size = this.entity!!.boundingBoxPolygons!![index].size shl sizeShift
                if (this.entity!!.boundingBoxPolygons!![index].hovered(mouseX + this.parent!!.hoverOffsetX, mouseY + this.parent!!.hoverOffsetY)
                    && this.entity!!.calculateProjection(toolkit, mouseX, mouseY)) {
                    this.entity!!.boundingBoxPolygons!![index].size = this.entity!!.boundingBoxPolygons!![index].size shr sizeShift
                    return true
                }
                this.entity!!.boundingBoxPolygons!![index].size = this.entity!!.boundingBoxPolygons!![index].size shr sizeShift
            }
        }
        return false
    }
}
