package com.rs.game.content.minigames.fightkiln.npcs;

import com.rs.game.content.minigames.fightkiln.FightKilnController;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

public class TokHaarKetDill extends FightKilnNPC {

	private int receivedHits;

	public TokHaarKetDill(int id, Tile tile, FightKilnController controller) {
		super(id, tile, controller);
	}

	@Override
	public void handlePreHit(final Hit hit) {
		if (hit.getLook() != HitLook.MELEE_DAMAGE && hit.getLook() != HitLook.RANGE_DAMAGE && hit.getLook() != HitLook.MAGIC_DAMAGE)
			return;
		if (receivedHits != -1) {
			Entity source = hit.getSource();
			if (source == null || !(source instanceof Player playerSource))
				return;
			hit.setDamage(0);
            int weaponId = playerSource.getEquipment().getWeaponId();
			if (weaponId == 1275 || weaponId == 13661 || weaponId == 15259) {
				receivedHits++;
				if ((weaponId == 1275 && receivedHits >= 5) || ((weaponId == 13661 || weaponId == 15259) && receivedHits >= 3)) {
					receivedHits = -1;
					transformIntoNPC(getId() + 1);
					playerSource.sendMessage("Your pickaxe breaks the TokHaar-Ket-Dill's thick armour!");
				} else
					playerSource.sendMessage("Your pickaxe slowy  cracks its way through the TokHaar-Ket-Dill's armour.");
			}
		}

	}

}
