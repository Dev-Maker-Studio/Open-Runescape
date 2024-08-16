package com.jagex.game.runetek5.entity.pathingentity;

import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.cutscene.CutsceneEntity;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.hitbar.HitbarType;
import com.jagex.game.runetek5.config.hitsplat.HitSplatType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimType;
import com.jagex.game.runetek5.entity.EntityNodeList;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.hint.HintArrow;
import com.jagex.game.runetek5.graphics.hint.HintDetails;
import com.jagex.game.runetek5.graphics.hint.HintRenderer;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.math.Quaternion;
import com.jagex.game.runetek5.math.QuaternionPool;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.vardomain.VarProviders;

public class EntityUpdating {
	
	public static int currentMovementType;
	public static final EntityNodeList aClass4577745 = new EntityNodeList();
	public static int anInt7741;
	
    public static void method12366(int i_0, int startX, int startY, int endX, int endY, int drawX, int drawY) {
        if (client.MAP_LOADING_STAGE == 1) {
            int playerIndiceCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
            int[] playerIndices = PlayerUpdate.OUT_PLAYER_INDICES;
            for (int index = 0; index < playerIndiceCount; index++) {
                PlayerEntity player = PlayerUpdate.players[playerIndices[index]];
                if (player != null) {
                    player.method15818(i_0, startX, startY, endX, endY, drawX, drawY);
                }
            }
            for (int index = 0; index < client.NPC_UPDATE_INDEX; index++) {
                int npcKey = client.NPC_UPDATE_INDICES[index];
                ObjectNode npcObjectNode = client.NPC_MAP.get(npcKey);
                if (npcObjectNode != null) {
                    ((PathingEntity) npcObjectNode.value).method15818(i_0, startX, startY, endX, endY, drawX, drawY);
                }
            }
        }

    }

    public static void move(PathingEntity pathingEntity, boolean cutscene) {
        int moveType = MovementType.STATIONARY.id;
        int turnDirection = 0;
        if (pathingEntity.forceMovementT1Delay > client.cycles) {
            PathingEntity.forceMovementType1(pathingEntity);
        } else if (pathingEntity.forceMovementT2Delay >= client.cycles) {
            PathingEntity.forceMovementType2(pathingEntity);
        } else {
            EntityUpdating.updatePosition(pathingEntity, cutscene);
            moveType = EntityUpdating.currentMovementType;
            turnDirection = EntityUpdating.turnDirectionIndex;
        }
        Vector3 abs = pathingEntity.getAbsolutePosition().coords;
        if ((int) abs.x < 512 || (int) abs.z < 512 || (int) abs.x >= (Resource.mapRegion.getSceneSizeX() - 1) * 512 || (int) abs.z >= (Resource.mapRegion.getSceneSizeY() - 1) * 512) {
            pathingEntity.currentSequence.update(-1);
            for (int index = 0; index < pathingEntity.spotAnimations.length; index++) {
                pathingEntity.spotAnimations[index].spotAnimationId = -1;
                pathingEntity.spotAnimations[index].sequence.update(-1);
            }
            pathingEntity.currentAnimations = null;
            pathingEntity.forceMovementT1Delay = 0;
            pathingEntity.forceMovementT2Delay = 0;
            moveType = MovementType.STATIONARY.id;
            turnDirection = 0;
            pathingEntity.setBasePosition((512 * pathingEntity.movementBaseX[0] + pathingEntity.getSize() * 256), abs.y, (pathingEntity.movementBaseY[0] * 512 + pathingEntity.getSize() * 256));
            pathingEntity.resetXyIndex();
        }
        if (PlayerUpdate.MY_PLAYER == pathingEntity && ((int) abs.x < 6144 || (int) abs.z < 6144 || (int) abs.x >= (Resource.mapRegion.getSceneSizeX() - 12) * 512 || (int) abs.z >= (Resource.mapRegion.getSceneSizeY() - 12) * 512)) {
            pathingEntity.currentSequence.update(-1);
            for (int index = 0; index < pathingEntity.spotAnimations.length; index++) {
                pathingEntity.spotAnimations[index].spotAnimationId = -1;
                pathingEntity.spotAnimations[index].sequence.update(-1);
            }
            pathingEntity.currentAnimations = null;
            pathingEntity.forceMovementT1Delay = 0;
            pathingEntity.forceMovementT2Delay = 0;
            moveType = MovementType.STATIONARY.id;
            turnDirection = 0;
            pathingEntity.setBasePosition(
            		(512 * pathingEntity.movementBaseX[0] + pathingEntity.getSize() * 256),
            		abs.y, 
            		(512 * pathingEntity.movementBaseY[0] + pathingEntity.getSize() * 256));
            pathingEntity.resetXyIndex();
        }
        int rotation = turnTo(pathingEntity);
        EntityUpdating.rotateEquipment(pathingEntity);
        PathingEntity.animateMovement(pathingEntity, moveType, turnDirection, rotation);
        PlayerUpdate.animateMovement(pathingEntity, moveType);
        PathingEntity.animateMovement(pathingEntity);
        Quaternion quaternion = QuaternionPool.newQuaternion();
        quaternion.rotateEuler(MathUtils.getTheta(pathingEntity.yaw.getRotation()), MathUtils.getTheta(pathingEntity.pitch.getRotation()), MathUtils.getTheta(pathingEntity.roll.getRotation()));
        pathingEntity.rotationUpdate(quaternion);
        quaternion.store();
    }

