package com.jagex.game.runetek5.graphics.hint;

import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.utils.MathUtils;

/**
 * 
 * @author Brainpower - 06/04/2022
 *
 */
public class HintRenderer {

	public static void drawHintArrow(int x, int y) {
	    HintArrow[] arrows = PlayerUpdate.HINT_ARROWS;
	    for (int index = 0; index < arrows.length; index++) {
	        HintArrow arrow = arrows[index];
	        if (arrow != null && arrow.targetType == 2) {
	            HintRenderer.calculateHintArrowPlacement(arrow.plane, arrow.x, arrow.y, arrow.height * 2);
	            if (client.HINT_POSITION[0] > -1.0f && client.cycles % 20 < 10) {
	                NativeSprite arrowSprite = StaticMedia.hintHeadiconSprites[arrow.arrowSprite];
	                int drawX = (int) (client.HINT_POSITION[0] + x - 12.0F);
	                int drawY = (int) (client.HINT_POSITION[1] + y - 28.0F);
	                arrowSprite.draw(drawX, drawY);
	            }
	        }
	    }
	}

	public static void calculateHintArrowPlacement(int plane, int absX, int absY, int translateSunX, int floorOffset, boolean calculateProjection) {
	    if (!calculateProjection && (absX < 512 || absY < 512 || absX > (Resource.mapRegion.getSceneSizeX() - 2) * 512 || absY > (Resource.mapRegion.getSceneSizeY() - 2) * 512)) {
	        client.HINT_POSITION[1] = -1.0f;
	        client.HINT_POSITION[0] = -1.0f;
	    } else {
	        int drawHeight = MapRegion.tileheightForAbsolutePosition(absX, absY, plane) - floorOffset;
	        client.SUN_MATRIX.copyFrom(Renderers.activeToolkit.copySunPosition());
	        client.SUN_MATRIX.translate(translateSunX, 0.0F, 0.0F);
	        Renderers.activeToolkit.setSunPosition(client.SUN_MATRIX);
	        if (calculateProjection) {
	            Renderers.activeToolkit.calculateProjection(absX, drawHeight, absY, client.HINT_POSITION);
	        } else {
	            Renderers.activeToolkit.computeWorldToScreen(absX, drawHeight, absY, client.HINT_POSITION);
	        }
	        client.SUN_MATRIX.translate(-translateSunX, 0.0F, 0.0F);
	        Renderers.activeToolkit.setSunPosition(client.SUN_MATRIX);
	        client.HINT_POSITION[0] -= client.hintArrowOffsetX;
	        client.HINT_POSITION[1] -= client.hintArrowOffsetY;
	    }
	}

	public static void calculateHintArrowPlacement(int plane, int absX, int absY, int translateSunX) {
	    calculateHintArrowPlacement(plane, absX, absY, 0, translateSunX, false);
	}

	public static void calculateHintForSceneobject(SceneGraphEntity graphNode, int floorOffset, boolean calculateProjection) {
	    int absX;
	    int absY;
	    if (graphNode instanceof AbstractInteractiveEntity interactive && graphNode instanceof GameObject) {
            int width = interactive.endX - interactive.startX + 1 << 9;
	        int length = interactive.endZ - interactive.startZ + 1 << 9;
	        absX = width / 2 + (interactive.startX << 9);
	        absY = length / 2 + (interactive.startZ << 9);
	    } else {
	        Vector3 abs = graphNode.getAbsolutePosition().coords;
	        absX = (int) abs.x;
	        absY = (int) abs.z;
	    }
	    calculateHintArrowPlacement(graphNode.plane, absX, absY, 0, floorOffset, calculateProjection);
	}

	public static void calculateSceneobjectHintProjection(SceneGraphEntity sceneObject, int floorOffset) {
	    calculateHintForSceneobject(sceneObject, floorOffset, true);
	}

	public static void calculateHintForSceneobject(SceneGraphEntity graphNode, int floorOffset) {
	    calculateHintForSceneobject(graphNode, floorOffset, false);
	}

