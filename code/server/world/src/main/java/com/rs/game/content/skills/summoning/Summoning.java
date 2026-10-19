package com.rs.game.content.skills.summoning;

import org.darkan.core.cache.EnumDefinitions;
import org.darkan.core.cache.ItemDefinitions;
import world.gregs.voidps.cache.definition.data.EnumDefinition;
import org.darkan.core.cache.NPCDefinitions;
import org.darkan.core.model.IFEvents;
import com.rs.game.content.ItemConstants;
import com.rs.game.content.skills.dungeoneering.DungeonController;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Constants;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;

import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.*;

import java.util.Arrays;

@PluginEventHandler
public class Summoning {

	public static enum ScrollTarget {
		ITEM, COMBAT, CLICK, OBJECT, ENTITY
	}

	public static final int POUCHES_INTERFACE = 672, SCROLLS_INTERFACE = 666;
	private static final int SCROLL_ANIM = 723;
	private static final int POUCH_ANIM = 725;
	private static final int POUCH_SPOTANIM = 1207;

	public static boolean isFamiliar(int npcId) {
		return EnumDefinitions.getEnum(1320).getValues().containsValue(npcId);
	}

	public static boolean isFollower(int npcId) {
		return EnumDefinitions.getEnum(1279).getValues().containsKey((long) npcId);
	}
	
	public static ItemOnItemHandler handleCarveTurnip = new ItemOnItemHandler(946, 12134, e -> e.getPlayer().repeatAction(2, count -> {
        if (!e.getPlayer().getInventory().containsItem(946, 1)) {
            e.getPlayer().sendMessage("You need a knife to cut the turnip.");
            return false;
        }
        if (!e.getPlayer().getInventory().containsItem(12134))
            return false;
        e.getPlayer().getInventory().deleteItem(12134, 1);
        e.getPlayer().getInventory().addItem(12153, 1);
        e.getPlayer().anim(6702);
        return true;
    }));

	public static ItemClickHandler handleSummonOps = new ItemClickHandler(Arrays.stream(Pouch.values()).map(Pouch::getId).toArray(), new String[] { "Summon" }, e -> {
		Pouch pouches = Pouch.forId(e.getItem().getId());
		if (pouches != null) {
			if (e.getPlayer().getSkills().getLevelForXp(Constants.SUMMONING) >= pouches.getLevel())
				spawnFamiliar(e.getPlayer(), pouches);
			else
				e.getPlayer().sendMessage("You need a summoning level of " + pouches.getLevel() + " to summon this familiar.");
		}
	});
	
	public static InterfaceOnPlayerHandler scrollOnPlayer = new InterfaceOnPlayerHandler(false, new int[] { 662, 747 }, e -> {
		if (e.getPlayer().getFamiliar() == null)
			return;
		e.getPlayer().stopAll(false, true, false);
		if ((e.getInterfaceId() == 747 && e.getComponentId() == 15) || (e.getInterfaceId() == 662 && e.getComponentId() == 65) || e.getInterfaceId() == 747 && e.getComponentId() == 24)
			e.getPlayer().getFamiliar().commandAttack(e.getTarget());
		if ((e.getInterfaceId() == 662 && e.getComponentId() == 74) || (e.getInterfaceId() == 747 && e.getComponentId() == 18))
			e.getPlayer().getFamiliar().executeSpecial(e.getTarget());
	});
	
	public static InterfaceOnNPCHandler scrollOnNPC = new InterfaceOnNPCHandler(false, new int[] { 662, 747 }, e -> {
		if (e.getPlayer().getFamiliar() == null)
			return;
		e.getPlayer().stopAll(false, true, false);
		if ((e.getInterfaceId() == 747 && e.getComponentId() == 15) || (e.getInterfaceId() == 662 && e.getComponentId() == 65) || e.getInterfaceId() == 747 && e.getComponentId() == 24)
			e.getPlayer().getFamiliar().commandAttack(e.getTarget());
		if ((e.getInterfaceId() == 662 && e.getComponentId() == 74) || (e.getInterfaceId() == 747 && e.getComponentId() == 18))
			e.getPlayer().getFamiliar().executeSpecial(e.getTarget());
	});
	
