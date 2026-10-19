package com.jagex.game.runetek5.cache.node

import java.lang.ref.ReferenceQueue
import java.util.ArrayList

import com.jagex.game.runetek5.toolkit.companion.Binding

class WeakReferenceMappingCache {

    @JvmField var occupiedSize = 0

    @JvmField var weakReferencedMappings = arrayOfNulls<WeakReferenceMapping>(16)

    @JvmField val referenceQueue = ReferenceQueue<Any>()

    /**
     * Remove
     */
    fun discardUnreferenced() {
        while (true) {
            val currentElement = referenceQueue.poll() as WeakReferenceMapping? ?: return
            val slot = currentElement.slot and weakReferencedMappings.size - 1
            var weakReference = weakReferencedMappings[slot]
            if (currentElement === weakReference) {
                weakReferencedMappings[slot] = currentElement.mapping
                occupiedSize--
            } else {
                while (weakReference != null && currentElement !== weakReference.mapping) {
                    weakReference = weakReference.mapping
                }
                if (weakReference != null) {
                    weakReference.mapping = currentElement.mapping
                    occupiedSize--
                }
            }
        }
    }

    fun identify(mappingToFind: Binding?) {
        if (mappingToFind == null) {
            throw NullPointerException()
        }
        discardUnreferenced()
        val mappingHash = System.identityHashCode(mappingToFind)
        val mappingCacheSlot = mappingHash and weakReferencedMappings.size - 1
        var referencedMapping = weakReferencedMappings[mappingCacheSlot]
        if (referencedMapping != null) {
            if (referencedMapping.get() === mappingToFind) {
                weakReferencedMappings[mappingCacheSlot] = referencedMapping.mapping
                occupiedSize--
            } else {
                while (true) {
                    val mapping = referencedMapping!!.mapping ?: break
                    if (mapping.get() === mappingToFind) {
                        referencedMapping.mapping = mapping.mapping
                        occupiedSize--
                        break
                    }
                    referencedMapping = mapping
                }
            }
        }
    }

    fun collect(): ArrayList<Any> {
        discardUnreferenced()
        val arraylist = ArrayList<Any>(occupiedSize)
        val mappings = weakReferencedMappings
        for (index in mappings.indices) {
            var weakReference = mappings[index]
            while (weakReference != null) {
                val mapping = weakReference.get()
                if (mapping != null) {
                    arraylist.add(mapping)
                }
                weakReference = weakReference.mapping
            }
        }
        return arraylist
    }

    fun expand() {
        val allMappings = weakReferencedMappings
        weakReferencedMappings = arrayOfNulls(allMappings.size * 2)
        val alteredMappings = allMappings
        for (index in alteredMappings.indices) {
            var referenced = alteredMappings[index]
            while (referenced != null) {
                val currentMapping = referenced.mapping
                val slot = referenced.slot and weakReferencedMappings.size - 1
                referenced.mapping = weakReferencedMappings[slot]
                weakReferencedMappings[slot] = referenced
                referenced = currentMapping
            }
        }
    }

    fun put(mapping: Binding?) {
        if (mapping == null) {
            throw NullPointerException()
        }
        discardUnreferenced()
        val mappingHashcode = System.identityHashCode(mapping)
        val slot = mappingHashcode and weakReferencedMappings.size - 1
        var weakReference = weakReferencedMappings[slot]
        if (weakReference == null) {
            weakReferencedMappings[slot] = WeakReferenceMapping(mapping, referenceQueue, mappingHashcode)
            occupiedSize++
            if (occupiedSize >= weakReferencedMappings.size) {
                expand()
            }
        } else {
            while (weakReference!!.get() !== mapping) {
                if (weakReference.mapping == null) {
                    weakReference.mapping = WeakReferenceMapping(mapping, referenceQueue, mappingHashcode)
                    occupiedSize++
                    if (occupiedSize >= weakReferencedMappings.size) {
                        expand()
                    }
                    break
                }
                weakReference = weakReference.mapping!!
            }
        }
    }
}
