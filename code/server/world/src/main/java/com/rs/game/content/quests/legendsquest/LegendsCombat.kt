package com.rs.game.content.quests.legendsquest

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.ANGRY
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.content.combat.CombatStyle
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.combat.CombatScript
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.npcCombat
import com.rs.plugin.kts.onInterfaceOnNPC
import com.rs.game.content.combat.CombatSpell
import com.rs.game.content.skills.magic.Magic
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.Utils
import org.darkan.core.model.Item
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

internal val CRYSTAL_REWARDS = mapOf(Npc.npc_936 to Obj.heartcrystal_sectiona, Npc.npc_937 to Obj.heartcrystal_sectionb, Npc.npc_938 to Obj.heartcrystal_sectionc)

@ServerStartupEvent
fun mapLegendsCombat() {
    onInterfaceOnNPC(checkDistance = false, fromInterfaceIds = intArrayOf(Interface.interface_192), fromComponentIds = intArrayOf(
        Utils.componentIdFromHash(Component.interface_192.component_192_26),
        Utils.componentIdFromHash(Component.interface_192.component_192_31),
        Utils.componentIdFromHash(Component.interface_192.component_192_35)
    )) { event ->
        val player = event.player
        val target = event.target
        val spell = CombatSpell.forId(event.interfaceId, event.componentId) ?: return@onInterfaceOnNPC
        if (target.id !in intArrayOf(Npc.ungadulu_good, Npc.ungadulu_bad)) {
            player.stopAll(false)
            if (target.definitions.hasAttackOption()) Magic.manualCast(player, target, spell)
            else player.sendMessage("You can't attack that.")
            return@onInterfaceOnNPC
        }
        if (target is OwnedNPC && target.owner !== player) return@onInterfaceOnNPC
        if (!player.inOctagram()) { player.sendMessage("The spell fizzles and has no effect as it passes through the flames."); return@onInterfaceOnNPC }
        if (!spell.canCast(player, target) || player.isLocked) return@onInterfaceOnNPC
        player.stopAll()
        spell.runeSet.deleteRunes(player)
        spell.cast(player, target)
        player.skills.addXp(Skills.MAGIC, spell.splashXp)
        player.startConversation {
            npc(Npc.ungadulu_good, CALM_TALK, "Thank you, Bwana! You have released me from the demon's grasp, but only for a short while! You must seek out the Book of Binding - it's hidden in these caves somewhere!")
            npc(Npc.ungadulu_good, CALM_TALK, "There are many trials you must pass... but please, you're my only hope of escape!")
            simple("The shaman's eyes roll back and he returns to his quiet, unassuming self.")
        }
    }
    npcCombat(Npc.nezikchened) { npc, target ->
        val demon = npc as? LegendsNezikchened
        when {
            demon?.round == 2 && npc.tickCounter % 4 == 0L -> {
                npc.forceTalk("Ha, ha, ha... feel my power!")
                npc.anim(Seq.demon_attack)
                CombatScript.delayHit(npc, 1, target, Hit.range(npc, CombatScript.getMaxHit(npc, 160, CombatStyle.RANGE, target)))
            }
            !npc.withinDistance(target.tile, 1) || npc.tickCounter % 5 == 0L -> {
                npc.anim(Seq.demon_attack)
                CombatScript.delayHit(npc, 1, target, Hit.magic(npc, CombatScript.getMaxHit(npc, 180, CombatStyle.MAGIC, target)))
            }
            else -> {
                npc.anim(Seq.demon_attack)
                CombatScript.delayHit(npc, 0, target, Hit.melee(npc, CombatScript.getMaxHit(npc, CombatStyle.MELEE, target)))
            }
        }
        4
    }
}

internal fun activeLegendsFight(player: Player): Boolean {
    val npc = player.tempAttribs.getO<NPC>("legendsFight") ?: return false
    return !npc.hasFinished() && !npc.isDead
}

internal fun bindUngadulu(player: Player) {
    if (player.legendsStage != BINDING || !player.inOctagram() || !player.legends.getB("bindingBook") || !player.inventory.containsItem(Obj.book_of_binding)) {
        player.sendMessage("You must enter the flaming octagram and bring the Book of Binding before Ungadulu.")
        return
    }
    if (activeLegendsFight(player)) return
    player.startConversation {
        simple("You open the Book of Binding in front of Ungadulu and a supernatural light emanates from the pages. Ungadulu's face twists and contorts in wild spasms.")
        npc(Npc.ungadulu_bad, ANGRY, "Curse you... foul intruder... wretched Vacu! Thy meddling ways hath brought thee to thy doom!")
        simple("Ungadulu drops to the floor, unconscious. From his prone body rises the shape of an enormous, angry demon...")
        legendsExec(player) {
            if (player.legends.getB("bindingBook") && player.inventory.containsItem(Obj.book_of_binding)) spawnLegendsDemon(player, 1)
            else player.sendMessage("You need to bring the Book of Binding before Ungadulu.")
        }
    }
}

