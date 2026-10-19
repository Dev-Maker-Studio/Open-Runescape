package com.rs.game.content

/**
 * What the buff bar shows for an effect. The icon id travels to the client as-is and is rendered
 * straight from the wire, so the cache holds no table of its own and an effect joins the bar by
 * declaring one of these and nothing else.
 */
class BuffBarEntry(val icon: Int, val isSprite: Boolean, val description: String)

/** An effect drawn with an item's icon, the way a potion's own vial identifies its buff. */
fun objIcon(icon: Int, description: String) = BuffBarEntry(icon, false, description)

/** An effect drawn with a sprite, for the spells and states no item stands for. */
fun spriteIcon(icon: Int, description: String) = BuffBarEntry(icon, true, description)
