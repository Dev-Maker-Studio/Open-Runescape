package com.rs.game.content.bosses.godwars

import com.rs.game.model.entity.npc.NPC
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

class GWDMinion(id: Int, tile: Tile, spawned: Boolean) : NPC(id, tile, spawned) {
    init {
        isIgnoreDocile = true
        setForceAgressive(true)
        setForceAggroDistance(64)
        isIntelligentRouteFinder = true
    }
}

@ServerStartupEvent
fun mapGWDMinions() {
    instantiateNpc(
        Npc.godwars_sergeant_goblin1, Npc.godwars_sergeant_goblin2, Npc.godwars_sergeant_goblin3,
        Npc.godwars_armadyl_male_armor03_blue, Npc.godwars_armadyl_female_armor03_green, Npc.godwars_armadyl_female_armor03_red,
        Npc.godwars_saradomin_unicorn, Npc.godwars_saradomin_lion, Npc.godwars_saradomin_centaur,
        Npc.godwars_ancient_greater_demon, Npc.godwars_ancient_lesser_demon, Npc.godwars_ancient_black_demon
    ) { npcId, tile -> GWDMinion(npcId, tile, false) }
}
