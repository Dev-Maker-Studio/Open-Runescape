package com.jagex.game.runetek5.config.`object`

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.TypeResult
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText

class ObjectTypeList(
    @JvmField val game: Game,
    @JvmField val language: Language,
    @JvmField val configResource: Js5ResourceProvider?,
    @JvmField val meshResource: Js5ResourceProvider
) {
    /**
     * The preferences set for the player, EG: textures off
     */
    @JvmField
    var preferenceMask: Int = 0

    @JvmField
    var meshCombineCache = arrayOfNulls<Mesh>(4)

    @JvmField
    var locresult = TypeResult(null, null)

    @JvmField
    var cache = LRUCache(256)

    @JvmField
    val modelsByShapeCache = LRUCache(500)

    @JvmField
    val typeresultCache = LRUCache(30)

    @JvmField
    val uidToModelCache = LRUCache(50)

    @JvmField
    var playerIsMember: Boolean = true

    @JvmField
    val defaultActions: Array<String?>

    init {
        if (configResource != null) {
            val lastArchive = configResource.getArchiveCount() - 1
            SharedConfigsType.OBJECTS.archiveFileCapacity()
            configResource.getArchiveFileCount(lastArchive)
        }

        defaultActions = if (game == Game.RUNESCAPE) {
            arrayOf(null, null, null, null, null, LocalizedText.EXAMINE.translate(language))
        } else {
            arrayOfNulls(6)
        }
    }

    fun list(id: Int): ObjectType {
        val loctype: ObjectType = synchronized(cache) {
            cache[id.toLong()] as? ObjectType
        } ?: run {
            val data: ByteArray? = synchronized(configResource!!) {
                configResource.getFile(
                    SharedConfigsType.OBJECTS.archiveId(id),
                    SharedConfigsType.OBJECTS.archiveFileId(id)
                )
            }

            val newLoctype = ObjectType().apply {
                this.id = id
                this.locTypeList = this@ObjectTypeList
                @Suppress("UNCHECKED_CAST")
                this.actions = defaultActions.clone() as Array<String>?

                if (data != null) {
                    decodeType(Packet(data))
                }

                postDecode()

                if (ignoreClipOnAltRoute) {
                    blocksMovement = 0
                    blocksProjectiles = false
                }

                if (!playerIsMember && members) {
                    actions = null
                    quests = null
                }
            }

            synchronized(cache) {
                cache.put(newLoctype, id.toLong())
            }

            newLoctype
        }

        return loctype
    }

    fun resetCaches(prefFlags: Int) {
        preferenceMask = prefFlags
        synchronized(modelsByShapeCache) {
            modelsByShapeCache.reset()
        }
        synchronized(typeresultCache) {
            typeresultCache.reset()
        }
        synchronized(uidToModelCache) {
            uidToModelCache.reset()
        }
    }

    fun resetAll() {
        synchronized(cache) {
            cache.reset()
        }
        synchronized(modelsByShapeCache) {
            modelsByShapeCache.reset()
        }
        synchronized(typeresultCache) {
            typeresultCache.reset()
        }
        synchronized(uidToModelCache) {
            uidToModelCache.reset()
        }
        meshCombineCache = arrayOfNulls(4)
        locresult = TypeResult(null, null)
    }

    fun cacheClean() {
        synchronized(cache) {
            cache.clean(5)
        }
        synchronized(modelsByShapeCache) {
            modelsByShapeCache.clean(5)
        }
        synchronized(typeresultCache) {
            typeresultCache.clean(5)
        }
        synchronized(uidToModelCache) {
            uidToModelCache.clean(5)
        }
    }

    fun clearQueue() {
        synchronized(cache) {
            cache.resize()
        }
        synchronized(modelsByShapeCache) {
            modelsByShapeCache.resize()
        }
        synchronized(typeresultCache) {
            typeresultCache.resize()
        }
        synchronized(uidToModelCache) {
            uidToModelCache.resize()
        }
    }

    fun resetLruCache(capacity: Int) {
        cache = LRUCache(capacity)
    }

    fun setMember(member: Boolean) {
        if (playerIsMember != member) {
            playerIsMember = member
            resetAll()
        }
    }
}
