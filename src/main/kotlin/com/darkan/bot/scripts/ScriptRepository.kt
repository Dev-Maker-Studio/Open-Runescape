package com.darkan.bot.scripts

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand.FastForwardMode
import org.eclipse.jgit.api.MergeResult.MergeStatus
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object ScriptRepository {
    const val DEFAULT_REMOTE = "https://gitlab.com/darkanrs/bot-scripts.git"
    const val BRANCH = "dev"
    private const val TIMEOUT_SECONDS = 30
    private const val REMOTE = Constants.DEFAULT_REMOTE_NAME
    private const val REMOTE_BRANCH = "refs/remotes/$REMOTE/$BRANCH"

    val remote: String get() = System.getProperty("bot.scripts.remote") ?: DEFAULT_REMOTE
    val syncOnStartup: Boolean get() = System.getProperty("bot.scripts.sync")?.toBoolean() ?: true

    @Volatile
    var syncing = false
        private set

    sealed class Outcome(val message: String) {
        val ok: Boolean get() = this !is Failed

        class Initialized(directory: File, remote: String, val setAside: List<String>) : Outcome(
            buildString {
                append("Scripts folder ${directory.absolutePath} now tracks branch '$BRANCH' of $remote.")
                if (setAside.isNotEmpty()) {
                    append("\nExisting files with the same names were kept as:\n")
                    append(setAside.joinToString("\n") { "  $it" })
                }
            }
        )

        class UpToDate(val commit: String) : Outcome("Scripts are up to date ($commit).")
        class Updated(val from: String, val to: String, val changedFiles: Int) :
            Outcome("Scripts updated $from -> $to ($changedFiles file(s) changed).")

        class Skipped(reason: String) : Outcome(reason)
        class Failed(reason: String, val cause: Throwable? = null) : Outcome(reason)
    }

    fun sync(directory: File = getScriptsDirectory()): Outcome {
        syncing = true
        try {
            directory.mkdirs()
            return if (File(directory, Constants.DOT_GIT).exists()) update(directory) else initialize(directory)
        } catch (e: Exception) {
            return Outcome.Failed("Could not update the scripts folder: ${e.message ?: e.javaClass.simpleName}", e)
        } finally {
            syncing = false
        }
    }

    fun syncAsync(directory: File = getScriptsDirectory(), onDone: (Outcome) -> Unit) =
        thread(name = "darkan-scripts-sync", isDaemon = true) { onDone(sync(directory)) }

    private fun initialize(directory: File): Outcome {
        removeClientGeneratedFiles(directory)
        Git.init().setDirectory(directory).setInitialBranch(BRANCH).call().use { git ->
            git.remoteAdd().setName(REMOTE).setUri(URIish(remote)).call()
            return adopt(git, directory)
        }
    }

    private fun update(directory: File): Outcome {
        Git.open(directory).use { git ->
            val repository = git.repository
            if (git.remoteList().call().none { it.name == REMOTE }) {
                git.remoteAdd().setName(REMOTE).setUri(URIish(remote)).call()
            }
            if (repository.resolve(Constants.HEAD) == null) {
                return adopt(git, directory)
            }

            val branch = repository.branch
            if (branch != BRANCH) {
                return Outcome.Skipped(
                    "Scripts folder is on branch '$branch', not '$BRANCH'; leaving it alone. Switch back to '$BRANCH' to receive updates."
                )
            }

            git.fetch().setRemote(REMOTE).setTimeout(TIMEOUT_SECONDS).call()
            val remoteHead = repository.resolve(REMOTE_BRANCH)
                ?: return Outcome.Failed("The scripts repository has no '$BRANCH' branch at $remote.")
            val localHead = repository.resolve(Constants.HEAD)
            if (localHead == remoteHead) {
                return Outcome.UpToDate(short(localHead))
            }
            if (git.status().call().hasUncommittedChanges()) {
                return Outcome.Skipped(
                    "Scripts folder has local changes to tracked files; not updating so nothing is lost. Commit, stash or revert them, then update again."
                )
            }

            val result = git.merge().include(remoteHead).setFastForward(FastForwardMode.FF_ONLY).call()
            return when (result.mergeStatus) {
                MergeStatus.FAST_FORWARD ->
                    Outcome.Updated(short(localHead), short(remoteHead), changedFiles(git, localHead, remoteHead))
                MergeStatus.ALREADY_UP_TO_DATE -> Outcome.UpToDate(short(remoteHead))
                else -> Outcome.Skipped(
                    "Scripts folder has local commits that are not on '$REMOTE/$BRANCH'; not updating. Rebase them onto '$REMOTE/$BRANCH' or reset the branch to receive updates."
                )
            }
        }
    }

    private fun adopt(git: Git, directory: File): Outcome {
        git.fetch().setRemote(REMOTE).setTimeout(TIMEOUT_SECONDS).call()
        val remoteHead = git.repository.resolve(REMOTE_BRANCH)
            ?: return Outcome.Failed("The scripts repository has no '$BRANCH' branch at $remote yet.")
        val setAside = setAsideCollidingFiles(git.repository, remoteHead, directory)
        git.checkout()
            .setName(BRANCH)
            .setCreateBranch(git.repository.exactRef("${Constants.R_HEADS}$BRANCH") == null)
            .setStartPoint("$REMOTE/$BRANCH")
            .setUpstreamMode(SetupUpstreamMode.TRACK)
            .call()
        return Outcome.Initialized(directory, git.remoteList().call().first { it.name == REMOTE }.urIs.first().toString(), setAside)
    }

    private fun setAsideCollidingFiles(repository: Repository, commit: ObjectId, directory: File): List<String> {
        val setAside = mutableListOf<String>()
        RevWalk(repository).use { walk ->
            val tree = walk.parseCommit(commit).tree
            TreeWalk(repository).use { treeWalk ->
                treeWalk.addTree(tree)
                treeWalk.isRecursive = true
                while (treeWalk.next()) {
                    val existing = File(directory, treeWalk.pathString)
                    if (!existing.isFile) continue
                    val aside = availableName(existing)
                    if (existing.renameTo(aside)) {
                        setAside += "${treeWalk.pathString} -> ${aside.name}"
                    }
                }
            }
        }
        return setAside
    }

    private fun availableName(file: File): File {
        var candidate = File(file.parentFile, "${file.name}.local")
        var index = 2
        while (candidate.exists()) {
            candidate = File(file.parentFile, "${file.name}.local$index")
            index++
        }
        return candidate
    }

    private fun removeClientGeneratedFiles(directory: File) {
        val sample = File(directory, "SampleScript.kts")
        if (sample.isFile && sample.useLines { it.firstOrNull() } == "// Sample KotlinScript Bot Script") {
            sample.delete()
        }
        val readme = File(directory, "README.md")
        if (readme.isFile && readme.readText().let { it.startsWith("# Darkan Bot Scripts") && "## Supported Script Types" in it }) {
            readme.delete()
        }
    }

    private fun changedFiles(git: Git, from: ObjectId, to: ObjectId): Int {
        val repository = git.repository
        repository.newObjectReader().use { reader ->
            RevWalk(repository).use { walk ->
                val oldTree = CanonicalTreeParser().apply { reset(reader, walk.parseCommit(from).tree) }
                val newTree = CanonicalTreeParser().apply { reset(reader, walk.parseCommit(to).tree) }
                return git.diff().setOldTree(oldTree).setNewTree(newTree).call().size
            }
        }
    }

    private fun short(id: ObjectId) = id.name.substring(0, 8)
}

fun main(args: Array<String>) {
    val directory = args.firstOrNull()?.let(::File) ?: getScriptsDirectory()
    val outcome = ScriptRepository.sync(directory)
    println(outcome.message)
    (outcome as? ScriptRepository.Outcome.Failed)?.cause?.printStackTrace()
    exitProcess(if (outcome.ok) 0 else 1)
}
