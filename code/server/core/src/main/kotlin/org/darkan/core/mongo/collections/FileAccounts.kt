package org.darkan.core.mongo.collections

import org.darkan.core.JsonFileManager
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.model.Account
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

class FileAccounts(private val dir: Path) : AccountStore {
    init {
        Files.createDirectories(dir)
    }

    private fun load(file: Path): Account? =
        runCatching { JsonFileManager.fromJSONString(file.readText(), Account::class.java) }.getOrNull()

    private fun all(): List<Account> =
        dir.listDirectoryEntries("*.json").mapNotNull(::load)

    override suspend fun find(usernameOrEmail: String): Account? {
        val key = usernameOrEmail.formatPlayerNameForProtocol()
        return all().firstOrNull { it.username == key || it.email == key }
    }

    override suspend fun findByDisplayName(displayName: String): Account? {
        val key = displayName.formatPlayerNameForProtocol()
        return all().firstOrNull { it.username == key || it.displayName == displayName }
    }

    override suspend fun findByUsernames(usernames: Collection<String>): List<Account> {
        val keys = usernames.map { it.formatPlayerNameForProtocol() }.toSet()
        return all().filter { it.username in keys }
    }

    override suspend fun save(account: Account) = write(account)

    @Synchronized
    private fun write(account: Account) {
        val target = dir.resolve(account.username.replace('/', '_') + ".json")
        val tmp = dir.resolve(target.name + ".tmp")
        Files.writeString(tmp, JsonFileManager.toJson(account))
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    override suspend fun rename(previousUsername: String, account: Account) {
        Files.deleteIfExists(dir.resolve(previousUsername.replace('/', '_') + ".json"))
        save(account)
    }

    override suspend fun findAll(): List<Account> = all()
}
