package com.jagex.game.runetek5.config.item

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteDrawQueue
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.SoftwareItemRender
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.config.param.ParamTypeList

class ItemTypeList(
    @JvmField val game: Game,
    @JvmField val language: Language,
    @JvmField val paramTypeList: ParamTypeList,
    @JvmField val objResource: Js5ResourceProvider,
    @JvmField val meshResource: Js5ResourceProvider
) {

    @JvmField val spriteDrawQueue: SpriteDrawQueue = SpriteDrawQueue()
    @JvmField val totalItemCount: Int
    @JvmField var isMember: Boolean = false
    @JvmField var preferenceFlags: Int = 0
    @JvmField val typeCache: LRUCache = LRUCache(64)
    @JvmField val modelCache: LRUCache = LRUCache(50)
    @JvmField val softwareItemRenderer: SoftwareItemRender = SoftwareItemRender()
    @JvmField val defaultGroundActions: Array<String?>
    @JvmField val defaultInventoryActions: Array<String?>

    init {
        this.isMember = true
        if (objResource != null) {
            val lastArchiveId = objResource.getArchiveCount() - 1
            this.totalItemCount = lastArchiveId * SharedConfigsType.ITEMS.archiveFileCapacity() + this.objResource.getArchiveFileCount(lastArchiveId)
        } else {
            this.totalItemCount = 0
        }

        if (this.game == Game.RUNESCAPE) {
            this.defaultGroundActions = arrayOf(null, null, LocalizedText.TAKE.translate(this.language), null, null, LocalizedText.EXAMINE.translate(this.language))
        } else {
            this.defaultGroundActions = arrayOf(null, null, LocalizedText.TAKE.translate(this.language), null, null, null)
        }

        this.defaultInventoryActions = arrayOf(null, null, null, null, LocalizedText.DROP.translate(this.language))
    }

    fun list(itemId: Int): ItemType {
        var objType: ItemType?
        synchronized(this.typeCache) {
            objType = this.typeCache.get(itemId.toLong()) as ItemType?
        }
        if (objType != null) {
            return objType!!
        }
        val data: ByteArray?
        synchronized(this.objResource) {
            data = this.objResource.getFile(SharedConfigsType.ITEMS.archiveId(itemId), SharedConfigsType.ITEMS.archiveFileId(itemId))
        }
        objType = ItemType()
        objType!!.typeList = this
        objType!!.id = itemId
        objType!!.groundActions = this.defaultGroundActions.clone()
        objType!!.inventoryActions = this.defaultInventoryActions.clone()
        if (data != null) {
            objType!!.decodeType(Packet(data))
        }
        objType!!.postDecode()
        if (objType!!.certTemplateId != -1) {
            objType!!.generateNote(list(objType!!.certTemplateId), list(objType!!.certId))
        }
        if (objType!!.lendTemplateId != -1) {
            objType!!.generateLend(list(objType!!.lendTemplateId), list(objType!!.lendId))
        }
        if (objType!!.bindTemplateId != -1) {
            objType!!.generateBind(list(objType!!.bindTemplateId), list(objType!!.bindId))
        }
        if (!this.isMember && objType!!.membersOnly) {
            objType!!.teamId = 0
            objType!!.groundActions = this.defaultGroundActions
            objType!!.inventoryActions = this.defaultInventoryActions
            objType!!.tradeable = false
            objType!!.quests = null
            if (objType!!.parameters != null) {
                var validCs2 = false
                var cs2Node = objType!!.parameters!!.first()
                while (cs2Node != null) {
                    val param = this.paramTypeList.list(cs2Node.pointer.toInt())
                    if (param.autoDisable) {
                        cs2Node.unlink()
                    } else {
                        validCs2 = true
                    }
                    cs2Node = objType!!.parameters!!.next()
                }
                if (!validCs2) {
                    objType!!.parameters = null
                }
            }
        }
        synchronized(this.typeCache) {
            this.typeCache.put(objType, itemId.toLong())
            return objType!!
        }
    }

    fun getSprite(mainToolkit: AbstractToolkit, itemToolkit: AbstractToolkit, itemId: Int, itemStacksize: Int, outlineSize: Int, shadowColor: Int, zoomedIn: Boolean, useMainToolkit: Boolean, renderStack: Component.StackMode, fontRenderer: AbstractFontRenderer, playerAppearance: PlayerAppearance?): NativeSprite? {
        if (!useMainToolkit) {
            val sprite = createNativeSprite(itemToolkit, itemId, itemStacksize, outlineSize, shadowColor, renderStack, playerAppearance)
            if (sprite != null) {
                return sprite
            }
        }
        var objType = list(itemId)
        if (itemStacksize > 1 && objType.stackIds != null) {
            var stackId = -1
            for (index in 0..9) {
                if (itemStacksize >= objType.stackTriggerAmount[index] && objType.stackTriggerAmount[index] != 0) {
                    stackId = objType.stackIds[index]
                }
            }
            if (stackId != -1) {
                objType = list(stackId)
            }
        }
        val sprites = objType.getSpritePixels(mainToolkit, itemToolkit, itemStacksize, outlineSize, shadowColor, zoomedIn, renderStack, fontRenderer, playerAppearance)
        if (sprites == null) {
            return null
        }
        val nativeSprite: NativeSprite
        if (useMainToolkit) {
            nativeSprite = mainToolkit.createNativeSprite(sprites, 36, 36, 32)
        } else {
            nativeSprite = itemToolkit.createNativeSprite(sprites, 36, 36, 32)
        }

        // this block gets hit on every display mode full & fixed screen
        if (!useMainToolkit) {
            val itemRenderer = SoftwareItemRender()
            itemRenderer.rendererId = itemToolkit.renderId
            itemRenderer.itemId = itemId
            itemRenderer.itemAmount = itemStacksize
            itemRenderer.outlineSize = outlineSize
            itemRenderer.shadowColor = shadowColor
            itemRenderer.renderStack = renderStack
            itemRenderer.hasPlayerAppearance = playerAppearance != null
            this.spriteDrawQueue.insert(nativeSprite, itemRenderer)
        }
        return nativeSprite
    }

    fun setMembersOnly(isMember: Boolean) {
        if (this.isMember != isMember) {
            this.isMember = isMember
            resetCaches()
        }
    }

    fun resetCaches() {
        synchronized(this.typeCache) {
            this.typeCache.reset()
        }
        synchronized(this.modelCache) {
            this.modelCache.reset()
        }
        synchronized(this.spriteDrawQueue) {
            this.spriteDrawQueue.reset()
        }
    }

    fun resetRecolorCache() {
        synchronized(this.spriteDrawQueue) {
            this.spriteDrawQueue.reset()
        }
    }

    fun resetModelCache() {
        synchronized(this.modelCache) {
            this.modelCache.reset()
        }
    }

    fun resetCache(preferenceFlags: Int) {
        this.preferenceFlags = preferenceFlags
        synchronized(this.modelCache) {
            this.modelCache.reset()
        }
    }

    fun processCycle() {
        synchronized(this.typeCache) {
            this.typeCache.clean(5)
        }
        synchronized(this.modelCache) {
            this.modelCache.clean(5)
        }
        synchronized(this.spriteDrawQueue) {
            this.spriteDrawQueue.processCycle()
        }
    }

    fun createNativeSprite(toolkit: AbstractToolkit, itemId: Int, itemAmount: Int, outlineSize: Int, shadowColor: Int, renderStack: Component.StackMode, playerAppearance: PlayerAppearance?): NativeSprite? {
        this.softwareItemRenderer.rendererId = toolkit.renderId
        this.softwareItemRenderer.itemId = itemId
        this.softwareItemRenderer.itemAmount = itemAmount
        this.softwareItemRenderer.outlineSize = outlineSize
        this.softwareItemRenderer.shadowColor = shadowColor
        this.softwareItemRenderer.renderStack = renderStack
        this.softwareItemRenderer.hasPlayerAppearance = playerAppearance != null
        return this.spriteDrawQueue.getNativeSprite(this.softwareItemRenderer)
    }

    fun clearCacheQueues() {
        synchronized(this.typeCache) {
            this.typeCache.resize()
        }
        synchronized(this.modelCache) {
            this.modelCache.resize()
        }
        synchronized(this.spriteDrawQueue) {
            this.spriteDrawQueue.remove()
        }
    }
}
