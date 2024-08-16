package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.toolkit.opengl.effect.ProgramAttribute;

/**
 * Helper class for building hardware programs
 * @author Brainpower - 27/05/2020
 *
 */
public class ProgramBuilder {

    public IterableNodeMap<ProgramAttribute> handleMap;
    public ProgramAttribute[] programAttributes;
    public final IProgramAttributeHandler programAttributeHandler;

    public ProgramBuilder(int programIndex, IProgramAttributeHandler programAttributeHandler) {
        int capacity;
        for (capacity = 1; capacity < programIndex; capacity += capacity) { }
        this.handleMap = new IterableNodeMap<ProgramAttribute>(capacity);
        this.programAttributes = new ProgramAttribute[capacity];
        this.programAttributeHandler = programAttributeHandler;
    }

    public ProgramAttribute loadAttribute(String attributeName) {
        long handle = this.programAttributeHandler.getHandle(attributeName);
        for (ProgramAttribute attribute = this.handleMap.get(handle); attribute != null; attribute = this.handleMap.nextInBucket()) {
            if (attribute.attributeName.equals(attributeName)) {
                return attribute;
            }
        }
        return null;
    }

    public void resetAttribute(int index) {
        ProgramAttribute attribute = getAttribute(index);
        if (attribute != null) {
            attribute.unlink();
            this.programAttributes[attribute.programIndex] = null;
        }
    }

    public void addAttribute(int programIndex, String attributeName, ProgramAttribute programAttribute) {
        ProgramAttribute existingAttribute = loadAttribute(attributeName);
        if (existingAttribute != null && existingAttribute.programIndex != programIndex) {
            throw new IllegalArgumentException(attributeName);
        } 
        resetAttribute(programIndex);
        if (programIndex >= this.programAttributes.length) {
            int capacity;
            for (capacity = this.programAttributes.length; programIndex >= capacity; capacity += capacity) { }
            this.handleMap = new IterableNodeMap<ProgramAttribute>(capacity);
            for (int attributeIndex = 0; attributeIndex < this.programAttributes.length; attributeIndex++) {
                ProgramAttribute handleAttribute = this.programAttributes[attributeIndex];
                if (handleAttribute != null) {
                    this.handleMap.put(handleAttribute, handleAttribute.pointer);
                }
            }
            ProgramAttribute[] attributes = new ProgramAttribute[capacity];
            System.arraycopy(this.programAttributes, 0, attributes, 0, this.programAttributes.length);
            this.programAttributes = attributes;
        }
        programAttribute.programIndex = programIndex;
        programAttribute.attributeName = attributeName;
        this.handleMap.put(programAttribute, this.programAttributeHandler.getHandle(attributeName));
        this.programAttributes[programIndex] = programAttribute;
    }

    public ProgramAttribute getAttribute(int index) {
        return index >= this.programAttributes.length ? null : this.programAttributes[index];
    }
}
