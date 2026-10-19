package com.rs.game.content.quests.deserttreasure

import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.gameobject.GameObject
import com.rs.game.content.containsTool
import com.rs.game.content.world.doors.Doors.handleDoubleDoor
import com.rs.game.content.combat.CombatSpell
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

private const val TORCH_BURN_TICKS = 200

private const val SMOKE_DEFEATED = "dtFareedDefeated"
private const val SMOKE_DIAMOND_CLAIMED = "dtSmokeDiamondClaimed"
private const val FAREED_ACTIVE = "dtFareedActive"
private const val FAREED_GATE_UNLOCKED = "dtFareedGateUnlocked"
private const val TORCH_SW_LIT = "dtSmokeTorchSWLit"
private const val TORCH_NW_LIT = "dtSmokeTorchNWLit"
private const val TORCH_SE_LIT = "dtSmokeTorchSELit"
private const val TORCH_NE_LIT = "dtSmokeTorchNELit"
private const val TORCH_SW_GENERATION = "dtSmokeTorchSWGeneration"
private const val TORCH_NW_GENERATION = "dtSmokeTorchNWGeneration"
private const val TORCH_SE_GENERATION = "dtSmokeTorchSEGeneration"
private const val TORCH_NE_GENERATION = "dtSmokeTorchNEGeneration"

private data class Torch(val id: Int, val state: String, val generation: String, val direction: String, val varbit: Int)

private val smokeTorches = listOf(
    Torch(Loc.loc_6407, TORCH_SW_LIT, TORCH_SW_GENERATION, "South-west", Varbit.fd_torch_count2),
    Torch(Loc.loc_6405, TORCH_NW_LIT, TORCH_NW_GENERATION, "North-west", Varbit.fd_torch_count1),
    Torch(Loc.loc_6409, TORCH_SE_LIT, TORCH_SE_GENERATION, "South-east", Varbit.fd_torch_count3),
    Torch(Loc.loc_6411, TORCH_NE_LIT, TORCH_NE_GENERATION, "North-east", Varbit.fd_torch_count4),
)

