package com.rs.net.encoders

import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.model.WorldProjectile
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.item.ItemTooltips
import com.rs.game.model.entity.player.managers.InterfaceManager.Sub
import com.rs.game.model.gameobject.GameObject
import com.rs.game.model.gameobject.ObjectMeshModifier
import com.rs.game.model.item.ItemsContainer
import com.rs.net.update.EntitySnapshotFactory
import com.rs.utils.MapXTEAs
import java.io.ByteArrayOutputStream
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.Animation
import org.darkan.core.model.ChatMessageType
import org.darkan.core.model.GroundItem
import org.darkan.core.model.HintIcon
import org.darkan.core.model.IFEvents as IFEventsModel
import org.darkan.core.model.Item
import org.darkan.core.model.RegionSize
import org.darkan.core.model.Sound
import org.darkan.core.model.SoundType
import org.darkan.core.model.SpotAnim
import org.darkan.core.net.Session
import org.darkan.core.net.prot.dto.GrandExchangeOffer
import org.darkan.core.net.prot.dto.InvSlotUpdate
import org.darkan.core.net.prot.packet.BlockMinimapState
import org.darkan.core.net.prot.packet.BuildInstancedRegion
import org.darkan.core.net.prot.packet.BuildRegion
import org.darkan.core.net.prot.packet.CamLookAt
import org.darkan.core.net.prot.packet.CamMoveTo
import org.darkan.core.net.prot.packet.CamResetHard
import org.darkan.core.net.prot.packet.CamResetSmooth
import org.darkan.core.net.prot.packet.CamShake
import org.darkan.core.net.prot.packet.ChatFilterSettings
import org.darkan.core.net.prot.packet.ChatFilterSettingsPrivateChat
import org.darkan.core.net.prot.packet.ClearVarps
import org.darkan.core.net.prot.packet.CreateGroundItem
import org.darkan.core.net.prot.packet.CreateObject
import org.darkan.core.net.prot.packet.CustomizeObject
import org.darkan.core.net.prot.packet.Cutscene
import org.darkan.core.net.prot.packet.DestroyObject
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.net.prot.packet.GroundItemCount
import org.darkan.core.net.prot.packet.HintArrow
import org.darkan.core.net.prot.packet.HintTrail
import org.darkan.core.net.prot.packet.IfCloseSub
import org.darkan.core.net.prot.packet.IfOpenSub
import org.darkan.core.net.prot.packet.IfOpenSubActiveGroundItem
import org.darkan.core.net.prot.packet.IfOpenSubActiveNpc
import org.darkan.core.net.prot.packet.IfOpenSubActiveObject
import org.darkan.core.net.prot.packet.IfOpenSubActivePlayer
import org.darkan.core.net.prot.packet.IfOpenTop
import org.darkan.core.net.prot.packet.IfSetAngle
import org.darkan.core.net.prot.packet.IfSetAnim
import org.darkan.core.net.prot.packet.IfSetEvents
import org.darkan.core.net.prot.packet.IfSetGraphic
import org.darkan.core.net.prot.packet.IfSetHide
import org.darkan.core.net.prot.packet.IfSetItem
import org.darkan.core.net.prot.packet.IfSetModel
import org.darkan.core.net.prot.packet.IfSetNpcHead
import org.darkan.core.net.prot.packet.IfSetPlayerHead
import org.darkan.core.net.prot.packet.IfSetPosition
import org.darkan.core.net.prot.packet.IfSetTargetParam
import org.darkan.core.net.prot.packet.IfSetText
import org.darkan.core.net.prot.packet.LogoutFull
import org.darkan.core.net.prot.packet.LogoutLobby
import org.darkan.core.net.prot.packet.MapProjAnim
import org.darkan.core.net.prot.packet.MessagePrivate
import org.darkan.core.net.prot.packet.MessagePublic
import org.darkan.core.net.prot.packet.MessageQuickChatPrivate
import org.darkan.core.net.prot.packet.MusicEffect
import org.darkan.core.net.prot.packet.MusicTrack
import org.darkan.core.net.prot.packet.NpcUpdate
import org.darkan.core.net.prot.packet.NpcUpdateLarge
import org.darkan.core.net.prot.packet.ObjAnim
import org.darkan.core.net.prot.packet.OpenUrl
import org.darkan.core.net.prot.packet.PlayerOption
import org.darkan.core.net.prot.packet.PlayerUpdate
import org.darkan.core.net.prot.packet.PlayerWeight
import org.darkan.core.net.prot.packet.RemoveGroundItem
import org.darkan.core.net.prot.packet.ResetSounds
import org.darkan.core.net.prot.packet.RunEnergy
import org.darkan.core.net.prot.packet.RunScript
import org.darkan.core.net.prot.packet.SetCursor
import org.darkan.core.net.prot.packet.SetDrawOrder
import org.darkan.core.net.prot.packet.SoundSynth
import org.darkan.core.net.prot.packet.SpotAnimSpecific
import org.darkan.core.net.prot.packet.TileMessage
import org.darkan.core.net.prot.packet.TriggerOnDialogAbort
import org.darkan.core.net.prot.packet.UpdateGESlot
import org.darkan.core.net.prot.packet.UpdateInvFull
import org.darkan.core.net.prot.packet.UpdateInvPartial
import org.darkan.core.net.prot.packet.UpdateRebootTimer
import org.darkan.core.net.prot.packet.UpdateStat
import org.darkan.core.net.prot.packet.UpdateZonePartialFollows
import org.darkan.core.net.prot.packet.VarclanSetByte
import org.darkan.core.net.prot.packet.VarclanSetInt
import org.darkan.core.net.prot.packet.VarclanSetLong
import org.darkan.core.net.prot.packet.VarclanSetString
import org.darkan.core.net.prot.packet.VorbisSound
import org.darkan.core.net.prot.packet.VorbisSpeechSound
import org.darkan.core.net.prot.packet.WorldReconnectDetails
import org.darkan.core.net.prot.packet.setVarbitPacket
import org.darkan.core.net.prot.packet.setVarcPacket
import org.darkan.core.net.prot.packet.setVarcStrPacket
import org.darkan.core.net.prot.packet.setVarpPacket
import org.darkan.core.net.prot.update.NpcUpdateTrackingState
import org.darkan.core.net.prot.update.PlayerUpdateTrackingState
import org.darkan.core.social.ChatMessage
import org.darkan.core.social.PublicChatMessage
import org.darkan.core.social.QuickChatMessage
import org.darkan.core.util.MapUtils
import org.darkan.core.util.Structure
import world.gregs.voidps.cache.Cache
import world.gregs.voidps.cache.definition.cs2.CS2Type
import world.gregs.voidps.type.Tile

