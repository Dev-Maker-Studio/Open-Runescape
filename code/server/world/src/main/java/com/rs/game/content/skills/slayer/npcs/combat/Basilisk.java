package com.rs.game.content.skills.slayer.npcs.combat;

import com.rs.game.content.skills.slayer.Slayer;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;

public class Basilisk extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Basilisk" };
	}

	@Override
	public int attack(NPC npc, final Entity target) {
		NPCCombatDefinitions def = npc.getCombatDefinitions();
		if (target instanceof Player player && !Slayer.hasReflectiveEquipment(player)) {
			Player targetPlayer = (Player) target;
			int randomSkill = Utils.random(0, 6);
			if (randomSkill != Constants.HITPOINTS) {
				int currentLevel = targetPlayer.getSkills().getLevel(randomSkill);
				targetPlayer.getSkills().set(randomSkill, currentLevel < 5 ? 0 : currentLevel - 5);
				delayHit(npc, 0, target, Hit.flat(npc, targetPlayer.getMaxHitpoints() / 10));
				WorldTasks.schedule(() -> target.setNextSpotAnim(new SpotAnim(747)));
			}
		} else
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, npc.getMaxHit(), def.getAttackStyle(), target)));
		npc.setNextAnimation(new Animation(def.getAttackEmote()));
		return npc.getAttackSpeed();
	}
}
