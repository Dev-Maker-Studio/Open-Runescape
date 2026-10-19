package com.rs.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub replacement for the deleted library DBConnection class.
 * TODO: Migrate to core MongoDB module (org.darkan.core.mongo).
 */
public class DBConnection {
    private static MongoClient client;
    private static MongoDatabase database;
    private final List<DBItemManager> itemManagers = new ArrayList<>();

    public DBConnection(String connectionString, String databaseName) {
        client = MongoClients.create(connectionString);
        database = client.getDatabase(databaseName);
    }

    public static MongoCollection<Document> getCollection(String name) {
        if (database == null)
            throw new IllegalStateException("DBConnection not initialized");
        return database.getCollection(name);
    }

    public void addItemManager(DBItemManager manager) {
        manager.init();
        itemManagers.add(manager);
    }

    public void shutdown() {
        if (client != null)
            client.close();
    }
}
