package com.jagex.game.runetek5.config.object;

/**
 * [J]
 * @author Steven - 22/11/2023
 *
 */
public class ObjectTypeCustomisation {

	public final long id;

    public final int[] objectModelIds;

    public final short[] recolorDst;

    public final short[] retexDst;

	public static long locCustomisationId = 1L;

    public ObjectTypeCustomisation(long id, int[] objectModelIds, short[] recolorDst, short[] retexDst) {
        this.id = id;
        this.objectModelIds = objectModelIds;
        this.recolorDst = recolorDst;
        this.retexDst = retexDst;
    }

}
