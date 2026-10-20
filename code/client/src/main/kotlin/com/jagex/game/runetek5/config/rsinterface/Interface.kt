package com.jagex.game.runetek5.config.rsinterface

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.HashTableIterator
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.MiniMenuEntry
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.trigger.worldmap.WorldMapTrigger
import com.jagex.game.runetek5.utils.ArrayUtils

class Interface(
    @JvmField var preventInteraction: Boolean,
    @JvmField var components: Array<Component?>
) {
    var subComponents: Array<Component?>? = null

    fun getComponent(componentId: Int): Component? {
        if (componentId ushr 16 != components[0]!!.idHash ushr 16) {
            throw IllegalArgumentException()
        }
        return components[componentId and 0xffff]
    }

    fun getComponents(): Array<Component?> {
        return subComponents ?: components
    }

    fun getOrCreateMutableComponents(): Array<Component?> {
        if (subComponents == null) {
            val componentCount = components.size
            subComponents = Array(componentCount) { null }
            System.arraycopy(components, 0, subComponents!!, 0, components.size)
        }
        return subComponents!!
    }

    companion object {
        @JvmField
        var INTERFACES: Array<Interface?>? = null

        @JvmField
        var INTERFACES_LOADED: BooleanArray? = null

        @JvmStatic
        var interfaceResource: Js5ResourceProvider? = null

        @JvmField
        var worldMapDragStartX: Int = 0

        @JvmField
        var preferenceMask: Int = 0

        @JvmStatic
        fun initialize(
            interfacesResource: Js5ResourceProvider,
            meshResource: Js5ResourceProvider,
            spriteResource: Js5ResourceProvider
        ) {
            interfaceResource = interfacesResource
            Resource.MESH = meshResource
            StaticMethods.SPRITES_INDEX = spriteResource
            INTERFACES = Array(interfaceResource!!.getArchiveCount()) { null }
            INTERFACES_LOADED = BooleanArray(interfaceResource!!.getArchiveCount())
        }

        @JvmStatic
        fun initializeTables() {
            INTERFACES = Array(interfaceResource!!.getArchiveCount()) { null }
            INTERFACES_LOADED = BooleanArray(interfaceResource!!.getArchiveCount())
        }

        @JvmStatic
        fun interfaceLoaded(interfaceId: Int, xteas: IntArray?): Boolean {
            if (interfaceId !in INTERFACES_LOADED!!.indices) {
                return false
            }
            if (INTERFACES_LOADED!![interfaceId]) {
                return true
            }
            INTERFACES!![interfaceId] = decodeComponents(interfaceId, xteas, INTERFACES!![interfaceId], false)
            if (INTERFACES!![interfaceId] == null) {
                return false
            }
            INTERFACES_LOADED!![interfaceId] = true
            return true
        }

        @JvmStatic
        fun getInterface(interfaceAndComponent: Int): Interface? {
            return INTERFACES!![interfaceAndComponent shr 16]
        }

        @JvmStatic
        fun unloadInterface(interfaceId: Int) {
            if (interfaceId !in INTERFACES_LOADED!!.indices) {
                return
            }
            INTERFACES_LOADED!![interfaceId] = false
            clearComponents(interfaceId)
        }

        @JvmStatic
        @JvmName("open_sub")
        fun openSub(interfaceId: Int, scriptMode: Int) {
            if (interfaceLoaded(interfaceId, null)) {
                runInterfaceScripts(INTERFACES!![interfaceId]!!.components, scriptMode)
            }
        }

        @JvmStatic
        fun resetComponentSequences(interfaceId: Int, xteas: IntArray?) {
            if (interfaceLoaded(interfaceId, xteas)) {
                val components = INTERFACES!![interfaceId]!!.components
                for (index in components.indices) {
                    val component = components[index]
                    if (component != null && component.animatingSequence != null) {
                        component.animatingSequence!!.reset()
                    }
                }
            }
        }

        @JvmStatic
        fun clearComponents(interfaceId: Int) {
            if (interfaceId in INTERFACES_LOADED!!.indices && !INTERFACES_LOADED!![interfaceId]) {
                interfaceResource!!.clearArchiveFiles(interfaceId)
                INTERFACES!![interfaceId] = null
            }
        }

        @JvmStatic
        fun closeChildInterface(parentInterface: SubInterface, unload: Boolean, suppressScripts: Boolean) {
            val parentInterfaceId = parentInterface.subInterfaceId
            val interfaceHash = parentInterface.pointer.toInt()
            parentInterface.unlink()
            if (unload) {
                unloadInterface(parentInterfaceId)
            }
            removeComponentSettings(parentInterfaceId)
            val component = Component.fromHash(interfaceHash)
            if (component != null) {
                Component.redrawComponent(component)
            }
            MiniMenu.removeMatchingEntries()
            if (!suppressScripts && Client.BASE_WINDOW_ID != -1) {
                openSub(Client.BASE_WINDOW_ID, 1)
            }
            val openInterfaceIt = HashTableIterator<SubInterface>(Client.OPEN_INTERFACES!!)
            var child = openInterfaceIt.first() as SubInterface?
            while (child != null) {
                if (!child.linked()) {
                    child = openInterfaceIt.first() as SubInterface?
                    if (child == null) {
                        break
                    }
                }
                if (child.overlay == 3) {
                    val uid = child.pointer.toInt()
                    if (uid ushr 16 == parentInterfaceId) {
                        closeChildInterface(child, true, suppressScripts)
                    }
                }
                child = openInterfaceIt.next() as SubInterface?
            }
        }

        @JvmStatic
        fun openSub(
            parentId: Int,
            subInterface: SubInterface,
            xteas: IntArray?,
            suppressScripts: Boolean
        ): SubInterface {
            val currentSubInterface = Client.OPEN_INTERFACES!!.get(parentId.toLong()) as SubInterface?
            if (currentSubInterface != null) {
                closeChildInterface(
                    currentSubInterface,
                    currentSubInterface.subInterfaceId != subInterface.subInterfaceId,
                    suppressScripts
                )
            }
            Client.OPEN_INTERFACES!!.put(subInterface, parentId.toLong())
            resetComponentSequences(subInterface.subInterfaceId, xteas)
            val parentComponent = Component.fromHash(parentId)
            if (parentComponent != null) {
                Component.redrawComponent(parentComponent)
            }
            if (Client.activeDialogueContinue != null) {
                Component.redrawComponent(Client.activeDialogueContinue!!)
                Client.activeDialogueContinue = null
            }
            MiniMenu.removeMatchingEntries()
            if (parentComponent != null) {
                Component.updateEntityLayout(INTERFACES!![parentComponent.idHash ushr 16]!!, parentComponent, !suppressScripts)
            }
            if (!suppressScripts) {
                Component.runScriptsIfLoaded(subInterface.subInterfaceId, xteas)
            }
            if (!suppressScripts && Client.BASE_WINDOW_ID != -1) {
                openSub(Client.BASE_WINDOW_ID, 1)
            }
            return subInterface
        }

        @JvmStatic
        fun decodeComponents(
            interfaceId: Int,
            xteas: IntArray?,
            rsInterface: Interface?,
            preventInteraction: Boolean
        ): Interface? {
            if (!interfaceResource!!.loadArchive(interfaceId)) {
                return null
            }
            val componentCount = interfaceResource!!.getArchiveFileCount(interfaceId)
            var interfaceComponents: Array<Component?> = if (componentCount == 0) {
                Array(0) { null }
            } else if (rsInterface == null) {
                Array(componentCount) { null }
            } else {
                rsInterface.components
            }
            var mutableRsInterface = rsInterface
            if (mutableRsInterface == null) {
                mutableRsInterface = Interface(preventInteraction, interfaceComponents)
            } else {
                mutableRsInterface.components = interfaceComponents
                mutableRsInterface.preventInteraction = preventInteraction
            }
            for (componentIndex in 0 until componentCount) {
                if (mutableRsInterface.components[componentIndex] == null) {
                    val interfaceData = interfaceResource!!.getFile(interfaceId, componentIndex, xteas)
                    if (interfaceData != null) {
                        val component = Component()
                        mutableRsInterface.components[componentIndex] = component
                        component.idHash = componentIndex + (interfaceId shl 16)
                        component.decode(Packet(interfaceData))
                    }
                }
            }
            return mutableRsInterface
        }

        @JvmStatic
        @JvmName("open_sub")
        fun openSub(
            subInterface: SubInterface?,
            subInterfaceId: Int,
            startX: Int,
            startY: Int,
            endX: Int,
            endY: Int,
            offsetX: Int,
            offsetY: Int,
            mouseX: Int,
            mouseY: Int
        ) {
            if (interfaceLoaded(subInterfaceId, null)) {
                if (subInterface != null && !subInterface.executeTrigger()) {
                    closeChildInterface(subInterface, true, false)
                } else {
                    val loadedInterface = INTERFACES!![subInterfaceId]!!
                    InterfaceRenderer.processInterfaceCycle(
                        loadedInterface,
                        loadedInterface.getComponents(),
                        -1,
                        startX,
                        startY,
                        endX,
                        endY,
                        offsetX,
                        offsetY,
                        mouseX,
                        mouseY
                    )
                }
            }
        }

        @JvmStatic
        fun removeComponentSettings(interfaceId: Int) {
            var node = Client.ICOMPONENT_SETTINGS_SLOTS.first()
            while (node != null) {
                if (interfaceId == (node.pointer shr 48 and 0xffffL).toInt()) {
                    node.unlink()
                }
                node = Client.ICOMPONENT_SETTINGS_SLOTS.next()
            }
        }

        @JvmStatic
        fun hasActiveMenuEntry(interfaceAndComponent: Int, slotId: Int): Boolean {
            if (!MiniMenu.isOpen) {
                return false
            }
            val interId = interfaceAndComponent shr 16
            val compId = interfaceAndComponent and 0xffff
            if (INTERFACES!![interId] != null && INTERFACES!![interId]!!.components[compId] != null) {
                val component = INTERFACES!![interId]!!.components[compId]!!
                if (slotId == -1 && component.type == ComponentType.CONTAINER) {
                    var menuEntry = MiniMenu.entryList.getFirst() as MiniMenuEntry?
                    while (menuEntry != null) {
                        if (menuEntry.clickType == 58 || menuEntry.clickType == 1007 || menuEntry.clickType == 25 || menuEntry.clickType == 57 || menuEntry.clickType == 30) {
                            var component2: Component? = Component.fromHash(menuEntry.param1)
                            while (component2 != null) {
                                if (component2.idHash == component.idHash) {
                                    return true
                                }
                                component2 = getSub(INTERFACES!![component2.idHash shr 16]!!, component2)
                            }
                        }
                        menuEntry = MiniMenu.entryList.next() as MiniMenuEntry?
                    }
                } else {
                    var menuEntry = MiniMenu.entryList.getFirst() as MiniMenuEntry?
                    while (menuEntry != null) {
                        if (menuEntry.param0 == slotId && component.idHash == menuEntry.param1 && (menuEntry.clickType == 58 || menuEntry.clickType == 1007 || menuEntry.clickType == 25 || menuEntry.clickType == 57 || menuEntry.clickType == 30)) {
                            return true
                        }
                        menuEntry = MiniMenu.entryList.next() as MiniMenuEntry?
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun ifHover() {
            if (Client.BASE_WINDOW_ID != -1) {
                var x = MouseRecord.recorder!!.getMouseX()
                var y = MouseRecord.recorder!!.getMouseY()
                val record = Client.MOUSE_MOVEMENTS.first() as MouseRecord?
                if (record != null) {
                    x = record.getX()
                    y = record.getY()
                }
                if (Client.draggingComponent != null && Client.frameComponent == Client.selectedComponent) {
                    Client.isComponentSelected = true
                    Client.selectedComponentX = 0
                    Client.selectedComponentY = 0
                    Client.selectedComponentWidth = GameShell.screenCanvasWidth
                    Client.selectedComponentHeight = GameShell.screenCanvasHeight * -969250379
                }
                openSub(
                    null,
                    Client.BASE_WINDOW_ID,
                    0,
                    0,
                    GameShell.screenCanvasWidth,
                    GameShell.screenCanvasHeight * -969250379,
                    0,
                    0,
                    x,
                    y
                )
                if (WorldMap.worldmapComponent != null) {
                    WorldMapTrigger.ifWorldmapHover(x, y)
                }
            }
        }

        @JvmStatic
        fun ifOptionInteract(option: Int, interfaceHash: Int, slotId: Int, optionName: String) {
            val component = Component.listIcomponent(interfaceHash, slotId)
            if (component != null) {
                if (component.params != null) {
                    val hookRequest = HookRequest()
                    hookRequest.source = component
                    hookRequest.clickedOption = option
                    hookRequest.optionName = optionName
                    hookRequest.params = component.params
                    CS2Executor.executeHookInner(hookRequest)
                }
                if (Client.getComponentSettings(component)!!.clickOptionEnabled(option - 1)) {
                    val connection = Client.connectionContext
                    if (Client.gameState == GameState.UNK_0 || Client.gameState == GameState.LOGGED_INGAME) {
                        var packet: TcpPacket
                        when (option) {
                            1 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP1, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            2 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP2, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            3 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP3, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            4 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP4, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            5 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP5, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            6 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP6, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            7 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP7, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            8 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP8, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            9 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP9, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            10 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP10, connection.isaac)
                                PacketEncoder.writeInterfaceComponentData(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun getSub(inter: Interface, component: Component): Component? {
            if (component.parentId != -1) {
                return inter.getComponent(component.parentId)
            }
            if (!inter.preventInteraction) {
                val interfaceId = component.idHash ushr 16
                val subInterfaces = HashTableIterator<SubInterface>(Client.OPEN_INTERFACES!!)
                var sub = subInterfaces.first() as SubInterface?
                while (sub != null) {
                    if (interfaceId == sub.subInterfaceId) {
                        return Component.fromHash(sub.pointer.toInt())
                    }
                    sub = subInterfaces.next() as SubInterface?
                }
            }
            return null
        }

        @JvmStatic
        fun getUseOptionString(component: Component): String? {
            return if (Client.getComponentSettings(component)!!.getUseOptionFlags() == 0) {
                null
            } else {
                val targetVerb = component.targetVerb
                if (targetVerb != null && !targetVerb.trim().isEmpty()) {
                    targetVerb
                } else {
                    if (Client.DISPLAY_HIDDEN_MENU_OPTIONS) "Hidden-use" else null
                }
            }
        }

        @JvmStatic
        fun runInterfaceScripts(components: Array<Component?>, scriptMode: Int) {
            for (index in components.indices) {
                val component = components[index]
                if (component != null) {
                    if (component.type == ComponentType.CONTAINER) {
                        if (component.itemSlots != null) {
                            runInterfaceScripts(component.itemSlots!!, scriptMode)
                        }
                        val subInterface = Client.OPEN_INTERFACES!!.get(component.idHash.toLong()) as SubInterface?
                        if (subInterface != null) {
                            openSub(subInterface.subInterfaceId, scriptMode)
                        }
                    }
                    if (scriptMode == 0 && component.onSubClose != null) {
                        val hookRequest = HookRequest()
                        hookRequest.source = component
                        hookRequest.params = component.onSubClose
                        CS2Executor.executeHookInner(hookRequest)
                    }
                    if (scriptMode == 1 && component.onSubChange != null) {
                        if (component.slotId >= 0) {
                            val parentComponent = Component.fromHash(component.idHash)
                            if (parentComponent == null || parentComponent.slotChildren == null || component.slotId >= parentComponent.slotChildren!!.size || component != parentComponent.slotChildren!![component.slotId]) {
                                continue
                            }
                        }
                        val hookRequest = HookRequest()
                        hookRequest.source = component
                        hookRequest.params = component.onSubChange
                        CS2Executor.executeHookInner(hookRequest)
                    }
                }
            }
        }

        @JvmStatic
        fun moveComponentToFront(inter: Interface, targetComponent: Component?) {
            if (targetComponent != null) {
                var searchIndex: Int
                if (targetComponent.slotId != -1) {
                    val interfaceComponent = inter.getComponent(targetComponent.parentId)
                    if (interfaceComponent != null) {
                        if (interfaceComponent.itemSlots === interfaceComponent.slotChildren) {
                            interfaceComponent.itemSlots = Array(interfaceComponent.slotChildren!!.size) { null }
                            interfaceComponent.itemSlots!![interfaceComponent.itemSlots!!.size - 1] = targetComponent
                            ArrayUtils.arrayCopy(interfaceComponent.slotChildren!!, 0, interfaceComponent.itemSlots!!, 0, targetComponent.slotId)
                            ArrayUtils.arrayCopy(
                                interfaceComponent.slotChildren!!,
                                targetComponent.slotId + 1,
                                interfaceComponent.itemSlots!!,
                                targetComponent.slotId,
                                interfaceComponent.slotChildren!!.size - targetComponent.slotId - 1
                            )
                        } else {
                            searchIndex = 0
                            val itemSlots = interfaceComponent.itemSlots!!
                            while (searchIndex < itemSlots.size && itemSlots[searchIndex] != targetComponent) {
                                searchIndex++
                            }
                            if (searchIndex < itemSlots.size) {
                                ArrayUtils.arrayCopy(itemSlots, searchIndex + 1, itemSlots, searchIndex, itemSlots.size - searchIndex - 1)
                                itemSlots[interfaceComponent.itemSlots!!.size - 1] = targetComponent
                            }
                        }
                    }
                } else {
                    val mutableComponents = inter.getOrCreateMutableComponents()
                    searchIndex = 0
                    while (searchIndex < mutableComponents.size && mutableComponents[searchIndex] != targetComponent) {
                        searchIndex++
                    }
                    if (searchIndex < mutableComponents.size) {
                        ArrayUtils.arrayCopy(mutableComponents, searchIndex + 1, mutableComponents, searchIndex, mutableComponents.size - searchIndex - 1)
                        mutableComponents[mutableComponents.size - 1] = targetComponent
                    }
                }
            }
        }

        @JvmStatic
        fun moveComponentToBack(inter: Interface, targetComponent: Component?) {
            if (targetComponent != null) {
                var searchIndex: Int
                if (targetComponent.slotId != -1) {
                    val interfaceComponent = inter.components[targetComponent.parentId and 0xffff]
                    if (interfaceComponent != null) {
                        if (interfaceComponent.slotChildren === interfaceComponent.itemSlots) {
                            interfaceComponent.itemSlots = Array(interfaceComponent.slotChildren!!.size) { null }
                            interfaceComponent.itemSlots!![0] = targetComponent
                            ArrayUtils.arrayCopy(interfaceComponent.slotChildren!!, 0, interfaceComponent.itemSlots!!, 1, targetComponent.slotId)
                            ArrayUtils.arrayCopy(
                                interfaceComponent.slotChildren!!,
                                targetComponent.slotId + 1,
                                interfaceComponent.itemSlots!!,
                                targetComponent.slotId + 1,
                                interfaceComponent.slotChildren!!.size - targetComponent.slotId - 1
                            )
                        } else {
                            searchIndex = 0
                            val itemSlotComponents = interfaceComponent.itemSlots!!
                            while (searchIndex < itemSlotComponents.size && itemSlotComponents[searchIndex] != targetComponent) {
                                searchIndex++
                            }
                            if (searchIndex < itemSlotComponents.size) {
                                ArrayUtils.arrayCopy(itemSlotComponents, 0, itemSlotComponents, 1, searchIndex)
                                itemSlotComponents[0] = targetComponent
                            }
                        }
                    }
                } else {
                    val mutableComponents = inter.getOrCreateMutableComponents()
                    searchIndex = 0
                    while (searchIndex < mutableComponents.size && mutableComponents[searchIndex] != targetComponent) {
                        searchIndex++
                    }
                    if (searchIndex < mutableComponents.size) {
                        ArrayUtils.arrayCopy(mutableComponents, 0, mutableComponents, 1, searchIndex)
                        mutableComponents[0] = targetComponent
                    }
                }
            }
        }
    }
}
