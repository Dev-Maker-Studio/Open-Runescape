package com.rs.game.content.skills.slayer.npcs.combat;

import com.rs.game.content.skills.slayer.Slayer;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;

public class CaveHorrorCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Cave horror" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions def = npc.getCombatDefinitions();
		if (target instanceof Player player && !Slayer.hasWitchWoodIcon(player)) {
			Player targetPlayer = (Player) target;
			int randomSkill = Utils.random(0, 6);
			int currentLevel = targetPlayer.getSkills().getLevel(randomSkill);
			if (randomSkill != Constants.HITPOINTS) {
				targetPlayer.getSkills().set(randomSkill, currentLevel < 5 ? 0 : currentLevel - 5);
				targetPlayer.sendMessage("The screams of the cave horror make you feel slightly weaker.");
				npc.setNextForceTalk(new ForceTalk("*OOOoooAHHHH*"));
			}
			delayHit(npc, 0, target, Hit.flat(npc, targetPlayer.getMaxHitpoints() / 3));
		} else
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, def.getMaxHit(), def.getAttackStyle(), target)));
		npc.setNextAnimation(new Animation(4237));
		return npc.getAttackSpeed();
	}
}
