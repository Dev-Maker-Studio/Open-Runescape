package org.darkan.migration

import com.mongodb.ConnectionString
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.InsertManyOptions
import org.bson.Document
import org.bson.types.ObjectId
import org.darkan.core.model.ClanSetting
import java.util.Date

/** The collections the legacy servers persist, with the transform that brings each to the modern shape. */
val COLLECTIONS: Map<String, Transform> = linkedMapOf(
    "accounts" to AccountsTransform,
    "clans" to ClansTransform,
    "players" to PlayersTransform,
    "highscores" to CopyTransform,
    "grandexchange" to OffersTransform,
    "logs" to LogsTransform,
)

class MongoTarget(uri: String) : AutoCloseable {
    private val connection = ConnectionString(uri)
    val databaseName: String = connection.database
        ?: throw IllegalArgumentException("URI must name a database: ${redact(uri)}")
    val host: String = connection.hosts.joinToString(",")
    val client: MongoClient = MongoClients.create(uri)
    val database: MongoDatabase = client.getDatabase(databaseName)

    fun describe() = "$host/$databaseName"
    override fun close() = client.close()

    companion object {
        fun redact(uri: String) = uri.replace(Regex("://([^:/@]+):[^@]+@"), "://$1:***@")
    }
}

/**
 * Copies every collection from [source] into [target], applying the per-collection transform on
 * the way. The source is only ever read. [dryRun] performs the whole transform and prints the
 * report but writes nothing, which is how a run is judged before it is trusted.
 */
fun migrate(source: MongoTarget, target: MongoTarget, only: Set<String>, dryRun: Boolean, replace: Boolean): Boolean {
    require(!(source.host == target.host && source.databaseName == target.databaseName)) {
        "Source and target are the same database (${source.describe()}); refusing."
    }
    println("Migrating ${source.describe()} -> ${target.describe()}${if (dryRun) " [DRY RUN]" else ""}")

    var clean = true
    for ((name, transform) in COLLECTIONS) {
        if (only.isNotEmpty() && name !in only) continue
        val report = Report(name)
        val targetCollection = target.database.getCollection(name)
        if (!dryRun) {
            val existing = targetCollection.countDocuments()
            if (existing > 0) {
                if (!replace) {
                    println("== $name: target already holds $existing documents; pass --replace to drop them first. Skipped.")
                    clean = false
                    continue
                }
                targetCollection.drop()
                println("== $name: dropped $existing existing target documents")
            }
        }

        val batch = ArrayList<Document>(BATCH_SIZE)
        fun flush() {
            if (batch.isEmpty()) return
            if (!dryRun) targetCollection.insertMany(batch, InsertManyOptions().ordered(false))
            report.written += batch.size
            batch.clear()
        }

        val sourceCollection = source.database.getCollection(name)
        transform.prepare(sourceCollection)
        for (doc in sourceCollection.find()) {
            report.scanned++
            val out = try {
                transform.apply(doc, report)
            } catch (e: Exception) {
                report.problem("transform threw ${e::class.simpleName}: ${e.message}", doc["_id"].toString())
                null
            }
            if (out != null) batch += out
            if (batch.size >= BATCH_SIZE) flush()
        }
        flush()
        report.print()
        if (report.hasProblems) clean = false
    }
    return clean
}

/**
 * Read-only survey of a legacy database: what is there, and which documents will need a decision
 * before the modern constraints (non-null fields, unique lowercase email) can hold.
 */
fun inspect(source: MongoTarget) {
    println("Inspecting ${source.describe()}")
    val names = source.database.listCollectionNames().toList().sorted()
    for (name in names)
        println("   $name: ${source.database.getCollection(name).countDocuments()} documents")

    val accounts = source.database.getCollection("accounts")
    val byEmail = HashMap<String, MutableList<String>>()
    var nullEmail = 0
    var emptyEmail = 0
    var legacyPasswordOnly = 0
    var argon2 = 0
    var nullDisplay = 0
    for (doc in accounts.find()) {
        val username = doc.getString("username") ?: "<no username>"
        val email = doc.getString("email")
        when {
            !doc.containsKey("email") || email == null -> nullEmail++
            email.isEmpty() -> emptyEmail++
            else -> byEmail.getOrPut(email.lowercase()) { mutableListOf() }.add(username)
        }
        val hash = doc.getString("passwordHash")
        if (hash != null && hash.startsWith("\$argon2")) argon2++ else legacyPasswordOnly++
        if (doc.containsKey("displayName") && doc["displayName"] == null) nullDisplay++
    }
    val duplicates = byEmail.filterValues { it.size > 1 }
    val byUsername = HashMap<String, MutableList<Document>>()
    for (doc in accounts.find().projection(Document("username", 1).append("email", 1).append("displayName", 1).append("rights", 1).append("lastIp", 1).append("banned", 1).append("passwordHash", 1)))
        byUsername.getOrPut(doc.getString("username") ?: "<no username>") { mutableListOf() }.add(doc)
    val duplicateUsernames = byUsername.filterValues { it.size > 1 }
    println("== accounts")
    println("   duplicate usernames: ${duplicateUsernames.size}")
    for ((username, docs) in duplicateUsernames)
        for (doc in docs)
            println("        $username -> _id=${doc["_id"]} (${(doc["_id"] as? ObjectId)?.date}) email=${doc["email"]} displayName=${doc["displayName"]} rights=${doc["rights"]} lastIp=${doc["lastIp"]} banned=${doc["banned"]} argon2=${doc.getString("passwordHash")?.startsWith("\$argon2") == true}")
    println("   argon2 hashes: $argon2, still on legacy credentials: $legacyPasswordOnly")
    println("   null email: $nullEmail, empty email: $emptyEmail, null displayName: $nullDisplay")
    println("   duplicate emails (case-insensitive): ${duplicates.size}")
    duplicates.entries.take(25).forEach { (email, users) -> println("        $email -> $users") }

    val players = source.database.getCollection("players")
    val dateShapes = HashMap<String, Int>()
    var unparseable = 0
    for (doc in players.find().projection(Document("dateJoined", 1))) {
        val raw = doc["dateJoined"]
        val shape = when (raw) {
            null -> "missing/null"
            is Date -> "bson date"
            is String -> if (LegacyDates.parse(raw) != null) "legacy string (parses)" else "legacy string (UNPARSEABLE)".also { unparseable++ }
            else -> raw::class.simpleName ?: "?"
        }
        dateShapes[shape] = (dateShapes[shape] ?: 0) + 1
    }
    println("== players.dateJoined shapes: $dateShapes")

    val clans = source.database.getCollection("clans")
    val unknownIds = HashMap<Int, Int>()
    for (doc in clans.find()) {
        val settings = doc.get("settings", Document::class.java) ?: continue
        for (key in settings.keys) {
            val id = key.toIntOrNull() ?: continue
            if (ClanSetting.forId(id) == null) unknownIds[id] = (unknownIds[id] ?: 0) + 1
        }
    }
    println("== clans: settings ids with no modern ClanSetting: $unknownIds")
}

private const val BATCH_SIZE = 500
