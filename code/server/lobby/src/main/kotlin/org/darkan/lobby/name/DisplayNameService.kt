package org.darkan.lobby.name

import org.darkan.core.formatPlayerNameForDisplay
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.isBotName
import org.darkan.core.isValidAccountName
import org.darkan.core.model.Account
import org.darkan.core.mongo.collections.Accounts

enum class DisplayNameRefusal(val message: String) {
    INVALID("That name is not a valid display name."),
    TAKEN("That name is already taken."),
    NO_CHANGES_LEFT("You have no display name changes available."),
    UNKNOWN_ACCOUNT("No such account."),
}

/**
 * The one place a display name is judged and written, so the lobby screen, the website and a
 * moderator all apply the same rules. A profane or reserved name is refused as [TAKEN]: telling a
 * player which word tripped the filter only teaches them how to get around it.
 */
object DisplayNameService {

    suspend fun available(displayName: String): DisplayNameRefusal? {
        val formatted = displayName.formatPlayerNameForProtocol()
        if (!formatted.isValidAccountName()) return DisplayNameRefusal.INVALID
        if (formatted.isBotName()) return DisplayNameRefusal.TAKEN
        val name = formatted.formatPlayerNameForDisplay()
        if (!DisplayNameFilter.isAcceptable(name)) return DisplayNameRefusal.TAKEN
        return null
    }

    /** Whether [displayName] is free, ignoring the name [self] already holds. */
    suspend fun claimable(displayName: String, self: Account? = null): DisplayNameRefusal? {
        available(displayName)?.let { return it }
        val holder = Accounts.findByDisplayName(displayName.formatPlayerNameForProtocol().formatPlayerNameForDisplay())
        if (holder != null && holder.username != self?.username) return DisplayNameRefusal.TAKEN
        return null
    }

    suspend fun change(username: String, displayName: String): DisplayNameRefusal? {
        val account = Accounts.find(username) ?: return DisplayNameRefusal.UNKNOWN_ACCOUNT
        val forced = account.nameChangeRequired
        if (!forced && account.displayNameChanges <= 0) return DisplayNameRefusal.NO_CHANGES_LEFT
        claimable(displayName, account)?.let { return it }
        account.renameTo(displayName.formatPlayerNameForProtocol().formatPlayerNameForDisplay(), forced)
        Accounts.save(account)
        return null
    }

    suspend fun grantChanges(username: String, changes: Int): Account? {
        val account = Accounts.find(username) ?: return null
        account.displayNameChanges += changes
        Accounts.save(account)
        return account
    }

    suspend fun requireNameChange(username: String, required: Boolean): Account? {
        val account = Accounts.find(username) ?: return null
        account.nameChangeRequired = required
        Accounts.save(account)
        return account
    }
}
