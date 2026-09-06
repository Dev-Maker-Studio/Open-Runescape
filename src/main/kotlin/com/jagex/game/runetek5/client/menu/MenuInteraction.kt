package com.jagex.game.runetek5.client.menu

import com.darkan.bot.Bot
import com.darkan.bot.componentIdFromHash
import com.darkan.bot.interfaceIdFromHash
import com.jagex.game.runetek5.cache.node.NodeIterator
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.KeyDownCommand
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.entity.gameobject.StationaryWall
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.trigger.worldmap.WorldMapTrigger
import com.jagex.game.runetek5.utils.TextUtils

object MenuInteraction {

    @JvmField
    var lastHoveredComponent: Component? = null

    @JvmStatic
    fun processMenuclick(entry: MiniMenuEntry?, x: Int, y: Int) {
        if (entry != null && MiniMenu.entryList!!.tail != entry) {
            val attr1 = entry.param0
            val attr2 = entry.param1
            var clickType = entry.clickType
            val uid = entry.entityUid.toInt()
            val bitpackedEntity = entry.entityUid
            if (clickType >= 2000) {
                clickType -= 2000
            }

            val baseCoordinates = Resource.mapRegion!!.getBaseCoordinates()
            var playerInteract: ClientProt? = null
            when (clickType) {
                44 -> playerInteract = ClientProt.PLAYER_OP1
                45 -> playerInteract = ClientProt.PLAYER_OP2
                46 -> playerInteract = ClientProt.PLAYER_OP3
                47 -> playerInteract = ClientProt.PLAYER_OP4
                48 -> playerInteract = ClientProt.PLAYER_OP5
                49 -> playerInteract = ClientProt.PLAYER_OP6
                50 -> playerInteract = ClientProt.PLAYER_OP7
                51 -> playerInteract = ClientProt.PLAYER_OP8
                52 -> playerInteract = ClientProt.PLAYER_OP9
                53 -> playerInteract = ClientProt.PLAYER_OP10
            }

            if (playerInteract != null) {
                val player = PlayerUpdate.players!![uid]
                if (player != null) {
                    Client.crossX = x
                    Client.crossY = y
                    Client.crossType = 2
                    Client.crossSpritePtr = 0
                    val ifPlayerPacket = PacketEncoder.createPacket(playerInteract, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                    ifPlayerPacket!!.buffer!!.writeShort(uid)
                    ifPlayerPacket.buffer!!.writeByteSubtract(if (KeyDownCommand.runKeyDown()) 1 else 0)
                    Client.GAME_CONNECTION_CONTEXT!!.queuePacket(ifPlayerPacket)
                    RouteFinder.calculateRoute(player.movementBaseX!![0], player.movementBaseY!![0], true, RouteStrategies.forPlayer(player.movementBaseX!![0], player.movementBaseY!![0], player.getSize(), player.getSize()))
                }
            }

            var packet: TcpPacket?
            if (clickType == 16) {
                Client.crossX = x
                Client.crossY = y
                Client.crossType = 2
                Client.crossSpritePtr = 0
                packet = PacketEncoder.createPacket(ClientProt.IF_ON_PLAYER, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                packet!!.buffer!!.writeShort(Client.selectedSlotId)
                packet.buffer!!.writeShortLittle(PlayerUpdate.MY_PLAYER!!.playerIndex)
                packet.buffer!!.writeByteSubtract(if (KeyDownCommand.runKeyDown()) 1 else 0)
                packet.buffer!!.writeIntInverseMiddle(Client.selectedComponentHash)
                packet.buffer!!.writeShortLittle(Client.selectedItemId)
                Client.GAME_CONNECTION_CONTEXT!!.queuePacket(packet)
            }

            if (clickType == 57 || clickType == 1007) {
                Interface.ifOptionInteract(uid, attr2, attr1, entry.coloredName)
            }

            if (clickType == 15) {
                val player = PlayerUpdate.players!![uid]
                if (player != null) {
                    Client.crossX = x
                    Client.crossY = y
                    Client.crossType = 2
                    Client.crossSpritePtr = 0
                    val ifPlayerPacket = PacketEncoder.createPacket(ClientProt.IF_ON_PLAYER, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                    ifPlayerPacket!!.buffer!!.writeShort(Client.selectedSlotId)
                    ifPlayerPacket.buffer!!.writeShortLittle(uid)
                    ifPlayerPacket.buffer!!.writeByteSubtract(if (KeyDownCommand.runKeyDown()) 1 else 0)
                    ifPlayerPacket.buffer!!.writeIntInverseMiddle(Client.selectedComponentHash)
                    ifPlayerPacket.buffer!!.writeShortLittle(Client.selectedItemId)
                    Client.GAME_CONNECTION_CONTEXT!!.queuePacket(ifPlayerPacket)
                    RouteFinder.calculateRoute(player.movementBaseX!![0], player.movementBaseY!![0], true, RouteStrategies.forPlayer(player.movementBaseX!![0], player.movementBaseY!![0], player.getSize(), player.getSize()))
                }
            }

            if (clickType == 2) {
                Client.crossX = x
                Client.crossY = y
                Client.crossType = 2
                Client.crossSpritePtr = 0
                packet = PacketEncoder.createPacket(ClientProt.IF_ON_OBJECT, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                Console.writeLine("IF_ON_OBJECT [" + interfaceIdFromHash(Client.selectedComponentHash) + ", " + componentIdFromHash(Client.selectedComponentHash) + ", " + Client.selectedSlotId + ", " + Client.selectedItemId + "] -> [" + ((bitpackedEntity ushr 32).toInt() and 0x7fffffff) + ", (" + (attr1 + baseCoordinates!!.x) + ", " + (attr2 + baseCoordinates.y) + ", " + Bot.getMyPlayerPosition().z + ")]")
                packet!!.buffer!!.writeShortAddLittle(attr1 + baseCoordinates.x)
                packet.buffer!!.writeByteSubtract(if (KeyDownCommand.runKeyDown()) 1 else 0)
                packet.buffer!!.writeIntMiddle((bitpackedEntity ushr 32).toInt() and 0x7fffffff)
                packet.buffer!!.writeInt(Client.selectedComponentHash)
                packet.buffer!!.writeShortLittle(Client.selectedItemId)
                packet.buffer!!.writeShortAdd(Client.selectedSlotId)
                packet.buffer!!.writeShortLittle(attr2 + baseCoordinates.y)
                Client.GAME_CONNECTION_CONTEXT!!.queuePacket(packet)
                RouteFinder.routeToSceneobject(attr1, attr2, bitpackedEntity)
            }

            var npcInteract: ClientProt? = null
            when (clickType) {
                9 -> npcInteract = ClientProt.NPC_OP1
                10 -> npcInteract = ClientProt.NPC_OP2
                11 -> npcInteract = ClientProt.NPC_OP3
                12 -> npcInteract = ClientProt.NPC_OP4
                13 -> npcInteract = ClientProt.NPC_OP5
                1003 -> npcInteract = ClientProt.NPC_EXAMINE
            }

            if (npcInteract != null) {
                val entity = Client.NPC_MAP.get(uid.toLong())
                if (entity != null) {
                    val npc = entity.value as NpcEntity
                    Client.crossX = x
                    Client.crossY = y
                    Client.crossType = 2
                    Client.crossSpritePtr = 0
                    Console.writeLine("$npcInteract index: $uid npcId: ${npc.definitions!!.type}")
                    val npcInteractPacket = PacketEncoder.createPacket(npcInteract, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                    npcInteractPacket!!.buffer!!.writeShort(uid)
                    npcInteractPacket.buffer!!.writeByte(if (KeyDownCommand.runKeyDown()) 1 else 0)
                    Client.GAME_CONNECTION_CONTEXT!!.queuePacket(npcInteractPacket)
                    RouteFinder.calculateRoute(npc.movementBaseX!![0], npc.movementBaseY!![0], true, RouteStrategies.forPlayer(npc.movementBaseX!![0], npc.movementBaseY!![0], npc.getSize(), npc.getSize()))
                }
            }

            if (clickType == 60) {
                if (Client.PLAYER_RIGHTS > 0 && KeyDownCommand.teleportKeyDown()) {
                    CommandExecutor.executeTeleportCommand(PlayerUpdate.MY_PLAYER!!.plane.toInt(), attr1 + baseCoordinates!!.x, attr2 + baseCoordinates.y)
                } else {
                    Client.crossX = x
                    Client.crossY = y
                    Client.crossType = 1
                    Client.crossSpritePtr = 0
                    val walkPacket = PacketEncoder.createPacket(ClientProt.UNK_63, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                    walkPacket!!.buffer!!.writeShortAdd(attr1 + baseCoordinates!!.x)
                    walkPacket.buffer!!.writeShort(attr2 + baseCoordinates.y)
                    Client.GAME_CONNECTION_CONTEXT!!.queuePacket(walkPacket)
                }
            }

            if (clickType == 23) {
                if (Client.PLAYER_RIGHTS > 0 && KeyDownCommand.teleportKeyDown()) {
                    CommandExecutor.executeTeleportCommand(PlayerUpdate.MY_PLAYER!!.plane.toInt(), attr1 + baseCoordinates!!.x, attr2 + baseCoordinates.y)
                } else {
                    val walkPacket = PacketEncoder.walkToTile(attr1, attr2, uid)
                    if (uid == 1) {
                        walkPacket!!.buffer!!.writeByte(-1)
                        walkPacket.buffer!!.writeByte(-1)
                        walkPacket.buffer!!.writeShort(Camera.camAngleY.toInt())
                        walkPacket.buffer!!.writeByte(57)
                        walkPacket.buffer!!.writeByte(Client.minimapRotation)
                        walkPacket.buffer!!.writeByte(Client.minimapZoom)
                        walkPacket.buffer!!.writeByte(89)
                        val absolutePosition = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition()!!.coords
                        walkPacket.buffer!!.writeShort(absolutePosition!!.x.toInt())
                        walkPacket.buffer!!.writeShort(absolutePosition.z.toInt())
                        walkPacket.buffer!!.writeByte(63)
                    } else {
                        Client.crossX = x
                        Client.crossY = y
                        Client.crossType = 1
                        Client.crossSpritePtr = 0
                    }

                    Client.GAME_CONNECTION_CONTEXT!!.queuePacket(walkPacket!!)
                    RouteFinder.calculateRoute(attr1, attr2, true, RouteStrategies.forFixedTile(attr1, attr2))
                }
            }

            if (clickType == 17) {
                Client.crossX = x
                Client.crossY = y
                Client.crossType = 2
                Client.crossSpritePtr = 0
                val ifGrounditemPacket = PacketEncoder.createPacket(ClientProt.IF_ON_GROUND_ITEM, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                ifGrounditemPacket!!.buffer!!.writeShortAdd(Client.selectedItemId)
                ifGrounditemPacket.buffer!!.writeIntInverseMiddle(Client.selectedComponentHash)
                ifGrounditemPacket.buffer!!.writeShort(uid)
                ifGrounditemPacket.buffer!!.writeByteSubtract(if (KeyDownCommand.runKeyDown()) 1 else 0)
                ifGrounditemPacket.buffer!!.writeShortAddLittle(Client.selectedSlotId)
                ifGrounditemPacket.buffer!!.writeShortAddLittle(attr2 + baseCoordinates!!.y)
                ifGrounditemPacket.buffer!!.writeShortLittle(attr1 + baseCoordinates.x)
                Client.GAME_CONNECTION_CONTEXT!!.queuePacket(ifGrounditemPacket)
                RouteFinder.routeToGrounditem(attr1, attr2)
            }

            if (clickType == 59) {
                Client.crossX = x
                Client.crossY = y
                Client.crossType = 1
                Client.crossSpritePtr = 0
                val ifTilePacket = PacketEncoder.createPacket(ClientProt.IF_ON_TILE, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                ifTilePacket!!.buffer!!.writeShortLittle(Client.selectedItemId)
                ifTilePacket.buffer!!.writeShortLittle(attr2 + baseCoordinates!!.y)
                ifTilePacket.buffer!!.writeIntMiddle(Client.selectedComponentHash)
                ifTilePacket.buffer!!.writeShortAdd(Client.selectedSlotId)
                ifTilePacket.buffer!!.writeShortLittle(attr1 + baseCoordinates.x)
                Client.GAME_CONNECTION_CONTEXT!!.queuePacket(ifTilePacket)
                RouteFinder.calculateRoute(attr1, attr2, true, RouteStrategies.forFixedTile(attr1, attr2))
            }

            if (clickType == 30 && Client.activeDialogueContinue == null) {
                PacketEncoder.continueInterfacePressed(attr2, attr1)
                val comp = Component.listIcomponent(attr2, attr1)
                Client.activeDialogueContinue = comp
                if (comp != null) {
                    Component.redrawComponent(comp)
                }
            }

            if (clickType == 58) {
                val component = Component.listIcomponent(attr2, attr1)
                if (component != null) {
                    PacketEncoder.sendInterfaceOnInterface(component)
                }
            }

            if (clickType == 25) {
                val component = Component.listIcomponent(attr2, attr1)
                if (component != null) {
                    InterfaceComponentRenderer.cancelInterfaceSelection()
                    val componentSettings = Client.getComponentSettings(component)
                    InterfaceRenderer.setUseOptionFlags(component, componentSettings!!.getUseOptionFlags(), componentSettings.interfaceId)
                    Client.useOnType = Interface.getUseOptionString(component)
                    if (Client.useOnType == null) {
                        Client.useOnType = "Null"
                    }

                    Client.useOnTarget = component.opBase + TextUtils.setTextColor(16777215)
                }
            } else {
                var groundItemInteract: ClientProt? = null
                when (clickType) {
                    18 -> groundItemInteract = ClientProt.GROUND_ITEM_OP1
                    19 -> groundItemInteract = ClientProt.GROUND_ITEM_OP2
                    20 -> groundItemInteract = ClientProt.GROUND_ITEM_OP3
                    21 -> groundItemInteract = ClientProt.GROUND_ITEM_OP4
                    22 -> groundItemInteract = ClientProt.GROUND_ITEM_OP5
                    1004 -> groundItemInteract = ClientProt.GROUND_ITEM_EXAMINE
                }

                if (groundItemInteract != null) {
                    Client.crossX = x
                    Client.crossY = y
                    Client.crossType = 2
                    Client.crossSpritePtr = 0
                    val groundItemPacket = PacketEncoder.createPacket(groundItemInteract, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                    groundItemPacket!!.buffer!!.writeShortAddLittle(uid)
                    groundItemPacket.buffer!!.writeByteInverse(if (KeyDownCommand.runKeyDown()) 1 else 0)
                    groundItemPacket.buffer!!.writeShort(attr2 + baseCoordinates!!.y)
                    groundItemPacket.buffer!!.writeShortAdd(attr1 + baseCoordinates.x)
                    Client.GAME_CONNECTION_CONTEXT!!.queuePacket(groundItemPacket)
                    RouteFinder.routeToGrounditem(attr1, attr2)
                }

                if (clickType == 8) {
                    val entity = Client.NPC_MAP.get(uid.toLong())
                    if (entity != null) {
                        val npc = entity.value as NpcEntity
                        Client.crossX = x
                        Client.crossY = y
                        Client.crossType = 2
                        Client.crossSpritePtr = 0
                        val ifNpcPacket = PacketEncoder.createPacket(ClientProt.IF_ON_NPC, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                        ifNpcPacket!!.buffer!!.writeIntInverseMiddle(Client.selectedComponentHash)
                        ifNpcPacket.buffer!!.writeShortAddLittle(uid)
                        ifNpcPacket.buffer!!.writeByteSubtract(if (KeyDownCommand.runKeyDown()) 1 else 0)
                        ifNpcPacket.buffer!!.writeShortAddLittle(Client.selectedItemId)
                        ifNpcPacket.buffer!!.writeShortAdd(Client.selectedSlotId)
                        Client.GAME_CONNECTION_CONTEXT!!.queuePacket(ifNpcPacket)
                        RouteFinder.calculateRoute(npc.movementBaseX!![0], npc.movementBaseY!![0], true, RouteStrategies.forPlayer(npc.movementBaseX!![0], npc.movementBaseY!![0], npc.getSize(), npc.getSize()))
                    }
                }

                var locInteract: ClientProt? = null
                when (clickType) {
                    3 -> locInteract = ClientProt.OBJECT_OP1
                    4 -> locInteract = ClientProt.OBJECT_OP2
                    5 -> locInteract = ClientProt.OBJECT_OP3
                    6 -> locInteract = ClientProt.OBJECT_OP4
                    1001 -> locInteract = ClientProt.OBJECT_OP5
                    1002 -> locInteract = ClientProt.OBJECT_EXAMINE
                }

                if (locInteract != null) {
                    Client.crossX = x
                    Client.crossY = y
                    Client.crossType = 2
                    Client.crossSpritePtr = 0
                    Console.writeLine("$locInteract id: ${(bitpackedEntity ushr 32).toInt() and 0x7fffffff} coords: (${attr1 + baseCoordinates!!.x}, ${attr2 + baseCoordinates.y}, ${Bot.getMyPlayerPosition().z})")
                    val locInteractPacket = PacketEncoder.createPacket(locInteract, Client.GAME_CONNECTION_CONTEXT!!.isaac)
                    locInteractPacket!!.buffer!!.writeShort(attr2 + baseCoordinates.y)
                    locInteractPacket.buffer!!.writeShort(attr1 + baseCoordinates.x)
                    locInteractPacket.buffer!!.writeInt((bitpackedEntity ushr 32).toInt() and 0x7fffffff)
                    locInteractPacket.buffer!!.writeByteAdd(if (KeyDownCommand.runKeyDown()) 1 else 0)
                    Client.GAME_CONNECTION_CONTEXT!!.queuePacket(locInteractPacket)
                    RouteFinder.routeToSceneobject(attr1, attr2, bitpackedEntity)
                }

                if (clickType == 1008 || clickType == 1009 || clickType == 1010 || clickType == 1011 || clickType == 1012) {
                    WorldMapTrigger.onWorldmapMinimenuClick(clickType, uid, attr1)
                }

                if (Client.interfaceSelectionActive) {
                    InterfaceComponentRenderer.cancelInterfaceSelection()
                }

                val component = lastHoveredComponent
                if (component != null && Client.menuItemHoverCycle == 0) {
                    Component.redrawComponent(component)
                }
            }
        }
    }

    @JvmStatic
    fun processMenuInput() {
        if (Client.CURRENT_CUTSCENE < 0) {
            val menufont = FontMetrics.getForMinimenu()
            if (!MiniMenu.isOpen) {
                MiniMenu.isCollapsed = MiniMenu.maxVisibleEntries != -1 && MiniMenu.entryCount >= MiniMenu.maxVisibleEntries || (if (FontMetrics.useBoldFont) 26 else 22) + MiniMenu.entryRowHeight * MiniMenu.entryCount > GameShell.screenCanvasHeight * -969250379
            }
            MiniMenu.lowPriorityEntries!!.remove_all()
            MiniMenu.interfaceEntries!!.remove_all()
            var entry = MiniMenu.entryList!!.getFirst() as MiniMenuEntry?
            while (entry != null) {
                val clickType = entry.clickType
                if (clickType < 1000) {
                    entry.unlink()
                    if (clickType != 59 && clickType != 2 && clickType != 8 && clickType != 17 && clickType != 15 && clickType != 16 && clickType != 58) {
                        MiniMenu.lowPriorityEntries!!.addFirst(entry)
                    } else {
                        MiniMenu.interfaceEntries!!.addFirst(entry)
                    }
                }
                entry = MiniMenu.entryList!!.next() as MiniMenuEntry?
            }
            MiniMenu.lowPriorityEntries!!.transferAllTo(MiniMenu.entryList)
            MiniMenu.interfaceEntries!!.transferAllTo(MiniMenu.entryList)
            if (MiniMenu.entryCount <= 1) {
                MiniMenu.activeEntry = null
                MiniMenu.topEntry = null
                MiniMenu.secondaryEntry = null
            } else {
                if (KeyDownCommand.minimenuKeyDown() && MiniMenu.entryCount > 2) {
                    MiniMenu.activeEntry = MiniMenu.entryList!!.tail!!.previous!!.previous as MiniMenuEntry?
                } else {
                    MiniMenu.activeEntry = MiniMenu.entryList!!.tail!!.previous as MiniMenuEntry?
                }
                MiniMenu.topEntry = MiniMenu.entryList!!.tail!!.previous as MiniMenuEntry?
                if (MiniMenu.entryCount > 2) {
                    MiniMenu.secondaryEntry = MiniMenu.topEntry!!.previous as MiniMenuEntry?
                } else {
                    MiniMenu.secondaryEntry = null
                }
            }
            val mouseRecord = Client.MOUSE_MOVEMENTS.getFirst() as MouseRecord?
            val mouseX: Int
            val mouseY: Int
            if (mouseRecord != null) {
                mouseX = mouseRecord.getX()
                mouseY = mouseRecord.getY()
            } else {
                mouseX = MouseRecord.recorder!!.getMouseX()
                mouseY = MouseRecord.recorder!!.getMouseY()
            }
            if (MiniMenu.isOpen) {
                var entryCounter: Int
                var entryY: Int
                if (KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.openMinimenuInputSubscriber, mouseRecord)) {
                    var hoveredIndex: Int
                    var queueIterator: QueueIterator?
                    if (MiniMenu.activeSubmenu != null && mouseX >= MiniMenu.submenuX && mouseX <= MiniMenu.submenuX + MiniMenu.submenuWidth && mouseY >= MiniMenu.submenuY && mouseY <= StationaryWall.submenuHeight + MiniMenu.submenuY) {
                        hoveredIndex = -1
                        entryCounter = 0
                        while (entryCounter < MiniMenu.activeSubmenu!!.queueSize) {
                            if (FontMetrics.useBoldFont) {
                                entryY = entryCounter * MiniMenu.entryRowHeight + menufont!!.topPadding + MiniMenu.submenuY + 1 + 20
                                if (mouseY > entryY - menufont.topPadding - 1 && mouseY < entryY + menufont.bottomPadding) {
                                    hoveredIndex = entryCounter
                                }
                            } else {
                                entryY = entryCounter * MiniMenu.entryRowHeight + MiniMenu.submenuY + 31
                                if (mouseY > entryY - 13 && mouseY < entryY + 3) {
                                    hoveredIndex = entryCounter
                                }
                            }
                            entryCounter++
                        }
                        if (hoveredIndex != -1) {
                            entryCounter = 0
                            queueIterator = QueueIterator(MiniMenu.activeSubmenu!!.nodeQueue)
                            var clickedEntry = queueIterator.nextAndReset() as MiniMenuEntry?
                            while (clickedEntry != null) {
                                if (hoveredIndex == entryCounter) {
                                    processMenuclick(clickedEntry, mouseX, mouseY)
                                    break
                                }
                                ++entryCounter
                                clickedEntry = queueIterator.next() as MiniMenuEntry?
                            }
                        }
                        MiniMenu.close()
                    } else if (mouseX >= StaticMethods.menuOpenX && mouseX <= MiniMenu.menuWidth + StaticMethods.menuOpenX && mouseY >= StaticMethods.menuOpenY && mouseY <= StaticMethods.menuOpenY + MiniMenu.menuHeight) {
                        if (!MiniMenu.isCollapsed) {
                            hoveredIndex = -1
                            entryCounter = 0
                            while (entryCounter < MiniMenu.entryCount) {
                                if (FontMetrics.useBoldFont) {
                                    entryY = (MiniMenu.entryCount - 1 - entryCounter) * MiniMenu.entryRowHeight + menufont!!.topPadding + StaticMethods.menuOpenY + 1 + 20
                                    if (mouseY > entryY - menufont.topPadding - 1 && mouseY < entryY + menufont.bottomPadding) {
                                        hoveredIndex = entryCounter
                                    }
                                } else {
                                    entryY = (MiniMenu.entryCount - 1 - entryCounter) * MiniMenu.entryRowHeight + StaticMethods.menuOpenY + 31
                                    if (mouseY > entryY - 13 && mouseY < entryY + 3) {
                                        hoveredIndex = entryCounter
                                    }
                                }
                                entryCounter++
                            }
                            if (hoveredIndex != -1) {
                                entryCounter = 0
                                val entryIterator = NodeIterator(MiniMenu.entryList)
                                var clickedEntry = entryIterator.nextAndReset() as MiniMenuEntry?
                                while (clickedEntry != null) {
                                    if (entryCounter == hoveredIndex) {
                                        processMenuclick(clickedEntry, mouseX, mouseY)
                                        break
                                    }
                                    ++entryCounter
                                    clickedEntry = entryIterator.next() as MiniMenuEntry?
                                }
                            }
                            MiniMenu.close()
                        } else {
                            hoveredIndex = -1
                            entryCounter = 0
                            while (entryCounter < MiniMenu.coloredEntryCount) {
                                if (FontMetrics.useBoldFont) {
                                    entryY = entryCounter * MiniMenu.entryRowHeight + menufont!!.topPadding + StaticMethods.menuOpenY + 1 + 20
                                    if (mouseY > entryY - menufont.topPadding - 1 && mouseY < entryY + menufont.bottomPadding) {
                                        hoveredIndex = entryCounter
                                        break
                                    }
                                } else {
                                    entryY = entryCounter * MiniMenu.entryRowHeight + StaticMethods.menuOpenY + 31
                                    if (mouseY > entryY - 13 && mouseY < entryY + 3) {
                                        hoveredIndex = entryCounter
                                        break
                                    }
                                }
                                entryCounter++
                            }
                            if (hoveredIndex != -1) {
                                entryCounter = 0
                                queueIterator = QueueIterator(MiniMenu.sortedColoredEntries)
                                var coloredEntry = queueIterator.nextAndReset() as ColoredMiniMenuEntry?
                                while (coloredEntry != null) {
                                    if (hoveredIndex == entryCounter) {
                                        val firstEntry = coloredEntry.nodeQueue.getCurrentNext() as? MiniMenuEntry
                                        if (firstEntry != null) {
                                            processMenuclick(firstEntry, mouseX, mouseY)
                                        }
                                        MiniMenu.close()
                                        break
                                    }
                                    ++entryCounter
                                    coloredEntry = queueIterator.next() as ColoredMiniMenuEntry?
                                }
                            }
                        }
                    }
                } else {
                    var isInSubmenuBounds = false
                    if (MiniMenu.activeSubmenu != null) {
                        if (mouseX >= MiniMenu.submenuX - 10 && mouseX <= MiniMenu.submenuWidth + MiniMenu.submenuX + 10 && mouseY >= MiniMenu.submenuY - 10 && mouseY <= StationaryWall.submenuHeight + MiniMenu.submenuY + 10) {
                            isInSubmenuBounds = true
                        } else {
                            MiniMenu.closeSubmenu()
                        }
                    }
                    if (!isInSubmenuBounds) {
                        if (mouseX >= StaticMethods.menuOpenX - 10 && mouseX <= MiniMenu.menuWidth + StaticMethods.menuOpenX + 10 && mouseY >= StaticMethods.menuOpenY - 10 && mouseY <= MiniMenu.menuHeight + StaticMethods.menuOpenY + 10) {
                            if (MiniMenu.isCollapsed) {
                                entryCounter = -1
                                entryY = -1
                                var coloredIndex: Int
                                coloredIndex = 0
                                while (coloredIndex < MiniMenu.coloredEntryCount) {
                                    val coloredEntryY: Int
                                    if (FontMetrics.useBoldFont) {
                                        coloredEntryY = coloredIndex * MiniMenu.entryRowHeight + menufont!!.topPadding + StaticMethods.menuOpenY + 1 + 20
                                        if (mouseY > coloredEntryY - menufont.topPadding - 1 && mouseY < coloredEntryY + menufont.bottomPadding) {
                                            entryCounter = coloredIndex
                                            entryY = coloredEntryY - menufont.topPadding - 1
                                            break
                                        }
                                    } else {
                                        coloredEntryY = coloredIndex * MiniMenu.entryRowHeight + StaticMethods.menuOpenY + 31
                                        if (mouseY > coloredEntryY - 13 && mouseY < coloredEntryY + 3) {
                                            entryCounter = coloredIndex
                                            entryY = coloredEntryY - 13
                                            break
                                        }
                                    }
                                    coloredIndex++
                                }
                                if (entryCounter != -1) {
                                    coloredIndex = 0
                                    val coloredIterator = QueueIterator(MiniMenu.sortedColoredEntries)
                                    var coloredEntry = coloredIterator.nextAndReset() as ColoredMiniMenuEntry?
                                    while (coloredEntry != null) {
                                        if (entryCounter == coloredIndex) {
                                            if (coloredEntry.queueSize > 1) {
                                                MiniMenu.openSubmenu(coloredEntry, entryY)
                                            }
                                            break
                                        }
                                        ++coloredIndex
                                        coloredEntry = coloredIterator.next() as ColoredMiniMenuEntry?
                                    }
                                }
                            }
                        } else {
                            MiniMenu.close()
                        }
                    }
                }
            } else {
                val isPrimaryClick = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.primaryClickSubscriber, mouseRecord)
                val isSecondaryClick = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.secondaryClickSubscriber, mouseRecord)
                var isContextMenu = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.contextMenuSubscriber, mouseRecord)
                if ((isPrimaryClick || isSecondaryClick) && (Client.menuOpenMode == 1 && MiniMenu.entryCount > 2 || MiniMenu.isTopEntryInterfaceAction())) {
                    isContextMenu = true
                }
                if (isContextMenu && MiniMenu.entryCount > 0) {
                    if (Client.draggingComponent == null && Client.menuItemCount == 0) {
                        StaticMethods.openContextMenu(mouseX, mouseY)
                    } else {
                        MiniMenu.deferredClickType = 2
                    }
                } else if (isSecondaryClick) {
                    if (MiniMenu.secondaryEntry != null) {
                        processMenuclick(MiniMenu.secondaryEntry, mouseX, mouseY)
                    }
                } else if (isPrimaryClick) {
                    if (MiniMenu.activeEntry == null) {
                        if (Client.interfaceSelectionActive) {
                            InterfaceComponentRenderer.cancelInterfaceSelection()
                        }
                    } else {
                        val isDeferring = Client.draggingComponent != null || Client.menuItemCount > 0
                        if (isDeferring) {
                            MiniMenu.deferredClickType = 1
                            MiniMenu.deferredEntry = MiniMenu.activeEntry
                        } else {
                            processMenuclick(MiniMenu.activeEntry, mouseX, mouseY)
                        }
                    }
                }
                if (Client.draggingComponent == null && Client.menuItemCount == 0) {
                    MiniMenu.deferredClickType = 0
                    MiniMenu.deferredEntry = null
                }
            }
        }
    }
}
