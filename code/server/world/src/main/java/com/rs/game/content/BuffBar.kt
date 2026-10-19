package com.rs.game.content

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varcstr
import org.darkan.core.gameval.gen.Varp
import java.util.IdentityHashMap

object BuffBar {

    @JvmStatic
    fun publish(player: Player) {
        if (player.vars.getVarBit(Varbit.option_buff_bar) == 0) return
        val published = publishedSlots(player)
        layOutSlots(player, published)
        for (slot in 0 until SLOT_COUNT) {
            val entry = published.layout[slot]
            if (entry == null) {
                publishSlot(player, published, slot, null, NO_EXPIRY, 0)
                continue
            }
            val ticks = published.layoutTicks[slot]
            publishSlot(player, published, slot, entry, World.getServerTicks() + ticks, ticksToSeconds(ticks))
        }
        if (published.firstPublish) {
            published.firstPublish = false
            for (varp in SLOT_VARPS)
                player.vars.setVar(varp, player.vars.getVar(varp), forceSend = true)
        }
    }

    // A bar opened afresh or shown again starts every countdown from what its varbits last held,
    // so each slot is resent with the time actually left.
    @JvmStatic
    fun refresh(player: Player) {
        val published = player.tempAttribs.getO<PublishedSlots>(SLOTS_ATTRIBUTE) ?: return
        published.icons.fill(UNPUBLISHED)
        published.firstPublish = true
    }
}

// Poison is the one bar entry that is not an Effect: it runs off its own counter rather than the
// effect map, so it declares its icon here instead of on a constant.
private val POISON_ENTRY = objIcon(Obj.poison, "You are taking poison damage.")

private const val SLOT_COUNT = 28
private const val EMPTY_ICON = 0
private const val NO_EXPIRY = 0L
private const val MAX_SECONDS = 4095L
private const val MILLIS_PER_TICK = 600L
private const val MILLIS_PER_SECOND = 1000L
private const val SLOTS_ATTRIBUTE = "buffBarSlots"

private val SLOT_VARPS = intArrayOf(
    Varp.varp_2689, Varp.varp_2695, Varp.varp_2697, Varp.varp_2698, Varp.varp_2699, Varp.varp_2704,
    Varp.varp_2706, Varp.varp_2707, Varp.varp_2713, Varp.varp_2714, Varp.varp_2626, Varp.varp_2628,
    Varp.varp_2630, Varp.varp_2631, Varp.varp_2638, Varp.varp_2639, Varp.varp_2640, Varp.varp_2641,
    Varp.varp_2642, Varp.varp_2643, Varp.varp_2651, Varp.varp_2652, Varp.varp_2653, Varp.varp_2654,
    Varp.varp_2655, Varp.varp_2656, Varp.varp_2657, Varp.varp_2658
)

private val SLOT_ICON_VARBITS = intArrayOf(
    Varbit.buff_bar_slot_1_icon, Varbit.buff_bar_slot_2_icon, Varbit.buff_bar_slot_3_icon,
    Varbit.buff_bar_slot_4_icon, Varbit.buff_bar_slot_5_icon, Varbit.buff_bar_slot_6_icon,
    Varbit.buff_bar_slot_7_icon, Varbit.buff_bar_slot_8_icon, Varbit.buff_bar_slot_9_icon,
    Varbit.buff_bar_slot_10_icon, Varbit.buff_bar_slot_11_icon, Varbit.buff_bar_slot_12_icon,
    Varbit.buff_bar_slot_13_icon, Varbit.buff_bar_slot_14_icon, Varbit.buff_bar_slot_15_icon,
    Varbit.buff_bar_slot_16_icon, Varbit.buff_bar_slot_17_icon, Varbit.buff_bar_slot_18_icon,
    Varbit.buff_bar_slot_19_icon, Varbit.buff_bar_slot_20_icon, Varbit.buff_bar_slot_21_icon,
    Varbit.buff_bar_slot_22_icon, Varbit.buff_bar_slot_23_icon, Varbit.buff_bar_slot_24_icon,
    Varbit.buff_bar_slot_25_icon, Varbit.buff_bar_slot_26_icon, Varbit.buff_bar_slot_27_icon,
    Varbit.buff_bar_slot_28_icon
)

