package com.rs.db.collection;

import com.google.gson.JsonIOException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.rs.game.ge.Offer;
import com.rs.game.ge.Offer.State;
import com.rs.db.DBItemManager;
import org.darkan.core.JsonFileManager;
import org.darkan.core.Logger;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GEManager extends DBItemManager {

	public GEManager() {
		super("grandexchange");
	}

	@Override
	public void initCollection() {
		getDocs().createIndex(Indexes.ascending("owner", "box"));
		getDocs().createIndex(Indexes.ascending("state", "selling", "itemId", "price"));
		dropIndexIfPresent("owner_text");
		dropIndexIfPresent("itemId_1_completedAmount_1");
		dropIndexIfPresent("itemId_1_state_1");
		dropIndexIfPresent("itemId_1_price_1");
		dropIndexIfPresent("itemId_1_selling_1");
	}

	public void get(String username, Consumer<List<Offer>> func) {
		execute(() -> func.accept(getSync(username)));
	}

	public void save(Offer offer) {
		save(offer, null);
	}

	public void save(Offer offer, Runnable done) {
		execute(() -> {
			saveSync(offer);
			if (done != null)
				done.run();
		});
	}

	public void saveSync(Offer offers) {
		getDocs().findOneAndReplace(Filters.and(Filters.eq("owner", offers.getOwner()), Filters.eq("box", offers.getBox())), Document.parse(JsonFileManager.toJson(offers)), new FindOneAndReplaceOptions().upsert(true));
	}

	public List<Offer> getSync(String username) {
		FindIterable<Document> offerDocs = getDocs().find(Filters.eq("owner", username));
		if (offerDocs == null)
			return null;
		try {
			List<Offer> offers = new ArrayList<>(6);
			for (Document d : offerDocs) {
				Offer offer = JsonFileManager.fromJSONString(JsonFileManager.toJson(d), Offer.class);
				offers.add(offer);
			}
			return offers;
		} catch (JsonIOException | IOException e) {
			Logger.handle(GEManager.class, "getSync", e);
			return null;
		}
	}

	public void remove(String username, int box, Runnable done) {
		execute(() -> {
			removeSync(username, box);
			if (done != null)
				done.run();
		});
	}

	public void removeSync(String username, int box) {
		getDocs().findOneAndDelete(Filters.and(Filters.eq("owner", username), Filters.eq("box", box)));
	}

	public List<Offer> getBestOffersSync(Offer other) {
		if (other.getState() == State.FINISHED)
			return null;
		List<Offer> result = new ArrayList<>();
		FindIterable<Document> docs = getDocs().find(Filters.and(Filters.eq("state", State.STABLE.toString()), Filters.eq("itemId", other.getItemId()), Filters.eq("selling", !other.isSelling()), other.isSelling() ? Filters.gte("price", other.getPrice()) : Filters.lte("price", other.getPrice()))).sort(Sorts.ascending("price"));
		for (Document doc : docs)
			try {
				Offer offer = JsonFileManager.fromJSONString(JsonFileManager.toJson(doc), Offer.class);
				result.add(offer);
			} catch (JsonIOException | IOException e) {
				Logger.handleNoRecord(GEManager.class, "getBestOffersSync", "Error converting document: " + result, e);
			}
		return result;
	}

	public void getBestOffer(int itemId, boolean sell, Consumer<Offer> func) {
		execute(() -> {
			FindIterable<Document> docs = getDocs().find(Filters.and(Filters.eq("state", State.STABLE.toString()), Filters.eq("itemId", itemId), Filters.eq("selling", !sell))).sort(sell ? Sorts.ascending("price") : Sorts.descending("price"));
			try {
				func.accept(JsonFileManager.fromJSONString(JsonFileManager.toJson(docs.first()), Offer.class));
			} catch (JsonIOException | IOException e) {
				func.accept(null);
			}
		});
	}

	public void getAllOffersOfType(boolean selling, Consumer<List<Offer>> func) {
		execute(() -> {
			List<Offer> result = new ArrayList<>();
			FindIterable<Document> docs = getDocs().find(Filters.and(Filters.eq("state", State.STABLE.toString()), Filters.eq("selling", selling)));
			for (Document doc : docs)
				try {
					Offer offer = JsonFileManager.fromJSONString(JsonFileManager.toJson(doc), Offer.class);
					result.add(offer);
				} catch (JsonIOException | IOException e) {
					Logger.handleNoRecord(GEManager.class, "getAllOffersOfType", "Error converting document: " + result, e);
				}
			func.accept(result);
		});
	}

}
