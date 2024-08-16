package com.jagex.game.runetek5.scene.culling;

import com.jagex.game.runetek5.entity.gameobject.AbstractWall;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;

/**
 * Rename to Culling / SceneCulling?
 * @author Brainpower
 *
 */
public class Culling {

    public static AbstractToolkit activeToolkit;
    /**
     * 
     */
    public boolean cullingEnabled = true;
    public int[] visibleAboveY;
    public int[] visibleAboveX;
    public int[] visibleBelowX;
    public int[] visibleBelowY;
    public int[] visibleBelowDrawheight;
    public int wallCullingCursorPosition;
    public int frusumCullingCursorPos;
    public int oneThirdViewportWidth = -1; //FIXME: naming
    public int oneThirdViewportHeight = -1; //FIXME: naming
    public int[] anIntArray2565;
    public int tileCullingClusterCount;
    public boolean drawingScene = true;
    public boolean cullingClustersCleared;
    public final float[] projection = new float[3];
    public final int[] viewport = new int[2];
    public final boolean benchmarkCulling = true;
    
    /**
     * Always 512 (128 << upscaling)
     */
    public final int fullTileDimensionX;
    public final int fullTileDimensionY;
    
    public final Scene scene;
    public final CullingCluster[] tileCullingCluster;
    public final CullingCluster[] interactiveCulling;
    public int interactiveCullingCursorPos;
    public final CullingCluster[] diagonalWallCulling;
    public final CullingCluster[] frustumClusters;
    public final int[][][] tileDrawCycles;
    public FrustumCulling frustumCulling;

    public Culling(Scene scene) {
    	this.fullTileDimensionX = scene.fullTileDimension;
        this.fullTileDimensionY = scene.fullTileDimension;
        this.scene = scene;
        this.tileCullingCluster = new CullingCluster[509];
        this.tileCullingClusterCount = 0;
        this.interactiveCulling = new CullingCluster[2034];
        this.interactiveCullingCursorPos = 0;
        this.diagonalWallCulling = new CullingCluster[1027];
        this.wallCullingCursorPosition = 0;
        this.frustumClusters = new CullingCluster[1005];
        this.frusumCullingCursorPos = 0;
        this.tileDrawCycles = new int[scene.regionSizeZ][scene.sceneDimensionX + 1][scene.sceneDimensionY + 1];
        this.cullingClustersCleared = false;
        this.cullingEnabled = !(scene.activeToolkit instanceof SoftwareToolkit);
        if (this.cullingEnabled) {
        	this.frustumCulling = new FrustumCulling(this);
        }
    }


    /**
     * 
     * @param displacement
     * @param z
     * @param x
     * @param y
     * @param cullY - 960 default (240 << scaling)
     * @param cullXZ
     */
    public void addWallCulling(int displacement, int z, int x, int y, int cullY, int cullXZ) {
        if (displacement != 8 && displacement != 16) {
            Tile tile = this.scene.tiles[z][x][y];
            if (tile == null) {
                this.scene.tiles[z][x][y] = tile = new Tile(z);
            }
            if (displacement == 1) { 
            	// rotation % 1 == 0
                tile.cullY = (short) cullY;
                tile.cullX = (short) cullXZ;
            } else if (displacement == 2) { 
            	// rotation % 1 == 1
                tile.interactiveSideViewHeightOffset = (short) cullY;
                tile.cullZ = (short) cullXZ;
            }

            if (this.cullingClustersCleared) {
                removeInteractiveCullingClusters();
            }
        } else if (displacement == 8) { 
        	// Wall placement on tiles	[   ][ / ]
        	//						  	[ / ][   ]
            int startX = x << this.scene.tileUnits;
            int endX = startX + this.scene.fullTileDimension;
            int startY = y << this.scene.tileUnits;
            int endY = startY + this.scene.fullTileDimension;
            int heightSouthWest = this.scene.surfaceHeightmap[z].getHeight(x, y);
            int heightNorthEast = this.scene.surfaceHeightmap[z].getHeight(x + 1, y + 1);
            this.diagonalWallCulling[this.wallCullingCursorPosition++] = new CullingCluster(this.scene, 8, z,
                    startX, endX, endX, startX, heightSouthWest, heightNorthEast, heightNorthEast - cullY, heightSouthWest - cullY,
                    startY, endY, endY, startY);
        } else { 
        	// Wall placement on tiles	[ \ ][   ]
        	//						  	[   ][ \ ]
        	int startX = this.scene.fullTileDimension + (x << this.scene.tileUnits);
        	int endX = startX - this.scene.fullTileDimension;
        	int startY = y << this.scene.tileUnits;
        	int endY = this.scene.fullTileDimension + startY;
        	int heightSouthEast = this.scene.surfaceHeightmap[z].getHeight(x + 1, y);
        	int heightNorthWest = this.scene.surfaceHeightmap[z].getHeight(x, y + 1);
            this.diagonalWallCulling[this.wallCullingCursorPosition++] = new CullingCluster(this.scene, displacement, z,
                    startX, endX, endX, startX, heightSouthEast, heightNorthWest, heightNorthWest - cullY, heightSouthEast - cullY,
                    startY, endY, endY, startY);
        }
    }