class WorldEncoder(private val player: Player, @JvmField val session: Session) {

    val playerTrackingState = PlayerUpdateTrackingState()
    private val invSync = InvSyncState()
    val npcTrackingState = NpcUpdateTrackingState()
    private var localPlayerBlockSent = false

    fun resetNpcTrackingState() {
        npcTrackingState.localCount = 0
    }

    private fun initPlayerTrackingState(regionHashes: IntArray) {
        val state = playerTrackingState
        state.localIndexCount = 0
        state.outIndexCount = 0
        state.localAddedPlayers = 0
        state.totalRenderDataSentLength = 0
        state.localPlayers[player.index] = EntitySnapshotFactory.buildPlayerUpdatePlayer(player, player)
        state.localIndexes[state.localIndexCount++] = player.index
        for (playerIndex in 1 until 2048) {
            if (playerIndex == player.index) continue
            state.regionHashes[playerIndex] = regionHashes[playerIndex]
            state.outIndexes[state.outIndexCount++] = playerIndex
        }
    }

    // ======================== Interface packets ========================

    fun sendWindowsPane(id: Int, type: Int) {
        session.queuePacket(IfOpenTop(id, type, EMPTY_XTEAS))
    }

    fun sendInterface(overlay: Boolean, topId: Int, topChildId: Int, subId: Int) {
        session.queuePacket(IfOpenSub(topId, topChildId, subId, overlay, EMPTY_XTEAS))
    }

    fun sendNPCInterface(npc: NPC, overlay: Boolean, topId: Int, topChildId: Int, subId: Int) {
        session.queuePacket(IfOpenSubActiveNpc(topId, topChildId, subId, overlay, npc.index, EMPTY_XTEAS))
    }

    fun sendObjectInterface(obj: GameObject, overlay: Boolean, topId: Int, topChildId: Int, subId: Int) {
        session.queuePacket(
            IfOpenSubActiveObject(
                topId, topChildId, subId, overlay, EMPTY_XTEAS,
                obj.id, obj.tile, obj.type.id, obj.rotation
            )
        )
    }

    fun sendPlayerInterface(overlay: Boolean, topId: Int, topChildId: Int, subId: Int) {
        session.queuePacket(IfOpenSubActivePlayer(topId, topChildId, subId, overlay, player.index, EMPTY_XTEAS))
    }

    fun sendGroundItemInterface(item: GroundItem, overlay: Boolean, topId: Int, topChildId: Int, subId: Int) {
        session.queuePacket(IfOpenSubActiveGroundItem(topId, topChildId, subId, overlay, item.tile, item.id, EMPTY_XTEAS))
    }

    fun closeInterface(topChildId: Int) {
        session.queuePacket(IfCloseSub(topChildId ushr 16, topChildId and 0xFFFF))
    }

    /**
     * Tells the client to run its dialogue-abort hook, tearing down the enter-amount /
     * enter-name prompt and the chat dialogue. Legacy sent ServerPacket.TRIGGER_ONDIALOGABORT
     * straight from Player#abortDialogue; it lives here because [TriggerOnDialogAbort] is a
     * Kotlin value class and cannot be constructed from Java.
     */
    fun sendTriggerOnDialogAbort() {
        session.queuePacket(TriggerOnDialogAbort())
    }

    fun setIFGraphic(interfaceId: Int, componentId: Int, graphicId: Int) {
        player.interfaceManager?.tracker?.graphic(interfaceId, componentId, graphicId)
        session.queuePacket(IfSetGraphic(interfaceId, componentId, graphicId))
    }

    fun setIFPosition(interfaceId: Int, componentId: Int, x: Int, y: Int) {
        session.queuePacket(IfSetPosition(interfaceId, componentId, x, y))
    }

    fun setIFModel(interfaceId: Int, componentId: Int, modelId: Int) {
        player.interfaceManager?.tracker?.model(interfaceId, componentId, modelId)
        session.queuePacket(IfSetModel(interfaceId, componentId, modelId))
    }

    fun setIFHidden(interfaceId: Int, componentId: Int, hidden: Boolean) {
        player.interfaceManager?.tracker?.hidden(interfaceId, componentId, hidden)
        session.queuePacket(IfSetHide(interfaceId, componentId, hidden))
    }

