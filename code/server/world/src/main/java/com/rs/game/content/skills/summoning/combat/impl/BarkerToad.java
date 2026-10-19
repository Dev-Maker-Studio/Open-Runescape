package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.World;
import com.rs.game.content.combat.CombatFormulaKt;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.combat.PlayerCombat;
import com.rs.game.content.combat.PlayerCombatKt;
import com.rs.game.content.skills.summoning.Familiar;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnNPCHandler;
import kotlin.Pair;

@PluginEventHandler
public class BarkerToad extends FamiliarCombatScript {
	
	public static ItemOnNPCHandler load = new ItemOnNPCHandler(Pouch.BARKER_TOAD.getIdKeys(), e -> {
		if (e.getItem().getId() != 2) {
			e.getPlayer().sendMessage("You can only load the toad with cannonballs.");
			return;
		}
		if (e.getNPC() instanceof Familiar f) {
			if (f.getAttribs().getB("storedCannonball")) {
				e.getPlayer().sendMessage("The toad already has a cannonball stored in it.");
				return;
			}
			f.getAttribs().setB("storedCannonball", true);
			e.getPlayer().getInventory().deleteItem(2, 1);
			f.sync(7704, 1400);
		}
	});

	@Override
	public Object[] getKeys() {
		return Pouch.BARKER_TOAD.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (!(npc instanceof Familiar f) || !f.getAttribs().getB("storedCannonball"))
			return CANCEL;
		shootCannonball(f, target);
		return f.getAttackSpeed();
	}
	
	public static void shootCannonball(Familiar f, Entity target) {
		f.getAttribs().removeB("storedCannonball");
		f.sync(7703, 1401);
		delayHit(f, 1, target, CombatFormulaKt.calculateHit(f.getOwner(), target, 0, 300, PlayerCombatKt.isRanging(f.getOwner()) ? CombatStyle.RANGE : CombatStyle.MELEE, true, 1.0)).setSource(f);
		World.sendProjectile(f, target, 1402, new Pair<>(10, 16), 30, 5, 16);
	}
}
