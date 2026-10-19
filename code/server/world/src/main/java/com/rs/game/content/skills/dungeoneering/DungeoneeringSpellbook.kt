package com.rs.game.content.skills.dungeoneering

import com.rs.game.World
import com.rs.game.content.skills.magic.Magic
import com.rs.game.content.skills.magic.Rune
import com.rs.game.content.skills.magic.RuneSet
import com.rs.game.content.skills.magic.lunar.LunarSpellbook
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.InterfaceManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onInterfaceOnInterface
import com.rs.plugin.kts.onInterfaceOnNPC
import com.rs.plugin.kts.onInterfaceOnPlayer
import org.darkan.core.Constants
import org.darkan.core.Utils.componentIdFromHash
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component.interface_950
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Item

private val BONES_TO_BANANAS = componentIdFromHash(interface_950.component_950_31)
private val LOW_ALCHEMY = componentIdFromHash(interface_950.component_950_35)
private val HIGH_ALCHEMY = componentIdFromHash(interface_950.component_950_46)
private val MONSTER_EXAMINE = componentIdFromHash(interface_950.component_950_51)
private val CURE_OTHER = componentIdFromHash(interface_950.component_950_52)
private val HUMIDIFY = componentIdFromHash(interface_950.component_950_53)
private val CURE_ME = componentIdFromHash(interface_950.component_950_55)
private val CURE_GROUP = componentIdFromHash(interface_950.component_950_57)
private val VENGEANCE_OTHER = componentIdFromHash(interface_950.component_950_64)
private val VENGEANCE = componentIdFromHash(interface_950.component_950_65)
private val VENGEANCE_GROUP = componentIdFromHash(interface_950.component_950_66)

private val PLAYER_TARGET_SPELLS = intArrayOf(CURE_OTHER, VENGEANCE_OTHER)

@ServerStartupEvent
fun registerDungeoneeringSpells() {
    onInterfaceOnPlayer(fromInterfaceIds = intArrayOf(Interface.interface_950), fromComponentIds = PLAYER_TARGET_SPELLS) { e ->
        when (e.componentId) {
            CURE_OTHER -> LunarSpellbook.handleCureOther(e.player, e.target)
            VENGEANCE_OTHER -> LunarSpellbook.handleVengeanceOther(e.player, e.target)
        }
    }
    onInterfaceOnNPC(fromInterfaceIds = intArrayOf(Interface.interface_950), fromComponentIds = PLAYER_TARGET_SPELLS + MONSTER_EXAMINE) { e ->
        when (e.componentId) {
            MONSTER_EXAMINE -> LunarSpellbook.handleMonsterExamine(e.player, e.target)
            else -> e.player.sendMessage("This spell can only be cast on other players.")
        }
    }
    onInterfaceOnInterface(
        fromInterfaceIds = intArrayOf(Interface.interface_950),
        fromComponentIds = intArrayOf(LOW_ALCHEMY, HIGH_ALCHEMY),
        toInterfaceIds = intArrayOf(Interface.inventory),
        toComponentIds = null,
    ) { e ->
        val item = e.player.inventory.getItem(e.toSlotId) ?: return@onInterfaceOnInterface
        DungeoneeringSpellbook.castAlchemy(e.player, item, e.fromComponentId == HIGH_ALCHEMY)
    }
}

object DungeoneeringSpellbook {
    private val BONES = intArrayOf(Obj.rand_bones, Obj.rand_bat_bones, Obj.rand_big_bones, Obj.rand_dragon_bones, Obj.rand_frost_dragon_bones)

    @JvmStatic
    fun handleButton(player: Player, componentId: Int, packet: Int) {
        when (componentId) {
            BONES_TO_BANANAS -> castBonesToBananas(player)
            HUMIDIFY -> LunarSpellbook.handleHumidify(player)
            CURE_ME -> LunarSpellbook.handleCureMe(player)
            CURE_GROUP -> LunarSpellbook.handleCureGroup(player)
            VENGEANCE -> LunarSpellbook.handleVengeance(player)
            VENGEANCE_GROUP -> LunarSpellbook.handleGroupVengeance(player)
            else -> Magic.processDungSpell(player, componentId, packet)
        }
    }

    private fun castBonesToBananas(player: Player) {
        if (!player.canCastSpell())
            return
        val bones = BONES.sumOf { player.inventory.getAmountOf(it) }
        if (bones == 0) {
            player.sendMessage("You aren't holding any bones!")
            return
        }
        if (!Magic.checkMagicAndRunes(player, 15, true, RuneSet(Rune.EARTH, 2, Rune.WATER, 2, Rune.NATURE, 1)))
            return
        for (id in BONES) {
            val amount = player.inventory.getAmountOf(id)
            if (amount > 0) {
                player.inventory.deleteItem(id, amount)
                player.inventory.addItem(Obj.rand_banana, amount)
            }
        }
        player.sync(Seq.human_castbonestobananas, Spotanim.bonestobananas_casting)
        player.skills.addXp(Constants.MAGIC, 25.0)
        player.addSpellDelay(2)
    }

    fun castAlchemy(player: Player, item: Item, high: Boolean) {
        player.interfaceManager.openTab(InterfaceManager.Sub.TAB_MAGIC)
        if (!player.canCastSpell())
            return
        val coins = alchemyValue(item, high)
        if (coins <= 0) {
            player.sendMessage("You can't cast alchemy on that!")
            return
        }
        val runes = if (high) RuneSet(Rune.FIRE, 5, Rune.NATURE, 1) else RuneSet(Rune.FIRE, 3, Rune.NATURE, 1)
        if (!Magic.checkMagicAndRunes(player, if (high) 55 else 21, true, runes))
            return
        player.stopAll(false, false, true)
        val staff = player.equipment.getItem(Equipment.WEAPON)?.name?.lowercase()?.contains("staff") == true
        when {
            high && staff -> player.sync(Seq.seq_9633, Spotanim.spotanim_1693)
            high -> player.sync(Seq.human_casthighlvlalchemy, Spotanim.highlvlalchemy)
            staff -> player.sync(Seq.low_lvl_alchemy_player_staff, Spotanim.low_lvl_alchemy_staff)
            else -> player.sync(Seq.human_castlowlvlalchemy, Spotanim.lowlvlalchemy)
        }
        World.soundEffect(player.tile, Sound.sound_98)
        player.inventory.deleteItem(item.id, 1)
        player.inventory.addItem(Obj.rand_coins, coins)
        player.skills.addXp(Constants.MAGIC, if (high) 65.0 else 31.0)
        player.addSpellDelay(if (high) 4 else 2)
    }

    private fun alchemyValue(item: Item, high: Boolean): Int {
        val def = ItemDefinitions.getDefs(item.id)
        val name = def.name.lowercase()
        if (item.id == Obj.rand_coins || "key" in name || "(b)" in name || "of kinship" in name || "gatestone" in name)
            return 0
        val sellValue = (def.getValue() * def.getDungShopValueMultiplier() * 0.3).toInt()
        return if (high) sellValue else sellValue * 2 / 3
    }
}
