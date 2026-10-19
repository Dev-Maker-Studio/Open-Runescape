package org.darkan.migration

import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Updates.set
import org.bson.Document
import org.darkan.core.isValidAccountName
import org.darkan.core.model.Rights
import org.darkan.lobby.name.DisplayNameFilter
import java.io.File

private const val ACCOUNTS = "accounts"
private const val USERNAME = "username"
private const val DISPLAY_NAME = "displayName"
private const val NAME_CHANGE_REQUIRED = "nameChangeRequired"
private const val RIGHTS = "rights"

private data class Offender(val username: String, val displayName: String, val verdict: String, val term: String, val flagged: Boolean)

/**
 * The one-off pass over accounts that already hold a name the filter now refuses. Accounts are read
 * as raw documents and flagged with a single field update, so nothing here re-encodes an account
 * and no other field can be touched.
 *
 * Staff are left alone - they hold the reserved names legitimately - and so is an account whose
 * username is still its sign-up email, which the lobby already holds at the name screen.
 */
fun displayNames(target: MongoTarget, approved: File?, apply: Boolean): Boolean {
    val collection = target.database.getCollection(ACCOUNTS)
    var skipped = 0
    val offenders = collection.find().mapNotNull { document ->
        val displayName = document.getString(DISPLAY_NAME) ?: return@mapNotNull null
        val username = document.getString(USERNAME) ?: return@mapNotNull null
        if (!username.isValidAccountName() || isStaff(document)) {
            if (DisplayNameFilter.match(displayName) != null) skipped++
            return@mapNotNull null
        }
        val match = DisplayNameFilter.match(displayName) ?: return@mapNotNull null
        Offender(
            username = username,
            displayName = displayName,
            verdict = match.verdict.name,
            term = match.term,
            flagged = document.getBoolean(NAME_CHANGE_REQUIRED, false),
        )
    }.sortedBy { it.username }

    println("${target.describe()}: ${offenders.size} of ${collection.countDocuments()} accounts hold a refused name")
    if (skipped > 0) println("($skipped skipped: staff, or still held at the name screen)")
    for (offender in offenders) {
        val flagged = if (offender.flagged) "  # already flagged" else ""
        println("${offender.username}  # \"${offender.displayName}\" ${offender.verdict}:${offender.term}$flagged")
    }

    if (!apply) {
        println()
        println("Nothing written. Save this list, delete every line you do not want flagged,")
        println("then re-run with --approved <file> --apply.")
        return true
    }

    val usernames = approved?.let(::readApproved) ?: run {
        System.err.println("error: --apply needs --approved <file>; refusing to flag an unreviewed list")
        return false
    }
    val unknown = usernames - offenders.mapTo(HashSet()) { it.username }
    if (unknown.isNotEmpty()) println("Warning: ${unknown.size} approved usernames no longer hold a refused name: ${unknown.take(10)}")

    val result = collection.updateMany(`in`(USERNAME, usernames), set(NAME_CHANGE_REQUIRED, true))
    println("Flagged ${result.modifiedCount} of ${usernames.size} approved accounts.")
    return true
}

private fun isStaff(document: Document): Boolean {
    val rights = document[RIGHTS]
    val name = (rights as? String) ?: (rights as? Number)?.let { Rights.entries.getOrNull(it.toInt())?.name }
    return (name?.let { runCatching { Rights.valueOf(it) }.getOrNull() } ?: Rights.PLAYER) != Rights.PLAYER
}

private fun readApproved(file: File): Set<String> = file.readLines()
    .map { it.substringBefore('#').trim() }
    .filter { it.isNotEmpty() }
    .toSet()
