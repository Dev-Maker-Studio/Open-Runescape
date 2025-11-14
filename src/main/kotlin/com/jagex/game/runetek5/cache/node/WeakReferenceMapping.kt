package com.jagex.game.runetek5.cache.node

import com.jagex.game.runetek5.toolkit.companion.Binding
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

class WeakReferenceMapping(
    mapping: Binding,
    queue: ReferenceQueue<in Binding>,
    @JvmField val slot: Int
) : WeakReference<Binding>(mapping, queue) {
    @JvmField
    var mapping: WeakReferenceMapping? = null
}
