package com.jagex.game.runetek5.config.cutscene;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;

public class CutsceneEntity {

    public boolean initialized;
    public final int id;
    public PlayerEntity player;
    public NpcEntity npc;
    public int[] animations;
    public final int index;
	public static int drawPriority = 1;

    CutsceneEntity(Packet buffer, int index) {
        this.index = index;
        int type = buffer.readUnsignedByte();
        switch (type) {
            case 0:
                this.id = buffer.readBigSmart();
                break;
            case 1:
            default:
                this.id = -1;
        }
        buffer.readString();
    }

    public void move(int x, int y, int plane, int direction) {
        if (!this.initialized) {
            this.initialized = true;
            if (this.id >= 0) {
                this.npc = new NpcEntity(Resource.mapRegion.getScene());
                this.npc.playerIndex = this.index;
                this.npc.lastUpdate = client.cycles;
                this.npc.setDirection(TypeLists.NPC.list(this.id));
                this.npc.setSize(this.npc.definitions.size);
                this.npc.turnDirection = this.npc.definitions.turnDirection << 3;
                this.npc.worldPriority = ++drawPriority - 1;
            } else {
                this.player = new PlayerEntity(Resource.mapRegion.getScene());
                this.player.decodeAppearance(CutsceneTypeList.CUTSCENE_BUFFER);
                this.player.playerIndex = this.index;
                this.player.lastUpdate = client.cycles;
                this.player.worldPriority = ++drawPriority - 1;
            }
        }
        if (this.id >= 0) {
            this.npc.move(plane, x, y, true, this.npc.getSize());
            this.npc.turn(direction, true);
        } else {
            this.player.plane = this.player.collisionPlane = (byte) plane;
            this.player.move(x, y);
            this.player.turn(direction, true);
        }
    }

    public void reset() {
        this.npc = null;
        this.player = null;
        this.initialized = false;
    }

    void move(int collisionPlane, int baseX, int baseY) {
        if (this.npc != null) {
            this.npc.move(collisionPlane, baseX, baseY, true, this.npc.getSize());
        } else {
            this.player.plane = this.player.collisionPlane = (byte) collisionPlane;
            this.player.move(baseX, baseY);
        }
    }

    boolean isFullyCached() {
        if (this.id < 0) {
            return true;
        } 
        NPCType npcType = TypeLists.NPC.list(this.id);
        boolean cached = npcType.isBodyCached();
        if (this.animations == null) {
            BasType basType = TypeLists.BAS.list(npcType.npcBasId);
            this.animations = basType.getAnimations();
        }
        for (int index = 0; index < this.animations.length; index++) {
            int seqType = this.animations[index];
            cached &= TypeLists.SEQ.list(seqType).ready();
        }
        return cached;
    }

    public PathingEntity getEntity() {
        return this.npc != null ? this.npc : this.player;
    }
}