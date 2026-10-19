package com.rs.game.content.holidayevents.halloween.hw09;

import org.darkan.core.cache.ObjectDefinitions;
import com.rs.engine.dialogue.Dialogue;
import com.rs.game.content.skills.magic.TeleType;
import com.rs.game.model.entity.Teleport;
import com.rs.game.model.entity.player.Controller;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import java.util.HashSet;
import java.util.Set;
import org.darkan.core.gameval.gen.Varbit;


public class Halloween2009Controller extends Controller {

	private final Set<Integer> path;
	private final Set<Integer> webbedUp;

    public Halloween2009Controller() {
		path = Halloween2009.getRandomPath();
		webbedUp = new HashSet<>();
	}

	@Override
	public void start() {
		player.tele(Halloween2009.START_LOCATION);
	}

	@Override
	public boolean sendDeath() {
		player.lock(7);
		player.stopAll();
		WorldTasks.scheduleLooping(new Task() {
			int loop;

			@Override
			public void run() {
				if (loop == 0)
					player.setNextAnimation(new Animation(836));
				else if (loop == 1)
					player.sendMessage("Oh dear, you have died.");
				else if (loop == 3) {
					player.tele(player.getI(Halloween2009.STAGE_KEY) < 10 ? Halloween2009.START_LOCATION : Tile.of(3211, 3424, 0));
					player.reset();
					player.setNextAnimation(new Animation(-1));
				} else if (loop == 4) {
					if (player.getI(Halloween2009.STAGE_KEY) >= 10)
						player.getControllerManager().forceStop();
					player.jingle(90);
					stop();
				}
				loop++;
			}
		}, 0, 1);
		return false;
	}

	@Override
	public boolean processObjectClick1(GameObject object) {
		switch(object.getId()) {
		case 46935:
			player.sendOptionDialogue("Are you sure you want to leave? Any maze/webbing progress will be reset!", ops -> {
				ops.add("Yes, I understand.", () -> {
					player.getControllerManager().forceStop();
					player.useStairs(Tile.of(3211, 3424, 0));
				});
				ops.add("Nevermind.");
			});
			break;
		}
		return true;
	}

	@Override
	public void process() {
		if (player.getI(Halloween2009.STAGE_KEY) >= 3)
			refreshVars(player);
	}

	@Override
	public boolean login() {
		Halloween2009.refreshWebbables(player, player.getEquipment().getWeaponId() == 15353);
		return false;
	}

	@Override
	public boolean logout() {
		return false;
	}

	@Override
	public void onTeleported(TeleType type) {

	}

	@Override
	public void forceClose() {

	}

	@Override
	public boolean processTeleport(Teleport tele) {
		player.sendMessage("A mysterious force prevents you from teleporting.");
		return false;
	}

	private void refreshVars(Player player) {
		if (player.getI(Halloween2009.STAGE_KEY) >= 10)
			player.getVars().setVarBit(Varbit.hw09_event, 50);
		else if (player.getI(Halloween2009.STAGE_KEY) >= 3)
			player.getVars().setVarBit(Varbit.hw09_event, 20);
	}

	public Set<Integer> getPath() {
		return path;
	}

	public void web(int objectId) {
		webbedUp.add(objectId);
		Halloween2009.refreshWebbables(player, player.getEquipment().getWeaponId() == 15353);
        int WEBS_TOTAL = 63;
        int numLeft = WEBS_TOTAL -webbedUp.size();
		if (numLeft > 0)
			player.sendMessage("You web up the " + ObjectDefinitions.getDefs(objectId, player.getVars()).getName().toLowerCase() + ". Only <col=FF0000>" + numLeft + "</col> more to go!");
		else {
			player.set(Halloween2009.STAGE_KEY, 8);
			player.sendMessage("<col=FF0000>That's all of them! Return to the Grim Reaper.");
			player.startConversation(new Dialogue().addNext(new SpiderStatement("That's it! We've done them all! Now we should talk to the scary skeleton guy.")));
		}
	}

	public Set<Integer> getWebbedUp() {
		return webbedUp;
	}
}
