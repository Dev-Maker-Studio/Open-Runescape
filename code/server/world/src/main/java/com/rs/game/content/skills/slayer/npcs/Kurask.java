package com.rs.game.content.skills.slayer.npcs;

import com.rs.game.content.combat.AmmoType;
import com.rs.game.content.combat.RangedWeapon;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class Kurask extends NPC {

	public Kurask(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void handlePreHit(Hit hit) {
		if (hit.getSource() instanceof Player player) {
            RangedWeapon weapon = RangedWeapon.forId(player.getEquipment().getWeaponId());
			AmmoType ammo = AmmoType.forId(player.getEquipment().getAmmoId());
			if (!(player.getEquipment().getWeaponId() == 13290 || player.getEquipment().getWeaponId() == 4158) && !(weapon != null && weapon.getAmmos() != null && ammo != null && (ammo == AmmoType.BROAD_ARROW || ammo == AmmoType.BROAD_TIPPED_BOLTS)))
				hit.setDamage(0);
		}
		super.handlePreHit(hit);
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 1608, 1609 }, Kurask::new);
}
