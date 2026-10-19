package com.rs.game.content.skills.slayer.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.skills.slayer.Slayer;
import com.rs.game.model.WorldProjectile;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;
import kotlin.Pair;

public class AberrantSpectre extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Aberrant spectre" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions def = npc.getCombatDefinitions();
		WorldProjectile p = World.sendProjectile(npc, target, def.getAttackProjectile(), new Pair<>(18, 18), 35, 5, 0);
		npc.setNextAnimation(new Animation(def.getAttackEmote()));
		if (target instanceof Player player && !Slayer.hasNosepeg(player)) {
			Player targetPlayer = (Player) target;
			if (!targetPlayer.getPrayer().isProtectingMage()) {
				int randomSkill = Utils.random(0, 6);
				if (randomSkill != Constants.HITPOINTS) {
					int currentLevel = targetPlayer.getSkills().getLevel(randomSkill);
					targetPlayer.getSkills().set(randomSkill, currentLevel < 5 ? 0 : currentLevel - 5);
					targetPlayer.sendMessage("The smell of the abberrant spectre make you feel slightly weaker.");
				}
			}
			delayHit(npc, p.getTaskDelay(), target, Hit.magic(npc, targetPlayer.getMaxHitpoints() / 10));
			// TODO player emote hands on ears
		} else
			delayHit(npc, p.getTaskDelay(), target, Hit.magic(npc, getMaxHit(npc, npc.getMaxHit(), def.getAttackStyle(), target)));
		return npc.getAttackSpeed();
	}
}
