package org.darkan.world

import com.rs.game.World
import com.rs.plugin.PluginManager
import com.rs.plugin.events.NPCClickEvent
import com.rs.plugin.kts.interceptNpcClick
import com.rs.plugin.kts.onNpcClick
import org.darkan.world.harness.TestDefinitions
import org.darkan.world.harness.WorldHarness
import kotlin.test.Test
import kotlin.test.assertEquals

class NpcClickInterceptTest {

    @Test
    fun `a claiming interceptor keeps the click from the npc's handler and a declining one passes it on`() {
        val harness = WorldHarness.login("intercept")
        val npc = World.spawnNPC(TestDefinitions.MAN, harness.player.tile, true, false, null)
        var claiming = false
        var claimed = 0
        var handled = 0
        interceptNpcClick("Talk-to") { _, clicked -> (claiming && clicked === npc).also { if (it) claimed++ } }
        onNpcClick(TestDefinitions.MAN) { handled++ }
        fun click() = NPCClickEvent(harness.player, npc, 1, true).let { it.intercepted() || PluginManager.handle(it) }

        click()
        assertEquals(0 to 1, claimed to handled)

        claiming = true
        click()
        assertEquals(1 to 1, claimed to handled)

        npc.finish()
        WorldHarness.logout(harness)
    }
}
