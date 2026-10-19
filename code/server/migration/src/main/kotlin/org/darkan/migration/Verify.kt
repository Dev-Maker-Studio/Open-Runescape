package org.darkan.migration

import com.rs.game.model.entity.player.Player
import com.rs.utils.json.ControllerAdapter
import com.rs.utils.json.WorldGson
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.darkan.core.EnvVars
import org.darkan.core.JsonFileManager
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.mongo.collections.Clans
import world.gregs.voidps.cache.Cache
import java.io.File

/**
 * Reads every migrated document back through the code the servers themselves use - the Mongo POJO
 * codecs behind [Accounts] and [Clans], and the world's Gson configuration for player saves - and
 * reports each one that fails to decode. A clean run is the evidence that nobody loses their
 * progress at login.
 *
 * The modern collections connect through `MONGO_URI`, so the process must be started with that
 * variable pointing at the target; this is checked rather than assumed.
 */
fun verify(target: MongoTarget): Boolean {
    val configured = MongoTarget(EnvVars.mongoUri)
    require(configured.describe() == target.describe()) {
        "MONGO_URI points at ${configured.describe()} but the target is ${target.describe()}; " +
            "run with MONGO_URI set to the target so the modern collections read it."
    }
    println("Verifying ${target.describe()} through the modern code paths")

    var clean = true
    clean = verifyAccounts(target) && clean
    clean = verifyClans(target) && clean
    clean = verifyPlayers(target) && clean
    return clean
}

private fun verifyAccounts(target: MongoTarget): Boolean {
    val report = Report("accounts (Accounts.find)")
    val raw = target.database.getCollection("accounts")
    try {
        for (doc in raw.find().projection(Document("username", 1))) {
            report.scanned++
            val username = doc.getString("username") ?: continue
            try {
                val account = runBlocking { Accounts.find(username) }
                when {
                    account == null -> report.problem("not found through Accounts.find", username)
                    account.username != username -> report.problem("username changed on decode", "$username -> ${account.username}")
                    else -> report.written++
                }
            } catch (e: Exception) {
                report.problem("decode failed: ${describe(e)}", username)
            }
        }
    } catch (e: Exception) {
        // Typically the unique email index failing to build on first access.
        report.problem("collection init failed: ${describe(e)}", "-")
    }
    report.print()
    return !report.hasProblems
}

private fun verifyClans(target: MongoTarget): Boolean {
    val report = Report("clans (Clans.find)")
    for (doc in target.database.getCollection("clans").find()) {
        report.scanned++
        val name = doc.getString("name") ?: continue
        val expectedMembers = doc.get("members", Document::class.java)?.size ?: 0
        try {
            val clan = runBlocking { Clans.find(name) }
            when {
                clan == null -> report.problem("not found through Clans.find", name)
                clan.name != name -> report.problem("name lost on decode", name)
                clan.members.size != expectedMembers -> report.problem("members lost on decode", "$name: ${clan.members.size}/$expectedMembers")
                clan.leaderUsername.isEmpty() -> report.problem("leader lost on decode", name)
                else -> report.written++
            }
        } catch (e: Exception) {
            report.problem("decode failed: ${describe(e)}", name)
        }
    }
    report.print()
    return !report.hasProblems
}

private fun verifyPlayers(target: MongoTarget): Boolean {
    val report = Report("players (world Gson)")
    JsonFileManager.setGSON(WorldGson.create())
    val cache = File(EnvVars.cachePath)
    if (cache.isDirectory) {
        Cache.init(EnvVars.cachePath)
    } else {
        println("   (cache not found at ${cache.absolutePath}; decoding without it)")
    }
    ControllerAdapter.init()

    for (doc in target.database.getCollection("players").find()) {
        report.scanned++
        val username = doc.getString("username") ?: continue
        try {
            val player = JsonFileManager.fromJSONString(JsonFileManager.toJson(doc), Player::class.java)
            if (player.username != username) report.problem("username changed on decode", "$username -> ${player.username}")
            else report.written++
        } catch (e: Throwable) {
            report.problem("decode failed: ${describe(e)}", username)
        }
    }
    report.print()
    return !report.hasProblems
}

private fun describe(e: Throwable): String {
    var root: Throwable = e
    while (root.cause != null && root.cause !== root) root = root.cause!!
    val message = (root.message ?: "").lineSequence().firstOrNull().orEmpty().take(400)
    return "${root::class.simpleName}: $message"
}
