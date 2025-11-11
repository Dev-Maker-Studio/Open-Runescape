package com.jagex.game.runetek5.entity.pathingentity.player;

import com.darkan.bot.Bot;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.ChatMessageType;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.cutscene.CutsceneEntity;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.sequence.PathingEntitySequence;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.graphics.hint.HintArrow;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder;
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.sound.player.area.AmbientSound;
import com.jagex.game.runetek5.vardomain.VarProviders;

import java.util.Arrays;

public class PlayerUpdate {

    public static final HintArrow[] HINT_ARROWS = new HintArrow[8];
    public static final int[] OUT_PLAYER_INDICES = new int[2048];
    public static final byte[] OUT_PLAYER_UPDATE_STATE = new byte[2048];
    public static final byte[] PLAYER_MOVEMENT_TYPES = new byte[2048];
    public static final Packet[] APPEARANCE_BUFFERS = new Packet[2048];
    public static final int[] anIntArray2426 = new int[2048];
    public static final int[] anIntArray2435 = new int[2048];
    public static final PlayerUpdateEntry[] PLAYER_UPDATES = new PlayerUpdateEntry[2048];
    public static PlayerEntity MY_PLAYER;
    public static int MY_PLAYER_PLANE;
    public static PlayerEntity[] players;
    public static int NUM_OUT_PLAYER_INDICES;
    public static int myPlayerIndex;
    public static int anInt2431;
    public static int anInt2434;
    public static String MY_PLAYER_USERNAME;

    public static void decode(Packet.Bit buffer, int size) {
        anInt2434 = 0;
        decodeUpdate(buffer);
        decodeUpdateMasks(buffer);
        if (buffer.currentPosition != size) {
            throw new RuntimeException(buffer.currentPosition + " " + size);
        }
    }

