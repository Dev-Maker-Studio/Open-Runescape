package com.rs.game.content.bosses.pestqueen.attack.impl;

import com.rs.game.content.bosses.pestqueen.PestQueen;
import com.rs.game.content.bosses.pestqueen.attack.Attack;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;

public class MagicAttack implements Attack {

	@Override
	public void processAttack(PestQueen queen, Entity target) {
		queen.setNextFaceEntity(target);
		queen.setNextAnimation(getAttackAnimation());
		int hit = Utils.random(0, getMaxHit());// TODO make the combat rolls
		// actually accurate using npc
		// bonuses
		target.applyHit(new Hit(queen, hit, hit == 0 ? HitLook.MISSED : HitLook.MAGIC_DAMAGE));
	}

	@Override
	public Animation getAttackAnimation() {
		return new Animation(14792);
	}

	@Override
	public int getMaxHit() {
		return 200;
	}

	@Override
	public boolean canAttack(PestQueen queen, Entity target) {
		return true;
	}

}