private val SLOT_SPRITE_VARBITS = intArrayOf(
    Varbit.buff_bar_slot_1_sprite, Varbit.buff_bar_slot_2_sprite, Varbit.buff_bar_slot_3_sprite,
    Varbit.buff_bar_slot_4_sprite, Varbit.buff_bar_slot_5_sprite, Varbit.buff_bar_slot_6_sprite,
    Varbit.buff_bar_slot_7_sprite, Varbit.buff_bar_slot_8_sprite, Varbit.buff_bar_slot_9_sprite,
    Varbit.buff_bar_slot_10_sprite, Varbit.buff_bar_slot_11_sprite, Varbit.buff_bar_slot_12_sprite,
    Varbit.buff_bar_slot_13_sprite, Varbit.buff_bar_slot_14_sprite, Varbit.buff_bar_slot_15_sprite,
    Varbit.buff_bar_slot_16_sprite, Varbit.buff_bar_slot_17_sprite, Varbit.buff_bar_slot_18_sprite,
    Varbit.buff_bar_slot_19_sprite, Varbit.buff_bar_slot_20_sprite, Varbit.buff_bar_slot_21_sprite,
    Varbit.buff_bar_slot_22_sprite, Varbit.buff_bar_slot_23_sprite, Varbit.buff_bar_slot_24_sprite,
    Varbit.buff_bar_slot_25_sprite, Varbit.buff_bar_slot_26_sprite, Varbit.buff_bar_slot_27_sprite,
    Varbit.buff_bar_slot_28_sprite
)

private val SLOT_SECONDS_VARBITS = intArrayOf(
    Varbit.buff_bar_slot_1_seconds, Varbit.buff_bar_slot_2_seconds, Varbit.buff_bar_slot_3_seconds,
    Varbit.buff_bar_slot_4_seconds, Varbit.buff_bar_slot_5_seconds, Varbit.buff_bar_slot_6_seconds,
    Varbit.buff_bar_slot_7_seconds, Varbit.buff_bar_slot_8_seconds, Varbit.buff_bar_slot_9_seconds,
    Varbit.buff_bar_slot_10_seconds, Varbit.buff_bar_slot_11_seconds,
    Varbit.buff_bar_slot_12_seconds, Varbit.buff_bar_slot_13_seconds,
    Varbit.buff_bar_slot_14_seconds, Varbit.buff_bar_slot_15_seconds,
    Varbit.buff_bar_slot_16_seconds, Varbit.buff_bar_slot_17_seconds,
    Varbit.buff_bar_slot_18_seconds, Varbit.buff_bar_slot_19_seconds,
    Varbit.buff_bar_slot_20_seconds, Varbit.buff_bar_slot_21_seconds,
    Varbit.buff_bar_slot_22_seconds, Varbit.buff_bar_slot_23_seconds,
    Varbit.buff_bar_slot_24_seconds, Varbit.buff_bar_slot_25_seconds,
    Varbit.buff_bar_slot_26_seconds, Varbit.buff_bar_slot_27_seconds,
    Varbit.buff_bar_slot_28_seconds
)

private val SLOT_DESCRIPTION_VARCSTRS = intArrayOf(
    Varcstr.buff_bar_slot_1_desc, Varcstr.buff_bar_slot_2_desc, Varcstr.buff_bar_slot_3_desc,
    Varcstr.buff_bar_slot_4_desc, Varcstr.buff_bar_slot_5_desc, Varcstr.buff_bar_slot_6_desc,
    Varcstr.buff_bar_slot_7_desc, Varcstr.buff_bar_slot_8_desc, Varcstr.buff_bar_slot_9_desc,
    Varcstr.buff_bar_slot_10_desc, Varcstr.buff_bar_slot_11_desc, Varcstr.buff_bar_slot_12_desc,
    Varcstr.buff_bar_slot_13_desc, Varcstr.buff_bar_slot_14_desc, Varcstr.buff_bar_slot_15_desc,
    Varcstr.buff_bar_slot_16_desc, Varcstr.buff_bar_slot_17_desc, Varcstr.buff_bar_slot_18_desc,
    Varcstr.buff_bar_slot_19_desc, Varcstr.buff_bar_slot_20_desc, Varcstr.buff_bar_slot_21_desc,
    Varcstr.buff_bar_slot_22_desc, Varcstr.buff_bar_slot_23_desc, Varcstr.buff_bar_slot_24_desc,
    Varcstr.buff_bar_slot_25_desc, Varcstr.buff_bar_slot_26_desc, Varcstr.buff_bar_slot_27_desc,
    Varcstr.buff_bar_slot_28_desc
)

private val BAR_EFFECTS: List<Effect> = Effect.entries.filter { it.isOnBuffBar }

private val ENTRY_EFFECT: Map<BuffBarEntry, Effect> =
    IdentityHashMap<BuffBarEntry, Effect>().apply {
        for (effect in BAR_EFFECTS) effect.buffBar?.let { put(it, effect) }
    }

private class PublishedSlots {
    val icons = IntArray(SLOT_COUNT) { UNPUBLISHED }
    val expiryTicks = LongArray(SLOT_COUNT)
    val layout = arrayOfNulls<BuffBarEntry>(SLOT_COUNT)
    val layoutTicks = LongArray(SLOT_COUNT)
    var firstPublish = true
}

private const val UNPUBLISHED = -1

