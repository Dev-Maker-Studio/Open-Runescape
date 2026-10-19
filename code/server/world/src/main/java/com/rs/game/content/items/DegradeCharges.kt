package com.rs.game.content.items

import com.rs.game.content.ItemConstants.ItemDegrade
import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.model.Item

const val DEGRADE_CHARGES_KEY = "combatCharges"

object DegradeStages {
    private val stages = HashMap<Int, IntArray>()
    private val bases = HashMap<Int, Int>()
    private val deathBreaks = HashMap<Int, Int>()

    fun register(base: Int, lowerStages: IntArray, broken: Int) {
        stages[base] = intArrayOf(base, *lowerStages)
        (lowerStages + broken).forEach { bases[it] = base }
    }

    fun registerDeathBreak(obj: Int, broken: Int) {
        deathBreaks[obj] = broken
    }

    @JvmStatic
    fun baseId(id: Int) = bases[id] ?: id

    @JvmStatic
    fun deathBroken(id: Int) = deathBreaks[id] ?: -1

    fun stageFor(id: Int, percent: Int): Int {
        val chain = stages[baseId(id)] ?: return id
        return chain[((100 - percent) * chain.size / 100).coerceIn(0, chain.lastIndex)]
    }
}

fun Item.degradeChargePercent(): Int? {
    val charges = getMetaDataI(DEGRADE_CHARGES_KEY)
    if (charges < 0) return null
    val max = ItemDegrade.forId(DegradeStages.baseId(id))?.defaultCharges ?: return null
    if (max <= 0) return null
    return ((charges * 100 + max - 1) / max).coerceIn(0, 100)
}

fun Item.drainDegradeCharge() {
    val metadata = getMetaData() ?: return
    val before = degradeChargePercent()
    metadata[DEGRADE_CHARGES_KEY] = getMetaDataI(DEGRADE_CHARGES_KEY) - 1
    val after = degradeChargePercent()
    if (after == before) return
    markChanged()
    after?.let { DegradeStages.stageFor(id, it) }?.takeIf { it != id }?.let(::changeId)
}

@ServerStartupEvent
fun mapDegradeChargeTooltip() {
    ItemTooltips.metadata(DEGRADE_CHARGES_KEY) { _, item, _ ->
        item.degradeChargePercent()?.let { "Charge: ${ItemTooltips.value("$it%")}" }
    }
}
