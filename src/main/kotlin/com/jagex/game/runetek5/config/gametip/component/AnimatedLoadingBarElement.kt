package com.jagex.game.runetek5.config.gametip.component

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY

class AnimatedLoadingBarElement(
    class3561: AnchorX,
    class3532: AnchorY,
    i_3: Int,
    i_4: Int,
    i_5: Int,
    i_6: Int,
    i_7: Int,
    i_8: Int,
    i_9: Int,
    i_10: Int,
    i_11: Int,
    i_12: Int,
    i_13: Int,
    i_14: Int,
    i_15: Int,
    @JvmField val anInt10160: Int
) : MultiSpriteLoadingBarElement(class3561, class3532, i_3, i_4, i_5, i_6, i_7, i_8, i_9, i_10, i_11, i_12, i_13, i_14, i_15) {
    override fun getRenderDetails(): TipRenderDetails {
        return TipRenderDetails.ANIMATED_LOADING_BAR
    }
}

fun decodeAnchored(buffer: Packet): AnchoredElement {
    val horizontalAnchor: AnchorX = AnchorX.entries[buffer.readUByte()]
    val verticalAnchor: AnchorY = AnchorY.entries[buffer.readUByte()]
    val offX = buffer.readShort()
    val offY = buffer.readShort()
    val width = buffer.readUShort()
    val height = buffer.readUShort()
    val i_8 = buffer.readShort()
    val fileId = buffer.readBigSmart()
    val i_10 = buffer.readInt()
    return AnchoredElement(horizontalAnchor, verticalAnchor, offX, offY, width, height, i_8, fileId, i_10)
}

fun decodeColorLoadingBar(rsbytebuffer0: Packet): AnchoredElement {
    val anchoredElement = decodeAnchored(rsbytebuffer0)
    val i_3 = rsbytebuffer0.readInt()
    val i_4 = rsbytebuffer0.readInt()
    return ColorLoadingBarElement(
        anchoredElement.horizontalAnchor, anchoredElement.verticalAnchor,
        anchoredElement.offsetX, anchoredElement.offsetY,
        anchoredElement.width, anchoredElement.height,
        anchoredElement.anInt4092, anchoredElement.fileId, anchoredElement.anInt4091, i_3, i_4
    )
}

fun decodeMultiSpriteLoadingBar(rsbytebuffer0: Packet): AnchoredElement {
    val class3502 = decodeAnchored(rsbytebuffer0)
    val i_3 = rsbytebuffer0.readBigSmart()
    val i_4 = rsbytebuffer0.readBigSmart()
    val i_5 = rsbytebuffer0.readBigSmart()
    val i_6 = rsbytebuffer0.readBigSmart()
    val i_7 = rsbytebuffer0.readBigSmart()
    val i_8 = rsbytebuffer0.readBigSmart()
    return MultiSpriteLoadingBarElement(class3502.horizontalAnchor, class3502.verticalAnchor, class3502.offsetX, class3502.offsetY, class3502.width, class3502.height, class3502.anInt4092, class3502.fileId, class3502.anInt4091, i_3, i_4, i_5, i_6, i_7, i_8)
}

fun decodeAnimatedLoadingBar(rsbytebuffer0: Packet): AnchoredElement {
    val class350Sub32 = decodeMultiSpriteLoadingBar(rsbytebuffer0) as MultiSpriteLoadingBarElement
    val i_3 = rsbytebuffer0.readShort()
    return AnimatedLoadingBarElement(
        class350Sub32.horizontalAnchor, class350Sub32.verticalAnchor,
        class350Sub32.offsetX, class350Sub32.offsetY,
        class350Sub32.width, class350Sub32.height,
        class350Sub32.anInt4092, class350Sub32.fileId, class350Sub32.anInt4091,
        class350Sub32.spriteA, class350Sub32.spriteB, class350Sub32.spriteC,
        class350Sub32.spriteD, class350Sub32.spriteE, class350Sub32.spriteF, i_3
    )
}

fun decodeSpriteLoadingBar(buffer: Packet): AnchoredElement {
    val element: AnchoredElement = decodeAnchored(buffer)
    val i_3 = buffer.readInt()
    val i_4 = buffer.readInt()
    val i_5 = buffer.readBigSmart()
    return SpriteLoadingBarElement(
        element.horizontalAnchor, element.verticalAnchor, element.offsetX, element.offsetY, element.width, element.height, element.anInt4092, element.fileId, element.anInt4091, i_4, i_5
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