	public static void method3151(int offsetX, int offsetY, Component component, InteractiveComponentSprite sprite, int clipLeft, int clipTop) {
	    HintArrow[] hintArrows = PlayerUpdate.HINT_ARROWS;
	
	    for (int index = 0; index < hintArrows.length; index++) {
	        HintArrow arrow = hintArrows[index];
	        if (arrow != null && arrow.targetType != 0 && client.cycles % 20 < 10) {
	            if (arrow.targetType == 1) {
	                ObjectNode objectNode = client.NPC_MAP.get(arrow.targetIndex);
	                if (objectNode != null) {
	                    NpcEntity npc = (NpcEntity) objectNode.value;
	                    Vector3 abs = npc.getAbsolutePosition().coords;
	                    int x = (int) abs.x / 128 - offsetX / 128;
	                    int y = (int) abs.z / 128 - offsetY / 128;
	                    HintRenderer.draw(component, sprite, clipLeft, clipTop, x, y, arrow.arrowSprite, 360000L);
	                }
	            }
	
	            if (arrow.targetType == 2) {
	                int x = arrow.x / 128 - offsetX / 128;
	                int y = arrow.y / 128 - offsetY / 128;
	                long long_17 = (long) arrow.distance << 7;
	                long_17 *= long_17;
	                HintRenderer.draw(component, sprite, clipLeft, clipTop, x, y, arrow.arrowSprite, long_17);
	            }
	
	            if (arrow.targetType == 10 && arrow.targetIndex >= 0 && arrow.targetIndex < PlayerUpdate.players.length) {
	                PlayerEntity player = PlayerUpdate.players[arrow.targetIndex];
	                if (player != null) {
	                    Vector3 abs = player.getAbsolutePosition().coords;
	                    int x = (int) abs.x / 128 - offsetX / 128;
	                    int y = (int) abs.z / 128 - offsetY / 128;
	                    HintRenderer.draw(component, sprite, clipLeft, clipTop, x, y, arrow.arrowSprite, 360000L);
	                }
	            }
	        }
	    }	
	}

	public static void draw(Component component, InteractiveComponentSprite sprite, int clipLeft, int clipTop, int x, int y, int arrowSprite, long maxDistance) {
	    int distanceSquared = y * y + x * x;
	    if (distanceSquared <= maxDistance) {
	        int angle;
	        if (Camera.cameraCalculationType == 4) {
	            angle = (int) Camera.camAngleY & 0x3fff;
	        } else {
	            angle = (int) Camera.camAngleY + client.minimapRotation & 0x3fff;
	        }
	        int sinAngle = MathUtils.SINE[angle];
	        int cosAngle = MathUtils.COSINE[angle];
	        if (Camera.cameraCalculationType != 4) {
	            sinAngle = sinAngle * 256 / (client.minimapZoom + 256);
	            cosAngle = cosAngle * 256 / (client.minimapZoom + 256);
	        }
	        int dx = cosAngle * x + y * sinAngle >> 14;
	        int dy = y * cosAngle - sinAngle * x >> 14;
	        NativeSprite hintSprite = StaticMedia.hintMapmarkerSprites[arrowSprite];
	        int width = hintSprite.getWidth();
	        int height = hintSprite.getHeight();
	        int x1 = dx + component.width / 2 - width / 2;
	        int x2 = width + x1;
	        int y1 = -dy + component.height / 2 - height;
	        int y2 = height + y1;
	        if (sprite.withinBounds(x1, y1) && sprite.withinBounds(x2, y1) && sprite.withinBounds(x1, y2) && sprite.withinBounds(x2, y2)) {
	            hintSprite.method2773(x1 + clipLeft, clipTop + y1, sprite.surface, clipLeft, clipTop);
	        } else {
	            double angleRadians = Math.atan2(dx, dy);
	            int maxArrowDistance = Math.min(component.width / 2, component.height / 2);
	            maxArrowDistance -= 6;
	            int i_25 = (int) (Math.sin(angleRadians) * maxArrowDistance);
	            int i_26 = (int) (Math.cos(angleRadians) * maxArrowDistance);
	            StaticMedia.hintMapedgeSprites[arrowSprite].method2758(clipLeft + component.width / 2.0F + i_25, clipTop + component.height / 2.0F - i_26, 4096, (int) (65535.0D * (-angleRadians / 6.283185307179586D)));
	        }
	    }
	}

}