    fun setIFEvents(params: IFEventsModel) {
        player.interfaceManager?.tracker?.events(params.interfaceId, params.componentId, params.fromSlot, params.toSlot, params.settings)
        session.queuePacket(IfSetEvents(params.interfaceId, params.componentId, params.fromSlot, params.toSlot, params.settings))
    }

    fun setIFRightClickOps(interfaceId: Int, componentId: Int, fromSlot: Int, toSlot: Int, vararg optionsSlots: Int) {
        setIFEvents(IFEventsModel(interfaceId, componentId, fromSlot, toSlot).enableRightClickOptions(*optionsSlots))
    }

    fun setIFTargetParam(interfaceId: Int, componentId: Int, fromSlot: Int, toSlot: Int, targetParam: Int) {
        session.queuePacket(IfSetTargetParam(interfaceId, componentId, targetParam, fromSlot, toSlot))
    }

    fun setIFText(interfaceId: Int, componentId: Int, text: String) {
        player.interfaceManager?.tracker?.text(interfaceId, componentId, text)
        session.queuePacket(IfSetText(interfaceId, componentId, text))
    }

    fun setIFText(component: Int, text: String) {
        setIFText(component shr 16, component and 0xFFFF, text)
    }

    fun setIFAnimation(emoteId: Int, interfaceId: Int, componentId: Int) {
        session.queuePacket(IfSetAnim(interfaceId, componentId, emoteId))
    }

    fun setIFAnimation(emoteId: Int, component: Int) {
        setIFAnimation(emoteId, component shr 16, component and 0xFFFF)
    }

    fun setIFAngle(interfaceId: Int, componentId: Int, pitch: Int, roll: Int, scale: Int) {
        session.queuePacket(IfSetAngle(interfaceId, componentId, pitch, roll, scale))
    }

    fun setIFItem(interfaceId: Int, componentId: Int, id: Int, amount: Int) {
        player.interfaceManager?.tracker?.item(interfaceId, componentId, id, amount)
        session.queuePacket(IfSetItem(interfaceId, componentId, id, amount))
    }

    fun setIFPlayerHead(interfaceId: Int, componentId: Int) {
        session.queuePacket(IfSetPlayerHead(interfaceId, componentId))
    }

    fun setIFNPCHead(interfaceId: Int, componentId: Int, npcId: Int) {
        session.queuePacket(IfSetNpcHead(interfaceId, componentId, npcId))
    }

    fun setIFItem(component: Int, id: Int, amount: Int) {
        setIFItem(component shr 16, component and 0xFFFF, id, amount)
    }

    fun setIFPlayerHead(component: Int) {
        setIFPlayerHead(component shr 16, component and 0xFFFF)
    }

    fun setIFNPCHead(component: Int, npcId: Int) {
        setIFNPCHead(component shr 16, component and 0xFFFF, npcId)
    }

    // ======================== GE / Draw / Hint ========================

    fun updateGESlot(slot: Int, progress: Int, item: Int, price: Int, amount: Int, currAmount: Int, totalPrice: Int) {
        session.queuePacket(UpdateGESlot(slot, GrandExchangeOffer(progress, item, price, amount, currAmount, totalPrice)))
    }

    fun sendDrawOrder(playerFirst: Boolean) {
        session.queuePacket(SetDrawOrder(playerFirst))
    }

    /**
     * Draws hint trail [index] (0-8) from [start] along the first [size] entries of [stepsX]/[stepsY].
     *
     * The trail body is built here because the modern [HintTrail] packet carries it pre-encoded. The
     * route finder hands its steps out destination-first, so they are written back-to-front, as signed
     * byte offsets from [start]. A zero offset pair is written ahead of them - and the step count is
     * padded by one to match - so the trail's first node is the player's own tile; the route finder
     * doesn't include it.
     */
    @JvmOverloads
    fun sendHintTrail(start: Tile, modelId: Int, stepsX: IntArray, stepsY: IntArray, size: Int, index: Int = 0) {
        val body = ByteArrayOutputStream(7 + size * 2)
        // The client reads this length with an offset "unsigned smart": byte - 64, or short - 49152.
        val length = size + 1
        if (length < 64) {
            body.write(length + 64)
        } else {
            body.write((length + 49152) shr 8)
            body.write(length + 49152)
        }
        body.write(start.x shr 8)
        body.write(start.x)
        body.write(start.y shr 8)
        body.write(start.y)
        body.write(0)
        body.write(0)
        for (i in size - 1 downTo 0) {
            body.write(stepsX[i] - start.x)
            body.write(stepsY[i] - start.y)
        }
        session.queuePacket(HintTrail(index, modelId, body.toByteArray()))
    }

    /**
     * Clears hint trail [index]. A model id of -1 is the client's signal to drop the trail, and it stops
     * reading before the body, so no body is sent.
     */
    @JvmOverloads
    fun clearHintTrail(index: Int = 0) {
        session.queuePacket(HintTrail(index, -1, ByteArray(0)))
    }

    fun sendHintIcon(icon: HintIcon) {
        session.queuePacket(
            HintArrow(
                iconIndex = icon.index,
                targetType = icon.targetType,
                arrowGraphic = icon.arrowType,
                plane = icon.plane,
                x = icon.coordX,
                y = icon.coordY,
                targetIndex = icon.targetIndex,
                modelId = icon.modelId,
                distance = icon.distanceFromFloor
            )
        )
    }

    // ======================== Camera packets ========================

    fun sendCameraShake(slotId: Int, v1: Int, v2: Int, v3: Int, v4: Int) {
        session.queuePacket(CamShake(slotId, v1, v2, v3, v4))
    }

