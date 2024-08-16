package com.jagex.game.runetek5.graphics.model.billboard;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class BillboardTypeList {

	
	public static final LRUCache BILLBOARD_CACHE = new LRUCache(64);
	public static Js5ResourceProvider BILLBOARD_RESOURCE;
	
	public static void initialize(Js5ResourceProvider billboardsResource) {
	    BILLBOARD_RESOURCE = billboardsResource;
	}
	
	public static BillboardType list(int id) {
	    BillboardType billboardType = (BillboardType) BillboardTypeList.BILLBOARD_CACHE.get(id);
	    if (billboardType == null) {
	        byte[] billboardData = BillboardTypeList.BILLBOARD_RESOURCE.getFile(0, id);
	        billboardType = new BillboardType();
	        if (billboardData != null) {
	            billboardType.decode(new Packet(billboardData));
	        }
	        BillboardTypeList.BILLBOARD_CACHE.put(billboardType, id);
	    }
	    return billboardType;
	}
}
