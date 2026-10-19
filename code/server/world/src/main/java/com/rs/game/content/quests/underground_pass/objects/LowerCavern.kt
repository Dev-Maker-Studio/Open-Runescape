package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.underground_pass.*
import com.rs.game.content.quests.underground_pass.npcs.showKoftikDwarfWarning
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick

private val PALADIN_BADGES = intArrayOf(Obj.paladinbadge1, Obj.paladinbadge2, Obj.paladinbadge3)
private val FIRST_WELL_TILE = Tile.of(2416, 9674, 0)
private val FIRST_WELL_DESTINATION = Tile.of(2423, 9660, 0)
private val FIRST_WELL_RETURN_ROCKPILE_TILE = Tile.of(2423, 9661, 0)
private val SECOND_WELL_TILE = Tile.of(2373, 9718, 0)
private val IBAN_LAIR_ENTRY_TILE = Tile.of(2173, 4725, 1)
private val IBAN_LAIR_RETURN_TILE = Tile.of(2369, 9718, 0)
private val IBAN_DOOR_LEFT_TILE = Tile.of(2368, 9717, 0)
private val IBAN_DOOR_RIGHT_TILE = Tile.of(2368, 9719, 0)
private val IBAN_LAIR_DOOR_NORTH_TILE = Tile.of(2174, 4726, 1)
private val IBAN_LAIR_DOOR_SOUTH_TILE = Tile.of(2174, 4724, 1)
private val IBAN_LAIR_NORTH_STAIRS_TILE = Tile.of(2112, 4729, 1)
private val IBAN_LAIR_NORTH_STAIRS_ENTRY = Tile.of(2113, 4729, 1)
private val IBAN_LAIR_SOUTH_STAIRS_TILE = Tile.of(2150, 4545, 1)
private val IBAN_LAIR_SOUTH_STAIRS_ENTRY = Tile.of(2150, 4546, 1)
private val DWARF_CAVERN_STAIRS_TILE = Tile.of(2336, 9793, 0)
private val DWARF_CAVERN_ENTRY_TILE = Tile.of(2336, 9794, 0)
private val KALRAG_CAVERN_STAIRS_TILE = Tile.of(2304, 9915, 0)
private val KALRAG_CAVERN_ENTRY_TILE = Tile.of(2305, 9915, 0)
private val FINAL_ESCAPE_CAVE_TILE = Tile.of(2438, 9607, 0)
private val FINAL_ESCAPE_CAVE_DESTINATION = Tile.of(2481, 9718, 0)