    public static void decodeUpdate(Packet.Bit bitBuffer) {
        int skipCount = 0;
        bitBuffer.initBitAccess();
        for (int index = 0; index < NUM_OUT_PLAYER_INDICES; index++) {
            int playerPtr = OUT_PLAYER_INDICES[index];
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr] & 0x1) == 0) {
                if (skipCount > 0) {
                    skipCount--;
                    OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                } else {
                    int hasUpdate = bitBuffer.readBits(1);
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer);
                        OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                    } else {
                        decodePositionUpdate(bitBuffer, playerPtr);
                    }
                }
            }
        }
        bitBuffer.finishBitAccess();
        if (skipCount != 0) {
            throw new RuntimeException();
        } 
        bitBuffer.initBitAccess();
        for (int index = 0; index < NUM_OUT_PLAYER_INDICES; index++) {
            int playerPtr = OUT_PLAYER_INDICES[index];
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr] & 0x1) != 0) {
                if (skipCount > 0) {
                    skipCount--;
                    OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                } else {
                    int hasUpdate = bitBuffer.readBits(1);
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer);
                        OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                    } else {
                        decodePositionUpdate(bitBuffer, playerPtr);
                    }
                }
            }
        }
        bitBuffer.finishBitAccess();
        if (skipCount != 0) {
            throw new RuntimeException();
        } 
        bitBuffer.initBitAccess();
        for (int index = 0; index < anInt2431; index++) {
            int playerPtr = anIntArray2426[index];
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr] & 0x1) != 0) {
                if (skipCount > 0) {
                    skipCount--;
                    OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                } else {
                    int hasUpdate = bitBuffer.readBits(1);
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer);
                        OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                    } else if (decodeMovementUpdate(bitBuffer, playerPtr)) {
                        OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                    }
                }
            }
        }
        bitBuffer.finishBitAccess();
        if (skipCount != 0) {
            throw new RuntimeException();
        } 
        bitBuffer.initBitAccess();
        for (int i_3 = 0; i_3 < anInt2431; i_3++) {
            int playerPtr = anIntArray2426[i_3];
            if ((OUT_PLAYER_UPDATE_STATE[playerPtr] & 0x1) == 0) {
                if (skipCount > 0) {
                    --skipCount;
                    OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                } else {
                    int hasUpdate = bitBuffer.readBits(1);
                    if (hasUpdate == 0) {
                        skipCount = decodeSkipcount(bitBuffer);
                        OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                    } else if (decodeMovementUpdate(bitBuffer, playerPtr)) {
                        OUT_PLAYER_UPDATE_STATE[playerPtr] |= 0x2;
                    }
                }
            }
        }
        bitBuffer.finishBitAccess();
        if (skipCount != 0) {
            throw new RuntimeException();
        } 
        NUM_OUT_PLAYER_INDICES = 0;
        anInt2431 = 0;
        for (int index = 1; index < 2048; index++) {
            OUT_PLAYER_UPDATE_STATE[index] >>= 1;
            PlayerEntity player = players[index];
            if (player != null) {
                OUT_PLAYER_INDICES[++NUM_OUT_PLAYER_INDICES - 1] = index;
            } else {
                anIntArray2426[++anInt2431 - 1] = index;
            }
        }
    }

    public static void decodeUpdateMasks(Packet.Bit buffer) {
        for (int i_2 = 0; i_2 < anInt2434; i_2++) {
            int playerPtr = anIntArray2435[i_2];
            PlayerEntity player = players[playerPtr];
            int updateMasks = buffer.readUByte();
            if ((updateMasks & 0x8) != 0) {
                updateMasks += buffer.readUByte() << 8;
            }
            if ((updateMasks & 0x100) != 0) {
                updateMasks += buffer.readUByte() << 16;
            }
            decodePlayerUpdateMask(buffer, playerPtr, player, updateMasks);
        }
    }

    public static void decodePlayerUpdateMask(Packet.Bit stream, int slot, PlayerEntity player, int flags) {
        byte defaultMovespeed = MovementType.STATIONARY.id;

        if ((flags & 0x20000) != 0) {
            player.hue = stream.readByte();
            player.saturation = stream.readByteInverse();
            player.lightness = stream.readByteSubtract();
            player.opacity = (byte) stream.readUByteSubtract();
            player.tintStartCycle = client.cycles + stream.readUShortAdd();
            player.tintEndCycle = client.cycles + stream.readUShort();
        }

        if ((flags & 0x400000) != 0) {
            int count = stream.buffer[++stream.currentPosition - 1] & 0xff;
            for (int index = 0; index < count; index++) {
                int key = stream.readUShortLittle();
                int value = stream.readInt();
                player.playerConfigs.put(key, value);
            }
        }

        if ((flags & 0x200) != 0) {
            int gfxId = stream.readUShort();
            int i_7 = stream.readUIntInverseMiddle();
            if (gfxId == 65535) {
                gfxId = -1;
            }

            int i_8 = stream.readUByteSubtract();
            int i_9 = i_8 & 0x7;
            int i_10 = i_8 >> 3 & 0xf;
            if (i_10 == 15) {
                i_10 = -1;
            }

            boolean bool11 = (i_8 >> 7 & 0x1) == 1;
            player.playSpotAnimation(gfxId, i_7, i_9, i_10, bool11, 1);
        }

        if ((flags & 0x20) != 0) {
            player.faceDirection = stream.readUShortAdd();
            if (player.movementStepsLeft == 0) {
                player.turn(player.faceDirection);
                player.faceDirection = -1;
            }
        }

        if ((flags & 0x800000) != 0) {
            player.playerConfigs.clear();
            int i_6 = stream.buffer[++stream.currentPosition - 1] & 0xff;

            for (int i_7 = 0; i_7 < i_6; i_7++) {
                int i_8 = stream.readUShortLittle();
                int i_9 = stream.readInt();
                player.playerConfigs.put(i_8, i_9);
            }
        }

        if ((flags & 0x10000) != 0) {
            player.reduceAttackPriority = stream.readUByteSubtract() == 1;
        }

        if ((flags & 0x100000) != 0) {
            int size = stream.readUByteSubtract();
            int[] ints_14 = new int[size];
            int[] ints_15 = new int[size];

            for (int i_9 = 0; i_9 < size; i_9++) {
                int i_10 = stream.readUShortAdd();
                if ((i_10 & 0xc000) == 49152) {
                    int i_19 = stream.readUShortAdd();
                    ints_14[i_9] = i_10 << 16 | i_19;
                } else {
                    ints_14[i_9] = i_10;
                }

                ints_15[i_9] = stream.readUShort();
            }

            player.method15797(ints_14, ints_15);
        }

        if ((flags & 0x4000) != 0) {
            String message = stream.readString();
            if (player == MY_PLAYER) {
                ChatBox.writeMessage(ChatMessageType.PUBLIC_CHAT, 0, player.getUsernameWithTitle(), player.getDisplayName(), player.username, message);
            }

            player.sendChat(message, 0, 0);
        }

        if ((flags & 0x80000) != 0) {
            String message = stream.readString();
            int chatFlags = stream.readUByteAdd();
            if ((chatFlags & 0x1) != 0) {
                ChatBox.writeMessage(ChatMessageType.PUBLIC_CHAT, chatFlags, player.getUsernameWithTitle(), player.getDisplayName(), player.username, message);
            }

            player.sendChat(message, 0, 0);
        }

        if ((flags & 0x40000) != 0) {
            int gfxId = stream.readUShortAddLittle();
            int i_7 = stream.readUIntInverseMiddle();
            if (gfxId == 65535) {
                gfxId = -1;
            }

            int i_8 = stream.readUByteAdd();
            int i_9 = i_8 & 0x7;
            int i_10 = i_8 >> 3 & 0xf;
            if (i_10 == 15) {
                i_10 = -1;
            }

            boolean bool11 = (i_8 >> 7 & 0x1) == 1;
            player.playSpotAnimation(gfxId, i_7, i_9, i_10, bool11, 2);
        }

        if ((flags & 0x8000) != 0) {
            player.pMapdot = stream.readUByteSubtract() == 1;
        }

        if ((flags & 0x40) != 0) {
            int numHits = stream.readUByteSubtract();
            if (numHits > 0) {
                for (int i_7 = 0; i_7 < numHits; i_7++) {
                    int soakHitsplatId = -1;
                    int soakText = -1;
                    int hitText = -1;
                    int hitsplatId = stream.readUSmart();
                    if (hitsplatId == 32767) {
                        hitsplatId = stream.readUSmart();
                        hitText = stream.readUSmart();
                        soakHitsplatId = stream.readUSmart();
                        soakText = stream.readUSmart();
                    } else if (hitsplatId == 32766) {
                        hitsplatId = -1;
                        hitText = stream.readUByteInverse();
                    } else {
                        hitText = stream.readUSmart();
                    }

                    int delay = stream.readUSmart();
                    player.applyHit(hitsplatId, hitText, soakHitsplatId, soakText, client.cycles, delay);
                }
            }

            int hitbarCount = stream.readUByte();
            if (hitbarCount > 0) {
                for (int index = 0; index < hitbarCount; index++) {
                    int hitbarId = stream.readUSmart();
                    int lifetimeCycles = stream.readUSmart();
                    if (lifetimeCycles != 32767) {
                        int delay = stream.readUSmart();
                        int fromPercentage = stream.readUByteSubtract();
                        int toPercentage = lifetimeCycles > 0 ? stream.readUByteAdd() : fromPercentage;
                        player.displayHitbar(hitbarId, client.cycles, lifetimeCycles, delay, fromPercentage, toPercentage);
                    } else {
                        player.hideHitbar(hitbarId);
                    }
                }
            }
        }

        if ((flags & 0x1) != 0) {
            int appearanceByteCount = stream.readUByteInverse();
            byte[] appearanceData = new byte[appearanceByteCount];
            Packet appearanceBuffer = new Packet(appearanceData);
            stream.readBytes(appearanceData, 0, appearanceByteCount);
            APPEARANCE_BUFFERS[slot] = appearanceBuffer;
            player.decodeAppearance(appearanceBuffer);
        }

        if ((flags & 0x10) != 0) {
            int[] movementAnimations = new int[MovementType.values().length];

            for (int moveType = 0; moveType < MovementType.values().length; moveType++) {
                movementAnimations[moveType] = stream.readBigSmart();
            }
            Bot.refreshLastAnimated();
            int speed = stream.readUByte();
            animate(player, movementAnimations, speed, false);
        }

        if ((flags & 0x2) != 0) {
            int faceEntity = stream.readUShortAdd();
            if (faceEntity == 65535) {
                faceEntity = -1;
            }

            player.faceEntity = faceEntity;
        }

        if ((flags & 0x1000) != 0) {
            defaultMovespeed = stream.readByteInverse();
        }

        if ((flags & 0x2000) != 0) {
            int i_6 = stream.readUByteSubtract();
            int[] animationIds = new int[i_6];
            int[] ints_15 = new int[i_6];
            int[] ints_17 = new int[i_6];

            for (int i_10 = 0; i_10 < i_6; i_10++) {
                animationIds[i_10] = stream.readBigSmart();
                ints_15[i_10] = stream.readUByteInverse();
                ints_17[i_10] = stream.readUShortAddLittle();
            }

            animate(player, animationIds, ints_15, ints_17);
        }

        if ((flags & 0x4) != 0) {
            PLAYER_MOVEMENT_TYPES[slot] = stream.readByteSubtract();
        }

        if ((flags & 0x200000) != 0) {
            int i_6 = stream.readUShortLittle();
            int i_7 = stream.readUIntLittle();
            if (i_6 == 65535) {
                i_6 = -1;
            }

            int i_8 = stream.readUByteSubtract();
            int i_9 = i_8 & 0x7;
            int i_10 = i_8 >> 3 & 0xf;
            if (i_10 == 15) {
                i_10 = -1;
            }

            boolean bool11 = (i_8 >> 7 & 0x1) == 1;
            player.playSpotAnimation(i_6, i_7, i_9, i_10, bool11, 3);
        }

        if ((flags & 0x800) != 0) {
            player.forceMovementT1XOff = stream.readByteInverse();
            player.forceMovementT1YOff = stream.readByteSubtract();
            player.forceMovementT2XOff = stream.readByteAdd();
            player.forceMovementT2YOff = stream.readByteInverse();
            player.forceMovementT1Delay = stream.readUShortAddLittle() + client.cycles;
            player.forceMovementT2Delay = stream.readUShortLittle() + client.cycles;
            player.forceMoveDirection = stream.readUShortAdd();
            if (player.aBool10568) {
                player.forceMovementT1XOff += player.basePositionX;
                player.forceMovementT1YOff += player.basePositionY;
                player.forceMovementT2XOff += player.basePositionX;
                player.forceMovementT2YOff += player.basePositionY;
                player.movementStepsLeft = 0;
            } else {
                player.forceMovementT1XOff += player.movementBaseX[0];
                player.forceMovementT1YOff += player.movementBaseY[0];
                player.forceMovementT2XOff += player.movementBaseX[0];
                player.forceMovementT2YOff += player.movementBaseY[0];
                player.movementStepsLeft = 1;
            }

            player.previousMovementStepsLeft = 0;
        }

        if ((flags & 0x80) != 0) {
            int spotAnimId = stream.readUShortLittle();
            int flags1 = stream.readInt();
            if (spotAnimId == 65535) {
                spotAnimId = -1;
            }

            int flags2 = stream.readUByteAdd();
            int rotation = flags2 & 0x7;
            int i_10 = flags2 >> 3 & 0xf;
            if (i_10 == 15) {
                i_10 = -1;
            }

            boolean bool11 = (flags2 >> 7 & 0x1) == 1;
            player.playSpotAnimation(spotAnimId, flags1, rotation, i_10, bool11, 0);
        }

        if (player.aBool10568) {
            if (defaultMovespeed == 127) {
                player.move(player.basePositionX, player.basePositionY);
            } else {
                byte speed;
                if (defaultMovespeed != MovementType.STATIONARY.id) {
                    speed = defaultMovespeed;
                } else {
                    speed = PLAYER_MOVEMENT_TYPES[slot];
                }

                animateMovement(player, speed);
                player.move(player.basePositionX, player.basePositionY, speed);
            }
        }

    }

    public static void decodePositionUpdate(Packet.Bit bitBuffer, int updateIndex) {
        CoordGrid baseCoordinates = Resource.mapRegion.getBaseCoordinates();
        boolean bool4 = bitBuffer.readBits(1) == 1;
        if (bool4) {
            anIntArray2435[++anInt2434 - 1] = updateIndex;
        }
        int size = bitBuffer.readBits(2);
        PlayerEntity player = players[updateIndex];
        if (size == 0) {
            if (bool4) {
                player.aBool10568 = false;
            } else {
                if (myPlayerIndex == updateIndex) {
                    throw new RuntimeException();
                }
                PlayerUpdateEntry update = PLAYER_UPDATES[updateIndex] = new PlayerUpdateEntry();
                update.packedCoordinates = (player.plane << 28) +
                		(baseCoordinates.y + player.movementBaseY[0] >> 6) +
                		(baseCoordinates.x + player.movementBaseX[0] >> 6 << 14);
                if (player.faceDirection != -1) {
                    update.rotation = player.faceDirection;
                } else {
                    update.rotation = player.yaw.getRotation();
                }
                update.faceEntity = player.faceEntity;
                update.pMapdot = player.pMapdot;
                update.reduceAttackPriority = player.reduceAttackPriority;
                if (player.isNpc > 0) {
                    AmbientSound.unlinkPlayerSound(player);
                }
                players[updateIndex] = null;
                if (bitBuffer.readBits(1) != 0) {
                    decodeMovementUpdate(bitBuffer, updateIndex);
                }
            }
        } else {
            if (size == 1) {
            	int direction = bitBuffer.readBits(3);
            	int x = player.movementBaseX[0];
            	int y = player.movementBaseY[0];
                if (direction == 0) {
                    x--;
                    y--;
                } else if (direction == 1) {
                    y--;
                } else if (direction == 2) {
                    x++;
                    y--;
                } else if (direction == 3) {
                    x--;
                } else if (direction == 4) {
                    x++;
                } else if (direction == 5) {
                    x--;
                    y++;
                } else if (direction == 6) {
                    y++;
                } else if (direction == 7) {
                    x++;
                    y++;
                }
                if (bool4) {
                    player.basePositionX = x;
                    player.basePositionY = y;
                    player.aBool10568 = true;
                } else {
                    player.move(x, y, PLAYER_MOVEMENT_TYPES[updateIndex]);
                }
            } else if (size == 2) {
            	int direction = bitBuffer.readBits(4);
            	int x = player.movementBaseX[0];
            	int y = player.movementBaseY[0];
                if (direction == 0) {
                    x -= 2;
                    y -= 2;
                } else if (direction == 1) {
                    x--;
                    y -= 2;
                } else if (direction == 2) {
                    y -= 2;
                } else if (direction == 3) {
                    x++;
                    y -= 2;
                } else if (direction == 4) {
                    x += 2;
                    y -= 2;
                } else if (direction == 5) {
                    x -= 2;
                    y--;
                } else if (direction == 6) {
                    x += 2;
                    y--;
                } else if (direction == 7) {
                    x -= 2;
                } else if (direction == 8) {
                    x += 2;
                } else if (direction == 9) {
                    x -= 2;
                    y++;
                } else if (direction == 10) {
                    x += 2;
                    y++;
                } else if (direction == 11) {
                    x -= 2;
                    y += 2;
                } else if (direction == 12) {
                    x--;
                    y += 2;
                } else if (direction == 13) {
                    y += 2;
                } else if (direction == 14) {
                    x++;
                    y += 2;
                } else if (direction == 15) {
                    x += 2;
                    y += 2;
                }
                if (bool4) {
                    player.basePositionX = x;
                    player.basePositionY = y;
                    player.aBool10568 = true;
                } else {
                    player.move(x, y, PLAYER_MOVEMENT_TYPES[updateIndex]);
                }
            } else {
            	int direction = bitBuffer.readBits(1);
                if (direction == 0) {
                	int i_8 = bitBuffer.readBits(12);
                	int i_9 = i_8 >> 10;
                	int i_10 = i_8 >> 5 & 0x1f;
                    if (i_10 > 15) {
                        i_10 -= 32;
                    }
                    int i_11 = i_8 & 0x1f;
                    if (i_11 > 15) {
                        i_11 -= 32;
                    }
                    int baseX = i_10 + player.movementBaseX[0];
                    int baseY = i_11 + player.movementBaseY[0];
                    if (bool4) {
                        player.basePositionX = baseX;
                        player.basePositionY = baseY;
                        player.aBool10568 = true;
                    } else {
                        player.move(baseX, baseY, PLAYER_MOVEMENT_TYPES[updateIndex]);
                    }
                    player.plane = player.collisionPlane = (byte) (i_9 + player.plane & 0x3);
                    if (Resource.mapRegion.getSettingsBits().areRoofsHidden(baseX, baseY)) {
                        player.collisionPlane += 1;
                    }
                    if (myPlayerIndex == updateIndex && player.plane != MY_PLAYER_PLANE) {
                        MY_PLAYER_PLANE = player.plane;
                    }
                } else {
                	int coordinates = bitBuffer.readBits(30);
                	int playerPlane = coordinates >> 28;
                	int playerX = coordinates >> 14 & 0x3fff;
                	int playerY = coordinates & 0x3fff;
                	int baseX = (playerX + baseCoordinates.x + player.movementBaseX[0] & 0x3fff) - baseCoordinates.x;
                	int baseY = (playerY + baseCoordinates.y + player.movementBaseY[0] & 0x3fff) - baseCoordinates.y;
                    if (bool4) {
                        player.basePositionX = baseX;
                        player.basePositionY = baseY;
                        player.aBool10568 = true;
                    } else {
                        player.move(baseX, baseY, PLAYER_MOVEMENT_TYPES[updateIndex]);
                    }
                    player.plane = player.collisionPlane = (byte) (playerPlane + player.plane & 0x3);
                    if (Resource.mapRegion.getSettingsBits().areRoofsHidden(baseX, baseY)) {
                        player.collisionPlane += 1;
                    }
                    if (myPlayerIndex == updateIndex) {
                        MY_PLAYER_PLANE = player.plane;
                    }
                }
            }
        }
    }

    public static int decodeSkipcount(Packet.Bit bitBuffer) {
        int skipType = bitBuffer.readBits(2);
        int skipCount;
        if (skipType == 0) {
            skipCount = 0;
        } else if (skipType == 1) {
            skipCount = bitBuffer.readBits(5);
        } else if (skipType == 2) {
            skipCount = bitBuffer.readBits(8);
        } else {
            skipCount = bitBuffer.readBits(11);
        }
        return skipCount;
    }

    public static boolean decodeMovementUpdate(Packet.Bit bitBuffer, int playerPtr) {
        int updateType = bitBuffer.readBits(2);
        if (updateType == 0) {
            if (bitBuffer.readBits(1) != 0) {
                decodeMovementUpdate(bitBuffer, playerPtr);
            }
            int xInRegion = bitBuffer.readBits(6);
            int yInRegion = bitBuffer.readBits(6);
            boolean bool18 = bitBuffer.readBits(1) == 1;
            if (bool18) {
                anIntArray2435[++anInt2434 - 1] = playerPtr;
            }
            if (players[playerPtr] != null) {
                throw new RuntimeException();
            } 
            PlayerUpdateEntry update = PLAYER_UPDATES[playerPtr];
            PlayerEntity player = players[playerPtr] = new PlayerEntity(Resource.mapRegion.getScene());
            player.playerIndex = playerPtr;
            if (APPEARANCE_BUFFERS[playerPtr] != null) {
                player.decodeAppearance(APPEARANCE_BUFFERS[playerPtr]);
            }
            player.turn(update.rotation, true);
            player.faceEntity = update.faceEntity;
            int coordinates = update.packedCoordinates;
            int playerHeight = coordinates >> 28;
            int playerX = coordinates >> 14 & 0xff;
            int playerY = coordinates & 0xff;
            CoordGrid basePosition = Resource.mapRegion.getBaseCoordinates();
            int baseX = xInRegion + (playerX << 6) - basePosition.x;
            int baseY = yInRegion + (playerY << 6) - basePosition.y;
            player.pMapdot = update.pMapdot;
            player.reduceAttackPriority = update.reduceAttackPriority;
            player.movementTypes[0] = PLAYER_MOVEMENT_TYPES[playerPtr];
            player.plane = player.collisionPlane = (byte) playerHeight;
            if (Resource.mapRegion.getSettingsBits().areRoofsHidden(baseX, baseY)) {
                player.collisionPlane += 1;
            }
            player.move(baseX, baseY);
            player.aBool10568 = false;
            PLAYER_UPDATES[playerPtr] = null;
            return true;           
        } else if (updateType == 1) {
        	int playerHeight = bitBuffer.readBits(2);
        	int coordinates = PLAYER_UPDATES[playerPtr].packedCoordinates;
            PLAYER_UPDATES[playerPtr].packedCoordinates = (coordinates & 0xfffffff) + (((coordinates >> 28) + playerHeight & 0x3) << 28);
            return false;
        } else if (updateType == 2) {
        	int bitpacked = bitBuffer.readBits(5);
        	int heightOffset = bitpacked >> 3;
        	int direction = bitpacked & 0x7;
        	int coordinates = PLAYER_UPDATES[playerPtr].packedCoordinates;
        	int playerHeight = (coordinates >> 28) + heightOffset & 0x3;
            int playerX = coordinates >> 14 & 0xff;
            int playerY = coordinates & 0xff;
            if (direction == 0) {
                playerX--;
                playerY--;
            }
            if (direction == 1) {
                playerY--;
            }
            if (direction == 2) {
                playerX++;
                playerY--;
            }
            if (direction == 3) {
                playerX--;
            }
            if (direction == 4) {
                playerX++;
            }
            if (direction == 5) {
                playerX--;
                playerY++;
            }
            if (direction == 6) {
                playerY++;
            }
            if (direction == 7) {
                playerX++;
                playerY++;
            }
            PLAYER_UPDATES[playerPtr].packedCoordinates = (playerX << 14) + playerY + (playerHeight << 28);
        } else {
        	int bitpackedPositionOffset = bitBuffer.readBits(18);
        	int heightOffset = bitpackedPositionOffset >> 16;
            int offsetX = bitpackedPositionOffset >> 8 & 0xff;
            int offsetY = bitpackedPositionOffset & 0xff;
            int coordinates = PLAYER_UPDATES[playerPtr].packedCoordinates;
            int playerHeight = (coordinates >> 28) + heightOffset & 0x3;
            int playerX = offsetX + (coordinates >> 14) & 0xff;
            int playerY = offsetY + coordinates & 0xff;
            PLAYER_UPDATES[playerPtr].packedCoordinates = (playerX << 14) + playerY + (playerHeight << 28);
        }
        return false;       
    }

    public static void animate(PathingEntity animable, int[] animationIds, int speed, boolean bool3) {
        if (animable.currentAnimations != null) {
            boolean sameSequence = true;
            for (int index = 0; index < animable.currentAnimations.length; index++) {
                if (animable.currentAnimations[index] != animationIds[index]) {
                    sameSequence = false;
                    break;
                }
            }
            Sequence currSeq = animable.currentSequence;
            if (sameSequence && currSeq.hasType()) {
                SeqType currSeqType = animable.currentSequence.getType();
                int speedType = currSeqType.loopType;
                if (speedType == 1) {
                    currSeq.reset(speed);
                }
                if (speedType == 2) {
                    currSeq.resetLoops();
                }
            }
        }
        boolean isDefault = true;
        for (int index = 0; index < animationIds.length; index++) {
            if (animationIds[index] != -1) {
                isDefault = false;
            }
            if (animable.currentAnimations == null || animable.currentAnimations[index] == -1 
            		|| TypeLists.SEQ.list(animationIds[index]).priority >= TypeLists.SEQ.list(animable.currentAnimations[index]).priority) {
                animable.currentAnimations = animationIds;
                animable.currentSequence.setSpeed(speed);
                if (bool3) {
                    animable.previousMovementStepsLeft = animable.movementStepsLeft;
                }
            }
        }
        if (isDefault) {
            animable.currentAnimations = animationIds;
            animable.currentSequence.setSpeed(speed);
            if (bool3) {
                animable.previousMovementStepsLeft = animable.movementStepsLeft;
            }
        }
    }

    public static void animateMovement(PathingEntity entity, int movementType) {
        if (entity.currentAnimations != null) {
            MovementType moveSpeed = (MovementType) Identifiable.identify(MovementType.values(), movementType);
            assert moveSpeed != null;
            int sequence = entity.currentAnimations[moveSpeed.getValueIncr()];
            if (sequence != entity.currentSequence.getId()) {
                entity.currentSequence.animateSequence(sequence, entity.currentSequence.getSpeed());
                entity.previousMovementStepsLeft = entity.movementStepsLeft;
            }
        }
    }

    public static void animate(PathingEntity animable, int[] animations, int[] ints_2, int[] animationFlags) {
        for (int index = 0; index < animations.length; index++) {
            int seqId = animations[index];
            int flags = animationFlags[index];
            int i_8 = ints_2[index];
            for (int seq = 0; flags != 0 && seq < animable.sequences.length; flags >>>= 1) {
                if ((flags & 0x1) != 0) {
                    if (seqId == -1) {
                        animable.sequences[seq] = null;
                    } else {
                        SeqType seqtype = TypeLists.SEQ.list(seqId);
                        int loopType = seqtype.loopType;
                        PathingEntitySequence entitySequence = animable.sequences[seq];
                        if (entitySequence != null && entitySequence.hasType()) {
                            if (seqId == entitySequence.getId()) {
                                if (loopType == 0) {
                                    animable.sequences[seq] = null;
                                    entitySequence = null;
                                } else if (loopType == 1) {
                                    entitySequence.reset();
                                    entitySequence.anInt10065 = i_8;
                                } else if (loopType == 2) {
                                    entitySequence.resetLoops();
                                }
                            } else if (seqtype.priority >= entitySequence.getType().priority) {
                                animable.sequences[seq] = null;
                                entitySequence = null;
                            }
                        }
                        if (entitySequence == null || !entitySequence.hasType()) {
                            entitySequence = animable.sequences[seq] = new PathingEntitySequence(animable);
                            entitySequence.update(seqId);
                            entitySequence.anInt10065 = i_8;
                        }
                    }
                }
                ++seq;
            }
        }
    }

    public static void movePlayers() {
        int numPlayers = NUM_OUT_PLAYER_INDICES;
        int[] playerIndices = OUT_PLAYER_INDICES;
        for (int i = 0; i < numPlayers; i++) {
            PlayerEntity player = players[playerIndices[i]];
            if (player != null) {
                EntityUpdating.move(player, false);
            }
        }
    }

    public static void reset() {
        NUM_OUT_PLAYER_INDICES = 0;
        for (int index = 0; index < 2048; index++) {
            APPEARANCE_BUFFERS[index] = null;
            PLAYER_MOVEMENT_TYPES[index] = MovementType.WALKING.id;
            PLAYER_UPDATES[index] = null;
        }
    }

    public static void decodeOwnPlayer(Packet.Bit packet) {
        packet.initBitAccess();
        int myPlayer = myPlayerIndex;
        PlayerEntity player = MY_PLAYER = players[myPlayer] = new PlayerEntity(Resource.mapRegion.getScene());
        player.playerIndex = myPlayer;
        int coordinates = packet.readBits(30);
        byte playerPlane = (byte) (coordinates >> 28);
        int playerX = coordinates >> 14 & 0x3fff;
        int playerY = coordinates & 0x3fff;
        CoordGrid baseCoordinates = Resource.mapRegion.getBaseCoordinates();
        player.movementBaseX[0] = playerX - baseCoordinates.x;
        player.movementBaseY[0] = playerY - baseCoordinates.y;
        player.setBasePosition(((player.movementBaseX[0] << 9) + (player.getSize() << 8)), player.getBasePosition().coords.y, ((player.movementBaseY[0] << 9) + (player.getSize() << 8)));
        MY_PLAYER_PLANE = player.plane = player.collisionPlane = playerPlane;
        if (Resource.mapRegion.getSettingsBits().areRoofsHidden(player.movementBaseX[0], player.movementBaseY[0])) {
            player.collisionPlane += 1;
        }
        if (APPEARANCE_BUFFERS[myPlayer] != null) {
            player.decodeAppearance(APPEARANCE_BUFFERS[myPlayer]);
        }
        NUM_OUT_PLAYER_INDICES = 0;
        OUT_PLAYER_INDICES[++NUM_OUT_PLAYER_INDICES - 1] = myPlayer;
        OUT_PLAYER_UPDATE_STATE[myPlayer] = 0;
        anInt2431 = 0;
        for (int updateIndex = 1; updateIndex < 2048; updateIndex++) {
            if (myPlayer != updateIndex) {
                int otherCoordinates = packet.readBits(18);
                int otherPlane = otherCoordinates >> 16;
                int otherX = otherCoordinates >> 8 & 0xff;
                int otherY = otherCoordinates & 0xff;
                PlayerUpdateEntry update = PLAYER_UPDATES[updateIndex] = new PlayerUpdateEntry();
                update.packedCoordinates = (otherX << 14) + otherY + (otherPlane << 28);
                update.rotation = 0;
                update.faceEntity = -1;
                update.pMapdot = false;
                update.reduceAttackPriority = false;
                anIntArray2426[++anInt2431 - 1] = updateIndex;
                OUT_PLAYER_UPDATE_STATE[updateIndex] = 0;
            }
        }
        packet.finishBitAccess();
    }

    public static void method6233() {
        int[] ints_1 = OUT_PLAYER_INDICES;
        int i_2;
        for (i_2 = 0; i_2 < NUM_OUT_PLAYER_INDICES; i_2++) {
            PlayerEntity player3 = players[ints_1[i_2]];
            if (player3 != null) {
                player3.updateHintdetailsOntick();
            }
        }
        for (i_2 = 0; i_2 < client.NPC_UPDATE_INDEX; i_2++) {
            long long_6 = client.NPC_UPDATE_INDICES[i_2];
            ObjectNode class282Sub475 = client.NPC_MAP.get(long_6);
            if (class282Sub475 != null) {
                ((PathingEntity) class282Sub475.value).updateHintdetailsOntick();
            }
        }
        if (client.MAP_LOADING_STAGE == 4) {
            for (i_2 = 0; i_2 < CutsceneTypeList.CUTSCENE_ENTITIES.length; i_2++) {
                CutsceneEntity class758 = CutsceneTypeList.CUTSCENE_ENTITIES[i_2];
                if (class758.initialized) {
                    class758.getEntity().updateHintdetailsOntick();
                }
            }
        }
    }

    public static void method8333(int i_0) {
        AmbientSound.update();
        int i_2 = TypeLists.VAR.list(i_0).anInt4327;
        if (i_2 != 0) {
            int i_3 = VarProviders.PLAYER_VAR_PROVIDER.activeVars[i_0];
            if (i_2 == 5) {
                client.anInt7339 = i_3;
            }

            if (i_2 == 6) {
                client.anInt7351 = i_3;
            }
        }

    }

    public static void findRoute(PlayerEntity player, int destX, int destY, byte moveType) {
        int rbx = player.movementBaseX[0];
        int rby = player.movementBaseY[0];
        if (rbx >= 0 && rbx < Resource.mapRegion.getSceneSizeX() && rby >= 0 && rby < Resource.mapRegion.getSceneSizeY() && destX >= 0 && destX < Resource.mapRegion.getSceneSizeX() && destY >= 0 && destY < Resource.mapRegion.getSceneSizeY()) {
            int i_7 = RouteFinder.findRoute(rbx, rby, player.getSize(), RouteStrategies.forFixedTile(destX, destY), Resource.mapRegion.getCollisionMap(player.plane), true, client.WALKING_STEPS_X, client.WALKING_STEPS_Y);
            if (i_7 >= 1 && i_7 <= 3) {
                for (int index = 0; index < i_7 - 1; index++) {
                    player.addMovementStep(client.WALKING_STEPS_X[index], client.WALKING_STEPS_Y[index], moveType);
                }
            }
        }
    }

    public static void animateZero(PlayerEntity player, int size, int speed) {
        int[] animations = new int[MovementType.values().length];
        Arrays.fill(animations, 0, animations.length, size);
        animate(player, animations, speed, false);
    }

	public static void method11626() {
	    int count = NUM_OUT_PLAYER_INDICES;
	    int[] playerIndices = OUT_PLAYER_INDICES;
	    int idleAnimationsSetting = PreferenceManager.preferences.idleAnimations.getValue();
	    boolean animate = idleAnimationsSetting == 1 && count > 200 || idleAnimationsSetting == 0 && count > 50;
	
	    for (int i_4 = 0; i_4 < count; i_4++) {
	        PlayerEntity player5 = players[playerIndices[i_4]];
	        if (!player5.hasAppearance()) {
	            player5.worldPriority = -1;
	        } else if (player5.hidden) {
	            player5.worldPriority = -1;
	        } else {
	            player5.calculatePosition();
	            if (player5.startX >= 0 && player5.startZ >= 0 && player5.endX < Resource.mapRegion.getSceneSizeX() && player5.endZ < Resource.mapRegion.getSceneSizeY()) {
	                player5.aBool10573 = player5.movingSequence.hasType && animate;
                    if (player5 == MY_PLAYER) {
                        player5.worldPriority = Integer.MAX_VALUE;
                    } else {
                        int priority = 0;

                        priority += player5.isHidden ? 0 : 1;
                        priority += player5.entityHitbars.isEmpty() ? 0 : 2;
                        priority += (5 - player5.getSize()) << 2;

                        if (player5.pMapdot || player5.reduceAttackPriority) {
                            priority += 512;
                        } else {
                            priority += client.prioritizePlayerDraw ? 128 : 32;
                            priority += 256;
                        }

                        player5.worldPriority = priority + 1;
                    }
	            } else {
	                player5.worldPriority = -1;
	            }
	        }
	    }
	
	    for (int npcIndex = 0; npcIndex < client.NPC_UPDATE_INDEX; npcIndex++) {
	        NpcEntity npc = (NpcEntity) (client.NPC_MAP.get(client.NPC_UPDATE_INDICES[npcIndex])).value;
	        if (npc.hasDefinition() && npc.definitions.hasTransformations(VarProviders.PLAYER_VAR_PROVIDER)) {
	            npc.calculatePosition();
	            if (npc.startX >= 0 && npc.startZ >= 0 && npc.endX < Resource.mapRegion.getSceneSizeX() && npc.endZ < Resource.mapRegion.getSceneSizeY()) {
	                int drawPriority = 0;
	                if (!npc.isHidden) {
	                    drawPriority++;
	                }
	
	                if (!npc.entityHitbars.isEmpty()) {
	                    drawPriority += 2;
	                }
	
	                drawPriority += 5 - npc.getSize() << 2;

                    int baseValue = client.prioritizePlayerDraw ? 64 : 128;
                    if (npc.definitions.lowPriority)
                        baseValue /= 2;
                    drawPriority += baseValue;
	
	                if (npc.definitions.highPriority) {
	                    drawPriority += 1024;
	                } else if (!npc.definitions.mediumPriority) {
	                    drawPriority += 256;
	                }
	
	                npc.worldPriority = drawPriority + 1;
	            } else {
	                npc.worldPriority = -1;
	            }
	        } else {
	            npc.worldPriority = -1;
	        }
	    }
	
	    for (int arrowIndex = 0; arrowIndex < HINT_ARROWS.length; arrowIndex++) {
	        HintArrow arrow = HINT_ARROWS[arrowIndex];
	        if (arrow != null) {
	            if (arrow.targetType == 1) {
	                ObjectNode npcObject = client.NPC_MAP.get(arrow.targetIndex);
	                if (npcObject != null) {
	                    NpcEntity npc = (NpcEntity) npcObject.value;
	                    if (npc.worldPriority >= 0) {
	                        npc.worldPriority += 2048;
	                    }
	                }
	            } else if (arrow.targetType == 10) {
	                PlayerEntity player = players[arrow.targetIndex];
	                if (player != null && player != MY_PLAYER && player.worldPriority >= 0) {
	                    player.worldPriority += 2048;
	                }
	            }
	        }
	    }
	
	}

	public static void updateDrawPriority(int plane) {
	    int numPlayerIndices = NUM_OUT_PLAYER_INDICES;
	    int[] playerIndices = OUT_PLAYER_INDICES;
	    int[][] drawPriority = Resource.mapRegion.getDrawPriority();
	    int[][] drawPrioritySharedby = Resource.mapRegion.getDrawPrioritySharedby();
	    int totalIndices;
	    if (client.MAP_LOADING_STAGE == 4) {
	        totalIndices = CutsceneTypeList.CUTSCENE_ENTITIES.length;
	    } else {
	        totalIndices = numPlayerIndices + client.NPC_UPDATE_INDEX;
	    }
	
	    for (int index = 0; index < totalIndices; index++) {
	    	PathingEntity entity;
	        if (client.MAP_LOADING_STAGE == 4) {
	            CutsceneEntity cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES[index];
	            if (!cutsceneEntity.initialized) {
	                continue;
	            }
	
	            entity = cutsceneEntity.getEntity();
	        } else {
	            if (index < numPlayerIndices) {
	                entity = players[playerIndices[index]];
	            } else {
	                entity = (PathingEntity) (client.NPC_MAP.get(client.NPC_UPDATE_INDICES[index - numPlayerIndices])).value;
	            }
	
	            if (plane != entity.plane || entity.worldPriority < 0) {
	                continue;
	            }
	        }
	
	        int size = entity.getSize();
	        Vector3 abs = entity.getAbsolutePosition().coords;
	        if ((size & 0x1) == 0) {
	            if (((int) abs.x & 0x1ff) != 0 || ((int) abs.z & 0x1ff) != 0) {
	                continue;
	            }
	        } else if (((int) abs.x & 0x1ff) != 256 || ((int) abs.z & 0x1ff) != 256) {
	            continue;
	        }
	
	        if (size == 1) {
	            int x = (int) abs.x >> 9;
	            int z = (int) abs.z >> 9;
	            if (entity.worldPriority > drawPriority[x][z]) {
	                drawPriority[x][z] = entity.worldPriority;
	                drawPrioritySharedby[x][z] = 1;
	            } else if (drawPriority[x][z] == entity.worldPriority) {
	                drawPrioritySharedby[x][z]++;
	            }
	        } else {
	            int absSize = (size - 1) * 256 + 60;
	            int startX = (int) abs.x - absSize >> 9;
	            int startZ = (int) abs.z - absSize >> 9;
	            int endX = absSize + (int) abs.x >> 9;
	            int endZ = absSize + (int) abs.z >> 9;
	
	            for (int x = startX; x <= endX; x++) {
	                for (int z = startZ; z <= endZ; z++) {
	                    if (entity.worldPriority > drawPriority[x][z]) {
	                        drawPriority[x][z] = entity.worldPriority;
	                        drawPrioritySharedby[x][z] = 1;
	                    } else if (drawPriority[x][z] == entity.worldPriority) {
	                        drawPrioritySharedby[x][z]++;
	                    }
	                }
	            }
	        }
	    }
	}

	public static void method11629(int plane) {
	    int numPlayerIndices = NUM_OUT_PLAYER_INDICES;
	    int[] playerIndices = OUT_PLAYER_INDICES;
	    int[][] drawPriority = Resource.mapRegion.getDrawPriority();
	    int[][] drawPrioritySharedby = Resource.mapRegion.getDrawPrioritySharedby();
	    int totalIndices;
	    if (client.MAP_LOADING_STAGE == 4) {
	        totalIndices = CutsceneTypeList.CUTSCENE_ENTITIES.length;
	    } else {
	        totalIndices = client.aBool7241 ? numPlayerIndices : numPlayerIndices + client.NPC_UPDATE_INDEX;
	    }
	
	    for (int index = 0; index < totalIndices; index++) {
	    	PathingEntity entity;
	        if (client.MAP_LOADING_STAGE == 4) {
	            CutsceneEntity cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES[index];
	            if (!cutsceneEntity.initialized) {
	                continue;
	            }
	
	            entity = cutsceneEntity.getEntity();
	        } else {
	            if (index < numPlayerIndices) {
	                entity = players[playerIndices[index]];
	            } else {
	                entity = (PathingEntity) (client.NPC_MAP.get(client.NPC_UPDATE_INDICES[index - numPlayerIndices])).value;
	            }
	
	            if (plane != entity.plane) {
	                continue;
	            }
	
	            if (entity.worldPriority < 0) {
	                entity.isHidden = false;
	                continue;
	            }
	        }
	
	        entity.anInt10319 = 0;
	        int size = entity.getSize();
	        Vector3 abs = entity.getAbsolutePosition().coords;
	        if ((size & 0x1) == 0) {
	            if (((int) abs.x & 0x1ff) != 0 || ((int) abs.z & 0x1ff) != 0) {
	                entity.isHidden = false;
	                continue;
	            }
	        } else if (((int) abs.x & 0x1ff) != 256 || ((int) abs.z & 0x1ff) != 256) {
	            entity.isHidden = false;
	            continue;
	        }
	
	        if (client.MAP_LOADING_STAGE != 4) {
	            if (size == 1) {
	                int x = (int) abs.x >> 9;
	                int z = (int) abs.z >> 9;
	                if (drawPriority[x][z] != entity.worldPriority) {
	                    entity.isHidden = true;
	                    continue;
	                }
	
	                if (drawPrioritySharedby[x][z] > 1) {
	                    drawPrioritySharedby[x][z]--;
	                    entity.isHidden = true;
	                    continue;
	                }
	            } else {
	                int absSize = (size - 1) * 256 + 252;
	                int startX = (int) abs.x - absSize >> 9;
	                int startZ = (int) abs.z - absSize >> 9;
	                int endX = absSize + (int) abs.x >> 9;
	                int endZ = absSize + (int) abs.z >> 9;
	                if (!PlayerUpdate.entityOnTop(drawPriority, drawPrioritySharedby, entity.worldPriority, startX, startZ, endX, endZ)) {
	                    for (int x = startX; x <= endX; x++) {
	                        for (int z = startZ; z <= endZ; z++) {
	                            if (drawPriority[x][z] == entity.worldPriority) {
	                                drawPrioritySharedby[x][z]--;
	                            }
	                        }
	                    }
	
	                    entity.isHidden = true;
	                    continue;
	                }
	            }
	        }
	
	        entity.isHidden = false;
	        entity.setBasePosition(abs.x, MapRegion.tileheightForAbsolutePosition((int) abs.x, (int) abs.z, entity.plane), abs.z);
	        Resource.mapRegion.getScene().addInteractive(entity, true);
	    }
	
	}

	public static boolean entityOnTop(int[][] maxDrawPriority, int[][] maxDrawPrioritySharedby, int entityDrawPriority, int startX, int startZ, int endX, int endZ) {
	    for (int x = startX; x <= endX; x++) {
	        for (int z = startZ; z <= endZ; z++) {
	            if (entityDrawPriority == maxDrawPriority[x][z] && maxDrawPrioritySharedby[x][z] <= 1) {
	                return true;
	            }
	        }
	    }
	    return false;
	}

	public static void method11619() {
	    int numIndices = NUM_OUT_PLAYER_INDICES;
	    int[] indices = OUT_PLAYER_INDICES;
	    int entities;
	    if (client.MAP_LOADING_STAGE == 4) {
	        entities = CutsceneTypeList.CUTSCENE_ENTITIES.length;
	    } else {
	        entities = client.aBool7241 ? numIndices : numIndices + client.NPC_UPDATE_INDEX;
	    }
	
	    for (int entityIndex = 0; entityIndex < entities; entityIndex++) {
	        Object entity;
	        if (client.MAP_LOADING_STAGE == 4) {
	            CutsceneEntity cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES[entityIndex];
	            if (!cutsceneEntity.initialized) {
	                continue;
	            }
	
	            entity = cutsceneEntity.getEntity();
	        } else {
	            if (entityIndex < numIndices) {
	                entity = players[indices[entityIndex]];
	            } else {
	                entity = (client.NPC_MAP.get(client.NPC_UPDATE_INDICES[entityIndex - numIndices])).value;
	            }
	
	            if (((PathingEntity) entity).worldPriority < 0) {
	                continue;
	            }
	        }
	
	        int size = ((PathingEntity) entity).getSize();
	        Vector3 abs = ((PathingEntity) entity).getAbsolutePosition().coords;
	        if ((size & 0x1) == 0) {
	            if (((int) abs.x & 0x1ff) == 0 && ((int) abs.z & 0x1ff) == 0) {
	                continue;
	            }
	        } else if (((int) abs.x & 0x1ff) == 256 && ((int) abs.z & 0x1ff) == 256) {
	            continue;
	        }
	
	        ((PathingEntity) entity).setBasePosition(abs.x, MapRegion.tileheightForAbsolutePosition((int) abs.x, (int) abs.z, ((PathingEntity) entity).plane), abs.z);
	        Resource.mapRegion.getScene().addInteractive((AbstractInteractiveEntity) entity, true);
	    }
	
	}
}
