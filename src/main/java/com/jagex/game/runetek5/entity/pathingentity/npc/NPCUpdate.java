package com.jagex.game.runetek5.entity.pathingentity.npc;

import static com.jagex.game.runetek5.entity.pathingentity.npc.UpdateFlag.*;

import java.util.Iterator;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.sound.player.area.AmbientSound;

public class NPCUpdate {

    public static void decode(boolean largeView) {
        client.localNpcCount = 0;
        client.npcListSize = 0;
        client.npcUpdateCycle++;
        decodeAddRemove();
        decodeUpdate(largeView);
        decodeMasks();
        boolean isUnlinked = false;
        for (int index = 0; index < client.localNpcCount; index++) {
        	int npcIndice = client.LOCAL_NPC_INDICES[index];
            ObjectNode objectNode = client.NPC_MAP.get(npcIndice);
            NpcEntity npcEntity = (NpcEntity) objectNode.value;
            if (npcEntity.lastUpdate != client.npcUpdateCycle) {
                if (MiniMenu.isOpen && MiniMenu.containsEntryForNpc(npcIndice)) {
                    MiniMenu.close();
                }
                if (npcEntity.definitions.hasSoundEffects()) {
                    AmbientSound.unlinkEntitySound(npcEntity);
                }
                npcEntity.setDirection(null);
                objectNode.unlink();
                isUnlinked = true;
            }
        }
        if (isUnlinked) {
            int i_3 = client.npcCount;
            client.npcCount = client.NPC_MAP.size();
            int i_4 = 0;
            ObjectNode class282Sub479;
            for (Iterator<ObjectNode> iterator8 = client.NPC_MAP.iterator(); iterator8.hasNext(); client.NPC_ARRAY[i_4++] = class282Sub479) {
                class282Sub479 = iterator8.next();
            }
            for (int npcIndex = client.npcCount; npcIndex < i_3; npcIndex++) {
                client.NPC_ARRAY[npcIndex] = null;
            }
        }
        if (client.GAME_CONNECTION_CONTEXT.receivedBuffer.currentPosition != client.GAME_CONNECTION_CONTEXT.currentPacketSize) {
            throw new RuntimeException(client.GAME_CONNECTION_CONTEXT.receivedBuffer.currentPosition + " " + client.GAME_CONNECTION_CONTEXT.currentPacketSize);
        } 
        for (int updateIndex = 0; updateIndex < client.NPC_UPDATE_INDEX; updateIndex++) {
            if (client.NPC_MAP.get(client.NPC_UPDATE_INDICES[updateIndex]) == null) {
                throw new RuntimeException(updateIndex + " " + client.NPC_UPDATE_INDEX);
            }
        }
        if (client.npcCount - client.NPC_UPDATE_INDEX != 0) {
            throw new RuntimeException("" + (client.npcCount - client.NPC_UPDATE_INDEX));
        } 
        for (int npcIndex = 0; npcIndex < client.npcCount; npcIndex++) {
            if (((PathingEntity) client.NPC_ARRAY[npcIndex].value).lastUpdate != client.npcUpdateCycle) {
                throw new RuntimeException("" + ((PathingEntity) client.NPC_ARRAY[npcIndex].value).playerIndex);
            }
        }
    }

