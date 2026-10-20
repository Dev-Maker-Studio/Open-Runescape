package com.jagex.game.runetek5.js5.request.local

import com.jagex.game.runetek5.js5.Js5MasterIndex
import com.jagex.game.runetek5.js5.request.FileWorker
import com.jagex.game.runetek5.tools.Hashing

class LocalFileWorker private constructor(private val indexId: Int) : FileWorker() {

    private val table: Js5MasterIndex? by lazy {
        PackedCache.read(255, indexId)?.let { Js5MasterIndex(it, Hashing.getCrc32(it, it.size), null) }
    }

    override val isLocal get() = true

    fun loginCrc(): Int = LoginCrcs.of(LoginCrcs.target(), indexId) ?: table!!.headerCrc

    override fun getMasterIndex() = table

    override fun requestArchive(archiveId: Int) = PackedCache.read(indexId, archiveId)

    override fun prefetch(fileId: Int) {}

    override fun getProgress(fileId: Int) = 100

    override fun getTotalArchives() = table?.validArchiveCount ?: 0

    override fun filesReceived() = getTotalArchives()

    override fun currentRequestId() = getTotalArchives()

    companion object {
        private val workers = arrayOfNulls<LocalFileWorker>(256)

        @JvmStatic
        fun of(indexId: Int): LocalFileWorker = synchronized(workers) { workers[indexId] ?: LocalFileWorker(indexId).also { workers[indexId] = it } }

        @JvmStatic
        fun warm() {
            (0 until 37).toList().parallelStream().forEach { of(it).getMasterIndex() }
        }
    }
}
