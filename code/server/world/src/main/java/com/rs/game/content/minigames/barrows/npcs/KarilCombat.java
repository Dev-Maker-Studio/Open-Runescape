package com.rs.game.content.minigames.barrows.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import kotlin.Pair;

public class KarilCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 2028 };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		npc.setNextAnimation(new Animation(defs.getAttackEmote()));
		int damage = getMaxHit(npc, defs.getMaxHit(), CombatStyle.RANGE, target);
		if (damage != 0 && target instanceof Player player && Utils.random(3) == 0) {
			target.setNextSpotAnim(new SpotAnim(401, 0, 100));
			int drain = (int) (player.getSkills().getLevelForXp(Constants.AGILITY) * 0.2);
			int currentLevel = player.getSkills().getLevel(Constants.AGILITY);
			player.getSkills().set(Constants.AGILITY, currentLevel < drain ? 0 : currentLevel - drain);
		}
		World.sendProjectile(npc, target, defs.getAttackProjectile(), new Pair<>(41, 16), 41, 5, 16);
		delayHit(npc, 2, target, Hit.range(npc, damage));
		return npc.getAttackSpeed();
	}
}
