package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;

/**
 * 
 * @author Brainpower - 06/04/2022
 *
 */
public class CutsceneUncenteredCameraMovement extends CutsceneAction {

    public final int localX;

    public final int moveZ;

    public final int localY;

    public final int angleX;

    public final int angleY;

    public CutsceneUncenteredCameraMovement(Packet buffer) {
        super(buffer);
        this.localX = buffer.readUnsignedShort();
        this.moveZ = buffer.readUnsignedShort();
        this.localY = buffer.readUnsignedShort();
        this.angleX = buffer.readUnsignedShort();
        this.angleY = buffer.readUnsignedShort();
    }

    @Override
    public void perform() {
        Camera.moveCamera(this.localX, this.localY, this.moveZ, 100, 100, false);
        Camera.setAngle(this.angleX, this.angleY, 0);
        client.aBool7321 = true;
    }
}