@ServerStartupEvent
fun mapSmokeDiamondDesertTreasure() {
    onLogin { (player) ->
        if (player.getBool(FAREED_ACTIVE) && !player.getBool(SMOKE_DEFEATED)) {
            player.set(FAREED_ACTIVE, false)
        }
    }

    onObjectClick(Loc.sword_haunted_well) { (player) ->
        if (!isSearchingForDiamonds(player)) return@onObjectClick
        player.simpleDialogue("I can see a lot of foul looking smoke at the bottom of this well. I don't think breathing it in directly would be very good for my health.")
    }

    smokeTorches.forEach { torch ->
        onObjectClick(torch.id) { (player) ->
            if (!isSearchingForDiamonds(player)) return@onObjectClick
            if (!player.inventory.containsItem(Obj.tinderbox, 1) && !player.containsTool(Obj.tinderbox)) {
                player.sendMessage("You need a tinderbox to light the torch.")
                return@onObjectClick
            }
            if (player.skills.getLevel(Skills.FIREMAKING) < 50) {
                player.sendMessage("You need a Firemaking level of 50 to light this torch.")
                return@onObjectClick
            }
            if (!player.getBool(torch.state)) {
                player.set(torch.state, true)
                player.vars.setVarBit(torch.varbit, 1)
                val generation = player.getI(torch.generation) + 1
                player.set(torch.generation, generation)
                player.sendMessage("You light the torch.")
                player.tasks.schedule(TORCH_BURN_TICKS) {
                    if (player.getBool(torch.state) && player.getI(torch.generation) == generation) {
                        player.set(torch.state, false)
                        player.vars.setVarBit(torch.varbit, 0)
                        player.sendMessage("The ${torch.direction} torch burns out...")
                    }
                }
                if (smokeTorches.all { player.getBool(it.state) }) {
                    player.sendMessage("The path is lit, now claim the key...")
                }
            } else {
                player.sendMessage("The ${torch.direction} torch is already lit.")
            }
        }
    }

    onObjectClick(Loc.fd_firedungeon_shutchest) { (player, obj) ->
        if (!smokeTorches.all { player.getBool(it.state) }) {
            player.simpleDialogue("There seems to be no way to open this chest. Engraved where the keyhole should be is a message; 'Light the path, and find the key...'")
            return@onObjectClick
        }

        if (!isSearchingForDiamonds(player)) return@onObjectClick
        player.faceObject(obj)
        player.anim(Seq.human_openchest)
        player.lock(2)
        World.spawnObjectTemporary(GameObject(Loc.fd_firedungeon_openchest, obj.type, obj.rotation, obj.tile), 2)
        if (player.containsItem(Obj.fd_firekey)) {
            player.sendMessage("The chest is empty.")
        } else {
            player.inventory.addItemDrop(Obj.fd_firekey, 1)
            player.sendMessage("You open the chest and take a key.")
        }
    }

    onObjectClick(
        Loc.fd_fw_metalgateclosed_l,
        Loc.fd_fw_metalgateclosed_r,
        tiles = arrayOf(Tile.of(3305, 9375, 0), Tile.of(3305, 9376, 0))
    ) { (player, obj) ->
        if (player.isLocked || !isSearchingForDiamonds(player)) return@onObjectClick
        if (!player.questManager.getAttribs(Quest.DESERT_TREASURE).getB(FAREED_GATE_UNLOCKED)) {
            if (!player.inventory.containsItem(Obj.fd_firekey, 1)) {
                player.sendMessage("The gate is locked. You need the warm key to unlock it.")
                return@onObjectClick
            }
            player.inventory.deleteItem(Obj.fd_firekey, 1)
            player.questManager.getAttribs(Quest.DESERT_TREASURE).setB(FAREED_GATE_UNLOCKED, true)
            player.sendMessage("You unlock the gate and enter the room.")
        }

        val entering = player.x <= 3304
        handleDoubleDoor(player, obj)
        if (!entering) return@onObjectClick

        if (player.getBool(SMOKE_DEFEATED)) {
            if (!DesertTreasureDiamonds.hasSmokeDiamond(player)) {
                player.sendMessage("The Diamond of Smoke seems to have mystically found its way back here...")
                DesertTreasureDiamonds.giveSmokeDiamond(player)
            }
            return@onObjectClick
        }
        if (player.getBool(FAREED_ACTIVE)) return@onObjectClick

        player.set(FAREED_ACTIVE, true)
        val fareed = QuestFareed(player, Tile.of(3317, 9376, 0))
        fareed.setAutoDespawnAtDistance(false)
        fareed.combatTarget = player
        fareed.forceTalk("You dare trespass in my realm?")
    }

    onNpcDeath(Npc.firediamond_firewarrior) { (killer, npc) ->
        val player = (npc as? OwnedNPC)?.owner ?: killer as? Player ?: return@onNpcDeath
        if (npc is OwnedNPC && npc.owner !== player) return@onNpcDeath
        player.set(SMOKE_DEFEATED, true)
        player.set(FAREED_ACTIVE, false)
        if (!player.getBool(SMOKE_DIAMOND_CLAIMED)) {
            player.sendMessage("You take the Diamond of Smoke from the ashes of the warrior.")
            DesertTreasureDiamonds.giveSmokeDiamond(player)
            player.set(SMOKE_DIAMOND_CLAIMED, true)
        }
    }
}

private fun isSearchingForDiamonds(player: Player): Boolean =
    player.getQuestStage(Quest.DESERT_TREASURE) == STAGE_DIAMONDS_OF_AZZANADRA


private class QuestFareed(owner: Player, tile: Tile) : OwnedNPC(owner, Npc.firediamond_firewarrior, tile, true) {
    override fun processNPC() {
        if (!isDead && !hasFinished() && withinDistance(owner, 1) && owner.equipment.glovesId != Obj.ice_gloves) {
            val weapon = owner.equipment.getItem(Equipment.WEAPON)
            if (weapon != null && owner.equipment.deleteSlot(Equipment.WEAPON)) {
                owner.inventory.addItemDrop(weapon)
                owner.appearance.generateAppearanceData()
                owner.combatDefinitions.resetSpells(true)
                owner.sendMessage("Fareed's intense heat forces you to remove your weapon.")
            }
        }
        super.processNPC()
    }

    override fun handlePreHit(hit: Hit) {
        if (hit.look == HitLook.MAGIC_DAMAGE && (hit.getData("combatSpell") as? CombatSpell)?.isWaterSpell != true) {
            hit.damage = 0
            return
        }
        super.handlePreHit(hit)
    }

    override fun handlePreHitOut(target: Entity, hit: Hit) {
        super.handlePreHitOut(target, hit)
        if (target is Player && target.equipment.glovesId == Obj.ice_gloves)
            hit.damage /= 2
    }
}
