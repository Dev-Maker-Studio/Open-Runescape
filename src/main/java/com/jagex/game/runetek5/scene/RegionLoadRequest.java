package com.jagex.game.runetek5.scene;

import com.jagex.game.runetek5.buffer.Packet;

/**
 * 
 * @author Brainpower - 11/03/2023
 *
 */
public class RegionLoadRequest {

    public final RegionLoadingType loadingType;

    public final Packet.Bit buffer;

    public RegionLoadRequest(RegionLoadingType maptype, Packet.Bit bitBuffer) {
        this.loadingType = maptype;
        this.buffer = bitBuffer;
    }

}
