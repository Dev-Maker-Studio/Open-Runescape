package com.rs.game.content.world.areas.citharede;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class Citharede {

	//Citharede
	public static ObjectClickHandler handlewinchladderup = new ObjectClickHandler(new Object[] { 63591 }, e -> e.getPlayer().tele(Tile.of(3415, 3159, 1)));

	public static ObjectClickHandler handlewinchladderdown= new ObjectClickHandler(new Object[] { 63592 }, e -> e.getPlayer().tele(Tile.of(3415, 3161, 0)));

	public static ObjectClickHandler handlespiraltowerup = new ObjectClickHandler(new Object[] { 63583 }, e -> e.getPlayer().tele(Tile.of(3449, 3174, 1)));

	public static ObjectClickHandler handlespiraltowerdown= new ObjectClickHandler(new Object[] { 63584 }, e -> e.getPlayer().tele(Tile.of(3446, 3177, 0)));

	public static ObjectClickHandler handlespiraltowerup2 = new ObjectClickHandler(new Object[] { 63585 }, e -> e.getPlayer().tele(Tile.of(3447, 3179, 2)));

	public static ObjectClickHandler handlespiraltowerdown2= new ObjectClickHandler(new Object[] { 63586 }, e -> e.getPlayer().tele(Tile.of(3451, 3176, 1)));

	public static ObjectClickHandler handlespiraltowerup3 = new ObjectClickHandler(new Object[] { 63587 }, e -> e.getPlayer().tele(Tile.of(3447, 3179, 3)));

	public static ObjectClickHandler handlespiraltowerdown3= new ObjectClickHandler(new Object[] { 63588 }, e -> e.getPlayer().tele(Tile.of(3450, 3175, 2)));



}
