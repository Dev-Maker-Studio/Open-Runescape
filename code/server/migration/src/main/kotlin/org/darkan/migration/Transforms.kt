package org.darkan.migration

import com.rs.utils.json.DateAdapter
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.bson.types.ObjectId
import org.darkan.core.model.Account
import org.darkan.core.model.Clan
import org.darkan.core.model.ClanSetting
import org.darkan.core.model.FriendsChat
import org.darkan.core.model.Social
import java.util.Date

/**
 * Rewrites one legacy document into the shape the modern Kotlin classes persist. Returns null to
 * drop the document, having recorded why on the [Report].
 */
interface Transform {
    fun prepare(source: MongoCollection<Document>) {}
    fun apply(doc: Document, report: Report): Document?
}

/** Collections whose documents already match the modern classes are copied verbatim. */
object CopyTransform : Transform {
    override fun apply(doc: Document, report: Report): Document = doc
}

/**
 * `accounts` → [Account]. The classes match field for field apart from the
 * renamed password history; the remaining work is making legacy nulls and casing conform to the
 * modern non-null, lowercase-email model.
 */
object AccountsTransform : Transform {
    private val nonNullStrings = listOf("email", "recoveryEmail", "displayName", "prevDisplayName", "passwordHash")
    // `currentFriendsChat` is nullable on the modern model (null = not in a chat), so a legacy null stays.
    private val socialNonNull = listOf("friends", "ignores", "status", "fcStatus", "connectedToClan", "friendsChat")
    private val friendsChatNonNull = listOf("friendsChatRanks", "rankToEnter", "rankToSpeak", "rankToKick", "rankToLS", "coinshare")

    private val socialDefaults: Map<String, Any> = Social().let { d ->
        mapOf(
            "friends" to d.friends.toList(),
            "ignores" to d.ignores.toList(),
            "status" to d.status.toInt(),
            "fcStatus" to d.fcStatus.toInt(),
            "connectedToClan" to d.connectedToClan,
            "currentFriendsChat" to checkNotNull(d.currentFriendsChat),
        )
    }
    private val friendsChatDefaults: Map<String, Any> = FriendsChat().let { d ->
        mapOf(
            "friendsChatRanks" to Document(),
            "rankToEnter" to d.rankToEnter.name,
            "rankToSpeak" to d.rankToSpeak.name,
            "rankToKick" to d.rankToKick.name,
            "rankToLS" to d.rankToLS.name,
            "coinshare" to d.coinshare,
        )
    }

    private fun fillMissing(doc: Document, defaults: Map<String, Any>, where: String, report: Report) {
        for ((field, default) in defaults) {
            if (!doc.containsKey(field)) {
                doc[field] = default
                report.count("missing $where.$field defaulted", 1)
            }
        }
    }

    private fun unsetNulls(doc: Document, fields: List<String>, where: String, report: Report) {
        for (field in fields) {
            if (doc.containsKey(field) && doc[field] == null) {
                doc.remove(field)
                report.count("null $where.$field unset", 1)
            }
        }
    }

    private val newestByUsername = HashMap<String, ObjectId>()

    /** Two documents with one username: the newest survives, since a re-registration superseded the first. */
    override fun prepare(source: MongoCollection<Document>) {
        for (doc in source.find().projection(Document("username", 1))) {
            val username = doc.getString("username") ?: continue
            val id = doc["_id"] as? ObjectId ?: continue
            newestByUsername.merge(username, id) { a, b -> if (b.date.after(a.date)) b else a }
        }
    }