private fun publishedSlots(player: Player): PublishedSlots {
    val existing = player.tempAttribs.getO<PublishedSlots>(SLOTS_ATTRIBUTE)
    if (existing != null) return existing
    val slots = PublishedSlots()
    player.tempAttribs.setO<PublishedSlots>(SLOTS_ATTRIBUTE, slots)
    return slots
}

// An effect keeps the slot it already holds so nothing on screen jumps when another one is applied;
// the survivors close up over the slots an expiry frees, and new effects take the tail.
private fun layOutSlots(player: Player, published: PublishedSlots) {
    val antipoison = activeAntipoisonEntry(player)
    var next = 0
    for (slot in 0 until SLOT_COUNT) {
        val entry = published.layout[slot] ?: continue
        val ticks = remainingTicks(player, entry, antipoison)
        if (ticks <= 0L) continue
        published.layout[next] = entry
        published.layoutTicks[next] = ticks
        next++
    }
    val placed = next
    for (entry in barEntries(antipoison)) {
        if (next == SLOT_COUNT) break
        if (isLaidOut(published, placed, entry)) continue
        val ticks = remainingTicks(player, entry, antipoison)
        if (ticks <= 0L) continue
        published.layout[next] = entry
        published.layoutTicks[next] = ticks
        next++
    }
    while (next < SLOT_COUNT) {
        published.layout[next] = null
        published.layoutTicks[next] = 0L
        next++
    }
}

private fun barEntries(antipoison: BuffBarEntry?): List<BuffBarEntry> {
    val entries = ArrayList<BuffBarEntry>(BAR_EFFECTS.size + 1)
    for (effect in BAR_EFFECTS) {
        if (effect == Effect.ANTIPOISON) continue
        effect.buffBar?.let { entries.add(it) }
    }
    antipoison?.let { entries.add(it) }
    entries.add(POISON_ENTRY)
    return entries
}

private fun isLaidOut(published: PublishedSlots, count: Int, entry: BuffBarEntry): Boolean {
    for (slot in 0 until count)
        if (published.layout[slot] === entry) return true
    return false
}

// The client runs the countdown itself, so a slot only needs resending when the tick it expires on
// moves. A countdown draining as expected leaves that tick where it was, and costs nothing.
private fun publishSlot(player: Player, published: PublishedSlots, slot: Int, entry: BuffBarEntry?, expiryTick: Long, seconds: Int): Boolean {
    val icon = entry?.icon ?: EMPTY_ICON
    if (published.icons[slot] == icon && published.expiryTicks[slot] == expiryTick) return false
    published.icons[slot] = icon
    published.expiryTicks[slot] = expiryTick
    player.vars.saveVarBit(SLOT_ICON_VARBITS[slot], icon)
    player.vars.saveVarBit(SLOT_SPRITE_VARBITS[slot], if (entry?.isSprite == true) 1 else 0)
    player.vars.saveVarBit(SLOT_SECONDS_VARBITS[slot], seconds)
    player.packets.sendVarcString(SLOT_DESCRIPTION_VARCSTRS[slot], if (entry == null) "" else label(entry))
    // Re-applying an effect republishes the same icon and duration, so the packed varp can be
    // unchanged and setVarBit would drop it, leaving the client on its old deadline. Only this
    // slot moved, so only this varp is forced - forcing all ten multiplies the cost of a slot
    // that republishes often, such as poison while its counter is paused behind an interface.
    player.vars.setVar(SLOT_VARPS[slot], player.vars.getVar(SLOT_VARPS[slot]), forceSend = true)
    return true
}

// The tooltip is the effect's name over its description; the countdown is already on the icon.
private fun label(entry: BuffBarEntry): String = "<col=ffffff>${entryName(entry)}</col><br>${entry.description}"

private fun entryName(entry: BuffBarEntry): String {
    if (entry === POISON_ENTRY) return "Poison"
    val effect = ENTRY_EFFECT[entry] ?: return "Poison immunity"
    val name = effect.displayName ?: effect.name.lowercase().replace('_', ' ')
    return name.replaceFirstChar { it.uppercase() }
}

// Both antifire effects can sit in the map at once; combat resolves super first and only falls
// back to the plain one when it is absent (PlayerCombat.kt:883), so the bar hides the weaker.
private fun remainingTicks(player: Player, entry: BuffBarEntry, antipoison: BuffBarEntry?): Long {
    if (entry === POISON_ENTRY) return player.poison.remainingTicks.toLong()
    if (entry === antipoison) return player.getEffectTicks(Effect.ANTIPOISON)
    val effect = ENTRY_EFFECT[entry] ?: return 0L
    if (effect == Effect.ANTIPOISON) return 0L
    if (effect == Effect.ANTIFIRE && player.hasEffect(Effect.SUPER_ANTIFIRE)) return 0L
    return player.getEffectTicks(effect)
}

private fun ticksToSeconds(ticks: Long): Int {
    val millis = ticks * MILLIS_PER_TICK
    return ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceIn(0L, MAX_SECONDS).toInt()
}
