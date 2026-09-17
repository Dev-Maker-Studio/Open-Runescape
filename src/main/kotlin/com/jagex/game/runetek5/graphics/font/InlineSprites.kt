package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.graphics.ifcomponent.Item
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.Utils

object InlineSprites {
    private const val GRAPHIC_TAG = "sprite="
    private const val OBJ_TAG = "obj="
    private const val OBJ_WIDTH = 36
    private const val OBJ_HEIGHT = 32
    private const val OBJ_OUTLINE = 1
    private const val OBJ_SHADOW = -0xcccccd

    private class Tag(val obj: Boolean, val id: Int, val arg: Int)

    private val tags = HashMap<String, Tag?>()
    private val graphicTypes = HashMap<Long, SpriteType>()
    private val graphicSprites = HashMap<Long, NativeSprite>()
    private var spriteToolkit: AbstractToolkit? = null

    @JvmStatic
    fun isTag(tag: String): Boolean = tag.startsWith(GRAPHIC_TAG) || tag.startsWith(OBJ_TAG)

    @JvmStatic
    fun width(tag: String): Int {
        val parsed = parse(tag) ?: return 0
        return size(parsed)?.first ?: 0
    }

    @JvmStatic
    fun draw(toolkit: AbstractToolkit, tag: String, x: Int, baselineY: Int, ascent: Int, lineHeight: Int): Int {
        val parsed = parse(tag) ?: return 0
        val (width, height) = size(parsed) ?: return 0
        sprite(toolkit, parsed)?.draw(x, baselineY - ascent - (height - lineHeight) / 2, 1, 0, 1)
        return width
    }

    @JvmStatic
    fun extents(line: String, lineHeight: Int, out: IntArray) {
        var tallest = 0
        var start = line.indexOf('<')
        while (start != -1) {
            val end = line.indexOf('>', start)
            if (end == -1) break
            val tag = line.substring(start + 1, end)
            if (isTag(tag)) parse(tag)?.let { size(it) }?.let { tallest = maxOf(tallest, it.second) }
            start = line.indexOf('<', end)
        }
        val overflow = maxOf(0, tallest - lineHeight)
        out[0] = overflow / 2
        out[1] = overflow - overflow / 2
    }

    private fun parse(tag: String): Tag? = tags.getOrPut(tag) {
        val obj = tag.startsWith(OBJ_TAG)
        val args = tag.substring(if (obj) OBJ_TAG.length else GRAPHIC_TAG.length).split(',')
        try {
            val id = Utils.parseInt(args[0])
            val arg = if (args.size > 1) Utils.parseInt(args[1]) else if (obj) 1 else 0
            Tag(obj, id, arg)
        } catch (_: Exception) {
            null
        }
    }

    private fun size(tag: Tag): Pair<Int, Int>? {
        if (tag.obj) return OBJ_WIDTH to OBJ_HEIGHT
        val type = graphicType(tag) ?: return null
        return type.widthSum() to type.heightSum()
    }

    private fun graphicKey(tag: Tag) = (tag.id.toLong() shl 32) or tag.arg.toLong()

    private fun graphicType(tag: Tag): SpriteType? {
        val key = graphicKey(tag)
        graphicTypes[key]?.let { return it }
        val frames = SpriteType.decodeGroup(StaticMethods.SPRITES_INDEX, tag.id) ?: return null
        val frame = frames.getOrNull(tag.arg) ?: return null
        graphicTypes[key] = frame
        return frame
    }

    private fun sprite(toolkit: AbstractToolkit, tag: Tag): NativeSprite? {
        if (tag.obj) {
            val sprite = TypeLists.OBJ!!.createNativeSprite(toolkit, tag.id, tag.arg, OBJ_OUTLINE, OBJ_SHADOW, Component.StackMode.WHEN_MULTIPLE, null)
            if (sprite == null) {
                Component.ITEMS.addFirst(Item(tag.id, tag.arg, OBJ_OUTLINE, OBJ_SHADOW, Component.StackMode.WHEN_MULTIPLE, false))
            }
            return sprite
        }
        if (spriteToolkit !== toolkit) {
            graphicSprites.clear()
            spriteToolkit = toolkit
        }
        val key = graphicKey(tag)
        graphicSprites[key]?.let { return it }
        val type = graphicType(tag) ?: return null
        return toolkit.generateSprite(type, true).also { graphicSprites[key] = it }
    }
}
