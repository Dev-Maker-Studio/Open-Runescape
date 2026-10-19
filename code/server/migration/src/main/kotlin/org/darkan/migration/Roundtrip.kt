package org.darkan.migration

import com.rs.db.model.Highscore
import com.rs.game.ge.Offer
import com.rs.game.model.entity.player.Player
import com.rs.utils.json.ControllerAdapter
import com.rs.utils.json.WorldGson
import kotlinx.coroutines.runBlocking
import org.bson.BsonDocument
import org.bson.BsonDocumentWrapper
import org.bson.BsonValue
import org.bson.Document
import org.darkan.core.EnvVars
import org.darkan.core.JsonFileManager
import org.darkan.core.mongo.MongoDB
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.mongo.collections.Clans
import world.gregs.voidps.cache.Cache
import java.io.File
import java.util.Date
import java.util.TreeMap

fun roundtrip(target: MongoTarget): Boolean {
    val configured = MongoTarget(EnvVars.mongoUri)
    require(configured.describe() == target.describe()) {
        "MONGO_URI points at ${configured.describe()} but the target is ${target.describe()}; " +
            "run with MONGO_URI set to the target so the modern collections read it."
    }
    println("Round-tripping ${target.describe()}: decode through the server code, encode back, diff against the stored document")
    JsonFileManager.setGSON(WorldGson.create())
    if (File(EnvVars.cachePath).isDirectory) Cache.init(EnvVars.cachePath) else println("   (cache not found at ${EnvVars.cachePath}; decoding without it)")
    ControllerAdapter.init()

    var clean = true
    clean = pojoRoundtrip(target, "accounts", "username") { runBlocking { Accounts.find(it) } } && clean
    clean = pojoRoundtrip(target, "clans", "name") { runBlocking { Clans.find(it) } } && clean
    clean = gsonRoundtrip(target, "players", "username", Player::class.java) && clean
    clean = gsonRoundtrip(target, "highscores", "username", Highscore::class.java) && clean
    clean = gsonRoundtrip(target, "grandexchange", "owner", Offer::class.java) && clean
    return clean
}

private fun pojoRoundtrip(target: MongoTarget, collection: String, keyField: String, load: (String) -> Any?): Boolean {
    val registry = MongoDB.database.codecRegistry
    val diff = Diff(collection, ignored = setOf("_id"))
    for (raw in target.database.getCollection(collection).find()) {
        val key = raw.getString(keyField) ?: continue
        diff.scanned++
        val decoded = try {
            load(key)
        } catch (e: Exception) {
            diff.problem("decode failed: ${describe(e)}", key)
            continue
        }
        if (decoded == null) {
            diff.problem("not found through the modern store", key)
            continue
        }
        val encoded = BsonDocumentWrapper.asBsonDocument(decoded, registry)
        diff.compare(raw.toBsonDocument(BsonDocument::class.java, registry), encoded, key)
    }
    diff.print()
    return !diff.hasProblems
}

private fun <T : Any> gsonRoundtrip(target: MongoTarget, collection: String, keyField: String, type: Class<T>): Boolean {
    val diff = Diff(collection, ignored = setOf("_id"))
    for (raw in target.database.getCollection(collection).find()) {
        val key = raw[keyField]?.toString() ?: raw.idKey()
        diff.scanned++
        val encoded = try {
            val decoded = JsonFileManager.fromJSONString(JsonFileManager.toJson(raw), type)
            Document.parse(JsonFileManager.toJson(decoded))
        } catch (e: Throwable) {
            diff.problem("decode failed: ${describe(e)}", key)
            continue
        }
        diff.compare(raw, encoded, key)
        val again = try {
            Document.parse(JsonFileManager.toJson(JsonFileManager.fromJSONString(encoded.toJson(), type)))
        } catch (e: Throwable) {
            diff.problem("re-decode of the re-encoded document failed: ${describe(e)}", key)
            continue
        }
        if (again != encoded) diff.problem("not a fixed point: encode(decode(x)) keeps changing", key)
    }
    diff.print()
    return !diff.hasProblems
}

private class Diff(val collection: String, private val ignored: Set<String> = emptySet()) {
    var scanned = 0
    private val lost = TreeMap<String, Int>()
    private val added = TreeMap<String, Int>()
    private val changed = TreeMap<String, Int>()
    private val retyped = TreeMap<String, Int>()
    private val reordered = TreeMap<String, Int>()
    private val examples = TreeMap<String, String>()
    private val problems = TreeMap<String, MutableList<String>>()
    private val documentsWithLoss = LinkedHashSet<String>()

