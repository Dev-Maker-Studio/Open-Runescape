package com.rs.game.content.world.areas.meiyerditch;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.LoginHandler;
import com.rs.plugin.handlers.ObjectClickHandler;
import org.darkan.core.gameval.gen.Varbit;

@PluginEventHandler
public class Meiyerditch {

	public static LoginHandler unlockVars = new LoginHandler(e -> {
		e.getPlayer().getVars().setVarBit(Varbit.myq3_sea_boat_visible, 1); //boat
		e.getPlayer().getVars().setVarBit(Varbit.myq3_wall_floorboards_down, 1); //kick down floor
	});

	public static ObjectClickHandler handleBoat = new ObjectClickHandler(false, new Object[] { 12945, 17955 }, e -> {
		if (e.getObjectId() == 12945)
			e.getPlayer().walkToAndExecute(Tile.of(3525, 3170, 0), () -> {
				e.getPlayer().faceObject(e.getObj());
				e.getPlayer().fadeScreen(() -> e.getPlayer().tele(Tile.of(3605, 3163, 0)));
			});
		else
			e.getPlayer().walkToAndExecute(Tile.of(3605, 3163, 0), () -> {
				e.getPlayer().faceObject(e.getObj());
				e.getPlayer().fadeScreen(() -> e.getPlayer().tele(Tile.of(3525, 3170, 0)));
			});
	});

	public static ObjectClickHandler handleRocks1 = new ObjectClickHandler(new Object[] { 17960, 17679 }, e -> e.getPlayer().useLadder(e.getPlayer().transform(e.getObjectId() == 17960 ? 4 : -4, 0, e.getObjectId() == 17960 ? -1 : 1)));

	public static ObjectClickHandler handleFloorClimb = new ObjectClickHandler(new Object[] { 18122, 18124 }, e -> e.getPlayer().useLadder(e.getObj().getTile().transform(e.getObjectId() == 18122 ? -1 : 1, 0, e.getObjectId() == 18122 ? -1 : 1)));

	public static ObjectClickHandler handleRubble = new ObjectClickHandler(new Object[] { 18037, 18038 }, e -> e.getPlayer().useLadder(e.getObj().getTile().transform(e.getObjectId() == 18037 ? 3 : -3, 0, 0)));

	public static ObjectClickHandler handleMeiyerditchstairs = new ObjectClickHandler(new Object[] { 17976, 17978 }, e -> {
		if (e.getObjectId() == 17976)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? 3 : e.getObj().getRotation() == 3 ? -3 : 0, e.getObj().getRotation() == 2 ? -3 : e.getObj().getRotation() == 0 ? 3 : 0,  1));
		else if (e.getObjectId() == 17978)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? -3 : e.getObj().getRotation() == 3 ? 3 : 0, e.getObj().getRotation() == 2 ? 3 : e.getObj().getRotation() == 0 ? -3 : 0, -1));
	});

}