    fun sendStopCameraShake() {
        session.queuePacket(CamResetHard())
    }

    fun sendCameraLook(tile: Tile, viewZ: Int) {
        sendCameraLook(tile.getXInScene(player.sceneBaseChunkId), tile.getYInScene(player.sceneBaseChunkId), viewZ)
    }

    fun sendCameraLook(tile: Tile, viewZ: Int, speedToExactDestination: Int, speedOnRoutePath: Int) {
        sendCameraLook(tile.getXInScene(player.sceneBaseChunkId), tile.getYInScene(player.sceneBaseChunkId), viewZ, speedToExactDestination, speedOnRoutePath)
    }

    fun sendCameraLook(viewLocalX: Int, viewLocalY: Int, viewZ: Int) {
        sendCameraLook(viewLocalX, viewLocalY, viewZ, -1, -1)
    }

    fun sendCameraLook(viewLocalX: Int, viewLocalY: Int, viewZ: Int, speedToExactDestination: Int, speedOnRoutePath: Int) {
        session.queuePacket(CamLookAt(viewLocalX, viewLocalY, viewZ, speedToExactDestination, speedOnRoutePath))
    }

    fun sendResetCamera() {
        session.queuePacket(CamResetHard())
    }

    fun sendResetCameraSoft() {
        session.queuePacket(CamResetSmooth())
    }

    fun sendCameraPos(tile: Tile, z: Int) {
        sendCameraPos(tile.getXInScene(player.sceneBaseChunkId), tile.getYInScene(player.sceneBaseChunkId), z)
    }

    fun sendCameraPos(tile: Tile, z: Int, speedToTile: Int, speedToExactDestination: Int) {
        sendCameraPos(tile.getXInScene(player.sceneBaseChunkId), tile.getYInScene(player.sceneBaseChunkId), z, speedToTile, speedToExactDestination)
    }

    fun sendCameraPos(moveLocalX: Int, moveLocalY: Int, moveZ: Int) {
        sendCameraPos(moveLocalX, moveLocalY, moveZ, -1, -1)
    }

    /**
     * @param speedToTile defines speed of the camera to the world tile with the default height then the exact position
     * @param speedToExactDestination defines speed of camera to the exact position specified by previous parameters.
     */
    fun sendCameraPos(moveLocalX: Int, moveLocalY: Int, moveZ: Int, speedToTile: Int, speedToExactDestination: Int) {
        session.queuePacket(CamMoveTo(moveLocalX, moveLocalY, moveZ, speedToTile, speedToExactDestination))
    }

    // ======================== RunScript ========================

    fun sendRunScript(scriptId: Int, vararg params: Any) {
        val l = params.toMutableList()
        l.reverse()
        sendRunScriptReverse(scriptId, *l.toTypedArray())
    }

    fun setIFSlotGraphic(interfaceId: Int, componentId: Int, slotId: Int, graphicId: Int) {
        sendRunScript(1351, Utils.toInterfaceHash(interfaceId, componentId), slotId, graphicId)
    }

    fun setIFText(interfaceId: Int, componentId: Int, slotId: Int, text: String) {
        sendRunScript(6566, Utils.toInterfaceHash(interfaceId, componentId), slotId, text)
    }

    fun sendRunScriptReverse(scriptId: Int, vararg params: Any) {
        session.queuePacket(RunScript(scriptId, arrayOf(*params)))
    }

    // ======================== Var packets ========================

    fun sendVarc(id: Int, value: Int) {
        session.queuePacket(setVarcPacket(id, value))
    }

    fun sendVarcString(id: Int, string: String) {
        session.queuePacket(setVarcStrPacket(id, string))
    }

    @Deprecated("Use modern var system")
    fun sendVar(id: Int, value: Int) {
        session.queuePacket(setVarpPacket(id, value))
    }

    @Deprecated("Use modern var system")
    fun sendVarBit(id: Int, value: Int) {
        session.queuePacket(setVarbitPacket(id, value))
    }

    @Deprecated("Use modern var system")
    fun clearVarps() {
        session.queuePacket(ClearVarps())
    }

    fun setClanVar(varId: Int, value: Any) {
        val def = Cache.clanVars[varId]
        when (def.type) {
            CS2Type.STRING -> session.queuePacket(VarclanSetString(varId, value as String))
            CS2Type.LONG -> session.queuePacket(VarclanSetLong(varId, (value as Number).toLong()))
            CS2Type.INT -> {
                val intVal = (value as Number).toInt()
                if (intVal < Byte.MIN_VALUE || intVal > Byte.MAX_VALUE)
                    session.queuePacket(VarclanSetInt(varId, intVal))
                else
                    session.queuePacket(VarclanSetByte(varId, intVal))
            }
            else -> error("Mismatching types for clan var $varId")
        }
    }

    // ======================== Player stats / weight / energy ========================

    fun sendRunEnergy(energy: Double) {
        session.queuePacket(RunEnergy(energy.toInt()))
    }

    fun refreshWeight(weight: Double) {
        session.queuePacket(PlayerWeight(weight.toInt()))
    }

    fun updateStats(vararg skills: Int) {
        for (skill in skills) {
            session.queuePacket(UpdateStat(player.skills.getXp(skill).toInt(), skill, player.skills.getLevel(skill)))
        }
    }

    // ======================== Zone update packets ========================

