package org.darkan.world.harness

import org.darkan.core.cache.DefinitionTables
import world.gregs.voidps.cache.definition.data.ItemDefinition
import world.gregs.voidps.cache.definition.data.NPCDefinition
import world.gregs.voidps.cache.definition.data.ObjectDefinition
import world.gregs.voidps.cache.definition.data.VarBitDefinition

/**
 * The definitions the default test suite runs against, in place of the packed cache's.
 *
 * Every id here is written down rather than read from a cache, so `./gradlew build` passes on a
 * machine that has never built one and can never fail because someone's cache is out of date. A
 * test whose subject *is* what the cache holds belongs in `cacheTest`, not here.
 */
object TestDefinitions {

    const val ORE = 440
    const val NOTED_ORE = 441
    const val ARROWS = 882
    const val COINS = 995
    const val SHIELD = 1171
    const val TWO_HANDER = 1307
    const val LOGS = 1511
    const val OAK_LOGS = 1521
    const val WHIP = 4151
    const val TREE = 1276
    const val WIDE_LOC = 1275
    const val MAN = 1

    private const val WEAPON_SLOT = 3
    private const val SHIELD_SLOT = 5
    private const val AMMO_SLOT = 13

    /** [ItemDefinition.isEquipType] reads the second and third wear slots; 5 means two-handed. */
    private const val TWO_HANDED = 5

    private const val STACKS = 1
    private const val HAS_MODEL = 1

    fun install() {
        DefinitionTables.items = items()
        DefinitionTables.objects = objects()
        DefinitionTables.npcs = npcs()
        DefinitionTables.varbits = varbits()
    }

    private fun items(): Array<ItemDefinition> {
        val table = blankItems(WHIP + 1)
        table[ORE] = item(ORE, "Iron ore", noteId = NOTED_ORE)
        table[NOTED_ORE] = item(NOTED_ORE, "Iron ore", stackable = STACKS, notedTemplateId = ORE, noteId = ORE)
        table[ARROWS] = item(ARROWS, "Bronze arrow", stackable = STACKS, wearPos = AMMO_SLOT, option = "Wield")
        table[COINS] = item(COINS, "Coins", stackable = STACKS)
        table[SHIELD] = item(SHIELD, "Wooden shield", wearPos = SHIELD_SLOT, option = "Wear")
        table[TWO_HANDER] = item(TWO_HANDER, "Bronze 2h sword", wearPos = WEAPON_SLOT, wearPos3 = TWO_HANDED, option = "Wield")
        table[LOGS] = item(LOGS, "Logs")
        table[OAK_LOGS] = item(OAK_LOGS, "Oak logs")
        table[WHIP] = item(WHIP, "Abyssal whip", wearPos = WEAPON_SLOT, option = "Wield")
        return table
    }

    private fun blankItems(size: Int) = Array(size) { ItemDefinition(id = it) }

    private fun item(
        id: Int,
        name: String,
        stackable: Int = 0,
        notedTemplateId: Int = -1,
        noteId: Int = -1,
        wearPos: Int = -1,
        wearPos3: Int = -1,
        option: String? = null,
    ) = ItemDefinition(
        id = id,
        name = name,
        stackable = stackable,
        notedTemplateId = notedTemplateId,
        noteId = noteId,
        wearPos = wearPos,
        wearPos3 = wearPos3,
        primaryMaleModel = if (wearPos == -1) -1 else HAS_MODEL,
        primaryFemaleModel = if (wearPos == -1) -1 else HAS_MODEL,
        options = arrayOf(option, null, null, null, "Drop"),
    )

    private fun objects(): Array<ObjectDefinition> {
        val table = Array(TREE + 1) { ObjectDefinition(id = it) }
        table[TREE] = ObjectDefinition(id = TREE, name = "Tree", interactive = 1, options = arrayOf("Chop down", null, null, null, null))
        table[WIDE_LOC] = ObjectDefinition(id = WIDE_LOC, sizeX = 3)
        return table
    }

    /**
     * The layouts the bank and equipment screens pack their state into. Written down because a
     * varbit with no definition writes to nothing, and the containers under test read them back.
     */
    private fun npcs(): Array<NPCDefinition> {
        val table = Array(MAN + 1) { NPCDefinition(id = it) }
        table[MAN] = NPCDefinition(id = MAN, name = "Man", options = arrayOf("Talk-to", null, null, null, null, "Examine"))
        return table
    }

    private fun varbits(): Array<VarBitDefinition> {
        val table = Array(maxOf(BANK_PLACEHOLDERS, KEYWORD_INDEX) + 1) { VarBitDefinition(id = it) }
        for ((index, varbit) in (FIRST_TAB_SIZE..LAST_TAB_SIZE).withIndex()) {
            val low = index % 2 == 0
            table[varbit] = VarBitDefinition(
                id = varbit,
                index = FIRST_TAB_SIZE_VARP + index / 2,
                startBit = if (low) 0 else TAB_SIZE_BITS + 1,
                endBit = if (low) TAB_SIZE_BITS else TAB_SIZE_BITS * 2 + 1,
            )
        }
        table[VIEWING_TAB] = VarBitDefinition(id = VIEWING_TAB, index = BANK_VARP, startBit = 27, endBit = 30)
        table[EQUIP_SCREEN] = VarBitDefinition(id = EQUIP_SCREEN, index = BANK_VARP, startBit = 31, endBit = 31)
        table[BANK_PLACEHOLDERS] = VarBitDefinition(id = BANK_PLACEHOLDERS, index = BANK_VARP, startBit = 20, endBit = 20)
        table[KEYWORD_CATEGORY] = VarBitDefinition(id = KEYWORD_CATEGORY, index = KEYWORD_VARP, startBit = 0, endBit = 5)
        table[KEYWORD_INDEX] = VarBitDefinition(id = KEYWORD_INDEX, index = KEYWORD_VARP, startBit = 6, endBit = 15)
        return table
    }

    private const val FIRST_TAB_SIZE = 4885
    private const val LAST_TAB_SIZE = 4892
    private const val FIRST_TAB_SIZE_VARP = 2715
    private const val TAB_SIZE_BITS = 10
    private const val VIEWING_TAB = 4893
    private const val EQUIP_SCREEN = 4894
    private const val BANK_VARP = 1248
    private const val BANK_PLACEHOLDERS = 11713
    private const val KEYWORD_CATEGORY = 9155
    private const val KEYWORD_INDEX = 9156
    private const val KEYWORD_VARP = 2138
}
