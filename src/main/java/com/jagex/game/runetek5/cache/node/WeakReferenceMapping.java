package com.jagex.game.runetek5.cache.node;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.jagex.game.runetek5.toolkit.companion.Binding;

public class WeakReferenceMapping extends WeakReference {

	public WeakReferenceMapping mapping;
    public final int slot;

    public WeakReferenceMapping(Binding mapping, ReferenceQueue queue, int slot) {
        super(mapping, queue);
        this.slot = slot;
    }

}
