package com.rs.utils;

import com.rs.game.World;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Rights;
import org.darkan.core.Logger;
import org.darkan.world.server.WorldServer;

import java.util.HashMap;
import java.util.Map;
import org.darkan.core.social.gateway.AccountAction;

public class ReportsManager {

	public static void report(Player player) {
		report(player, null);
	}

	public static void report(Player player, String name) {
		if (player.getInterfaceManager().containsScreenInter()) {
			player.sendMessage("Please close the interface that you opened before activating the 'Report' system.");
			return;
		}
		if (name != null)
			player.getPackets().sendVarcString(24, name);
		if (player.hasRights(Rights.MOD))
			player.getPackets().setIFHidden(594, 8, false);
		player.getInterfaceManager().sendInterface(594);

	}

	public static void report(Player player, String displayName, int type, boolean mute) {
		if (mute && !player.hasRights(Rights.MOD))
			return;
		if (displayName == null)
			return;
		Player reported = World.getPlayerByUsername(displayName);
		if (reported == null)
			return;
		if (mute) {
			long until = AccountAction.days(2);
			reported.mute(until);
			reported.sendMessage("You've been muted for 2 days by " + player.getDisplayName() + ".");
			WorldServer.INSTANCE.accountAction(player, new AccountAction.Mute(reported.getUsername(), until));
		}
		player.sendMessage("Thank-you, your abuse report has been received.");
		try {
//			Rule rule = Rule.forId(type);
//			if (rule != null)
//				WorldDB.getLogs().logReport(player, reported, rule);
		} catch (Throwable e) {
			Logger.handle(ReportsManager.class, "report", e);
		}
	}

	public enum Rule {
		BUG_EXPLOITATION(4),
		STAFF_IMPERSONATION(5),
		REAL_WORLD_TRADING(6, 17),
		MACROING(7),
		ENCOURAGING_RULE_BREAKING(9),
		SCAMMING(15),
		OFFENSIVE_LANGUAGE(16),
		DISRUPTIVE_BEHAVIOR(18),
		OFFENSIVE_NAME(19),
		REAL_WORLD_OFFENSE(20, 21, 13),
		ADVERTISING(11);

		private static final Map<Integer, Rule> MAPPING = new HashMap<>();

		static {
			for (Rule rule : Rule.values()) {
				for (int id : rule.ids)
					MAPPING.put(id, rule);
			}
		}

		public static Rule forId(int id) {
			return MAPPING.get(id);
		}

		private final int[] ids;

		private Rule(int... ids) {
			this.ids = ids;
		}

		public int[] getIds() {
			return ids;
		}
	}

}
