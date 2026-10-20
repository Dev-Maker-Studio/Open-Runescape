package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.js5.request.local.PackedCache
import java.lang.management.ManagementFactory

object LoadTimer {

    private val names = arrayOf(
        "initialize_js5", "load_tips", "create_fonts", "cache_fonts", "prepare_fonts", "create_js5", "load_sprites", "initialize_audio",
        "prepare_native_library_loader", "load_natives", "load_typelists", "cache_static_media", "initialize_worldmap", "discard_unpacked",
        "display_login_window", "initialize_player", "prepare_toolkit", "prepare_player"
    )
    private val begin = System.nanoTime() - ManagementFactory.getRuntimeMXBean().uptime * 1_000_000
    private var last = System.nanoTime()
    private var stage = -1

    @JvmStatic
    fun stage(number: Int) {
        if (number == stage) return
        val now = System.nanoTime()
        if (stage >= 0) println("[load] stage=${names.getOrElse(stage) { stage.toString() }} ms=${(now - last) / 1_000_000} total=${(now - begin) / 1_000_000}")
        if (number == names.size - 1) println("[load] done total=${(now - begin) / 1_000_000} cache=${if (PackedCache.present) "local" else "network"}")
        last = now
        stage = number
    }
}
