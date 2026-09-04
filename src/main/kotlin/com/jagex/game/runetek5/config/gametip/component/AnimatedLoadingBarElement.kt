package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class AnimatedLoadingBarElement(
    horizontalAnchor: AnchorX,
    verticalAnchor: AnchorY,
    offsetX: Int,
    offsetY: Int,
    width: Int,
    height: Int,
    textOffsetY: Int,
    fileId: Int,
    textColor: Int,
    fillSpriteId: Int,
    emptySpriteId: Int,
    leftCapSpriteId: Int,
    rightCapSpriteId: Int,
    topBorderSpriteId: Int,
    bottomBorderSpriteId: Int,
    @JvmField val scrollSpeed: Int
) : MultiSpriteLoadingBarElement(horizontalAnchor, verticalAnchor, offsetX, offsetY, width, height, textOffsetY, fileId, textColor, fillSpriteId, emptySpriteId, leftCapSpriteId, rightCapSpriteId, topBorderSpriteId, bottomBorderSpriteId) {
    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.ANIMATED_LOADING_BAR
    }
}

fun decodeAnchored(buffer: Packet): AnchoredElement {
    val horizontalAnchor: AnchorX = AnchorX.entries[buffer.readUByte()]
    val verticalAnchor: AnchorY = AnchorY.entries[buffer.readUByte()]
    val offsetX = buffer.readShort()
    val offsetY = buffer.readShort()
    val width = buffer.readUShort()
    val height = buffer.readUShort()
    val textOffsetY = buffer.readShort()
    val fileId = buffer.readBigSmart()
    val textColor = buffer.readInt()
    return AnchoredElement(horizontalAnchor, verticalAnchor, offsetX, offsetY, width, height, textOffsetY, fileId, textColor)
}

fun decodeColorLoadingBar(buffer: Packet): AnchoredElement {
    val element = decodeAnchored(buffer)
    val fillColor = buffer.readInt()
    val outlineColor = buffer.readInt()
    return ColorLoadingBarElement(
        element.horizontalAnchor, element.verticalAnchor,
        element.offsetX, element.offsetY,
        element.width, element.height,
        element.textOffsetY, element.fileId, element.textColor, fillColor, outlineColor
    )
}

fun decodeMultiSpriteLoadingBar(buffer: Packet): AnchoredElement {
    val element = decodeAnchored(buffer)
    val fillSpriteId = buffer.readBigSmart()
    val emptySpriteId = buffer.readBigSmart()
    val leftCapSpriteId = buffer.readBigSmart()
    val rightCapSpriteId = buffer.readBigSmart()
    val topBorderSpriteId = buffer.readBigSmart()
    val bottomBorderSpriteId = buffer.readBigSmart()
    return MultiSpriteLoadingBarElement(element.horizontalAnchor, element.verticalAnchor, element.offsetX, element.offsetY, element.width, element.height, element.textOffsetY, element.fileId, element.textColor, fillSpriteId, emptySpriteId, leftCapSpriteId, rightCapSpriteId, topBorderSpriteId, bottomBorderSpriteId)
}

fun decodeAnimatedLoadingBar(buffer: Packet): AnchoredElement {
    val element = decodeMultiSpriteLoadingBar(buffer) as MultiSpriteLoadingBarElement
    val scrollSpeed = buffer.readShort()
    return AnimatedLoadingBarElement(
        element.horizontalAnchor, element.verticalAnchor,
        element.offsetX, element.offsetY,
        element.width, element.height,
        element.textOffsetY, element.fileId, element.textColor,
        element.spriteA, element.spriteB, element.spriteC,
        element.spriteD, element.spriteE, element.spriteF, scrollSpeed
    )
}

fun decodeSpriteLoadingBar(buffer: Packet): AnchoredElement {
    val element: AnchoredElement = decodeAnchored(buffer)
    val fillColor = buffer.readInt()
    val backgroundColor = buffer.readInt()
    val spriteFileId = buffer.readBigSmart()
    return SpriteLoadingBarElement(
        element.horizontalAnchor, element.verticalAnchor, element.offsetX, element.offsetY, element.width, element.height, element.textOffsetY, element.fileId, element.textColor, backgroundColor, spriteFileId
    )
}

fun decodeRotatable(buffer: Packet): SpritePositioning {
    val positioning = SpritePositioning.decode(buffer)
    val angle = buffer.readMedium()
    return RotatableSpritePositioning(
        positioning.spriteId,
        positioning.horizontalAnchor,
        positioning.verticalAnchor,
        positioning.spriteOffsetX,
        positioning.spriteOffsetY,
        angle
    )
}