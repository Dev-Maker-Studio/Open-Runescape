package com.jagex.game.runetek5.entity;

import com.jagex.game.runetek5.scene.InteractiveEntityCache;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class RenderedEntity {

    /**
     * Only set for interactives
     */
    public SceneGraphEntity entity;
    public boolean interactive;
    public InteractiveEntityCache parent;

    public boolean hovered(AbstractToolkit toolkit, int mouseX, int mouseY) {
        int sizeShift = this.entity.getMaxSizeShift();
        if (this.entity.boundingBoxPolygons != null) {
            for (int index = 0; index < this.entity.boundingBoxPolygons.length; index++) {
                this.entity.boundingBoxPolygons[index].size <<= sizeShift;
                if (this.entity.boundingBoxPolygons[index].hovered(mouseX + this.parent.hoverOffsetX, mouseY + this.parent.hoverOffsetY)
                		&& this.entity.calculateProjection(toolkit, mouseX, mouseY)) {
                    this.entity.boundingBoxPolygons[index].size >>= sizeShift;
                    return true;
                }
                this.entity.boundingBoxPolygons[index].size >>= sizeShift;
            }
        }
        return false;
    }
}
