package com.jagex.game.runetek5.js5.request

import com.jagex.game.runetek5.js5.Js5MasterIndex

abstract class FileWorker {
    abstract fun getMasterIndex(): Js5MasterIndex?

    abstract fun requestArchive(archiveId: Int): ByteArray?

    abstract fun prefetch(fileId: Int)

    abstract fun getProgress(fileId: Int): Int

    open val isLocal: Boolean get() = false

    open fun getTotalArchives(): Int = 0

    open fun filesReceived(): Int = 0

    open fun currentRequestId(): Int = 0

    open fun getReferenceTableCompletion(): Int = 100
}
