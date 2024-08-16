package com.jagex.game.runetek5.entity.hint;

import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.hint.HintTrail;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.particle.Silhouette;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.movement.CoordinateSpace;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class HintTrailGraphNode extends AbstractInteractiveEntity {

    public Model model;
    public final HintTrail hintTrail;

    public HintTrailGraphNode(Scene scene, HintTrail hintTrail, int plane, int collisionPlane, int x, int y, int z) {
        super(scene, plane, collisionPlane, x, y, z, x >> 9, x >> 9, z >> 9, z >> 9, false, (byte) 0);
        this.hintTrail = hintTrail;
    }

    @Override
    public boolean calculateProjection(AbstractToolkit graphicalrenderer1, int i_2, int i_3) {
        return false;
    }

    @Override
    public boolean hasTileShadow() {
        return false;
    }

    @Override
    public boolean disregardTextureMovement() {
        return this.model == null || !this.model.hasAnimatedTexture();
    }

    @Override
    public boolean hasAlpha() {
        return this.model != null && this.model.accountForLayers();
    }

    @Override
    public void castShadow(SceneGraphEntity class521Sub12, int i_3, int i_4, int i_5, boolean bool6) {
    }

    @Override
    public void removeTileShadow() {
    }

    @Override
    public RenderedEntity draw(AbstractToolkit toolkit) {
        if (this.model == null) {
            this.model = this.hintTrail.getStationaryModel(toolkit);
        }

        if (this.model != null) {
            Matrix43 matrix44var3 = toolkit.getRenderingMatrix();
            Matrix43 matrix44var4 = getProjectionMatrix();
            CoordinateSpace abs = getAbsolutePosition();
            matrix44var3.copyFrom(matrix44var4);
            Tile tile = super.scene.tiles[super.plane][(int) abs.coords.x >> 9][(int) abs.coords.z >> 9];
            if (tile != null && tile.groundDecoration != null) {
                matrix44var3.translate(0.0F, (-tile.groundDecoration.floorOffsetY), 0.0F);
            }

            this.model.drawMatrixThreaded(matrix44var3, null, ModelFunctionMaskGL.EMPTY);
        }

        return null;
    }
    
    @Override
    public int getMinY() {
        return this.model != null ? this.model.getMinY() : 0;
    }

    @Override
    public void particleCycle(AbstractToolkit toolkit) {
    }

    @Override
    public Silhouette getAxisAlignedBoundingBox(AbstractToolkit toolkit) {
        return null;
    }
}
