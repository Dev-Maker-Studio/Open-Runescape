package com.rs.game.content.minigames.fightkiln.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;

public class TokHaarKetDillCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "TokHaar-Ket-Dill" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions defs = npc.getCombatDefinitions();
		if (Utils.random(6) == 0) {
			delayHit(npc, 0, target, Hit.flat(npc, Utils.random(defs.getMaxHit() + 1)));
			target.setNextSpotAnim(new SpotAnim(2999));
			if (target instanceof Player player)
				player.sendMessage("The TokHaar-Ket-Dill slams it's tail to the ground.");
		} else
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), defs.getAttackStyle(), target)));
		npc.setNextAnimation(new Animation(defs.getAttackEmote()));
		return npc.getAttackSpeed();
	}
}