	private static void spawnFamiliar(Player player, Pouch pouch) {
		if (player.getFamiliar() != null || player.getPet() != null) {
			player.sendMessage("You already have a follower.");
			return;
		}
		if (!player.getControllerManager().canSummonFamiliar())
			return;
		if (player.getSkills().getLevel(Constants.SUMMONING) < pouch.getSummoningCost()) {
			player.sendMessage("You do not have enough summoning points to spawn this.");
			return;
		}
		if (player.getSkills().getLevelForXp(Constants.SUMMONING) < pouch.getLevel()) {
			player.sendMessage("You need a summoning level of " + pouch.getLevel() + " in order to use this pouch.");
			return;
		}
		Tile spawnTile = player.getNearestTeleTile(NPCDefinitions.getDefs(pouch.getBaseNpc(), player.getVars()).getSize());
		if (spawnTile == null) {
			player.sendMessage("Theres not enough space to summon your familiar here.");
			return;
		}
		player.getInventory().deleteItem(pouch.getId(), 1);
		player.getSkills().drainSummoning(pouch.getSummoningCost());
		player.setFamiliar(new Familiar(player, pouch, spawnTile, -1, true));
		player.getFamiliar().sendFollowerDetails();
	}

	public static boolean hasPouch(Player player) {
		for (Pouch pouch : Pouch.values())
			if (player.getInventory().containsOneItem(pouch.getId()))
				return true;
		return false;
	}

	public static void openInfusionInterface(Player player, boolean dung) {
		player.getInterfaceManager().sendInterface(POUCHES_INTERFACE);
		player.getPackets().sendPouchInfusionOptionsScript(dung, POUCHES_INTERFACE, 16, 78, 8, 10, "Infuse<col=FF9040>", "Infuse-5<col=FF9040>", "Infuse-10<col=FF9040>", "Infuse-X<col=FF9040>", "Infuse-All<col=FF9040>", "List<col=FF9040>");
		player.getPackets().setIFEvents(new IFEvents(POUCHES_INTERFACE, 16, 0, 462).enableRightClickOptions(0,1,2,3,4,6));
	}

	//1182 = real pouch list
	public static int forSlotId(int slotId, boolean dung) {
		EnumDefinition realDef = EnumDefinitions.getEnum(1182);
		return realDef.getIntValue(((slotId-2)/5)+(dung ? 1100 : 1));
	}

	public static ButtonClickHandler handleDungeoneeringPouchButtons = new ButtonClickHandler(672, e -> {
		if (e.getComponentId() == 16) {
			Pouch pouch = Pouch.forId(e.getSlotId2());
			if (pouch == null) {
				if(e.getPlayer().getControllerManager().isIn(DungeonController.class))
					e.getPlayer().sendMessage("You need " + getMaterialListString(Pouch.forId(forSlotId(e.getSlotId(), true))) + " to create this pouch.");
				else
					e.getPlayer().sendMessage("You need " + getMaterialListString(Pouch.forId(forSlotId(e.getSlotId(), false))) + " to create this pouch.");
				return;
			}
			if (e.getOpNum() == 1)
				handlePouchInfusion(e.getPlayer(), pouch, 1);
			else if (e.getOpNum() == 2)
				handlePouchInfusion(e.getPlayer(), pouch, 5);
			else if (e.getOpNum() == 3)
				handlePouchInfusion(e.getPlayer(), pouch, 10);
			else if (e.getOpNum() == 4)
				handlePouchInfusion(e.getPlayer(), pouch, Integer.MAX_VALUE);
			else if (e.getOpNum() == 5)
				handlePouchInfusion(e.getPlayer(), pouch, 28);
			else if (e.getOpNum() == 7)
				e.getPlayer().sendMessage("You need " + getMaterialListString(pouch) + " to create this pouch.");
		} else if (e.getComponentId() == 19 && e.getOpNum() == 1)
			openScrollInfusionInterface(e.getPlayer(), e.getPlayer().getControllerManager().isIn(DungeonController.class));
	});
	
	public static void openScrollInfusionInterface(Player player, boolean dung) {
		player.getInterfaceManager().sendInterface(SCROLLS_INTERFACE);
		player.getPackets().sendScrollInfusionOptionsScript(dung, SCROLLS_INTERFACE, 16, 78, 8, 10, "Transform<col=FF9040>", "Transform-5<col=FF9040>", "Transform-10<col=FF9040>", "Transform-All<col=FF9040>", "Transform-X<col=FF9040>");
		player.getPackets().setIFEvents(new IFEvents(SCROLLS_INTERFACE, 16, 0, 462).enableRightClickOptions(0,1,2,3,4,5));
	}

