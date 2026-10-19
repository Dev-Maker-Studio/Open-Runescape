package com.rs.game.content.world.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;

public class CatableponCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 4397, 4398, 4399 };
	}

	@Override
	public int attack(NPC npc, final Entity target) {
		NPCCombatDefinitions def = npc.getCombatDefinitions();
		if (Utils.random(10) == 0 && target instanceof Player player) {
			int strLvl = player.getSkills().getLevelForXp(Constants.STRENGTH);
			if (strLvl - player.getSkills().getLevel(Constants.STRENGTH) < 8) {
				player.getSkills().drainLevel(Constants.STRENGTH, (int) (strLvl * 0.15));
				npc.setNextAnimation(new Animation(4272));
				delayHit(npc, 1, target, Hit.magic(npc, getMaxHit(npc, def.getMaxHit(), def.getAttackStyle(), target)));
				return npc.getAttackSpeed();
			}

		}
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, def.getMaxHit(), def.getAttackStyle(), target)));
		npc.setNextAnimation(new Animation(def.getAttackEmote()));
		return npc.getAttackSpeed();
	}
}
