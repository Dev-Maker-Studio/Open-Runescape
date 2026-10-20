package com.jagex.game.runetek5.config.rsinterface.itemcontainer

import com.darkan.bot.Bot
import com.darkan.bot.utils.ContainerWrapper
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask.RECOLOR
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Hashing

class ItemContainer : Node<ItemContainer>() {

    @JvmField
    var itemIds: IntArray = intArrayOf(-1)

    @JvmField
    var amounts: IntArray = IntArray(1)

    @JvmField
    var tooltips: Array<String?> = arrayOfNulls(1)

    fun getAnimatedBodyModel(
        toolkit: AbstractToolkit,
        functionMask: Int,
        sequence: Sequence?,
        bastypeId: Int,
        female: Boolean,
        playerAppearance: PlayerAppearance?
    ): Model? {
        var animateFunctionMask = functionMask
        var basType: BasType? = null
        if (bastypeId != -1) {
            basType = TypeLists.BAS!!.list(bastypeId)
        }

        var items = this.itemIds
        if (basType != null && basType.objVisibility != null) {
            items = IntArray(basType.objVisibility!!.size)

            for (slot in basType.objVisibility!!.indices) {
                val visibleSlot = basType.objVisibility!![slot]
                items[slot] = if (visibleSlot >= 0 && visibleSlot < this.itemIds.size) {
                    this.itemIds[visibleSlot]
                } else {
                    -1
                }
            }
        }

        if (sequence != null) {
            animateFunctionMask = functionMask or sequence.getFunctionMask()
        }

        val hash = calculateHash(items, bastypeId, playerAppearance?.recolorDstIndices, female)
        var model = CONTAINER_ITEM_CACHE.get(hash) as Model?

        if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), animateFunctionMask) != 0) {
            if (model != null) {
                animateFunctionMask = toolkit.mergeFunctionMasks(animateFunctionMask, model.getFunctionMask())
            }

            val combineFunctionMask: Int
            var modelsMissing = false

            for (index in items.indices) {
                if (items[index] != -1 && !TypeLists.OBJ!!.list(items[index]).isBodyCached(female, null)) {
                    modelsMissing = true
                }
            }

            if (modelsMissing) {
                return null
            }

            val meshes = arrayOfNulls<Mesh>(items.size)

            for (index in items.indices) {
                if (items[index] != -1) {
                    meshes[index] = TypeLists.OBJ!!.list(items[index]).getBodyMesh(female, null)
                }
            }

            if (basType != null && basType.objVerticeTransformations != null) {
                val transformations = basType.objVerticeTransformations!!
                for (index in transformations.indices) {
                    if (transformations[index] != null && meshes[index] != null) {
                        val translateX = transformations[index]!![0]
                        val translateY = transformations[index]!![1]
                        val translateZ = transformations[index]!![2]
                        val rotateX = transformations[index]!![3]
                        val rotateY = transformations[index]!![4]
                        val rotateZ = transformations[index]!![5]
                        if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
                            meshes[index]!!.rotateWithoutNormals(rotateX, rotateY, rotateZ)
                        }

                        if (translateX != 0 || translateY != 0 || translateZ != 0) {
                            meshes[index]!!.translate(translateX, translateY, translateZ)
                        }
                    }
                }
            }

            val combinedMesh = Mesh(meshes, meshes.size)
            combineFunctionMask = if (playerAppearance != null) {
                animateFunctionMask or RECOLOR
            } else {
                animateFunctionMask
            }

            model = toolkit.createModel(combinedMesh, combineFunctionMask, ItemContainerTypeList.preferenceMask, 65, 857)
            if (playerAppearance != null) {
                for (x in 0..9) {
                    for (y in PlayerAppearance.recolorPaletteSrc!![x].indices) {
                        if (playerAppearance.recolorDstIndices!![x] < PlayerAppearance.recolorPaletteDst!![x][y].size) {
                            model!!.recolor(
                                PlayerAppearance.recolorPaletteSrc!![x][y],
                                PlayerAppearance.recolorPaletteDst!![x][y][playerAppearance.recolorDstIndices!![x]]
                            )
                        }
                    }
                }
            }

            model!!.applyShading(animateFunctionMask)
            CONTAINER_ITEM_CACHE.put(model, hash)
        }

        if (sequence == null) {
            return model
        }
        val copiedModel = model!!.copy(1.toByte(), animateFunctionMask, true)
        sequence.animateRotatedModel(copiedModel!!, 0)
        return copiedModel
    }

    fun calculateHash(items: IntArray, bastypeId: Int, recolorDstIndices: IntArray?, female: Boolean): Long {
        val hashtable = Hashing.CRC64_TABLE
        var hash = -1L
        hash = hash ushr 8 xor hashtable[(hash xor (bastypeId shr 8).toLong() and 0xffL).toInt()]
        hash = hash ushr 8 xor hashtable[(hash xor bastypeId.toLong() and 0xffL).toInt()]

        for (index in items.indices) {
            hash = hash ushr 8 xor hashtable[(hash xor (items[index] shr 24).toLong() and 0xffL).toInt()]
            hash = hash ushr 8 xor hashtable[(hash xor (items[index] shr 16).toLong() and 0xffL).toInt()]
            hash = hash ushr 8 xor hashtable[(hash xor (items[index] shr 8).toLong() and 0xffL).toInt()]
            hash = hash ushr 8 xor hashtable[(hash xor items[index].toLong() and 0xffL).toInt()]
        }

        if (recolorDstIndices != null) {
            for (index in 0..4) {
                hash = hash ushr 8 xor hashtable[(hash xor recolorDstIndices[index].toLong() and 0xffL).toInt()]
            }
        }

        hash = hash ushr 8 xor hashtable[(hash xor (if (female) 1 else 0).toLong() and 0xffL).toInt()]
        return hash
    }

    companion object {
        @JvmField
        val CONTAINER_MAP: IterableNodeMap<ItemContainer> = IterableNodeMap(32)

        @JvmField
        val CONTAINER_ITEM_CACHE = LRUCache(11)

        @JvmStatic
        fun getAmountAtSlot(containerId: Int, slot: Int, negativeKey: Boolean): Int {
            val container = getContainer(containerId, negativeKey)
            return if (container == null) 0 else if (slot >= 0 && slot < container.amounts.size) container.amounts[slot] else 0
        }

        @JvmStatic
        fun getItemIdAtSlot(containerId: Int, slot: Int, negativeKey: Boolean): Int {
            val container = getContainer(containerId, negativeKey)
            return if (container == null) -1 else if (slot >= 0 && slot < container.itemIds.size) container.itemIds[slot] else -1
        }

        @JvmStatic
        fun getTooltipAtSlot(containerId: Int, slot: Int, negativeKey: Boolean): String? {
            val container = getContainer(containerId, negativeKey) ?: return null
            return if (slot >= 0 && slot < container.tooltips.size) container.tooltips[slot] else null
        }

        @JvmStatic
        fun findTooltipSlot(containerId: Int, preferredSlot: Int, itemId: Int, negativeKey: Boolean): Int {
            val container = getContainer(containerId, negativeKey) ?: return -1
            if (preferredSlot >= 0 && preferredSlot < container.itemIds.size && container.itemIds[preferredSlot] == itemId) {
                return preferredSlot
            }
            return container.itemIds.indexOf(itemId)
        }

        @JvmStatic
        fun setTooltipInContainer(containerId: Int, slot: Int, tooltip: String?, negativeKey: Boolean) {
            val container = getContainer(containerId, negativeKey) ?: return
            if (slot >= 0 && slot < container.tooltips.size) {
                container.tooltips[slot] = tooltip
            }
        }

        @JvmStatic
        fun getContainer(containerId: Int, negativeKey: Boolean): ItemContainer? {
            val key = (containerId.toLong() or if (negativeKey) Int.MIN_VALUE.toLong() else 0)
            return CONTAINER_MAP.get(key) as ItemContainer?
        }

        @JvmStatic
        fun deleteItemContainer(containerId: Int, negativeKey: Boolean) {
            val container = getContainer(containerId, negativeKey)
            container?.unlink()
        }

        @JvmStatic
        fun setItemInContainer(containerId: Int, slot: Int, itemId: Int, amount: Int, negativeKey: Boolean) {
            val key = (containerId.toLong() or if (negativeKey) Int.MIN_VALUE.toLong() else 0)
            var container = CONTAINER_MAP.get(key) as ItemContainer?
            if (container == null) {
                container = ItemContainer()
                CONTAINER_MAP.put(container, key)
            }

            if (container.itemIds.size <= slot) {
                val itemIds = IntArray(slot + 1)
                val itemAmounts = IntArray(slot + 1)

                for (index in container.itemIds.indices) {
                    itemIds[index] = container.itemIds[index]
                    itemAmounts[index] = container.amounts[index]
                }

                for (index in container.itemIds.size until slot) {
                    itemIds[index] = -1
                    itemAmounts[index] = 0
                }

                container.itemIds = itemIds
                container.amounts = itemAmounts
                container.tooltips = container.tooltips.copyOf(slot + 1)
            }

            container.itemIds[slot] = itemId
            container.amounts[slot] = amount
            if (key == 93L) {
                Bot.inventory = ContainerWrapper(container)
            } else if (key == 95L) {
                Bot.bank = ContainerWrapper(container)
            }
        }

        @JvmStatic
        fun resetContainerMap() {
            CONTAINER_MAP.clear()
        }

        @JvmStatic
        fun resetContainer(containerId: Int, negativeKey: Boolean) {
            val container = getContainer(containerId, negativeKey)
            if (container != null) {
                for (index in container.itemIds.indices) {
                    container.itemIds[index] = -1
                    container.amounts[index] = 0
                }
                container.tooltips.fill(null)
            }
        }

        @JvmStatic
        fun getContainerTotal(containerId: Int, itemId: Int, negativeKey: Boolean): Int {
            val container = getContainer(containerId, negativeKey) ?: return 0
            if (itemId == -1) {
                return 0
            }
            var total = 0
            for (index in container.amounts.indices) {
                if (container.itemIds[index] == itemId) {
                    total += container.amounts[index]
                }
            }
            return total
        }

        @JvmStatic
        fun getParamBasedContainerTotal(containerId: Int, paramId: Int, stackable: Boolean): Int {
            val container = getContainer(containerId, false) ?: return 0
            var total = 0
            for (index in container.itemIds.indices) {
                if (container.itemIds[index] >= 0 && container.itemIds[index] < TypeLists.OBJ!!.totalItemCount) {
                    val objType = TypeLists.OBJ!!.list(container.itemIds[index])
                    val param = objType.getCs2Integer(paramId, TypeLists.PARAMS!!.list(paramId).defaultInt)
                    total += if (stackable) {
                        param * container.amounts[index]
                    } else {
                        param
                    }
                }
            }
            return total
        }

        @JvmStatic
        fun getFreeSlots(containerId: Int): Int {
            val container = getContainer(containerId, false)
            if (container == null) {
                return TypeLists.CONTAINER!!.list(containerId).maxItemSize
            }
            var freeSlots = 0
            for (index in container.itemIds.indices) {
                if (container.itemIds[index] == -1) {
                    freeSlots++
                }
            }
            freeSlots += TypeLists.CONTAINER!!.list(containerId).maxItemSize - container.itemIds.size
            return freeSlots
        }

        @JvmStatic
        fun resetCache(preferenceMask: Int) {
            ItemContainerTypeList.preferenceMask = preferenceMask
            CONTAINER_ITEM_CACHE.reset()
        }

        @JvmStatic
        fun resetContainerItemCache() {
            CONTAINER_ITEM_CACHE.reset()
        }
    }
}
