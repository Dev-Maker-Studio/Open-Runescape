package com.rs.game.content.world.npcs;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import com.rs.utils.Ticks;
import com.rs.utils.WorldUtil;

public class SkeletalWyvernCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 3068, 3069, 3070, 3071 };
	}

	private static boolean hasShield(Entity target) {
		if (!(target instanceof Player))
			return true;
		int shieldId = ((Player) target).getEquipment().getShieldId();
		return shieldId == 2890 || shieldId == 9731 || shieldId == 20436 || shieldId == 20438 || shieldId == 18691 || (shieldId >= 11283 && shieldId <= 12285);
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int attackStyle = Utils.random(WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0) ? 3 : 2);
		switch (attackStyle) {
		case 0:
			npc.setNextAnimation(new Animation(1592));
			npc.setNextSpotAnim(new SpotAnim(501));
			target.setNextSpotAnim(new SpotAnim(502));
			if (Utils.random(10) == 0)
				target.freeze(Ticks.fromSeconds(5));
			delayHit(npc, 1, target, Hit.flat(npc, Utils.getRandomInclusive(hasShield(target) ? 150 : 600)));
			break;
		case 1:
			npc.setNextAnimation(new Animation(1593));
			npc.setNextSpotAnim(new SpotAnim(499));
			delayHit(npc, 1, target, Hit.range(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.RANGE, target)));
			break;
		case 2:
			npc.setNextAnimation(new Animation(1589 + Utils.random(2)));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target)));
			break;
		}
		return npc.getAttackSpeed();
	}

}
