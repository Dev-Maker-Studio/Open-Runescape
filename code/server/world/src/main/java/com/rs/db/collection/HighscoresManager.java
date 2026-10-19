package com.rs.db.collection;

import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.rs.db.model.Highscore;
import com.rs.game.model.entity.player.Player;
import com.rs.db.DBItemManager;
import org.darkan.core.JsonFileManager;
import org.darkan.core.model.Rights;
import org.darkan.core.Logger;
import org.bson.Document;

import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;

public class HighscoresManager extends DBItemManager {

	public HighscoresManager() {
		super("highscores");
	}

	@Override
	public void initCollection() {
		getDocs().createIndex(Indexes.ascending("username"));
		getDocs().createIndex(Indexes.descending("totalLevel", "totalXp"));
		dropIndexIfPresent("displayName_text_username_text");
	}

	public void save(Player player) {
		save(player, null);
	}

	public void save(Player player, Runnable done) {
		execute(() -> {
			saveSync(player);
			if (done != null)
				done.run();
		});
	}

	public void saveSync(Player player) {
		if (player.isBot() || player.hasRights(Rights.OWNER) || player.hasRights(Rights.DEVELOPER) || player.hasRights(Rights.ADMIN) || player.getSkills().getTotalXp() < 50000)
			return;
		try {
			getDocs().findOneAndReplace(eq("username", player.getUsername()), Document.parse(JsonFileManager.toJson(new Highscore(player))), new FindOneAndReplaceOptions().upsert(true));
		} catch (Exception e) {
			Logger.handle(HighscoresManager.class, "saveSync", e);
		}
	}

	public void getPlayerAtPosition(int rank, Consumer<Highscore> top) {
		execute(() -> {
			try {
				top.accept(JsonFileManager.fromJSONString(getDocs().find().sort(Sorts.descending("totalLevel", "totalXp")).skip(rank).limit(1).first().toJson(), Highscore.class));
			} catch(Throwable e) {
				top.accept(null);
			}
		});
	}
}