    private fun sendZoneHeader(tile: Tile) {
        val baseCoords = MapUtils.decode(Structure.CHUNK, player.sceneBaseChunkId)
        val localCoords = MapUtils.decode(Structure.CHUNK, tile.getChunkId())
        session.queuePacket(
            UpdateZonePartialFollows(
                localCoords[0] - baseCoords[0],
                localCoords[1] - baseCoords[1],
                localCoords[2]
            )
        )
    }

    fun sendObjectAnimation(obj: GameObject, animation: Animation) {
        sendZoneHeader(obj.tile)
        session.queuePacket(ObjAnim(obj.tile.getXInChunk(), obj.tile.getYInChunk(), obj.type.id, obj.rotation, animation.primaryId))
    }

    fun removeGroundItem(item: GroundItem) {
        sendZoneHeader(item.tile)
        session.queuePacket(RemoveGroundItem(item.tile.getXInChunk(), item.tile.getYInChunk(), item.id))
    }

    fun sendGroundItem(item: GroundItem) {
        sendZoneHeader(item.tile)
        session.queuePacket(CreateGroundItem(item.tile.getXInChunk(), item.tile.getYInChunk(), item.id, item.amount))
    }

    fun sendSetGroundItemAmount(item: GroundItem, oldAmount: Int) {
        sendZoneHeader(item.tile)
        session.queuePacket(GroundItemCount(item.tile.getXInChunk(), item.tile.getYInChunk(), item.id, oldAmount, item.amount))
    }

    fun sendProjectile(projectile: WorldProjectile) {
        sendZoneHeader(projectile.source)
        val flags = if (projectile.useTerrainHeight) 0x80 else 0
        session.queuePacket(
            MapProjAnim(
                localX = projectile.source.getXInChunk(),
                localY = projectile.source.getYInChunk(),
                xOffset = projectile.destination.x - projectile.source.x,
                yOffset = projectile.destination.y - projectile.source.y,
                lockOn = projectile.lockOnId,
                spotAnimId = projectile.spotAnimId,
                startHeight = projectile.startHeight,
                endHeight = projectile.endHeight,
                startTime = projectile.startDelayClientCycles,
                endTime = projectile.startDelayClientCycles + projectile.inAirClientCycles,
                angle = projectile.angle,
                slope = projectile.offset,
                useFloorHeight = projectile.useTerrainHeight
            )
        )
    }

    fun sendTileMessage(message: String, tile: Tile, delay: Int, height: Int, color: Int) {
        sendZoneHeader(tile)
        session.queuePacket(TileMessage(tile.getXInChunk(), tile.getYInChunk(), height, delay, color, message))
    }

    fun sendTileMessage(message: String, tile: Tile, color: Int) {
        sendTileMessage(message, tile, 5000, 255, color)
    }

    fun sendRemoveObject(obj: GameObject) {
        sendZoneHeader(obj.tile)
        session.queuePacket(DestroyObject(obj.tile.getXInChunk(), obj.tile.getYInChunk(), obj.type.id, obj.rotation))
    }

    fun sendAddObject(obj: GameObject) {
        sendZoneHeader(obj.tile)
        session.queuePacket(CreateObject(obj.tile.getXInChunk(), obj.tile.getYInChunk(), obj.id, obj.type.id, obj.rotation))
        if (obj.meshModifier != null)
            sendCustomizeObject(obj.meshModifier)
    }

    fun sendCustomizeObject(modifier: ObjectMeshModifier) {
        val obj = modifier.`object`
        sendZoneHeader(obj.tile)
        session.queuePacket(
            CustomizeObject(
                localX = obj.tile.getXInChunk(),
                localY = obj.tile.getYInChunk(),
                objectId = obj.id,
                shape = obj.type.id,
                rotation = obj.rotation,
                modelIds = modifier.modelIds,
                recolors = modifier.modifiedColors?.let { arr -> ShortArray(arr.size) { arr[it].toShort() } },
                retextures = modifier.modifiedTextures?.let { arr -> ShortArray(arr.size) { arr[it].toShort() } }
            )
        )
    }

    // ======================== Message packets ========================

    fun sendMessage(type: ChatMessageType, text: String, p: Player?) {
        session.queuePacket(GameMessage(type, text, p?.account?.displayName))
    }

    fun sendPublicMessage(p: Player, message: ChatMessage) {
        session.queuePacket(MessagePublic(p.index, p.messageIcon, message))
    }

    fun sendPrivateMessage(username: String, message: String) {
        session.queuePacket(MessagePrivate(player.account.rights.crown, player.displayName, player.displayName, message))
    }

    fun sendPrivateQuickMessage(username: String, message: QuickChatMessage) {
        session.queuePacket(MessageQuickChatPrivate(player.account.rights.crown, player.displayName, player.displayName, message))
    }

    fun sendChatFilterSettings() {
        session.queuePacket(ChatFilterSettings(player.tradeStatus, player.publicStatus))
    }

    fun sendChatFilterSettingsPrivateChat() {
        session.queuePacket(ChatFilterSettingsPrivateChat(player.account.social.status.toInt()))
    }

    // ======================== Map region packets ========================

    fun sendDynamicMapRegion() {
        val chunks = BuildInstancedRegion.chunks(player.chunkX, player.chunkY, player.mapSize, ::instancedChunkDescriptor)
        val otherPlayerRegionIds = takeLocalPlayerBlock()
        val withLocalPlayer = otherPlayerRegionIds != null
        session.queuePacket(
            BuildInstancedRegion(
                mapSize = player.mapSize,
                chunks = chunks,
                xteas = MapRegionKeys.forChunks(chunks, MapXTEAs::getMapKeys),
                chunkX = player.chunkX,
                chunkY = player.chunkY,
                forceMapRefresh = player.isForceNextMapLoadRefresh,
                localPlayerPid = if (withLocalPlayer) player.index else null,
                localPlayerBaseTileHash = if (withLocalPlayer) player.tile.id else null,
                otherPlayerRegionIds = otherPlayerRegionIds,
            )
        )
    }

