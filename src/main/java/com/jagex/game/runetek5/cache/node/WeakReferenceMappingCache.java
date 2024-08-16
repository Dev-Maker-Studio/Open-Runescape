package com.jagex.game.runetek5.cache.node;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;

import com.jagex.game.runetek5.toolkit.companion.Binding;

public class WeakReferenceMappingCache {

    public int occupiedSize;

    public WeakReferenceMapping[] weakReferencedMappings = new WeakReferenceMapping[16];

    public final ReferenceQueue referenceQueue = new ReferenceQueue();

    /**
     * Remove
     */
    public void discardUnreferenced() {
        while (true) {
            WeakReferenceMapping currentElement = (WeakReferenceMapping) this.referenceQueue.poll();
            if (currentElement == null) {
                return;
            }
            int slot = currentElement.slot & this.weakReferencedMappings.length - 1;
            WeakReferenceMapping weakReference = this.weakReferencedMappings[slot];
            if (currentElement == weakReference) {
                this.weakReferencedMappings[slot] = currentElement.mapping;
                this.occupiedSize--;
            } else {
                while (weakReference != null && currentElement != weakReference.mapping) {
                    weakReference = weakReference.mapping;
                }
                if (weakReference != null) {
                    weakReference.mapping = currentElement.mapping;
                    this.occupiedSize--;
                }
            }
        }
    }

    public void identify(Binding mappingToFind) {
        if (mappingToFind == null) {
            throw new NullPointerException();
        } 
        discardUnreferenced();
        int mappingHash = System.identityHashCode(mappingToFind);
        int mappingCacheSlot = mappingHash & this.weakReferencedMappings.length - 1;
        WeakReferenceMapping referencedMapping = this.weakReferencedMappings[mappingCacheSlot];
        if (referencedMapping != null) {
            if (referencedMapping.get() == mappingToFind) {
                this.weakReferencedMappings[mappingCacheSlot] = referencedMapping.mapping;
                this.occupiedSize--;
            } else {
                while (true) {
                    WeakReferenceMapping mapping = referencedMapping.mapping;
                    if (mapping == null) {
                        break;
                    }
                    if (mapping.get() == mappingToFind) {
                        referencedMapping.mapping = mapping.mapping;
                        this.occupiedSize--;
                        break;
                    }
                    referencedMapping = mapping;
                }
            }
        }
    }

    public ArrayList collect() {
        discardUnreferenced();
        ArrayList arraylist = new ArrayList(this.occupiedSize);
        WeakReferenceMapping[] mappings = this.weakReferencedMappings;
        for (int index = 0; index < mappings.length; index++) {
            for (WeakReferenceMapping weakReference = mappings[index]; weakReference != null; weakReference = weakReference.mapping) {
                Object mapping = weakReference.get();
                if (mapping != null) {
                    arraylist.add(mapping);
                }
            }
        }
        return arraylist;
    }

    public void expand() {
        WeakReferenceMapping[] allMappings = this.weakReferencedMappings;
        this.weakReferencedMappings = new WeakReferenceMapping[allMappings.length * 2];
        WeakReferenceMapping[] alteredMappings = allMappings;
        WeakReferenceMapping currentMapping;
        for (int index = 0; index < alteredMappings.length; index++) {
            for (WeakReferenceMapping referenced = alteredMappings[index]; referenced != null; referenced = currentMapping) {
                currentMapping = referenced.mapping;
                int slot = referenced.slot & this.weakReferencedMappings.length - 1;
                referenced.mapping = this.weakReferencedMappings[slot];
                this.weakReferencedMappings[slot] = referenced;
            }
        }
    }

    public void put(Binding mapping) {
        if (mapping == null) {
            throw new NullPointerException();
        }
        discardUnreferenced();
        int mappingHashcode = System.identityHashCode(mapping);
        int slot = mappingHashcode & this.weakReferencedMappings.length - 1;
        WeakReferenceMapping weakReference = this.weakReferencedMappings[slot];
        if (weakReference == null) {
            this.weakReferencedMappings[slot] = new WeakReferenceMapping(mapping, this.referenceQueue, mappingHashcode);
            this.occupiedSize++;
            if (this.occupiedSize >= this.weakReferencedMappings.length) {
                expand();
            }
        } else {
            while (weakReference.get() != mapping) {
                if (weakReference.mapping == null) {
                    weakReference.mapping = new WeakReferenceMapping(mapping, this.referenceQueue, mappingHashcode);
                    this.occupiedSize++;
                    if (this.occupiedSize >= this.weakReferencedMappings.length) {
                        expand();
                    }
                    break;
                }
                weakReference = weakReference.mapping;
            }
        }
    }
}
