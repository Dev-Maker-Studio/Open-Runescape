package org.darkan.world

import com.rs.Launcher
import java.util.TimeZone

/**
 * World server entry point.
 * Delegates to the legacy Launcher for now.
 */
fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    Launcher.main(args)
}