@ServerStartupEvent
fun mapLowerUndergroundPassCavern() {
    onItemOnObject(
        arrayOf(Loc.di2_blood_well),
        arrayOf(Obj.cave_unicorn_horn, Obj.paladinbadge1, Obj.paladinbadge2, Obj.paladinbadge3),
        tiles = arrayOf(SECOND_WELL_TILE)
    ) { (player, _, item) ->
        player.vars.setVarBit(Varbit.upass_read_second_well, 1)
        when (item.id) {
            Obj.cave_unicorn_horn -> {
                player.inventory.deleteItem(item)
                player.questManager.getAttribs(Quest.UNDERGROUND_PASS).setB("usedUnicornHorn", true)
                player.sendMessage("The unicorn horn dissolves in the well, weakening the seal.")
            }

            in PALADIN_BADGES -> {
                player.inventory.deleteItem(item)
                player.questManager.getAttribs(Quest.UNDERGROUND_PASS).setB("usedBadge${item.id}", true)
                player.sendMessage("The paladin's badge dissolves in the well, weakening the seal.")
            }
        }
        val attributes = player.questManager.getAttribs(Quest.UNDERGROUND_PASS)
        if (attributes.getB("usedUnicornHorn") && PALADIN_BADGES.all { attributes.getB("usedBadge$it") }) {
            player.sendMessage("The horn and three badges unlock the way into the lower cavern.")
            player.advanceUndergroundPass(STAGE_PASSED_PALADINS)
        }
    }

    onObjectClick(
        Loc.di2_cave_well,
        Loc.mudpile_upass,
        tiles = arrayOf(FIRST_WELL_TILE, FIRST_WELL_RETURN_ROCKPILE_TILE)
    ) { (player, obj) ->
        if (obj.id == Loc.di2_cave_well) {
            if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_DESTROYED_ORBS) {
                player.sendMessage("Light from the four orbs seals the well.")
                return@onObjectClick
            }
            player.fadeScreen { player.tele(FIRST_WELL_DESTINATION) }
        } else {
            player.fadeScreen { player.tele(FIRST_WELL_TILE) }
        }
    }

    onObjectClick(Loc.di2_blood_well, Loc.upass_last_out) { (player, obj) ->
        when (obj.id) {
            Loc.di2_blood_well -> {
                player.vars.setVarBit(Varbit.upass_read_second_well, 1)
                if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_PASSED_PALADINS) {
                    player.sendMessage("The well is sealed by unicorn and paladin heraldry.")
                    return@onObjectClick
                }
                player.sendMessage("The offerings have broken the seal. The doors to the west are unlocked.")
            }
            Loc.upass_last_out -> {
                if (obj.tile == FINAL_ESCAPE_CAVE_TILE) {
                    player.fadeScreen { player.tele(FINAL_ESCAPE_CAVE_DESTINATION) }
                    return@onObjectClick
                }
                if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_PASSED_PALADINS) {
                    player.sendMessage("The way is sealed. The unicorn's horn and three paladin badges form the lock.")
                    return@onObjectClick
                }
                player.fadeScreen { player.tele(IBAN_LAIR_ENTRY_TILE) }
            }
        }
    }

    onObjectClick(
        Loc.cavewalltunnel_upass_down,
        tiles = arrayOf(IBAN_LAIR_NORTH_STAIRS_TILE, IBAN_LAIR_SOUTH_STAIRS_TILE)
    ) { (player, obj) ->
        val destination = when (obj.tile) {
            IBAN_LAIR_NORTH_STAIRS_TILE -> KALRAG_CAVERN_ENTRY_TILE
            IBAN_LAIR_SOUTH_STAIRS_TILE -> DWARF_CAVERN_ENTRY_TILE
            else -> return@onObjectClick
        }
        player.fadeScreen {
            player.tele(destination)
            if (destination == DWARF_CAVERN_ENTRY_TILE)
                player.showKoftikDwarfWarning()
        }
    }

    onObjectClick(
        Loc.cavewalltunnel_upass_up,
        tiles = arrayOf(KALRAG_CAVERN_STAIRS_TILE, DWARF_CAVERN_STAIRS_TILE)
    ) { (player, obj) ->
        val destination = when (obj.tile) {
            KALRAG_CAVERN_STAIRS_TILE -> IBAN_LAIR_NORTH_STAIRS_ENTRY
            DWARF_CAVERN_STAIRS_TILE -> IBAN_LAIR_SOUTH_STAIRS_ENTRY
            else -> return@onObjectClick
        }
        player.fadeScreen { player.tele(destination) }
    }

    onObjectClick(
        Loc.loc_3220,
        Loc.loc_3221,
        tiles = arrayOf(IBAN_DOOR_LEFT_TILE, IBAN_DOOR_RIGHT_TILE)
    ) { (player) ->
        if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_PASSED_PALADINS) {
            player.sendMessage("The doors remain sealed. The well demands a unicorn horn and three paladin badges.")
            return@onObjectClick
        }
        player.fadeScreen { player.tele(IBAN_LAIR_ENTRY_TILE) }
    }

    onObjectClick(
        Loc.loc_34288,
        Loc.loc_34289,
        tiles = arrayOf(IBAN_LAIR_DOOR_NORTH_TILE, IBAN_LAIR_DOOR_SOUTH_TILE)
    ) { (player) ->
        player.fadeScreen { player.tele(IBAN_LAIR_RETURN_TILE) }
    }
}