    public static void drawEntitites(int clipLeft, int clipTop, int width, int height) {
        int playerIndiceCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
        int[] playerIndices = PlayerUpdate.OUT_PLAYER_INDICES;
        client.currentHintPtr = 0;
        int maxIndex;
        if (client.MAP_LOADING_STAGE == 4) {
            maxIndex = CutsceneTypeList.CUTSCENE_ENTITIES.length;
        } else {
            maxIndex = playerIndiceCount + client.NPC_UPDATE_INDEX;
        }
        Object entity;
        for (int id = 0; id < maxIndex; id++) {
            NPCType npctype = null;
            if (client.MAP_LOADING_STAGE == 4) {
                CutsceneEntity cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES[id];
                if (!cutsceneEntity.initialized) {
                    continue;
                }
                entity = cutsceneEntity.getEntity();
                if (((PathingEntity) entity).drawCycle != client.timesSceneRendered) {
                    continue;
                }
                if (cutsceneEntity.id >= 0) {
                    npctype = ((NpcEntity) entity).definitions;
                    if (npctype.alternativeIds != null) {
                        npctype = npctype.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
                        if (npctype == null) {
                            continue;
                        }
                    }
                }
            } else {
                if (id < playerIndiceCount) {
                    entity = PlayerUpdate.players[playerIndices[id]];
                } else {
                    entity = (client.NPC_MAP.get(client.NPC_UPDATE_INDICES[id - playerIndiceCount])).value;
                    npctype = ((NpcEntity) entity).definitions;
                    if (npctype.alternativeIds != null) {
                        npctype = npctype.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
                        if (npctype == null) {
                            continue;
                        }
                    }
                }
                if (((PathingEntity) entity).worldPriority < 0 || ((PathingEntity) entity).drawCycle != client.timesSceneRendered && ((PathingEntity) entity).plane != PlayerUpdate.MY_PLAYER.plane) {
                    continue;
                }
            }
            HintRenderer.calculateHintForSceneobject((SceneGraphEntity) entity, ((PathingEntity) entity).getIconHeight());
            if (client.HINT_POSITION[0] >= 0.0F) {
                if (((PathingEntity) entity).privateMessagesEnabled()) {
                    HintDetails details = ((PathingEntity) entity).getHintDetails();
                    if (details != null && client.currentHintPtr < client.MAX_HINTS) {
                        client.HINT_WIDTH[client.currentHintPtr] = FontMetrics.aClass4143438.getWidthNoSprites(details.getText()) / 2;
                        client.HINT_X[client.currentHintPtr] = (int) client.HINT_POSITION[0];
                        client.HINT_Y[client.currentHintPtr] = (int) client.HINT_POSITION[1];
                        client.HINT_DETAILS[client.currentHintPtr] = details;
                        client.currentHintPtr++;
                    }
                }
                int drawY = (int) (clipTop + client.HINT_POSITION[1]);
                drawY -= FontMetrics.aClass4143438.topPadding;
                boolean bool94 = false;
                if (!((PathingEntity) entity).isHidden && !((PathingEntity) entity).entityHitbars.isEmpty()) {
                    for (EntityHitbar entityHitbar = (EntityHitbar) ((PathingEntity) entity).entityHitbars.getFirst(); entityHitbar != null; entityHitbar = (EntityHitbar) ((PathingEntity) entity).entityHitbars.getNext()) {
                        EntityHitbarDisplayDetails displayDetails = entityHitbar.method12600(client.cycles);
                        if (displayDetails == null) {
                            if (entityHitbar.method12602()) {
                                entityHitbar.unlinkNode();
                            }
                        } else {
                            HitbarType hitbarType = entityHitbar.hitbarType;
                            PlayerEntity playerEntity = PlayerUpdate.players[playerIndices[id]];
                            NativeSprite redBar;
                            NativeSprite greenBar;
                            if (playerEntity != null && playerEntity.pMapdot) {
                                redBar = hitbarType.getPRedBar(Renderers.activeToolkit);
                                greenBar = hitbarType.getPGreenBar(Renderers.activeToolkit);
                            } else {
                                redBar = hitbarType.getRedBar(Renderers.activeToolkit);
                                greenBar = hitbarType.getGreenBar(Renderers.activeToolkit);
                            }
                            if (redBar != null && greenBar != null) {
                                int alpha = 255;
                                int i_23 = client.cycles - displayDetails.discardAtCycle;
                                int i_24 = greenBar.getWidth() * displayDetails.toPercentage / 255;
                                int i_116;
                                if (displayDetails.lifetimeCycles > i_23) {
                                    int i_25 = hitbarType.anInt2442 == 0 ? 0 : hitbarType.anInt2442 * (i_23 / hitbarType.anInt2442);
                                    int drawX = greenBar.getWidth() * displayDetails.fromPercentage / 255;
                                    i_116 = i_25 * (i_24 - drawX) / displayDetails.lifetimeCycles + drawX;
                                } else {
                                    i_116 = i_24;
                                    int i_25 = displayDetails.lifetimeCycles + hitbarType.anInt2443 - i_23;
                                    if (hitbarType.anInt2439 >= 0) {
                                        alpha = (i_25 << 8) / (hitbarType.anInt2443 - hitbarType.anInt2439);
                                    }
                                }
                                if (displayDetails.toPercentage > 0 && i_116 < 2) {
                                    i_116 = 2;
                                }
                                int i_25 = redBar.getHeight();
                                int drawX = (int) (client.HINT_POSITION[0] + clipLeft - (redBar.getWidth() >> 1));
                                drawY -= i_25;
                                if (alpha >= 0 && alpha < 255) {
                                    int packedAlpha = alpha << 24;
                                    int colorArgb = packedAlpha | 0xffffff;
                                    redBar.draw(drawX, drawY, 0, colorArgb, 1);
                                    Renderers.activeToolkit.expandClipping(drawX, drawY, drawX + i_116, drawY + i_25);
                                    greenBar.draw(drawX, drawY, 0, colorArgb, 1);
                                } else {
                                    redBar.draw(drawX, drawY);
                                    Renderers.activeToolkit.expandClipping(drawX, drawY, i_116 + drawX, drawY + i_25);
                                    greenBar.draw(drawX, drawY);
                                }
                                Renderers.activeToolkit.setClipping(clipLeft, clipTop, clipLeft + width, height + clipTop);
                                drawY -= 2;
                                bool94 = true;
                            }
                        }
                    }
                }
                if (!bool94) {
                    drawY -= client.ENTITY_DEFAULTS.anInt7028 + 2;
                }
                if (!((PathingEntity) entity).isHidden) {
                    if (npctype == null) {
                        PlayerEntity playerEntity = (PlayerEntity) entity;
                        if (playerEntity.skullId != -1) {
                        	NativeSprite skullSprite = StaticMedia.headiconPkSprites[playerEntity.skullId];
                            drawY -= skullSprite.getHeight();
                            skullSprite.draw((int) (client.HINT_POSITION[0] + clipLeft - 12.0F), drawY);
                            drawY -= 2;
                        }
                        if (playerEntity.prayerHeadIconId != -1) {
                        	NativeSprite prayerSprite = StaticMedia.headiconPrayerSprites[playerEntity.prayerHeadIconId];
                            drawY -= prayerSprite.getHeight();
                            prayerSprite.draw((int) (client.HINT_POSITION[0] + clipLeft - 12.0F), drawY);
                            drawY -= 2;
                        }
                    } else if (npctype.headIcons >= 0 && npctype.headIcons < StaticMedia.headiconPrayerSprites.length) {
                        NativeSprite nativesprite120 = StaticMedia.headiconPrayerSprites[npctype.headIcons];
                        drawY -= nativesprite120.getHeight();
                        nativesprite120.draw((int) (clipLeft + client.HINT_POSITION[0] - (nativesprite120.getWidth() >> 1)), drawY);
                        drawY -= 2;
                    }
                }
                int i_10000;
                if (entity instanceof PlayerEntity) {
                    if (id >= 0) {
                        int i_15 = 0;
                        for (int index = 0; index < PlayerUpdate.HINT_ARROWS.length; index++) {
                        	HintArrow arrow = PlayerUpdate.HINT_ARROWS[index];
                            if (arrow != null && arrow.targetType == 10 && arrow.targetIndex == playerIndices[id]) {
                                NativeSprite playerArrow = StaticMedia.hintHeadiconSprites[arrow.arrowSprite];
                                if (playerArrow.getHeight() > i_15) {
                                    i_15 = playerArrow.getHeight();
                                }
                                playerArrow.draw((int) (clipLeft + client.HINT_POSITION[0] - 12.0F), drawY - playerArrow.getHeight());
                            }
                        }
                        if (i_15 > 0) {
                            i_10000 = drawY - (i_15 + 2);
                        }
                    }
                } else {
                    int i_15 = 0;
                    for (int index = 0; index < PlayerUpdate.HINT_ARROWS.length; index++) {
                    	HintArrow arrow = PlayerUpdate.HINT_ARROWS[index];
                        if (arrow != null && arrow.targetType == 1 && client.NPC_UPDATE_INDICES[id - playerIndiceCount] == arrow.targetIndex) {
                            NativeSprite npcArrow = StaticMedia.hintHeadiconSprites[arrow.arrowSprite];
                            if (npcArrow.getHeight() > i_15) {
                                i_15 = npcArrow.getHeight();
                            }
                            boolean shouldDraw;
                            if (arrow.denominator == 0) {
                                shouldDraw = true;
                            } else {
                                int i_21 = GameShell.gameCyclesPerSecond() * 1000 / arrow.denominator / 2;
                                shouldDraw = client.cycles % (i_21 * 2) < i_21;
                            }
                            if (shouldDraw) {
                                npcArrow.draw((int) (clipLeft + client.HINT_POSITION[0] - 12.0F), drawY - npcArrow.getHeight());
                            }
                        }
                    }
                    if (i_15 > 0) {
                        i_10000 = drawY - (i_15 + 2);
                    }
                }
                for (int hitsplatIndex = 0; hitsplatIndex < client.ENTITY_DEFAULTS.maximumHits; hitsplatIndex++) {
                    int cycle = ((PathingEntity) entity).hitsplatCycles[hitsplatIndex];
                    int hitplatId = ((PathingEntity) entity).activeHitsplatIds[hitsplatIndex];
                    HitSplatType hitsplatType = null;
                    int cyclesVisible = 0;
                    if (hitplatId >= 0) {
                        if (cycle <= client.cycles) {
                            continue;
                        }
                        hitsplatType = TypeLists.HITSPLAT.list(((PathingEntity) entity).activeHitsplatIds[hitsplatIndex]);
                        cyclesVisible = hitsplatType.cyclesVisible;
                    } else if (cycle < 0) {
                        continue;
                    }
                    int soakHitsplatId = ((PathingEntity) entity).soakHitsplatIds[hitsplatIndex];
                    HitSplatType soakHitsplat = null;
                    if (soakHitsplatId >= 0) {
                        soakHitsplat = TypeLists.HITSPLAT.list(soakHitsplatId);
                    }
                    if (cycle - cyclesVisible <= client.cycles) {
                        if (hitsplatType == null) {
                            ((PathingEntity) entity).hitsplatCycles[hitsplatIndex] = -1;
                        } else {
                            int i_116 = ((PathingEntity) entity).getIconHeight() / 2;
                            HintRenderer.calculateHintForSceneobject((SceneGraphEntity) entity, i_116);
                            if (client.HINT_POSITION[0] > -1.0f) {
                                client.HINT_POSITION[0] += client.ENTITY_DEFAULTS.hitOffsetsX[hitsplatIndex];
                                client.HINT_POSITION[1] += client.ENTITY_DEFAULTS.hitOffsetsY[hitsplatIndex];
                                int i_27 = 0;
                                int i_28 = 0;
                                int i_29 = 0;
                                int i_30 = 0;
                                int i_31 = 0;
                                int i_32 = 0;
                                int i_33 = 0;
                                int i_34 = 0;
                                NativeSprite nativesprite35 = null;
                                NativeSprite nativesprite36 = null;
                                NativeSprite nativesprite37 = null;
                                NativeSprite nativesprite38 = null;
                                int i_39 = 0;
                                int i_40 = 0;
                                int i_41 = 0;
                                int i_42 = 0;
                                int i_43 = 0;
                                int i_44 = 0;
                                int i_45 = 0;
                                int i_46 = 0;
                                int i_47 = 0;
                                NativeSprite nativesprite48 = hitsplatType.method3835(Renderers.activeToolkit);
                                if (nativesprite48 != null) {
                                    i_27 = nativesprite48.getWidth();
                                    int i_49 = nativesprite48.getHeight();
                                    if (i_49 > i_47) {
                                        i_47 = i_49;
                                    }
                                    nativesprite48.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                    i_31 = client.HITSPLAT_SIZE_CACHE[0];
                                }
                                NativeSprite nativesprite106 = hitsplatType.method3836(Renderers.activeToolkit);
                                if (nativesprite106 != null) {
                                    i_28 = nativesprite106.getWidth();
                                    int i_50 = nativesprite106.getHeight();
                                    if (i_50 > i_47) {
                                        i_47 = i_50;
                                    }
                                    nativesprite106.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                    i_32 = client.HITSPLAT_SIZE_CACHE[0];
                                }
                                NativeSprite nativesprite107 = hitsplatType.method3832(Renderers.activeToolkit);
                                if (nativesprite107 != null) {
                                    i_29 = nativesprite107.getWidth();
                                    int i_51 = nativesprite107.getHeight();
                                    if (i_51 > i_47) {
                                        i_47 = i_51;
                                    }
                                    nativesprite107.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                    i_33 = client.HITSPLAT_SIZE_CACHE[0];
                                }
                                NativeSprite nativesprite108 = hitsplatType.method3838(Renderers.activeToolkit);
                                int i_52;
                                if (nativesprite108 != null) {
                                    i_30 = nativesprite108.getWidth();
                                    i_52 = nativesprite108.getHeight();
                                    if (i_52 > i_47) {
                                        i_47 = i_52;
                                    }
                                    nativesprite108.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                    i_34 = client.HITSPLAT_SIZE_CACHE[0];
                                }
                                if (soakHitsplat != null) {
                                    nativesprite35 = soakHitsplat.method3835(Renderers.activeToolkit);
                                    if (nativesprite35 != null) {
                                        i_39 = nativesprite35.getWidth();
                                        i_52 = nativesprite35.getHeight();
                                        if (i_52 > i_47) {
                                            i_47 = i_52;
                                        }
                                        nativesprite35.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                        i_43 = client.HITSPLAT_SIZE_CACHE[0];
                                    }
                                    nativesprite36 = soakHitsplat.method3836(Renderers.activeToolkit);
                                    if (nativesprite36 != null) {
                                        i_40 = nativesprite36.getWidth();
                                        i_52 = nativesprite36.getHeight();
                                        if (i_52 > i_47) {
                                            i_47 = i_52;
                                        }
                                        nativesprite36.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                        i_44 = client.HITSPLAT_SIZE_CACHE[0];
                                    }
                                    nativesprite37 = soakHitsplat.method3832(Renderers.activeToolkit);
                                    if (nativesprite37 != null) {
                                        i_41 = nativesprite37.getWidth();
                                        i_52 = nativesprite37.getHeight();
                                        if (i_52 > i_47) {
                                            i_47 = i_52;
                                        }
                                        nativesprite37.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                        i_45 = client.HITSPLAT_SIZE_CACHE[0];
                                    }
                                    nativesprite38 = soakHitsplat.method3838(Renderers.activeToolkit);
                                    if (nativesprite38 != null) {
                                        i_42 = nativesprite38.getWidth();
                                        i_52 = nativesprite38.getHeight();
                                        if (i_52 > i_47) {
                                            i_47 = i_52;
                                        }
                                        nativesprite38.cacheSizeInto(client.HITSPLAT_SIZE_CACHE);
                                        i_46 = client.HITSPLAT_SIZE_CACHE[0];
                                    }
                                }
                                AbstractFontRenderer fontrenderer109 = Renderers.FONT_RENDERER;
                                AbstractFontRenderer soakFont = Renderers.FONT_RENDERER;
                                FontMetrics fontmetrics54 = FontManager.worldMapAreaFont;
                                FontMetrics fontmetrics55 = FontManager.worldMapAreaFont;
                                int fontId = hitsplatType.fontId;
                                AbstractFontRenderer fontrenderer57;
                                FontMetrics fontmetrics58;
                                if (fontId >= 0) {
                                    fontrenderer57 = (AbstractFontRenderer) StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, fontId, true, hitsplatType.hasColor);
                                    fontmetrics58 = StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, fontId);
                                    if (fontrenderer57 != null && fontmetrics58 != null) {
                                        fontrenderer109 = fontrenderer57;
                                        fontmetrics54 = fontmetrics58;
                                    }
                                }
                                if (soakHitsplat != null) {
                                    fontId = soakHitsplat.fontId;
                                    if (fontId >= 0) {
                                        fontrenderer57 = (AbstractFontRenderer) StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, fontId, true, soakHitsplat.hasColor);
                                        fontmetrics58 = StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, fontId);
                                        if (fontrenderer57 != null && fontmetrics58 != null) {
                                            soakFont = fontrenderer57;
                                            fontmetrics55 = fontmetrics58;
                                        }
                                    }
                                }
                                fontrenderer57 = null;
                                String soakText = null;
                                boolean bool59 = false;
                                int soakWidth = 0;
                                String hitsplatText = hitsplatType.getCenteredText(((PathingEntity) entity).hitsplatText[hitsplatIndex]);
                                int regularWidth = fontmetrics54.getWidthNoSprites(hitsplatText);
                                if (soakHitsplat != null) {
                                    soakText = soakHitsplat.getCenteredText(((PathingEntity) entity).soakHitsplatText[hitsplatIndex]);
                                    soakWidth = fontmetrics55.getWidthNoSprites(soakText);
                                }
                                int i_63 = 0;
                                int i_64 = 0;
                                if (i_28 > 0) {
                                    i_63 = regularWidth / i_28 + 1;
                                }
                                if (soakHitsplat != null && i_40 > 0) {
                                    i_64 = soakWidth / i_40 + 1;
                                }
                                int i_65 = 0;
                                int i_66 = i_65;
                                if (i_27 > 0) {
                                    i_65 += i_27;
                                }
                                i_65 += 2;
                                int i_67 = i_65;
                                if (i_29 > 0) {
                                    i_65 += i_29;
                                }
                                int i_68 = i_65;
                                int i_69 = i_65;
                                int i_70;
                                if (i_28 > 0) {
                                    i_70 = i_28 * i_63;
                                    i_65 += i_70;
                                    i_69 += (i_70 - regularWidth) / 2;
                                } else {
                                    i_65 += regularWidth;
                                }
                                i_70 = i_65;
                                if (i_30 > 0) {
                                    i_65 += i_30;
                                }
                                int i_71 = 0;
                                int i_72 = 0;
                                int i_73 = 0;
                                int i_74 = 0;
                                int i_75 = 0;
                                int i_76;
                                if (soakHitsplat != null) {
                                    i_65 += 2;
                                    i_71 = i_65;
                                    if (i_39 > 0) {
                                        i_65 += i_39;
                                    }
                                    i_65 += 2;
                                    i_72 = i_65;
                                    if (i_41 > 0) {
                                        i_65 += i_41;
                                    }
                                    i_73 = i_65;
                                    i_75 = i_65;
                                    if (i_40 > 0) {
                                        i_76 = i_64 * i_40;
                                        i_65 += i_76;
                                        i_75 += (i_76 - soakWidth) / 2;
                                    } else {
                                        i_65 += soakWidth;
                                    }
                                    i_74 = i_65;
                                    if (i_42 > 0) {
                                        i_65 += i_42;
                                    }
                                }
                                i_76 = ((PathingEntity) entity).hitsplatCycles[hitsplatIndex] - client.cycles;
                                int i_77 = hitsplatType.anInt2846 - i_76 * hitsplatType.anInt2846 / hitsplatType.cyclesVisible;
                                int i_78 = i_76 * hitsplatType.anInt2833 / hitsplatType.cyclesVisible + -hitsplatType.anInt2833;
                                int i_79 = (int) (clipLeft + client.HINT_POSITION[0] - (i_65 >> 1) + i_77);
                                int i_80 = (int) (i_78 + (client.HINT_POSITION[1] + clipTop - 12.0F));
                                int i_81 = i_80;
                                int i_82 = i_80 + i_47;
                                int i_83 = i_80 + hitsplatType.anInt2832 + 15;
                                int i_84 = i_83 - fontmetrics54.topPadding;
                                int i_85 = i_83 + fontmetrics54.bottomPadding;
                                if (i_84 < i_80) {
                                    i_81 = i_84;
                                }
                                if (i_85 > i_82) {
                                    i_82 = i_85;
                                }
                                int i_86 = 0;
                                int i_87;
                                int i_88;
                                if (soakHitsplat != null) {
                                    i_86 = i_80 + soakHitsplat.anInt2832 + 15;
                                    i_87 = i_86 - fontmetrics55.topPadding;
                                    i_88 = i_86 + fontmetrics55.bottomPadding;
                                    if (i_87 < i_81) {
                                    }
                                    if (i_88 > i_82) {
                                    }
                                }
                                i_87 = 255;
                                if (hitsplatType.anInt2847 >= 0) {
                                    i_87 = (i_76 << 8) / (hitsplatType.cyclesVisible - hitsplatType.anInt2847);
                                }
                                if (i_87 >= 0 && i_87 < 255) {
                                    int alpha = i_87 << 24;
                                    int i_89 = alpha | 0xffffff;
                                    if (nativesprite48 != null) {
                                        nativesprite48.draw(i_66 + i_79 - i_31, i_80, 0, i_89, 1);
                                    }
                                    if (nativesprite107 != null) {
                                        nativesprite107.draw(i_67 + i_79 - i_33, i_80, 0, i_89, 1);
                                    }
                                    int i_90;
                                    if (nativesprite106 != null) {
                                        for (i_90 = 0; i_90 < i_63; i_90++) {
                                            nativesprite106.draw(i_90 * i_28 + (i_68 + i_79 - i_32), i_80, 0, i_89, 1);
                                        }
                                    }
                                    if (nativesprite108 != null) {
                                        nativesprite108.draw(i_70 + i_79 - i_34, i_80, 0, i_89, 1);
                                    }
                                    fontrenderer109.renderPlain(hitsplatText, i_79 + i_69, i_83, hitsplatType.color | alpha, 0);
                                    if (soakHitsplat != null) {
                                        if (nativesprite35 != null) {
                                            nativesprite35.draw(i_79 + i_71 - i_43, i_80, 0, i_89, 1);
                                        }
                                        if (nativesprite37 != null) {
                                            nativesprite37.draw(i_79 + i_72 - i_45, i_80, 0, i_89, 1);
                                        }
                                        if (nativesprite36 != null) {
                                            for (i_90 = 0; i_90 < i_64; i_90++) {
                                                nativesprite36.draw(i_90 * i_40 + (i_73 + i_79 - i_44), i_80, 0, i_89, 1);
                                            }
                                        }
                                        if (nativesprite38 != null) {
                                            nativesprite38.draw(i_79 + i_74 - i_46, i_80, 0, i_89, 1);
                                        }
                                        soakFont.renderPlain(soakText, i_75 + i_79, i_86, soakHitsplat.color | alpha, 0);
                                    }
                                } else {
                                    if (nativesprite48 != null) {
                                        nativesprite48.draw(i_79 + i_66 - i_31, i_80);
                                    }
                                    if (nativesprite107 != null) {
                                        nativesprite107.draw(i_67 + i_79 - i_33, i_80);
                                    }
                                    if (nativesprite106 != null) {
                                        for (int i = 0; i < i_63; i++) {
                                            nativesprite106.draw(i * i_28 + (i_68 + i_79 - i_32), i_80);
                                        }
                                    }
                                    if (nativesprite108 != null) {
                                        nativesprite108.draw(i_70 + i_79 - i_34, i_80);
                                    }
                                    fontrenderer109.renderPlain(hitsplatText, i_79 + i_69, i_83, hitsplatType.color | -16777216, 0);
                                    if (soakHitsplat != null) {
                                        if (nativesprite35 != null) {
                                            nativesprite35.draw(i_79 + i_71 - i_43, i_80);
                                        }
                                        if (nativesprite37 != null) {
                                            nativesprite37.draw(i_79 + i_72 - i_45, i_80);
                                        }
                                        if (nativesprite36 != null) {
                                            for (int i = 0; i < i_64; i++) {
                                                nativesprite36.draw(i_40 * i + (i_79 + i_73 - i_44), i_80);
                                            }
                                        }
                                        if (nativesprite38 != null) {
                                            nativesprite38.draw(i_74 + i_79 - i_46, i_80);
                                        }
                                        soakFont.renderPlain(soakText, i_79 + i_75, i_86, soakHitsplat.color | -16777216, 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int index = 0; index < client.anInt7304; index++) {
            int ptr = client.ENTITY_INDEX_REFERENCES[index];
            if (ptr < 2048) {
                entity = PlayerUpdate.players[ptr];
            } else {
                entity = (client.NPC_MAP.get(ptr - 2048)).value;
            }
            int i_92 = client.NPC_INDEX_REFERENCES[index];
            Object obj14;
            if (i_92 < 2048) {
                obj14 = PlayerUpdate.players[i_92];
            } else {
                obj14 = (client.NPC_MAP.get(i_92 - 2048)).value;
            }
            method7140((PathingEntity) entity, (PathingEntity) obj14, --((PathingEntity) entity).anInt10319, clipLeft, clipTop);
        }
        int padding = FontMetrics.aClass4143438.bottomPadding + FontMetrics.aClass4143438.topPadding + 2;
        for (int hintIndex = 0; hintIndex < client.currentHintPtr; hintIndex++) {
            int hintX = client.HINT_X[hintIndex];
            int hintY = client.HINT_Y[hintIndex];
            int hintWidth = client.HINT_WIDTH[hintIndex];
            boolean loop = true;
            while (loop) {
                loop = false;
                for (int index = 0; index < hintIndex; index++) {
                    if (hintY + 2 > client.HINT_Y[index] - padding && hintY - padding < client.HINT_Y[index] + 2 && hintX - hintWidth < client.HINT_WIDTH[index] + client.HINT_X[index] && hintX + hintWidth > client.HINT_X[index] - client.HINT_WIDTH[index] && client.HINT_Y[index] - padding < hintY) {
                        hintY = client.HINT_Y[index] - padding;
                        loop = true;
                    }
                }
            }
            client.HINT_Y[hintIndex] = hintY;
            String drawnText = client.HINT_DETAILS[hintIndex].getText();
            if (client.anInt7351 == 0) {
                int color = 16776960;
                int i_18 = client.HINT_DETAILS[hintIndex].getColorType();
                if (i_18 < 6) {
                    color = client.anIntArray7148[i_18];
                }
                if (i_18 == 6) {
                    color = client.timesSceneRendered % 20 < 10 ? 16711680 : 16776960;
                }
                if (i_18 == 7) {
                    color = client.timesSceneRendered % 20 < 10 ? 255 : 65535;
                }
                if (i_18 == 8) {
                    color = client.timesSceneRendered % 20 < 10 ? 45056 : 8454016;
                }
                if (i_18 == 9) {
                    int i_19 = 150 - client.HINT_DETAILS[hintIndex].getTicksLeft() * 150 / client.HINT_DETAILS[hintIndex].getTotalTicksVisible();
                    if (i_19 < 50) {
                        color = i_19 * 1280 + 16711680;
                    } else if (i_19 < 100) {
                        color = 16776960 - (i_19 - 50) * 327680;
                    } else if (i_19 < 150) {
                        color = (i_19 - 100) * 5 + 65280;
                    }
                }
                if (i_18 == 10) {
                    int i_19 = 150 - client.HINT_DETAILS[hintIndex].getTicksLeft() * 150 / client.HINT_DETAILS[hintIndex].getTotalTicksVisible();
                    if (i_19 < 50) {
                        color = i_19 * 5 + 16711680;
                    } else if (i_19 < 100) {
                        color = 16711935 - (i_19 - 50) * 327680;
                    } else if (i_19 < 150) {
                        color = (i_19 - 100) * 327680 + 255 - (i_19 - 100) * 5;
                    }
                }
                if (i_18 == 11) {
                    int i_19 = 150 - client.HINT_DETAILS[hintIndex].getTicksLeft() * 150 / client.HINT_DETAILS[hintIndex].getTotalTicksVisible();
                    if (i_19 < 50) {
                        color = 16777215 - i_19 * 327685;
                    } else if (i_19 < 100) {
                        color = (i_19 - 50) * 327685 + 65280;
                    } else if (i_19 < 150) {
                        color = 16777215 - (i_19 - 100) * 327680;
                    }
                }
                int drawColor = color | -16777216;
                int drawType = client.HINT_DETAILS[hintIndex].getDrawType();
                if (drawType == 0) {
                	// All regular text: no effects / plain color effects (red:) / cyan / flash1 / flash2 / flash3 / glow1 / glow2 / glow3
                    MiniMenu.minimenuHeader.renderCentered(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, -16777216);
                }
                if (drawType == 1) {
                	// Wave effect (wave:texthere)
                    MiniMenu.minimenuHeader.renderCenteredWave(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, client.timesSceneRendered);
                }
                if (drawType == 2) {
                	// Wave2 effect (wave2:texthere)
                    MiniMenu.minimenuHeader.renderCenteredWave2(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, client.timesSceneRendered);
                }
                if (drawType == 3) {
                	// Shake effect (shake:texthere)
                    int i_21 = 150 - client.HINT_DETAILS[hintIndex].getTicksLeft() * 150 / client.HINT_DETAILS[hintIndex].getTotalTicksVisible();
                    MiniMenu.minimenuHeader.renderCenteredShake(drawnText, clipLeft + hintX, hintY + clipTop, drawColor, client.timesSceneRendered, i_21);
                }
                if (drawType == 4) {
                	// Scroll effect (scroll:texthere)
                    int i_21 = 150 - client.HINT_DETAILS[hintIndex].getTicksLeft() * 150 / client.HINT_DETAILS[hintIndex].getTotalTicksVisible();
                    int i_116 = i_21 * (FontMetrics.aClass4143438.getWidthNoSprites(drawnText) + 100) / 150;
                    Renderers.activeToolkit.expandClipping(clipLeft + hintX - 50, clipTop, clipLeft + hintX + 50, height + clipTop);
                    MiniMenu.minimenuHeader.renderPlain(drawnText, clipLeft + hintX + 50 - i_116, hintY + clipTop, drawColor, -16777216);
                    Renderers.activeToolkit.setClipping(clipLeft, clipTop, clipLeft + width, height + clipTop);
                }
                if (drawType == 5) {
                	// Slide effect (slide:texthere)
                    int i_21 = 150 - client.HINT_DETAILS[hintIndex].getTicksLeft() * 150 / client.HINT_DETAILS[hintIndex].getTotalTicksVisible();
                    int i_116 = 0;
                    if (i_21 < 25) {
                        i_116 = i_21 - 25;
                    } else if (i_21 > 125) {
                        i_116 = i_21 - 125;
                    }
                    int paddingY = FontMetrics.aClass4143438.topPadding + FontMetrics.aClass4143438.bottomPadding;
                    Renderers.activeToolkit.expandClipping(clipLeft, hintY + clipTop - paddingY - 1, clipLeft + width, hintY + clipTop + 5);
                    MiniMenu.minimenuHeader.renderCentered(drawnText, clipLeft + hintX, i_116 + hintY + clipTop, drawColor, -16777216);
                    Renderers.activeToolkit.setClipping(clipLeft, clipTop, clipLeft + width, height + clipTop);
                }
            } else {
                MiniMenu.minimenuHeader.renderCentered(drawnText, clipLeft + hintX, hintY + clipTop, -256, -16777216);
            }
        }
    }

    public static int turnTo(PathingEntity current) {
        if (current.turnDirection == 0) {
            return 0;
        } 
        if (current.faceEntity != -1) {
            Object facingEntity = null;
            if (current.faceEntity < 32768) {
                ObjectNode facingNpc = client.NPC_MAP.get(current.faceEntity);
                if (facingNpc != null) {
                    facingEntity = facingNpc.value;
                }
            } else {
                facingEntity = PlayerUpdate.players[current.faceEntity - 32768];
            }
            if (facingEntity != null) {
            	Vector3 delta = Vector3Pool.delta(current.getAbsolutePosition().coords, ((PathingEntity) facingEntity).getAbsolutePosition().coords);
                int deltaX = (int) delta.x;
                int deltaZ = (int) delta.z;
                if (deltaX != 0 || deltaZ != 0) {
                    current.turn((int) (Math.atan2(deltaX, deltaZ) * 2607.5945876176133D) & 0x3fff);
                }
            }
        }
        if (current instanceof PlayerEntity player) {
            if (player.faceDirection != -1 && (player.movementStepsLeft == 0 || player.anInt10366 > 0)) {
                player.turn(player.faceDirection);
                player.faceDirection = -1;
            }
        } else if (current instanceof NpcEntity npc) {
            if (npc.faceX != -1 && (npc.movementStepsLeft == 0 || npc.anInt10366 > 0)) {
            	Vector3 abs = npc.getAbsolutePosition().coords;
                CoordGrid base = Resource.mapRegion.getBaseCoordinates();
                int deltaX = (int) abs.x - (npc.faceX * 256 - base.x * 256 - base.x * 256);
                int deltaZ = (int) abs.z - (npc.faceY * 256 - base.y * 256 - base.y * 256);
                if (deltaX != 0 || deltaZ != 0) {
                    npc.turn((int) (Math.atan2(deltaX, deltaZ) * 2607.5945876176133D) & 0x3fff);
                }
                npc.faceX = -1;
            }
        }
        return current.setFacingGradual();
    }

    public static void method7140(PathingEntity pathingEntity, PathingEntity pathingNpc, int i_2, int clipLeft, int clipTop) {
        int overheadSpriteId = pathingNpc.getOverheadSprite();
        if (overheadSpriteId != -1) {
            NativeSprite overheadSprites = (NativeSprite) client.OVERHEAD_SPRITES.get(overheadSpriteId);
            if (overheadSprites == null) {
                SpriteType[] overheadSpriteTypes = SpriteType.decodeGroup(Resource.SPRITES, overheadSpriteId);
                if (overheadSpriteTypes == null) {
                    return;
                }
                overheadSprites = Renderers.activeToolkit.generateSprite(overheadSpriteTypes[0], true);
                client.OVERHEAD_SPRITES.put(overheadSprites, overheadSpriteId);
            }
            Vector3 abs = pathingEntity.getAbsolutePosition().coords;
            HintRenderer.calculateHintArrowPlacement(pathingEntity.plane, (int) abs.x, (int) abs.z, pathingEntity.getSize() * 256, 0, false);
            int drawX = (int) (clipLeft + client.HINT_POSITION[0] - 18.0F);
            int drawY = (int) (clipTop + client.HINT_POSITION[1] - 16.0F - 54.0F);
            drawX += i_2 / 4 * 18;
            drawY += i_2 % 4 * 18;
            overheadSprites.draw(drawX, drawY);
            if (pathingNpc == pathingEntity) {
				Renderers.activeToolkit.drawOpaqueRectangle(drawX - 1, drawY - 1, 18, 18, -256);
            }
            EntityOverhead class275Sub215 = createEntityOverhead();
            class275Sub215.pathingEntity = pathingNpc;
            class275Sub215.startX = drawX;
            class275Sub215.startY = drawY;
            class275Sub215.endX = drawX + 16;
            class275Sub215.endY = drawY + 16;
            client.ENTITY_OVERHEADS.offer(class275Sub215);
        }
    }

    public static EntityOverhead createEntityOverhead() {
        EntityOverhead overhead = (EntityOverhead) EntityUpdating.aClass4577745.get();
        if (overhead != null) {
            --EntityUpdating.anInt7741;
            return overhead;
        } 
        return new EntityOverhead();        
    }
    
	public static void method8695(EntityOverhead class275Sub20) {
	    class275Sub20.pathingEntity = null;
	    if (anInt7741 < 20) {
	        aClass4577745.offer(class275Sub20);
	        ++anInt7741;
	    }
	}

	public static void method14519() {
	    for (EntityOverhead overhead = (EntityOverhead) client.ENTITY_OVERHEADS.get(); overhead != null; overhead = (EntityOverhead) client.ENTITY_OVERHEADS.get()) {
	        method8695(overhead);
	    }
	    byte fromPlane = 0;
	    byte toPlane = 3;
	    int plane;
	    if (client.MAP_LOADING_STAGE == 4) {
	        for (plane = fromPlane; plane <= toPlane; plane++) {
	            PlayerUpdate.method11629(plane);
	        }
	    } else {
	        PlayerUpdate.method11626();
	        for (plane = fromPlane; plane <= toPlane; plane++) {
	            MapRegion.resetDrawPriorities();
	            PlayerUpdate.updateDrawPriority(plane);
	            PlayerUpdate.method11629(plane);
	        }
	        client.method11750();
	    }
	    PlayerUpdate.method11619();
	}

    public static void updatePosition(PathingEntity entity, boolean cutscene) {
	    BasType bas = entity.getBastype();
	    if (entity.movementStepsLeft == 0) {
	        entity.anInt10366 = 0;
	        EntityUpdating.currentMovementType = MovementType.STATIONARY.id;
	        EntityUpdating.turnDirectionIndex = 0;
	    } else {
	        if (entity.currentSequence.hasType() && !entity.currentSequence.hasSpeed()) {
	            SeqType animationdefinitions4 = entity.currentSequence.getType();
	            if (entity.previousMovementStepsLeft > 0 && animationdefinitions4.interruptType == 0) {
	                ++entity.anInt10366;
	                EntityUpdating.currentMovementType = MovementType.STATIONARY.id;
	                EntityUpdating.turnDirectionIndex = 0;
	                return;
	            }
	            if (entity.previousMovementStepsLeft <= 0 && animationdefinitions4.moveType == 0) {
	                ++entity.anInt10366;
	                EntityUpdating.currentMovementType = MovementType.STATIONARY.id;
	                EntityUpdating.turnDirectionIndex = 0;
	                return;
	            }
	        }
	        for (int index = 0; index < entity.spotAnimations.length; index++) {
	            if (entity.spotAnimations[index].spotAnimationId != -1 && entity.spotAnimations[index].sequence.hasSpeed()) {
	                SpotAnimType spotanimtype = TypeLists.SPOT_ANIM.list(entity.spotAnimations[index].spotAnimationId);
	                if (spotanimtype.replay && spotanimtype.sequenceId != -1) {
	                    SeqType seqtype = TypeLists.SEQ.list(spotanimtype.sequenceId);
	                    if (entity.previousMovementStepsLeft > 0 && seqtype.interruptType == 0) {
	                        entity.anInt10366++;
	                        EntityUpdating.currentMovementType = MovementType.STATIONARY.id;
	                        EntityUpdating.turnDirectionIndex = 0;
	                        return;
	                    }
	                    if (entity.previousMovementStepsLeft <= 0 && seqtype.moveType == 0) {
	                        entity.anInt10366++;
	                        EntityUpdating.currentMovementType = MovementType.STATIONARY.id;
	                        EntityUpdating.turnDirectionIndex = 0;
	                        return;
	                    }
	                }
	            }
	        }
	        Vector3 abs = Vector3Pool.vector3From(entity.getAbsolutePosition().coords);
	        int startX = (int) abs.x;
	        int startZ = (int) abs.z;
	        int endX = 512 * entity.movementBaseX[entity.movementStepsLeft - 1] + entity.getSize() * 256;
	        int endZ = 512 * entity.movementBaseY[entity.movementStepsLeft - 1] + entity.getSize() * 256;
	        if (startX < endX) {
	            if (startZ < endZ) {
	                entity.turn(10240);
	            } else if (startZ > endZ) {
	                entity.turn(14336);
	            } else {
	                entity.turn(12288);
	            }
	        } else if (startX > endX) {
	            if (startZ < endZ) {
	                entity.turn(6144);
	            } else if (startZ > endZ) {
	                entity.turn(2048);
	            } else {
	                entity.turn(4096);
	            }
	        } else if (startZ < endZ) {
	            entity.turn(8192);
	        } else if (startZ > endZ) {
	            entity.turn(0);
	        }
	        byte moveType = entity.movementTypes[entity.movementStepsLeft - 1];
	        if (cutscene || endX - startX <= 1024 && endX - startX >= -1024 && endZ - startZ <= 1024 && endZ - startZ >= -1024) {
	            int positionDelta = 16;
	            boolean delayMovement = true;
	            if (entity instanceof NpcEntity) {
	                delayMovement = ((NpcEntity) entity).definitions.delayMovement;
	            }
	            if (delayMovement) {
	                int yawRotationDelta = entity.lastYawRotation - entity.yaw.rotation;
	                if (yawRotationDelta != 0 && entity.faceEntity == -1 && entity.turnDirection != 0) {
	                    positionDelta = 8;
	                }
	                if (!cutscene && entity.movementStepsLeft > 2) {
	                    positionDelta = 24;
	                }
	                if (!cutscene && entity.movementStepsLeft > 3) {
	                    positionDelta = 32;
	                }
	            } else {
	                if (!cutscene && entity.movementStepsLeft > 1) {
	                    positionDelta = 24;
	                }
	                if (!cutscene && entity.movementStepsLeft > 2) {
	                    positionDelta = 32;
	                }
	            }
	            if (entity.anInt10366 > 0 && entity.movementStepsLeft > 1) {
	                positionDelta = 32;
	                entity.anInt10366--;
	            }
	            if (moveType == MovementType.RUNNING.id) {
	                positionDelta <<= 1;
	            } else if (moveType == MovementType.HALF_WALK.id) {
	                positionDelta >>= 1;
	            }
	            // Is always -1
	            if (bas.unused != -1) { 
	                positionDelta <<= 9;
	                if (entity.movementStepsLeft == 1) {
	                    int i_15 = entity.anInt10368 * entity.anInt10368;
	                    int sizeX = ((int) abs.x > endX ? (int) abs.x - endX : endX - (int) abs.x) << 9;
	                    int sizeZ = ((int) abs.z > endZ ? (int) abs.z - endZ : endZ - (int) abs.z) << 9;
	                    int maxSize = Math.max(sizeX, sizeZ);
	                    int i_16 = maxSize * bas.unused * 2;
	                    if (i_15 > i_16) {
	                        entity.anInt10368 /= 2;
	                    } else if (i_15 / 2 > maxSize) {
	                        entity.anInt10368 -= bas.unused;
	                        if (entity.anInt10368 < 0) {
	                            entity.anInt10368 = 0;
	                        }
	                    } else if (entity.anInt10368 < positionDelta) {
	                        entity.anInt10368 += bas.unused;
	                        if (entity.anInt10368 > positionDelta) {
	                            entity.anInt10368 = positionDelta;
	                        }
	                    }
	                } else if (entity.anInt10368 < positionDelta) {
	                    entity.anInt10368 += bas.unused;
	                    if (entity.anInt10368 > positionDelta) {
	                        entity.anInt10368 = positionDelta;
	                    }
	                } else {
	                    entity.anInt10368 -= bas.unused;
	                    if (entity.anInt10368 < 0) {
	                        entity.anInt10368 = 0;
	                    }
	                }
	                positionDelta = entity.anInt10368 >> 9;
	                if (positionDelta < 1) {
	                    positionDelta = 1;
	                }
	            }
	            EntityUpdating.turnDirectionIndex = 0;
	            if (endX == startX && endZ == startZ) {
	                EntityUpdating.currentMovementType = MovementType.STATIONARY.id;
	            } else {
	                if (startX < endX) {
	                    abs.x += positionDelta;
	                    EntityUpdating.turnDirectionIndex |= 0x4;
	                    if (abs.x > endX) {
	                        abs.x = endX;
	                    }
	                } else if (startX > endX) {
	                    abs.x -= positionDelta;
	                    EntityUpdating.turnDirectionIndex |= 0x8;
	                    if (abs.x < endX) {
	                        abs.x = endX;
	                    }
	                }
	                if (startZ < endZ) {
	                    abs.z += positionDelta;
	                    EntityUpdating.turnDirectionIndex |= 0x1;
	                    if (abs.z > endZ) {
	                        abs.z = endZ;
	                    }
	                } else if (startZ > endZ) {
	                    abs.z -= positionDelta;
	                    EntityUpdating.turnDirectionIndex |= 0x2;
	                    if (abs.z < endZ) {
	                        abs.z = endZ;
	                    }
	                }
	                entity.setBasePosition(abs);
	                if (positionDelta >= 32) {
	                    EntityUpdating.currentMovementType = MovementType.RUNNING.id;
	                } else {
	                    EntityUpdating.currentMovementType = moveType;
	                }
	            }
	            if (endX == (int) abs.x && endZ == (int) abs.z) {
	                entity.movementStepsLeft--;
	                if (entity.previousMovementStepsLeft > 0) {
	                    entity.previousMovementStepsLeft--;
	                }
	            }
	        } else {
	            entity.setBasePosition(endX, abs.y, endZ);
	            entity.turn(entity.lastYawRotation, false);
	            entity.movementStepsLeft--;
	            if (entity.previousMovementStepsLeft > 0) {
	                entity.previousMovementStepsLeft--;
	            }
	            EntityUpdating.currentMovementType = MovementType.STATIONARY.id;
	            EntityUpdating.turnDirectionIndex = 0;
	        }
	        abs.pushVectorStack();
	    }
	}

	public static void rotateEquipment(PathingEntity current) {
	    if (current.equipmentUpdateDetails != null || current.equipmentRotations != null) {
	        boolean reset = true;
	        CoordGrid base = Resource.mapRegion.getBaseCoordinates();
	        for (int equipIndex = 0; equipIndex < current.equipmentUpdateDetails.length; equipIndex++) {
	            int bitpacked = -1;
	            bitpacked = current.equipmentUpdateDetails[equipIndex];
	            if (bitpacked == -1) {
	                if (!current.rotateEquipment(equipIndex, -1)) {
	                    reset = false;
	                }
	            } else {
	                reset = false;
	                Vector3 currentAbsolutePosition = current.getAbsolutePosition().coords;
	                int deltaX;
	                int deltaZ;
	                if ((bitpacked & -1073741824) == -1073741824) {
	                    int position = bitpacked & 0xfffffff;
	                    int absX = position >> 14;
	                    int absY = position & 0x3fff;
	                    deltaX = (int) currentAbsolutePosition.x - ((absX - base.x) * 512 + 256);
	                    deltaZ = (int) currentAbsolutePosition.z - ((absY - base.y) * 512 + 256);
	                } else {
	                    Vector3 lastAbsolutePosition;
	                    if ((bitpacked & 0x8000) != 0) {
	                        int playerIndex = bitpacked & 0x7fff;
	                        PlayerEntity player = PlayerUpdate.players[playerIndex];
	                        if (player == null) {
	                            current.rotateEquipment(equipIndex, -1);
	                            continue;
	                        }
	                        lastAbsolutePosition = player.getAbsolutePosition().coords;
	                    } else {
	                        ObjectNode npcReference = client.NPC_MAP.get(bitpacked);
	                        if (npcReference == null) {
	                            current.rotateEquipment(equipIndex, -1);
	                            continue;
	                        }
	                        NpcEntity npcEntity = (NpcEntity) npcReference.value;
	                        lastAbsolutePosition = npcEntity.getAbsolutePosition().coords;
	                    }
	                    deltaX = (int) currentAbsolutePosition.x - (int) lastAbsolutePosition.x;
	                    deltaZ = (int) currentAbsolutePosition.z - (int) lastAbsolutePosition.z;
	                }
	                if (deltaX != 0 || deltaZ != 0) {
	                    current.rotateEquipment(equipIndex, (int) (Math.atan2(deltaX, deltaZ) * 2607.5945876176133D) & 0x3fff);
	                }
	            }
	        }
	        if (reset) {
	            current.equipmentUpdateDetails = null;
	            current.equipmentRotations = null;
	        }
	    }
	}

	public static int turnDirectionIndex;
}
