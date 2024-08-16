package com.jagex.game.runetek5.entity;

import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

/**
jag::graphics::GraphEntity::LineIntersectionSupport(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     .text  00000000000ADFD0 00000003                   R . . . . . .
jag::graphics::GraphEntity::LowerCurrentLOD(int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  00000000000ADFB0 00000002                   R . . . . . .
jag::graphics::GraphEntity::OccludesView(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .text  00000000000ADFE0 00000003                   R . . . . . .
jag::graphics::GraphEntity::TestIntersection(jag::math::Vector3 const&,jag::math::Vector3 const&,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         .text  00000000000ADFC0 00000003                   R . . . . . .
jag::graphics::GraphEntity::~GraphEntity() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000F7F50 0000000B                   R . . . . . .
jag::graphics::GraphEntity::~GraphEntity() [clone]     
 */
public abstract class SceneGraphEntity extends SceneGraphNode {

    public byte plane;
    public byte collisionPlane;
    public Scene scene;
    public BoundingBoxPolygon[] boundingBoxPolygons;
    public SceneGraphEntity child;
    public int floorDepth;

    protected SceneGraphEntity(Scene scene) {
        this.scene = scene;
    }

    public abstract boolean calculateProjection(AbstractToolkit toolkit, int mouseX, int mouseY);

    public abstract void removeTileShadow();

    public abstract boolean hasTileShadow();

    public abstract boolean disregardTextureMovement();

    // FIXME: find correct name
    // has_alpha
    // account_for_layers
    // account_for_priorities
    public abstract boolean hasAlpha();

    public abstract RenderedEntity draw(AbstractToolkit toolkit);

    public abstract void particleCycle(AbstractToolkit toolkit);

    public abstract Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit);

    public abstract int getMinY();

    public int getMaxSizeShift() {
        return 0;
    }

    public int getIconHeight() {
        return -getMinY();
    }

    public int getActiveLights(int x, int y, PointLight[] lights) {
        long tileLightBaseUid = this.scene.tileLightUids[this.plane][x][y];
        long uidShift = 0L;
        int lightCount;
        for (lightCount = 0; uidShift <= 48L; uidShift += 16L) {
            int slot = (int) (tileLightBaseUid >> uidShift & 65535L);
            if (slot <= 0) {
                break;
            }
            lights[lightCount++] = this.scene.lightSlotOccupant[slot - 1].pointLight;
        }
        for (int lightIndex = lightCount; lightIndex < 4; lightIndex++) {
            lights[lightIndex] = null;
        }
        return lightCount;
    }
    
    protected void setupBoundingBox(int amount) {
        this.boundingBoxPolygons = new BoundingBoxPolygon[amount];
        for (int count = 0; count < this.boundingBoxPolygons.length; count++) {
            this.boundingBoxPolygons[count] = new BoundingBoxPolygon();
        }
    }

    public abstract void castShadow(SceneGraphEntity node, int width, int length, int height, boolean var6);

    public abstract boolean canRender();

    public abstract int getActiveLights(PointLight[] nodes);

    public abstract boolean isSoftHidden(AbstractToolkit toolkit);
}