    private fun instancedChunkDescriptor(chunkX: Int, chunkY: Int, plane: Int): Int {
        val chunk = ChunkManager.getChunk(Structure.CHUNK.encode(chunkX, chunkY, plane))
        if (chunk.renderChunkX == 0 || chunk.renderChunkY == 0)
            return BuildInstancedRegion.ABSENT_CHUNK
        return BuildInstancedRegion.chunkDescriptor(chunk.renderChunkX, chunk.renderChunkY, chunk.renderPlane, chunk.rotation)
    }

    fun sendMapRegion() {
        val regionIds = BuildRegion.regionIds(player.chunkX, player.chunkY, player.mapSize)
        val otherPlayerRegionIds = takeLocalPlayerBlock()
        val withLocalPlayer = otherPlayerRegionIds != null
        session.queuePacket(
            BuildRegion(
                regionIds = regionIds,
                mapSize = player.mapSize,
                xteas = MapRegionKeys.forRegions(regionIds, MapXTEAs::getMapKeys),
                chunkX = player.chunkX,
                chunkY = player.chunkY,
                forceMapRefresh = player.isForceNextMapLoadRefresh,
                localPlayerPid = if (withLocalPlayer) player.index else null,
                localPlayerBaseTileHash = if (withLocalPlayer) player.tile.id else null,
                otherPlayerRegionIds = otherPlayerRegionIds,
            )
        )
    }

    fun sendReconnectDetails() {
        val otherPlayerRegionIds = checkNotNull(takeLocalPlayerBlock()) { "Reconnect details must be the first thing a fresh encoder sends" }
        session.queuePacket(WorldReconnectDetails(player.index, player.tile.id, otherPlayerRegionIds), noIsaac = true)
    }

    private fun takeLocalPlayerBlock(): IntArray? {
        if (localPlayerBlockSent)
            return null
        localPlayerBlockSent = true
        val regionHashes = buildOtherPlayerRegionIds()
        initPlayerTrackingState(regionHashes)
        return regionHashes
    }

    private fun buildOtherPlayerRegionIds(): IntArray {
        val regionIds = IntArray(2048)
        for (playerIndex in 1 until 2048) {
            val p: Player? = World.players[playerIndex]
            regionIds[playerIndex] = p?.getRegionHash() ?: 0
        }
        return regionIds
    }

    // ======================== Cutscene ========================

    fun sendCutscene(id: Int) {
        // TODO: Load cutscene areas from cache to provide XTEA keys
        session.queuePacket(Cutscene(id, emptyArray(), player.appearance.appeareanceData!!))
    }

    // ======================== Player option ========================

    @Deprecated("Use modern player option system")
    fun sendPlayerOption(option: String, slot: Int, top: Boolean) {
        sendPlayerOption(option, slot, top, -1)
    }

    @Deprecated("Use modern player option system")
    fun sendPlayerOption(option: String, slot: Int, top: Boolean, cursor: Int) {
        session.queuePacket(PlayerOption(option, slot, top, cursor))
    }

    // ======================== Entity updates ========================

    fun sendLocalPlayersUpdate() {
        val snapshot = EntitySnapshotFactory.buildPlayerSnapshot(player)
        val bytes = session.codec.updateProtocol!!.encodePlayerUpdate(snapshot, playerTrackingState)
        session.queuePacket(PlayerUpdate(bytes))
    }

    fun sendLocalNPCsUpdate() {
        val snapshot = EntitySnapshotFactory.buildNpcSnapshot(player)
        val bytes = session.codec.updateProtocol!!.encodeNpcUpdate(snapshot, npcTrackingState)
        if (player.hasLargeSceneView())
            session.queuePacket(NpcUpdateLarge(bytes))
        else
            session.queuePacket(NpcUpdate(bytes))
    }

    // ======================== Spot anim ========================

    fun sendSpotAnim(spotAnim: SpotAnim, target: Any) {
        val targetHash = when (target) {
            is Player -> (target.index and 0xffff) or (1 shl 28)
            is NPC -> (target.index and 0xffff) or (1 shl 29)
            is Tile -> (target.plane shl 28) or (target.x shl 14) or (target.y and 0x3fff) or (1 shl 30)
            else -> 0
        }
        session.queuePacket(
            SpotAnimSpecific(
                slot = 0,
                spotAnimId = spotAnim.id,
                settings = spotAnim.settingsHash,
                speed = spotAnim.speed,
                targetHash = targetHash,
                height = spotAnim.height
            )
        )
    }

    // ======================== System / misc ========================

    fun sendSystemUpdate(delay: Int) {
        session.queuePacket(UpdateRebootTimer(delay))
    }

    fun sendUpdateItems(key: Int, negativeKey: Boolean, items: Array<Item?>, vararg slots: Int) {
        val updates = Array(slots.size) { i ->
            val slot = slots[i]
            val item = if (slot in items.indices) items[slot] else null
            invSync.update(key, negativeKey, slot, item, ItemTooltips.of(player, item))
        }
        session.queuePacket(UpdateInvPartial(key, negativeKey, updates))
    }

    fun sendUpdateItems(key: Int, items: ItemsContainer<*>, vararg slots: Int) {
        sendUpdateItems(key, items.array(), *slots)
    }

