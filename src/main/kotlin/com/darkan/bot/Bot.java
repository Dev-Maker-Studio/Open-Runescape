package com.darkan.bot;

import com.darkan.bot.scripts.ScriptExecutor;
import com.darkan.bot.utils.*;
import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.cache.node.HashTableIterator;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;

import java.text.NumberFormat;
import java.util.*;

import static com.darkan.bot.UIKt.initializeUI;

public class Bot {

	public static String IP = "prod.darkan.org";

	public static String PROXY_IP = "147.135.15.128";
	public static int PROXY_PORT = -1;

	public static final int ATTACK = 0, DEFENCE = 1, STRENGTH = 2, HITPOINTS = 3, RANGE = 4, PRAYER = 5, MAGIC = 6, COOKING = 7, WOODCUTTING = 8, FLETCHING = 9, FISHING = 10, FIREMAKING = 11, CRAFTING = 12, SMITHING = 13, MINING = 14, HERBLORE = 15, AGILITY = 16, THIEVING = 17, SLAYER = 18, FARMING = 19, RUNECRAFTING = 20, CONSTRUCTION = 22, HUNTER = 21, SUMMONING = 23, DUNGEONEERING = 24;

	public static final boolean randomMac = true;

	public static boolean TRIVIA_ENABLED = false;

	public static boolean disableInterfaces = false;

	public static boolean disableIncomingPackets = false;

	public static boolean usingSmartResponse = false;
	public static boolean safetyLogout = false;

	public static NpcEntity currentTarget = null;

	public static ArrayList<String> responsePlayers = new ArrayList<String>();

	public static String storedResponse = "";
	public static String userspam = "";

	public static ContainerWrapper inventory = new ContainerWrapper(new ItemContainer());
	public static ContainerWrapper bank = new ContainerWrapper(new ItemContainer());

	public static String username = "";
	public static String password = "";

	public static long lastAnimated = System.currentTimeMillis();

	public static String targetNpc;
	public static String uN;
	public static String uP;

	public static String[] optionsDialogue = new String[5];

	public static int moneyPouch;

	public static void main(String[] args) {
		//DiscordRPC.discordInitialize("671478034871877652", null, true);
		ScriptExecutor.INSTANCE.loadScripts();
		initializeUI();
		Loader.main(args);
	}
	
