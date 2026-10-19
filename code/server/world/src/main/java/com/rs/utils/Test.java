package com.rs.utils;

import com.google.gson.GsonBuilder;
import com.rs.Settings;
import world.gregs.voidps.cache.Cache;
import org.darkan.core.cache.ItemDefinitions;
import world.gregs.voidps.cache.definition.data.ItemDefinition;
import org.darkan.core.cache.VarBitDefinitions;
import org.darkan.core.cache.IComponentDefinitions;
import com.rs.game.model.entity.player.Controller;
import com.rs.game.model.item.ItemsContainer;
import org.darkan.core.JsonFileManager;
import org.darkan.core.model.Item;
// TODO: DateAdapter
// TODO: net.packets.Packet
// TODO: net.packets.PacketEncoder
// TODO: PacketAdapter
// TODO: PacketEncoderAdapter
// TODO: RecordTypeAdapterFactory
import org.darkan.core.Utils;
import com.rs.utils.drop.Drop;
import com.rs.utils.drop.DropSet;
import com.rs.utils.drop.DropTable;
import com.rs.utils.json.ControllerAdapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Test {

	public static int HOURS_TO_SAMPLE = 300;
	
	public static void main(String[] args) throws IOException {
		JsonFileManager.setGSON(new GsonBuilder()
				.registerTypeAdapter(Controller.class, new ControllerAdapter())
				// TODO: re-add legacy adapters when migrated
				.disableHtmlEscaping()
				.setPrettyPrinting()
				.create());

		Settings.getConfig();
		Cache.init(Settings.getConfig().getCachePath());

		File file = new File("itemListMeshModifiers.txt");
		if (file.exists())
			file.delete();
		else
			file.createNewFile();
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.flush();
		for (int itemId = 0;itemId < Utils.getItemDefinitionsSize();itemId++) {
			ItemDefinition def = ItemDefinitions.getDefs(itemId);
			boolean colors = def.getOriginalColours() != null && def.getOriginalColours().length > 0 || def.getModifiedColours() != null && def.getModifiedColours().length > 0;
			boolean textures = def.getOriginalTextureColours() != null && def.getOriginalTextureColours().length > 0 || def.getModifiedTextureColours() != null && def.getModifiedTextureColours().length > 0;
			if (def.getEquipSlot() != -1 && (colors || textures)) {
				writer.write(itemId + " (" + def.getName() + "):");
				writer.newLine();
				if (colors) {
					writer.write("Original colors: " + Arrays.toString(def.getOriginalColours()));
					writer.write("Modified colors: " + Arrays.toString(def.getModifiedColours()));
					writer.newLine();
				}
				if (textures) {
					writer.write("Original textures: " + Arrays.toString(def.getOriginalTextureColours()));
					writer.write("Modified textures: " + Arrays.toString(def.getModifiedTextureColours()));
					writer.newLine();
				}
			}
		}
	}

//	public static void main(String[] args) throws IOException {
//		int numKills = 1000000;
//
//		JsonFileManager.setGSON(new GsonBuilder()
//				.registerTypeAdapter(Controller.class, new ControllerAdapter())
//				.registerTypeAdapter(Date.class, new DateAdapter())
//				.registerTypeAdapter(PacketEncoder.class, new PacketEncoderAdapter())
//				.registerTypeAdapter(Packet.class, new PacketAdapter())
//				.registerTypeAdapterFactory(new RecordTypeAdapterFactory())
//				.disableHtmlEscaping()
//				.setPrettyPrinting()
//				.create());
//
//		Settings.getConfig();
//		Cache.init(Settings.getConfig().getCachePath());
//		DropSets.init();
//
//		ItemsContainer<Item> items = new ItemsContainer<>(500, true);
//
//		DropList table = DropSets.getDropSet(6260).createDropList();
//		for (int i = 0;i < numKills;i++)
//			items.addAll(table.genDrop());
//
//		List<Item> sorted = new ArrayList<>();
//
//		for (Item item : items.array()) {
//			if (item == null)
//				continue;
//			sorted.add(item);
//		}
//
//		sorted.sort((o1, o2) -> o1.getId() - o2.getId());
//
//		for (Item item : sorted)
//			Logger.debug(item.getName() + " - " + item.getAmount() + " Rate: 1/" + (numKills / item.getAmount()));
//	}

	//		for (int i = 0;i < 2000;i++) {
	//			int musicIndex = (int) EnumDefinitions.getEnum(1351).getKeyForValue(i);
	//			if (musicIndex != -1) {
	//				String musicName = EnumDefinitions.getEnum(1345).getStringValue(musicIndex);
	//				Logger.debug(i + " - " + musicName + " - musicIndex: " + musicIndex);
	//			}
	//		}

	//		for (NPCDirection dir : NPCDirection.values()) {
	//			Logger.debug(dir.name() + " - " + dir.getFaceDirection());
	//			int d = ((int) (Math.atan2(dir.getDx(), dir.getDy()) * 2607.5945876176133));
	//			double ang = d / 2607.5945876176133;
	//			Logger.debug(Math.round(Math.sin(ang)) + ", " + Math.round(Math.cos(ang)));
	//		}
	//
	//
	//		RockType rock = RockType.GEM;
	//		Pickaxe pick = Pickaxe.RUNE;

	//		for (int pickIdx = Pickaxe.BRONZE.ordinal();pickIdx <= Pickaxe.DRAGON.ordinal();pickIdx += 2) {
	//			for (int i = 1;i <= 99;i += 1) {
	//				if (i % 5 == 0 || i == rock.getLevel() || i == 99) {
	//					String average = getMiningAverage(rock, i, Pickaxe.values()[pickIdx]);
	//					if (!average.isEmpty())
	//						Logger.debug(average);
	//				}
	//			}
	//			Logger.debug();
	//		}

	//		for (int i = 1; i <= 99; i += 1) {
	//			if (i % 5 == 0 || i == rock.getLevel() || i == 99) {
	//				String average = getMiningAverage(rock, i, pick);
	//				if (!average.isEmpty())
	//					Logger.debug(average);
	//			}
	//		}
	//	}
	//
	//	private static String getFishingAverage(FishingSpot spot, int level) {
	//		if (level < spot.getLevel())
	//			return "";
	//		ItemsContainer<Item> fishContainer = new ItemsContainer<>(10, true);
	//
	//		int attemptsPerHour = (60*60*1000)/(5*600);
	//		int xp = 0;
	//		for (int hours = 0;hours < HOURS_TO_SAMPLE;hours++) {
	//			for (int i = 0;i < attemptsPerHour;i++) {
	//				for (Fish f : spot.getFish()) {
	//					if (level >= f.getLevel() && f.rollSuccess(level)) {
	//						fishContainer.add(new Item(f.getId(), 1));
	//						xp += f.getXp();
	//						break;
	//					}
	//				}
	//			}
	//		}
	//
	//		String fishCounts = "";
	//		List<Item> sorted = fishContainer.asList();
	//		sorted.sort((o1, o2) -> {
	//			return o1.getName().compareTo(o2.getName());
	//		});
	//		for (Item f : sorted) {
	//			if (f != null)
	//				fishCounts += (int) ((double) f.getAmount() / (double) HOURS_TO_SAMPLE) + " " + f.getName() + ", ";
	//		}
	//
	//		return spot.name() + " -> Level: " + level + " Caught: " + fishCounts + " XP: " + Utils.formatNumber((int) (xp / HOURS_TO_SAMPLE));
	//	}
	//
	//	private static String getWoodcuttingAverage(TreeType tree, int level, Hatchet hatchet) {
	//		if (level < tree.getLevel())
	//			return "";
	//		ItemsContainer<Item> wood = new ItemsContainer<>(10, true);
	//
	//		int attemptsPerHour = (60*60*1000)/(4*600);
	//		int xp = 0;
	//		for (int hours = 0;hours < HOURS_TO_SAMPLE;hours++) {
	//			for (int i = 0;i < attemptsPerHour;i++) {
	//				if (tree.rollSuccess(level, hatchet)) {
	//					wood.add(new Item(tree.getLogsId()[0], 1));
	//					xp += tree.getXp();
	//				}
	//			}
	//		}
	//
	//		String logCounts = "";
	//		List<Item> sorted = wood.asList();
	//		sorted.sort((o1, o2) -> {
	//			return o1.getName().compareTo(o2.getName());
	//		});
	//		for (Item f : sorted) {
	//			if (f != null)
	//				logCounts += (int) ((double) f.getAmount() / (double) HOURS_TO_SAMPLE) + " " + f.getName() + ", ";
	//		}
	//
	//		return "Tree: " + tree.name() + " Hatchet: " + hatchet.name() + " Level: " + level + " Cut: " + logCounts + " XP: " + Utils.formatNumber((int) (xp / HOURS_TO_SAMPLE));
	//	}
	//
	//	private static String getMiningAverage(RockType rock, int level, Pickaxe pick) {
	//		if (level < rock.getLevel() || level < pick.getLevel())
	//			return "";
	//		ItemsContainer<Item> ores = new ItemsContainer<>(10, true);
	//
	//		int totalTicks = 6000 * HOURS_TO_SAMPLE;
	//		int usedTicks = 0;
	//		int xp = 0;
	//		int tries = 0;
	//		while (usedTicks < totalTicks) {
	//			boolean success = false;
	//			tries++;
	//			for (Ore ore : rock.getOres()) {
	//				if (level >= ore.getLevel() && ore.rollSuccess(level)) {
	//					ores.add(new Item(ore.getId(), 1));
	//					xp += ore.getXp();
	//					success = true;
	//					break;
	//				}
	//			}
	//			if (success) {
	//				if ((pick == Pickaxe.DRAGON || pick == Pickaxe.DRAGON_G) && Utils.random(2) == 0)
	//					usedTicks += tries <= 1 ? 2 : pick.getTicks()-1;
	//				else
	//					usedTicks += tries <= 1 ? 3 : pick.getTicks();
	//				tries = 0;
	//			} else {
	//				if ((pick == Pickaxe.DRAGON || pick == Pickaxe.DRAGON_G) && Utils.random(2) == 0)
	//					usedTicks += pick.getTicks()-1;
	//				else
	//					usedTicks += pick.getTicks();
	//			}
	//		}
	//
	//		String logCounts = "";
	//		List<Item> sorted = ores.asList();
	//		sorted.sort((o1, o2) -> {
	//			return o1.getName().compareTo(o2.getName());
	//		});
	//		for (Item f : sorted) {
	//			if (f != null)
	//				logCounts += (int) ((double) f.getAmount() / (double) HOURS_TO_SAMPLE) + " " + f.getName() + ", ";
	//		}
	//
	//		return "Rock: " + rock.name() + " Pickaxe: " + pick.name() + " Level: " + level + " Mined: " + logCounts + " XP: " + Utils.formatNumber((int) (xp / HOURS_TO_SAMPLE));
	//	}
	//
	//	private static String getJagAverageFor(int ticksPerAction, int level, int sucLvl1, int sucLvl99, double xp) {
	//		return getJagAverageFor(ticksPerAction, level, 0, sucLvl1, sucLvl99, xp);
	//	}
	//
	//	private static String getJagAverageFor(int ticksPerAction, int level, int toolBonus, int sucLvl1, int sucLvl99, double xp) {
	//		int attemptsPerHour = (60*60*1000)/(ticksPerAction*600);
	//		int caught = 0;
	//		for (int hours = 0;hours < HOURS_TO_SAMPLE;hours++) {
	//			for (int i = 0;i < attemptsPerHour;i++) {
	//				if (Utils.skillSuccess(level, toolBonus, sucLvl1, sucLvl99))
	//					caught++;
	//			}
	//		}
	//
	//		return "Level: " + level + " Tool: "+toolBonus+" Items: " + Utils.formatNumber(caught / HOURS_TO_SAMPLE) + " XP: " + Utils.formatNumber((int) (xp * (caught / HOURS_TO_SAMPLE)));
	//	}
}
