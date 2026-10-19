package com.rs.game.content.world.areas.pollnivneach;

import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class PollnivneachSlayerDungeon {

	public static ObjectClickHandler handleBarriers = new ObjectClickHandler(new Object[] { 31435, 31436 }, e -> {
		e.getPlayer().setNextSpotAnim(new SpotAnim(1659));
		if (e.getObjectId() == 31435)
			e.getPlayer().forceMove(e.getPlayer().transform(e.getPlayer().getX() > e.getObj().getX() ? -2 : 2, 0), 10584, 0, 30);
		else
			e.getPlayer().forceMove(e.getPlayer().transform(0, e.getPlayer().getY() > e.getObj().getY() ? -2 : 2), 10584, 0, 30);
	});

	public static ObjectClickHandler handleDownStairsNE = new ObjectClickHandler(new Object[] { 31412 }, new Tile[] { Tile.of(3374, 9426, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(-52, -5061)));

	public static ObjectClickHandler handleUpStairsNE = new ObjectClickHandler(new Object[] { 31417 }, new Tile[] { Tile.of(3317, 4364, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(52, 5061)));

	public static ObjectClickHandler handleDownStairsSE = new ObjectClickHandler(new Object[] { 31412 }, new Tile[] { Tile.of(3377, 9367, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(-54, -5028)));

	public static ObjectClickHandler handleUpStairsSE = new ObjectClickHandler(new Object[] { 31417 }, new Tile[] { Tile.of(3318, 4339, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(54, 5028)));

	public static ObjectClickHandler handleDownStairsSW = new ObjectClickHandler(new Object[] { 31412 }, new Tile[] { Tile.of(3338, 9368, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(-72, -5029)));

	public static ObjectClickHandler handleUpStairsSW = new ObjectClickHandler(new Object[] { 31417 }, new Tile[] { Tile.of(3271, 4339, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(72, 5029)));

	public static ObjectClickHandler handleDownStairsNW = new ObjectClickHandler(new Object[] { 31412 }, new Tile[] { Tile.of(3340, 9426, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(-68, -5059)));

	public static ObjectClickHandler handleUpStairsNW = new ObjectClickHandler(new Object[] { 31417 }, new Tile[] { Tile.of(3277, 4367, 0) }, e -> e.getPlayer().useStairs(e.getPlayer().transform(68, 5059)));

	public static ObjectClickHandler handlepulsingmarkingkurask = new ObjectClickHandler(new Object[] { 31363 }, Tile.of(3295, 4407, 0), e -> e.getPlayer().tele(Tile.of(2700, 9999, 0)));

	public static ObjectClickHandler handlepulsingmarkingcavecrawler = new ObjectClickHandler(new Object[] { 31363 }, Tile.of(3325, 4391, 0), e -> e.getPlayer().tele(Tile.of(2789, 9997, 0)));

	public static ObjectClickHandler handlepulsingmarkingbasilisk = new ObjectClickHandler(new Object[] { 31363 }, Tile.of(3294, 4293, 0), e -> e.getPlayer().tele(Tile.of(2743, 10009, 0)));
	public static ObjectClickHandler handlepulsingmarkingturoth = new ObjectClickHandler(new Object[] { 31363 }, Tile.of(3288, 4293, 0), e -> e.getPlayer().tele(Tile.of(2723, 10005, 0)));
}