	public static void pulse() {
        try {
            ScriptExecutor.INSTANCE.mainLogic();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

	public static boolean bankIsOpen() {
		return interfaceOpen(762);
	}

	public static void refreshLastAnimated() {
		lastAnimated = System.currentTimeMillis();
	}

	public static long getTimeSinceLastAnimation() {
		return System.currentTimeMillis() - lastAnimated;
	}

	public static Tile getMyPlayerPosition() {
		return new Tile(getMyPlayerX(), getMyPlayerY(), getMyPlayer().plane);
	}

	public static int getCurrentAnimation() {
		if (getMyPlayer().currentAnimations == null)
			return -1;
		else
			return getMyPlayer().currentAnimations[0];
	}

	public static long millisElapsed(long startTime) {
		return System.currentTimeMillis() - startTime;
	}

	public static float secondsElapsed(long startTime) {
		return millisElapsed(startTime) / 1000;
	}

	public static float minutesElapsed(long startTime) {
		return secondsElapsed(startTime) / 60;
	}

	public static float hoursElapsed(long startTime) {
		return minutesElapsed(startTime) / 60;
	}

	public static int getXpPerHour(int skillId, int startingXp, long startTime) {
		return (int) ((Bot.getXp(skillId) - startingXp) / hoursElapsed(startTime));
	}

	public static String getFormattedXpPerHour(int skillId, int startingXp, long startTime) {
		return format(getXpPerHour(skillId, startingXp, startTime));
	}

	public static String format(int number) {
		return NumberFormat.getNumberInstance(Locale.US).format(number);
	}

	public static boolean isAnimating() {
		return getCurrentAnimation() != -1;
	}

	public static void tradePlayer(String username) {
		playerOption(username, 4);
	}
	
	public static void continueDialogue() {
		clickDialogue(1184, 18);
	}
	
	public static boolean interfaceOpen(int id) {
		HashTableIterator<SubInterface> class460 = new HashTableIterator<>(client.OPEN_INTERFACES);
		for (SubInterface inter = (SubInterface) class460.first(); null != inter; inter = class460.next()) {
			if (inter.subInterfaceId == id) {
				return true;
			}
		}
		return false;
	}
	
	public static void setRunning(boolean on) {
		if (getVar(173) == 0 && on)
			Bot.clickButton(750, 4);
	}

	public static void alchItem(String replaceAll) {
		//TODO
	}
	
	public static String append(byte[] is) {
		char[] close = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		int i = is.length;
		char[] cs = new char[i * 2];
		int i_4_ = 0;
		for (int i_5_ = 0; i_5_ < i; i_5_++) {
			cs[i_4_++] = close[is[i_5_] >> 4 & 0xf];
			cs[i_4_++] = close[is[i_5_] & 0xf];
		}
		return new String(cs);
	}
	
	public static void clickDialogueOption(int option) {
		clickDialogue(1188, option == 1 ? 11 : (12+option-1));
	}
	
	public static void clickDialogueOption(String option) {
		int op = -1;
		for (int i = 0;i < optionsDialogue.length;i++) {
			if (optionsDialogue[i] != null) {
				if (optionsDialogue[i].toLowerCase().contains(option.toLowerCase()))
					op = i;
			}
		}
		if (op != -1) {
			clickDialogue(1188, op == 0 ? 11 : (12+op));
		}
	}

	public static int getPlayerPid(String username) {
		PlayerEntity player;
		for (int i = 0; i < Class197.NUM_PLAYER_INDICES; i++) {
			player = client.players[Class197.PLAYER_INDICES[i]];
			if (player.username == null)
				continue;

			if (player.username.equalsIgnoreCase(username)) {
				return Class197.PLAYER_INDICES[i];
			}
		}
		return -1;
	}

	public static PlayerEntity getPlayer(String username) {
		PlayerEntity player;
		for (int i = 0; i < Class197.NUM_PLAYER_INDICES; i++) {
			player = client.players[Class197.PLAYER_INDICES[i]];
			if (player.username == null)
				continue;

			if (player.username.equalsIgnoreCase(username)) {
				return player;
			}
		}
		return null;
	}
	
	public static Item getClosestGroundItem() {
		Map<Integer, Item> distanceMap = new TreeMap<Integer, Item>();
		ArrayList<Item> objects = getGroundItems();
		for (Item object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object.getPosition());
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (Item) distanceMap.get(sortedKeys.get(0));
	}

	public static WorldObject getClosestObjectWithOption(String option) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		ArrayList<WorldObject> objects = getNearbyObjectsWithOption(option);
		for (WorldObject object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}

	public static WorldObject getClosestObjectNoClip(String id) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		ArrayList<WorldObject> objects = getObjectsNearby(id);
		for (WorldObject object : objects) {
			if (object != null) {
				int distance = Utils.distance(getMyPlayerPosition(), object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}
	
	public static WorldObject getClosestObjectExact(String id) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		ArrayList<WorldObject> objects = getObjectsNearbyExact(id);
		for (WorldObject object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}

	public static WorldObject getClosestObject(String id) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		ArrayList<WorldObject> objects = getObjectsNearby(id);
		for (WorldObject object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}

	public static WorldObject getClosestObject(int... ids) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		for (int i = 0; i < ids.length; i++) {
			ArrayList<WorldObject> objects = getObjectsNearby(ids[i]);
			for (WorldObject object : objects) {
				if (object != null) {
					int distance = getDistanceTo(object);
					if (distance != -1)
						distanceMap.put(distance, object);
				}
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}

	public static WorldObject getClosestObject(int id) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		ArrayList<WorldObject> objects = getObjectsNearby(id);
		for (WorldObject object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}

	public static WorldObject getClosestObject(String id, String option) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		ArrayList<WorldObject> objects = getObjectsNearby(id, option);
		for (WorldObject object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}

	public static WorldObject getClosestObject(int id, String option) {
		Map<Integer, WorldObject> distanceMap = new TreeMap<Integer, WorldObject>();
		ArrayList<WorldObject> objects = getObjectsNearby(id, option);
		for (WorldObject object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (WorldObject) distanceMap.get(sortedKeys.get(0));
	}

	public static boolean contains(String[] array, String option) {
		for (String s : array) {
			if (s != null && s.equalsIgnoreCase(option))
				return true;
		}
		return false;
	}

	public static WorldObject getObjectAt(Tile tile) {
		ArrayList<WorldObject> allObjects = getNearbyObjects();
		for (WorldObject object : allObjects) {
			if (object.getX() == tile.getX() && object.getY() == tile.getY())
				return object;
		}
		return null;
	}

	public static ArrayList<WorldObject> getObjectsNearby(int id, String option) {
		ArrayList<WorldObject> allObjects = getNearbyObjects();
		ArrayList<WorldObject> objects = new ArrayList<WorldObject>();
		for (WorldObject object : allObjects) {
			if (object.getId() == id && contains(object.getDefinitions().options, option))
				objects.add(object);
		}
		return objects;
	}

	public static ArrayList<WorldObject> getObjectsNearby(String id, String option) {
		ArrayList<WorldObject> allObjects = getNearbyObjects();
		ArrayList<WorldObject> objects = new ArrayList<WorldObject>();
		for (WorldObject object : allObjects) {
			if (object.getDefinitions().name.toLowerCase().contains(id.toLowerCase()) && contains(object.getDefinitions().options, option))
				objects.add(object);
		}
		return objects;
	}

	public static ArrayList<WorldObject> getObjectsNearby(int id) {
		ArrayList<WorldObject> allObjects = getNearbyObjects();
		ArrayList<WorldObject> objects = new ArrayList<WorldObject>();
		for (WorldObject object : allObjects) {
			if (object.getId() == id)
				objects.add(object);
		}
		return objects;
	}

	public static ArrayList<WorldObject> getNearbyObjectsWithOption(String option) {
		ArrayList<WorldObject> allObjects = getNearbyObjects();
		ArrayList<WorldObject> objects = new ArrayList<WorldObject>();
		for (WorldObject object : allObjects) {
			if (contains(object.getDefinitions().options, option))
				objects.add(object);
		}
		return objects;
	}

	public static ArrayList<WorldObject> getObjectsNearby(String id) {
		ArrayList<WorldObject> allObjects = getNearbyObjects();
		ArrayList<WorldObject> objects = new ArrayList<WorldObject>();
		for (WorldObject object : allObjects) {
			if (object.getDefinitions().name.toLowerCase().contains(id.toLowerCase()))
				objects.add(object);
		}
		return objects;
	}
	
	public static ArrayList<WorldObject> getObjectsNearbyExact(String id) {
		ArrayList<WorldObject> allObjects = getNearbyObjects();
		ArrayList<WorldObject> objects = new ArrayList<WorldObject>();
		for (WorldObject object : allObjects) {
			if (object.getDefinitions().name.toLowerCase().equals(id.toLowerCase()))
				objects.add(object);
		}
		return objects;
	}
	
	public static boolean openClosestDepositBox() {
		WorldObject bank = getClosestObjectWithOption("deposit");
		if (bank != null) {
			clickObject(bank, "deposit");
			return true;
		}
		bank = getClosestObjectWithOption("bank");
		if (bank != null) {
			clickObject(bank, "bank");
			return true;
		}
		WorldObject chest = getClosestObject("bank chest");
		if (chest != null) {
			clickObject(chest, "use");
			return true;
		}
		NPCEntity banker = getClosestNPC("banker");
		if (banker != null) {
			clickNPC(banker, "bank");
			return true;
		}
		return false;
	}

	public static boolean openClosestBank() {
		if (inventory.contains(5733, 1)) {
			clickItem("rotten potato", 2);
			// clickDialogue(1188, 16);
			// clickDialogue(1188, 16);
			// clickDialogue(1188, 11);
			return true;
		}
		WorldObject bank = getClosestObjectWithOption("bank");
		if (bank != null) {
			clickObject(bank, "bank");
			return true;
		}
		WorldObject chest = getClosestObject("bank chest");
		if (chest != null) {
			clickObject(chest, "use");
			return true;
		}
		NPCEntity banker = getClosestNPC("banker");
		if (banker != null) {
			clickNPC(banker, "bank");
			return true;
		}
		return false;
	}

	public static void clickClosestObject(int name, String action) {
		WorldObject object = getClosestObject(name, action);
		if (object == null)
			return;
		clickObject(object, object.containsOption(action));
	}

	public static void clickClosestObject(String name, String action) {
		WorldObject object = getClosestObject(name, action);
		if (object == null)
			return;
		clickObject(object, object.containsOption(action));
	}

	public static void clickClosestObject(int name, int action) {
		clickObject(getClosestObject(name), action);
	}

	public static void clickClosestObject(int[] name, int action) {
		clickObject(getClosestObject(name), action);
	}

	public static void clickClosestObject(String name, int action) {
		clickObject(getClosestObject(name), action);
	}

	public static void clickClosestObject(int name) {
		clickObject(getClosestObject(name));
	}

	public static void clickClosestObject(String name) {
		clickObject(getClosestObject(name));
	}

	public static void playerOption(String username, int option) {
		ClientProt class379 = null;
		if (1 == option)
			class379 = ClientProt.PLAYER_OP1;
		else if (4 == option)
			class379 = ClientProt.PLAYER_OP4;
		else if (option == 5)
			class379 = ClientProt.PLAYER_OP5;
		else if (6 == option)
			class379 = ClientProt.PLAYER_OP6;
		else if (option == 7)
			class379 = ClientProt.PLAYER_OP7;
		else if (option == 9)
			class379 = ClientProt.PLAYER_OP9;
		if (null != class379) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(class379, (client.GAME_CONNECTION_CONTEXT.outKeys));
			class282_sub23.buffer.writeShort(getPlayerPid(username));
			class282_sub23.buffer.write128Byte(0);
			client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
		}
	}

	public static void sendText(String string) {
		TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.RESUME_TEXTDIALOG, client.GAME_CONNECTION_CONTEXT.outKeys);
		class282_sub23.buffer.writeByte(string.length() + 1);
		class282_sub23.buffer.writeString(string);
		client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
	}

	public static void addResponsePlayer(String username) {
		if (!playerInList(username))
			responsePlayers.add(username);
	}

	public static void removeResponsePlayer(String username) {
		responsePlayers.remove(username);
	}

	public static void clearResponsePlayers() {
		responsePlayers.clear();
	}

	public static void sendConsoleMessage(String string) {
		Class209.printConsoleMessage(string);
	}

	public static void sendItemOnObject(String item, WorldObject object) {
		sendItemOnObject(inventory.getItem(inventory.getSlotByItem(item)), object.getId(), object.getX(), object.getY());
	}

	public static void sendItemOnObject(String item, int id) {
		WorldObject object = getClosestObject(id);
		if (object == null)
			return;
		sendItemOnObject(item, object);
	}

	public static void sendItemOnObject(String item, String objectName) {
		WorldObject object = getClosestObject(objectName);
		if (object == null)
			return;
		sendItemOnObject(item, object);
	}

	public static void sendItemOnObject(int itemId, int objectId, int x, int y) {
		TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_ON_OBJECT, client.GAME_CONNECTION_CONTEXT.outKeys);
		class282_sub23.buffer.writeShortLE128(x);
		class282_sub23.buffer.write128Byte(VertexNormal.method1465() ? 1 : 0);
		class282_sub23.buffer.writeIntV1(objectId);
		class282_sub23.buffer.writeInt(44498944);
		class282_sub23.buffer.writeShortLE(itemId);
		class282_sub23.buffer.writeShort128(inventory.getSlotByItem(itemId));
		class282_sub23.buffer.writeShortLE(y);
		client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
		//walkTo(new Tile(x, y));
	}

	public static void clickDialogue(int id) {
		TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_CONTINUE, client.GAME_CONNECTION_CONTEXT.outKeys);
		class282_sub23.buffer.writeIntV1(id);
		class282_sub23.buffer.writeShortLE128(-1);
		client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
	}

	public static void clickSkillDialogue(int option) {
		clickDialogue(905, option + 13);
	}

	public static void clickDialogue(int interfaceId, int componentId) {
		clickDialogue(interfaceId << 16 | componentId);
	}

	public static boolean playerInList(String username) {
		for (String string : responsePlayers) {
			if (string.equalsIgnoreCase(username)) {
				return true;
			}
		}
		return false;
	}

	public static PlayerEntity getMyPlayer() {
		return PlayerUpdate.MY_PLAYER;
	}

	public static void itemOnItem(String item1, String item2) {
		itemOnItem(inventory.getItem(inventory.getSlotByItem(item1)), inventory.getItem(inventory.getSlotByItem(item2)));
	}
	
	public static void itemOnItem(int itemId, String item) {
		itemOnItem(itemId, inventory.getSlotByItem(itemId), inventory.getItem(inventory.getSlotByItem(item)), inventory.getSlotByItem(item));
	}

	public static void itemOnItem(int itemId, int itemId2) {
		itemOnItem(itemId, inventory.getSlotByItem(itemId), itemId2, inventory.getSlotByItem(itemId2));
	}

	public static void itemOnItem(int fromItem, int fromSlot, int toItem, int toSlot) {
		interfaceOnInterface(44498944, 44498944, fromSlot, toSlot, fromItem, toItem);
	}
	
	public static void interfaceOnInterface(int fromIf, int toIf, int fromSlot, int toSlot, int fromItem, int toItem) {
		TCPPacket packet = TCPPacket.createPacket(ClientProt.IF_ON_IF, client.GAME_CONNECTION_CONTEXT.outKeys);
        packet.buffer.writeShortLE128(toSlot);
        packet.buffer.writeShortLE(fromSlot);
        packet.buffer.writeShortLE128(toItem);
        packet.buffer.writeIntLE(fromIf);
        packet.buffer.writeIntV2(toIf);
        packet.buffer.writeShortLE(fromItem);
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
	}

	public static void activateQuickprayers() {
		clickButton(749, 4);
	}

	public static void depositItem(int itemId) {
		if (Bot.interfaceOpen(11))
			clickButton(11, 17, inventory.getSlotByItem(itemId), itemId, 1);
		else
			clickButton(763, 0, inventory.getSlotByItem(itemId), itemId, 1);
	}

	public static void depositAllOfItem(int itemId) {
		if (Bot.interfaceOpen(11))
			clickButton(11, 17, inventory.getSlotByItem(itemId), itemId, 4);
		else
			clickButton(763, 0, inventory.getSlotByItem(itemId), itemId, 6);
	}
	
	public static void depositAllOfItem(String itemName) {
		if (Bot.interfaceOpen(11))
			clickButton(11, 17, inventory.getSlotByItem(itemName), inventory.getItemId(itemName), 4);
		else
			clickButton(763, 0, inventory.getSlotByItem(itemName), inventory.getItemId(itemName), 6);
	}

	public static void depositAllOfItemBySlot(int slot) {
		if (Bot.interfaceOpen(11))
			clickButton(11, 17, slot, inventory.getItem(slot), 4);
		else
			clickButton(763, 0, slot, inventory.getItem(slot), 6);
	}

	public static void depositAllExcept(int... itemIds) {
		for (int i = 0;i < 28;i++) {
			int item = inventory.getItem(i);
			if (item == -1 || Arrays.stream(itemIds).anyMatch(id -> id == item)) continue;
			if (Bot.interfaceOpen(11))
				clickButton(11, 17, i, item, 4);
			else
				clickButton(763, 0, i, item, 6);
		}
	}

	public static void withdrawOneItem(int itemId) {
		clickButton(762, 95, bank.getSlotByItem(itemId), itemId, 1);
	}

	public static void withdraw5Item(int itemId) {
		clickButton(762, 95, bank.getSlotByItem(itemId), itemId, 2);
	}

	public static void withdraw10Item(int itemId) {
		clickButton(762, 95, bank.getSlotByItem(itemId), itemId, 3);
	}

	public static void withdrawCustomItem(int itemId) {
		clickButton(762, 95, bank.getSlotByItem(itemId), itemId, 4);
	}

	public static void withdrawAllButOneItem(int itemId) {
		clickButton(762, 95, bank.getSlotByItem(itemId), itemId, 7);
	}

	public static void withdrawAllOfItem(int itemId) {
		clickButton(762, 95, bank.getSlotByItem(itemId), itemId, 6);
	}

	public static void withdrawAllOfItem(String name) {
		clickButton(762, 95, bank.getSlotByItem(name), bank.getItem(bank.getSlotByItem(name)), 6);
	}

	public static void withdrawOneItem(String name) {
		clickButton(762, 95, bank.getSlotByItem(name), bank.getItem(bank.getSlotByItem(name)), 1);
	}

	public static void withdraw5Item(String name) {
		clickButton(762, 95, bank.getSlotByItem(name), bank.getItem(bank.getSlotByItem(name)), 2);
	}

	public static void withdraw10Item(String name) {
		clickButton(762, 95, bank.getSlotByItem(name), bank.getItem(bank.getSlotByItem(name)), 3);
	}

	public static void withdrawCustomItem(String name) {
		clickButton(762, 95, bank.getSlotByItem(name), bank.getItem(bank.getSlotByItem(name)), 4);
	}

	public static void withdrawAllButOneItem(String name) {
		int slot = bank.getSlotByItemOfGreaterAmount(name, 1);
		clickButton(762, 95, slot, bank.getItem(slot), 7);
	}

	public static void equipItem(int itemId, int slot) {
		clickButton(679, 0, slot, itemId, 2);
	}

	public static void clickItem(int itemId, int slot) {
		clickButton(679, 0, slot, itemId, 1);
	}
	
	public static void clickItem(int itemId, int slot, int option) {
		clickButton(679, 0, slot, itemId, option);
	}

	public static void clickItem(int itemId) {
		clickButton(679, 0, inventory.getSlotByItem(itemId), itemId, 1);
	}

	public static void clickItem(String name) {
		clickButton(679, 0, inventory.getSlotByItem(name), inventory.getItem(inventory.getSlotByItem(name)), 1);
	}

	public static void clickItem(String name, int option) {
		clickButton(679, 0, inventory.getSlotByItem(name), inventory.getItem(inventory.getSlotByItem(name)), option);
	}

	public static void dropItem(int itemId, int slot) {
		clickButton(679, 0, slot, itemId, 8);
	}

	public static void bankAll() {
		if (inventory.contains(5733, 1))
			depositAllExcept(5733);
		else
			clickButton(762, 33);
	}
	
	public static void bankAllInDepositBox() {
		clickButton(11, 18);
	}
	
	public static void bankAllExcept(int id) {
		clickButton(762, 33);
		withdrawOneItem(id);
	}

	public static ArrayList<Item> getNearbyGroundItemsWithIds(int... ids) {
		ArrayList<Item> items = new ArrayList<Item>();
		if (!Bot.getGroundItems().isEmpty()) {
			for (Item item : Bot.getGroundItems()) {
				if (item != null && Utils.distance(Bot.getMyPlayerPosition(), item.getPosition()) < 7) {
					for (int i = 0; i < ids.length; i++) {
						if (item.getId() == ids[i]) {
							items.add(item);
						}
					}
				}
			}
		}
		return items;
	}
	
	public static void closeInterfaces() {
		LinkedNodeList.closeInterfaces();
	}
	
	public static void bankEquipment() {
		clickButton(49938469, -1, -1, 1);
	}

	public static void clickButton(int interfaceId, int componentId) {
		clickButton(interfaceId, componentId, -1, -1, 1);
	}

	public static void clickButton(int interfaceId, int componentId, int clickType) {
		clickButton(interfaceId, componentId, -1, -1, clickType);
	}
	
	public static void clickButton(int interfaceId, int componentId, int slotId, int clickType) {
		clickButton(interfaceId, componentId, slotId, -1, clickType);
	}

	public static void clickButton(int interfaceId, int componentId, int slotId, int slotId2, int clickType) {
		int interfaceHash = interfaceId << 16 | componentId;
        BufferedConnectionContext class184 = BufferedConnectionContext.getConnectionContext();
		if (1 == clickType) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP1, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (clickType == 2) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP2, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (3 == clickType) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP3, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (clickType == 4) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP4, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (clickType == 5) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP5, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (6 == clickType) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP6, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (clickType == 7) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP7, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (clickType == 8) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP8, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (clickType == 9) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP9, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
		if (clickType == 10) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.IF_OP10, class184.outKeys);
			Class350_Sub3.method12590(class282_sub23, interfaceHash, slotId, slotId2);
			class184.queuePacket(class282_sub23);
		}
	}
	
