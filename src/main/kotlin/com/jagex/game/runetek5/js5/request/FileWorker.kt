package com.jagex.game.runetek5.js5.request

import com.jagex.game.runetek5.js5.Js5MasterIndex

abstract class FileWorker {
    abstract fun getMasterIndex(): Js5MasterIndex

    abstract fun requestArchive(archiveId: Int): ByteArray?

    abstract fun prefetch(fileId: Int)

    abstract fun getProgress(fileId: Int): Int
}