    public void clear() {
        removeInteractiveCullingClusters();
    }

    public void processCulling(AbstractToolkit toolkit, int nextPlane) {
        activeToolkit = toolkit;
        if (this.cullingEnabled && this.drawingScene) {
            if (this.benchmarkCulling) {
                this.scene.nanoTimer.getTime();
            }

            activeToolkit.accumulateViewportDimensions(this.viewport);
            if (this.oneThirdViewportWidth != (int) (this.viewport[0] / 3.0F) || (int) (this.viewport[1] / 3.0F) != this.oneThirdViewportHeight) {
                this.oneThirdViewportWidth =  (int) (this.viewport[0] / 3.0F);
                this.oneThirdViewportHeight = (int) (this.viewport[1] / 3.0F);
                this.anIntArray2565 = new int[this.oneThirdViewportWidth * this.oneThirdViewportHeight];
            }

            this.frusumCullingCursorPos = 0;
            for (int index = 0; index < this.tileCullingClusterCount; index++) {
                processCullingCluster(activeToolkit, this.tileCullingCluster[index], nextPlane);
            }

            for (int index = 0; index < this.interactiveCullingCursorPos; index++) {
                processCullingCluster(activeToolkit, this.interactiveCulling[index], nextPlane);
            }

            for (int index = 0; index < this.wallCullingCursorPosition; index++) {
                processCullingCluster(activeToolkit, this.diagonalWallCulling[index], nextPlane);
            }

            this.frustumCulling.anInt2588 = 0;
            if (this.frusumCullingCursorPos > 0) {
            	int size = this.anIntArray2565.length;
                int i = 0;
                while (i < size) {
                    this.anIntArray2565[i++] = Integer.MAX_VALUE;
                }
                this.frustumCulling.state = 1;
                for (int index = 0; index < this.frusumCullingCursorPos; index++) {
                    CullingCluster cluster = this.frustumClusters[index];
                    
                    this.frustumCulling.cull(cluster.projectionY[0], cluster.projectionY[1], cluster.projectionY[3],
	    								 cluster.projectionX[0], cluster.projectionX[1], cluster.projectionX[3],
	    								 cluster.projectionZ[0], cluster.projectionZ[1], cluster.projectionZ[3]);
                    
                    this.frustumCulling.cull(cluster.projectionY[1], cluster.projectionY[2], cluster.projectionY[3],
									     cluster.projectionX[1], cluster.projectionX[2], cluster.projectionX[3],
									     cluster.projectionZ[1], cluster.projectionZ[2], cluster.projectionZ[3]);
                }
                this.frustumCulling.state = 2;
            }
            if (this.benchmarkCulling) {
                this.scene.nanoTimer.getTime();
            }
        } else {
            this.frusumCullingCursorPos = 0;
        }
    }

    public void createTileCullingCluster(int plane, int xEnd, int xStart, int i_5, int i_6, int i_7, int i_8) {
        this.tileCullingCluster[this.tileCullingClusterCount++] = new CullingCluster(this.scene, 4, plane, xEnd, xStart, xStart, xEnd, i_7, i_8, i_8, i_7, i_5, i_5, i_6, i_6);
    }

    public boolean calculateProjection(CullingCluster cluster, int cullingIndex) {
        if (!projectedPositionHidden(cluster.tileCullingsX[cullingIndex], cluster.drawHeights[cullingIndex], cluster.tileCullingsZ[cullingIndex])) {
            return false;
        } 
        cluster.projectionX[cullingIndex] = (short) ((int) this.projection[0]);
        cluster.projectionY[cullingIndex] = (short) ((int) this.projection[1]);
        cluster.projectionZ[cullingIndex] = (short) ((int) this.projection[2]);
        return true;
    }

    public boolean projectedPositionHidden(int x, int y, int z) {
        activeToolkit.calculateProjection(x, y, z, this.projection);
        if (this.projection[2] < 50.0F) {
            return false;
        } 
        this.projection[0] /= 3.0F;
        this.projection[1] /= 3.0F;
        return true;
    }

