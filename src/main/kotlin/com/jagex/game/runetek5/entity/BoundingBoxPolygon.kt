package com.jagex.game.runetek5.entity

class BoundingBoxPolygon : EntityNode() {

    @JvmField var top: Int = 0
    @JvmField var left: Int = 0
    @JvmField var bottom: Int = 0
    @JvmField var right: Int = 0
    @JvmField var size: Int = 0 // TODO: better name?
    @JvmField var positionCalculated: Boolean = false //TODO: is this correct?

    fun hovered(mouseX: Int, mouseY: Int): Boolean {
        if (!this.positionCalculated) {
            return false
        }
        val width = this.right - this.left
        val length = this.top - this.bottom
        val size2d = width * width + length * length
        var referenceSize2d = width * mouseX + length * mouseY - (width * this.left + length * this.bottom)
        if (referenceSize2d <= 0) {
            val offsetLeft = this.left - mouseX
            val offsetBottom = this.bottom - mouseY
            return offsetLeft * offsetLeft + offsetBottom * offsetBottom < this.size * this.size
        }
        if (referenceSize2d > size2d) {
            val relativeX = this.right - mouseX
            val relativeY = this.top - mouseY
            return relativeX * relativeX + relativeY * relativeY < this.size * this.size
        }
        referenceSize2d = (referenceSize2d shl 10) / size2d
        val l = (width * referenceSize2d shr 10) + this.left - mouseX
        val t = (length * referenceSize2d shr 10) + this.bottom - mouseY
        return l * l + t * t < this.size * this.size
    }
}
