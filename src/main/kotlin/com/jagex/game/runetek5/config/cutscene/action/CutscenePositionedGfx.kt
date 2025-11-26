package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.sequence.PositionedGfx
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation
import com.jagex.game.runetek5.scene.MapRegion

/**
 *
 * @author Brainpower - 01/02/2023
 *
 */
class CutscenePositionedGfx(buffer: Packet) : AbstractCutsceneSpotAnimation(buffer) {

    @JvmField val x: Int
    @JvmField val y: Int
    @JvmField val plane: Int

    init {
        val bitpackedPosition = buffer.readInt()
        this.x = bitpackedPosition ushr 16
        this.y = bitpackedPosition and 0xffff
        this.plane = buffer.readUByte()
    }

    override fun perform() {
        val absX = this.x * 512 + 256
        val absY = this.y * 512 + 256
        var height = this.plane
        if (height < 3 && Resource.mapRegion!!.settingsBits.areRoofsHidden(this.x, this.y)) {
            height++
        }

        val gfx = SpotAnimation(Resource.mapRegion!!.getScene(), super.gfxId, 0, this.plane, height, absX,
            MapRegion.tileheightForAbsolutePosition(absX, absY, this.plane) - super.displayHeight, absY, this.x, this.x, this.y, this.y, super.rotation, false)
        client.ACTIVE_GFX.put(PositionedGfx(gfx), (this.x.toLong() shl 16) or this.y.toLong())
    }
}