    fun sendUpdateItems(key: Int, items: Array<Item?>, vararg slots: Int) {
        sendUpdateItems(key, key < 0, items, *slots)
    }

    fun sendItems(key: Int, negativeKey: Boolean, items: Array<Item?>) {
        val itemIds = IntArray(items.size) { items[it]?.id ?: -1 }
        val amounts = IntArray(items.size) { items[it]?.amount ?: 0 }
        val tooltips = Array(items.size) { ItemTooltips.of(player, items[it]) }
        invSync.full(key, negativeKey, items, tooltips)
        session.queuePacket(UpdateInvFull(key, negativeKey, itemIds, amounts, tooltips))
    }

    fun sendItems(key: Int, items: ItemsContainer<*>) {
        sendItems(key, key < 0, items)
    }

    fun sendItems(key: Int, negativeKey: Boolean, items: ItemsContainer<*>) {
        sendItems(key, negativeKey, items.array())
    }

    fun sendItems(key: Int, items: Array<Item?>) {
        sendItems(key, key < 0, items)
    }

    fun sendDirtyItems(key: Int, items: ItemsContainer<*>, negativeKey: Boolean = key < 0): Boolean {
        if (!items.isDirty) return false
        val array = items.array()
        if (!invSync.hasSent(key, negativeKey)) {
            items.clearDirty()
            sendItems(key, negativeKey, array)
            return true
        }
        val candidates = if (items.isAllDirty) array.indices.toList().toIntArray() else items.dirtySlots()
        items.clearDirty()
        val updates = ArrayList<InvSlotUpdate>()
        for (slot in candidates) {
            val item = array[slot]
            val tooltip = ItemTooltips.of(player, item)
            if (invSync.differs(key, negativeKey, slot, item, tooltip)) updates += invSync.update(key, negativeKey, slot, item, tooltip)
        }
        if (updates.isEmpty()) return false
        if (updates.size * PARTIAL_SLOT_BYTES >= array.size * FULL_SLOT_BYTES) sendItems(key, negativeKey, array)
        else session.queuePacket(UpdateInvPartial(key, negativeKey, updates.toTypedArray()))
        return true
    }

    fun sendLogout(lobby: Boolean) {
        session.queuePacket(if (lobby) LogoutLobby() else LogoutFull())
        // Every caller (Player.logout, forceLogout, idleLog) finishes the player immediately after
        // this returns, and the world tick's flush pass skips finished players - so without a flush
        // here the logout packet is queued and never written, and the client just sits there. The
        // legacy encoder flushed on the spot for exactly this reason.
        session.requestFlush()
        if (player.hasFinished())
            player.session.close()
    }

    fun sendSound(sound: Sound) {
        session.queuePacket(
            when (sound.type) {
                SoundType.EFFECT -> SoundSynth(sound.id, sound.repeat, sound.delay, sound.volume, sound.sampleRate)
                SoundType.VORB_EFFECT -> VorbisSound(sound.id, sound.repeat, sound.delay, sound.volume, sound.sampleRate)
                SoundType.VOICE -> VorbisSpeechSound(sound.id, sound.repeat, sound.delay, sound.volume)
                SoundType.JINGLE -> MusicEffect(0, sound.volume, sound.id)
                SoundType.MUSIC -> MusicTrack(sound.delay, sound.volume, sound.id)
            }
        )
    }

    fun resetSounds() {
        session.queuePacket(ResetSounds())
    }

    fun setBlockMinimapState(state: Int) {
        session.queuePacket(BlockMinimapState(state))
    }

    fun sendOpenURL(url: String) {
        session.queuePacket(OpenUrl(url))
    }

    fun sendSetCursor(walkHereReplace: String, cursor: Int) {
        session.queuePacket(SetCursor(cursor, walkHereReplace))
    }

    // ======================== Convenience / composite methods ========================

    fun sendGameBarStages() {
        sendVar(1054, player.clanStatus)
        sendVar(1055, player.assistStatus)
        sendVar(Varp.gimp_main, if (player.isFilterGame) 1 else 0)
        sendVar(2159, player.account.social.fcStatus.toInt())
        sendChatFilterSettings()
        sendChatFilterSettingsPrivateChat()
    }

    fun sendInventoryMessage(border: Int, slotId: Int, message: String) {
        sendGameMessage(message)
        sendRunScriptReverse(948, border, slotId, message)
    }

    fun sendGameMessage(text: String) {
        sendGameMessage(text, false)
    }

    fun sendGameMessage(text: String, filter: Boolean) {
        sendMessage(if (filter) ChatMessageType.FILTERABLE else ChatMessageType.UNFILTERABLE, text, null)
    }

    fun sendDevConsoleMessage(text: String) {
        sendMessage(ChatMessageType.DEV_CONSOLE, text, null)
    }

    fun sendTradeRequestMessage(p: Player) {
        sendMessage(ChatMessageType.TRADE_REQUEST, "wishes to trade with you.", p)
    }

    fun sendClanWarsRequestMessage(p: Player) {
        sendMessage(ChatMessageType.CLAN_CHALLENGE_REQUEST, "wishes to challenge your clan to a clan war.", p)
    }

    fun sendDuelChallengeRequestMessage(p: Player, friendly: Boolean) {
        sendMessage(ChatMessageType.DUEL_REQUEST, "wishes to duel with you (${if (friendly) "friendly" else "stake"}).", p)
    }

    fun sendDungeonneringRequestMessage(p: Player) {
        sendMessage(ChatMessageType.DUNGEONEERING_INVITE, "has invited you to a dungeon party.", p)
    }

