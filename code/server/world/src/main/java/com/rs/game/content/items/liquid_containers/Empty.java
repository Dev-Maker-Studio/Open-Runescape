package com.rs.game.content.items.liquid_containers;

import com.rs.game.content.items.liquid_containers.FillAction.Filler;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemClickHandler;

@PluginEventHandler
public class Empty {
    public static ItemClickHandler handleGenericEmpty = new ItemClickHandler(new String[] { "Empty" }, e -> {
        Filler fillable = Filler.forFull(e.getItem().getId());
        if (fillable == null)
            return;
        e.getItem().setId(fillable.getEmptyItem().getId());
        e.getPlayer().getInventory().refresh(e.getSlotId());
    });

    public static ItemClickHandler potEmpty = new ItemClickHandler(new Object[] { 1933, 7468, 7811, 14816}, new String[] { "Empty" }, e -> {
        e.getItem().setId(1931);
        e.getPlayer().getInventory().refresh(e.getItem().getSlot());
    });

    public static ItemClickHandler teaPotBrownEmpty = new ItemClickHandler(new Object[] { 7692, 7694, 7696, 7698, 7700 }, new String[] { "Empty" }, e -> {
        e.getItem().setId(7702);
        e.getPlayer().getInventory().refresh(e.getItem().getSlot());
    });

    public static ItemClickHandler teaPotWhiteEmpty = new ItemClickHandler(new Object[] { 7704, 7706, 7708, 7710, 7712 }, new String[] { "Empty" }, e -> {
        e.getItem().setId(7714);
        e.getPlayer().getInventory().refresh(e.getItem().getSlot());
    });

    public static ItemClickHandler teaPotGoldEmpty = new ItemClickHandler(new Object[] { 7716, 7718, 7720, 7722, 7724 }, new String[] { "Empty" }, e -> {
        e.getItem().setId(7726);
        e.getPlayer().getInventory().refresh(e.getItem().getSlot());
    });


}