	public static void closeMainInterface() {
		//Class108.method1150(true, (byte) 1);
	}

	public static void takeGroundItem(Item item) {
		clickGroundItem(item, 3);
	}
	
	public static void takeGroundItem(int itemId, Tile tile) {
		clickGroundItem(new Item(itemId, tile), 3);
	}
	
	public static void clickGroundItem(Item item, int op) {
		ClientProt class379_8_ = null;
		if (1 == op)
			class379_8_ = ClientProt.GROUND_ITEM_OP1;
		else if (2 == op)
			class379_8_ = ClientProt.GROUND_ITEM_OP2;
		else if (op == 3)
			class379_8_ = ClientProt.GROUND_ITEM_OP3;
		else if (4 == op)
			class379_8_ = ClientProt.GROUND_ITEM_OP4;
		else if (op == 5)
			class379_8_ = ClientProt.GROUND_ITEM_OP5;
		else if (op == 6)
			class379_8_ = ClientProt.GROUND_ITEM_EXAMINE;
		if (null != class379_8_) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(class379_8_, (client.GAME_CONNECTION_CONTEXT.outKeys));
			class282_sub23.buffer.writeShortLE128(item.getId());
			class282_sub23.buffer.writeByteC(VertexNormal.method1465() ? 1 : 0);
			class282_sub23.buffer.writeShort(item.getPosition().getY());
			class282_sub23.buffer.writeShort128(item.getPosition().getX());
			client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
			client.method12073(item.getPosition().getX(), item.getPosition().getY());
		}
	}

	public static void command(String message) {
		chat(";;" + message, 0, 0);
	}

	public static void chat(String message, int color, int effect) {
        BufferedConnectionContext context = BufferedConnectionContext.getConnectionContext();
		TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.CHAT, context.outKeys);
		class282_sub23.buffer.writeByte(0);
		int i_3_ = (-1990677291 * class282_sub23.buffer.index);
		class282_sub23.buffer.writeByte(color);
		class282_sub23.buffer.writeByte(effect);
		ByteBuf.writeHuffmanString(class282_sub23.buffer, message);
		class282_sub23.buffer.writeIndex(class282_sub23.buffer.index - i_3_);
		context.queuePacket(class282_sub23);
	}

	public static void walkTo(Tile tile) {
		TCPPacket class282_sub23 = CutsceneArea.method12078(tile.getX() - getBaseX(), tile.getY() - getBaseY(), 0);
		client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
		MapRegion.routeTo(tile.getX() - getBaseX(), tile.getY() - getBaseY(), true, SunDefinitions.method853(tile.getX() - getBaseX(), tile.getY() - getBaseY()));
	}

	public static int getDistanceTo(RouteStrategy strategy, boolean alt) {
		return Utils.findRouteDistance(getMyPlayer().localX, getMyPlayer().localY, VertexNormal.MY_PLAYER.getSize(), strategy, (IndexLoaders.MAP_REGION_DECODER.getClipMap(VertexNormal.MY_PLAYER.plane)), alt, client.walkStepsX, client.walkStepsY);
	}
	
	public static int getDistanceTo(RouteStrategy strategy) {
		return getDistanceTo(strategy, false);
	}

	public static int getDistanceTo(NPCEntity npc) {
		return getDistanceTo(Class344.method6115(npc.localX, npc.localY, npc.getSize(), npc.getSize()));
	}

	public static int getDistanceTo(Tile tile) {
		return getDistanceTo(SunDefinitions.method853(tile.getX() - getBaseX(), tile.getY() - getBaseY()));
	}

	public static boolean inCombat(PathingEntity entity) {
		if (entity.faceEntity != -1 && !facingMe(entity) && getHitbarValue(getFacing(entity), 0) <= 0)
			return true;
		return false;
	}
	
	public static boolean facingMe(PathingEntity entity) {
		return (entity.faceEntity - 32768) == getMyPlayer().index;
	}
	
	public static int getHitbarValue(PathingEntity entity, int hitbarIndex) {
		HitbarDefinitions hitbarDef = IndexLoaders.HITBAR_LOADER.getDefinitions(hitbarIndex);
		for (EntityHitbar hitbar = (EntityHitbar) entity.hitbars.start(); hitbar != null; hitbar = (EntityHitbar) entity.hitbars.next()) {
			if (hitbar.defs != hitbarDef)
				continue;
			EntityHitbarUpdate update = hitbar.method12600(client.FRAME_COUNT);
			if (update != null)
				return update.barValEnd;
		}
		return -1;
	}
	
	public static PathingEntity getFacing(PathingEntity entity) {
		if (entity.faceEntity == -1)
			return null;
        if (entity.faceEntity < 32768) {
            ObjectNode class282_sub47_3 = (ObjectNode) client.NPC_MAP.get(entity.faceEntity);
            if (class282_sub47_3 != null)
                return (NPCEntity) class282_sub47_3.anObject8068;
        } else if (entity.faceEntity >= 32768)
            return client.players[entity.faceEntity - 32768];
        return null;
	}

	public static NPCEntity getClosestOutOfCombatNPC(String id) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCs(id);
		for (NPCEntity object : objects) {
			if (object != null && !inCombat(object)) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(Utils.distance(Bot.getMyPlayerPosition(), new Tile(object.localX + getBaseX(), object.localY + getBaseY())), object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}
	
	public static NPCEntity getClosestOutOfCombatNPCContaining(String id) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCsContaining(id);
		for (NPCEntity object : objects) {
			if (object != null && !inCombat(object)) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}

	public static NPCEntity getClosestOutOfCombatNPC(int id) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCs(id);
		for (NPCEntity object : objects) {
			if (object != null && !inCombat(object)) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}
	
	public static NPCEntity getClosestNPCWithOption(String option) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCsByOption(option);
		for (NPCEntity object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}

	public static NPCEntity getClosestNPC(String id) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCs(id);
		for (NPCEntity object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}

	public static NPCEntity getClosestNPC(int id) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCs(id);
		for (NPCEntity object : objects) {
			if (object != null) {
				int distance = getDistanceTo(object);
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}

	public static int getDistanceTo(WorldObject object) {
        ObjectType objType = (ObjectType) Class386.identify(ObjectType.values(), object.getType());
		RouteStrategy strategy;
		if (objType == ObjectType.SCENERY_INTERACT || ObjectType.GROUND_INTERACT == objType || objType == ObjectType.GROUND_DECORATION) {
			ObjectDefinition class478 = IndexLoaders.MAP_REGION_DECODER.method4436().getObjectDefs(object.getId());
			int i_7_;
			int i_8_;
			if (object.getRotation() == 0 || object.getRotation() == 2) {
				i_7_ = class478.sizeX;
				i_8_ = class478.sizeY;
			} else {
				i_7_ = class478.sizeY;
				i_8_ = -class478.sizeX;
			}
			strategy = JS5CacheFile.createWallObjectStrategy(object.getX() - getBaseX(), object.getY() - getBaseY(), i_7_, i_8_, ObjectType.WALL_STRAIGHT, 0);
		} else if (Class308.isWall(objType.id))
			strategy = JS5CacheFile.createWallObjectStrategy(object.getX() - getBaseX(), object.getY() - getBaseY(), 0, 0, objType, object.getRotation());
		else
			strategy = Class148.createNormalObjectStrategy(object.getX() - getBaseX(), object.getY() - getBaseY(), objType, object.getRotation());
		return getDistanceTo(strategy);
	}

	public static void clickObject(WorldObject object, String action) {
		if (object == null)
			return;
		clickObject(object, object.containsOption(action));
	}

	public static void clickObject(WorldObject object, int option) {
		if (object == null)
			return;
		clickObject(object.getId(), object, option);
	}

	public static void clickObject(WorldObject object) {
		if (object == null)
			return;
		clickObject(object.getId(), object, 2);
	}
	
	public static void clickObject(int objectId, Tile tile, int option) {
		clickObject(objectId, tile, option, true);
	}

	public static void clickObject(int objectId, Tile tile, int option, boolean walk) {
		ClientProt class379_9_ = null;
		if (option == 1)
			class379_9_ = ClientProt.OBJECT_OP1;
		else if (option == 2)
			class379_9_ = ClientProt.OBJECT_OP2;
		else if (option == 3)
			class379_9_ = ClientProt.OBJECT_OP3;
		else if (option == 4)
			class379_9_ = ClientProt.OBJECT_OP4;
		else if (option == 5)
			class379_9_ = ClientProt.OBJECT_OP5;
		else if (option == 6)
			class379_9_ = ClientProt.OBJECT_EXAMINE;
		if (class379_9_ != null) {
			TCPPacket class282_sub23 = TCPPacket.createPacket(class379_9_, (client.GAME_CONNECTION_CONTEXT.outKeys));
			class282_sub23.buffer.writeShort(tile.getY());
			class282_sub23.buffer.writeShort(tile.getX());
			class282_sub23.buffer.writeInt(objectId);
			class282_sub23.buffer.writeByte128(VertexNormal.method1465() ? 1 : 0);
			client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
		}
		//walkTo(tile);
	}

	public static int getBaseX() {
		return IndexLoaders.MAP_REGION_DECODER.getBase().x;
	}

	public static int getBaseY() {
		return IndexLoaders.MAP_REGION_DECODER.getBase().y;
	}

	public static int getMyPlayerX() {
		return getMyPlayer().localX + getBaseX();
	}

	public static int getMyPlayerY() {
		return getMyPlayer().localY + getBaseY();
	}

	public static int getNpcIndexByName(String name) {
		for (int i = 0; i < client.anInt7216; i++) {
			int i_196_ = client.anIntArray7421[i];
			ObjectNode class282_sub47 = ((ObjectNode) client.NPC_MAP.get(i_196_));
			NPCEntity npc = (NPCEntity) class282_sub47.anObject8068;
			if (npc == null)
				continue;
			if (npc.definitions.name.equalsIgnoreCase(name)) {
				return npc.index;
			}
		}
		return -1;
	}

	public static int getPid(PathingEntity entity) {
		return entity.index;
	}

	public static NPCEntity getNPCByIndexExcluding(int index, int exclude) {
		for (int i = 0; i < client.anInt7216 * -423388407; i++) {
			int i_196_ = client.anIntArray7421[i];
			ObjectNode class282_sub47 = ((ObjectNode) client.NPC_MAP.get(i_196_));
			NPCEntity npc = (NPCEntity) class282_sub47.anObject8068;
			if (npc == null || getPid(npc) == exclude)
				continue;
			if (npc.index == index)
				return npc;
		}
		return null;
	}

	public static NPCEntity getNPCByIndex(int index) {
		for (int i = 0; i < client.anInt7216 * -423388407; i++) {
			int i_196_ = client.anIntArray7421[i];
			ObjectNode class282_sub47 = ((ObjectNode) client.NPC_MAP.get(i_196_));
			NPCEntity npc = (NPCEntity) class282_sub47.anObject8068;
			if (npc == null)
				continue;
			if ((npc.index) == index)
				return npc;
		}
		return null;
	}

	public static int getNpcIndexById(int id) {
		for (int i = 0; i < client.anInt7216 * -423388407; i++) {
			int i_196_ = client.anIntArray7421[i];
			ObjectNode class282_sub47 = ((ObjectNode) client.NPC_MAP.get(i_196_));
			NPCEntity npc = (NPCEntity) class282_sub47.anObject8068;
			if (npc == null)
				continue;
			if ((npc.definitions.id) == id)
				return npc.index;
		}
		return -1;
	}
	
	public static void buy500ShopItem(int slot) {
		clickButton(1265, 20, slot, 6);
	}

	public static void clickNPC(NPCEntity npc, int option) {
		if (npc == null)
			return;
		npcOption(npc.index, option);
	}

	public static void clickNPC(NPCEntity npc, String option) {
		if (npc == null)
			return;
		npcOption(npc.index, Utils.stringContainsIgnoreCase(npc.definitions.options, option));
	}

	public static void clickNpc(String name, int option) {
		NPCEntity npc = getClosestNPC(name);
		if (npc != null)
			clickNPC(npc, option);
	}

	public static void clickNPCWithNearestOption(String name, String optionRequired, String action) {
		ArrayList<NPCEntity> npcs = getNPCs();
		NPCEntity nearestNPC = null;
		for(NPCEntity npc : npcs) {
			if(nearestNPC != null)
				break;
			if(npc.definitions.name.equalsIgnoreCase(name))
				for(String option : npc.definitions.options) {
					if(option.contains(optionRequired)) {
						nearestNPC = npc;
						break;
					}
				}
		}
		Bot.clickNPC(nearestNPC, action);
	}

	public static void clickObjectWithNearestOption(String name, String optionRequired, String action) {
		ArrayList<WorldObject> objects = getNearbyObjects();
		WorldObject nearestObject = null;
		for(WorldObject object : objects) {
			if(object == null)
				continue;
			if(nearestObject != null)
				break;
			if(object.getDefinitions().name.equalsIgnoreCase(name)) {
				if(contains(object.getDefinitions().options, optionRequired)) {
					nearestObject = object;
				}
			}
		}

		Bot.sendConsoleMessage("Clicking " + nearestObject.getDefinitions().name);
		Bot.clickObject(nearestObject, action);
	}

	public static void clickNpc(int npcId, int option) {
		NPCEntity npc = getClosestNPC(npcId);
		if (npc != null)
			clickNPC(npc, option);
	}

	public static void npcOption(int index, int option) {
		ClientProt class379_7_ = null;
		if (option == 1)
			class379_7_ = ClientProt.NPC_OP1;
		else if (2 == option)
			class379_7_ = ClientProt.NPC_OP2;
		else if (option == 3)
			class379_7_ = ClientProt.NPC_OP3;
		else if (4 == option)
			class379_7_ = ClientProt.NPC_OP4;
		else if (option == 5)
			class379_7_ = ClientProt.NPC_OP5;
		else if (6 == option)
			class379_7_ = ClientProt.NPC_EXAMINE;
		if (class379_7_ != null) {
			ObjectNode class282_sub47_19 = (ObjectNode) client.NPC_MAP.get(index);
			if (null != class282_sub47_19) {
				NPCEntity class521_sub1_sub1_sub2_sub2 = ((NPCEntity) class282_sub47_19.anObject8068);
				TCPPacket class282_sub23 = TCPPacket.createPacket(class379_7_, (client.GAME_CONNECTION_CONTEXT.outKeys));
				class282_sub23.buffer.writeShort(index);
				class282_sub23.buffer.writeByte(VertexNormal.method1465() ? 1 : 0);
				client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
				MapRegion.routeTo(class521_sub1_sub1_sub2_sub2.regionBaseX[0], class521_sub1_sub1_sub2_sub2.regionBaseY[0], true, Class344.method6115(class521_sub1_sub1_sub2_sub2.regionBaseX[0], class521_sub1_sub1_sub2_sub2.regionBaseY[0], class521_sub1_sub1_sub2_sub2.getSize(), class521_sub1_sub1_sub2_sub2.getSize()));
			}
		}
	}
	
	public static void dropAllExceptExact(String... itemNames) {
		for (int i = 0; i < 28; i++) {
			if (containsAnyStringExact(TypeLists.OBJ.list(inventory.getItem(i)).name, itemNames))
				continue;
			Bot.dropItem(inventory.getItem(i), i);
			Bot.clickDialogue(77529097);
		}
	}
	
	public static void dropAllExcept(String... itemNames) {
		for (int i = 0; i < 28; i++) {
			if (containsAnyString(TypeLists.OBJ.list(inventory.getItem(i)).name, itemNames))
				continue;
			Bot.dropItem(inventory.getItem(i), i);
			Bot.clickDialogue(77529097);
		}
	}
	
	public static boolean containsAnyStringExact(String s, String[] comp) { 
		for (String contain : comp) {
			if (s.toLowerCase().equals(contain.toLowerCase()))
				return true;
		}
		return false;
	}
	
	public static boolean containsAnyString(String s, String[] comp) { 
		for (String contain : comp) {
			if (s.toLowerCase().contains(contain.toLowerCase()))
				return true;
		}
		return false;
	}
	
	public static void dropWholeInventory() {
		for (int i = 0; i < 28; i++) {
			Bot.dropItem(inventory.getItem(i), i);
			Bot.clickDialogue(77529097);
		}
	}

	public static boolean isLoggedOut() {
		return GameState.loggedOut(client.GAME_STATE);
	}

	public static ArrayList<Item> getGroundItems() {
		ArrayList<Item> items = new ArrayList<Item>();
		for (int x = 0; x < 104; x++) {
			for (int y = 0; y < 104; y++) {
				long positionHash = (long) (Bot.getMyPlayer().plane << 28 | y + Bot.getBaseY() << 14 | x + Bot.getBaseX());
				Node_Sub29 deque = (Node_Sub29) client.aClass465_7414.get(positionHash);
				if (deque != null) {
					for (GroundItemNode groundItems = (GroundItemNode) deque.aClass482_7708.head(); null != groundItems; groundItems = ((GroundItemNode) deque.aClass482_7708.next())) {
						items.add(new Item(groundItems.id, groundItems.amount, new Tile(x + Bot.getBaseX(), y + Bot.getBaseY(), Bot.getMyPlayer().plane)));
					}
				}
			}
		}
		return items;
	}

	public static ArrayList<WorldObject> getNearbyObjects() {
		ArrayList<WorldObject> objects = new ArrayList<WorldObject>();
		for (int x = 0; x < 104; x++) {
			for (int y = 0; y < 104; y++) {
				com.jagex.WorldObject object = ((com.jagex.WorldObject) (IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getInteractableObject(getMyPlayer().plane, x, y, client.anInterface25_7446)));
				if (object == null)
					object = ((com.jagex.WorldObject) IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getGroundDecoration(getMyPlayer().plane, x, y));
				if (object == null)
					object = ((com.jagex.WorldObject) IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getWall(getMyPlayer().plane, x, y));
				if (object == null)
					object = ((com.jagex.WorldObject) IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getWallDecoration(getMyPlayer().plane, x, y));
				if (object != null) {
					objects.add(new WorldObject(object.getId(), object.method92(), object.method89(), x + getBaseX(), y + getBaseY(), IndexLoaders.MAP_REGION_DECODER.method4436().getObjectDefs(object.getId()).options));
				}
			}
		}
		return objects;
	}
	
	public static void handleConsoleCommand(String[] cmd) {
		int x, y;
		if (cmd[0].startsWith("walk")) {
			x = Integer.valueOf(cmd[1]);
			y = Integer.valueOf(cmd[2]);
			walkTo(new Tile(x, y, 0));
		} else if (cmd[0].startsWith("clickbutton")) {
			int interfaceId = Integer.valueOf(cmd[1]);
			int componentId = Integer.valueOf(cmd[2]);
			int slotId = Integer.valueOf(cmd[3]);
			int clickType = Integer.valueOf(cmd[4]);
			Bot.clickButton(interfaceId, componentId, slotId, clickType);
		} else if (cmd[0].startsWith("itemtest")) {
			if (!Bot.getGroundItems().isEmpty()) {
				for (Item item : Bot.getGroundItems()) {
					System.out.println(item.toString());
				}
			}
		} else if (cmd[0].startsWith("test")) {
			// clickButton(42598415, -1, -1, 1);
			clickButton(374 << 16 | 5, -1, -1, 1);
		} else if (cmd[0].startsWith("bankall")) {
			bankAll();
		} else if (cmd[0].startsWith("withdraw")) {
			withdrawOneItem(1333);
		} else if (cmd[0].startsWith("trade")) {
			tradePlayer(cmd[1].replace("_", " "));
		} else if (cmd[0].startsWith("banticket")) {
			command("banticket " + cmd[1].replace("_", " "));
		} else if (cmd[0].equalsIgnoreCase("empty")) {
			Bot.command("empty");
		} else if (cmd[0].equalsIgnoreCase("drop")) {
			for (int i = 0; i < 28; i++) {
				Bot.dropItem(inventory.getItem(i), i);
				Bot.clickDialogue(77529097);
			}
		} else if (cmd[0].startsWith("food")) {
			Bot.withdraw10Item(15272);
		} else if (cmd[0].startsWith("pray")) {
			Bot.withdraw5Item(139);
		} else if (cmd[0].equalsIgnoreCase("buy")) {
			for (int i = 0; i < 20; i++) {
				clickButton(82903060, -1, Integer.valueOf(cmd[1]), 6);
			}
		} else if (cmd[0].startsWith("useon")) {
			for (int i = 0; i < 14; i++) {
				itemOnItem(inventory.getItem(i), i, inventory.getItem(i+14), i+14);
			}
		} else if (cmd[0].startsWith("string")) {
			sendText(cmd[1]);
		} else if (cmd[0].startsWith("togglepackets")) {
			disableIncomingPackets = !disableIncomingPackets;
			sendConsoleMessage("Disabled packets: " + disableIncomingPackets);
		} else if (cmd[0].startsWith("toggleinterfaces")) {
			disableInterfaces = !disableInterfaces;
			sendConsoleMessage("Disabled interfaces: " + disableInterfaces);
		} else if (cmd[0].startsWith("crash")) {
			chat("Bank sale.", -1, -1);
		} else if (cmd[0].startsWith("pos")) {
			sendConsoleMessage("x: " + getMyPlayerX() + " y: " + getMyPlayerY() + " (" + getMyPlayer().localX + ", " + getMyPlayer().localY + ")");
		} else if (cmd[0].startsWith("localpos")) {
			sendConsoleMessage("x: " + getMyPlayer().localX + " y: " + getMyPlayer().localY);
		} else if (cmd[0].startsWith("closebank")) {
			closeInterfaces();
		} else if (cmd[0].startsWith("add")) {
			addResponsePlayer(cmd[1].replace("_", " "));
		} else if (cmd[0].startsWith("remove")) {
			removeResponsePlayer(cmd[1].replace("_", " "));
		} else if (cmd[0].startsWith("withdrawcustom")) {
			withdrawCustomItem(995);
		} else if (cmd[0].equals("iswalking")) {
			sendConsoleMessage("" + isWalking());
			sendConsoleMessage("" + Arrays.toString(getMyPlayer().walkTypes));
			sendConsoleMessage("" + getMyPlayer().aAnimation_Sub3_10337.getDefs().id);
		} else if (cmd[0].equals("healthpray")) {
			System.out.println("Health: " + getHealth() + " Prayer: " + getPrayer());
		} else if (cmd[0].equals("b")) {
			command("bank");
			bankAll();
		} else if (cmd[0].startsWith("clear")) {
			clearResponsePlayers();
		} else if (cmd[0].equals("npcnext")) {
			System.out.println(getClosestNPC("fishing spot"));
			for (NPCEntity n : getNPCs())
				System.out.println(n.definitions.name);
		} else if (cmd[0].equals("objnext")) {
			System.out.println(getClosestObject("Fur stall"));
			System.out.println("Number found: " + getObjectsNearby("Fur stall").size());
			clickClosestObject("Fur stall");
		} else if (cmd[0].equals("nearbyobjects")) {
			sendConsoleMessage("Objects found: " + getObjectsNearby(cmd[1]).size() + ", Ids: ");
			for(WorldObject obj : getObjectsNearby(cmd[1]))
				sendConsoleMessage("ID: " + obj.getId() + ", Coords: X" + obj.getX() + " Y" + obj.getY() + " Z" + obj.getZ());
		} else if (cmd[0].equals("pos")) {
			sendConsoleMessage("My Player: X" + getMyPlayerX() + " Y" + getMyPlayerY() + " Z" + getMyPlayer().plane);
		}
		else if (cmd[0].equals("objonme")) {
			com.jagex.WorldObject object = ((com.jagex.WorldObject) (IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getInteractableObject(getMyPlayer().plane, getMyPlayer().localX, getMyPlayer().localY, client.anInterface25_7446)));
			System.out.println(object.getId());
		} else if (cmd[0].equals("objtest")) {
			for (x = 0; x < 104; x++) {
				for (y = 0; y < 104; y++) {
					com.jagex.WorldObject object = ((com.jagex.WorldObject) (IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getInteractableObject(getMyPlayer().plane, x, y, client.anInterface25_7446)));
					if (object == null)
						object = ((com.jagex.WorldObject) IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getGroundDecoration(getMyPlayer().plane, x, y));
					if (object == null)
						object = ((com.jagex.WorldObject) IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getWall(getMyPlayer().plane, x, y));
					if (object == null)
						object = ((com.jagex.WorldObject) IndexLoaders.MAP_REGION_DECODER.getSceneObjectManager().getWallDecoration(getMyPlayer().plane, x, y));
					if (object != null) {
						System.out.println("Object type: " + object.getClass().getName());
					}
				}
			}
		} else if (cmd[0].startsWith("yell")) {
			String yell = "";
			for (int i = 1; i < cmd.length; i++) {
				yell += " ";
				yell += cmd[i];
			}
			command("yell" + yell);
		} else if (cmd[0].startsWith("bid")) {
			command("ah bid");
		} else if (cmd[0].startsWith("1b")) {
			clickButton(85983267, -1, -1, 1);
		} else if (cmd[0].startsWith("teleto")) {
			String teleto = "";
			for (int i = 1; i < cmd.length; i++) {
				teleto += " ";
				teleto += cmd[i];
			}

			command("teleto" + teleto);
		} else if (cmd[0].startsWith("dicebag")) {
			String dicebag = "";
			for (int i = 1; i < cmd.length; i++) {
				dicebag += " ";
				dicebag += cmd[i];
			}

			command("dicebag" + dicebag);
		} else if (cmd[0].startsWith("ticket")) {
			String ticket = "";
			for (int i = 1; i < cmd.length; i++) {
				ticket += " ";
				ticket += cmd[i];
			}
			command("ticket" + ticket);
		} else if (cmd[0].startsWith("bank")) {
			String bank = "";
			for (int i = 1; i < cmd.length; i++) {
				bank += " ";
				bank += cmd[i];
			}
			command("bank" + bank);
		} else if (cmd[0].startsWith("log")) {
			sendLogout(true);
		} else if (cmd[0].startsWith("resettask")) {
			String resettask = "";
			for (int i = 1; i < cmd.length; i++) {
				resettask += " ";
				resettask += cmd[i];
			}
			command("resettask" + resettask);
		} else if (cmd[0].startsWith("chet")) {
			String chat = "";
			for (int i = 1; i < cmd.length; i++) {
				chat += " ";
				chat += cmd[i];
			}
			chat(chat, 2, 4);
		} else if (cmd[0].startsWith("curemote")) {
			sendConsoleMessage("Emote: " + getCurrentAnimation());
		} else {
			sendConsoleMessage("Unknown command.");
		}
	}

	public static Tile findGroundItem(String id) {
		return findGroundItem(id, 15);
	}

	public static Tile findGroundItem(String id, int distance) {
		if (!Bot.getGroundItems().isEmpty()) {
			for (Item item : Bot.getGroundItems()) {
				if (item != null && Utils.distance(Bot.getMyPlayerPosition(), item.getPosition()) < distance) {
					if (getDistanceTo(item.getPosition()) != -1 && item.getName().toLowerCase().contains(id.toLowerCase()))
						return new Tile(item.getPosition().getX(), item.getPosition().getY(), item.getId());
				}
			}
		}
		return null;
	}

	public static Tile findGroundItem(int id, int distance) {
		if (!Bot.getGroundItems().isEmpty()) {
			for (Item item : Bot.getGroundItems()) {
				if (item != null && Utils.distance(Bot.getMyPlayerPosition(), item.getPosition()) < distance) {
					if (getDistanceTo(item.getPosition()) != -1 && item.getId() == id)
						return new Tile(item.getPosition().getX(), item.getPosition().getY(), item.getId());
				}
			}
		}
		return null;
	}

	public static Tile findGroundItem(int id) {
		return findGroundItem(id, 10);
	}
	
	public static boolean isWalking() {
		return getMyPlayer().aAnimation_Sub3_10337.getDefs().id != 11975 || getMyPlayer().aAnimation_Sub3_10337.getDefs().id == 11976;
	}

	public static int getXp(int skillId) {
		return client.SKILL_XP[skillId];
	}

	public static int getLevel(int skillId) {
		return client.SKILL_LEVEL_VISIBLE[skillId];
	}

	public static void sendLogout(boolean lobby) {
		CursorIndexLoader.killConnections(lobby ? Class9.aBool71 : false);
	}

	public static int getXPForLevel(int level) {
		int points = 0;
		int output = 0;
		for (int lvl = 1; lvl <= level; lvl++) {
			points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			if (lvl >= level) {
				return output;
			}
			output = (int) Math.floor(points / 4);
		}
		return 0;
	}

	public static ArrayList<NPCEntity> getNPCs() {
		ArrayList<NPCEntity> npcs = new ArrayList<NPCEntity>();
		for (int i = 0; i < client.anInt7210; i++) {
			ObjectNode npcNode = client.NPC_ARRAY[i];
			if (npcNode != null)
				npcs.add((NPCEntity) npcNode.anObject8068);
		}
		return npcs;
	}

	public static ArrayList<NPCEntity> getFilteredNPCs(int id) {
		ArrayList<NPCEntity> allNpcs = getNPCs();
		if (allNpcs != null) {
			ArrayList<NPCEntity> filtered = new ArrayList<NPCEntity>();
			for (NPCEntity n : allNpcs) {
				if (n != null && n.definitions.id == id)
					filtered.add(n);
			}
			return filtered;
		}
		return new ArrayList<NPCEntity>();
	}

	public static ArrayList<NPCEntity> getFilteredNPCs(String id) {
		ArrayList<NPCEntity> allNpcs = getNPCs();
		if (allNpcs != null) {
			ArrayList<NPCEntity> filtered = new ArrayList<NPCEntity>();
			for (NPCEntity n : allNpcs) {
				if (n != null && n.definitions.name.equalsIgnoreCase(id))
					filtered.add(n);
			}
			return filtered;
		}
		return new ArrayList<NPCEntity>();
	}
	
	public static ArrayList<NPCEntity> getFilteredNPCsByOption(String id) {
		ArrayList<NPCEntity> allNpcs = getNPCs();
		if (allNpcs != null) {
			ArrayList<NPCEntity> filtered = new ArrayList<NPCEntity>();
			for (NPCEntity n : allNpcs) {
				if (n == null)
					continue;
				if (contains(n.definitions.options, id))
					filtered.add(n);
			}
			return filtered;
		}
		return new ArrayList<NPCEntity>();
	}
	
	public static ArrayList<NPCEntity> getFilteredNPCsContaining(String id) {
		ArrayList<NPCEntity> allNpcs = getNPCs();
		if (allNpcs != null) {
			ArrayList<NPCEntity> filtered = new ArrayList<NPCEntity>();
			for (NPCEntity n : allNpcs) {
				if (n != null && n.definitions.name.toLowerCase().contains(id.toLowerCase()))
					filtered.add(n);
			}
			return filtered;
		}
		return new ArrayList<NPCEntity>();
	}

	public static NPCEntity getClosestNPCNoClip(String id) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCs(id);
		for (NPCEntity object : objects) {
			if (object != null) {
				int distance = Utils.distance(getMyPlayerPosition(), new Tile(object.localX + getBaseX(), object.localY + getBaseY()));
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}

	public static NPCEntity getClosestNPCNoClip(int id) {
		Map<Integer, NPCEntity> distanceMap = new TreeMap<Integer, NPCEntity>();
		ArrayList<NPCEntity> objects = getFilteredNPCs(id);
		for (NPCEntity object : objects) {
			if (object != null) {
				int distance = Utils.distance(getMyPlayerPosition(), new Tile(object.localX + getBaseX(), object.localY + getBaseY()));
				if (distance != -1)
					distanceMap.put(distance, object);
			}
		}
		if (distanceMap.isEmpty())
			return null;
		ArrayList<Integer> sortedKeys = new ArrayList<Integer>(distanceMap.keySet());
		Collections.sort(sortedKeys);
		return (NPCEntity) distanceMap.get(sortedKeys.get(0));
	}

	public static String getTimeToLevel(int skillId, int startXP, long startTime, int level) {
		String formatReturn;
		int xpPh = Bot.getXpPerHour(skillId, startXP, startTime);
		float xpPm = xpPh / 60;
		float xpPs = xpPm / 60;

		//Get raw times to level
		int secondsToLevel = (int) (getXPToLevel(skillId, level) / xpPs);
		int minutesToLevel = secondsToLevel / 60;
		int hoursToLevel = minutesToLevel / 60;
		
		//Calculate real times, IE 120 seconds is 2 minutes 0 seconds
		int formatMinutesToLevel = minutesToLevel - (hoursToLevel * 60);
		int formatSecondsToLevel = secondsToLevel - (minutesToLevel * 60);

		//Construct return string with formatted times
	    formatReturn = (hoursToLevel < 10) ? ("0" + hoursToLevel + ":") : (hoursToLevel + ":");
	    formatReturn += (formatMinutesToLevel < 10) ? ("0" + formatMinutesToLevel + ":") : (formatMinutesToLevel + ":");
	    formatReturn += (formatSecondsToLevel < 10) ? ("0" + formatSecondsToLevel) : (formatSecondsToLevel + "");
		
		return formatReturn;
	}

	public static int getXPToLevel(int skillId, int level) {
		if (level == 126)
		{
			return 200000000 - getXp(skillId);
		}
		return getXPForLevel(level) - getXp(skillId);
	}

	public static int getXPToNextLevel(int skillId) {
		return getXPForLevel(getLevel(skillId) + 1) - getXp(skillId);
	}

	public static String getScriptTime(long currTime, long startTime) {
		long ms = currTime - startTime;
		long totalSecs = ms / 1000;
		long hours = (totalSecs / 3600);
		long mins = (totalSecs / 60) % 60;
		long secs = totalSecs % 60;
		String hoursString = (hours == 0) ? "00" : ((hours < 10) ? "0" + hours : "" + hours);
		String minsString = (mins == 0) ? "00" : ((mins < 10) ? "0" + mins : "" + mins);
		String secsString = (secs == 0) ? "00" : ((secs < 10) ? "0" + secs : "" + secs);

		return hoursString + ":" + minsString + ":" + secsString;
	}

	public static int getLootPerHour(int itemAmount, int startingAmount, long startTime) {
		return (int) ((itemAmount - startingAmount) / hoursElapsed(startTime));
	}

	public static String getFormattedLootPerHour(int itemAmount, int startingAmount, long startTime) {
		return format(getLootPerHour(itemAmount, startingAmount, startTime));
	}

	public static boolean myPlayerInCombat() {
		return inCombat(Bot.getMyPlayer());
	}
	
	public static double getHealth() {
		return Class158_Sub1.PLAYER_VAR_PROVIDER.getVarBit(7198);
	}

	public static double getHealthPercent() {
		return (((double) getHealth()) / ((double) getLevel(HITPOINTS) * 10)) * 100.0;
	}

	public static boolean usingSpec() {
		return Class158_Sub1.PLAYER_VAR_PROVIDER.getVar(301) == 1;
	}

	public static double specPercent() {
		return Class158_Sub1.PLAYER_VAR_PROVIDER.getVar(300) / 10.0;
	}
	
	public static double getPrayer() {
		return Class158_Sub1.PLAYER_VAR_PROVIDER.getVar(2382);
	}
	
	public static int getVar(int varId) {
		return Class158_Sub1.PLAYER_VAR_PROVIDER.getVar(varId);
	}
	
	public static int getVarBit(int varBit) {
		return Class158_Sub1.PLAYER_VAR_PROVIDER.getVarBit(varBit);
	}

	public static boolean hasntAnimatedFor(int time) {
		return getTimeSinceLastAnimation() > time;
	}

	public static void dropAll(int... ids) {
		for (int i = 0;i < 28;i++) {
			for (int id : ids) {
				if (inventory.getItem(i) == id)
					dropItem(id, i);
			}
		}
	}

	public static void sendClick() {
		TCPPacket class282_sub23 = TCPPacket.createPacket(ClientProt.MOUSE_CLICK, client.GAME_CONNECTION_CONTEXT.outKeys);
		class282_sub23.buffer.writeIntLE(Utils.random(0, 400) | Utils.random(0, 400) << 16);
		class282_sub23.buffer.writeShort(5 | 10 << 0);
		client.GAME_CONNECTION_CONTEXT.queuePacket(class282_sub23);
	}

	public static void loginLobby(String un, String pw) {
		MaterialProp35.lobbyLogin(un, pw);
	}
	
	public static void loginWorld() {
		ParticleArchive1Def.worldLogin(Class9.aString99, Class9.aString102);
	}
}
