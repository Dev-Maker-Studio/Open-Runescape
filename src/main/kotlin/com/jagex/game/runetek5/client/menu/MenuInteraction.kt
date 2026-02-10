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
    var aClass1184825: Component? = null

    @JvmStatic
    fun processMenuclick(entry: MiniMenuEntry?, x: Int, y: Int) {
        if (entry != null && MiniMenu.aClass482171!!.tail != entry) {
            val attr1 = entry.anInt9581
            val attr2 = entry.anInt9582
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
                    PacketEncoder.method2590(component)
                }
            }

            if (clickType == 25) {
                val component = Component.listIcomponent(attr2, attr1)
                if (component != null) {
                    InterfaceComponentRenderer.cancelInterfaceSelection()
                    val class282Sub1021 = Client.getComponentSettings(component)
                    InterfaceRenderer.setUseOptionFlags(component, class282Sub1021!!.getUseOptionFlags(), class282Sub1021.interfaceId)
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

                val component = aClass1184825
                if (component != null && Client.menuItemHoverCycle == 0) {
                    Component.redrawComponent(component)
                }
            }
        }
    }

    @JvmStatic
    fun method7703() {
        if (Client.CURRENT_CUTSCENE < 0) {
            val menufont = FontMetrics.getForMinimenu()
            if (!MiniMenu.isOpen) {
                MiniMenu.aBool162 = MiniMenu.anInt198 != -1 && MiniMenu.entryCount >= MiniMenu.anInt198 || (if (FontMetrics.useBoldFont) 26 else 22) + MiniMenu.anInt178 * MiniMenu.entryCount > GameShell.screenCanvasHeight * -969250379
            }
            MiniMenu.aClass482174!!.remove_all()
            MiniMenu.aClass482175!!.remove_all()
            var entry = MiniMenu.aClass482171!!.getFirst() as MiniMenuEntry?
            while (entry != null) {
                val clickType = entry.clickType
                if (clickType < 1000) {
                    entry.unlink()
                    if (clickType != 59 && clickType != 2 && clickType != 8 && clickType != 17 && clickType != 15 && clickType != 16 && clickType != 58) {
                        MiniMenu.aClass482174!!.addFirst(entry)
                    } else {
                        MiniMenu.aClass482175!!.addFirst(entry)
                    }
                }
                entry = MiniMenu.aClass482171!!.next() as MiniMenuEntry?
            }
            MiniMenu.aClass482174!!.method8063(MiniMenu.aClass482171)
            MiniMenu.aClass482175!!.method8063(MiniMenu.aClass482171)
            if (MiniMenu.entryCount <= 1) {
                MiniMenu.aCacheableNodeSub79445 = null
                MiniMenu.aCacheableNodeSub712 = null
                MiniMenu.aCacheableNodeSub7168 = null
            } else {
                if (KeyDownCommand.minimenuKeyDown() && MiniMenu.entryCount > 2) {
                    MiniMenu.aCacheableNodeSub79445 = MiniMenu.aClass482171!!.tail!!.previous!!.previous as MiniMenuEntry?
                } else {
                    MiniMenu.aCacheableNodeSub79445 = MiniMenu.aClass482171!!.tail!!.previous as MiniMenuEntry?
                }
                MiniMenu.aCacheableNodeSub712 = MiniMenu.aClass482171!!.tail!!.previous as MiniMenuEntry?
                if (MiniMenu.entryCount > 2) {
                    MiniMenu.aCacheableNodeSub7168 = MiniMenu.aCacheableNodeSub712!!.previous as MiniMenuEntry?
                } else {
                    MiniMenu.aCacheableNodeSub7168 = null
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
                var i_6: Int
                var i_11: Int
                if (KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.openMinimenuInputSubscriber, mouseRecord)) {
                    var i_5: Int
                    var class46915: QueueIterator?
                    if (MiniMenu.aCacheableNodeSub15163 != null && mouseX >= MiniMenu.anInt3996 && mouseX <= MiniMenu.anInt3996 + MiniMenu.anInt8518 && mouseY >= MiniMenu.anInt9471 && mouseY <= StationaryWall.anInt10526 + MiniMenu.anInt9471) {
                        i_5 = -1
                        i_6 = 0
                        while (i_6 < MiniMenu.aCacheableNodeSub15163!!.queueSize) {
                            if (FontMetrics.useBoldFont) {
                                i_11 = i_6 * MiniMenu.anInt178 + menufont!!.topPadding + MiniMenu.anInt9471 + 1 + 20
                                if (mouseY > i_11 - menufont.topPadding - 1 && mouseY < i_11 + menufont.bottomPadding) {
                                    i_5 = i_6
                                }
                            } else {
                                i_11 = i_6 * MiniMenu.anInt178 + MiniMenu.anInt9471 + 31
                                if (mouseY > i_11 - 13 && mouseY < i_11 + 3) {
                                    i_5 = i_6
                                }
                            }
                            i_6++
                        }
                        if (i_5 != -1) {
                            i_6 = 0
                            class46915 = QueueIterator(MiniMenu.aCacheableNodeSub15163!!.nodeQueue)
                            var clickedEntry = class46915.nextAndReset() as MiniMenuEntry?
                            while (clickedEntry != null) {
                                if (i_5 == i_6) {
                                    processMenuclick(clickedEntry, mouseX, mouseY)
                                    break
                                }
                                ++i_6
                                clickedEntry = class46915.next() as MiniMenuEntry?
                            }
                        }
                        MiniMenu.close()
                    } else if (mouseX >= StaticMethods.anInt3555 && mouseX <= MiniMenu.anInt8975 + StaticMethods.anInt3555 && mouseY >= StaticMethods.anInt2300 && mouseY <= StaticMethods.anInt2300 + MiniMenu.anInt1104) {
                        if (!MiniMenu.aBool162) {
                            i_5 = -1
                            i_6 = 0
                            while (i_6 < MiniMenu.entryCount) {
                                if (FontMetrics.useBoldFont) {
                                    i_11 = (MiniMenu.entryCount - 1 - i_6) * MiniMenu.anInt178 + menufont!!.topPadding + StaticMethods.anInt2300 + 1 + 20
                                    if (mouseY > i_11 - menufont.topPadding - 1 && mouseY < i_11 + menufont.bottomPadding) {
                                        i_5 = i_6
                                    }
                                } else {
                                    i_11 = (MiniMenu.entryCount - 1 - i_6) * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31
                                    if (mouseY > i_11 - 13 && mouseY < i_11 + 3) {
                                        i_5 = i_6
                                    }
                                }
                                i_6++
                            }
                            if (i_5 != -1) {
                                i_6 = 0
                                val nodeiterator7 = NodeIterator(MiniMenu.aClass482171)
                                var clickedEntry = nodeiterator7.nextAndReset() as MiniMenuEntry?
                                while (clickedEntry != null) {
                                    if (i_6 == i_5) {
                                        processMenuclick(clickedEntry, mouseX, mouseY)
                                        break
                                    }
                                    ++i_6
                                    clickedEntry = nodeiterator7.next() as MiniMenuEntry?
                                }
                            }
                            MiniMenu.close()
                        } else {
                            i_5 = -1
                            i_6 = 0
                            while (i_6 < MiniMenu.anInt170) {
                                if (FontMetrics.useBoldFont) {
                                    i_11 = i_6 * MiniMenu.anInt178 + menufont!!.topPadding + StaticMethods.anInt2300 + 1 + 20
                                    if (mouseY > i_11 - menufont.topPadding - 1 && mouseY < i_11 + menufont.bottomPadding) {
                                        i_5 = i_6
                                        break
                                    }
                                } else {
                                    i_11 = i_6 * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31
                                    if (mouseY > i_11 - 13 && mouseY < i_11 + 3) {
                                        i_5 = i_6
                                        break
                                    }
                                }
                                i_6++
                            }
                            if (i_5 != -1) {
                                i_6 = 0
                                class46915 = QueueIterator(MiniMenu.aClass477182)
                                var class282Sub50Sub1517 = class46915.nextAndReset() as ColoredMiniMenuEntry?
                                while (class282Sub50Sub1517 != null) {
                                    if (i_5 == i_6) {
                                        processMenuclick(class282Sub50Sub1517.nodeQueue!!.current!!.next as MiniMenuEntry, mouseX, mouseY)
                                        MiniMenu.close()
                                        break
                                    }
                                    ++i_6
                                    class282Sub50Sub1517 = class46915.next() as ColoredMiniMenuEntry?
                                }
                            }
                        }
                    }
                } else {
                    var bool16 = false
                    if (MiniMenu.aCacheableNodeSub15163 != null) {
                        if (mouseX >= MiniMenu.anInt3996 - 10 && mouseX <= MiniMenu.anInt8518 + MiniMenu.anInt3996 + 10 && mouseY >= MiniMenu.anInt9471 - 10 && mouseY <= StationaryWall.anInt10526 + MiniMenu.anInt9471 + 10) {
                            bool16 = true
                        } else {
                            MiniMenu.method6810()
                        }
                    }
                    if (!bool16) {
                        if (mouseX >= StaticMethods.anInt3555 - 10 && mouseX <= MiniMenu.anInt8975 + StaticMethods.anInt3555 + 10 && mouseY >= StaticMethods.anInt2300 - 10 && mouseY <= MiniMenu.anInt1104 + StaticMethods.anInt2300 + 10) {
                            if (MiniMenu.aBool162) {
                                i_6 = -1
                                i_11 = -1
                                var i_12: Int
                                i_12 = 0
                                while (i_12 < MiniMenu.anInt170) {
                                    val i_13: Int
                                    if (FontMetrics.useBoldFont) {
                                        i_13 = i_12 * MiniMenu.anInt178 + menufont!!.topPadding + StaticMethods.anInt2300 + 1 + 20
                                        if (mouseY > i_13 - menufont.topPadding - 1 && mouseY < i_13 + menufont.bottomPadding) {
                                            i_6 = i_12
                                            i_11 = i_13 - menufont.topPadding - 1
                                            break
                                        }
                                    } else {
                                        i_13 = i_12 * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31
                                        if (mouseY > i_13 - 13 && mouseY < i_13 + 3) {
                                            i_6 = i_12
                                            i_11 = i_13 - 13
                                            break
                                        }
                                    }
                                    i_12++
                                }
                                if (i_6 != -1) {
                                    i_12 = 0
                                    val class4699 = QueueIterator(MiniMenu.aClass477182)
                                    var class282Sub50Sub1510 = class4699.nextAndReset() as ColoredMiniMenuEntry?
                                    while (class282Sub50Sub1510 != null) {
                                        if (i_6 == i_12) {
                                            if (class282Sub50Sub1510.queueSize > 1) {
                                                MiniMenu.method12212(class282Sub50Sub1510, i_11)
                                            }
                                            break
                                        }
                                        ++i_12
                                        class282Sub50Sub1510 = class4699.next() as ColoredMiniMenuEntry?
                                    }
                                }
                            }
                        } else {
                            MiniMenu.close()
                        }
                    }
                }
            } else {
                val bool16 = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.anInterface175878, mouseRecord)
                val bool18 = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.anInterface175874, mouseRecord)
                var bool20 = KeyDownCommand.isActive(TypeLists.INTERACTION_DEFAULTS!!.anInterface175875, mouseRecord)
                if ((bool16 || bool18) && (Client.menuOpenMode == 1 && MiniMenu.entryCount > 2 || MiniMenu.method12562())) {
                    bool20 = true
                }
                if (bool20 && MiniMenu.entryCount > 0) {
                    if (Client.draggingComponent == null && Client.menuItemCount == 0) {
                        StaticMethods.method4653(mouseX, mouseY)
                    } else {
                        MiniMenu.anInt167 = 2
                    }
                } else if (bool18) {
                    if (MiniMenu.aCacheableNodeSub7168 != null) {
                        processMenuclick(MiniMenu.aCacheableNodeSub7168, mouseX, mouseY)
                    }
                } else if (bool16) {
                    if (MiniMenu.aCacheableNodeSub79445 == null) {
                        if (Client.interfaceSelectionActive) {
                            InterfaceComponentRenderer.cancelInterfaceSelection()
                        }
                    } else {
                        val bool19 = Client.draggingComponent != null || Client.menuItemCount > 0
                        if (bool19) {
                            MiniMenu.anInt167 = 1
                            MiniMenu.aCacheableNodeSub7438 = MiniMenu.aCacheableNodeSub79445
                        } else {
                            processMenuclick(MiniMenu.aCacheableNodeSub79445, mouseX, mouseY)
                        }
                    }
                }
                if (Client.draggingComponent == null && Client.menuItemCount == 0) {
                    MiniMenu.anInt167 = 0
                    MiniMenu.aCacheableNodeSub7438 = null
                }
            }
        }
    }
}