    /**
     * Checks if a tile is hidden (underlay & overlay not rendered)
     */
    public boolean isTileHidden(int plane, int x, int y) {
        if (this.cullingEnabled && this.drawingScene) {
            if (this.frustumCulling.anInt2588 < 102) {
                return false;
            } 
            int lastDrawCycle = this.tileDrawCycles[plane][x][y];
            if (lastDrawCycle == -this.scene.tileDrawCycle) {
                return false;
            } 
            if (this.scene.tileDrawCycle == lastDrawCycle) {
                return true;
            } 
            if (this.scene.localHeightmap == this.scene.underwaterHeightmap) {
                return false;
            }
            int upscaledX = x << this.scene.tileUnits;
            int upscaledZ = y << this.scene.tileUnits;
            if (isCulled(
            		upscaledX + 1,
            		this.scene.localHeightmap[plane].getHeight(x, y),
            		upscaledZ + 1,
            		upscaledX - 1 + this.scene.fullTileDimension,
            		this.scene.localHeightmap[plane].getHeight(x + 1, y + 1),
            		upscaledZ - 1 + this.scene.fullTileDimension,
            		upscaledX + 1,
            		this.scene.localHeightmap[plane].getHeight(x, y + 1),
            		upscaledZ - 1 + this.scene.fullTileDimension)
            	&& 
            	isCulled(
            		upscaledX + 1,
            		this.scene.localHeightmap[plane].getHeight(x, y),
        			upscaledZ + 1,
        			upscaledX - 1 + this.scene.fullTileDimension,
        			this.scene.localHeightmap[plane].getHeight(x + 1, y),
        			upscaledZ + 1,
        			upscaledX - 1 + this.scene.fullTileDimension,
        			this.scene.localHeightmap[plane].getHeight(x + 1, y + 1),
        			upscaledZ - 1 + this.scene.fullTileDimension)) {
                this.tileDrawCycles[plane][x][y] = this.scene.tileDrawCycle;
                return true;
            } 
            this.tileDrawCycles[plane][x][y] = -this.scene.tileDrawCycle;
            return false;
        } 
        return false;
    }

    public boolean isTileHidden(int z, int x, int y, int minY) {
        if (this.cullingEnabled && this.drawingScene) {
            if (this.frustumCulling.anInt2588 < 102) {
                return false;
            } 
            if (!isTileHidden(z, x, y)) {
                return false;
            } 
            int upscaledX = x << this.scene.tileUnits;
            int upscaledY = y << this.scene.tileUnits;
            return boundaryCulled(upscaledX, this.scene.localHeightmap[z].getHeight(x, y), upscaledY, this.scene.fullTileDimension, minY, this.scene.fullTileDimension);
        } 
        return false;
    }

