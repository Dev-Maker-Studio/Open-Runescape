package com.jagex.game.runetek5.config.rsinterface.itemcontainer;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.obj.ObjType;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.Hashing;
import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

/**
 * 
 * @author Brainpower - 06/05/2022
 *
 */
public class ItemContainer extends Node {

	public static final IterableNodeMap CONTAINER_MAP = new IterableNodeMap(32);
    public static final LRUCache CONTAINER_ITEM_CACHE = new LRUCache(11);
    public int[] itemIds = {-1};
    public int[] amounts = new int[1];

    public static int getAmountAtSlot(int containerId, int slot, boolean negativeKey) {
        ItemContainer container = getContainer(containerId, negativeKey);
        return container == null ? 0 : (slot >= 0 && slot < container.amounts.length ? container.amounts[slot] : 0);
    }

    public static int getItemIdAtSlot(int containerId, int slot, boolean negativeKey) {
        ItemContainer container = getContainer(containerId, negativeKey);
        return container == null ? -1 : (slot >= 0 && slot < container.itemIds.length ? container.itemIds[slot] : -1);
    }

    public static ItemContainer getContainer(int containerId, boolean negativeKey) {
        long key = containerId | (negativeKey ? Integer.MIN_VALUE : 0);
        return (ItemContainer) CONTAINER_MAP.get(key);
    }

    public static void deleteItemContainer(int containerId, boolean negativeKey) {
        ItemContainer container = getContainer(containerId, negativeKey);
        if (container != null) {
            container.unlink();
        }
    }

    public static void setItemInContainer(int containerId, int slot, int itemId, int amount, boolean negativeKey) {
        long key = containerId | (negativeKey ? Integer.MIN_VALUE : 0);
        ItemContainer container = (ItemContainer) CONTAINER_MAP.get(key);
        if (container == null) {
            container = new ItemContainer();
            CONTAINER_MAP.put(container, key);
        }

        if (container.itemIds.length <= slot) {
            int[] itemIds = new int[slot + 1];
            int[] itemAmounts = new int[slot + 1];

            for (int index = 0; index < container.itemIds.length; index++) {
                itemIds[index] = container.itemIds[index];
                itemAmounts[index] = container.amounts[index];
            }

            for (int index = container.itemIds.length; index < slot; index++) {
                itemIds[index] = -1;
                itemAmounts[index] = 0;
            }

            container.itemIds = itemIds;
            container.amounts = itemAmounts;
        }

        container.itemIds[slot] = itemId;
        container.amounts[slot] = amount;
    }
    
	public static void resetContainerMap() {
	    CONTAINER_MAP.clear();
	}

    public static void resetContainer(int containerId, boolean negativeKey) {
        ItemContainer container = getContainer(containerId, negativeKey);
        if (container != null) {
            for (int index = 0; index < container.itemIds.length; index++) {
                container.itemIds[index] = -1;
                container.amounts[index] = 0;
            }
        }
    }
    
    public static int getContainerTotal(int containerId, int itemId, boolean negativeKey) {
        ItemContainer container = getContainer(containerId, negativeKey);
        if (container == null) {
            return 0;
        } 
        if (itemId == -1) {
            return 0;
        } 
        int total = 0;
        for (int index = 0; index < container.amounts.length; index++) {
            if (container.itemIds[index] == itemId) {
                total += container.amounts[index];
            }
        }
        return total;
    }

    public static int getParamBasedContainerTotal(int containerId, int paramId, boolean stackable) {
        ItemContainer container = getContainer(containerId, false);
        if (container == null) {
            return 0;
        } 
        int total = 0;
        for (int index = 0; index < container.itemIds.length; index++) {
            if (container.itemIds[index] >= 0 && container.itemIds[index] < TypeLists.OBJ.totalItemCount) {
                ObjType objType = TypeLists.OBJ.list(container.itemIds[index]);
                int param = objType.getCs2Integer(paramId, TypeLists.PARAMS.list(paramId).defaultInt);
                if (stackable) {
                    total += param * container.amounts[index];
                } else {
                    total += param;
                }
            }
        }
        return total;
    }

    public static int getFreeSlots(int containerId) {
        ItemContainer container = getContainer(containerId, false);
        if (container == null) {
            return TypeLists.CONTAINER.list(containerId).maxItemSize;
        } 
        int freeSlots = 0;
        for (int index = 0; index < container.itemIds.length; index++) {
            if (container.itemIds[index] == -1) {
                freeSlots++;
            }
        }
        freeSlots += TypeLists.CONTAINER.list(containerId).maxItemSize - container.itemIds.length;
        return freeSlots;
    }

