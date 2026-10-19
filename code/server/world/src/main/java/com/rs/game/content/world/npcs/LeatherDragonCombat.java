package com.rs.game.content.world.npcs;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.combat.PlayerCombatKt;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;

public class LeatherDragonCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Green dragon", "Blue dragon", "Red dragon", "Black dragon", 742, 14548 };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int distanceX = target.getX() - npc.getX();
		int distanceY = target.getY() - npc.getY();
		int size = npc.getSize();
		if (distanceX > size || distanceX < -1 || distanceY > size || distanceY < -1)
			return 0;
		if (Utils.getRandomInclusive(3) != 0) {
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target)));
		} else {
			int damage = Utils.getRandomInclusive(500);
			int fireBreathAnimID = 12259;
			if(npc.getId() >= 10219 && npc.getId() <= 10224)//Black dragons in dung
				fireBreathAnimID = 13155;
			npc.setNextAnimation(new Animation(fireBreathAnimID));
			npc.setNextSpotAnim(new SpotAnim(1, 0, 100));
			int protection = PlayerCombatKt.getAntifireLevel(target, true);
			if (protection == 1)
				damage = Utils.getRandomInclusive(50);
			else if (protection == 2)
				damage = 0;
			delayHit(npc, 1, target, Hit.flat(npc, damage));
		}
		return npc.getAttackSpeed();
	}
}
