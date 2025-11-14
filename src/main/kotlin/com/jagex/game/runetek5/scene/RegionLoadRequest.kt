package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.buffer.Packet

/**
 *
 * @author Brainpower - 11/03/2023
 *
 */
class RegionLoadRequest(
    @JvmField val loadingType: RegionLoadingType?, //TODO check nullability requirements on both fields here
    @JvmField val buffer: Packet.Bit?
)
