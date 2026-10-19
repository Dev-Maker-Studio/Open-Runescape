package com.rs.game.content.bosses.pestqueen.attack;

import com.rs.game.content.bosses.pestqueen.PestQueen;
import com.rs.game.model.entity.Entity;
import org.darkan.core.model.Animation;

public interface Attack {

	/**
	 * Process the attack.
	 *
	 * @param queen
	 *            The {@code PestQueen} instance.
	 * @param target
	 *            The {@code Entity} instance.
	 */
	public void processAttack(PestQueen queen, Entity target);

	public Animation getAttackAnimation();

	public int getMaxHit();

	public boolean canAttack(PestQueen queen, Entity target);
}
