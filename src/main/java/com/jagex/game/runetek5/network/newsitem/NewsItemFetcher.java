package com.jagex.game.runetek5.network.newsitem;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.network.WorldConnection;

public class NewsItemFetcher implements Runnable {

    public Thread thread;

    public volatile boolean fetched;

    public NewsItem[] newsItems;

    public NewsItem get(int id) {
        return this.newsItems != null && id >= 0 && id < this.newsItems.length ? this.newsItems[id] : null;
    }

    public boolean fetch() {
        if (this.fetched) {
            return true;
        } 
        if (this.thread == null) {
            this.thread = new Thread(this);
            this.thread.start();
        }
        return this.fetched;
    }

    @Override
    public void run() {
        try {
            int port = ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LIVE ? 80 : WorldConnection.GAME_CONNECTION.worldId + 7000;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream((new URL("http://" + WorldConnection.GAME_CONNECTION.host + ":" + port + "/news.ws?game=" + client.CURRENT_GAME.id)).openStream())));
            String line = reader.readLine();
            ArrayList<String> lines;
            for (lines = new ArrayList<>(); line != null; line = reader.readLine()) {
                lines.add(line);
            }
            String[] lineArray = new String[lines.size()];
            lines.toArray(lineArray);
            if (lineArray.length % 3 != 0) {
                return;
            }
            this.newsItems = new NewsItem[lineArray.length / 3];
            for (int index = 0; index < lineArray.length; index += 3) {
                this.newsItems[index / 3] = new NewsItem(lineArray[index], lineArray[index + 1], lineArray[index + 2]);
            }
        } catch (IOException ioexception) {
        	// NOOP
        }
        this.fetched = true;
    }
}
