package org.darkan.world

import com.rs.game.model.item.ItemsContainer
import org.darkan.core.model.Item
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ItemsContainerDirtyTrackingTest {
    private fun container() = ItemsContainer<Item?>(28, false)

    @Test
    fun `a metadata write marks only the slot holding the item`() {
        val container = container()
        val item = Item(1, 1)
        container[4] = item
        container.clearDirty()

        item.addMetaData("charges", 3)

        assertContentEquals(intArrayOf(4), container.dirtySlots())
    }

    @Test
    fun `an item removed from a container no longer marks it`() {
        val container = container()
        val item = Item(1, 1)
        container[4] = item
        container[4] = null
        container.clearDirty()

        item.addMetaData("charges", 3)

        assertFalse(container.isDirty)
        assertNull(item.owner)
    }

    @Test
    fun `an item moved to another container marks only its new container`() {
        val from = container()
        val to = container()
        val item = Item(1, 1)
        from[2] = item
        to[7] = item
        from.clearDirty()
        to.clearDirty()

        item.setAmount(5)

        assertFalse(from.isDirty)
        assertContentEquals(intArrayOf(7), to.dirtySlots())
        assertSame(to, item.owner)
    }

    @Test
    fun `a stale slot index is corrected from the container contents`() {
        val container = container()
        val item = Item(1, 1)
        container[3] = item
        container.clearDirty()
        item.slot = 9

        item.changeId(2)

        assertContentEquals(intArrayOf(3), container.dirtySlots())
        assertTrue(item.slot == 3)
    }

    @Test
    fun `an item overwritten in its slot stops marking the container`() {
        val container = container()
        val old = Item(1, 1)
        container[0] = old
        container[0] = Item(2, 1)
        container.clearDirty()

        old.addMetaData("charges", 1)

        assertFalse(container.isDirty)
        assertNull(old.owner)
    }
}
