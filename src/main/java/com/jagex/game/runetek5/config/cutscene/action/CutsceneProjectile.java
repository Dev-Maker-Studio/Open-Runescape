package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.impl.ProjectileSpawnRequest;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.sequence.Projectile;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.math.Vector3;

public class CutsceneProjectile extends CutsceneAction {

    public final int anInt9403;

    public final int anInt9404;

    public final int anInt9402;

    public final int anInt9407;

    public final int anInt9408;

    public final int anInt9400;

    public final int anInt9401;

    public final int gfxId;

    public final int anInt9405;

    public final int endHeight;

    public final int anInt9409;

    public final int anInt9412;

    public final int anInt9399;

    public CutsceneProjectile(Packet rsbytebuffer1, int i_2, int i_3) {
        super(rsbytebuffer1);
        int i_4;
        if (i_2 == 0) {
            i_4 = rsbytebuffer1.readInt();
            this.anInt9403 = i_4 >>> 16;
            this.anInt9404 = i_4 & 0xffff;
            this.anInt9402 = -1;
        } else {
            this.anInt9403 = -1;
            this.anInt9404 = -1;
            this.anInt9402 = rsbytebuffer1.readUnsignedShort();
        }
        if (i_3 == 0) {
            i_4 = rsbytebuffer1.readInt();
            this.anInt9407 = i_4 >>> 16;
            this.anInt9408 = i_4 & 0xffff;
            this.anInt9400 = -1;
        } else {
            this.anInt9407 = -1;
            this.anInt9408 = -1;
            this.anInt9400 = rsbytebuffer1.readUnsignedShort();
        }
        if (i_2 == 0 && i_3 == 0) {
            this.anInt9401 = rsbytebuffer1.readUnsignedByte();
        } else {
            this.anInt9401 = -1;
        }
        this.gfxId = rsbytebuffer1.readUnsignedShort();
        this.anInt9405 = rsbytebuffer1.readUnsignedByte();
        this.endHeight = rsbytebuffer1.readUnsignedByte();
        this.anInt9409 = rsbytebuffer1.read24BitUnsignedInteger();
        this.anInt9412 = rsbytebuffer1.readUnsignedShort();
        this.anInt9399 = rsbytebuffer1.readUnsignedByte();
    }

    @Override
    public void perform() {
        int dstX;
        int dstY;
        int plane;
        if (this.anInt9403 >= 0) {
            dstX = this.anInt9403 * 512 + 256;
            dstY = this.anInt9404 * 512 + 256;
            plane = this.anInt9401;
        } else {
            PathingEntity animable9 = CutsceneTypeList.CUTSCENE_ENTITIES[this.anInt9402].getEntity();
            Vector3 abs = animable9.getAbsolutePosition().coords;
            dstX = (int) abs.x;
            dstY = (int) abs.z;
            plane = animable9.plane;
        }
        int srcX;
        int srcY;
        if (this.anInt9404 >= 0) {
            srcX = this.anInt9407 * 512 + 256;
            srcY = this.anInt9408 * 512 + 256;
        } else {
            PathingEntity entity = CutsceneTypeList.CUTSCENE_ENTITIES[this.anInt9400].getEntity();
            Vector3 abs = entity.getAbsolutePosition().coords;
            srcX = (int) abs.x;
            srcY = (int) abs.z;
            if (plane < 0) {
                plane = entity.plane;
            }
        }
        int i_7 = this.anInt9399 << 2;
        Projectile projectile = new Projectile(Resource.mapRegion.getScene(), this.gfxId, plane, plane, dstX, dstY,
        		this.anInt9405 << 2, client.cycles, this.anInt9409 + client.cycles, this.anInt9412, i_7, this.anInt9402 + 1, this.anInt9400 + 1, this.endHeight << 2, false, 0);
        projectile.start(srcX, srcY, this.endHeight << 2, this.anInt9409 + client.cycles);
        client.PROJECTILES.addFirst(new ProjectileSpawnRequest(projectile));
    }

    @Override
    public boolean isFullyCached() {
        SpotAnimType gfxType = TypeLists.SPOT_ANIM.list(this.gfxId);
        boolean loaded = gfxType.isModelLoaded();
        SeqType seqType = TypeLists.SEQ.list(gfxType.sequenceId);
        loaded &= seqType.ready();
        return loaded;
    }
}
