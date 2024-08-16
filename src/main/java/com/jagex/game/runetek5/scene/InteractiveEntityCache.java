package com.jagex.game.runetek5.scene;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.entity.RenderedEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;

/**
 * 
 * @author Steven - 20/09/2023
 *
 */
public class InteractiveEntityCache {

    public final List<RenderedEntity> entities = new LinkedList<>();

    /**
     * Always false
     */
    public final boolean removeDuplicates;

    /**
     * Always 0
     */
    public int hoverOffsetX;

    /**
     * Always 0
     */
    public int hoverOffsetY;

	public InteractiveEntityCache() {
        this.removeDuplicates = false;
    }

    public void clear() {
        Iterator<RenderedEntity> interactivesIterator = this.entities.iterator();
        while (interactivesIterator.hasNext()) {
            RenderedEntity interactive = interactivesIterator.next();
            interactivesIterator.remove();
            RenderedEntityCache.clear(interactive);
        }
    }

    public void add(RenderedEntity addedReference) {
        SceneGraphEntity addition = addedReference.entity;
        boolean skip = true;
        BoundingBoxPolygon[] boundingBox = addedReference.entity.boundingBoxPolygons;
        for (int index = 0; index < boundingBox.length; index++) {
            if (boundingBox[index].positionCalculated) {
                skip = false;
                break;
            }
        }
        if (!skip) {
            addedReference.parent = this;
            if (this.removeDuplicates) {
                Iterator<RenderedEntity> interactivesIterator = this.entities.iterator();
                while (interactivesIterator.hasNext()) {
                	RenderedEntity prexistingInteractive = interactivesIterator.next();
                    if (addition == prexistingInteractive.entity) {
                        interactivesIterator.remove();
                        RenderedEntityCache.clear(prexistingInteractive);
                    }
                }
            }
            ListIterator<RenderedEntity> listIterator = this.entities.listIterator();
            while (listIterator.hasNext()) {
            	RenderedEntity renderedObject = listIterator.next();
                if (addition.floorDepth >= renderedObject.entity.floorDepth) {
                    listIterator.previous();
                    listIterator.add(addedReference);
                    return;
                }
            }
            this.entities.add(addedReference);
        }
    }
}
