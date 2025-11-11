package com.jagex.game.runetek5.network.packet

import com.jagex.game.runetek5.botdetection.ReflectionCheckManager
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.buffer.Packet.Bit
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.cache.node.impl.ProjectileSpawnRequest
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.login.*
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.MiniMenuBuilder
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageTypeList
import com.jagex.game.runetek5.config.rsinterface.*
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.config.sequence.PositionedGfx
import com.jagex.game.runetek5.config.sequence.Projectile
import com.jagex.game.runetek5.encoding.HuffmanList
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode
import com.jagex.game.runetek5.entity.grounditem.GroundItemUpdating
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCUpdate
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation
import com.jagex.game.runetek5.graphics.hint.HintArrow
import com.jagex.game.runetek5.graphics.hint.HintTrail
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.interaction.Friend
import com.jagex.game.runetek5.interaction.FriendList
import com.jagex.game.runetek5.interaction.Ignore
import com.jagex.game.runetek5.interaction.IgnoreList
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.ChatFilter
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannelDelta
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettingsDelta
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChatPlayer
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.HostNameIdentifier
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.world.WorldDescriptor
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.*
import com.jagex.game.runetek5.scene.tile.TileMessage
import com.jagex.game.runetek5.sound.VorbisSoundManager
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.tools.Browser
import com.jagex.game.runetek5.tools.DebugDisplay
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.trigger.ClientTriggerType
import com.jagex.game.runetek5.trigger.rsinterface.*
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarProviders
import java.io.IOException

object PacketDecoder {
    @JvmField
    var HOSTNAME_IDENTIFIER: HostNameIdentifier? = null

