package com.rs.game.content.minigames.fightcaves.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;

public class YtMejKotCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Yt-MejKot" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		npc.setNextAnimation(new Animation(defs.getAttackEmote()));
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), defs.getAttackStyle(), target)));
		if (npc.getHitpoints() < npc.getMaxHitpoints() / 2)
			if (npc.getTempAttribs().removeB("Heal")) {
				npc.setNextSpotAnim(new SpotAnim(2980, 0, 100));
				npc.queryNearbyNPCsByTileRange(4, n -> {
					if (n == null || n.isDead() || n.hasFinished())
						return false;
					n.heal(100);
					return true;
				});
			} else
				npc.getTempAttribs().setB("Heal", Boolean.TRUE);
		return npc.getAttackSpeed();
	}
}
