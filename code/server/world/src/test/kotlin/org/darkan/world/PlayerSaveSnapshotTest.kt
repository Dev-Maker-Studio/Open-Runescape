package org.darkan.world

import com.rs.db.collection.PlayerManager
import org.darkan.core.JsonFileManager
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import org.darkan.world.harness.WorldHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlayerSaveSnapshotTest {

    private class CapturingSaves : PlayerManager() {
        val deferred = ArrayDeque<Runnable>()
        var written: String? = null

        override fun initCollection() {}
        override fun execute(task: Runnable) { deferred.addLast(task) }
        override fun write(username: String, json: String) { written = json }
    }

    @Test
    fun `a save writes the player as it was when save was called, not as it is when the write runs`() {
        val harnessPlayer = WorldHarness.login("saver")
        val player = harnessPlayer.player
        try {
            player.inventory.addItem(Item(Obj.coins, 1000))
            val atSaveTime = JsonFileManager.toJson(player)
            val saves = CapturingSaves()

            saves.save(player)
            player.inventory.reset()
            player.equipment.reset()
            saves.deferred.removeFirst().run()

            assertEquals(atSaveTime, assertNotNull(saves.written))
        } finally {
            WorldHarness.logout(harnessPlayer)
        }
    }
}
