package com.rs.game.content.world.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.combat.PlayerCombatKt;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class MetalDragonCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Bronze dragon", "Iron dragon", "Steel dragon", "Pit iron dragon" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions defs = npc.getCombatDefinitions();
		if (WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0) && Utils.random(2) == 0) {
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target)));
		} else {
			int damage = 100 + Utils.getRandomInclusive(500);
			final Player player = target instanceof Player p ? p : null;
			if (player != null) {
				int protection = PlayerCombatKt.getAntifireLevel(target, false);
				if (protection == 1)
					damage = Utils.getRandomInclusive(50);
				else if (protection == 2)
					damage = 0;
			}

			npc.setNextAnimation(new Animation(13160));
			World.sendProjectile(npc, target, 393, new Pair<>(28, 16), 35, 10, 16);
			delayHit(npc, 1, target, Hit.flat(npc, damage));
		}
		return npc.getAttackSpeed();
	}

}
