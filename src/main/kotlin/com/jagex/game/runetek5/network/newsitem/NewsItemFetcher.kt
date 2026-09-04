package com.jagex.game.runetek5.network.newsitem

import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.network.WorldConnection
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

/**
 * Fetches news items from the game server in a background thread.
 */
class NewsItemFetcher : Runnable {

    @JvmField var thread: Thread? = null
    @Volatile @JvmField var fetched = false
    @JvmField var newsItems: Array<NewsItem>? = null

    /**
     * Retrieves a news item by ID.
     *
     * @param id The news item ID
     * @return The news item, or null if not found or not yet fetched
     */
    fun get(id: Int): NewsItem? {
        return if (newsItems != null && id >= 0 && id < newsItems!!.size) newsItems!![id] else null
    }

    /**
     * Initiates the fetch operation if not already started.
     *
     * @return true if the fetch is complete, false if still in progress
     */
    fun fetch(): Boolean {
        if (fetched) {
            return true
        }
        if (thread == null) {
            thread = Thread(this)
            thread!!.start()
        }
        return fetched
    }

    override fun run() {
        try {
            val port = if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LIVE) {
                80
            } else {
                WorldConnection.GAME_CONNECTION!!.worldId + 7000
            }
            val reader = BufferedReader(
                InputStreamReader(
                    DataInputStream(
                        URL("http://${WorldConnection.GAME_CONNECTION!!.host}:$port/news.ws?game=${Client.CURRENT_GAME!!.id}")
                            .openStream()
                    )
                )
            )
            var line = reader.readLine()
            val lines = ArrayList<String>()
            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }
            val lineArray = lines.toTypedArray()
            if (lineArray.size % 3 != 0) {
                return
            }
            newsItems = Array(lineArray.size / 3) { index ->
                val baseIndex = index * 3
                NewsItem(lineArray[baseIndex], lineArray[baseIndex + 1], lineArray[baseIndex + 2])
            }
        } catch (ioexception: IOException) {
            // NOOP
        }
        fetched = true
    }
}
