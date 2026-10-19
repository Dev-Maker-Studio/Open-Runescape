package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.toolkit.opengl.effect.ProgramAttribute

/**
 * Helper class for building hardware shader programs with dynamic attribute management.
 *
 * @author Brainpower - 27/05/2020
 */
class ProgramBuilder(
    programIndex: Int,
    @JvmField val programAttributeHandler: IProgramAttributeHandler
) {

    @JvmField var handleMap: IterableNodeMap<ProgramAttribute>
    @JvmField var programAttributes: Array<ProgramAttribute?>

    init {
        var capacity = 1
        while (capacity < programIndex) {
            capacity += capacity
        }
        handleMap = IterableNodeMap(capacity)
        programAttributes = arrayOfNulls(capacity)
    }

    /**
     * Loads an attribute by name from the handle map.
     *
     * @param attributeName The name of the attribute
     * @return The program attribute, or null if not found
     */
    fun loadAttribute(attributeName: String): ProgramAttribute? {
        val handle = programAttributeHandler.getHandle(attributeName)
        var attribute = handleMap.get(handle)
        while (attribute != null) {
            if (attribute.attributeName == attributeName) {
                return attribute
            }
            attribute = handleMap.nextInBucket()
        }
        return null
    }

    /**
     * Resets an attribute at the specified index.
     *
     * @param index The attribute index to reset
     */
    fun resetAttribute(index: Int) {
        val attribute = getAttribute(index)
        if (attribute != null) {
            attribute.unlink()
            programAttributes[attribute.programIndex] = null
        }
    }

    /**
     * Adds or replaces a program attribute.
     *
     * @param programIndex The program index
     * @param attributeName The attribute name
     * @param programAttribute The program attribute to add
     * @throws IllegalArgumentException if an attribute with the same name exists at a different index
     */
    fun addAttribute(programIndex: Int, attributeName: String, programAttribute: ProgramAttribute) {
        val existingAttribute = loadAttribute(attributeName)
        if (existingAttribute != null && existingAttribute.programIndex != programIndex) {
            throw IllegalArgumentException(attributeName)
        }
        resetAttribute(programIndex)
        if (programIndex >= programAttributes.size) {
            var capacity = programAttributes.size
            while (programIndex >= capacity) {
                capacity += capacity
            }
            handleMap = IterableNodeMap(capacity)
            for (attributeIndex in programAttributes.indices) {
                val handleAttribute = programAttributes[attributeIndex]
                if (handleAttribute != null) {
                    handleMap.put(handleAttribute, handleAttribute.pointer)
                }
            }
            val attributes = arrayOfNulls<ProgramAttribute>(capacity)
            System.arraycopy(programAttributes, 0, attributes, 0, programAttributes.size)
            programAttributes = attributes
        }
        programAttribute.programIndex = programIndex
        programAttribute.attributeName = attributeName
        handleMap.put(programAttribute, programAttributeHandler.getHandle(attributeName))
        programAttributes[programIndex] = programAttribute
    }

    /**
     * Gets an attribute by index.
     *
     * @param index The attribute index
     * @return The program attribute, or null if not found
     */
    fun getAttribute(index: Int): ProgramAttribute? {
        return if (index >= programAttributes.size) null else programAttributes[index]
    }
}
