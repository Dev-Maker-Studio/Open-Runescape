package com.jagex.game.runetek5.entity;

import com.jagex.game.runetek5.graphics.light.FlickeringEffect;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public abstract class AbstractInteractiveEntity extends SceneGraphEntity {

    public short startX;
    public short endX;
    public short startZ;
    public short endZ;
    public final boolean supportsItems;
    public final byte rotation;

    protected AbstractInteractiveEntity(Scene scene, int plane, int collisionPlane, int x, int y, int z, int startX, int endX, int startY, int endY, boolean supportsItems, byte rotation) {
        super(scene);
        this.plane = (byte) plane;
        this.collisionPlane = (byte) collisionPlane;
        this.setBasePosition(new Vector3(x, y, z));
        this.startX = (short) startX;
        this.endX = (short) endX;
        this.startZ = (short) startY;
        this.endZ = (short) endY;
        this.supportsItems = supportsItems;
        this.rotation = rotation;
    }
    
    public void calculatePosition() {
    	// NOOP
    } 

    @Override
	public int getActiveLights(PointLight[] cycledLightNodes) {
		int lightCount = 0;
		LoopLights: for (int x = this.startX; x <= this.endX; x++) {
			for (int y = this.startZ; y <= this.endZ; y++) {
				long currentLightUid = (super.scene.tileLightUids[super.plane][x][y]);
				long uidStep = 0L;
				FindLightSlot: while (uidStep <= 48L) {
					int slot = (int) (currentLightUid >>> (int) uidStep & 0xffffL);
					if (slot <= 0)
						break;
					FlickeringEffect positionedLight = (super.scene.lightSlotOccupant[slot - 1]);
					for (int loopPlane = 0; loopPlane < lightCount; loopPlane++) {
						if (cycledLightNodes[loopPlane] == positionedLight.pointLight) {
							uidStep += 16L;
							continue FindLightSlot;
						}
					}
					cycledLightNodes[lightCount++] = positionedLight.pointLight;
					if (lightCount == 4)
						break LoopLights;
					uidStep += 16L;
				}
			}
		}
		for (int lightIndex = lightCount; lightIndex < 4; lightIndex++)
			cycledLightNodes[lightIndex] = null;
		if (this.rotation != 0) {
			int cameraOffsetX = (this.startX - super.scene.cameraGridPositionX);
			int cameraOffsetY = (this.startZ - super.scene.cameraGridPositionY);
			short StartX;
			int StartY;
			int EndX;
			short EndY;
			if (this.rotation == 1) {
				if (cameraOffsetY > cameraOffsetX) {
					StartX = this.startX;
					StartY = this.startZ - 1;
					EndX = 1 + this.startX;
					EndY = this.startZ;
				} else {
					StartX = this.startX;
					StartY = 1 + this.startZ;
					EndX = this.startX - 1;
					EndY = this.startZ;
				}
			} else if (cameraOffsetY > -cameraOffsetX) {
				StartX = this.startX;
				StartY = this.startZ - 1;
				EndX = this.startX - 1;
				EndY = this.startZ;
			} else {
				StartX = this.startX;
				StartY = this.startZ + 1;
				EndX = 1 + this.startX;
				EndY = this.startZ;
			}
			int plane = 0;
			FindLight: for (/**/; plane < lightCount; plane++) {
				long lightUid = (super.scene.tileLightUids[plane][StartX][StartY]);
				while (0L != lightUid) {
					FlickeringEffect positionedLight = (super.scene.lightSlotOccupant[(int) ((lightUid & 0xffffL) - 1L)]);
					lightUid >>>= 16;
					if (cycledLightNodes[plane] == positionedLight.pointLight)
						continue FindLight;
				}
				lightUid = (super.scene.tileLightUids[plane][EndX][EndY]);
				while (lightUid != 0L) {
					FlickeringEffect positionedLight = (super.scene.lightSlotOccupant[(int) ((lightUid & 0xffffL) - 1L)]);
					lightUid >>>= 16;
					if (cycledLightNodes[plane] == positionedLight.pointLight)
						continue FindLight;
				}
				for (int ascendingLightPlane = plane; ascendingLightPlane < lightCount - 1; ascendingLightPlane++)
					cycledLightNodes[ascendingLightPlane] = cycledLightNodes[1 + ascendingLightPlane];
				lightCount--;
			}
		}
		return lightCount;
	}
    
    
    

    /**
     * [J] Tile the entity is located on is hidden
     */
    @Override
    public boolean isSoftHidden(AbstractToolkit toolkit) {
        return super.scene.culling.hideTile(super.collisionPlane, this.startX, this.endX, this.startZ, this.endZ, getAxisAlignedBoundingBox(toolkit));
    }

    /**
     * [J] Is the entity in view distance
     */
    @Override
    public boolean canRender() {
        for (int x = this.startX; x <= this.endX; x++) {
            for (int y = this.startZ; y <= this.endZ; y++) {
                int tileX = super.scene.visibleTiles + (x - super.scene.cameraGridPositionX);
                if (tileX >= 0 && tileX < super.scene.adjacent_tile_on_screen.length) {
                    int tileY = super.scene.visibleTiles + (y - super.scene.cameraGridPositionY);
                    if (tileY >= 0 && tileY < super.scene.adjacent_tile_on_screen.length && super.scene.adjacent_tile_on_screen[tileX][tileY]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
