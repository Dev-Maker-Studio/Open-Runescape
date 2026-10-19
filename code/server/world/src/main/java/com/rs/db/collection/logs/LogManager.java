package com.rs.db.collection.logs;

import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.rs.game.ge.Offer;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.Trade;
import com.rs.db.DBItemManager;
import org.darkan.core.JsonFileManager;
import org.darkan.core.model.GroundItem;
import org.darkan.core.model.Item;
import com.rs.utils.ReportsManager;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class LogManager extends DBItemManager {

	public LogManager() {
		super("logs");
	}

	@Override
	public void initCollection() {
		getDocs().createIndex(Indexes.ascending("hash"));
		getDocs().createIndex(Indexes.ascending("date"), new IndexOptions().expireAfter(180L, TimeUnit.DAYS));
		dropIndexIfPresent("type_text");
		dropIndexIfPresent("hash_-1");
	}

	public void save(LogEntry entry) {
		save(entry, null);
	}

	public void save(LogEntry entry, Runnable done) {
		execute(() -> {
			saveSync(entry);
			if (done != null)
				done.run();
		});
	}

	public void saveSync(LogEntry entry) {
		try {
			getDocs().findOneAndReplace(eq("hash", entry.getHash()), Document.parse(JsonFileManager.toJson(entry)), new FindOneAndReplaceOptions().upsert(true));
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public void logGE(Offer offer, Offer other, int num, int price) {
		GELog log = new GELog(offer, other, num, price);
		save(new LogEntry(LogEntry.LogType.GE, log.hashCode(), log));
	}

	public void logTrade(Player player1, Trade p1Trade, Player p2, Trade p2Trade) {
		List<Item> p1Items = new ArrayList<>();
		List<Item> p2Items = new ArrayList<>();
		for (Item item : p1Trade.getItems().array())
			if (item != null)
				p1Items.add(item);
		for (Item item : p2Trade.getItems().array())
			if (item != null)
				p2Items.add(item);
		if (p1Items.size() > 0 || p2Items.size() > 0) {
			TradeLog log = new TradeLog(player1, p1Items, p2, p2Items);
			save(new LogEntry(LogEntry.LogType.TRADE, log.hashCode(), log));
		}
	}

	public void logPickup(Player player, GroundItem item) {
		PickupLog log = new PickupLog(player, item);
		save(new LogEntry(LogEntry.LogType.PICKUP, log.hashCode(), log));
	}
	public void logGrave(GraveLog log) {
		save(new LogEntry(LogEntry.LogType.GRAVE, log.hashCode(), log));
	}

	public void logReport(Player reporter, Player reported, ReportsManager.Rule rule) {
		if (reporter == null || reported == null)
			return;
		ReportLog log = new ReportLog(reporter, reported, rule);
		save(new LogEntry(LogEntry.LogType.REPORT, log.hashCode(), log));
	}

	public void logCommand(String username, String commandStr) {
		CommandLog log = new CommandLog(username, commandStr);
		save(new LogEntry(LogEntry.LogType.COMMAND, log.hashCode(), log));
	}
}