    @JvmStatic
    fun decodePacketFromStream(connectionContext: BufferedConnectionContext): Boolean {
        return try {
            decode(connectionContext)
        } catch (e: IOException) {
            e.printStackTrace()
            if (client.gameState == GameState.UNK_7) {
                connectionContext.reset()
                false
            } else {
                WorldConnection.dropClient()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val basePos = Resource.mapRegion.getBaseCoordinates()
            val player = PlayerUpdate.MY_PLAYER

            val opcodes = listOf(
                connectionContext.currentPacket,
                connectionContext.secondLastPacket,
                connectionContext.thirdLastPacket
            ).joinToString(",") { it?.opcode?.toString() ?: "-1" }

            val bufferSample = connectionContext.receivedBuffer.buffer
                .take(minOf(connectionContext.currentPacketSize, 50))
                .joinToString(",")

            val debug = "$opcodes ${connectionContext.currentPacketSize},${player.movementBaseX[0] + basePos.x},${player.movementBaseY[0] + basePos.y} $bufferSample"
            PacketEncoder.sendErrorToServer(debug, e)
            client.logout(false)
            true
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun decode(context: BufferedConnectionContext): Boolean {
        val connection = context.getConnection()
        val buffer = context.receivedBuffer
        if (connection == null) return false
        if (context.currentPacket == null) {
            if (context.readPadding) {
                if (!connection.available(1)) {
                    return false
                }
                connection.read(context.receivedBuffer.buffer, 0, 1)
                context.read++
                context.idleReadPulses = 0
                context.readPadding = false
            }
            buffer.currentPosition = 0
            if (buffer.nextIsSmart()) {
                if (!connection.available(1)) {
                    return false
                }
                connection.read(context.receivedBuffer.buffer, 1, 1)
                context.read++
                context.idleReadPulses = 0
            }
            context.readPadding = true
            val opcode = buffer.readEncryptedSmart()
            val packet = ServerProt.forId(opcode)
            if (packet == null) {
                throw IOException(opcode.toString() + " " + buffer.currentPosition)
            }
            context.currentPacket = packet
            context.currentPacketSize = context.currentPacket.size
        }
        if (context.currentPacketSize == -1) {
            if (!connection.available(1)) {
                return false
            }
            connection.read(buffer.buffer, 0, 1)
            context.currentPacketSize = buffer.buffer[0].toInt() and 0xff
            context.read++
            context.idleReadPulses = 0
        }
        if (context.currentPacketSize == -2) {
            if (!connection.available(2)) {
                return false
            }
            connection.read(buffer.buffer, 0, 2)
            buffer.currentPosition = 0
            context.currentPacketSize = buffer.readUShort()
            context.read += 2
            context.idleReadPulses = 0
        }
        if (context.currentPacketSize > 0) {
            if (!connection.available(context.currentPacketSize)) {
                return false
            }
            buffer.currentPosition = 0
            connection.read(buffer.buffer, 0, context.currentPacketSize)
            context.read += context.currentPacketSize
            context.idleReadPulses = 0
        }
        context.thirdLastPacket = context.secondLastPacket
        context.secondLastPacket = context.lastPacket
        context.lastPacket = context.currentPacket

        if (context.currentPacket == ServerProt.IF_SETMODEL) {
            val componentHash = buffer.readInt()
            val primaryModelId = buffer.readUIntInverseMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcontentUpdate(componentHash, 1, primaryModelId, -1)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.PROCESS_DEV_CONSOLE_COMMAND) {
            val commandString = buffer.readString()
            CommandExecutor.executeCommand(commandString, false, false)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_INV_FULL) {
            val itemContainer = buffer.readUShort()
            val flags = buffer.readUByte()
            val isNegativeKey = (flags and 0x1) == 1
            ItemContainer.resetContainer(itemContainer, isNegativeKey)
            val size = buffer.readUShort()
            for (slot in 0..<size) {
                var amount = buffer.readUByteSubtract()
                if (amount == 255) {
                    amount = buffer.readUIntInverseMiddle()
                }
                val itemId = buffer.readUShortAddLittle()
                ItemContainer.setItemInContainer(itemContainer, slot, itemId - 1, amount, isNegativeKey)
            }
            client.RECENTLY_USED_ITEM_CONTAINERS[++client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - 1 and 0x1f] = itemContainer
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SET_DRAW_ORDER) {
            client.requestVarVerifyid()
            client.prioritizePlayerDraw = buffer.readUByteInverse() == 1
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SET_CURSOR) {
            InterfaceRenderer.translatedUptext = if (context.currentPacketSize > 2) buffer.readString() else LocalizedText.WALK_HERE.translate(client.CURRENT_LANGUAGE)
            CursorManager.activeCursorId = if (context.currentPacketSize > 0) buffer.readUShort() else -1
            if (CursorManager.activeCursorId == 65535) {
                CursorManager.activeCursorId = -1
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CAM_FORCEANGLE) {
            val angleY = buffer.readUShortAdd()
            val angleX = buffer.readUShortAddLittle()
            client.requestVarVerifyid()
            Camera.setAngle(angleX, angleY, 0)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.LOGOUT_FULL) {
            client.logout(false)
            context.currentPacket = null
            return false
        } else if (context.currentPacket == ServerProt.MAP_PROJANIM_HALFSQ) {
            decodeTilestreamPacket(UpdateZonePacket.MAP_PROJANIM_HALFSQ)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLANCHANNEL_FULL) {
            client.anInt7395 = client.anInt7347
            val nonGuest = buffer.readUByte() == 1
            if (context.currentPacketSize == 1) {
                if (nonGuest) {
                    client.CLAN_CHANNEL = null
                } else {
                    client.LISTENED_CLAN_CHANNEL = null
                }
            } else {
                if (nonGuest) {
                    client.CLAN_CHANNEL = ClanChannel(buffer)
                } else {
                    client.LISTENED_CLAN_CHANNEL = ClanChannel(buffer)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_GE_SLOT) {
            val key = buffer.readUByte()
            if (buffer.readUByte() == 0) {
                client.GRAND_EXCHANGE_SLOTS[key] = GrandExchangeSlot()
            } else {
                buffer.currentPosition--
                client.GRAND_EXCHANGE_SLOTS[key] = GrandExchangeSlot(buffer)
            }
            client.anInt7320 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETANGLE) {
            val componentHash = buffer.readInt()
            val pitch = buffer.readUShortLittle()
            val roll = buffer.readUShortLittle()
            val scale = buffer.readUShortAddLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfangleUpdate(componentHash, pitch, roll, scale)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETTEXT) {
            val text = buffer.readString()
            val componentHash = buffer.readUIntMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIftextUpdate(componentHash, text)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_OPENTOP) {
            val key3 = buffer.readUIntMiddle()
            val key4 = buffer.readUIntMiddle()
            val interfaceId = buffer.readUShortAdd()
            val type = buffer.readUByteSubtract()
            val key2 = buffer.readInt()
            val key1 = buffer.readInt()
            client.requestVarVerifyid()
            if (type == 2) {
                client.setLoading()
            }
            val xteas = intArrayOf(key1, key2, key3, key4)
            client.BASE_WINDOW_ID = interfaceId
            Interface.resetComponentSequences(interfaceId, xteas)
            WindowMode.interfaceResize(false)
            Component.runScriptsIfLoaded(client.BASE_WINDOW_ID, xteas)
            for (index in 0..106) {
                client.IS_COMPONENT_ON_TOP[index] = true
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERMODEL_OTHER) {
            val primaryModelId = buffer.readUShortAdd()
            val componentHash = buffer.readUIntMiddle()
            val secondaryModelId = buffer.readUIntMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcontentUpdate(componentHash, 5, primaryModelId, secondaryModelId)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CAM_SMOOTHRESET) {
            client.requestVarVerifyid()
            Camera.smoothReset()
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARCSTR_SMALL) {
            val string_63 = buffer.readString()
            val flags = buffer.readUShortAddLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityVarcstringUpdate(flags, string_63)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETSCROLLPOS) {
            val key = buffer.readUShort()
            val flags = buffer.readInt()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfscrollposUpdate(flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_PRIVATE) {
            val noCrown = buffer.readUByte() == 1
            val crownedName = buffer.readString()
            var name = crownedName
            if (noCrown) name = buffer.readString()
            val hashIdk1 = buffer.readUShort().toLong()
            val hashIdk2 = buffer.readUMedium().toLong()
            val rights = buffer.readUByte()
            val qcMsgId = buffer.readUShort()
            val messageUniqueHash = hashIdk2 + (hashIdk1 shl 32)
            var ignore = false
            var i_81 = 0
            while (true) {
                if (i_81 >= 100) {
                    if (rights <= 1 && IgnoreList.contains(name)) {
                        ignore = true
                    }
                    break
                }
                if (client.CHAT_MESSAGE_UNIQUE_HASHES[i_81] == messageUniqueHash) {
                    ignore = true
                    break
                }
                ++i_81
            }
            if (!ignore) {
                client.CHAT_MESSAGE_UNIQUE_HASHES[client.anInt7389] = messageUniqueHash
                client.anInt7389 = (client.anInt7389 + 1) % 100
                val qcMessage = TypeLists.QUICK_CHAT_MESSAGE.listMessage(qcMsgId).fillDynamicValues(buffer)
                if (rights == 2) {
                    ChatBox.writeMessage(ChatMessageType.PRIVATE_QUICKCHAT, 0, TextUtils.getCrown(1) + crownedName, TextUtils.getCrown(1) + name, crownedName, qcMessage, null, qcMsgId)
                } else if (rights == 1) {
                    ChatBox.writeMessage(ChatMessageType.PRIVATE_QUICKCHAT, 0, TextUtils.getCrown(0) + crownedName, TextUtils.getCrown(0) + name, crownedName, qcMessage, null, qcMsgId)
                } else {
                    ChatBox.writeMessage(ChatMessageType.PRIVATE_QUICKCHAT, 0, crownedName, name, crownedName, qcMessage, null, qcMsgId)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETCOLOR) {
            val key = buffer.readUIntLittle()
            val flags = buffer.readUShortAdd()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcolorUpdate(key, flags)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VORBIS_SOUND) {
            var soundId = buffer.readUShort()
            if (soundId == 65535) {
                soundId = -1
            }
            val repeat = buffer.readUByte()
            val delay = buffer.readUShort()
            val volume = buffer.readUByte()
            val sampleRate = buffer.readUShort()
            AreaSoundPlayer.playSoundVorbis(soundId, repeat, delay, volume, false, sampleRate)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.RUN_ENERGY) {
            client.RUN_ENERGY = buffer.readUByte()
            client.anInt7397 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERHEAD_OTHER) {
            val key = buffer.readInt()
            val flags = buffer.readUShortAddLittle()
            val i_6 = buffer.readUIntMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcontentUpdate(i_6, 3, flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SPOT_ANIM_SPECIFIC) {
            val idk = buffer.readUByteInverse()
            var reqGfxId = buffer.readUShortAdd()
            if (reqGfxId == 65535) {
                reqGfxId = -1
            }
            val settings = buffer.readUByteInverse()
            val speed = buffer.readUShortAdd()
            val targetHash = buffer.readUIntLittle()
            val height = buffer.readUShortLittle()
            val rotation = settings and 0x7
            var setting1 = settings shr 3 and 0xf
            if (setting1 == 15) {
                setting1 = -1
            }
            val setting2 = (settings shr 7 and 0x1) == 1
            if (targetHash shr 30 != 0) {
                val base = Resource.mapRegion.getBaseCoordinates()
                val fromPlane = targetHash shr 28 and 0x3
                val x = (targetHash shr 14 and 0x3fff) - base.x
                val y = (targetHash and 0x3fff) - base.y
                if (x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                    if (reqGfxId == -1) {
                        val positionedGfx = client.ACTIVE_GFX.get(x.toLong() shl 16 or y.toLong())
                        if (positionedGfx != null) {
                            positionedGfx.objectGfx.resetCycle()
                            positionedGfx.unlink()
                        }
                    } else {
                        val tileX = x * 512 + 256
                        val tileY = y * 512 + 256
                        var toPlane = fromPlane
                        if (fromPlane < 3 && Resource.mapRegion.getSettingsBits().areRoofsHidden(x, y)) {
                            toPlane = fromPlane + 1
                        }
                        val spotAnimation = SpotAnimation(Resource.mapRegion.getScene(), reqGfxId, speed, fromPlane, toPlane, tileX, MapRegion.tileheightForAbsolutePosition(tileX, tileY, fromPlane) - height, tileY, x, x, y, y, rotation, setting2)
                        client.ACTIVE_GFX.put(PositionedGfx(spotAnimation), x.toLong() shl 16 or y.toLong())
                    }
                }
            } else if (targetHash shr 29 != 0) {
                val npcId = targetHash and 0xffff
                val npcObject = client.NPC_MAP.get(npcId.toLong())
                if (npcObject != null) {
                    val npc = npcObject.value as NpcEntity
                    val npcGfx = npc.spotAnimations[idk]
                    var overwriteCurrent = true
                    val curGfxId = npcGfx.spotAnimationId
                    if (reqGfxId != -1 && curGfxId != -1) {
                        if (reqGfxId == curGfxId) {
                            val reqGfx = TypeLists.SPOT_ANIM.list(reqGfxId)
                            if (reqGfx.replay && reqGfx.sequenceId != -1) {
                                val reqGfxSeq = TypeLists.SEQ.list(reqGfx.sequenceId)
                                val reqLoopType = reqGfxSeq.loopType
                                if (reqLoopType != 0 && reqLoopType != 2) {
                                    if (reqLoopType == 1) {
                                    }
                                } else {
                                    overwriteCurrent = false
                                }
                            }
                        } else {
                            val reqGfx = TypeLists.SPOT_ANIM.list(reqGfxId)
                            val curGfx = TypeLists.SPOT_ANIM.list(curGfxId)
                            if (reqGfx.sequenceId != -1 && curGfx.sequenceId != -1) {
                                val reqSeq = TypeLists.SEQ.list(reqGfx.sequenceId)
                                val curSeq = TypeLists.SEQ.list(curGfx.sequenceId)
                                if (reqSeq.priority < curSeq.priority) {
                                    overwriteCurrent = false
                                }
                            }
                        }
                    }
                    if (overwriteCurrent) {
                        npcGfx.spotAnimationId = reqGfxId
                        npcGfx.height = height
                        npcGfx.adjustmentType = setting1
                        if (reqGfxId != -1) {
                            val reqGfx = TypeLists.SPOT_ANIM.list(reqGfxId)
                            var replayMode = if (reqGfx.replay) 0 else 2
                            if (setting2) {
                                replayMode = 1
                            }
                            npcGfx.sequence.animateSequence(reqGfx.sequenceId, speed, replayMode, false)
                        } else {
                            npcGfx.sequence.update(-1)
                        }
                    }
                }
            } else if (targetHash shr 28 != 0) {
                val i_35 = targetHash and 0xffff
                val player117: PlayerEntity?
                if (i_35 == PlayerUpdate.myPlayerIndex) {
                    player117 = PlayerUpdate.MY_PLAYER
                } else {
                    player117 = PlayerUpdate.players[i_35]
                }
                if (player117 != null) {
                    val class161121 = player117.spotAnimations[idk]
                    var bool16 = true
                    val i_34 = class161121.spotAnimationId
                    if (reqGfxId != -1 && i_34 != -1) {
                        if (i_34 == reqGfxId) {
                            val spotanimdefinitions125 = TypeLists.SPOT_ANIM.list(reqGfxId)
                            if (spotanimdefinitions125.replay && spotanimdefinitions125.sequenceId != -1) {
                                val animationdefinitions124 = TypeLists.SEQ.list(spotanimdefinitions125.sequenceId)
                                val i_40 = animationdefinitions124.loopType
                                if (i_40 != 0 && i_40 != 2) {
                                    if (i_40 == 1) {
                                    }
                                } else {
                                    bool16 = false
                                }
                            }
                        } else {
                            val spotanimdefinitions125 = TypeLists.SPOT_ANIM.list(reqGfxId)
                            val spotanimdefinitions38 = TypeLists.SPOT_ANIM.list(i_34)
                            if (spotanimdefinitions125.sequenceId != -1 && spotanimdefinitions38.sequenceId != -1) {
                                val animationdefinitions86 = TypeLists.SEQ.list(spotanimdefinitions125.sequenceId)
                                val animationdefinitions123 = TypeLists.SEQ.list(spotanimdefinitions38.sequenceId)
                                if (animationdefinitions86.priority < animationdefinitions123.priority) {
                                    bool16 = false
                                }
                            }
                        }
                    }
                    if (bool16) {
                        class161121.spotAnimationId = reqGfxId
                        class161121.height = height
                        class161121.adjustmentType = setting1
                        class161121.rotation = rotation
                        if (reqGfxId != -1) {
                            val spotanimdefinitions125 = TypeLists.SPOT_ANIM.list(reqGfxId)
                            var i_83 = if (spotanimdefinitions125.replay) 0 else 2
                            if (setting2) {
                                i_83 = 1
                            }
                            class161121.sequence.animateSequence(spotanimdefinitions125.sequenceId, speed, i_83, false)
                        } else {
                            class161121.sequence.update(-1)
                        }
                    }
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CAM_LOOKAT) {
            val viewX = buffer.readUByte()
            val viewY = buffer.readUShortAdd() shl 2
            val viewZ = buffer.readUByteAdd()
            val i_7 = buffer.readUByteInverse()
            val i_8 = buffer.readUByteSubtract()
            client.requestVarVerifyid()
            Camera.lookat(viewX, viewZ, viewY, i_8, i_7)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.aClass3754368) {
            val key = buffer.readUByte()
            val flags = buffer.readUShortAdd()
            client.requestVarVerifyid()
            VorbisSoundManager.method1575(flags, key, true)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.GAME_MESSAGE) {
            val type = ChatMessageType.forId(buffer.readUSmart())
            val effectFlags = buffer.readInt()
            val flags = buffer.readUByte()
            var nameSimple = ""
            var nameFormatted = nameSimple
            if ((flags and 0x1) != 0) {
                nameSimple = buffer.readString()
                if ((flags and 0x2) != 0) {
                    nameFormatted = buffer.readString()
                } else {
                    nameFormatted = nameSimple
                }
            }
            val message = buffer.readString()
            if (type == ChatMessageType.DEV_CONSOLE) {
                Console.writeLine(message)
            } else if (type == ChatMessageType.DEV_CONSOLE_CLEAR) {
                Console.setConsoleText(message)
            } else {
                if (!nameFormatted.isEmpty() && IgnoreList.contains(nameFormatted)) {
                    context.currentPacket = null
                    return true
                }
                ChatBox.writeMessage(type, effectFlags, nameSimple, nameFormatted, nameSimple, message)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.OBJECT_PREFETCH) {
            decodeTilestreamPacket(UpdateZonePacket.OBJECT_PREFETCH)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.OPEN_URL) {
            if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false)
            val bytes = ByteArray(context.currentPacketSize - 1)
            val bool66 = buffer.readUByte() == 1
            buffer.readBytes(bytes, context.currentPacketSize - 1)
            val rsbytebuffer127 = Packet(bytes)
            val url = rsbytebuffer127.readString()
            if (bool66) {
                var string_137 = rsbytebuffer127.readString()
                if (string_137.isEmpty()) string_137 = url
                if (!client.isApplet || SystemInfo.operatingSystemLowercase.startsWith("mac") || !Browser.openBrowser(url, 1, JSObjectManager.SERVER_PACKET_URL.getCallName())) Browser.openOpenjs(string_137, true, client.aBool7158)
            } else Browser.openOpenjs(url, true, client.aBool7158)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CAM_SHAKE) {
            val key = buffer.readUByteSubtract()
            val flags = buffer.readUShort()
            val i_6 = buffer.readUByteInverse()
            val i_7 = buffer.readUByteInverse()
            val i_8 = buffer.readUByteSubtract()
            client.requestVarVerifyid()
            client.CAM_SHAKING[i_6] = true
            client.anIntArray7268[i_6] = i_7
            client.CUSTOM_LOWEST_YAW[i_6] = key
            client.anIntArray7462[i_6] = i_8
            client.anIntArray7435[i_6] = flags
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_PLAYER_GROUP) {
            val bool91 = buffer.readUByte() == 1
            val string_88 = buffer.readString()
            var str92 = string_88
            if (bool91) {
                str92 = buffer.readString()
            }
            val i_7 = buffer.readUByte()
            var ignore = false
            if (i_7 <= 1) {
                if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                    if (IgnoreList.contains(str92)) {
                        ignore = true
                    }
                } else {
                    ignore = true
                }
            }
            if (!ignore) {
                val message = TextUtils.atLtGtTags(HuffmanList.decode(buffer))
                if (i_7 == 2) {
                    ChatBox.writeMessage(ChatMessageType.GROUP_CHAT, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, message, null, -1)
                } else if (i_7 == 1) {
                    ChatBox.writeMessage(ChatMessageType.GROUP_CHAT, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, message, null, -1)
                } else {
                    ChatBox.writeMessage(ChatMessageType.GROUP_CHAT, 0, string_88, str92, string_88, message, null, -1)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_PLAYER_GROUP) {
            val hasRights = buffer.readUByte() == 1
            val username = buffer.readString()
            var crownedUsername = username
            if (hasRights) {
                crownedUsername = buffer.readString()
            }
            val rights = buffer.readUByte()
            val msgId = buffer.readUShort()
            val ignore = rights <= 1 && IgnoreList.contains(crownedUsername)
            if (!ignore) {
                val message = TypeLists.QUICK_CHAT_MESSAGE.listMessage(msgId).fillDynamicValues(buffer)
                if (rights == 2) {
                    ChatBox.writeMessage(ChatMessageType.GROUP_QUICKCHAT, 0, TextUtils.getCrown(1) + username, TextUtils.getCrown(1) + crownedUsername, username, message, null, msgId)
                } else if (rights == 1) {
                    ChatBox.writeMessage(ChatMessageType.GROUP_QUICKCHAT, 0, TextUtils.getCrown(0) + username, TextUtils.getCrown(0) + crownedUsername, username, message, null, msgId)
                } else {
                    ChatBox.writeMessage(ChatMessageType.GROUP_QUICKCHAT, 0, username, crownedUsername, username, message, null, msgId)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CHAT_FILTER_SETTINGS_PRIVATECHAT) {
            ChatBox.PRIVATE_FILTER = ChatFilter.forId(buffer.readUByte())
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.ANIMATE_NPC) {
            val npcIndex = buffer.readUShortAddLittle()
            val animationIds = IntArray(MovementType.entries.size)
            for (i_6 in MovementType.entries.toTypedArray().indices) {
                animationIds[i_6] = buffer.readUIntLittle()
            }
            val speed = buffer.readUByte()
            val class282Sub47131 = client.NPC_MAP.get(npcIndex.toLong())
            if (class282Sub47131 != null) {
                PlayerUpdate.animate(class282Sub47131.value as PathingEntity?, animationIds, speed, true)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETPOSITION) {
            val y = buffer.readShortAdd()
            val component = buffer.readUIntInverseMiddle()
            val x = buffer.readShortAddLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfpositionUpdate(component, x, y)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.DYNAMIC_MAP_REGION) {
            val bitBuffer = Bit(context.currentPacketSize)
            System.arraycopy(context.receivedBuffer.buffer, context.receivedBuffer.currentPosition, bitBuffer.buffer, 0, context.currentPacketSize)
            client.resetCutscenes()
            if (PreferenceManager.preferences.asyncMaploading.getValue() == 1) {
                Resource.mapRegionLoaderTask.requestMapUpdate(RegionLoadRequest(RegionLoadingType.INSTANCE, bitBuffer))
            } else {
                Resource.mapRegion.initialize(RegionLoadRequest(RegionLoadingType.INSTANCE, bitBuffer))
            }
            context.currentPacket = null
            return false
        } else if (context.currentPacket == ServerProt.DEPRECATED_52_CLIENTPACKET_97) {
            client.aByte7458 = buffer.readByte()
            if (client.aByte7458.toInt() == 0 || client.aByte7458.toInt() == 1) {
                client.aBool7323 = true
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETNPCHEAD) {
            val key = buffer.readUIntLittle()
            val flags = buffer.readUIntMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcontentUpdate(key, 2, flags, -1)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_PUBLIC) {
            val playerIndex = buffer.readUShort()
            val player: PlayerEntity?
            if (playerIndex == PlayerUpdate.myPlayerIndex) {
                player = PlayerUpdate.MY_PLAYER
            } else {
                player = PlayerUpdate.players[playerIndex]
            }
            if (player != null) {
                var chatEffects = buffer.readUShort()
                val icon = buffer.readUByte()
                val isQuickchat = (chatEffects and 0x8000) != 0
                if (player.displayName != null && player.playerAppearance != null) {
                    var bool69 = false
                    if (icon <= 1) {
                        if (!isQuickchat && (client.USERDETAIL_QUICKCHAT && !client.VERIFIED_EMAIL_ADDRESS || client.IS_QUICKCHAT_ONLY)) {
                            bool69 = true
                        } else if (IgnoreList.contains(player.displayName)) {
                            bool69 = true
                        }
                    }
                    if (!bool69) {
                        var qcMessageId = -1
                        val message: String
                        if (isQuickchat) {
                            chatEffects = chatEffects and 0x7fff
                            val quickchatMessage = QuickchatMessageTypeList.decodeQuickchatMessage(buffer)
                            qcMessageId = quickchatMessage.id
                            message = quickchatMessage.message.fillDynamicValues(buffer)
                        } else {
                            message = TextUtils.atLtGtTags(HuffmanList.decode(buffer))
                        }
                        player.sendChat(message.trim { it <= ' ' }, chatEffects shr 8, chatEffects and 0xff)
                        val messageType: ChatMessageType?
                        if (icon != 1 && icon != 2) {
                            messageType = if (isQuickchat) ChatMessageType.PUBLIC_QUICKCHAT else ChatMessageType.PUBLIC_CHAT
                        } else {
                            messageType = if (isQuickchat) ChatMessageType.PUBLIC_QUICKCHAT else ChatMessageType.STAFF_CHAT
                        }
                        if (icon == 2) {
                            ChatBox.writeMessage(messageType, 0, TextUtils.getCrown(1) + player.getUsernameWithTitle(), TextUtils.getCrown(1) + player.getDisplayName(), player.username, message, null, qcMessageId)
                        } else if (icon == 1) {
                            ChatBox.writeMessage(messageType, 0, TextUtils.getCrown(0) + player.getUsernameWithTitle(), TextUtils.getCrown(0) + player.getDisplayName(), player.username, message, null, qcMessageId)
                        } else {
                            ChatBox.writeMessage(messageType, 0, player.getUsernameWithTitle(), player.getDisplayName(), player.username, message, null, qcMessageId)
                        }
                    }
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.QUICK_HOP_WORLDS) {
            val port = buffer.readUShort()
            val host = buffer.readString()
            val reconnecting = buffer.readUByte() == 1
            WorldConnection.connectionInfo = LoginManager.GAME_CONNECTION_INFO
            WorldConnection.reconnectingWorld = reconnecting
            LoginManager.setGameHost(port, host)
            client.setGameState(GameState.UNK_17)
            context.currentPacket = null
            return false
        } else if (context.currentPacket == ServerProt.LOGOUT_LOBBY) {
            client.logout(LoginManager.currentlyInLobby)
            context.currentPacket = null
            return false
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARC_LARGE) {
            val key = buffer.readUShortLittle()
            val flags = buffer.readUIntInverseMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityClientvarcUpdate(key, flags)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_GROUNDITEM) {
            val interfaceId = buffer.readUIntInverseMiddle()
            val xtea3 = buffer.readUIntLittle()
            val subInterfaceId = buffer.readUShortLittle()
            val xtea4 = buffer.readInt()
            val xtea1 = buffer.readUIntMiddle()
            val overlay = buffer.readUByteSubtract()
            val absolutePosition = CoordGrid(buffer.readUIntMiddle())
            val xtea2 = buffer.readUIntMiddle()
            val itemId = buffer.readUShortAddLittle()
            client.requestVarVerifyid()
            Interface.openSub(interfaceId, IFSubActiveGroundItem(subInterfaceId, overlay, GroundItemFetcher(absolutePosition, itemId)), intArrayOf(xtea1, xtea2, xtea3, xtea4), false)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.FRIENDS_CHAT_CHANNEL) {
            client.anInt7179 = client.anInt7347
            if (context.currentPacketSize == 0) {
                FriendsChat.FC_NAME = null
                FriendsChat.FC_OWNER_NAME = null
                FriendsChat.FC_PLAYER_COUNT = 0
                FriendsChat.FC_PLAYERS = null
                context.currentPacket = null
                return true
            } else {
                FriendsChat.FC_OWNER_NAME = buffer.readString()
                val isOwner = buffer.readUByte() == 1
                if (isOwner) {
                    buffer.readString()
                }
                val encodedName = buffer.readLong()
                FriendsChat.FC_NAME = Utils.getStringFromLong(encodedName)
                FriendsChat.FC_MIN_RANK_CAN_KICK = buffer.readByte()
                val playerCount = buffer.readUByte()
                if (playerCount != 255) {
                    FriendsChat.FC_PLAYER_COUNT = playerCount
                    val friendsChatPlayers = arrayOfNulls<FriendsChatPlayer>(100)
                    for (index in 0..<FriendsChat.FC_PLAYER_COUNT) {
                        friendsChatPlayers[index] = FriendsChatPlayer()
                        friendsChatPlayers[index]!!.displayName = buffer.readString()
                        val changedName = buffer.readUByte() == 1
                        if (changedName) {
                            friendsChatPlayers[index]!!.username = buffer.readString()
                        } else {
                            friendsChatPlayers[index]!!.username = friendsChatPlayers[index]!!.displayName
                        }
                        friendsChatPlayers[index]!!.textName = TextUtils.formatDisplayname(friendsChatPlayers[index]!!.username)
                        friendsChatPlayers[index]!!.worldId = buffer.readUShort()
                        friendsChatPlayers[index]!!.rank = buffer.readByte()
                        friendsChatPlayers[index]!!.worldName = buffer.readString()
                        if (friendsChatPlayers[index]!!.username == PlayerUpdate.MY_PLAYER.displayName) {
                            FriendsChat.MY_PLAYER_FC_RANK = friendsChatPlayers[index]!!.rank
                        }
                    }
                    var bool69 = false
                    var total = FriendsChat.FC_PLAYER_COUNT
                    while (total > 0) {
                        bool69 = true
                        --total
                        for (index in 0..<total) {
                            if (friendsChatPlayers[index]!!.textName.compareTo(friendsChatPlayers[index + 1]!!.textName) > 0) {
                                val tmp = friendsChatPlayers[index]
                                friendsChatPlayers[index] = friendsChatPlayers[index + 1]
                                friendsChatPlayers[index + 1] = tmp
                                bool69 = false
                            }
                        }
                        if (bool69) {
                            break
                        }
                    }
                    FriendsChat.FC_PLAYERS = friendsChatPlayers
                }
                context.currentPacket = null
                return true
            }
        } else if (context.currentPacket == ServerProt.CAM_RESET) {
            client.requestVarVerifyid()
            Camera.method12606()
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SET_CLAN_STRING) {
            val key = buffer.readUShort()
            val string_88 = buffer.readString()
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = arrayOfNulls<Any>(TypeLists.CLAN_VAR.size)
            }
            ClanVarTypeList.CLAN_VARS[key] = string_88
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 and 0x1f] = key
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MUSIC_TRACK) {
            val key = buffer.readUByteAdd()
            val flags = buffer.readUByteAdd()
            var i_6 = buffer.readUShortAdd()
            if (i_6 == 65535) {
                i_6 = -1
            }
            SongPlayer.playSong(i_6, flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETCLICKMASK) {
            val bool91 = buffer.readUByteSubtract() == 1
            val flags = buffer.readUIntLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfclickmaskUpdate(flags, bool91)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MUSIC_EFFECT) {
            val key = buffer.readUMediumAlternateOrder()
            val flags = buffer.readUByteSubtract()
            var i_6 = buffer.readUShortLittle()
            if (i_6 == 65535) {
                i_6 = -1
            }
            SoundEffectPlayer.playSoundJingle(i_6, flags)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CHAT_FILTER_SETTINGS) {
            ChatBox.TRADE_FILTER = buffer.readUByteInverse()
            ChatBox.PUBLIC_FILTER = buffer.readUByteAdd()
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLEAR_VARPS) {
            VarProviders.PLAYER_VAR_PROVIDER.reset()
            client.pulseEventConfigsPtr += 32
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.TILE_MESSAGE) {
            decodeTilestreamPacket(UpdateZonePacket.TILE_MESSAGE)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.PLAYER_WEIGHT) {
            client.RUN_WEIGHT = buffer.readShort()
            client.anInt7397 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.aClass3754399) {
            val key = buffer.readUIntLittle()
            client.requestVarVerifyid()
            if (key == -1) {
                PingRequester.anInt5864 = -1
                StaticMethods.anInt833 = -1
            } else {
                val coordgrid114 = Resource.mapRegion.getBaseCoordinates()
                var i_6 = key shr 14 and 0x3fff
                var i_7 = key and 0x3fff
                i_6 -= coordgrid114.x
                if (i_6 < 0) {
                    i_6 = 0
                } else if (i_6 >= Resource.mapRegion.getSceneSizeX()) {
                    i_6 = Resource.mapRegion.getSceneSizeX()
                }
                i_7 -= coordgrid114.y
                if (i_7 < 0) {
                    i_7 = 0
                } else if (i_7 >= Resource.mapRegion.getSceneSizeY()) {
                    i_7 = Resource.mapRegion.getSceneSizeY()
                }
                PingRequester.anInt5864 = (i_6 shl 9) + 256
                StaticMethods.anInt833 = (i_7 shl 9) + 256
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.RESET_ALL_ANIMATIONS) {
            for (key in PlayerUpdate.players.indices) {
                if (PlayerUpdate.players[key] != null) {
                    PlayerUpdate.players[key].currentAnimations = null
                    PlayerUpdate.players[key].currentSequence.update(-1)
                }
            }
            for (key in 0..<client.npcCount) {
                (client.NPC_ARRAY[key].value as PathingEntity).currentAnimations = null
                (client.NPC_ARRAY[key].value as PathingEntity).currentSequence.update(-1)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_CLANCHANNEL) {
            val nonGuest = buffer.readUByte() == 1
            val string_88 = buffer.readString()
            val long_49 = buffer.readUShort().toLong()
            val long_51 = buffer.readUMedium().toLong()
            val i_10 = buffer.readUByte()
            val i_11 = buffer.readUShort()
            val long_53 = long_51 + (long_49 shl 32)
            var bool14 = false
            val class282Sub4102 = if (nonGuest) client.CLAN_CHANNEL else client.LISTENED_CLAN_CHANNEL
            if (class282Sub4102 == null) {
                bool14 = true
            } else {
                for (i_34 in 0..99) {
                    if (long_53 == client.CHAT_MESSAGE_UNIQUE_HASHES[i_34]) {
                        bool14 = true
                        break
                    }
                }
                if (i_10 <= 1 && IgnoreList.contains(string_88))
                    bool14 = true
            }
            if (!bool14) {
                client.CHAT_MESSAGE_UNIQUE_HASHES[client.anInt7389] = long_53
                client.anInt7389 = (client.anInt7389 + 1) % 100
                val string_17 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_11).fillDynamicValues(buffer)
                val messageType = if (nonGuest) ChatMessageType.CLAN_QUICKCHAT else ChatMessageType.GUEST_CLAN_QUICKCHAT
                if (i_10 != 2 && i_10 != 3) {
                    if (i_10 == 1) {
                        ChatBox.writeMessage(messageType, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + string_88, string_88, string_17, class282Sub4102!!.clanName, i_11)
                    } else {
                        ChatBox.writeMessage(messageType, 0, string_88, string_88, string_88, string_17, class282Sub4102!!.clanName, i_11)
                    }
                } else {
                    ChatBox.writeMessage(messageType, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + string_88, string_88, string_17, class282Sub4102!!.clanName, i_11)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARCLAN_SET_LONG) {
            val key = buffer.readUShort()
            val long_47 = buffer.readLong()
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = arrayOfNulls<Any>(TypeLists.CLAN_VAR.size)
            }
            ClanVarTypeList.CLAN_VARS[key] = long_47
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 and 0x1f] = key
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_FRIENDS_CHAT) {
            val bool91 = buffer.readUByte() == 1
            val string_88 = buffer.readString()
            var str92 = string_88
            if (bool91) {
                str92 = buffer.readString()
            }
            val long_28 = buffer.readLong()
            val long_30 = buffer.readUShort().toLong()
            val long_55 = buffer.readUMedium().toLong()
            val i_35 = buffer.readUByte()
            val i_77 = buffer.readUShort()
            val long_57 = (long_30 shl 32) + long_55
            var bool115 = false
            var i_82 = 0
            while (true) {
                if (i_82 >= 100) {
                    if (i_35 <= 1 && IgnoreList.contains(str92)) {
                        bool115 = true
                    }
                    break
                }
                if (client.CHAT_MESSAGE_UNIQUE_HASHES[i_82] == long_57) {
                    bool115 = true
                    break
                }
                ++i_82
            }
            if (!bool115) {
                client.CHAT_MESSAGE_UNIQUE_HASHES[client.anInt7389] = long_57
                client.anInt7389 = (client.anInt7389 + 1) % 100
                val string_41 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_77).fillDynamicValues(buffer)
                if (i_35 == 2) {
                    ChatBox.writeMessage(ChatMessageType.FC_QUICKCHAT, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, string_41, TextUtils.decodeUidName(long_28), i_77)
                } else if (i_35 == 1) {
                    ChatBox.writeMessage(ChatMessageType.FC_QUICKCHAT, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, string_41, TextUtils.decodeUidName(long_28), i_77)
                } else {
                    ChatBox.writeMessage(ChatMessageType.FC_QUICKCHAT, 0, string_88, str92, string_88, string_41, TextUtils.decodeUidName(long_28), i_77)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_QUICKCHAT_PRIVATE_ECHO) {
            val string_63 = buffer.readString()
            val flags = buffer.readUShort()
            val str92 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(flags).fillDynamicValues(buffer)
            ChatBox.writeMessage(ChatMessageType.PRIVATE_QUICKCHAT_ECHO, 0, string_63, string_63, string_63, str92, null, flags)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARCLAN_SET_INT) {
            val key = buffer.readUShort()
            val flags = buffer.readInt()
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = arrayOfNulls<Any>(TypeLists.CLAN_VAR.size)
            }
            ClanVarTypeList.CLAN_VARS[key] = flags
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 and 0x1f] = key
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETTARGETPARAM) {
            var toSlot = buffer.readUShortAddLittle()
            if (toSlot == 65535) {
                toSlot = -1
            }
            val interfaceHash = buffer.readUIntInverseMiddle()
            var fromSlot = buffer.readUShort()
            if (fromSlot == 65535) {
                fromSlot = -1
            }
            val settings = buffer.readUIntLittle()
            client.requestVarVerifyid()
            for (slot in fromSlot..toSlot) {
                val slots = slot + (interfaceHash.toLong() shl 32)
                val currentSettings = client.ICOMPONENT_SETTINGS_SLOTS.get(slots) as IFEvents?
                val newSettings: IFEvents?
                if (currentSettings == null) {
                    if (slot == -1) {
                        newSettings = IFEvents(settings, Component.fromHash(interfaceHash).targetParams.interfaceId)
                    } else {
                        newSettings = IFEvents(settings, -1)
                    }
                } else {
                    newSettings = IFEvents(settings, currentSettings.interfaceId)
                    currentSettings.unlink()
                }
                client.ICOMPONENT_SETTINGS_SLOTS.put(newSettings, slots)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_RESETTARGETPARAM) {
            val interfaceHash = buffer.readUIntMiddle()
            val interfaceId = buffer.readUShortAddLittle()
            var toSlot = buffer.readUShort()
            if (toSlot == 65535) {
                toSlot = -1
            }
            var fromSlot = buffer.readUShortAdd()
            if (fromSlot == 65535) {
                fromSlot = -1
            }
            client.requestVarVerifyid()
            for (slot in fromSlot..toSlot) {
                val slots = slot + (interfaceHash.toLong() shl 32)
                val currentSettings = client.ICOMPONENT_SETTINGS_SLOTS.get(slots) as IFEvents?
                val newSettings: IFEvents?
                if (currentSettings == null) {
                    if (slot == -1) {
                        newSettings = IFEvents(Component.fromHash(interfaceHash).targetParams.settingsHash, interfaceId)
                    } else {
                        newSettings = IFEvents(0, interfaceId)
                    }
                } else {
                    newSettings = IFEvents(currentSettings.settingsHash, interfaceId)
                    currentSettings.unlink()
                }
                client.ICOMPONENT_SETTINGS_SLOTS.put(newSettings, slots)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.HINT_TRAIL) {
            val idx = buffer.readUByte()
            val modelId = buffer.readBigSmart()
            if (client.HINT_TRAILS[idx] != null) {
                client.HINT_TRAILS[idx].removeFromScene(Resource.mapRegion.getScene())
                client.HINT_TRAILS[idx] = null
            }
            if (modelId != -1) {
                client.HINT_TRAILS[idx] = HintTrail(Renderers.activeToolkit, buffer, modelId)
                client.HINT_TRAILS[idx].addTo(Resource.mapRegion.getScene())
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.RESET_SOUNDS) {
            AreaSoundPlayer.resetSounds()
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.REQUEST_FPS) {
            val key = buffer.readInt()
            val flags = buffer.readInt()
            val tcpmessage111 = PacketEncoder.createPacket(ClientProt.SEND_FPS, context.isaac)
            tcpmessage111.buffer.writeIntInverseMiddle(key)
            tcpmessage111.buffer.writeIntMiddle(flags)
            tcpmessage111.buffer.writeByteSubtract(client.FPS)
            context.queuePacket(tcpmessage111)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.OBJ_ANIM) {
            decodeTilestreamPacket(UpdateZonePacket.OBJ_ANIM)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARCLAN_SET_BYTE) {
            val key = buffer.readUShort()
            val b_84 = buffer.readByte()
            if (ClanVarTypeList.CLAN_VARS == null) {
                ClanVarTypeList.CLAN_VARS = arrayOfNulls<Any>(TypeLists.CLAN_VAR.size)
            }
            ClanVarTypeList.CLAN_VARS[key] = b_84.toInt()
            client.CLAN_VAR_KEYS[++client.CLAN_VAR_COUNTER - 1 and 0x1f] = key
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.GROUND_ITEM_REVEAL) {
            decodeTilestreamPacket(UpdateZonePacket.GROUND_ITEM_REVEAL)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_INV_STOP_TRANSMIT) {
            val key = buffer.readUByteInverse()
            val flags = buffer.readUShortLittle()
            val isNegativeKey = (key and 0x1) == 1
            ItemContainer.deleteItemContainer(flags, isNegativeKey)
            client.RECENTLY_USED_ITEM_CONTAINERS[++client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - 1 and 0x1f] = flags
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_REBOOT_TIMER) {
            if (GameState.inLobby(client.gameState)) {
                client.REBOOT_TIMER = (buffer.readUShort() * 2.5f).toInt()
            } else {
                client.REBOOT_TIMER = buffer.readUShort() * 30
            }
            client.anInt7397 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MIDI_SONG_LOCATION) {
            decodeTilestreamPacket(UpdateZonePacket.MIDI_SONG_LOCATION)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_ZONE_PARTIAL_FOLLOWS) {
            UpdateZonePacket.UPDATE_ZONE_Y = buffer.readByteAdd().toInt() shl 3
            UpdateZonePacket.UPDATE_ZONE_PLANE = buffer.readUByteAdd()
            UpdateZonePacket.UPDATE_ZONE_X = buffer.readByteAdd().toInt() shl 3
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_ZONE_PARTIAL_ENCLOSED) {
            UpdateZonePacket.UPDATE_ZONE_Y = buffer.readByteSubtract().toInt() shl 3
            UpdateZonePacket.UPDATE_ZONE_PLANE = buffer.readUByteAdd()
            UpdateZonePacket.UPDATE_ZONE_X = buffer.readByte().toInt() shl 3
            while (buffer.currentPosition < context.currentPacketSize) {
                val tilestreampacket68: UpdateZonePacket? = UpdateZonePacket.entries[buffer.readUByte()]
                decodeTilestreamPacket(tilestreampacket68)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_ZONE_FULL_FOLLOWS) {
            UpdateZonePacket.UPDATE_ZONE_X = buffer.readByteSubtract().toInt() shl 3
            UpdateZonePacket.UPDATE_ZONE_Y = buffer.readByte().toInt() shl 3
            UpdateZonePacket.UPDATE_ZONE_PLANE = buffer.readUByte()
            val base = Resource.mapRegion.getBaseCoordinates()
            var groundItems = client.GROUND_ITEMS.first()
            while (groundItems != null) {
                val plane = (groundItems.pointer shr 28 and 0x3L).toInt()
                val absX = (groundItems.pointer and 0x3fffL).toInt()
                val x = absX - base.x
                val absY = (groundItems.pointer shr 14 and 0x3fffL).toInt()
                val y = absY - base.y
                if (plane == UpdateZonePacket.UPDATE_ZONE_PLANE && x >= UpdateZonePacket.UPDATE_ZONE_X && x < UpdateZonePacket.UPDATE_ZONE_X + 8 && y >= UpdateZonePacket.UPDATE_ZONE_Y && y < UpdateZonePacket.UPDATE_ZONE_Y + 8) {
                    groundItems.unlink()
                    if (x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                        GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y)
                    }
                }
                groundItems = client.GROUND_ITEMS.next()
            }
            var locAction: LocAction?
            locAction = LocAction.pendingRemovals.getFirst() as LocAction?
            while (locAction != null) {
                if (locAction.x >= UpdateZonePacket.UPDATE_ZONE_X && locAction.x < UpdateZonePacket.UPDATE_ZONE_X + 8 && locAction.y >= UpdateZonePacket.UPDATE_ZONE_Y && locAction.y < UpdateZonePacket.UPDATE_ZONE_Y + 8 && locAction.plane == UpdateZonePacket.UPDATE_ZONE_PLANE) {
                    locAction.inUpdateZone = true
                }
                locAction = LocAction.pendingRemovals.next() as LocAction?
            }
            locAction = LocAction.pendingCustomizations.getFirst() as LocAction?
            while (locAction != null) {
                if (locAction.x >= UpdateZonePacket.UPDATE_ZONE_X && locAction.x < UpdateZonePacket.UPDATE_ZONE_X + 8 && locAction.y >= UpdateZonePacket.UPDATE_ZONE_Y && locAction.y < UpdateZonePacket.UPDATE_ZONE_Y + 8 && locAction.plane == UpdateZonePacket.UPDATE_ZONE_PLANE) {
                    locAction.inUpdateZone = true
                }
                locAction = LocAction.pendingCustomizations.next() as LocAction?
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SOUND_SYNTH) {
            var soundId = buffer.readUShort()
            if (soundId == 65535) {
                soundId = -1
            }
            val flags = buffer.readUByte()
            val i_6 = buffer.readUShort()
            val delay = buffer.readUByte()
            val i_8 = buffer.readUShort()
            AreaSoundPlayer.playSoundSynth(soundId, flags, i_6, delay, i_8)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SHOW_FACE_HERE) {
            val showFaceHere = buffer.readUByte() == 1
            client.requestVarVerifyid()
            MiniMenuBuilder.SHOW_FACE_HERE = showFaceHere
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_UID192) {
            buffer.currentPosition += 28
            if (buffer.checkCrc()) GameShell.updateRandomDatUID192(buffer, buffer.currentPosition - 28)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.HINT_ARROW) {
            val flags = buffer.readUByte()
            val iconIndex = flags shr 5
            val targetType = flags and 0x1f
            if (targetType == 0) {
                PlayerUpdate.HINT_ARROWS[iconIndex] = null
            } else {
                val arrow = HintArrow()
                arrow.targetType = targetType
                arrow.arrowSprite = buffer.readUByte()
                if (arrow.arrowSprite >= 0 && arrow.arrowSprite < StaticMedia.hintHeadiconSprites.size) {
                    if (arrow.targetType != 1 && arrow.targetType != 10) {
                        if (arrow.targetType <= 6) {
                            if (arrow.targetType == 2) {
                                arrow.x = 256
                                arrow.y = 256
                            }
                            if (arrow.targetType == 3) {
                                arrow.x = 0
                                arrow.y = 256
                            }
                            if (arrow.targetType == 4) {
                                arrow.x = 512
                                arrow.y = 256
                            }
                            if (arrow.targetType == 5) {
                                arrow.x = 256
                                arrow.y = 0
                            }
                            if (arrow.targetType == 6) {
                                arrow.x = 256
                                arrow.y = 512
                            }
                            arrow.targetType = 2
                            arrow.plane = buffer.readUByte()
                            val grid = Resource.mapRegion.getBaseCoordinates()
                            arrow.x += buffer.readUShort() - grid.x shl 9
                            arrow.y += buffer.readUShort() - grid.y shl 9
                            arrow.height = buffer.readUByte() shl 2
                            arrow.distance = buffer.readUShort()
                        }
                    } else {
                        arrow.targetIndex = buffer.readUShort()
                        arrow.denominator = buffer.readUShort()
                        buffer.currentPosition += 4
                    }
                    arrow.modelId = buffer.readInt()
                    PlayerUpdate.HINT_ARROWS[iconIndex] = arrow
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.PROJANIM_SPECIFIC) {
            var endHeight = buffer.readUByteAdd() * 4
            val source = buffer.readShortAddLittle()
            val endTime = buffer.readUShortLittle()
            val lockOn = buffer.readShort()
            val flags = buffer.readUByteAdd()
            val y = buffer.readUShortAdd()
            var slope = buffer.readUShort()
            val startTime = buffer.readUShortAddLittle()
            val spotAnimId = buffer.readUShortAddLittle()
            var angle = buffer.readUByteAdd()
            if (angle == 255) {
                angle = -1
            }
            val xDist = buffer.readByteInverse()
            val yDist = buffer.readByteInverse()
            val x = buffer.readUShortAdd()
            var startHeight = buffer.readUByteInverse()
            val useFloorHeight = (flags and 0x1) != 0
            val hasBASOffIdx = (flags and 0x2) != 0
            val basOffIdx = if (hasBASOffIdx) flags shr 2 else -1
            if (hasBASOffIdx) {
                startHeight = startHeight.toByte().toInt()
            } else {
                startHeight *= 4
            }
            val base = Resource.mapRegion.getBaseCoordinates()
            var localX = x - base.x * 2
            var localY = y - base.y * 2
            var xOff = xDist + localX
            var yOff = yDist + localY
            if (localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() * 2 && localY < Resource.mapRegion.getSceneSizeX() * 2 && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeY() * 2 && yOff < Resource.mapRegion.getSceneSizeY() * 2 && spotAnimId != 65535) {
                localX *= 256
                localY *= 256
                xOff *= 256
                yOff *= 256
                startHeight = startHeight shl 2
                endHeight = endHeight shl 2
                slope = slope shl 2
                ProjectileSpawnRequest.create(spotAnimId, source, lockOn, basOffIdx, startHeight, endHeight, localX, localY, xOff, yOff, startTime, endTime, angle, slope, useFloorHeight)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.WORLD_LIST) {
            val refresh = buffer.readUByte() == 1
            val data = ByteArray(context.currentPacketSize - 1)
            buffer.readBytes(data, 0, context.currentPacketSize - 1)
            decodeWorldList(refresh, data)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARBIT_LARGE) {
            val variable = buffer.readUIntInverseMiddle()
            val varbitId = buffer.readUShortAdd()
            VarProviders.PLAYER_VAR_PROVIDER.setVarBitValueFromServer(varbitId, variable)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.aClass3754362) { //request packet 37?
            val key = buffer.readUShortAddLittle()
            client.requestVarVerifyid()
            StaticMethods.method1563(key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLANSETTINGS_FULL) {
            client.anInt7192 = client.anInt7347
            val bool91 = buffer.readUByte() == 1
            if (context.currentPacketSize == 1) {
                if (bool91) {
                    client.CLAN_SETTINGS = null
                } else {
                    client.GUEST_CLAN_SETTINGS = null
                }
            } else {
                if (bool91) {
                    client.CLAN_SETTINGS = ClanSettings(buffer)
                } else {
                    client.GUEST_CLAN_SETTINGS = ClanSettings(buffer)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.aClass3754457) {
            val key = buffer.readUShort()
            client.requestVarVerifyid()
            StaticMethods.setVorbisSound(key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_INV_PARTIAL) {
            val key = buffer.readUShort()
            val flags = buffer.readUByte()
            var i_7: Int
            var i_8: Int
            var i_9: Int
            val isNegativeKey = (flags and 0x1) == 1
            while (buffer.currentPosition < context.currentPacketSize) {
                i_7 = buffer.readUSmart()
                i_8 = buffer.readUShort()
                i_9 = 0
                if (i_8 != 0) {
                    i_9 = buffer.readUByte()
                    if (i_9 == 255) {
                        i_9 = buffer.readInt()
                    }
                }
                ItemContainer.setItemInContainer(key, i_7, i_8 - 1, i_9, isNegativeKey)
            }
            client.RECENTLY_USED_ITEM_CONTAINERS[++client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - 1 and 0x1f] = key
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.PING) {
            context.currentPacket = null
            return false
        } else if (context.currentPacket == ServerProt.VARP_SMALL) {
            val value = buffer.readByte()
            val configIndex = buffer.readUShortAddLittle()
            VarProviders.PLAYER_VAR_PROVIDER.setVarValueFromServer(configIndex, value.toInt())
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.aClass3753828) {
            val near = buffer.readUByteSubtract()
            val volume = buffer.readUByte()
            val far = buffer.readUByteInverse()
            val musicArchive = buffer.readInt()
            val i_8 = buffer.readUIntMiddle()
            val plane = i_8 shr 28
            val x = i_8 shr 14 and 0x3fff
            val y = i_8 and 0x3fff
            SongPlayer.method5731(x, y, near, far, musicArchive, volume)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.TRIGGER_ONDIALOGABORT) {
            if (client.BASE_WINDOW_ID != -1) {
                Interface.open_sub(client.BASE_WINDOW_ID, 0)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_FRIENDCHAT_CHANNEL_SINGLEUSER) {
            val displayName = buffer.readString()
            val hasPreviousName = buffer.readUByte() == 1
            val username: String?
            if (hasPreviousName) {
                username = buffer.readString()
            } else {
                username = displayName
            }
            val worldId = buffer.readUShort()
            val rank = buffer.readByte()
            val bool69 = rank.toInt() == -128
            var i_11: Int
            if (bool69) {
                if (FriendsChat.FC_PLAYER_COUNT == 0) {
                    context.currentPacket = null
                    return true
                }
                i_11 = 0
                while (i_11 < FriendsChat.FC_PLAYER_COUNT && (FriendsChat.FC_PLAYERS[i_11].username != username || worldId != FriendsChat.FC_PLAYERS[i_11].worldId)) {
                    i_11++
                }
                if (i_11 < FriendsChat.FC_PLAYER_COUNT) {
                    while (i_11 < FriendsChat.FC_PLAYER_COUNT - 1) {
                        FriendsChat.FC_PLAYERS[i_11] = FriendsChat.FC_PLAYERS[i_11 + 1]
                        ++i_11
                    }
                    FriendsChat.FC_PLAYERS[--FriendsChat.FC_PLAYER_COUNT] = null
                }
            } else {
                val worldName = buffer.readString()
                val fcPlayer = FriendsChatPlayer()
                fcPlayer.displayName = displayName
                fcPlayer.username = username
                fcPlayer.textName = TextUtils.formatDisplayname(fcPlayer.username)
                fcPlayer.worldId = worldId
                fcPlayer.rank = rank
                fcPlayer.worldName = worldName
                var i_12: Int
                i_12 = FriendsChat.FC_PLAYER_COUNT - 1
                while (i_12 >= 0) {
                    val i_35 = FriendsChat.FC_PLAYERS[i_12].textName.compareTo(fcPlayer.textName)
                    if (i_35 == 0) {
                        FriendsChat.FC_PLAYERS[i_12].worldId = worldId
                        FriendsChat.FC_PLAYERS[i_12].rank = rank
                        FriendsChat.FC_PLAYERS[i_12].worldName = worldName
                        if (username == PlayerUpdate.MY_PLAYER.displayName) {
                            FriendsChat.MY_PLAYER_FC_RANK = rank
                        }
                        client.anInt7179 = client.anInt7347
                        context.currentPacket = null
                        return true
                    }
                    if (i_35 < 0) {
                        break
                    }
                    --i_12
                }
                if (FriendsChat.FC_PLAYER_COUNT >= FriendsChat.FC_PLAYERS.size) {
                    context.currentPacket = null
                    return true
                }
                if (FriendsChat.FC_PLAYER_COUNT - 1 - i_12 >= 0) System.arraycopy(FriendsChat.FC_PLAYERS, i_12 + 1, FriendsChat.FC_PLAYERS, i_12 + 1 + 1, FriendsChat.FC_PLAYER_COUNT - 1 - i_12)
                if (FriendsChat.FC_PLAYER_COUNT == 0) {
                    FriendsChat.FC_PLAYERS = arrayOfNulls<FriendsChatPlayer>(100)
                }
                FriendsChat.FC_PLAYERS[i_12 + 1] = fcPlayer
                FriendsChat.FC_PLAYER_COUNT++
                if (username == PlayerUpdate.MY_PLAYER.displayName) {
                    FriendsChat.MY_PLAYER_FC_RANK = rank
                }
            }
            client.anInt7179 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETHIDE) {
            val key = buffer.readUByteAdd()
            val flags = buffer.readInt()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfhide(flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLANCHANNEL_DELTA) {
            client.anInt7395 = client.anInt7347
            val bool91 = buffer.readUByte() == 1
            val delta = ClanChannelDelta(buffer)
            val clanChannel: ClanChannel
            if (bool91) {
                clanChannel = client.CLAN_CHANNEL
            } else {
                clanChannel = client.LISTENED_CLAN_CHANNEL
            }
            delta.apply(clanChannel)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.REGION) {
            val bitBuffer = Bit(context.currentPacketSize)
            System.arraycopy(context.receivedBuffer.buffer, context.receivedBuffer.currentPosition, bitBuffer.buffer, 0, context.currentPacketSize)
            client.resetCutscenes()
            if (PreferenceManager.preferences.asyncMaploading.getValue() == 1) {
                Resource.mapRegionLoaderTask.requestMapUpdate(RegionLoadRequest(RegionLoadingType.STANDARD, bitBuffer))
            } else {
                Resource.mapRegion.initialize(RegionLoadRequest(RegionLoadingType.STANDARD, bitBuffer))
            }
            context.currentPacket = null
            return false
        } else if (context.currentPacket == ServerProt.REMOVE_GROUND_ITEM) {
            decodeTilestreamPacket(UpdateZonePacket.REMOVE_GROUND_ITEM)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETANIM) {
            val key = buffer.readUIntInverseMiddle()
            val flags = buffer.readInt()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfanimUpdate(flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SPOT_ANIM) {
            decodeTilestreamPacket(UpdateZonePacket.SPOT_ANIM)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARP_LARGE) {
            val key = buffer.readUIntInverseMiddle()
            val flags = buffer.readUShortAddLittle()
            VarProviders.PLAYER_VAR_PROVIDER.setVarValueFromServer(flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_CLOSESUB) {
            val parentInterface = buffer.readInt()
            client.requestVarVerifyid()
            val subInterface = client.OPEN_INTERFACES.get(parentInterface.toLong())
            if (subInterface != null) {
                Interface.closeChildInterface(subInterface, true, false)
            }
            if (client.aClass1187352 != null) {
                Component.redrawComponent(client.aClass1187352)
                client.aClass1187352 = null
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_STAT) {
            val xp = buffer.readInt()
            val skillId = buffer.readUByte()
            val level = buffer.readUByte()
            client.SKILL_XP[skillId] = xp
            client.SKILL_LEVEL_VISIBLE[skillId] = level
            client.SKILL_LEVEL_ACTUAL[skillId] = 1
            val maxLevel = SkillConstants.SKILL_MAX_LEVEL[skillId] - 1
            for (i in 0..<maxLevel) {
                if (xp >= SkillConstants.SKILL_XP_FOR_LEVEL[i]) {
                    client.SKILL_LEVEL_ACTUAL[skillId] = i + 2
                }
            }
            client.anIntArray7283[++client.anInt7384 - 1 and 0x1f] = skillId
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.aClass3754453) {
            val b_100 = buffer.readByte()
            val flags = buffer.readUByteInverse()
            client.requestVarVerifyid()
            StaticMethods.method14501(b_100.toInt(), flags)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARCLAN_ENABLE) {
            ClanVarTypeList.CLAN_VARS = arrayOfNulls<Any>(TypeLists.CLAN_VAR.size)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETRECOL) {
            val key = buffer.readUIntInverseMiddle()
            val flags = buffer.readUShortLittle()
            val i_6 = buffer.readUShortLittle()
            val i_7 = buffer.readUByte()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfrecol(key, i_7, flags, i_6)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETTEXTANTIMACRO) {
            val enabled = buffer.readUByteSubtract() == 1
            val componentHash = buffer.readUIntInverseMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.prioritySetIftextantimacro(componentHash, enabled)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CREATE_CHECK_EMAIL_REPLY) {
            val key = buffer.readUByte()
            var request = Identifiable.identify(CheckEmailRequest.getValues(), key) as CheckEmailRequest?
            if (request == null) {
                request = CheckEmailRequest.RECEIVED_RESPONSE
            }
            AccountCreationStage.setCheckemailRequeststatus(request)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_DOB) {
            LoginManager.PLAYER_DOB = buffer.readMedium()
            client.USERDETAIL_QUICKCHAT = buffer.readUByte() == 1
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_PLAYER) {
            val key = buffer.readUIntLittle()
            val overlay = buffer.readUByte()
            val i_6 = buffer.readUIntMiddle()
            val subInterfaceId = buffer.readUShort()
            val i_8 = buffer.readInt()
            val interfaceId = buffer.readUIntMiddle()
            val i_10 = buffer.readUIntMiddle()
            val playerIndex = buffer.readUShortLittle()
            client.requestVarVerifyid()
            Interface.openSub(interfaceId, IFSubActivePlayer(subInterfaceId, overlay, playerIndex), intArrayOf(i_6, i_8, i_10, key), false)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLANSETTINGS_DELTA) {
            client.anInt7192 = client.anInt7347
            val clanSettings = buffer.readUByte() == 1
            val delta = ClanSettingsDelta(buffer)
            val settings: ClanSettings
            if (clanSettings) {
                settings = client.CLAN_SETTINGS
            } else {
                settings = client.GUEST_CLAN_SETTINGS
            }
            delta.queueUpdates(settings)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MINIMAP_FLAG) {
            var key = buffer.readUByteSubtract()
            var flags = buffer.readUByteSubtract()
            if (key == 255) {
                key = -1
                flags = -1
            }
            PulseEventQueue.priorityMinimapFlagUpdate(key, flags)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.BLOCK_MINIMAP_STATE) {
            ComponentMinimap.minimapState = buffer.readUByte()
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.OUTDATED_SET_THEORA_STRING_SOMETHING) {
            val key = buffer.readUShortAdd()
            val flags = buffer.readUIntLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityT17Update(flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_OPENSUB) {
            val key = buffer.readInt()
            val flags = buffer.readUIntInverseMiddle()
            val i_6 = buffer.readUIntInverseMiddle()
            val subId = buffer.readUShortAddLittle()
            val i_8 = buffer.readUIntLittle()
            val topUid = buffer.readInt()
            val overlay = buffer.readUByteInverse()
            client.requestVarVerifyid()
            Interface.openSub(topUid, SubInterface(subId, overlay), intArrayOf(key, i_6, flags, i_8), false)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CREATE_ACCOUNT_REPLY) {
            val key = buffer.readUByte()
            var class49496 = Identifiable.identify(CreateAccountRequest.method8038(), key) as CreateAccountRequest?
            if (class49496 == null) {
                class49496 = CreateAccountRequest.aClass4945782
            }
            AccountCreationStage.method12948(class49496)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.PLAYER_OPTION) {
            var option: String? = buffer.readString()
            val slot = buffer.readUByteAdd()
            var cursor = buffer.readUShortAddLittle()
            if (cursor == 65535) {
                cursor = -1
            }
            val onTop = buffer.readUByteInverse()
            if (slot >= 1 && slot <= 8) {
                if ("null".equals(option, ignoreCase = true)) {
                    option = null
                }
                client.PLAYER_OPTIONS[slot - 1] = option
                client.PLAYER_OPTION_CURSORS[slot - 1] = cursor
                client.PLAYER_OPTION_REDUCED_PRIORITY[slot - 1] = onTop == 0
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETITEM) {
            var itemId = buffer.readUShort()
            if (itemId == 65535) {
                itemId = -1
            }
            val interfaceHash = buffer.readInt()
            val amount = buffer.readUIntInverseMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfitemUpdate(interfaceHash, itemId, amount)
            val defs = TypeLists.OBJ.list(itemId)
            PulseEventQueue.priorityIfangleUpdate(interfaceHash, defs.modelRotationX, defs.modelRotationY, defs.modelZoom)
            PulseEventQueue.priorityIfoffsetUpdate(interfaceHash, defs.modelOffsetX, defs.modelOffsetY, defs.modelRotationZ)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERHEAD_IGNOREWORN) {
            val idKit1 = buffer.readUShortLittle()
            val usernameMeme = buffer.readUShortAdd()
            val idKit2 = buffer.readUShortAdd()
            val interfaceHash = buffer.readUIntInverseMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcontentUpdate(interfaceHash, 7, idKit1 shl 16 or idKit2, usernameMeme)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.GROUND_ITEM_COUNT) {
            decodeTilestreamPacket(UpdateZonePacket.GROUND_ITEM_COUNT)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERMODEL) {
            val key = buffer.readUIntMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcontentUpdate(key, 5, PlayerUpdate.myPlayerIndex, 0)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.PRELOAD_SONG) {
            var songId = buffer.readUShortAddLittle()
            if (songId == 65535) {
                songId = -1
            }
            PacketEncoder.preloadSong(songId)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.APPLY_DEBUG) {
            val key = buffer.readUShort()
            DebugDisplay.applyDebugOpcode(key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CREATE_OBJECT) {
            decodeTilestreamPacket(UpdateZonePacket.CREATE_OBJECT)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_PRIVATE_ECHO) {
            val string_63 = buffer.readString()
            val string_88 = TextUtils.atLtGtTags(HuffmanList.decode(buffer))
            ChatBox.writeMessage(ChatMessageType.PRIVATE_MESSAGE_ECHO, 0, string_63, string_63, string_63, string_88)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CREATE_GROUND_ITEM) {
            decodeTilestreamPacket(UpdateZonePacket.CREATE_GROUND_ITEM)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.FRIENDLIST_LOADED) {
            FriendList.LOADED = 1
            client.anInt7386 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETPLAYERHEAD) {
            val key = buffer.readUIntLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfcontentUpdate(key, 3, PlayerUpdate.myPlayerIndex, 0)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.JCOINS_UPDATE) {
            val key = buffer.readUIntLittle()
            if (key != client.JCOINS) {
                client.JCOINS = key
                CS2Executor.executeTrigger(ClientTriggerType.JCOINS_CHANGED, -1, -1)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.REFLECTION_CHECK) {
            ReflectionCheckManager.requestReflectionCheck(buffer)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARBIT_SMALL) {
            val key = buffer.readUShort()
            val flags = buffer.readUByteAdd()
            VarProviders.PLAYER_VAR_PROVIDER.setVarBitValueFromServer(key, flags)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_PRIVATE) {
            val bool91 = buffer.readUByte() == 1
            val string_88 = buffer.readString()
            var str92 = string_88
            if (bool91) {
                str92 = buffer.readString()
            }
            val long_28 = buffer.readUShort().toLong()
            val long_30 = buffer.readUMedium().toLong()
            val i_11 = buffer.readUByte()
            val long_53 = (long_28 shl 32) + long_30
            var bool14 = false
            var i_15 = 0
            while (true) {
                if (i_15 >= 100) {
                    if (i_11 <= 1) {
                        if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                            if (IgnoreList.contains(str92)) {
                                bool14 = true
                            }
                        } else {
                            bool14 = true
                        }
                    }
                    break
                }
                if (client.CHAT_MESSAGE_UNIQUE_HASHES[i_15] == long_53) {
                    bool14 = true
                    break
                }
                ++i_15
            }
            if (!bool14) {
                client.CHAT_MESSAGE_UNIQUE_HASHES[client.anInt7389] = long_53
                client.anInt7389 = (client.anInt7389 + 1) % 100
                val string_118 = TextUtils.atLtGtTags(HuffmanList.decode(buffer))
                if (i_11 == 2) {
                    ChatBox.writeMessage(ChatMessageType.PRIVATE_STAFF, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, string_118, null, -1)
                } else if (i_11 == 1) {
                    ChatBox.writeMessage(ChatMessageType.PRIVATE_STAFF, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, string_118, null, -1)
                } else {
                    ChatBox.writeMessage(ChatMessageType.PRIVATE_MESSAGE, 0, string_88, str92, string_88, string_118, null, -1)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARC_SMALL) {
            val b_100 = buffer.readByteSubtract()
            val flags = buffer.readUShortLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityClientvarcUpdate(flags, b_100.toInt())
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETTEXTFONT) {
            val key = buffer.readInt()
            val flags = buffer.readUIntMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIffontUpdate(flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETGRAPHIC) {
            val key = buffer.readUIntInverseMiddle()
            val flags = buffer.readUIntInverseMiddle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfgraphicUpdate(flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.DEBUG_SERVER_TRIGGERS) {
            val key = buffer.readUShort()
            val flags = buffer.readUShort()
            val i_6 = buffer.readUShort()
            client.requestVarVerifyid()
            var i_8: Int
            if (Interface.INTERFACES[key] != null) {
                for (i_7 in flags..<i_6) {
                    i_8 = buffer.readUMedium()
                    if (i_7 < Interface.INTERFACES[key].components.size && Interface.INTERFACES[key].components[i_7] != null) {
                        Interface.INTERFACES[key].components[i_7].serverTriggers = i_8
                    }
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CUSTOMIZE_OBJECT) {
            decodeTilestreamPacket(UpdateZonePacket.CUSTOMIZE_OBJECT)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.PLAYER_UPDATE) {
            PlayerUpdate.decode(buffer, context.currentPacketSize)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CAM_MOVETO) {
            val localY = buffer.readUByte()
            val localX = buffer.readUByte()
            val constantSpeed = buffer.readUByteSubtract()
            val moveZ = buffer.readUShortLittle() shl 2
            val variableSpeed = buffer.readUByteAdd()
            client.requestVarVerifyid()
            Camera.moveCamera(localX, localY, moveZ, constantSpeed, variableSpeed, true)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_CLANCHANNEL) {
            val bool91 = buffer.readUByte() == 1
            val string_88 = buffer.readString()
            val long_49 = buffer.readUShort().toLong()
            val long_51 = buffer.readUMedium().toLong()
            val i_10 = buffer.readUByte()
            val long_55 = long_51 + (long_49 shl 32)
            var bool112 = false
            val class282Sub437 = if (bool91) client.CLAN_CHANNEL else client.LISTENED_CLAN_CHANNEL
            if (class282Sub437 == null) {
                bool112 = true
            } else {
                for (i_81 in 0..99) { //TODO validate this logic
                    if (client.CHAT_MESSAGE_UNIQUE_HASHES[i_81] == long_55) {
                        bool112 = true
                        break
                    }
                }
                if (i_10 <= 1) {
                    if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                        if (IgnoreList.contains(string_88))
                            bool112 = true
                    } else
                        bool112 = true
                }
            }
            if (!bool112) {
                client.CHAT_MESSAGE_UNIQUE_HASHES[client.anInt7389] = long_55
                client.anInt7389 = (client.anInt7389 + 1) % 100
                val string_101 = TextUtils.atLtGtTags(HuffmanList.decode(buffer))
                val i_34 = if (bool91) ChatMessageType.CLAN_CHAT else ChatMessageType.GUEST_CLAN_CHAT
                if (i_10 != 2 && i_10 != 3) {
                    if (i_10 == 1) {
                        ChatBox.writeMessage(i_34, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + string_88, string_88, string_101, class282Sub437!!.clanName, -1)
                    } else {
                        ChatBox.writeMessage(i_34, 0, string_88, string_88, string_88, string_101, class282Sub437!!.clanName, -1)
                    }
                } else {
                    ChatBox.writeMessage(i_34, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + string_88, string_88, string_101, class282Sub437!!.clanName, -1)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.WINDOW_CALL_WITH_ARGS) {
            val argsBuffer = ByteArray(context.currentPacketSize)
            buffer.readBytes(argsBuffer, context.currentPacketSize)
            val windowCallParam = Utils.readString(argsBuffer, 0, context.currentPacketSize)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MAP_PROJANIM) {
            decodeTilestreamPacket(UpdateZonePacket.MAP_PROJANIM)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_SITESETTINGS_COOKIE) {
            //no applet deprecated
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.REDUCE_ATTACK_PRIORITY) {
            client.REDUCE_ATTACK_PRIORITY = buffer.readUByteAdd() == 1
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.FRIEND_STATUS) {
            while (buffer.currentPosition < context.currentPacketSize) {
                val warnMessage = buffer.readUByte() == 1
                var displayName: String? = buffer.readString()
                val previousDisplayName = buffer.readString()
                val world = buffer.readUShort()
                val friendchatRank = buffer.readUByte()
                val referrer = buffer.readUByte() == 1
                var worldName = ""
                var platform = -1
                var worldFlags = 0
                if (world > 0) {
                    worldName = buffer.readString()
                    platform = buffer.readUByte()
                    worldFlags = buffer.readInt()
                }
                for (index in 0..<FriendList.FRIEND_COUNT) {
                    val friend = FriendList.FRIENDS[index]
                    if (!warnMessage) {
                        if (displayName == friend.displayName) {
                            if (world != friend.worldId) {
                                var announce = true
                                var statusUpdate = client.PLAYER_LOGINSTATE_UPDATES.getFirst() as LoginStateUpdate?
                                while (statusUpdate != null) {
                                    if (statusUpdate.username == displayName) {
                                        if (world != 0 && statusUpdate.actionType.toInt() == 0) {
                                            statusUpdate.unlinkNode()
                                            announce = false
                                        } else if (world == 0 && statusUpdate.actionType.toInt() != 0) {
                                            statusUpdate.unlinkNode()
                                            announce = false
                                        }
                                    }
                                    statusUpdate = client.PLAYER_LOGINSTATE_UPDATES.getNext() as LoginStateUpdate?
                                }
                                if (announce) {
                                    client.PLAYER_LOGINSTATE_UPDATES.offer(LoginStateUpdate(displayName, world))
                                }
                                friend.worldId = world
                            }
                            friend.previousDisplayName = previousDisplayName
                            friend.worldName = worldName
                            friend.friendsChatRank = friendchatRank
                            friend.platform = platform
                            friend.referrer = referrer
                            friend.worldFlags = worldFlags
                            displayName = null
                            break
                        }
                    } else if (previousDisplayName == friend.displayName) {
                        friend.displayName = displayName
                        friend.previousDisplayName = previousDisplayName
                        displayName = null
                        break
                    }
                }
                if (displayName != null && FriendList.FRIEND_COUNT < 200) {
                    val friend = Friend()
                    FriendList.FRIENDS[FriendList.FRIEND_COUNT] = friend
                    friend.displayName = displayName
                    friend.previousDisplayName = previousDisplayName
                    friend.worldId = world
                    friend.worldName = worldName
                    friend.friendsChatRank = friendchatRank
                    friend.platform = platform
                    friend.referrer = referrer
                    friend.worldFlags = worldFlags
                    ++FriendList.FRIEND_COUNT
                }
            }
            FriendList.LOADED = 2
            client.anInt7386 = client.anInt7347
            var bool91 = false
            var flags = FriendList.FRIEND_COUNT
            while (flags > 0) {
                bool91 = true
                --flags
                for (i_6 in 0..<flags) {
                    var bool70 = false
                    val class6132 = FriendList.FRIENDS[i_6]
                    val class6134 = FriendList.FRIENDS[i_6 + 1]
                    if (class6132.worldId != LoginManager.GAME_CONNECTION_INFO.worldId && class6134.worldId == LoginManager.GAME_CONNECTION_INFO.worldId) {
                        bool70 = true
                    }
                    if (!bool70 && class6132.worldId == 0 && class6134.worldId != 0) {
                        bool70 = true
                    }
                    if (!bool70 && !class6132.referrer && class6134.referrer) {
                        bool70 = true
                    }
                    if (bool70) {
                        val class687 = FriendList.FRIENDS[i_6]
                        FriendList.FRIENDS[i_6] = FriendList.FRIENDS[i_6 + 1]
                        FriendList.FRIENDS[i_6 + 1] = class687
                        bool91 = false
                    }
                }
                if (bool91) {
                    break
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IDENTIFY_HOST_NAME) {
            val ip = buffer.readInt()
            HOSTNAME_IDENTIFIER = HostNameIdentifier(ip)
            val thread94: Thread = Thread(HOSTNAME_IDENTIFIER)
            thread94.setPriority(1)
            thread94.start()
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_MOVESUB) {
            val fromParentUid = buffer.readUIntInverseMiddle()
            val toParentUid = buffer.readInt()
            client.requestVarVerifyid()
            val toSub = client.OPEN_INTERFACES.get(toParentUid.toLong())
            val fromSub = client.OPEN_INTERFACES.get(fromParentUid.toLong())
            if (fromSub != null) {
                Interface.closeChildInterface(fromSub, toSub == null || toSub.subInterfaceId != fromSub.subInterfaceId, false)
            }
            if (toSub != null) {
                toSub.unlink()
                client.OPEN_INTERFACES.put(toSub, fromParentUid.toLong())
            }
            var parent2Def = Component.fromHash(toParentUid)
            if (parent2Def != null) {
                Component.redrawComponent(parent2Def)
            }
            parent2Def = Component.fromHash(fromParentUid)
            if (parent2Def != null) {
                Component.redrawComponent(parent2Def)
                Component.method483(Interface.INTERFACES[parent2Def.idHash ushr 16], parent2Def, true)
            }
            if (client.BASE_WINDOW_ID != -1) {
                Interface.open_sub(client.BASE_WINDOW_ID, 1)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CUTSCENE) {
            val key = buffer.readUShort()
            client.cycleCutsceneLoaded = -1
            client.CURRENT_CUTSCENE = key
            client.MAP_LOADING_STAGE = 3
            Resource.CUTSCENE.isFileCached(client.CURRENT_CUTSCENE)
            MiniMenu.close()
            MiniMenu.resetEntries()
            val flags = buffer.readUShort()
            MapRegion.CUTSCENE_MAP_XTEAS = Array<IntArray?>(flags) { IntArray(4) }
            for (i_6 in 0..<flags) {
                for (i_7 in 0..3) {
                    MapRegion.CUTSCENE_MAP_XTEAS[i_6][i_7] = buffer.readInt()
                }
            }
            val i_6 = buffer.readUByte()
            CutsceneTypeList.CUTSCENE_BUFFER = Packet(i_6)
            CutsceneTypeList.CUTSCENE_BUFFER.writeBytes(buffer.buffer, buffer.currentPosition, i_6)
            buffer.currentPosition += i_6
            context.currentPacket = null
            return false
        } else if (context.currentPacket == ServerProt.DESTROY_OBJECT) {
            decodeTilestreamPacket(UpdateZonePacket.DESTROY_OBJECT)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.LOYALTY_UPDATE) {
            val key = buffer.readInt()
            val bool66 = buffer.readUByteInverse() == 1
            if (bool66 != QueueIterator.LOYALTY_ENABLED || key != LoginManager.LOYALTY_BALANCE) {
                QueueIterator.LOYALTY_ENABLED = bool66
                LoginManager.LOYALTY_BALANCE = key
                CS2Executor.executeTrigger(ClientTriggerType.LOYALTY_UPDATED, -1, -1)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_NPC) {
            val npcIndex = buffer.readUShortAdd()
            val flags = buffer.readUIntLittle()
            val overlay = buffer.readUByteInverse()
            val interfaceId = buffer.readInt()
            val i_8 = buffer.readUIntLittle()
            val subInterfaceId = buffer.readUShortAddLittle()
            val i_10 = buffer.readInt()
            val i_11 = buffer.readInt()
            client.requestVarVerifyid()
            Interface.openSub(interfaceId, IFSubActiveNpc(subInterfaceId, overlay, npcIndex), intArrayOf(i_11, flags, i_10, i_8), false)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.ADD_IGNORE) {
            val flags = buffer.readUByte()
            val editDisplayName = (flags and 0x1) == 1
            val username = buffer.readString()
            val displayName = buffer.readString()
            if (!editDisplayName) {
                val ignore = Ignore()
                IgnoreList.IGNORED[IgnoreList.IGNORE_COUNT++] = ignore
                ignore.unfilteredUsername = username
                ignore.displayName = displayName
                ignore.temporary = (flags and 0x2) == 2
            } else {
                for (i in 0..<IgnoreList.IGNORE_COUNT) {
                    val ignore = IgnoreList.IGNORED[i]
                    if (displayName == ignore.unfilteredUsername) {
                        ignore.unfilteredUsername = username
                        ignore.displayName = displayName
                        break
                    }
                }
            }
            client.anInt7386 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.CLIENT_SETVARCSTR_LARGE) {
            val key = buffer.readUShortLittle()
            val string_88 = buffer.readString()
            client.requestVarVerifyid()
            PulseEventQueue.priorityVarcstringUpdate(key, string_88)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.NPC_UPDATE) {
            NPCUpdate.decode(false)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.MESSAGE_FRIENDS_CHAT) {
            val bool91 = buffer.readUByte() == 1
            val string_88 = buffer.readString()
            var str92 = string_88
            if (bool91) {
                str92 = buffer.readString()
            }
            val long_28 = buffer.readLong()
            val long_30 = buffer.readUShort().toLong()
            val long_55 = buffer.readUMedium().toLong()
            val i_35 = buffer.readUByte()
            val long_60 = (long_30 shl 32) + long_55
            var bool16 = false
            var i_34 = 0
            while (true) {
                if (i_34 >= 100) {
                    if (i_35 <= 1) {
                        if ((!client.USERDETAIL_QUICKCHAT || client.VERIFIED_EMAIL_ADDRESS) && !client.IS_QUICKCHAT_ONLY) {
                            if (IgnoreList.contains(str92)) {
                                bool16 = true
                            }
                        } else {
                            bool16 = true
                        }
                    }
                    break
                }
                if (long_60 == client.CHAT_MESSAGE_UNIQUE_HASHES[i_34]) {
                    bool16 = true
                    break
                }
                ++i_34
            }
            if (!bool16) {
                client.CHAT_MESSAGE_UNIQUE_HASHES[client.anInt7389] = long_60
                client.anInt7389 = (client.anInt7389 + 1) % 100
                val string_17 = TextUtils.atLtGtTags(HuffmanList.decode(buffer))
                if (i_35 != 2 && i_35 != 3) {
                    if (i_35 == 1) {
                        ChatBox.writeMessage(ChatMessageType.FC_CHAT, 0, TextUtils.getCrown(0) + string_88, TextUtils.getCrown(0) + str92, string_88, string_17, TextUtils.decodeUidName(long_28), -1)
                    } else {
                        ChatBox.writeMessage(ChatMessageType.FC_CHAT, 0, string_88, str92, string_88, string_17, TextUtils.decodeUidName(long_28), -1)
                    }
                } else {
                    ChatBox.writeMessage(ChatMessageType.FC_CHAT, 0, TextUtils.getCrown(1) + string_88, TextUtils.getCrown(1) + str92, string_88, string_17, TextUtils.decodeUidName(long_28), -1)
                }
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.OBJ_ANIM_SPECIFIC) {
            val tileHash = buffer.readUIntInverseMiddle()
            val plane = tileHash shr 28 and 0x3
            var x = tileHash shr 14 and 0x3fff
            var y = tileHash and 0x3fff
            val animationId = buffer.readUIntLittle()
            val objFlags = buffer.readUByteSubtract()
            val shape = objFlags shr 2
            val rotation = objFlags and 0x3
            val type = client.OBJECT_TYPE_SLOTS[shape]
            val base = Resource.mapRegion.getBaseCoordinates()
            x -= base.x
            y -= base.y
            SceneGraph.animateObject(plane, x, y, type, shape, rotation, animationId)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VORBIS_SPEECH_SOUND) {
            var id = buffer.readUShort()
            if (id == 65535) {
                id = -1
            }
            val soundType = buffer.readUByte()
            val delay = buffer.readUShort()
            val i_7 = buffer.readUByte()
            AreaSoundPlayer.playSoundVorbis(id, soundType, delay, i_7, true, 256)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.UPDATE_IGNORE_LIST) {
            IgnoreList.IGNORE_COUNT = buffer.readUByte()
            for (key in 0..<IgnoreList.IGNORE_COUNT) {
                val class1089 = Ignore()
                IgnoreList.IGNORED[key] = class1089
                class1089.unfilteredUsername = buffer.readString()
                class1089.displayName = buffer.readString()
                class1089.temporary = false
            }
            client.anInt7386 = client.anInt7347
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_SETRETEX) {
            val key = buffer.readUShortLittle()
            val flags = buffer.readUShort()
            val i_6 = buffer.readUByteSubtract()
            val i_7 = buffer.readUIntLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityIfretex(i_7, i_6, flags, key)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.SET_TARGET) {
            client.ENTITY_TARGET = buffer.readShortLittle() //reads same identifier as projectile (index+1 for npc and -index-1 for player)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.IF_OPENSUB_ACTIVE_OBJECT) {
            val interfaceId = buffer.readUIntInverseMiddle()
            val xtea3 = buffer.readUIntInverseMiddle()
            val overlay = buffer.readUByteAdd()
            val xtea2 = buffer.readInt()
            val xtea1 = buffer.readUIntMiddle()
            val subInterfaceId = buffer.readUShortAdd()
            val bitpacked = buffer.readUByteSubtract()
            val type = bitpacked shr 2
            val rotation = bitpacked and 0x3
            val xtea4 = buffer.readInt()
            val absolutePosition = CoordGrid(buffer.readUIntInverseMiddle())
            val objectId = buffer.readInt()
            client.requestVarVerifyid()
            Interface.openSub(interfaceId, IFSubActiveObject(subInterfaceId, overlay, IFSubObjectPosition(absolutePosition, type, objectId)), intArrayOf(xtea1, xtea2, xtea3, xtea4), false)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.VARCLAN_DISABLE) {
            ClanVarTypeList.CLAN_VARS = null
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.aClass3754437) {
            if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
                WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false)
            }
            val bytes_64 = ByteArray(context.currentPacketSize)
            buffer.readBytes(bytes_64, context.currentPacketSize)
            val string_88 = Utils.readString(bytes_64, 0, context.currentPacketSize)
            val str92 = "opensn"
            if (!client.isApplet || !Browser.openBrowser(string_88, 1, str92)) {
                Browser.openBrowser(string_88, true, str92, client.aBool7158)
            }
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.OUTDATED_INTERFACE_PULSE_EVENT) {
            val key = buffer.readUShortAdd()
            val flags = buffer.readUIntInverseMiddle()
            val i_6 = buffer.readUShortLittle()
            client.requestVarVerifyid()
            PulseEventQueue.priorityT13Update(flags, key + (i_6 shl 16))
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.RUN_CS2_SCRIPT) {
            val string_63 = buffer.readString()
            val arr_26 = arrayOfNulls<Any>(string_63.length + 1)
            for (i_6 in string_63.length - 1 downTo 0) {
                if (string_63.get(i_6).code == 115) {
                    arr_26[i_6 + 1] = buffer.readString()
                } else {
                    arr_26[i_6 + 1] = buffer.readInt()
                }
            }
            arr_26[0] = buffer.readInt()
            client.requestVarVerifyid()
            val hookrequest27 = HookRequest()
            hookrequest27.params = arr_26
            CS2Executor.executeHookInner(hookrequest27)
            context.currentPacket = null
            return true
        } else if (context.currentPacket == ServerProt.NPC_UPDATE_LARGE) {
            NPCUpdate.decode(true)
            context.currentPacket = null
            return true
        } else {
            PacketEncoder.sendErrorToServer((if (context.currentPacket != null) context.currentPacket.opcode else -1).toString() + "," + (if (context.secondLastPacket != null) context.secondLastPacket.opcode else -1) + "," + (if (context.thirdLastPacket != null) context.thirdLastPacket.opcode else -1) + " " + context.currentPacketSize, RuntimeException())
            client.logout(false)
            return true
        }
    }

    fun decodeTilestreamPacket(packet: UpdateZonePacket?) {
        val buffer = client.GAME_CONNECTION_CONTEXT.receivedBuffer
        if (packet == UpdateZonePacket.GROUND_ITEM_COUNT) {
            val i_3 = buffer.readUByte()
            val coordgrid4 = Resource.mapRegion.getBaseCoordinates()
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val y = i_5 + coordgrid4.y
            val i_7 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val x = i_7 + coordgrid4.x
            val i_9 = buffer.readUShort()
            val i_10 = buffer.readUShort()
            val amount = buffer.readUShort()
            if (client.GROUND_ITEMS != null) {
                val class282Sub2912 = client.GROUND_ITEMS.get(UpdateZonePacket.UPDATE_ZONE_PLANE.toLong() shl 28 or (y.toLong() shl 14) or x.toLong())
                if (class282Sub2912 != null) {
                    var item = class282Sub2912.groundItems.getFirst() as GroundItemNode?
                    while (item != null) {
                        if ((i_9 and 0x7fff) == item.id && i_10 == item.amount) {
                            item.unlink()
                            item.amount = amount
                            GroundItemUpdating.addGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, item)
                            break
                        }
                        item = class282Sub2912.groundItems.next() as GroundItemNode?
                    }
                    if (i_7 >= 0 && i_5 >= 0 && i_7 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                        GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, i_7, i_5)
                    }
                }
            }
        } else if (packet == UpdateZonePacket.OBJECT_PREFETCH) {
            val i_3 = buffer.readInt()
            val i_21 = buffer.readUByte()
            Resource.mapRegion.getLoctypeList().list(i_3).isTypeCached(i_21)
        } else if (packet == UpdateZonePacket.SPOT_ANIM) {
            val bitpacked = buffer.readUByte()
            val x = (bitpacked shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (bitpacked and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var spotAnimId = buffer.readUShort()
            if (spotAnimId == 65535) {
                spotAnimId = -1
            }
            val height = buffer.readUShort()
            val speed = buffer.readUShort()
            val rotation = buffer.readUByte()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                if (spotAnimId == -1) {
                    val class282Sub50Sub1022 = client.ACTIVE_GFX.get(x.toLong() shl 16 or y.toLong())
                    if (class282Sub50Sub1022 != null) {
                        class282Sub50Sub1022.objectGfx.resetCycle()
                        class282Sub50Sub1022.unlink()
                    }
                } else {
                    val tileX = x * 512 + 256
                    val tileY = y * 512 + 256
                    var adjustedPlane = UpdateZonePacket.UPDATE_ZONE_PLANE
                    if (adjustedPlane < 3 && Resource.mapRegion.getSettingsBits().areRoofsHidden(x, y)) {
                        adjustedPlane++
                    }
                    val spotAnimation = SpotAnimation(Resource.mapRegion.getScene(), spotAnimId, speed, UpdateZonePacket.UPDATE_ZONE_PLANE, adjustedPlane, tileX, MapRegion.tileheightForAbsolutePosition(tileX, tileY, UpdateZonePacket.UPDATE_ZONE_PLANE) - height, tileY, x, x, y, y, rotation, false)
                    client.ACTIVE_GFX.put(PositionedGfx(spotAnimation), x.toLong() shl 16 or y.toLong())
                }
            }
        } else if (packet == UpdateZonePacket.MAP_PROJANIM_HALFSQ) {
            val localCoordsHash = buffer.readUByte()
            var localX = (localCoordsHash shr 4 and 0xf) + UpdateZonePacket.UPDATE_ZONE_X * 2
            var localY = UpdateZonePacket.UPDATE_ZONE_Y * 2 + (localCoordsHash and 0xf)
            val flags = buffer.readUByte()
            val adjustToTerrainHeight = (flags and 0x1) != 0
            val adjustToBASAnimFrameHeight = (flags and 0x2) != 0
            val basOffIdx = if (adjustToBASAnimFrameHeight) flags shr 2 else -1
            var xOff = localX + buffer.readByte()
            var yOff = localY + buffer.readByte()
            val source = buffer.readShort()
            val lockOn = buffer.readShort()
            val spotAnimId = buffer.readUShort()
            var startHeight = buffer.readUByte()
            if (adjustToBASAnimFrameHeight) {
                startHeight = startHeight.toByte().toInt()
            } else {
                startHeight *= 4
            }
            var endHeight = buffer.readUByte() * 4
            val startTime = buffer.readUShort()
            val endTime = buffer.readUShort()
            var angle = buffer.readUByte()
            var slope = buffer.readUShort()
            if (angle == 255) {
                angle = -1
            }
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() * 2 && localY < Resource.mapRegion.getSceneSizeX() * 2 && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeY() * 2 && yOff < Resource.mapRegion.getSceneSizeY() * 2 && spotAnimId != 65535) {
                localX *= 256
                localY *= 256
                xOff *= 256
                yOff *= 256
                startHeight = startHeight shl 2
                endHeight = endHeight shl 2
                slope = slope shl 2
                ProjectileSpawnRequest.create(spotAnimId, source, lockOn, basOffIdx, startHeight, endHeight, localX, localY, xOff, yOff, startTime, endTime, angle, slope, adjustToTerrainHeight)
            }
        } else if (packet == UpdateZonePacket.MAP_PROJANIM) {
            val flags = buffer.readUByte()
            val useFloorHeight = (flags and 0x80) != 0
            var localX = (flags shr 3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            var localY = (flags and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var xOff = localX + buffer.readByte()
            var yOff = localY + buffer.readByte()
            val lockOn = buffer.readShort()
            val spotAnimId = buffer.readUShort()
            var startHeight = buffer.readUByte() * 4
            var endHeight = buffer.readUByte() * 4
            val startTime = buffer.readUShort()
            val endTime = buffer.readUShort()
            var angle = buffer.readUByte()
            var slope = buffer.readUShort()
            if (angle == 255) {
                angle = -1
            }
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY() && xOff >= 0 && yOff >= 0 && xOff < Resource.mapRegion.getSceneSizeX() && yOff < Resource.mapRegion.getSceneSizeY() && spotAnimId != 65535) {
                localX = localX * 512 + 256
                localY = localY * 512 + 256
                xOff = xOff * 512 + 256
                yOff = yOff * 512 + 256
                startHeight = startHeight shl 2
                endHeight = endHeight shl 2
                slope = slope shl 2
                val p = Projectile(Resource.mapRegion.getScene(), spotAnimId, UpdateZonePacket.UPDATE_ZONE_PLANE, UpdateZonePacket.UPDATE_ZONE_PLANE, localX, localY, startHeight, startTime + client.cycles, endTime + client.cycles, angle, slope, 0, lockOn, endHeight, useFloorHeight, -1)
                p.start(xOff, yOff, MapRegion.tileheightForAbsolutePosition(xOff, yOff, UpdateZonePacket.UPDATE_ZONE_PLANE) - endHeight, startTime + client.cycles)
                client.PROJECTILES.addFirst(ProjectileSpawnRequest(p))
            }
        } else if (packet == UpdateZonePacket.MIDI_SONG_LOCATION) {
            val i_3 = buffer.readUByte()
            val i_21 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var i_6 = buffer.readUShort()
            if (i_6 == 65535) {
                i_6 = -1
            }
            val i_7 = buffer.readUByte()
            val i_8 = i_7 shr 4 and 0xf
            val i_9 = i_7 and 0x7
            val i_10 = buffer.readUByte()
            val i_11 = buffer.readUByte()
            val i_23 = buffer.readUShort()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && i_21 >= 0 && i_5 >= 0 && i_21 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                val i_24 = i_8 + 1
                if (PlayerUpdate.MY_PLAYER.movementBaseX[0] >= i_21 - i_24 && PlayerUpdate.MY_PLAYER.movementBaseX[0] <= i_21 + i_24 && PlayerUpdate.MY_PLAYER.movementBaseY[0] >= i_5 - i_24 && PlayerUpdate.MY_PLAYER.movementBaseY[0] <= i_24 + i_5) {
                    AreaSoundPlayer.playSoundSynth(i_6, i_9, i_10, i_11, i_8 + (UpdateZonePacket.UPDATE_ZONE_PLANE shl 24) + (i_5 shl 8) + (i_21 shl 16), i_23)
                }
            }
        } else if (packet == UpdateZonePacket.DESTROY_OBJECT) {
            val display = buffer.readUByteAdd()
            val shape = display shr 2
            val rotation = display and 0x3
            val type = client.OBJECT_TYPE_SLOTS[shape]
            val coords = buffer.readUByte()
            val x = (coords shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (coords and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            if (Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations() || x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() && y < Resource.mapRegion.getSceneSizeY()) {
                LocAction.destroyObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, type, -1, shape, rotation)
            }
        } else if (packet == UpdateZonePacket.REMOVE_GROUND_ITEM) {
            val i_3 = buffer.readUByteInverse()
            val coordgrid = Resource.mapRegion.getBaseCoordinates()
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val i_6 = i_5 + coordgrid.y
            val i_7 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_8 = i_7 + coordgrid.x
            val i_9 = buffer.readUShortAddLittle()
            val class282Sub2939 = client.GROUND_ITEMS.get(UpdateZonePacket.UPDATE_ZONE_PLANE.toLong() shl 28 or (i_6.toLong() shl 14) or i_8.toLong())
            if (class282Sub2939 != null) {
                var class282Sub3426 = class282Sub2939.groundItems.getFirst() as GroundItemNode?
                while (class282Sub3426 != null) {
                    if ((i_9 and 0x7fff) == class282Sub3426.id) {
                        class282Sub3426.unlink()
                        break
                    }
                    class282Sub3426 = class282Sub2939.groundItems.next() as GroundItemNode?
                }
                if (class282Sub2939.groundItems.hasNext()) {
                    class282Sub2939.unlink()
                }
                if (i_7 >= 0 && i_5 >= 0 && i_7 < Resource.mapRegion.getSceneSizeX() && i_5 < Resource.mapRegion.getSceneSizeY()) {
                    GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, i_7, i_5)
                }
            }
        } else if (packet == UpdateZonePacket.CUSTOMIZE_OBJECT) {
            val locId = buffer.readInt()
            val locType = Resource.mapRegion.getLoctypeList().list(locId)
            val i_5 = buffer.readUByteAdd()
            var locShape = i_5 shr 2
            val type = client.OBJECT_TYPE_SLOTS[locShape]
            val transformFlags = buffer.readUByte()
            val coords = buffer.readUByteAdd()
            val x = (coords shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (coords and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            if (locShape == ObjectShapes.GROUND_INTERACT.shape) {
                locShape = ObjectShapes.SCENERY_INTERACT.shape
            }
            var modelCount = 0
            if (locType.shapes != null) {
                var i_24 = -1
                for (i_14 in locType.shapes.indices) {
                    if (locShape == locType.shapes[i_14].toInt()) {
                        i_24 = i_14
                        break
                    }
                }
                modelCount = locType.objectModelIds[i_24].size
            }
            var recolCount = 0
            if (locType.recolorDst != null) {
                recolCount = locType.recolorDst.size
            }
            var retexCount = 0
            if (locType.retextureDst != null) {
                retexCount = locType.retextureDst.size
            }
            if ((transformFlags and 0x1) == 1) {
                LocAction.customizeObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, type, locId, locShape, null)
            } else {
                var locModelTransform: IntArray? = null
                if ((transformFlags and 0x2) == 2) {
                    locModelTransform = IntArray(modelCount)
                    for (index in 0..<modelCount) {
                        locModelTransform[index] = buffer.readInt()
                    }
                }
                var locColorTransform: ShortArray? = null
                if ((transformFlags and 0x4) == 4) {
                    locColorTransform = ShortArray(recolCount)
                    for (index in 0..<recolCount) {
                        locColorTransform[index] = buffer.readUShort().toShort()
                    }
                }
                var locTextureTransform: ShortArray? = null
                if ((transformFlags and 0x8) == 8) {
                    locTextureTransform = ShortArray(retexCount)
                    for (index in 0..<retexCount) {
                        locTextureTransform[index] = buffer.readUShort().toShort()
                    }
                }
                LocAction.customizeObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, type, locId, locShape, ObjectTypeCustomisation(ObjectTypeCustomisation.locCustomisationId++, locModelTransform, locColorTransform, locTextureTransform))
            }
        } else if (packet == UpdateZonePacket.TILE_MESSAGE) {
            buffer.readUByte()
            val i_3 = buffer.readUByte()
            val i_21 = (i_3 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_5 = (i_3 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val i_6 = buffer.readUShort()
            val i_7 = buffer.readUByte()
            val i_8 = buffer.readUMedium()
            val string_27 = buffer.readString()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE) {
                TileMessage.offer(UpdateZonePacket.UPDATE_ZONE_PLANE, i_21, i_5, i_7, i_6, i_8, string_27)
            }
        } else if (packet == UpdateZonePacket.SOUND_AREA) { //sound related again
            val flags = buffer.readUByte()
            val localX = (flags shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val localY = (flags and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            var soundId = buffer.readUShort()
            if (soundId == 65535) {
                soundId = -1
            }
            val i_7 = buffer.readUByte()
            val i_8 = i_7 shr 4 and 0xf
            val timesRepeated = i_7 and 0x7
            val delay = buffer.readUByte()
            val volume = buffer.readUByte()
            val sampleRate = buffer.readUShort()
            val voiceEffect = buffer.readUByte() == 1
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE && localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY()) {
                val i_14 = i_8 + 1
                if (PlayerUpdate.MY_PLAYER.movementBaseX[0] >= localX - i_14 && PlayerUpdate.MY_PLAYER.movementBaseX[0] <= localX + i_14 && PlayerUpdate.MY_PLAYER.movementBaseY[0] >= localY - i_14 && PlayerUpdate.MY_PLAYER.movementBaseY[0] <= i_14 + localY) {
                    AreaSoundPlayer.playSoundVorbis(soundId, timesRepeated, delay, volume, i_8 + (UpdateZonePacket.UPDATE_ZONE_PLANE shl 24) + (localY shl 8) + (localX shl 16), voiceEffect, sampleRate)
                }
            }
        } else if (packet == UpdateZonePacket.OBJ_ANIM) {
            val i_3 = buffer.readUByteSubtract()
            val type = i_3 shr 2
            val rotation = i_3 and 0x3
            val slot = client.OBJECT_TYPE_SLOTS[type]
            val i_7 = buffer.readUByteSubtract()
            val x = (i_7 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val y = (i_7 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val animationId = buffer.readUIntLittle()
            if (Resource.mapRegion.getLoadingType() !== RegionLoadingType.CUTSCENE) {
                SceneGraph.animateObject(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, slot, type, rotation, animationId)
            }
        } else if (packet == UpdateZonePacket.GROUND_ITEM_REVEAL) {
            val playerId = buffer.readUShort()
            val i_21 = buffer.readUByte()
            val coordgrid25 = Resource.mapRegion.getBaseCoordinates()
            val localY = (i_21 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val y = localY + coordgrid25.y
            val localX = (i_21 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val x = localX + coordgrid25.x
            val amount = buffer.readUShortAddLittle()
            val id = buffer.readUShortAddLittle()
            if (playerId != PlayerUpdate.myPlayerIndex) {
                val withinSceneRange = localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY()
                if (withinSceneRange || Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations()) {
                    GroundItemUpdating.addGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, GroundItemNode(id, amount))
                    if (withinSceneRange) {
                        GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, localX, localY)
                    }
                }
            }
        } else if (packet == UpdateZonePacket.CREATE_GROUND_ITEM) {
            val amount = buffer.readUShortAddLittle()
            val id = buffer.readUShortAdd()
            val i_5 = buffer.readUByteInverse()
            val coordgrid31 = Resource.mapRegion.getBaseCoordinates()
            val localY = (i_5 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val y = localY + coordgrid31.y
            val localX = (i_5 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val x = localX + coordgrid31.x
            val bool36 = localX >= 0 && localY >= 0 && localX < Resource.mapRegion.getSceneSizeX() && localY < Resource.mapRegion.getSceneSizeY()
            if (bool36 || Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations()) {
                GroundItemUpdating.addGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, x, y, GroundItemNode(id, amount))
                if (bool36) {
                    GroundItemUpdating.removeGroundItem(UpdateZonePacket.UPDATE_ZONE_PLANE, localX, localY)
                }
            }
        } else if (packet == UpdateZonePacket.CREATE_OBJECT) {
            val i_3 = buffer.readInt()
            val i_21 = buffer.readUByteSubtract()
            val i_5 = (i_21 shr 4 and 0x7) + UpdateZonePacket.UPDATE_ZONE_X
            val i_6 = (i_21 and 0x7) + UpdateZonePacket.UPDATE_ZONE_Y
            val i_7 = buffer.readUByteSubtract()
            val i_8 = i_7 shr 2
            val i_9 = i_7 and 0x3
            val i_10 = client.OBJECT_TYPE_SLOTS[i_8]
            if (Resource.mapRegion.getLoadingType().ignoreUpdateZoneSizeLimitations() || i_5 >= 0 && i_6 >= 0 && i_5 < Resource.mapRegion.getSceneSizeX() && i_6 < Resource.mapRegion.getSceneSizeY()) {
                LocAction.destroyObject(UpdateZonePacket.UPDATE_ZONE_PLANE, i_5, i_6, i_10, i_3, i_8, i_9)
            }
        } else {
            PacketEncoder.sendErrorToServer("" + packet, RuntimeException())
            client.logout(false)
        }
    }

    fun decodeWorldList(refresh: Boolean, data: ByteArray) {
        if (WorldMeta.WORLD_LIST_BUFFER == null) WorldMeta.WORLD_LIST_BUFFER = Packet(20000)
        WorldMeta.WORLD_LIST_BUFFER.writeBytes(data, 0, data.size)
        if (refresh) {
            WorldDescriptor.decodeWorldList(WorldMeta.WORLD_LIST_BUFFER.buffer)
            WorldConnection.WORLD_DESCRIPTORS_BYID = arrayOfNulls<WorldDescriptor>(WorldMeta.WORLD_LIST_SIZE)
            var worldNumber = 0
            for (worldIndex in WorldDescriptor.WORLD_LIST_START..WorldMeta.WORLD_LIST_SIZEPLUS1) {
                val world: WorldDescriptor? = WorldDescriptor.getWorld(worldIndex)
                if (world != null) WorldConnection.WORLD_DESCRIPTORS_BYID[worldNumber++] = world
            }
            WorldConnection.requestingWorldList = false
            WorldConnection.worldListReceivedTime = Utils.time()
            WorldMeta.WORLD_LIST_BUFFER = null
        }
    }

}
