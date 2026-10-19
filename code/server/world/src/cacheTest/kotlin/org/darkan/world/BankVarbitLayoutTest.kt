package org.darkan.world

import org.darkan.core.Utils
import org.darkan.core.cache.VarBitDefinitions
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import org.darkan.world.harness.WorldHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The bank's toggles share a varp with the rest of the bank's state, so their bits must not collide. */
class BankVarbitLayoutTest {

    @Test
    fun thePlaceholderToggleOwnsItsBitAlone() {
        WorldHarness.bootWithCacheDefinitions()
        val placeholders = VarBitDefinitions.getDefs(Varbit.bank_placeholders)
        assertEquals(Varp.varp_1248, placeholders.index)
        assertEquals(placeholders.startBit, placeholders.endBit, "a toggle is one bit wide")

        val bit = placeholders.startBit
        for (id in 0 until Utils.getVarbitDefinitionsSize()) {
            if (id == Varbit.bank_placeholders) continue
            val other = VarBitDefinitions.getDefs(id)
            if (other.index != placeholders.index) continue
            assertTrue(bit < other.startBit || bit > other.endBit, "bit $bit overlaps varbit $id (${other.startBit}-${other.endBit})")
        }
    }
}
