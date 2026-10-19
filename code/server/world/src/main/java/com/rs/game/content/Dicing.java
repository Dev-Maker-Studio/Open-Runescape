package com.rs.game.content;

import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemClickHandler;

import java.util.Random;
import java.util.stream.IntStream;

@PluginEventHandler
public class Dicing {
	
	public static ItemClickHandler handleRoll = new ItemClickHandler(new Object[] { IntStream.range(15086, 15100).toArray() }, new String[] { "Drop" }, e -> handleRoll(e.getPlayer(), e.getItem().getId(), true));

	public static void handleRoll(final Player player, int itemId, boolean friends) {
		if (friends)
			switch (itemId) {
			case 15086:
				friendsRoll(player, itemId, 2072, 1, 6);
				break;
			case 15088:
				friendsRoll(player, itemId, 2074, 1, 12);
				break;
			case 15090:
				friendsRoll(player, itemId, 2071, 1, 8);
				break;
			case 15092:
				friendsRoll(player, itemId, 2070, 1, 10);
				break;
			case 15094:
				friendsRoll(player, itemId, 2073, 1, 12);
				break;
			case 15096:
				friendsRoll(player, itemId, 2068, 1, 20);
				break;
			case 15098:
				friendsRoll(player, itemId, 2075, 1, 100);
				break;
			case 15100:
				friendsRoll(player, itemId, 2069, 1, 4);
				break;
			}
		else
			switch (itemId) {
			case 15086:
				privateRoll(player, itemId, 2072, 1, 6);
				break;
			case 15088:
				privateRoll(player, itemId, 2074, 1, 12);
				break;
			case 15090:
				privateRoll(player, itemId, 2071, 1, 8);
				break;
			case 15092:
				privateRoll(player, itemId, 2070, 1, 10);
				break;
			case 15094:
				privateRoll(player, itemId, 2073, 1, 12);
				break;
			case 15096:
				privateRoll(player, itemId, 2068, 1, 20);
				break;
			case 15098:
				privateRoll(player, itemId, 2075, 1, 100);
				break;
			case 15100:
				privateRoll(player, itemId, 2069, 1, 4);
				break;
			}
	}

	public static void privateRoll(final Player player, final int itemId, int graphic, final int lowest, final int highest) {
		player.sendMessage("Rolling...", true);
		player.getInventory().deleteItem(itemId, 1);
		player.setNextAnimation(new Animation(11900));
		player.setNextSpotAnim(new SpotAnim(graphic));
		WorldTasks.schedule(new Task() {
			@Override
			public void run() {
				player.getInventory().addItem(itemId, 1);
				player.sendMessage("You rolled <col=db3535>" + getRandom(lowest, highest) + "</col> on " + diceText(itemId) + " die.", true);
			}
		}, 1);
	}

	//TODO
	public static void friendsRoll(final Player player, final int itemId, int graphic, final int lowest, final int highest) {
//				if (player.getAccount().getSocial().getCurrentFriendsChat() == null) {
//					player.sendMessage("You need to be in a friend chat to use this option.");
//					return;
//				}
//				player.lock(2);
//				player.sendMessage("Rolling...");
//				player.getInventory().deleteItem(itemId, 1);
//				player.setNextAnimation(new Animation(11900));
//				player.setNextSpotAnim(new SpotAnim(graphic));
//				WorldTasks.schedule(1, () -> {
//					player.getInventory().addItem(itemId, 1);
//					player.sendDiceMessage(player, "Friends Chat channel-mate <col=db3535>" + player.getDisplayName() + "</col> rolled <col=db3535>" + getRandom(lowest, highest) + "</col> on " + diceText(itemId) + " die.");
//				});
	}

	public static int getRandom(int lowest, int highest) {
		Random r = new Random();
		if (lowest > highest)
			return -1;
		long range = (long) highest - (long) lowest + 1;
		long fraction = (long) (range * r.nextDouble());
		int numberRolled = (int) (fraction + lowest);
		return numberRolled;
	}

	public static String diceText(int id) {
        return switch (id) {
            case 15086 -> "a six-sided";
            case 15088 -> "two six-sided";
            case 15090 -> "an eight-sided";
            case 15092 -> "a ten-sided";
            case 15094 -> "a twelve-sided";
            case 15096 -> "a a twenty-sided";
            case 15098 -> "the percentile";
            case 15100 -> "a four-sided";
            default -> "";
        };
    }

	public static int getGraphic(int id) {
		return 0;
	}
}
