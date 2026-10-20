package com.jagex.game.runetek5.config.rsinterface.cs2

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.cache.FileOnDisk
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.client.login.AccountCreationResponseOpcodes
import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.CreateAccountRequest
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.MiniMenuEntry
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.client.window.DisplayModeDetails
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.clanvar.ClanVarType
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsType
import com.jagex.game.runetek5.config.enumtype.EnumType
import com.jagex.game.runetek5.config.material.texture.ColorImageCache
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.param.ParamType
import com.jagex.game.runetek5.config.quest.QuestType
import com.jagex.game.runetek5.config.quickchat.QuickChatCategoryType
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageType
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.IFEvents
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerType
import com.jagex.game.runetek5.config.sequence.InterfaceSequence
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.graphics.hint.HintRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapCategory
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay
import com.jagex.game.runetek5.graphics.model.MediaType
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.Sprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY
import com.jagex.game.runetek5.interaction.Friend
import com.jagex.game.runetek5.interaction.FriendList
import com.jagex.game.runetek5.interaction.Ignore
import com.jagex.game.runetek5.interaction.IgnoreList
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.ChatLine
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketDecoder
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.network.world.WorldDescriptor
import com.jagex.game.runetek5.rendering.MessageBox
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.*
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.VorbisSoundManager
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.ProfileCpu
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Browser
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.*
import com.jagex.game.runetek5.vardomain.VarProviders

import com.jagex.game.runetek5.config.rsinterface.cs2.ops.CS2Dispatch

object CS2Interpreter {

    @JvmField
    val MALE_IDKIT_SLOTS: IntArray = intArrayOf(0, 1, 2, 3, 4, 5, 6, 14)
    @JvmField
    val FEMALE_IDKIT_SLOTS: IntArray = intArrayOf(7, 8, 9, 10, 11, 12, 13, 15)
    @JvmField
    var lastEnteredGraphicsMode: Int = 0

    @JvmStatic
    fun executeOperation(operation: ClientScriptOpCode, exec: CS2Executor) {
        CS2Dispatch.execute(operation, exec)
    }

    @JvmStatic
    fun queryItem(queryParam: String?, tradeableOnly: Boolean, paramId: Int, paramIntQuery: Int, paramStringQuery: String?, paramIsString: Boolean) {
	    val query = queryParam!!.lowercase()
	    var results = ShortArray(16)
	    var paramIntDefault = -1
	    var paramStrDefault: String? = null
	    if (paramId != -1) {
	        val paramDef: ParamType? = TypeLists.PARAMS!!.list(paramId)
	        if (paramDef == null || paramDef!!.isString() != paramIsString) {
	            return
	        }
	        if (paramDef!!.isString()) {
	            paramStrDefault = paramDef!!.typeName
	        } else {
	            paramIntDefault = paramDef!!.defaultInt
	        }
	    }
	    var numResults = 0
	    for (i in 0 until TypeLists.OBJ!!.totalItemCount) {
	        val itemDef: ItemType? = TypeLists.OBJ!!.list(i)
	        if ((!tradeableOnly || itemDef!!.tradeable) && itemDef!!.certTemplateId == -1 && itemDef!!.lendTemplateId == -1 && itemDef!!.bindTemplateId == -1 && itemDef!!.unknownInt6 == 0 && (itemDef!!.name ?: "").lowercase().indexOf(query) != -1) {
	            if (paramId != -1) {
	                if (paramIsString) {
	                    if (!paramStringQuery.equals(itemDef!!.getCs2String(paramId, paramStrDefault ?: ""))) {
	                        continue
	                    }
	                } else if (paramIntQuery != itemDef!!.getCs2Integer(paramId, paramIntDefault)) {
	                    continue
	                }
	            }
	            if (numResults >= 250) {
	                CS2QueryResults.count = -1
	                CS2QueryResults.ids = null
	                return
	            }
	            if (numResults >= results.size) {
	                val newResults = ShortArray(results.size * 2)
	                System.arraycopy(results, 0, newResults, 0, numResults)
	                results = newResults
	            }
	            results[numResults++] = i.toShort()
	        }
	    }
	    CS2QueryResults.ids = results
	    CS2QueryResults.cursor = 0
	    CS2QueryResults.count = numResults
	    val resultNames: Array<String?> = arrayOfNulls<String>(CS2QueryResults.count)
	    for (value in 0 until CS2QueryResults.count) {
	        resultNames[value] = TypeLists.OBJ!!.list(results[value].toInt()).name
	    }
	    @Suppress("UNCHECKED_CAST")
        Utils.sortAlphabetically(resultNames as Array<String>, CS2QueryResults.ids)
    }

}
