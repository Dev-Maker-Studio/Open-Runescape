package com.jagex.game.runetek5.js5.request;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;

/**
 * [J]
 * @author Steven - 11/10/2023
 *
 */
public abstract class Js5WorkerRequestMessage extends CacheableNode {

	public boolean highPriority;
	public boolean isCompleted;
	public volatile boolean waiting = true;

    public abstract byte[] getPayload();

    public abstract int getCompletion();

}