    public static void decodeAddRemove() {
        Packet.Bit buffer = client.GAME_CONNECTION_CONTEXT.receivedBuffer;
        buffer.initBitAccess();
        int size = buffer.readBits(8);
        if (size < client.NPC_UPDATE_INDEX) {
            for (int i = size; i < client.NPC_UPDATE_INDEX; i++) {
                client.LOCAL_NPC_INDICES[++client.localNpcCount - 1] = client.NPC_UPDATE_INDICES[i];
            }
        }
        if (size > client.NPC_UPDATE_INDEX) {
            throw new RuntimeException();
        } 
        client.NPC_UPDATE_INDEX = 0;
        for (int i = 0; i < size; i++) {
            int updateIndice = client.NPC_UPDATE_INDICES[i];
            NpcEntity npc = (NpcEntity) (client.NPC_MAP.get(updateIndice)).value;
            int updateRequired = buffer.readBits(1);
            if (updateRequired == 0) {
                client.NPC_UPDATE_INDICES[++client.NPC_UPDATE_INDEX - 1] = updateIndice;
                npc.lastUpdate = client.npcUpdateCycle;
            } else {
                int moveSpeed = buffer.readBits(2);
                if (moveSpeed == 0) {
                	/** Standing still **/
                    client.NPC_UPDATE_INDICES[++client.NPC_UPDATE_INDEX - 1] = updateIndice;
                    npc.lastUpdate = client.npcUpdateCycle;
                    client.NPC_INDICES[++client.npcListSize - 1] = updateIndice;
                } else if (moveSpeed == 1) {
                    client.NPC_UPDATE_INDICES[++client.NPC_UPDATE_INDEX - 1] = updateIndice;
                    npc.lastUpdate = client.npcUpdateCycle;
                    NPCDirection movementDirection = (NPCDirection) Identifiable.identify(NPCDirection.values(), buffer.readBits(3));
                    npc.move(movementDirection, MovementType.WALKING.id);
                    int i_9 = buffer.readBits(1);
                    if (i_9 == 1) {
                        client.NPC_INDICES[++client.npcListSize - 1] = updateIndice;
                    }
                } else if (moveSpeed == 2) {
                	/** General movement **/
                    client.NPC_UPDATE_INDICES[++client.NPC_UPDATE_INDEX - 1] = updateIndice;
                    npc.lastUpdate = client.npcUpdateCycle;
                    if (buffer.readBits(1) == 1) {
                        NPCDirection firstMovementDirection = (NPCDirection) Identifiable.identify(NPCDirection.values(), buffer.readBits(3));
                        npc.move(firstMovementDirection, MovementType.RUNNING.id);
                        NPCDirection secondaryMovementDirection = (NPCDirection) Identifiable.identify(NPCDirection.values(), buffer.readBits(3));
                        npc.move(secondaryMovementDirection, MovementType.RUNNING.id);
                    } else {
                        NPCDirection movementDirection = (NPCDirection) Identifiable.identify(NPCDirection.values(), buffer.readBits(3));
                        npc.move(movementDirection, MovementType.HALF_WALK.id);
                    }

                    int i_11 = buffer.readBits(1);
                    if (i_11 == 1) {
                        client.NPC_INDICES[++client.npcListSize - 1] = updateIndice;
                    }
                } else if (moveSpeed == 3) {
                	/** Teleport **/
                    client.LOCAL_NPC_INDICES[++client.localNpcCount - 1] = updateIndice;
                }
            }
        }
    }