    public boolean hideTile(int z, int startX, int endX, int startY, int endY, Silhouette axisAlignedBoundingBox) {
        if (this.cullingEnabled && this.drawingScene) {
            if (this.frustumCulling.anInt2588 < 102) {
                return false;
            } 
            if (endX == startX && endY == startY) {
                return isTileHidden(z, startX, startY) && hideBoundary(axisAlignedBoundingBox);
            } 
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    if (this.tileDrawCycles[z][x][y] == -this.scene.tileDrawCycle) {
                        return false;
                    }
                }
            }
            return hideBoundary(axisAlignedBoundingBox);
        } 
        return false;
    }

    public boolean hideWall(AbstractWall wall, int z, int x, int y) {
        if (this.cullingEnabled && this.drawingScene) {
            if (this.frustumCulling.anInt2588 < 102) {
                return false;
            } 
            if (!isTileHidden(z, x, y)) {
                return false;
            } 
            int upscaledX = x << this.scene.tileUnits;
            int upscaledY = y << this.scene.tileUnits;
            int groundHeight = this.scene.localHeightmap[z].getHeight(x, y) - 1;
            int wallDrawHeight = groundHeight + wall.getMinY();
            if (wall.cullingType == 1) {
            	return (isCulled(upscaledX, groundHeight, upscaledY, upscaledX, wallDrawHeight, upscaledY, upscaledX, wallDrawHeight, this.scene.fullTileDimension + upscaledY) &&
                		isCulled(upscaledX, groundHeight, upscaledY, upscaledX, wallDrawHeight, this.scene.fullTileDimension + upscaledY, upscaledX, groundHeight, this.scene.fullTileDimension + upscaledY));
            }
            if (wall.cullingType == 2) {
            	return (isCulled(upscaledX, groundHeight, this.scene.fullTileDimension + upscaledY, this.scene.fullTileDimension + upscaledX, wallDrawHeight, this.scene.fullTileDimension + upscaledY, upscaledX, wallDrawHeight, this.scene.fullTileDimension + upscaledY) &&
                		isCulled(upscaledX, groundHeight, this.scene.fullTileDimension + upscaledY, this.scene.fullTileDimension + upscaledX, groundHeight, this.scene.fullTileDimension + upscaledY, this.scene.fullTileDimension + upscaledX, wallDrawHeight, this.scene.fullTileDimension + upscaledY));
            }
            if (wall.cullingType == 4) {
            	return (isCulled(this.scene.fullTileDimension + upscaledX, groundHeight, upscaledY, this.scene.fullTileDimension + upscaledX, wallDrawHeight, upscaledY, this.scene.fullTileDimension + upscaledX, wallDrawHeight, this.scene.fullTileDimension + upscaledY) &&
            			isCulled(this.scene.fullTileDimension + upscaledX, groundHeight, upscaledY, this.scene.fullTileDimension + upscaledX, wallDrawHeight, this.scene.fullTileDimension + upscaledY, this.scene.fullTileDimension + upscaledX, groundHeight, this.scene.fullTileDimension + upscaledY));
            }
            if (wall.cullingType == 8) {
            	return (isCulled(upscaledX, groundHeight, upscaledY, this.scene.fullTileDimension + upscaledX, wallDrawHeight, upscaledY, upscaledX, wallDrawHeight, upscaledY) &&
            			isCulled(upscaledX, groundHeight, upscaledY, this.scene.fullTileDimension + upscaledX, groundHeight, upscaledY, this.scene.fullTileDimension + upscaledX, wallDrawHeight, upscaledY));
            }
            if (wall.cullingType == 16) {
            	return boundaryCulled(upscaledX, groundHeight, this.scene.halfTileOffset + upscaledY, this.scene.halfTileOffset, wallDrawHeight, this.scene.halfTileOffset);
            }
            if (wall.cullingType == 32) {
            	return boundaryCulled(this.scene.halfTileOffset + upscaledX, groundHeight, this.scene.halfTileOffset + upscaledY, this.scene.halfTileOffset, wallDrawHeight, this.scene.halfTileOffset);
            }
            if (wall.cullingType == 64) {
            	return boundaryCulled(this.scene.halfTileOffset + upscaledX, groundHeight, upscaledY, this.scene.halfTileOffset, wallDrawHeight, this.scene.halfTileOffset);
            }
            if (wall.cullingType == 128) {
            	return boundaryCulled(upscaledX, groundHeight, upscaledY, this.scene.halfTileOffset, wallDrawHeight, this.scene.halfTileOffset);
            }
            return true;
        } 
        return false;
    }

    /**
     * 
     * @param minX - the lowest width coordinate
     * @param minY - the lowest height coordinate
     * @param minZ - the lowest length coordinate
     * @param sizeX - the width
     * @param sizeY - the height
     * @param sizeZ - the length
     * @return - culled
     */
    public boolean boundaryCulled(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        int maxX = sizeX + minX;
        int maxY = sizeY + minY;
        int maxZ = sizeZ + minZ;
        if (!isCulled(minX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ)) {
            return false;
        } 
        if (!isCulled(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ)) {
            return false;
        } 
        if (minX < this.scene.cameraPositionX) {
            if (!isCulled(minX, minY, maxZ, minX, maxY, minZ, minX, maxY, maxZ)) {
                return false;
            }

            if (!isCulled(minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ)) {
                return false;
            }
        } else {
            if (!isCulled(maxX, minY, maxZ, maxX, maxY, minZ, maxX, maxY, maxZ)) {
                return false;
            }

            if (!isCulled(maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ)) {
                return false;
            }
        }
        if (minZ < this.scene.cameraPositionZ) {
            if (!isCulled(minX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ)) {
                return false;
            }

            return isCulled(minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ);
        } else {
            if (!isCulled(minX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ)) {
                return false;
            }
            return isCulled(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ);
        }
    }

    //max_x, min_y, max_z, max_x, min_y, min_z, max_x, max_y, min_z
    public boolean isCulled(int xA, int yA, int zA, int xB, int yB, int zB, int xC, int yC, int zC) {
        if (!projectedPositionHidden(xA, yA, zA)) {
            return false;
        } 
        xA = (int) this.projection[0];
        yA = (int) this.projection[1];
        zA = (int) this.projection[2];
        if (!projectedPositionHidden(xB, yB, zB)) {
            return false;
        } 
        xB = (int) this.projection[0];
        yB = (int) this.projection[1];
        zB = (int) this.projection[2];
        if (!projectedPositionHidden(xC, yC, zC)) {
            return false;
        } 
        xC = (int) this.projection[0];
        yC = (int) this.projection[1];
        zC = (int) this.projection[2];
        return this.frustumCulling.cull(yA, yB, yC, xA, xB, xC, zA, zB, zC);
    }

    public void processCullingCluster(AbstractToolkit toolkit, CullingCluster cullingCluster, int plane) {
        activeToolkit = toolkit;
        
        /**
         * Check if rendering the tile should be forced
         */
        if (this.visibleBelowDrawheight != null && cullingCluster.plane >= plane) {
            for (int index = 0; index < this.visibleBelowDrawheight.length; index++) {
                if (this.visibleBelowDrawheight[index] != -1000000 &&
            		  (cullingCluster.drawHeights[0] <= this.visibleBelowDrawheight[index]
            		|| cullingCluster.drawHeights[1] <= this.visibleBelowDrawheight[index]
            		|| cullingCluster.drawHeights[2] <= this.visibleBelowDrawheight[index]
            		|| cullingCluster.drawHeights[3] <= this.visibleBelowDrawheight[index])
            		  &&                 		  
            		  (cullingCluster.tileCullingsX[0] <= this.visibleBelowX[index]
            		|| cullingCluster.tileCullingsX[1] <= this.visibleBelowX[index]
            		|| cullingCluster.tileCullingsX[2] <= this.visibleBelowX[index]
            		|| cullingCluster.tileCullingsX[3] <= this.visibleBelowX[index])
            		  &&                 		  
            		  (cullingCluster.tileCullingsX[0] >= this.visibleAboveX[index]
            		|| cullingCluster.tileCullingsX[1] >= this.visibleAboveX[index]
            		|| cullingCluster.tileCullingsX[2] >= this.visibleAboveX[index]
            		|| cullingCluster.tileCullingsX[3] >= this.visibleAboveX[index])
            		  &&                 		  
            		  (cullingCluster.tileCullingsZ[0] <= this.visibleBelowY[index]
            		|| cullingCluster.tileCullingsZ[1] <= this.visibleBelowY[index]
            		|| cullingCluster.tileCullingsZ[2] <= this.visibleBelowY[index]
            		|| cullingCluster.tileCullingsZ[3] <= this.visibleBelowY[index])
            		  &&                 		  
            		  (cullingCluster.tileCullingsZ[0] >= this.visibleAboveY[index]
            		|| cullingCluster.tileCullingsZ[1] >= this.visibleAboveY[index]
            		|| cullingCluster.tileCullingsZ[2] >= this.visibleAboveY[index]
            		|| cullingCluster.tileCullingsZ[3] >= this.visibleAboveY[index])) {
                    return; // Ensure tile is rendered
                }
            }
        }
        if (cullingCluster.type == 1) {
        	int sceneX = this.scene.visibleTiles + (cullingCluster.startX - this.scene.cameraGridPositionX);
            if (sceneX >= 0 && sceneX <= this.scene.visibleTiles + this.scene.visibleTiles) {
            	int sceneStartY = this.scene.visibleTiles + (cullingCluster.startY - this.scene.cameraGridPositionY);
                if (sceneStartY < 0) {
                    sceneStartY = 0;
                } else if (sceneStartY > this.scene.visibleTiles + this.scene.visibleTiles) {
                    return;
                }
                int sceneEndY = this.scene.visibleTiles + (cullingCluster.endY - this.scene.cameraGridPositionY);
                if (sceneEndY > this.scene.visibleTiles + this.scene.visibleTiles) {
                    sceneEndY = this.scene.visibleTiles + this.scene.visibleTiles;
                } else if (sceneEndY < 0) {
                    return;
                }
                boolean onScreen = false;
                while (sceneStartY <= sceneEndY) {
                    if (this.scene.adjacent_tile_on_screen[sceneX][sceneStartY++]) {
                        onScreen = true;
                        break;
                    }
                }
                if (onScreen) {
                	float cameraOffsetX = (this.scene.cameraPositionX - cullingCluster.tileCullingsX[0]);
                    if (cameraOffsetX < 0.0F) {
                        cameraOffsetX *= -1.0f;
                    }
                    if (cameraOffsetX >= this.fullTileDimensionX
                    		&& calculateProjection(cullingCluster, 0)
                    		&& calculateProjection(cullingCluster, 1)
                    		&& calculateProjection(cullingCluster, 2)
                    		&& calculateProjection(cullingCluster, 3)) {
                        this.frustumClusters[this.frusumCullingCursorPos++] = cullingCluster;
                    }
                }
            }
        } else if (cullingCluster.type == 2) {
        	int y = this.scene.visibleTiles + (cullingCluster.startY - this.scene.cameraGridPositionY);
            if (y >= 0 && y <= this.scene.visibleTiles + this.scene.visibleTiles) {
            	int startX = this.scene.visibleTiles + (cullingCluster.startX - this.scene.cameraGridPositionX);
                if (startX < 0) {
                    startX = 0;
                } else if (startX > this.scene.visibleTiles + this.scene.visibleTiles) {
                    return;
                }
                int endX = this.scene.visibleTiles + (cullingCluster.endX - this.scene.cameraGridPositionX);
                if (endX > this.scene.visibleTiles + this.scene.visibleTiles) {
                    endX = this.scene.visibleTiles + this.scene.visibleTiles;
                } else if (endX < 0) {
                    return;
                }
                boolean onScreen = false;
                while (startX <= endX) {
                    if (this.scene.adjacent_tile_on_screen[startX++][y]) {
                        onScreen = true;
                        break;
                    }
                }
                if (onScreen) {
                	float cameraOffsetY = (this.scene.cameraPositionZ - cullingCluster.tileCullingsZ[0]);
                    if (cameraOffsetY < 0.0F) {
                        cameraOffsetY *= -1.0f;
                    }
                    if (cameraOffsetY >= this.fullTileDimensionX
                    		&& calculateProjection(cullingCluster, 0)
                    		&& calculateProjection(cullingCluster, 1)
                    		&& calculateProjection(cullingCluster, 2)
                    		&& calculateProjection(cullingCluster, 3)) {
                        this.frustumClusters[this.frusumCullingCursorPos++] = cullingCluster;
                    }
                }
            }
        } else if (cullingCluster.type != 16 && cullingCluster.type != 8) {
            if (cullingCluster.type == 4) {
                float f_12 = (cullingCluster.drawHeights[0] - this.scene.cameraPositionY);
                if (f_12 > this.fullTileDimensionY) {
                	int startY = this.scene.visibleTiles + (cullingCluster.startY - this.scene.cameraGridPositionY);
                    if (startY < 0) {
                        startY = 0;
                    } else if (startY > this.scene.visibleTiles + this.scene.visibleTiles) {
                        return;
                    }
                    int endY = this.scene.visibleTiles + (cullingCluster.endY - this.scene.cameraGridPositionY);
                    if (endY > this.scene.visibleTiles + this.scene.visibleTiles) {
                        endY = this.scene.visibleTiles + this.scene.visibleTiles;
                    } else if (endY < 0) {
                        return;
                    }
                    int startX = this.scene.visibleTiles + (cullingCluster.startX - this.scene.cameraGridPositionX);
                    if (startX < 0) {
                        startX = 0;
                    } else if (startX > this.scene.visibleTiles + this.scene.visibleTiles) {
                        return;
                    }
                    int endX = this.scene.visibleTiles + (cullingCluster.endX - this.scene.cameraGridPositionX);
                    if (endX > this.scene.visibleTiles + this.scene.visibleTiles) {
                        endX = this.scene.visibleTiles + this.scene.visibleTiles;
                    } else if (endX < 0) {
                        return;
                    }
                    boolean onScreen = false;
                    CheckOnScreen: for (int x = startX; x <= endX; x++) {
                        for (int y = startY; y <= endY; y++) {
                            if (this.scene.adjacent_tile_on_screen[x][y]) {
                                onScreen = true;
                                break CheckOnScreen;
                            }
                        }
                    }
                    if (onScreen &&
                    		calculateProjection(cullingCluster, 0) &&
                    		calculateProjection(cullingCluster, 1) &&
                    		calculateProjection(cullingCluster, 2) &&
                    		calculateProjection(cullingCluster, 3)) {
                        this.frustumClusters[this.frusumCullingCursorPos++] = cullingCluster;
                    }
                }
            }
        } else {
        	int startX = this.scene.visibleTiles + (cullingCluster.startX - this.scene.cameraGridPositionX);
            if (startX >= 0 && startX <= this.scene.visibleTiles + this.scene.visibleTiles) {
            	int startY = this.scene.visibleTiles + (cullingCluster.startY - this.scene.cameraGridPositionY);
                if (startY >= 0 && startY <= this.scene.visibleTiles + this.scene.visibleTiles && this.scene.adjacent_tile_on_screen[startX][startY]) {
                    float cameraOffsetX = (this.scene.cameraPositionX - cullingCluster.tileCullingsX[0]);
                    if (cameraOffsetX < 0.0F) {
                        cameraOffsetX *= -1.0f;
                    }
                    float cameraOffsetY = (this.scene.cameraPositionZ - cullingCluster.tileCullingsZ[0]);
                    if (cameraOffsetY < 0.0F) {
                        cameraOffsetY *= -1.0f;
                    }
                    if ((cameraOffsetX >= this.fullTileDimensionX || cameraOffsetY >= this.fullTileDimensionX)
                    		&& calculateProjection(cullingCluster, 0)
                    		&& calculateProjection(cullingCluster, 1)
                    		&& calculateProjection(cullingCluster, 2)
                    		&& calculateProjection(cullingCluster, 3)) {
                        this.frustumClusters[this.frusumCullingCursorPos++] = cullingCluster;
                    }
                }
            }
        }
    }

    public void removeCullingCluster(int displacement, int z, int x, int y) {
        if (displacement != 8 && displacement != 16) { // interactive
            Tile tile = this.scene.tiles[z][x][y];
            if (tile != null) {
                if (displacement == 1) {
                    tile.cullY = 0;
                } else if (displacement == 2) {
                    tile.interactiveSideViewHeightOffset = 0;
                }
            }
            removeInteractiveCullingClusters();
        } else { // non interactive
            for (int clusterIndex = 0; clusterIndex < this.wallCullingCursorPosition; clusterIndex++) {
                CullingCluster wallCluster = this.diagonalWallCulling[clusterIndex];
                if (wallCluster.type == displacement && z == wallCluster.plane && (x == wallCluster.startX && y == wallCluster.startY || x == wallCluster.endX && y == wallCluster.endY)) {
                    System.arraycopy(this.diagonalWallCulling, clusterIndex + 1, this.diagonalWallCulling, clusterIndex, this.diagonalWallCulling.length - (clusterIndex + 1));
                    this.wallCullingCursorPosition--;
                    break;
                }
            }
        }
    }

    public boolean hideBoundary(Silhouette axisAlignedBoundingBox) {
        return axisAlignedBoundingBox != null && boundaryCulled(axisAlignedBoundingBox.minX, axisAlignedBoundingBox.minY, axisAlignedBoundingBox.minZ, axisAlignedBoundingBox.maxX - axisAlignedBoundingBox.minX, axisAlignedBoundingBox.maxY - axisAlignedBoundingBox.minY, axisAlignedBoundingBox.maxZ - axisAlignedBoundingBox.minZ);
    }

    public void removeInteractiveCullingClusters() {
        for (int index = 0; index < this.interactiveCullingCursorPos; index++) {
            this.interactiveCulling[index] = null;
        }
        this.interactiveCullingCursorPos = 0;
        for (int scenePlane = 0; scenePlane < this.scene.regionSizeZ; scenePlane++) {
            for (int sceneY = 0; sceneY < this.scene.sceneDimensionX; sceneY++) {
                for (int sceneX = 0; sceneX < this.scene.sceneDimensionY; sceneX++) {
                	Tile tile = this.scene.tiles[scenePlane][sceneX][sceneY];
                    if (tile != null) {
                        if (tile.cullY > 0) {
                            tile.cullY *= -1;
                        }
                        if (tile.interactiveSideViewHeightOffset > 0) {
                            tile.interactiveSideViewHeightOffset *= -1;
                        }
                    }
                }
            }
        }
        for (int scenePlane = 0; scenePlane < this.scene.regionSizeZ; scenePlane++) {
            for (int sceneY = 0; sceneY < this.scene.sceneDimensionX; sceneY++) {
                for (int sceneX = 0; sceneX < this.scene.sceneDimensionY; sceneX++) {
                	Tile tile = this.scene.tiles[scenePlane][sceneX][sceneY];
                    if (tile != null) {
                        boolean hasBridge = this.scene.tiles[0][sceneX][sceneY] != null && this.scene.tiles[0][sceneX][sceneY].bridge != null;
                        if (tile.cullY < 0) {
                            int yStart = sceneY;
                            int yEnd = sceneY;
                            int zEnd = scenePlane;
                            Tile nextTile = this.scene.tiles[scenePlane][sceneX][sceneY - 1];
                            int count;
                            for (count = this.scene.surfaceHeightmap[scenePlane].getHeight(sceneX, sceneY); nextTile != null && nextTile.cullY < 0 && nextTile.cullY == tile.cullY && nextTile.cullX == tile.cullX && count == this.scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yStart - 1) && (yStart - 1 <= 0 || count == this.scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yStart - 2)) && yEnd - yStart <= 10; nextTile = this.scene.tiles[scenePlane][sceneX][yStart - 1]) {
                                yStart--;
                            }
                            for (nextTile = this.scene.tiles[scenePlane][sceneX][yEnd + 1]; yEnd < this.scene.sceneDimensionY && nextTile != null && nextTile.cullY < 0 && nextTile.cullY == tile.cullY && nextTile.cullX == tile.cullX && count == this.scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yEnd + 1) && (yEnd + 1 >= this.scene.sceneDimensionY || count == this.scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yEnd + 2)) && yEnd - yStart <= 10; nextTile = this.scene.tiles[scenePlane][sceneX][yEnd + 1]) {
                                yEnd++;
                            }
                            int highestSouthWest = this.scene.surfaceHeightmap[hasBridge ? scenePlane + 1 : scenePlane].getHeight(sceneX, yStart);
                            int i_14 = highestSouthWest + tile.cullY;
                            int heighestNorthWest = this.scene.surfaceHeightmap[hasBridge ? scenePlane + 1 : scenePlane].getHeight(sceneX, yEnd + 1);
                            int i_16 = heighestNorthWest + tile.cullY;
                            int mainX = sceneX << this.scene.tileUnits;
                            int tileEndY2 = yStart << this.scene.tileUnits;
                            int i_19 = (yEnd << this.scene.tileUnits) + this.scene.fullTileDimension;
                            this.interactiveCulling[this.interactiveCullingCursorPos++] = new CullingCluster(this.scene, 1, scenePlane, mainX + tile.cullX, mainX + tile.cullX, mainX + tile.cullX, mainX + tile.cullX, highestSouthWest, heighestNorthWest, i_16, i_14, tileEndY2, i_19, i_19, tileEndY2);
                            for (int tilePlane = scenePlane; tilePlane <= zEnd; tilePlane++) {
                                for (int tileY = yStart; tileY <= yEnd; tileY++) {
                                    this.scene.tiles[tilePlane][sceneX][tileY].cullY *= -1;
                                }
                            }
                        }
                        if (tile.interactiveSideViewHeightOffset < 0) {
                        	int xStart = sceneX;
                        	int xEnd = sceneX;
                        	int zEnd = scenePlane;
                            Tile nextTile = this.scene.tiles[scenePlane][sceneX - 1][sceneY];
                            int drawHeight;
                            for (drawHeight = this.scene.surfaceHeightmap[scenePlane].getHeight(sceneX, sceneY); nextTile != null &&
                            		nextTile.interactiveSideViewHeightOffset < 0 &&
                            		nextTile.interactiveSideViewHeightOffset == tile.interactiveSideViewHeightOffset &&
                            		nextTile.cullZ == tile.cullZ &&
                            		drawHeight == this.scene.surfaceHeightmap[scenePlane].getHeight(xStart - 1, sceneY) &&
                            		(xStart - 1 <= 0 || drawHeight == this.scene.surfaceHeightmap[scenePlane].getHeight(xStart - 2, sceneY)) &&
                            		xEnd - xStart <= 10; nextTile = this.scene.tiles[scenePlane][xStart - 1][sceneY]) {
                                xStart--;
                            }
                            for (nextTile = this.scene.tiles[scenePlane][xEnd + 1][sceneY];
                                 xEnd < this.scene.sceneDimensionX && nextTile != null && nextTile.interactiveSideViewHeightOffset < 0 &&
                        				nextTile.interactiveSideViewHeightOffset == tile.interactiveSideViewHeightOffset &&
                        				nextTile.cullZ == tile.cullZ && drawHeight == this.scene.surfaceHeightmap[scenePlane].getHeight(xEnd + 1, sceneY) &&
                        				(xEnd + 1 >= this.scene.sceneDimensionX || drawHeight == this.scene.surfaceHeightmap[scenePlane].getHeight(xEnd + 2, sceneY)) &&
                        				xEnd - xStart <= 10; nextTile = this.scene.tiles[scenePlane][xEnd + 1][sceneY]) {
                                xEnd++;
                            }
                            int heightSouthWest = this.scene.surfaceHeightmap[hasBridge ? scenePlane + 1 : scenePlane].getHeight(xStart, sceneY);
                            int i_14 = heightSouthWest + tile.interactiveSideViewHeightOffset;
                            int heightSouthEast = this.scene.surfaceHeightmap[hasBridge ? scenePlane + 1 : scenePlane].getHeight(xEnd + 1, sceneY);
                            int i_16 = heightSouthEast + tile.interactiveSideViewHeightOffset;
                            int startX = xStart << this.scene.tileUnits;
                            int endX = (xEnd << this.scene.tileUnits) + this.scene.fullTileDimension;
                            int startY = sceneY << this.scene.tileUnits;
                            this.interactiveCulling[this.interactiveCullingCursorPos++] = new CullingCluster(this.scene, 2, scenePlane, startX, endX, endX, startX, heightSouthWest, heightSouthEast, i_16, i_14, startY + tile.cullZ, startY + tile.cullZ, startY + tile.cullZ, startY + tile.cullZ);
                            for (int tilePlane = scenePlane; tilePlane <= zEnd; tilePlane++) {
                                for (int tileX = xStart; tileX <= xEnd; tileX++) {
                                    this.scene.tiles[tilePlane][tileX][sceneY].interactiveSideViewHeightOffset *= -1;
                                }
                            }
                        }
                    }
                }
            }
        }
        this.cullingClustersCleared = true;
    }
}