    override fun apply(doc: Document, report: Report): Document? {
        val username = doc.getString("username")
        if (username.isNullOrEmpty()) {
            report.problem("missing username (dropped)", doc.idKey())
            return null
        }
        val newest = newestByUsername[username]
        if (newest != null && newest != doc["_id"]) {
            report.count("older duplicate username dropped", 1)
            println("   duplicate username $username: dropped ${doc.idKey()}, keeping $newest")
            return null
        }

        if (doc.containsKey("prevPasswords")) {
            val history = (doc["prevPasswords"] as? List<*>).orEmpty()
            // Legacy tolerated raw byte arrays mixed in; only Argon2 strings mean anything now.
            val strings = history.filterIsInstance<String>()
            report.count("prevPasswords renamed", 1)
            report.count("non-string password history entries dropped", history.size - strings.size)
            doc.remove("prevPasswords")
            doc["previousPasswords"] = strings
        }

        for (field in nonNullStrings) {
            if (doc.containsKey(field) && doc[field] == null) {
                doc.remove(field)
                report.count("null $field unset", 1)
            }
        }
        for (field in listOf("social", "rights")) {
            if (doc.containsKey(field) && doc[field] == null) {
                doc.remove(field)
                report.count("null $field unset", 1)
            }
        }
        // The nested Social / FriendsChat constructors are non-null for these too.
        // Social and FriendsChat are Kotlin data classes; their codec passes a missing field to the
        // constructor as null instead of applying the declared default, so absent keys are filled
        // from a freshly constructed instance - the defaults can never drift from the class.
        val social = doc.get("social", Document::class.java)
        if (social != null) {
            unsetNulls(social, socialNonNull, "social", report)
            fillMissing(social, socialDefaults, "social", report)
            val friendsChat = social.get("friendsChat", Document::class.java)
            if (friendsChat != null) {
                unsetNulls(friendsChat, friendsChatNonNull, "social.friendsChat", report)
                fillMissing(friendsChat, friendsChatDefaults, "social.friendsChat", report)
            }
        }

        val email = doc.getString("email")
        if (email != null && email != email.lowercase()) {
            doc["email"] = email.lowercase()
            report.count("email lowercased", 1)
        }
        val recovery = doc.getString("recoveryEmail")
        if (recovery != null && recovery != recovery.lowercase()) {
            doc["recoveryEmail"] = recovery.lowercase()
            report.count("recoveryEmail lowercased", 1)
        }
        return doc
    }
}

/**
 * `clans` → [Clan]. Settings move from client var ids to enum names, the
 * motto is surfaced as its own field, and the transient update block is dropped.
 */
object ClansTransform : Transform {
    override fun apply(doc: Document, report: Report): Document? {
        val name = doc.getString("name")
        if (name.isNullOrEmpty()) {
            report.problem("missing name (dropped)", doc.idKey())
            return null
        }

        if (doc.containsKey("clanLeaderUsername")) {
            doc["leaderUsername"] = doc.remove("clanLeaderUsername")
            report.count("clanLeaderUsername renamed", 1)
        }
        if (doc.remove("updateBlock") != null)
            report.count("updateBlock unset", 1)

        val settings = doc.get("settings", Document::class.java) ?: Document()
        val named = Document()
        for ((key, raw) in settings) {
            val value = normaliseNumber(raw, report, "settings")
            val id = key.toIntOrNull()
            if (id == null) {
                // Already an enum name, or junk; keep whatever the modern enum recognises.
                if (ClanSetting.forName(key) != null) named[key] = value
                else report.problem("unknown settings key dropped", "$name: $key")
                continue
            }
            val setting = ClanSetting.forId(id)
            if (setting == null) {
                report.problem("settings id with no modern ClanSetting dropped", "$name: $id")
                continue
            }
            named[setting.name] = value
            report.count("settings keys remapped id->name", 1)
        }
        doc["settings"] = named

        val motto = named[ClanSetting.MOTTO.name] as? String
        doc["motto"] = motto ?: ""
        if (motto != null) report.count("motto surfaced", 1)

        val vars = doc.get("vars", Document::class.java)
        if (vars != null) {
            val cleaned = Document()
            for ((key, raw) in vars) cleaned[key] = normaliseNumber(raw, report, "vars")
            doc["vars"] = cleaned
        }

        val members = doc.get("members", Document::class.java)
        if (members != null) {
            for ((_, member) in members) {
                val data = member as? Document ?: continue
                val joinDate = data["joinDate"]
                if (joinDate is Double) {
                    data["joinDate"] = joinDate.toLong()
                    report.count("member joinDate double->long", 1)
                }
            }
        }
        return doc
    }

    /**
     * The legacy read path went through Gson with `Object` targets, which turns every number into
     * a double, so integers that were re-saved come back as `3.0`. The modern setting values are
     * ints, longs or strings.
     */
    private fun normaliseNumber(value: Any?, report: Report, where: String): Any? {
        if (value !is Double) return value
        if (value != Math.floor(value) || value.isInfinite()) return value
        report.count("$where double->integer", 1)
        val long = value.toLong()
        return if (long in Int.MIN_VALUE..Int.MAX_VALUE) long.toInt() else long
    }
}