    fun sendClanInviteMessage(p: Player) {
        sendMessage(ChatMessageType.CLAN_INVITE, "${p.displayName} is inviting you to join their clan.", p)
    }

    fun sendPouchInfusionOptionsScript(dung: Boolean, interfaceId: Int, componentId: Int, slotLength: Int, width: Int, height: Int, vararg options: String) {
        val parameters = arrayOfNulls<Any>(5 + options.size)
        var index = 0
        if (dung) {
            parameters[index++] = 1159
            parameters[index++] = 1100
        } else {
            parameters[index++] = slotLength
            parameters[index++] = 1
        }
        for (count in options.indices.reversed())
            parameters[index++] = options[count]
        parameters[index++] = height
        parameters[index++] = width
        parameters[index++] = (interfaceId shl 16) or componentId
        @Suppress("UNCHECKED_CAST")
        sendRunScriptReverse(757, *(parameters as Array<Any>))
    }

    fun sendScrollInfusionOptionsScript(dung: Boolean, interfaceId: Int, componentId: Int, slotLength: Int, width: Int, height: Int, vararg options: String) {
        val parameters = arrayOfNulls<Any>(5 + options.size)
        var index = 0
        if (dung) {
            parameters[index++] = 1159
            parameters[index++] = 1100
        } else {
            parameters[index++] = slotLength
            parameters[index++] = 1
        }
        for (count in options.indices.reversed())
            parameters[index++] = options[count]
        parameters[index++] = height
        parameters[index++] = width
        parameters[index++] = (interfaceId shl 16) or componentId
        @Suppress("UNCHECKED_CAST")
        sendRunScriptReverse(763, *(parameters as Array<Any>))
    }

    fun sendRunScriptBlank(scriptId: Int) {
        sendRunScriptReverse(scriptId)
    }

    fun sendGroundItemMessage(item: GroundItem, message: String) {
        sendGroundItemMessage(0, 0xFFFFFF, item, message)
    }

    fun sendPlayerMessage(border: Int, color: Int, message: String) {
        sendGameMessage(message)
        sendVarcString(306, message)
        sendVarc(1699, color)
        sendVarc(1700, border)
        sendVarc(1695, 1)
        sendPlayerInterface(true, player.interfaceManager.topInterface, Sub.RENDER_SPACE.getComponent(player.resizeable()), 1177)
    }

    fun sendNPCMessage(border: Int, color: Int, npc: NPC, message: String) {
        sendGameMessage(message)
        sendVarcString(306, message)
        sendVarc(1699, color)
        sendVarc(1700, border)
        sendVarc(1695, 1)
        sendNPCInterface(npc, true, player.interfaceManager.topInterface, Sub.RENDER_SPACE.getComponent(player.resizeable()), 1177)
    }

    fun sendGroundItemMessage(border: Int, color: Int, item: GroundItem, message: String) {
        sendGameMessage(message)
        sendVarcString(306, message)
        sendVarc(1699, color)
        sendVarc(1700, border)
        sendVarc(1695, 1)
        sendGroundItemInterface(item, true, player.interfaceManager.topInterface, Sub.RENDER_SPACE.getComponent(player.resizeable()), 1177)
    }

    fun sendObjectMessage(border: Int, color: Int, obj: GameObject, message: String) {
        sendGameMessage(message)
        sendVarcString(306, message)
        sendVarc(1699, color)
        sendVarc(1700, border)
        sendVarc(1695, 1)
        sendObjectInterface(obj, true, player.interfaceManager.topInterface, Sub.RENDER_SPACE.getComponent(player.resizeable()), 1177)
    }

    fun openGESearch() {
        player.interfaceManager.sendChatBoxInterface(7, 389)
        sendRunScriptReverse(570, "Grand Exchange Item Search")
    }

    fun sendInterFlashScript(interfaceId: Int, componentId: Int, width: Int, height: Int, slot: Int) {
        sendRunScriptReverse(143, slot, height, width, (interfaceId shl 16) or componentId)
    }

    fun sendInterSetItemsOptionsScript(interfaceId: Int, componentId: Int, key: Int, width: Int, height: Int, vararg options: String) {
        sendInterSetItemsOptionsScript(interfaceId, componentId, key, false, width, height, *options)
    }

    fun sendInterSetItemsOptionsScript(interfaceId: Int, componentId: Int, key: Int, negativeKey: Boolean, width: Int, height: Int, vararg options: String) {
        val parameters = arrayOfNulls<Any>(6 + options.size)
        var index = 0
        for (count in options.indices.reversed())
            parameters[index++] = options[count]
        parameters[index++] = -1
        parameters[index++] = 0
        parameters[index++] = height
        parameters[index++] = width
        parameters[index++] = key
        parameters[index++] = (interfaceId shl 16) or componentId
        @Suppress("UNCHECKED_CAST")
        sendRunScriptReverse(if (negativeKey) 695 else 150, *(parameters as Array<Any>))
    }

    fun sendInputIntegerScript(message: String) {
        sendRunScriptReverse(108, message)
    }

    fun sendInputNameScript(message: String) {
        sendRunScriptReverse(109, message)
    }

    fun sendInputLongTextScript(message: String) {
        sendRunScriptReverse(110, message)
    }

    companion object {
        private val EMPTY_XTEAS = intArrayOf(0, 0, 0, 0)
        private const val PARTIAL_SLOT_BYTES = 4
        private const val FULL_SLOT_BYTES = 3
    }
}