internal fun soakUngadulu(player: Player) {
    if (player.legendsStage != BINDING || !player.inOctagram()) { player.sendMessage("The spell fizzles and has no effect as it passes through the flames."); return }
    if (!player.inventory.containsItem(Obj.holy_water)) return
    player.inventory.deleteItem(Obj.holy_water, 1)
    player.legends.setB("holyWater", true)
    player.startConversation {
        npc(Npc.ungadulu_bad, ANGRY, "Vile serpent, thou wilt pay for that!")
        simple("The vial smashes on Ungadulu, soaking him in sacred water.")
        npc(Npc.ungadulu_good, CALM_TALK, "...What, what happened? Why am I all wet?")
    }
}

internal fun repelUngadulu(player: Player) {
    if (!player.inOctagram()) { player.sendMessage("You feel certain that your weapon would melt if you tried to attack through these magical flames."); return }
    player.stopAll()
    player.applyHit(Hit.magic(null, 50))
    player.useStairs(Seq.human_longjump, Tile.of(2793, 9336, 0), 0, 2)
    player.sendMessage(if (player.legendsStage >= SEEDS) "Hey! That's no way to treat a shaman. I hope that teaches you to respect your elders!" else "Foolish Vacu... come for me again and thou shalt taste more of my power!")
}

internal fun revealEchned(player: Player, light: Boolean) {
    if (player.legendsStage != DAGGER || !player.legends.getB("barrier") || !player.atLegendsSpring() || activeLegendsFight(player)) return
    if (light) {
        if (!player.inventory.containsItem(Obj.holyforce) || player.legends.getB("viyeldiDead")) return
    } else if (!player.legends.getB("viyeldiDead") || (!player.inventory.containsItem(Obj.deathdaggerdone) && player.equipment.weaponId != Obj.deathdaggerdone && !player.legends.getB("daggerReturned"))) return
    player.startConversation {
        if (light) {
            simple("You thrust the Holy Force spell in front of the spirit. A bright, holy light streams out from the page. The spirit lets out an unearthly, blood-curdling scream.", "The Holy Force spell seems to weaken the demon.")
        } else {
            npc(Npc.echned_zekin, CALM_TALK, "Aha, I see you have completed your task. I'll take that dagger from you now.")
            simple("As his ghostly hand wraps around the blade, a shadow flows from the dagger into the spirit's ethereal form.")
            npc(Npc.echned_zekin, ANGRY, "Ahhhhh, yes... I feel my power returning. Now, to deal with thee...")
        }
        legendsExec(player) {
            if (activeLegendsFight(player) || player.legendsStage != DAGGER || !player.atLegendsSpring()) return@legendsExec
            val tile = player.legendsDestination(player.transform(1, 0, 0), 3) ?: return@legendsExec
            if (light) {
                if (!player.inventory.containsItem(Obj.holyforce) || player.legends.getB("viyeldiDead")) return@legendsExec
                player.inventory.deleteItem(Obj.holyforce, 1)
                player.legends.setB("lightPath", true)
            } else {
                if (!player.legends.getB("viyeldiDead") || (!player.inventory.containsItem(Obj.deathdaggerdone) && player.equipment.weaponId != Obj.deathdaggerdone && !player.legends.getB("daggerReturned"))) return@legendsExec
                if (player.equipment.weaponId == Obj.deathdaggerdone) {
                    player.equipment.replace(Equipment.WEAPON, null)
                    player.appearance.generateAppearanceData()
                } else player.inventory.deleteItem(Obj.deathdaggerdone, 1)
                player.legends.setB("daggerReturned", true)
            }
            World.getNPCsInChunkRange(player.chunkId, 3).filter { it.id == Npc.echned_zekin && it is OwnedNPC && it.owner === player }.forEach(NPC::finish)
            spawnLegendsDemon(player, 2, tile)
        }
    }
}

internal fun killViyeldi(player: Player) {
    if (player.isLocked || player.plane != 0 || player.legendsStage != DAGGER || player.legends.getB("viyeldiDead") || player.x !in 2370..2385 || player.y !in 4708..4718 || player.legends.getB("lightPath")) return
    if (player.equipment.weaponId != Obj.deathdagger && !player.inventory.containsItem(Obj.deathdagger)) return
    val spirit = World.getNPCsInChunkRange(player.chunkId, 3).firstOrNull { it.id == Npc.viyeldi && !it.hasFinished() && it is OwnedNPC && it.owner === player } ?: return
    if (!player.withinDistance(spirit.tile, 2)) return
    player.stopAll()
    player.faceTile(spirit.tile)
    player.lock(3)
    player.anim(Seq.human_ddagger_lunge)
    WorldTasks.schedule(1) {
        if (player.hasFinished() || player.isDead || spirit.hasFinished() || !player.withinDistance(spirit.tile, 2) || player.legendsStage != DAGGER || player.legends.getB("lightPath") || player.legends.getB("viyeldiDead")) return@schedule
        if (player.equipment.weaponId == Obj.deathdagger) {
            player.equipment.replace(Equipment.WEAPON, Item(Obj.deathdaggerdone, 1))
            player.appearance.generateAppearanceData()
        } else if (!player.replaceLegendsItem(Obj.deathdagger, Obj.deathdaggerdone)) return@schedule
        player.legends.setB("viyeldiDead", true)
        spirit.finish()
        player.sendMessage("You strike Viyeldi with the dark dagger. His spirit is drawn into the blade, which begins to glow.")
    }
}

