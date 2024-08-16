package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;

public class CutsceneDirectCameraTransition extends CutsceneAction {

    public final int anInt9299;

    public final int anInt9300;

    public final int anInt9301;

    public final int anInt9302;

    public final int anInt9303;

    public final int anInt9304;

    public CutsceneDirectCameraTransition(Packet buffer) {
        super(buffer);
        this.anInt9299 = buffer.readUnsignedShort();
        this.anInt9300 = buffer.readUnsignedShort();
        this.anInt9301 = buffer.readUnsignedShort();
        this.anInt9302 = buffer.readUnsignedShort();
        this.anInt9303 = buffer.readUnsignedShort();
        this.anInt9304 = buffer.readUnsignedShort();
    }

    @Override
    public void perform() {
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS[this.anInt9299].method1363(0);
        CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS[this.anInt9300].method1363(1);
        client.anInt7277 = 0;
        client.anInt7279 = this.anInt9301;
        client.anInt7256 = 0;
        client.anInt7327 = this.anInt9303;
        client.anInt7182 = this.anInt9304;
        client.anInt7448 = 1;
        client.anInt7280 = this.anInt9302;
        Camera.cameraCalculationType = 3;
        Camera.calculateCutsceneCameraPosition();
        client.aBool7321 = true;
    }
}
