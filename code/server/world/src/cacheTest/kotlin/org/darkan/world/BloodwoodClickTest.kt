package org.darkan.world

import com.rs.game.content.skills.woodcutting.bloodwoodTreeNear
import com.rs.game.map.ChunkManager
import com.rs.plugin.PluginManager
import com.rs.plugin.events.ObjectClickEvent
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.world.harness.WorldHarness
import world.gregs.voidps.type.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BloodwoodClickTest {

    private fun chopAt(x: Int, y: Int): Int {
        WorldHarness.bootWithCacheDefinitions()
        val h = WorldHarness.login("bloodwood_$x")
        val p = h.player
        p.vars.saveVarBit(Varbit.sniper_spoken_to_mami_rimba, 1)
        p.skills.set(Constants.WOODCUTTING, 99)
        p.inventory.addItem(Obj.rune_axe, 1)
        p.setTile(Tile.of(x, y - 1, 0))
        p.loadMapRegions()
        WorldHarness.tick(10)
        val tile = Tile.of(x, y, 0)
        val obj = ChunkManager.getChunk(tile.getChunkId()).getBaseObjects(tile).filterNotNull().first()
        val originalId = obj.id
        println("PROBE chopAt site=$x,$y objId=${obj.id}")
        PluginManager.handle(ObjectClickEvent(p, obj, 1, true))
        WorldHarness.tick(200)
        val logs = p.inventory.getNumberOf(Obj.sniper_bloodwood_logs)
        println("PROBE chopAt site=$x,$y logs=$logs")
        if (obj.id != originalId)
            obj.changeId(originalId)
        WorldHarness.logout(h)
        return logs
    }

    @Test
    fun `chopping a placeholder site yields logs`() {
        assertTrue(chopAt(3052, 3912) > 0, "the placeholder trunk site did not yield bloodwood logs")
    }

    @Test
    fun `Mami Rimba's decoration tree cannot be chopped`() {
        assertEquals(0, chopAt(3059, 3517), "Mami Rimba's private bloodwood tree yielded logs")
    }

    @Test
    fun `a grown varbit tree counts as a bloodwood tree nearby`() {
        WorldHarness.bootWithCacheDefinitions()
        val h = WorldHarness.login("bloodwood_near")
        val p = h.player
        p.setTile(Tile.of(3052, 3911, 0))
        p.loadMapRegions()
        WorldHarness.tick(10)
        assertNotNull(bloodwoodTreeNear(p), "a grown varbit bloodwood tree was not recognised near the player")
        WorldHarness.logout(h)
    }
}