internal fun pushSourceBoulder(player: Player, npc: NPC) {
    if (player.legendsStage < SOURCE || !player.legends.getB("barrier")) return
    if (player.legendsStage < WATER_RESTORED) {
        echnedDialogue(player)
        return
    }
    if (player.isLocked || npc.hasFinished() || !player.withinDistance(npc.tile, 2)) return
    val index = SOURCE_BOULDERS.indexOfFirst { it.x == npc.tempAttribs.getI("legendsSpawnX") && it.y == npc.tempAttribs.getI("legendsSpawnY") }
    if (index < 0) return
    player.faceTile(npc.tile)
    player.lock(3)
    player.anim(Seq.human_push)
    WorldTasks.schedule(2) {
        if (player.hasFinished() || player.isDead || npc.hasFinished() || !player.withinDistance(npc.tile, 2) || player.legendsStage < WATER_RESTORED) return@schedule
        player.legends.setI("boulders", player.legends.getI("boulders") or (1 shl index))
        npc.finish()
        player.sendMessage("You push the boulder out of the way. A spring of sacred water appears.")
        refreshLegendsScene(player)
    }
}

internal fun beginFinalBattle(player: Player, tile: Tile) {
    if (player.legendsStage != TOTEM || activeLegendsFight(player)) return
    player.legends.setI("battleX", tile.x)
    player.legends.setI("battleY", tile.y)
    player.sendMessage("You attempt to replace the corrupted totem pole. A black cloud emanates from the rotten wood, and it slowly coalesces into the dread demon Nezikchened.")
    if (player.legends.getB("viyeldiDead")) {
        player.sendMessage("Protectors of the sacred water, ye alive in death, do not rest while this Vacu draws breath!")
        spawnFinalGuardian(player)
    } else spawnLegendsDemon(player, 3)
}

internal fun spawnFinalGuardian(player: Player) {
    if (player.legendsStage != TOTEM || player.hasFinished() || player.isDead || !player.atLegendsTotemBattle() || activeLegendsFight(player)) return
    val index = (0..2).firstOrNull { player.legends.getI("finalHeroes") and (1 shl it) == 0 }
    if (index == null) { spawnLegendsDemon(player, 3); return }
    val id = CRYSTAL_REWARDS.keys.elementAt(index)
    val tile = player.legendsDestination(player.transform(1, 0, 0)) ?: return
    val guardian = LegendsGuardian(player, id, tile, true)
    player.tempAttribs.setO<NPC>("legendsFight", guardian)
    guardian.forceTalk("Corrupted we are now Viyeldi was slain..")
    WorldTasks.schedule(2) { if (!guardian.hasFinished() && !guardian.isDead) guardian.forceTalk("Slaves to this demon and forced to bring pain...") }
    guardian.setCombatTarget(player)
}

internal fun spawnLegendsDemon(player: Player, round: Int, destination: Tile? = null) {
    val expected = when (round) { 1 -> BINDING; 2 -> DAGGER; else -> TOTEM }
    if (player.legendsStage != expected || player.isDead || player.hasFinished() || activeLegendsFight(player)) return
    if (when (round) { 1 -> !player.inOctagram(); 2 -> !player.atLegendsSpring(); else -> !player.atLegendsTotemBattle() }) return
    val tile = destination ?: player.legendsDestination(if (round == 1) Tile.of(2790, 9326, 0) else player.transform(1, 0, 0), 3) ?: return
    val demon = LegendsNezikchened(player, tile, round)
    player.tempAttribs.setO<NPC>("legendsFight", demon)
    player.prayer.drainPrayer(player.prayer.points * if (round == 2 && player.legends.getB("lightPath")) 0.5 else 0.9)
    demon.forceTalk(if (round == 1 || round == 3) "Thy faith will help you little here." else "Perish, Vacu!")
    demon.setCombatTarget(player)
    player.sendMessage("A sense of hopelessness fills your body.")
}

internal fun Player.atLegendsSpring() = plane == 0 && x in 2370..2405 && y in 4673..4696
internal fun Player.atLegendsTotemBattle() = inKharazi() && tile.distanceTo(Tile.of(legends.getI("battleX"), legends.getI("battleY"), 0)) <= 14
