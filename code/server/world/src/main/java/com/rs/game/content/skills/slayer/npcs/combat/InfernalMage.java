package com.rs.game.content.skills.slayer.npcs.combat;

import com.rs.game.World;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import kotlin.Pair;

public class InfernalMage extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Infernal Mage" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions defs = npc.getCombatDefinitions();
		npc.setNextAnimation(new Animation(defs.getAttackEmote()));

		World.sendProjectile(npc, target, defs.getAttackProjectile(), new Pair<>(30, 30), 50, 5, Utils.random(5));
		delayHit(npc, 3, target, Hit.magic(npc, getMaxHit(npc, defs.getMaxHit(), defs.getAttackStyle(), target)));
		target.setNextSpotAnim(new SpotAnim(2739, 3, 100));
		return npc.getAttackSpeed();
	}
}
