package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import kotlin.Pair;

public class TomeOfLexicus extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Tome of Strength", "Tome of Ranging", "Tome of Magic" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		int type = npc.getId() - 9856;
		switch (type) {
			case 0 -> {
				npc.setNextAnimation(new Animation(13479));
				delayHit(npc, 0, target, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, target)));
			}
			case 1, 2 -> {
				boolean range_style = type == 1;
				npc.setNextAnimation(new Animation(13480));
				npc.setNextSpotAnim(new SpotAnim(range_style ? 2408 : 2424));
				World.sendProjectile(npc, target, range_style ? 2409 : 2425, new Pair<>(40, 40), 54, 5, 5);
				if (range_style)
					delayHit(npc, 1, target, Hit.range(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.RANGE, target)));
				else
					delayHit(npc, 1, target, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, target)));
				target.setNextSpotAnim(new SpotAnim(range_style ? 2410 : 2426, 75, 0));
			}
		}
		return 4;
	}
}
