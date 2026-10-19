package com.rs.game.content.quests.rovingelves

import com.rs.game.content.skills.magic.Rune
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.content.skills.summoning.Pouch
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.model.Item

class GlarialsTombController : Controller() {
    override fun start() { player.prayer.closeAllPrayers() }
    override fun process() {
        if (!player.inGlarialsTomb()) { removeController(); return }
        player.prayer.closeAllPrayers()
    }
    override fun login(): Boolean {
        if (!player.inGlarialsTomb()) return true
        start()
        return false
    }
    override fun logout() = false
    override fun onTeleported(type: TeleType) { if (!player.inGlarialsTomb()) removeController() }
    override fun canSummonFamiliar(): Boolean { player.sendMessage("You cannot summon a familiar in Glarial's tomb."); return false }
    override fun canEquip(slotId: Int, itemId: Int): Boolean {
        if (!forbiddenTombItem(Item(itemId, 1))) return true
        player.sendMessage("You cannot use weapons or armour in Glarial's tomb.")
        return false
    }
    override fun canAttack(target: Entity): Boolean = player.validTombEquipment()
    override fun keepCombating(target: Entity): Boolean = player.validTombEquipment()
    override fun processButtonClick(interfaceId: Int, componentId: Int, slotId: Int, slotId2: Int, opNum: Int): Boolean {
        if (interfaceId != Interface.interface_271 && !(((interfaceId shl 16) or componentId) == Component.interface_749.component_749_4 && opNum == 1)) return true
        player.sendMessage("You cannot use prayer in Glarial's tomb.")
        return false
    }
}

private val FORBIDDEN_TOMB_SLOTS = setOf(Equipment.HEAD, Equipment.CAPE, Equipment.WEAPON, Equipment.CHEST, Equipment.SHIELD, Equipment.LEGS, Equipment.HANDS, Equipment.FEET, Equipment.AMMO)

internal fun forbiddenTombItem(item: Item): Boolean {
    val definition = ItemDefinitions.getDefs(item.id)
    val base = if (definition.noted) ItemDefinitions.getDefs(definition.noteId) else definition
    return base.wearPos in FORBIDDEN_TOMB_SLOTS || Rune.forId(base.id) != null || base.name.endsWith("rune", true) || base.name.endsWith("runes", true) || Pouch.forId(base.id) != null || base.name.contains("clue scroll", true)
}

internal fun Player.validTombEquipment(): Boolean {
    if (familiar != null || (inventory.items.itemsCopy + equipment.itemsCopy).filterNotNull().any(::forbiddenTombItem)) {
        sendMessage("You cannot take weapons, armour, runes, clue scrolls or familiars into Glarial's tomb.")
        return false
    }
    return true
}