    val hasProblems get() = problems.isNotEmpty() || lost.isNotEmpty() || changed.isNotEmpty()

    fun problem(rule: String, key: String) {
        problems.getOrPut(rule) { mutableListOf() }.add(key)
    }

    fun compare(before: Any?, after: Any?, key: String) = compare("", before, after, key)

    private fun compare(path: String, before: Any?, after: Any?, key: String) {
        when {
            before is Map<*, *> && after is Map<*, *> -> {
                for (field in before.keys.map { it.toString() }) {
                    if (path.isEmpty() && field in ignored) continue
                    val child = if (path.isEmpty()) field else "$path.$field"
                    if (!after.containsKey(field)) record(lost, child, key, "${before[field]}") else compare(child, before[field], after[field], key)
                }
                for (field in after.keys.map { it.toString() }) {
                    if (path.isEmpty() && field in ignored) continue
                    if (!before.containsKey(field)) record(added, if (path.isEmpty()) field else "$path.$field", key, "${after[field]}")
                }
            }
            before is List<*> && after is List<*> -> when {
                before.size != after.size -> record(changed, "$path[]", key, "size ${before.size} -> ${after.size}")
                before.all { it !is Map<*, *> && it !is List<*> } && before.map { it.toString() }.sorted() == after.map { it.toString() }.sorted() ->
                    if (before != after) record(reordered, "$path[]", key, "same elements, other order")
                else -> before.indices.forEach { compare("$path[]", before[it], after[it], key) }
            }
            before == null || after == null -> if (before != after) record(changed, path, key, "$before -> $after")
            isNumber(before) && isNumber(after) -> {
                val a = numberOf(before)
                val b = numberOf(after)
                if (a != b) record(changed, path, key, "$before -> $after")
                else if (before::class != after::class) record(retyped, path, key, "${before::class.simpleName} -> ${after::class.simpleName}")
            }
            isDate(before) && isDate(after) -> if (dateOf(before) != dateOf(after)) record(changed, path, key, "$before -> $after")
            else -> if (unwrap(before) != unwrap(after)) record(changed, path, key, "$before -> $after")
        }
    }

    private fun record(bucket: TreeMap<String, Int>, path: String, key: String, example: String) {
        bucket[path] = (bucket[path] ?: 0) + 1
        examples.putIfAbsent(path, "$key: ${example.take(200)}")
        if (bucket === lost || bucket === changed) documentsWithLoss.add(key)
    }

    fun print() {
        println("== $collection: scanned $scanned; documents that would lose or change data: ${documentsWithLoss.size}")
        section("lost on write (field dropped by the modern class)", lost)
        section("changed on write", changed)
        section("added on write (modern default)", added)
        section("same value, other BSON type", retyped)
        section("same set, other order", reordered)
        for ((rule, keys) in problems) {
            println("   !! $rule: ${keys.size}")
            keys.take(25).forEach { println("        $it") }
            if (keys.size > 25) println("        ... ${keys.size - 25} more")
        }
    }

    private fun section(title: String, bucket: TreeMap<String, Int>) {
        if (bucket.isEmpty()) return
        println("   ${if (bucket === lost || bucket === changed) "!! " else ""}$title: ${bucket.size} paths")
        bucket.entries.sortedByDescending { it.value }.take(40).forEach { (path, n) -> println("        $path x$n   e.g. ${examples[path]}") }
        if (bucket.size > 40) println("        ... ${bucket.size - 40} more paths")
    }

    private fun isNumber(v: Any) = v is Number || (v is BsonValue && v.isNumber)
    private fun numberOf(v: Any): Double = if (v is BsonValue) v.asNumber().doubleValue() else (v as Number).toDouble()
    private fun isDate(v: Any) = v is Date || (v is BsonValue && v.isDateTime)
    private fun dateOf(v: Any): Long = if (v is BsonValue) v.asDateTime().value else (v as Date).time
    private fun unwrap(v: Any): Any = when {
        v is BsonValue && v.isString -> v.asString().value
        v is BsonValue && v.isBoolean -> v.asBoolean().value
        v is BsonValue && v.isNull -> "null"
        v is BsonValue && v.isObjectId -> v.asObjectId().value
        else -> v
    }
}

private fun describe(e: Throwable): String {
    var root: Throwable = e
    while (root.cause != null && root.cause !== root) root = root.cause!!
    val message = (root.message ?: "").lineSequence().firstOrNull().orEmpty().take(400)
    return "${root::class.simpleName}: $message"
}
