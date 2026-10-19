package com.rs.game.content.skills.dungeoneering;

import com.rs.game.content.skills.magic.TeleType;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Controller;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class DamonheimController extends Controller {

	private boolean showingOption;
	
	public static ObjectClickHandler handleJumpDownExit = new ObjectClickHandler(new Object[] { 50552 }, e -> {
		if (e.getPlayer().getControllerManager().getController() instanceof DungeonController)
			e.getPlayer().getControllerManager().removeControllerWithoutCheck();
		e.getPlayer().forceMove(e.getObj().getTile(), 5, 35);
		e.getPlayer().getPackets().sendVarc(234, 0);// Party Config Interface
		e.getPlayer().getControllerManager().startController(new DamonheimController());
		e.getPlayer().useStairs(13760, Tile.of(3454, 3725, 0), 2, 3);
	});
	
	@Override
	public void start() {
		setInviteOption(true);
	}

	@Override
	public boolean canPlayerOption1(Player target) {
		player.setNextFaceTile(target.getTile());
		player.getDungManager().invite(target.getDisplayName());
		return false;
	}

	@Override
	public boolean login() {
		moved();
		DungeonManager.checkRejoin(player);
		return false;
	}

	@Override
	public void onTeleported(TeleType type) {
		setInviteOption(false);
		player.getDungManager().leaveParty();
		removeController();
	}

	@Override
	public boolean sendDeath() {
		setInviteOption(false);
		player.getDungManager().leaveParty();
		removeController();
		return true;
	}

	@Override
	public boolean logout() {
		return false; // so doesnt remove script
	}

	@Override
	public void forceClose() {
		setInviteOption(false);
	}

	/**
	 * return process normaly
	 */
	@Override
	public boolean processNPCClick1(NPC npc) {
		if (npc.getId() == 9707)
			player.getDungManager().leaveParty();
		return true;
	}

	/**
	 * return process normaly
	 */
	@Override
	public boolean processNPCClick2(NPC npc) {
		if (npc.getId() == 9707)
			player.getDungManager().leaveParty();
		return true;
	}

	@Override
	public void moved() {
		if (player.getDungManager().isInsideDungeon())
			return;
		if (!isAtKalaboss(player.getTile())) {
			setInviteOption(false);
			player.getDungManager().leaveParty();
			removeController();
		} else
			setInviteOption(true);
	}

	public static boolean isAtKalaboss(Tile tile) {
		return tile.getX() >= 3385 && tile.getX() <= 3513 && tile.getY() >= 3605 && tile.getY() <= 3794;
	}

	public void setInviteOption(boolean show) {
		if (show == showingOption)
			return;
		showingOption = show;
		player.setPlayerOption(show ? "Invite" : "null", 1);
	}
}
