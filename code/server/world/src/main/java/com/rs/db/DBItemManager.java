package com.rs.db;

import com.mongodb.client.MongoCollection;
import com.rs.engine.thread.CatchExceptionRunnable;
import org.bson.Document;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Stub replacement for the deleted library DBItemManager class.
 * TODO: Migrate to core MongoDB module (org.darkan.core.mongo).
 */
public abstract class DBItemManager {
    private final String collectionName;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DBItemManager(String collectionName) {
        this.collectionName = collectionName;
    }

    public void init() {
        initCollection();
    }

    public abstract void initCollection();

    protected String getCollectionName() {
        return collectionName;
    }

    public MongoCollection<Document> getDocs() {
        return DBConnection.getCollection(collectionName);
    }

    protected void dropIndexIfPresent(String name) {
        for (Document index : getDocs().listIndexes())
            if (name.equals(index.getString("name"))) {
                getDocs().dropIndex(name);
                return;
            }
    }

    public void execute(Runnable task) {
        // Wrapped so a failing save is logged instead of vanishing into the ignored Future
        // returned by submit(). Matches legacy com.rs.lib.db.DBItemManager#execute (line 61).
        executor.execute(new CatchExceptionRunnable(task));
    }
}
