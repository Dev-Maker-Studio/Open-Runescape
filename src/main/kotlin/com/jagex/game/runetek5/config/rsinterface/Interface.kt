package com.jagex.game.runetek5.config.rsinterface

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.HashTableIterator
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.MiniMenuEntry
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.network.BufferedConnectionContext
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

    fun getComponent(componentId: Int): Component {
        if (componentId ushr 16 != components[0]!!.idHash ushr 16) {
            throw IllegalArgumentException()
        }
        return components[componentId and 0xffff]!!
    }

    fun getComponents(): Array<Component?> {
        return subComponents ?: components
    }

    fun method1617(): Array<Component?> {
        if (subComponents == null) {
            val i2 = components.size
            subComponents = Array(i2) { null }
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
        var anInt3008: Int = 0

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
            INTERFACES = Array(interfaceResource!!.archiveCount) { null }
            INTERFACES_LOADED = BooleanArray(interfaceResource!!.archiveCount)
        }

        @JvmStatic
        fun initializeTables() {
            INTERFACES = Array(interfaceResource!!.archiveCount) { null }
            INTERFACES_LOADED = BooleanArray(interfaceResource!!.archiveCount)
        }

        @JvmStatic
        fun interfaceLoaded(interfaceId: Int, xteas: IntArray?): Boolean {
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
            INTERFACES_LOADED!![interfaceId] = false
            clearComponents(interfaceId)
        }

        @JvmStatic
        @JvmName("open_sub")
        fun openSub(interfaceId: Int, i1: Int) {
            if (interfaceLoaded(interfaceId, null)) {
                method2591(INTERFACES!![interfaceId]!!.components, i1)
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
            if (interfaceId != -1 && !INTERFACES_LOADED!![interfaceId]) {
                interfaceResource!!.clearArchiveFiles(interfaceId)
                INTERFACES!![interfaceId] = null
            }
        }

        @JvmStatic
        fun closeChildInterface(parentInterface: SubInterface, unload: Boolean, bool2: Boolean) {
            val parentInterfaceId = parentInterface.subInterfaceId
            val interfaceHash = parentInterface.pointer.toInt()
            parentInterface.unlink()
            if (unload) {
                unloadInterface(parentInterfaceId)
            }
            method14650(parentInterfaceId)
            val component = Component.fromHash(interfaceHash)
            if (component != null) {
                Component.redrawComponent(component)
            }
            MiniMenu.method420()
            if (!bool2 && client.BASE_WINDOW_ID != -1) {
                openSub(client.BASE_WINDOW_ID, 1)
            }
            val openInterfaceIt = HashTableIterator<SubInterface>(client.OPEN_INTERFACES)
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
                        closeChildInterface(child, true, bool2)
                    }
                }
                child = openInterfaceIt.next() as SubInterface?
            }
        }

        @JvmStatic
        fun openSub(
            parentId: Int,
            subInterface: SubInterface,
            ints2: IntArray?,
            bool3: Boolean
        ): SubInterface {
            val currentSubInterface = client.OPEN_INTERFACES.get(parentId.toLong()) as SubInterface?
            if (currentSubInterface != null) {
                closeChildInterface(
                    currentSubInterface,
                    currentSubInterface.subInterfaceId != subInterface.subInterfaceId,
                    bool3
                )
            }
            client.OPEN_INTERFACES.put(subInterface, parentId.toLong())
            resetComponentSequences(subInterface.subInterfaceId, ints2)
            val parentComponent = Component.fromHash(parentId)
            if (parentComponent != null) {
                Component.redrawComponent(parentComponent)
            }
            if (client.aClass1187352 != null) {
                Component.redrawComponent(client.aClass1187352!!)
                client.aClass1187352 = null
            }
            MiniMenu.method420()
            if (parentComponent != null) {
                Component.method483(INTERFACES!![parentComponent.idHash ushr 16]!!, parentComponent, !bool3)
            }
            if (!bool3) {
                Component.runScriptsIfLoaded(subInterface.subInterfaceId, ints2)
            }
            if (!bool3 && client.BASE_WINDOW_ID != -1) {
                openSub(client.BASE_WINDOW_ID, 1)
            }
            return subInterface
        }

        @JvmStatic
        fun decodeComponents(
            interfaceId: Int,
            xteas: IntArray?,
            rsInterface: Interface?,
            bool3: Boolean
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
                mutableRsInterface = Interface(bool3, interfaceComponents)
            } else {
                mutableRsInterface.components = interfaceComponents
                mutableRsInterface.preventInteraction = bool3
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
                    val interface11 = INTERFACES!![subInterfaceId]!!
                    InterfaceRenderer.processInterfaceCycle(
                        interface11,
                        interface11.getComponents(),
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
        fun method14650(i0: Int) {
            var node = client.ICOMPONENT_SETTINGS_SLOTS.first()
            while (node != null) {
                if (i0 == (node.pointer shr 48 and 0xffffL).toInt()) {
                    node.unlink()
                }
                node = client.ICOMPONENT_SETTINGS_SLOTS.next()
            }
        }

        @JvmStatic
        fun method4248(interfaceAndComponent: Int, i1: Int): Boolean {
            if (!MiniMenu.isOpen) {
                return false
            }
            val interId = interfaceAndComponent shr 16
            val compId = interfaceAndComponent and 0xffff
            if (INTERFACES!![interId] != null && INTERFACES!![interId]!!.components[compId] != null) {
                val component = INTERFACES!![interId]!!.components[compId]!!
                if (i1 == -1 && component.type == ComponentType.CONTAINER) {
                    var menuEntry = MiniMenu.aClass482171.first() as MiniMenuEntry?
                    while (menuEntry != null) {
                        if (menuEntry.clickType == 58 || menuEntry.clickType == 1007 || menuEntry.clickType == 25 || menuEntry.clickType == 57 || menuEntry.clickType == 30) {
                            var component2: Component? = Component.fromHash(menuEntry.anInt9582)
                            while (component2 != null) {
                                if (component2.idHash == component.idHash) {
                                    return true
                                }
                                component2 = getSub(INTERFACES!![component2.idHash shr 16]!!, component2)
                            }
                        }
                        menuEntry = MiniMenu.aClass482171.next() as MiniMenuEntry?
                    }
                } else {
                    var menuEntry = MiniMenu.aClass482171.first() as MiniMenuEntry?
                    while (menuEntry != null) {
                        if (menuEntry.anInt9581 == i1 && component.idHash == menuEntry.anInt9582 && (menuEntry.clickType == 58 || menuEntry.clickType == 1007 || menuEntry.clickType == 25 || menuEntry.clickType == 57 || menuEntry.clickType == 30)) {
                            return true
                        }
                        menuEntry = MiniMenu.aClass482171.next() as MiniMenuEntry?
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun ifHover() {
            if (client.BASE_WINDOW_ID != -1) {
                var x = MouseRecord.recorder!!.getMouseX()
                var y = MouseRecord.recorder!!.getMouseY()
                val record = client.MOUSE_MOVEMENTS.first() as MouseRecord?
                if (record != null) {
                    x = record.getX()
                    y = record.getY()
                }
                if (client.draggingComponent != null && client.frameComponent == client.selectedComponent) {
                    client.isComponentSelected = true
                    client.selectedComponentX = 0
                    client.selectedComponentY = 0
                    client.selectedComponentWidth = GameShell.screenCanvasWidth
                    client.selectedComponentHeight = client.screenCanvasHeight * -969250379
                }
                openSub(
                    null,
                    client.BASE_WINDOW_ID,
                    0,
                    0,
                    GameShell.screenCanvasWidth,
                    client.screenCanvasHeight * -969250379,
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
                    hookRequest.anInt8051 = option
                    hookRequest.optionName = optionName
                    hookRequest.params = component.params
                    CS2Executor.executeHookInner(hookRequest)
                }
                if (client.getComponentSettings(component).clickOptionEnabled(option - 1)) {
                    val connection = client.getConnectionContext()
                    if (client.gameState == GameState.UNK_0 || client.gameState == GameState.LOGGED_INGAME) {
                        var packet: TcpPacket
                        when (option) {
                            1 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP1, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            2 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP2, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            3 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP3, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            4 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP4, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            5 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP5, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            6 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP6, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            7 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP7, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            8 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP8, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            9 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP9, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
                                connection.queuePacket(packet)
                            }
                            10 -> {
                                packet = PacketEncoder.createPacket(ClientProt.IF_OP10, connection.isaac)
                                PacketEncoder.method12590(packet, interfaceHash, slotId, component.containerItemId)
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
                val subInterfaces = HashTableIterator<SubInterface>(client.OPEN_INTERFACES)
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
            return if (client.getComponentSettings(component).getUseOptionFlags() == 0) {
                null
            } else {
                if (component.targetVerb != null && !component.targetVerb.trim().isEmpty()) {
                    component.targetVerb
                } else {
                    if (client.DISPLAY_HIDDEN_MENU_OPTIONS) "Hidden-use" else null
                }
            }
        }

        @JvmStatic
        fun method2591(components: Array<Component?>, i1: Int) {
            for (index in components.indices) {
                val component = components[index]
                if (component != null) {
                    if (component.type == ComponentType.CONTAINER) {
                        if (component.itemSlots != null) {
                            method2591(component.itemSlots!!, i1)
                        }
                        val subInterface = client.OPEN_INTERFACES.get(component.idHash.toLong()) as SubInterface?
                        if (subInterface != null) {
                            openSub(subInterface.subInterfaceId, i1)
                        }
                    }
                    if (i1 == 0 && component.hookrequestType0Params != null) {
                        val hookRequest = HookRequest()
                        hookRequest.source = component
                        hookRequest.params = component.hookrequestType0Params
                        CS2Executor.executeHookInner(hookRequest)
                    }
                    if (i1 == 1 && component.hookrequestType1Params != null) {
                        if (component.slotId >= 0) {
                            val icomponentdefinitions7 = Component.fromHash(component.idHash)
                            if (icomponentdefinitions7 == null || icomponentdefinitions7.slotChildren == null || component.slotId >= icomponentdefinitions7.slotChildren!!.size || component != icomponentdefinitions7.slotChildren!![component.slotId]) {
                                continue
                            }
                        }
                        val hookRequest = HookRequest()
                        hookRequest.source = component
                        hookRequest.params = component.hookrequestType1Params
                        CS2Executor.executeHookInner(hookRequest)
                    }
                }
            }
        }

        @JvmStatic
        fun method7554(inter: Interface, defs: Component?) {
            if (defs != null) {
                var i4: Int
                if (defs.slotId != -1) {
                    val interfaceComponent = inter.getComponent(defs.parentId)
                    if (interfaceComponent != null) {
                        if (interfaceComponent.itemSlots === interfaceComponent.slotChildren) {
                            interfaceComponent.itemSlots = Array(interfaceComponent.slotChildren!!.size) { null }
                            interfaceComponent.itemSlots!![interfaceComponent.itemSlots!!.size - 1] = defs
                            ArrayUtils.arrayCopy(interfaceComponent.slotChildren!!, 0, interfaceComponent.itemSlots!!, 0, defs.slotId)
                            ArrayUtils.arrayCopy(
                                interfaceComponent.slotChildren!!,
                                defs.slotId + 1,
                                interfaceComponent.itemSlots!!,
                                defs.slotId,
                                interfaceComponent.slotChildren!!.size - defs.slotId - 1
                            )
                        } else {
                            i4 = 0
                            val arr5 = interfaceComponent.itemSlots!!
                            while (i4 < arr5.size && arr5[i4] != defs) {
                                i4++
                            }
                            if (i4 < arr5.size) {
                                ArrayUtils.arrayCopy(arr5, i4 + 1, arr5, i4, arr5.size - i4 - 1)
                                arr5[interfaceComponent.itemSlots!!.size - 1] = defs
                            }
                        }
                    }
                } else {
                    val arr6 = inter.method1617()
                    i4 = 0
                    while (i4 < arr6.size && arr6[i4] != defs) {
                        i4++
                    }
                    if (i4 < arr6.size) {
                        ArrayUtils.arrayCopy(arr6, i4 + 1, arr6, i4, arr6.size - i4 - 1)
                        arr6[arr6.size - 1] = defs
                    }
                }
            }
        }

        @JvmStatic
        fun method3710(inter: Interface, defs: Component?) {
            if (defs != null) {
                var i4: Int
                if (defs.slotId != -1) {
                    val interfaceComponent = inter.components[defs.parentId and 0xffff]
                    if (interfaceComponent != null) {
                        if (interfaceComponent.slotChildren === interfaceComponent.itemSlots) {
                            interfaceComponent.itemSlots = Array(interfaceComponent.slotChildren!!.size) { null }
                            interfaceComponent.itemSlots!![0] = defs
                            ArrayUtils.arrayCopy(interfaceComponent.slotChildren!!, 0, interfaceComponent.itemSlots!!, 1, defs.slotId)
                            ArrayUtils.arrayCopy(
                                interfaceComponent.slotChildren!!,
                                defs.slotId + 1,
                                interfaceComponent.itemSlots!!,
                                defs.slotId + 1,
                                interfaceComponent.slotChildren!!.size - defs.slotId - 1
                            )
                        } else {
                            i4 = 0
                            val itemSlotComponents = interfaceComponent.itemSlots!!
                            while (i4 < itemSlotComponents.size && itemSlotComponents[i4] != defs) {
                                i4++
                            }
                            if (i4 < itemSlotComponents.size) {
                                ArrayUtils.arrayCopy(itemSlotComponents, 0, itemSlotComponents, 1, i4)
                                itemSlotComponents[0] = defs
                            }
                        }
                    }
                } else {
                    val arr6 = inter.method1617()
                    i4 = 0
                    while (i4 < arr6.size && arr6[i4] != defs) {
                        i4++
                    }
                    if (i4 < arr6.size) {
                        ArrayUtils.arrayCopy(arr6, 0, arr6, 1, i4)
                        arr6[0] = defs
                    }
                }
            }
        }
    }
}
