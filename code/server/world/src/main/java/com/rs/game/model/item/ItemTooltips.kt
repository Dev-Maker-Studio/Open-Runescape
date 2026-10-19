package com.rs.game.model.item

import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import org.darkan.core.model.Item

enum class TooltipSection { CHARGES, CONTENTS, STATE, FOOTER }

fun interface ItemTooltip {
    fun text(player: Player, item: Item): String?
}

fun interface MetadataTooltip {
    fun text(player: Player, item: Item, value: Any): String?
}

object ItemTooltips {
    const val METADATA_KEY = "tooltip"
    private const val NAME_COLOUR = "ff9040"
    private const val VALUE_COLOUR = "2dc3ff"

    private class Entry<P>(val section: TooltipSection, val provider: P)

    private val byObj = HashMap<Int, MutableList<Entry<ItemTooltip>>>()
    private val byMetadata = HashMap<String, MutableList<Entry<MetadataTooltip>>>()
    private val global = ArrayList<Entry<ItemTooltip>>()

    init {
        metadata(METADATA_KEY, TooltipSection.FOOTER) { _, _, value -> value as? String }
    }

    @JvmStatic
    @JvmOverloads
    fun register(section: TooltipSection = TooltipSection.CONTENTS, tooltip: ItemTooltip) {
        global += Entry(section, tooltip)
    }

    @JvmStatic
    @JvmOverloads
    fun register(objs: IntArray, section: TooltipSection = TooltipSection.CONTENTS, tooltip: ItemTooltip) {
        for (obj in objs) byObj.getOrPut(obj) { ArrayList() } += Entry(section, tooltip)
    }

    @JvmStatic
    @JvmOverloads
    fun metadata(key: String, section: TooltipSection = TooltipSection.CHARGES, tooltip: MetadataTooltip) {
        byMetadata.getOrPut(key) { ArrayList() } += Entry(section, tooltip)
    }

    @JvmStatic
    fun value(value: Any) = "<col=$VALUE_COLOUR>${if (value is Number) Utils.formatNumber(value.toInt()) else value}</col>"

    @JvmStatic
    @JvmOverloads
    fun count(label: String, current: Int, max: Int? = null) =
        "$label: ${value(current)}" + if (max != null) "/${Utils.formatNumber(max)}" else ""

    @JvmStatic
    fun of(player: Player, item: Item?): String? {
        if (item == null) return null
        val lines = ArrayList<Pair<TooltipSection, String>>()
        byObj[item.id]?.forEach { entry -> entry.provider.text(player, item)?.let { lines += entry.section to it } }
        global.forEach { entry -> entry.provider.text(player, item)?.let { lines += entry.section to it } }
        item.getMetaData()?.forEach { (key, value) ->
            byMetadata[key]?.forEach { entry -> entry.provider.text(player, item, value)?.let { lines += entry.section to it } }
        }
        if (lines.isEmpty()) return null
        lines.sortBy { it.first }
        return "<col=$NAME_COLOUR>${item.name}</col><br>" + lines.joinToString("<br>") { it.second }
    }
}
