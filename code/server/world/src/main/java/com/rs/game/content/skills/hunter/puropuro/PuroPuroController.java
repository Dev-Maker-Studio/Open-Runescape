package com.rs.game.content.skills.hunter.puropuro;

import com.rs.game.content.skills.magic.Magic;
import com.rs.game.content.skills.magic.TeleType;
import com.rs.game.model.entity.player.Controller;
import com.rs.game.model.gameobject.GameObject;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;

@PluginEventHandler
public class PuroPuroController extends Controller {

	private final Tile entranceTile;

	public PuroPuroController(Tile tile) {
		this.entranceTile = tile;
	}

	@Override
	public void start() {
		player.getPackets().setBlockMinimapState(2);
		player.getInterfaceManager().sendOverlay(169);
	}

	@Override
	public void forceClose() {
		player.getPackets().setBlockMinimapState(0);
		player.getInterfaceManager().removeOverlay();
	}

	@Override
	public void onTeleported(TeleType type) {
		player.getControllerManager().forceStop();
	}

	@Override
	public boolean logout() {
		return false;
	}

	@Override
	public boolean login() {
		start();
		return false;
	}

	@Override
	public boolean processObjectClick1(GameObject object) {
		switch (object.getId()) {
			case 25014:
				player.getControllerManager().forceStop();
				Magic.sendTeleportSpell(player, 6601, -1, 1118, -1, 0, 0, entranceTile, 9, false, TeleType.OBJECT, null);
				return true;
		}
		return true;
	}

}