    public static void decodeUpdate(boolean largeView) {
        Packet.Bit bitBuffer = client.GAME_CONNECTION_CONTEXT.receivedBuffer;
        while (bitBuffer.readableBits(client.GAME_CONNECTION_CONTEXT.currentPacketSize) >= 15) {
            int npcIndex = bitBuffer.readBits(15);
            if (npcIndex == 32767) {
                break;
            }
            boolean requiresRotating = false;
            ObjectNode cachedNpc = client.NPC_MAP.get(npcIndex);
            NpcEntity npcEntity;
            if (cachedNpc == null) {
                npcEntity = new NpcEntity(Resource.mapRegion.getScene());
                npcEntity.playerIndex = npcIndex;
                cachedNpc = new ObjectNode(npcEntity);
                client.NPC_MAP.put(cachedNpc, npcIndex);
                client.NPC_ARRAY[++client.npcCount - 1] = cachedNpc;
                requiresRotating = true;
            }
            npcEntity = (NpcEntity) cachedNpc.value;
            client.NPC_UPDATE_INDICES[++client.NPC_UPDATE_INDEX - 1] = npcIndex;
            npcEntity.lastUpdate = client.npcUpdateCycle;
            if (npcEntity.definitions != null && npcEntity.definitions.hasSoundEffects()) {
                AmbientSound.unlinkEntitySound(npcEntity);
            }
            int add = bitBuffer.readBits(1);
            if (add == 1) {
                client.NPC_INDICES[++client.npcListSize - 1] = npcIndex;
            }
            int offY;
            if (largeView) {
                offY = bitBuffer.readBits(8);
                if (offY > 127) {
                    offY -= 256;
                }
            } else {
                offY = bitBuffer.readBits(5);
                if (offY > 15) {
                    offY -= 32;
                }
            }
            int turnRotations = bitBuffer.readBits(3) + 4 << 11 & 0x3fff;
            npcEntity.setDirection(TypeLists.NPC.list(bitBuffer.readBits(15)));
            int offX;
            if (largeView) {
                offX = bitBuffer.readBits(8);
                if (offX > 127) {
                    offX -= 256;
                }
            } else {
                offX = bitBuffer.readBits(5);
                if (offX > 15) {
                    offX -= 32;
                }
            }
            int i_11 = bitBuffer.readBits(1);
            int i_12 = bitBuffer.readBits(2);
            npcEntity.setSize(npcEntity.definitions.size);
            npcEntity.turnDirection = npcEntity.definitions.turnDirection << 3;
            if (requiresRotating) {
                npcEntity.turn(turnRotations, true);
            }
            npcEntity.move(i_12, PlayerUpdate.MY_PLAYER.movementBaseX[0] + offX, PlayerUpdate.MY_PLAYER.movementBaseY[0] + offY, i_11 == 1, npcEntity.getSize());
            if (npcEntity.definitions.hasSoundEffects()) {
                AmbientSound.createAmbientSoundEffect(npcEntity.plane, npcEntity.movementBaseX[0], npcEntity.movementBaseY[0], 0, null, npcEntity, null);
            }
        }
        bitBuffer.finishBitAccess();
    }

