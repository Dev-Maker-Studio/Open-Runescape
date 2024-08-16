package com.jagex.game.runetek5.config.rsinterface.itemcontainer;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * 
 * @author Brainpower - 06/05/2022
 *
 */
public class ItemContainerTypeList {

	/**
	 * Cached types
	 */
    public final LRUCache cached = new LRUCache(64);

    /**
     * The resource for shared resourced for config files
     */
    public final Js5ResourceProvider configResource;

    /**
     * The preferences the player has selected, I.E.: textures off
     */
	public static int preferenceMask;

    public ItemContainerTypeList(Js5ResourceProvider configResource) {
        this.configResource = configResource;
        this.configResource.getArchiveFileCount(SharedConfigsType.INVENTORIES.id);
    }

    public ItemContainerType list(int inventoryId) {
        ItemContainerType containerType;
        synchronized (this.cached) {
            containerType = (ItemContainerType) this.cached.get(inventoryId);
        }
        if (containerType != null) {
            return containerType;
        } 
        byte[] data;
        synchronized (this.configResource) {
            data = this.configResource.getFile(SharedConfigsType.INVENTORIES.id, inventoryId);
        }
        containerType = new ItemContainerType();
        if (data != null) {
            containerType.decode(new Packet(data));
        }
        synchronized (this.cached) {
        	this.cached.put(containerType, inventoryId);
            return containerType;
        }
    }
}