package com.rs.game.content.world.areas.ullek;


import com.rs.game.content.skills.agility.Agility;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnObjectHandler;
import com.rs.plugin.handlers.ObjectClickHandler;
//wrong animations
@PluginEventHandler
public class Ullek {

	public static ObjectClickHandler handlereeds = new ObjectClickHandler(new Object[] { 28474 }, e -> {
		if (e.getObj().getRotation() == 3 || e.getObj().getRotation() == 1)
			Agility.handleObstacle(e.getPlayer(), 3303, 1, e.getPlayer().transform(e.getPlayer().getX() < e.getObj().getX() ? 2 : -2, 0, 0), 0);
		else
			Agility.handleObstacle(e.getPlayer(), 3303, 1, e.getPlayer().transform(0, e.getPlayer().getY() < e.getObj().getY() ? 2 : -2, 0), 0);
	});//wrong animation

	public static ObjectClickHandler handleplateauentrance = new ObjectClickHandler(new Object[] { 28515 }, e -> e.getPlayer().tele(Tile.of(3419, 2803, 1)));

	public static ObjectClickHandler handleplateauexit= new ObjectClickHandler(new Object[] { 28516 }, e -> e.getPlayer().tele(Tile.of(3419, 2801, 0)));

	public static ObjectClickHandler handlelowall = new ObjectClickHandler(new Object[] { 28512 }, e -> {
		if (e.getObj().getRotation() == 3 || e.getObj().getRotation() == 1)
			Agility.handleObstacle(e.getPlayer(), 3303, 1, e.getPlayer().transform(e.getPlayer().getX() < e.getObj().getX() ? 2 : -2, 0, 0), 0);
		else
			Agility.handleObstacle(e.getPlayer(), 3303, 1, e.getPlayer().transform(0, e.getPlayer().getY() < e.getObj().getY() ? 2 : -2, 0), 0);
	});//wrong animation

	public static ObjectClickHandler handleullekentrance= new ObjectClickHandler(new Object[] { 28481 }, e -> e.getPlayer().tele(Tile.of(3448, 9252, 1)));

	public static ObjectClickHandler handleullekexit= new ObjectClickHandler(new Object[] { 28401 }, e -> e.getPlayer().tele(Tile.of(3412, 2847, 1)));

	public static ObjectClickHandler handlefloortrapullek = new ObjectClickHandler(new Object[] { 28525 }, e -> {
		if (e.getObj().getRotation() == 3 || e.getObj().getRotation() == 1)
			Agility.handleObstacle(e.getPlayer(), 3303, 1, e.getPlayer().transform(e.getPlayer().getX() < e.getObj().getX() ? 2 : -2, 0, 0), 0);
		else
			Agility.handleObstacle(e.getPlayer(), 3303, 1, e.getPlayer().transform(0, e.getPlayer().getY() < e.getObj().getY() ? 2 : -2, 0), 0);
	});//wrong animation and trap

	public static ItemOnObjectHandler TEMPneedropedecendrock = new ItemOnObjectHandler(new Object[] { 28494 }, new Object[] { 954 }, e -> {
		if (e.getPlayer().getX() == 3382 && e.getPlayer().getY() == 2823) {
			e.getPlayer().tele(Tile.of(3382, 2826, 0));
		} else
			e.getPlayer().sendMessage("You are too far away to do this.");
	});

}
