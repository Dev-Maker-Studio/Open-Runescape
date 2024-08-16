package com.jagex.game.runetek5.toolkit.sw3d;

import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;

public class FinalizeCacheSw3d {

    public static int capacity;
    public static boolean shutdown;
    public static final NodeCollection finalizedObjects = new NodeCollection();

    public static synchronized void addFinalizedObject(INativeObject nativeObject) {
        if (!shutdown) {
            if (capacity > 0) {
                ObjectNode objectNode = new ObjectNode(nativeObject);
                finalizedObjects.addFirst(objectNode);
            } else {
                nativeObject.destroy(false);
            }
        }
    }

    public static synchronized void incrementCapacity() {
        capacity++;
    }

    public static synchronized void decrementCapacity() {
        if (--capacity == 0) {
            destroyAll();
        }
    }

    public static synchronized void shutdown() {
        shutdown = true;
    }

    public static synchronized void destroyAll() {
        while (true) {
            ObjectNode objectNode = (ObjectNode) finalizedObjects.popHead();
            if (objectNode == null) {
                return;
            }
            ((INativeObject) objectNode.value).destroy(true);
            objectNode.unlink();
        }
    }

}