    public Model getAnimatedBodyModel(AbstractToolkit toolkit, int functionMask, Sequence sequence, int bastypeId, boolean female, PlayerAppearance playerAppearance) {
        int animateFunctionMask = functionMask;
        BasType basType = null;
        if (bastypeId != -1) {
            basType = TypeLists.BAS.list(bastypeId);
        }

        int[] items = this.itemIds;
        if (basType != null && basType.objVisibility != null) {
            items = new int[basType.objVisibility.length];

            for (int slot = 0; slot < basType.objVisibility.length; slot++) {
                int visibleSlot = basType.objVisibility[slot];
                if (visibleSlot >= 0 && visibleSlot < this.itemIds.length) {
                    items[slot] = this.itemIds[visibleSlot];
                } else {
                    items[slot] = -1;
                }
            }
        }

        if (sequence != null) {
            animateFunctionMask = functionMask | sequence.getFunctionMask();
        }

        long hash = calculateHash(items, bastypeId, playerAppearance != null ? playerAppearance.recolorDstIndices : null, female);
        Model model = (Model) CONTAINER_ITEM_CACHE.get(hash);

        if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), animateFunctionMask) != 0) {
            if (model != null) {
                animateFunctionMask = toolkit.mergeFunctionMasks(animateFunctionMask, model.getFunctionMask());
            }

            int combineFunctionMask = animateFunctionMask;
            boolean modelsMissing = false;

            for (int index = 0; index < items.length; index++) {
                if (items[index] != -1 && !TypeLists.OBJ.list(items[index]).isBodyCached(female, null)) {
                    modelsMissing = true;
                }
            }

            if (modelsMissing) {
                return null;
            }

            Mesh[] meshes = new Mesh[items.length];

            for (int index = 0; index < items.length; index++) {
                if (items[index] != -1) {
                    meshes[index] = TypeLists.OBJ.list(items[index]).getBodyMesh(female, null);
                }
            }

            if (basType != null && basType.objVerticeTransformations != null) {
                for (int index = 0; index < basType.objVerticeTransformations.length; index++) {
                    if (basType.objVerticeTransformations[index] != null && meshes[index] != null) {
                        int translateX = basType.objVerticeTransformations[index][0];
                        int translateY = basType.objVerticeTransformations[index][1];
                        int translateZ = basType.objVerticeTransformations[index][2];
                        int rotateX = basType.objVerticeTransformations[index][3];
                        int rotateY = basType.objVerticeTransformations[index][4];
                        int rotateZ = basType.objVerticeTransformations[index][5];
                        if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
                            meshes[index].rotateWithoutNormals(rotateX, rotateY, rotateZ);
                        }

                        if (translateX != 0 || translateY != 0 || translateZ != 0) {
                            meshes[index].translate(translateX, translateY, translateZ);
                        }
                    }
                }
            }

            Mesh combinedMesh = new Mesh(meshes, meshes.length);
            if (playerAppearance != null) {
                combineFunctionMask = animateFunctionMask | RECOLOR;
            }

            model = toolkit.createModel(combinedMesh, combineFunctionMask, ItemContainerTypeList.preferenceMask, 65, 857);
            if (playerAppearance != null) {
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < PlayerAppearance.recolorPaletteSrc[x].length; y++) {
                        if (playerAppearance.recolorDstIndices[x] < PlayerAppearance.recolorPaletteDst[x][y].length) {
                            model.recolor(PlayerAppearance.recolorPaletteSrc[x][y], PlayerAppearance.recolorPaletteDst[x][y][playerAppearance.recolorDstIndices[x]]);
                        }
                    }
                }
            }

            model.applyShading(animateFunctionMask);
            CONTAINER_ITEM_CACHE.put(model, hash);
        }

        if (sequence == null) {
            return model;
        } 
        Model copiedModel = model.copy((byte) 1, animateFunctionMask, true);
        sequence.animateRotatedModel(copiedModel, 0);
        return copiedModel;
    }

	public static void resetCache(int preferenceMask) {
	    ItemContainerTypeList.preferenceMask = preferenceMask;
	    CONTAINER_ITEM_CACHE.reset();
	}

    public static void resetContainerItemCache() {
        CONTAINER_ITEM_CACHE.reset();
    }
    
    public long calculateHash(int[] items, int bastypeId, int[] recolorDstIndices, boolean female) {
        long[] hashtable = Hashing.CRC64_TABLE;
        long hash = -1L;
        hash = hash >>> 8 ^ hashtable[(int) ((hash ^ (bastypeId >> 8)) & 0xffL)];
        hash = hash >>> 8 ^ hashtable[(int) ((hash ^ bastypeId) & 0xffL)];

        for (int index = 0; index < items.length; index++) {
            hash = hash >>> 8 ^ hashtable[(int) ((hash ^ (items[index] >> 24)) & 0xffL)];
            hash = hash >>> 8 ^ hashtable[(int) ((hash ^ (items[index] >> 16)) & 0xffL)];
            hash = hash >>> 8 ^ hashtable[(int) ((hash ^ (items[index] >> 8)) & 0xffL)];
            hash = hash >>> 8 ^ hashtable[(int) ((hash ^ items[index]) & 0xffL)];
        }

        if (recolorDstIndices != null) {
            for (int index = 0; index < 5; index++) {
                hash = hash >>> 8 ^ hashtable[(int) ((hash ^ recolorDstIndices[index]) & 0xffL)];
            }
        }

        hash = hash >>> 8 ^ hashtable[(int) ((hash ^ (female ? 1 : 0)) & 0xffL)];
        return hash;
    }
}