/**
 * `players`: the save graph is identical between legacy and modern apart from the join date,
 * which becomes a real BSON date so the world's [DateAdapter] can read it.
 */
object PlayersTransform : Transform {
    override fun apply(doc: Document, report: Report): Document? {
        val username = doc.getString("username")
        if (username.isNullOrEmpty()) {
            report.problem("missing username (dropped)", doc.idKey())
            return null
        }
        // An unreadable join date is not worth losing a character over: drop the field, keep the save.
        convertDate(doc, "dateJoined", username, report, dropDocument = false)
        rename(doc.get("appearence", Document::class.java), "male", "isMale", report)
        rename(doc.get("prayer", Document::class.java), "curses", "isCurses", report)
        doc.get("combatDefinitions", Document::class.java)?.let { combat ->
            rename(combat, "autoRetaliate", "isAutoRetaliate", report)
            rename(combat, "defensiveCasting", "isDefensiveCasting", report)
            rename(combat, "showMiscallaneousSpells", "showMiscSpells", report)
            if (combat.containsKey("spellBook") && !combat.containsKey("spellbook")) {
                val book = LEGACY_SPELLBOOKS.getOrNull((combat.remove("spellBook") as? Number)?.toInt() ?: -1)
                if (book != null) combat["spellbook"] = book
                report.count("combatDefinitions.spellBook index -> spellbook name", 1)
            }
        }
        report.count("item metaData renamed metadata", renameItemMetadata(doc))
        return doc
    }

    private val LEGACY_SPELLBOOKS = listOf("MODERN", "ANCIENT", "LUNAR", "DUNGEONEERING")

    private fun rename(doc: Document?, from: String, to: String, report: Report) {
        if (doc == null || !doc.containsKey(from) || doc.containsKey(to)) return
        doc[to] = doc.remove(from)
        report.count("$from renamed $to", 1)
    }
}

/** `grandexchange`: offers embed items, whose metadata key the modern [org.darkan.core.model.Item] spells differently. */
object OffersTransform : Transform {
    override fun apply(doc: Document, report: Report): Document {
        report.count("item metaData renamed metadata", renameItemMetadata(doc))
        return doc
    }
}

private fun renameItemMetadata(value: Any?): Int = when (value) {
    is Document -> {
        var renamed = 0
        if (value.containsKey("metaData") && value.containsKey("id") && !value.containsKey("metadata")) {
            value["metadata"] = value.remove("metaData")
            renamed++
        }
        renamed + value.values.sumOf { renameItemMetadata(it) }
    }
    is List<*> -> value.sumOf { renameItemMetadata(it) }
    else -> 0
}

/** `logs`: the entry date becomes a BSON date, which also makes the 180 day TTL index effective. */
object LogsTransform : Transform {
    override fun apply(doc: Document, report: Report): Document? {
        val key = doc["hash"]?.toString() ?: doc.idKey()
        // Without a date the TTL index can never expire the entry, so an unreadable one is dropped.
        return if (convertDate(doc, "date", key, report, dropDocument = true)) doc else null
    }
}

/**
 * Replaces a legacy date string in [field] with a BSON date. Returns false when the value could not
 * be read and [dropDocument] asks for the document to be discarded; otherwise the field is unset and
 * the document kept.
 */
private fun convertDate(doc: Document, field: String, key: String, report: Report, dropDocument: Boolean): Boolean {
    if (!doc.containsKey(field)) return true
    val raw = doc[field]
    if (raw == null) {
        doc.remove(field)
        report.count("null $field unset", 1)
        return true
    }
    if (raw is Date) return true
    val date = LegacyDates.toDate(raw)
    if (date == null) {
        report.problem(if (dropDocument) "unparseable $field (document dropped)" else "unparseable $field (field unset)", "$key: $raw")
        doc.remove(field)
        return !dropDocument
    }
    doc[field] = date
    report.count("$field string->date", 1)
    return true
}

internal fun Document.idKey() = this["_id"]?.toString() ?: "<no _id>"