	public static ButtonClickHandler handleScrollButtons = new ButtonClickHandler(666, e -> {
		if (e.getComponentId() == 16) {
			if (e.getOpNum() == 1)
				createScroll(e.getPlayer(), e.getSlotId2(), 1);
			else if (e.getOpNum() == 2)
				createScroll(e.getPlayer(), e.getSlotId2(), 5);
			else if (e.getOpNum() == 3)
				createScroll(e.getPlayer(), e.getSlotId2(), 10);
			else if (e.getOpNum() == 4)
				createScroll(e.getPlayer(), e.getSlotId2(), Integer.MAX_VALUE);
		} else if (e.getComponentId() == 18 && e.getOpNum() == 1)
			openInfusionInterface(e.getPlayer(), e.getPlayer().getControllerManager().isIn(DungeonController.class));
	});

	public static void createScroll(Player player, int itemId, int amount) {
		Scroll scroll = Scroll.forId(itemId);
		if (scroll == null) {
			player.sendMessage("You do not have the pouch required to create this scroll.");
			return;
		}
		if (player.getSkills().getLevelForXp(Constants.SUMMONING) < scroll.fromPouches().getFirst().getLevel()) {
			player.sendMessage("You need a summoning level of " + scroll.fromPouches().getFirst().getLevel() + " to create " + amount + " " + ItemDefinitions.getDefs(scroll.getId()).getName().toLowerCase() + "s.");
			return;
		}
		boolean hasReqs = false;
		for (Pouch pouch : scroll.fromPouches()) {
			if (player.getInventory().containsItem(pouch.getId()))
				hasReqs = true;
		}
		if (!hasReqs) {
			player.sendMessage("You do not have the pouch required to create this scroll.");
			return;
		}
		for (int i = 0;i < amount;i++) {
			Pouch pouch = null;
			for (Pouch p : scroll.fromPouches()) {
				if (player.getInventory().containsItem(p.getId())) {
					pouch = p;
					break;
				}
			}
			if (pouch == null)
				break;
			player.getInventory().deleteItem(pouch.getId(), 1);
			player.getInventory().addItem(pouch.getScroll().getId(), 10);
			player.getSkills().addXp(Constants.SUMMONING, pouch.getScroll().getXp());
		}
		player.anim(SCROLL_ANIM);
	}


	public static void handlePouchInfusion(Player player, Pouch pouch, int creationCount) {
		if (ItemConstants.isDungItem(pouch.getId()) && !player.getControllerManager().isIn(DungeonController.class))
			return;
		if (!ItemConstants.isDungItem(pouch.getId()) && player.getControllerManager().isIn(DungeonController.class))
			return;
		Item[] itemReq = (pouch == Pouch.PRAYING_MANTIS) ? PouchMaterialList.getMantisMaterialList(player) : pouch.getMaterialList().get();
		if (itemReq == null)
			return;
		for (int i = 0; i < creationCount; i++) {
			if (!player.getInventory().containsItems(itemReq)) {
				player.sendMessage("You need " + getMaterialListString(pouch) + " to create this pouch.");
				return;
			}
			if (player.getSkills().getLevelForXp(Constants.SUMMONING) < pouch.getLevel()) {
				player.sendMessage("You need a summoning level of " + pouch.getLevel() + " to create this pouch.");
				return;
			}
			player.getInventory().removeItems(itemReq);
			player.getInventory().addItem(new Item(pouch.getId(), 1));
			player.getSkills().addXp(Constants.SUMMONING, pouch.getExperience());
		}
		player.sync(POUCH_ANIM, POUCH_SPOTANIM);
	}

	public static String getMaterialListString(Pouch pouch) {
		StringBuilder list = new StringBuilder();
		if (pouch == null)
			return "nothing";
		if (pouch == Pouch.PRAYING_MANTIS) {
			return "168 spirit shards, 1 crimson charm, 1 pouch, and 1 flowers (any except black or white)";
		}
		Item[] items = pouch.getMaterialList().get();
		for (int i = 0;i < items.length;i++)
			list.append(items[i].getAmount()).append(" ").append(items[i].getDefinitions().getName().toLowerCase()).append((i == items.length - 1) ? "" : ", ");
		return list.toString();
	}
}