    public static void decodeMasks() {
        Packet.Bit buffer = client.GAME_CONNECTION_CONTEXT.receivedBuffer;
        for (int listIndex = 0; listIndex < client.npcListSize; listIndex++) {
            int npcIndice = client.NPC_INDICES[listIndex];
            NpcEntity npc = (NpcEntity) (client.NPC_MAP.get(npcIndice)).value;
            int updateFlag = buffer.readUnsignedByte();
            if ((updateFlag & 0x40) != 0) {
                updateFlag += buffer.readUnsignedByte() << 8;
            }
            if ((updateFlag & 0x2000) != 0) {
                updateFlag += buffer.readUnsignedByte() << 16;
            }
            if ((updateFlag & 0x100000) != 0) {
                updateFlag += buffer.readUnsignedByte() << 24;
            }
            if ((updateFlag & 0x10) != 0) {
                int[] movementAnimations = new int[MovementType.values().length];
                for (int index = 0; index < MovementType.values().length; index++) {
                    movementAnimations[index] = buffer.readBigSmart();
                }
                int speed = buffer.readUnsignedByte();
                PlayerUpdate.animate(npc, movementAnimations, speed, true);
            }
            if ((updateFlag & CUSTOMIZE) != 0) {
                int modelCount = npc.definitions.modelIds.length;
                int colorAdjustments = 0;
                if (npc.definitions.recolorDst != null) {
                    colorAdjustments = npc.definitions.recolorDst.length;
                }
                int textureAdjustments = 0;
                if (npc.definitions.retextureDst != null) {
                    textureAdjustments = npc.definitions.retextureDst.length;
                }
                int actionFlags = buffer.readUnsigned128Byte();
                if ((actionFlags & 0x1) == 1) {
                    npc.customisation = null;
                } else {
                    int[] models = null;
                    if ((actionFlags & 0x2) == 2) {
                        models = new int[modelCount];
                        for (int modelIndex = 0; modelIndex < modelCount; modelIndex++) {
                            models[modelIndex] = buffer.readBigSmart();
                        }
                    }
                    short[] colors = null;
                    if ((actionFlags & 0x4) == 4) {
                        colors = new short[colorAdjustments];
                        for (int colorIndex = 0; colorIndex < colorAdjustments; colorIndex++) {
                            colors[colorIndex] = (short) buffer.readUnsignedShort();
                        }
                    }
                    short[] retextures = null;
                    if ((actionFlags & 0x8) == 8) {
                        retextures = new short[textureAdjustments];
                        for (int textureIndex = 0; textureIndex < textureAdjustments; textureIndex++) {
                            retextures[textureIndex] = (short) buffer.readUnsignedShort();
                        }
                    }
                    long uid = npcIndice | (long) (++npc.meshModifierCount - 1) << 32;
                    npc.customisation = new NPCTypeCustomisation(uid, models, colors, retextures);
                }
            }
            if ((updateFlag & FORCE_MOVE) != 0) {
                npc.forceMovementT1XOff = buffer.read128Byte();
                npc.forceMovementT1YOff = buffer.readByte128();
                npc.forceMovementT2XOff = buffer.readByteC();
                npc.forceMovementT2YOff = buffer.readByte128();
                npc.forceMovementT1Delay = buffer.readShortLE() + client.cycles;
                npc.forceMovementT2Delay = buffer.readShortLE() + client.cycles;
                npc.forceMoveDirection = buffer.readUnsignedShortLE128();
                npc.forceMovementT1XOff += npc.movementBaseX[0];
                npc.forceMovementT1YOff += npc.movementBaseY[0];
                npc.forceMovementT2XOff += npc.movementBaseX[0];
                npc.forceMovementT2YOff += npc.movementBaseY[0];
                npc.movementStepsLeft = 1;
                npc.previousMovementStepsLeft = 0;
            }
            if ((updateFlag & SET_BAS) != 0) {
                npc.currRenderAnim = buffer.readUnsignedShort();
                if (npc.currRenderAnim == 0xffff) {
                    npc.currRenderAnim = -1;
                }
            }
            if ((updateFlag & SET_VARN) != 0) {
                int count = buffer.buffer[++buffer.currentPosition - 1] & 0xff;
                for (int index = 0; index < count; index++) {
                    int id = buffer.readUnsignedShortLE128();
                    int value = buffer.readIntV1();
                    npc.varns.setVarn(id, value);
                }
            }
            if ((updateFlag & PLAY_GFX) != 0) {
                int i_14 = buffer.readUnsignedShort128();
                int i_6 = buffer.readIntV2();
                if (i_14 == 0xffff) {
                    i_14 = -1;
                }
                int i_7 = buffer.readUnsignedByte128();
                int i_8 = i_7 & 0x7;
                int i_9 = i_7 >> 3 & 0xf;
                if (i_9 == 15) {
                    i_9 = -1;
                }
                boolean bool24 = (i_7 >> 7 & 0x1) == 1;
                npc.playSpotAnimation(i_14, i_6, i_8, i_9, bool24, 3);
            }
            if ((updateFlag & FACE_ENTITY) != 0) {
                npc.faceEntity = buffer.readShortLE();
                if (npc.faceEntity == 65535) {
                    npc.faceEntity = -1;
                }
            }
            if ((updateFlag & ANIMATE) != 0) {
                int count = buffer.readUnsignedByteC();
                int[] sequences = new int[count];
                int[] ints_16 = new int[count];
                int[] ints_17 = new int[count];
                for (int index = 0; index < count; index++) {
                    int sequence = buffer.readBigSmart();
                    sequences[index] = sequence;
                    ints_16[index] = buffer.readUnsignedByte();
                    ints_17[index] = buffer.readShortLE();
                }
                PlayerUpdate.animate(npc, sequences, ints_16, ints_17);
            }
            if ((updateFlag & 0x800) != 0) {
                int gfxId = buffer.readShortLE();
                int i_6 = buffer.readInt();
                if (gfxId == 0xffff) {
                    gfxId = -1;
                }
                int i_7 = buffer.readUnsignedByte128();
                int i_8 = i_7 & 0x7;
                int i_9 = i_7 >> 3 & 0xf;
                if (i_9 == 15) {
                    i_9 = -1;
                }
                boolean bool24 = (i_7 >> 7 & 0x1) == 1;
                npc.playSpotAnimation(gfxId, i_6, i_8, i_9, bool24, 1);
            }
            if ((updateFlag & ADD_HITSPLAT) != 0) {
                int hitCount = buffer.readUnsignedByte128();
                if (hitCount > 0) {
                    for (int index = 0; index < hitCount; index++) {
                        int soakHitsplatId = -1;
                        int hitText;
                        int soakText = -1;
                        int hitsplatId = buffer.readUnsignedSmart();
                        if (hitsplatId == 32767) {
                        	// Soaking
                            hitsplatId = buffer.readUnsignedSmart();
                            hitText = buffer.readUnsignedSmart();
                            soakHitsplatId = buffer.readUnsignedSmart();
                            soakText = buffer.readUnsignedSmart();
                        } else if (hitsplatId != 32766) {
                        	// Hitmark
                            hitText = buffer.readUnsignedSmart();
                        } else {
                        	// Missed
                            hitsplatId = -1;
                            hitText = buffer.readUnsignedByte();
                        }
                        int delay = buffer.readUnsignedSmart();
                        npc.applyHit(hitsplatId, hitText, soakHitsplatId, soakText, client.cycles, delay);
                    }
                }
                int hitbarCount = buffer.readUnsignedByte128();
                if (hitbarCount > 0) {
                    for (int index = 0; index < hitbarCount; index++) {
                        int hitbarId = buffer.readUnsignedSmart();
                        int lifetimeCycles = buffer.readUnsignedSmart();
                        if (lifetimeCycles != 32767) {
                            int delay = buffer.readUnsignedSmart();
                            int fromPercentage = buffer.readUnsigned128Byte();
                            int toPercentage = lifetimeCycles > 0 ? buffer.readUnsignedByte128() : fromPercentage;
                            npc.displayHitbar(hitbarId, client.cycles, lifetimeCycles, delay, fromPercentage, toPercentage);
                        } else {
                            npc.hideHitbar(hitbarId);
                        }
                    }
                }
            }
            if ((updateFlag & 0x8) != 0) {
                if (npc.definitions.hasSoundEffects()) {
                    AmbientSound.unlinkEntitySound(npc);
                }
                npc.setDirection(TypeLists.NPC.list(buffer.readBigSmart()));
                npc.setSize(npc.definitions.size);
                npc.turnDirection = npc.definitions.turnDirection << 3;
                if (npc.definitions.hasSoundEffects()) {
                    AmbientSound.createAmbientSoundEffect(npc.plane, npc.movementBaseX[0], npc.movementBaseY[0], 0, null, npc, null);
                }
            }
            if ((updateFlag & 0x2000000) != 0) {
                int spotanimId = buffer.readShortLE();
                int flags = buffer.readIntLE();
                if (spotanimId == 65535) {
                    spotanimId = -1;
                }
                int bitpacked = buffer.readUnsignedByte();
                int rotation = bitpacked & 0x7;
                int i_9 = bitpacked >> 3 & 0xf;
                if (i_9 == 15) {
                    i_9 = -1;
                }
                boolean bool24 = (bitpacked >> 7 & 0x1) == 1;
                npc.playSpotAnimation(spotanimId, flags, rotation, i_9, bool24, 2);
            }
            if ((updateFlag & UPDATE_STATS) != 0) {
                int i_14 = buffer.buffer[++buffer.currentPosition - 1] & 0xff;
                for (int index = 0; index < i_14; index++) {
                    int statIndex = buffer.readUnsignedByte128();
                    int max = buffer.readInt();
                    int current = buffer.method13132();
                    npc.updateStats(statIndex, max, current);
                }
            }
            if ((updateFlag & 0x40000) != 0) {
                int headmodelCount = npc.definitions.headModels.length;
                int adjustmentLength = 0;
                if (npc.definitions.recolorDst != null) {
                    adjustmentLength = npc.definitions.recolorDst.length;
                }
                byte total = 0;
                if (npc.definitions.retextureDst != null) {
                    adjustmentLength = npc.definitions.retextureDst.length;
                }
                int actionFlag = buffer.readUnsignedByte128();
                if ((actionFlag & 0x1) != 1) {
                    int[] models = null;
                    if ((actionFlag & 0x2) == 2) {
                        models = new int[headmodelCount];
                        for (int modelIndex = 0; modelIndex < headmodelCount; modelIndex++) {
                            models[modelIndex] = buffer.readBigSmart();
                        }
                    }
                    short[] recolorDst = null;
                    if ((actionFlag & 0x4) == 4) {
                        recolorDst = new short[adjustmentLength];
                        for (int colorIndex = 0; colorIndex < adjustmentLength; colorIndex++) {
                            recolorDst[colorIndex] = (short) buffer.readUnsignedShort();
                        }
                    }
                    short[] retextureDst = null;
                    if ((actionFlag & 0x8) == 8) {
                        retextureDst = new short[total];
                    }
                    long uid = npcIndice | (long) (++npc.headMeshModifierCount - 1) << 32;
                    new NPCTypeCustomisation(uid, models, recolorDst, retextureDst);
                }
            }
            if ((updateFlag & UPDATE_CB_LVL) != 0) {
                npc.modifiedCbLvl = buffer.readUnsignedShort128();
                if (npc.modifiedCbLvl == 0xffff) {
                    npc.modifiedCbLvl = npc.definitions.combatLevel;
                }
            }
            if ((updateFlag & 0x400000) != 0) {
                npc.varns.clear();
                int i_14 = buffer.buffer[++buffer.currentPosition - 1] & 0xff;
                for (int i_6 = 0; i_6 < i_14; i_6++) {
                    int i_7 = buffer.readUnsignedShort();
                    int i_8 = buffer.readIntV1();
                    npc.varns.setVarn(i_7, i_8);
                }
            }
            if ((updateFlag & 0x4) != 0) {
                npc.faceX = buffer.readUnsignedShortLE128();
                npc.faceY = buffer.readShortLE();
            }
            if ((updateFlag & 0x20000) != 0) {
                npc.hue = buffer.read128Byte();
                npc.saturation = buffer.readByte();
                npc.lightness = buffer.readByte();
                npc.opacity = (byte) buffer.readUnsigned128Byte();
                npc.tintStartCycle = client.cycles + buffer.readShortLE();
                npc.tintEndCycle = client.cycles + buffer.readUnsignedShort();
            }
            if ((updateFlag & 0x800000) != 0) {
                npc.modifiedName = buffer.readString();
                assert npc.modifiedName != null;
                if (npc.modifiedName.isEmpty() || npc.modifiedName.equals(npc.definitions.name)) {
                    npc.modifiedName = npc.definitions.name;
                }
            }
            if ((updateFlag & 0x2) != 0) {
                npc.method16164(buffer.readString());
            }
            if ((updateFlag & 0x4000) != 0) {
                int i_14 = buffer.readUnsigned128Byte();
                int[] ints_15 = new int[i_14];
                int[] ints_16 = new int[i_14];
                for (int i_8 = 0; i_8 < i_14; i_8++) {
                    int i_9 = buffer.readUnsignedShortLE128();
                    if ((i_9 & 0xc000) == 49152) {
                        int i_10 = buffer.readUnsignedShort();
                        ints_15[i_8] = i_9 << 16 | i_10;
                    } else {
                        ints_15[i_8] = i_9;
                    }
                    ints_16[i_8] = buffer.readUnsignedShortLE128();
                }
                npc.method15797(ints_15, ints_16);
            }
            if ((updateFlag & 0x20) != 0) {
                int i_14 = buffer.readUnsignedShort();
                int i_6 = buffer.readIntV2();
                if (i_14 == 65535) {
                    i_14 = -1;
                }
                int i_7 = buffer.readUnsignedByteC();
                int i_8 = i_7 & 0x7;
                int i_9 = i_7 >> 3 & 0xf;
                if (i_9 == 15) {
                    i_9 = -1;
                }
                boolean bool24 = (i_7 >> 7 & 0x1) == 1;
                npc.playSpotAnimation(i_14, i_6, i_8, i_9, bool24, 0);
            }
        }
    }

    public static void moveNpcs() {
        for (int index = 0; index < client.NPC_UPDATE_INDEX; index++) {
            int npcRef = client.NPC_UPDATE_INDICES[index];
            ObjectNode objectNode = client.NPC_MAP.get(npcRef);
            if (objectNode != null) {
                NpcEntity npc = (NpcEntity) objectNode.value;
                EntityUpdating.move(npc, false);
            }
        }
    }
}
