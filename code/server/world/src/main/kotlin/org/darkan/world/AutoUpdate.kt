package org.darkan.world

import com.rs.game.World
import com.rs.utils.Ticks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.darkan.core.EnvVars
import org.darkan.core.deploy.ArtifactWatcher
import org.darkan.core.Logger.logInfo

/**
 * Restarts the world onto a new CI build the same way `::update` does: a system-update countdown,
 * every player saved and logged out, then [com.rs.Launcher.shutdown] - and the run script's loop
 * brings the server back up on the build it just downloaded.
 */
object AutoUpdate {
    private val watcher = ArtifactWatcher("world-server", CoroutineScope(Dispatchers.IO + SupervisorJob())) { version ->
        val minutes = EnvVars.autoUpdateCountdownMinutes
        logInfo("Starting the $minutes minute system update countdown for build $version")
        World.sendWorldMessage("<col=ff0000>A new server build is ready. The server will restart in $minutes minutes.", false)
        World.safeShutdown(Ticks.fromMinutes(minutes))
    }

    @JvmStatic
    fun start() = watcher.start()
}
