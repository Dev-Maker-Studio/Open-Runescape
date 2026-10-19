package com.rs.game.content.minigames.fightpits;

import com.rs.game.content.skills.magic.TeleType;
import com.rs.game.model.entity.player.Controller;
import com.rs.game.model.gameobject.GameObject;

public class FightPitsLobbyController extends Controller {

	@Override
	public void start() {

	}

	@Override
	public boolean login() {
		FightPits.enterLobby(player, true);
		return false;
	}

	@Override
	public void onTeleported(TeleType type) {
		FightPits.leaveLobby(player, 2);
	}

	@Override
	public boolean processObjectClick1(GameObject object) {
		if (object.getId() == 68223) {
			FightPits.leaveLobby(player, 1);
			return false;
		}
		if (object.getId() == 68222) {
			player.sendMessage("The heat prevents you passing through.");
			return false;
		}
		if (object.getId() == 68220) {
			player.getActionManager().setAction(new FightPitsViewingOrb());
			return false;
		}
		return true;
	}

	@Override
	public boolean sendDeath() {
		FightPits.leaveLobby(player, 2);
		return true;
	}

	@Override
	public boolean logout() {
		FightPits.leaveLobby(player, 0);
		return false;
	}

	@Override
	public void forceClose() {
		FightPits.leaveLobby(player, 2);
	}

}
