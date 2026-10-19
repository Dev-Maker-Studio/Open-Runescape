package com.rs.game.content.world.areas.dungeons.ancientcavern;

import org.darkan.core.map.pathfinder.Direction;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class AncientCavern {

	public static int FIXED_RING_VARBIT = 6774;

	public static ObjectClickHandler handleFixRing = new ObjectClickHandler(new Object[] { "Enchanted land" }, e -> {
		if (!e.getOption().equals("Plant")) {
			e.getPlayer().sendMessage("YIKES");
			return;
		}
		if (!e.getPlayer().getInventory().containsItem(6004, 5)) {
			e.getPlayer().sendMessage("You need 5 bittercap mushrooms to repair the fairy ring.");
			return;
		}
		e.getPlayer().setNextAnimation(new Animation(2291));
		e.getPlayer().getInventory().deleteItem(6004, 5);
		e.getPlayer().getVars().saveVarBit(FIXED_RING_VARBIT, 1);
	});
	
	public static ObjectClickHandler handleRoughSteps = new ObjectClickHandler(new Object[] { 25337, 39468 }, e -> {
		switch(e.getObjectId()) {
		case 25337 -> e.getPlayer().tele(Tile.of(1744, 5321, 1));
		case 39468 -> e.getPlayer().tele(Tile.of(1745, 5325, 0));
		}
	});

	public static ObjectClickHandler handleMithrilDoor = new ObjectClickHandler(new Object[] { 25341, 40208 }, e -> e.getPlayer().useStairs(e.getObjectId() == 25341 ? Tile.of(1823, 5273, 0) : Tile.of(1759, 5342, 1)));

	public static ObjectClickHandler handleDownStepsEntrance = new ObjectClickHandler(new Object[] { 25338 }, e -> e.getPlayer().tele(Tile.of(1772, 5366, 0)));

	public static ObjectClickHandler handleUpStepsEntrance = new ObjectClickHandler(new Object[] { 25336 }, e -> e.getPlayer().tele(Tile.of(1768, 5366, 1)));

	public static ObjectClickHandler handleDownStepsMithDrags = new ObjectClickHandler(new Object[] { 25340 }, e -> e.getPlayer().tele(Tile.of(1778, 5346, 0)));

	public static ObjectClickHandler handleUpStepsMithDrags = new ObjectClickHandler(new Object[] { 25339 }, e -> e.getPlayer().tele(Tile.of(1778, 5343, 1)));

//	public static ObjectClickHandler handleDownStepsKuradal = new ObjectClickHandler(new Object[] { 39468 }) {
//		@Override
//		public void handle(ObjectClickEvent e) {
//			e.getPlayer().tele(Tile.of(1744, 5325, 0));
//		}
//	};

//	public static ObjectClickHandler handleUpStepsKuradal = new ObjectClickHandler(new Object[] { 25337 }) {
//		@Override
//		public void handle(ObjectClickEvent e) {
//			e.getPlayer().tele(e.getPlayer().isQuestComplete(Quest.WHILE_GUTHIX_SLEEPS) ? Tile.of(1774, 5321, 1) : Tile.of(1694, 5296, 1));
//		}
//	};

	public static ObjectClickHandler handleWhirlpool = new ObjectClickHandler(new Object[] { 67966 }, e -> {
		e.getPlayer().lock();
		e.getPlayer().setRunHidden(false);
		e.getPlayer().walkToAndExecute(Tile.of(2512, 3516, 0), () -> {
			e.getPlayer().setFaceAngle(Direction.SOUTH.getAngle());
			e.getPlayer().forceMove(Tile.of(2512, 3508, 0), 6723, 30, 7*30, () -> {
				e.getPlayer().anim(-1);
				e.getPlayer().tele(Tile.of(1764, 5365, 1));
				e.getPlayer().setRunHidden(true);
			});
		});
	});

}
