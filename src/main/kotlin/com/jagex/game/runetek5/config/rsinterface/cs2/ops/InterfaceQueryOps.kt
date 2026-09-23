package com.jagex.game.runetek5.config.rsinterface.cs2.ops

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
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interpreter
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2CallFrame
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode

object InterfaceQueryOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.IF_GETX -> ifGetx(exec)
            ClientScriptOpCode.IF_GETY -> ifGety(exec)
            ClientScriptOpCode.IF_GETWIDTH -> getCompWidth(exec)
            ClientScriptOpCode.IF_GETHEIGHT -> getCompHeight(exec)
            ClientScriptOpCode.IF_GETHIDE -> getCompHidden(exec)
            ClientScriptOpCode.IF_GETLAYER -> ifGetLayer(exec)
            ClientScriptOpCode.IF_GETPARENTLAYER -> ifGetParentLayer(exec)
            ClientScriptOpCode.IF_GETCOLOUR -> ifGetColour(exec)
            ClientScriptOpCode.IF_GETSCROLLX -> ifGetScrollX(exec)
            ClientScriptOpCode.IF_GETSCROLLY -> ifGetScrollY(exec)
            ClientScriptOpCode.IF_GETTEXT -> getCompText(exec)
            ClientScriptOpCode.IF_GETSCROLLWIDTH -> ifGetScrollWidth(exec)
            ClientScriptOpCode.IF_GETSCROLLHEIGHT -> ifGetScrollHeight(exec)
            ClientScriptOpCode.IF_GETMODELZOOM -> ifGetModelZoom(exec)
            ClientScriptOpCode.IF_GETMODELANGLE_X -> ifGetModelAngleX(exec)
            ClientScriptOpCode.IF_GETMODELANGLE_Z -> ifGetModelAngleZ(exec)
            ClientScriptOpCode.IF_GETMODELANGLE_Y -> ifGetModelAngleY(exec)
            ClientScriptOpCode.IF_GETTRANS -> ifGetTransparency(exec)
            ClientScriptOpCode.IF_GETMODELXOF -> ifGetModelXof(exec)
            ClientScriptOpCode.IF_GETMODELYOF -> ifGetModelYof(exec)
            ClientScriptOpCode.IF_GETGRAPHIC -> ifGetGraphic(exec)
            ClientScriptOpCode.IF_GET2DANGLE -> ifGetAngle2D(exec)
            ClientScriptOpCode.IF_GETMODEL -> ifGetModel(exec)
            ClientScriptOpCode.IF_GETFONTGRAPHIC -> ifGetFontGraphic(exec)
            ClientScriptOpCode.IF_GETGRAPHICDIMENSIONS -> ifGetGraphicDimensions(exec)
            ClientScriptOpCode.IF_GETFONTMETRICS -> ifGetFontMetrics(exec)
            ClientScriptOpCode.IF_GETINVOBJECT -> ifGetInvObject(exec)
            ClientScriptOpCode.IF_GETINVCOUNT -> ifGetInvCount(exec)
            ClientScriptOpCode.IF_HASSUB -> ifGetSubId(exec)
            ClientScriptOpCode.IF_GETNEXTSUBID -> ifGetNextSubId(exec)
            ClientScriptOpCode.IF_HASSUBMODAL -> ifHasSubModal(exec)
            ClientScriptOpCode.IF_HASSUBOVERLAY -> ifHasSubOverlay(exec)
            ClientScriptOpCode.IF_GETTARGETMASK -> ifGetTargetMask(exec)
            ClientScriptOpCode.IF_GETOP -> ifGetOp(exec)
            ClientScriptOpCode.IF_GETOPBASE -> ifGetOpBase(exec)
            ClientScriptOpCode.IF_CALLONRESIZE -> ifCallonresize(exec)
            ClientScriptOpCode.IF_GETCHARINDEXATPOS -> ifGetCharIndexAtPos(exec)
            ClientScriptOpCode.IF_GETCHARPOSATINDEX -> ifGetCharPosAtIndex(exec)
            ClientScriptOpCode.IF_NPC_SETCUSTOMBODYMODEL -> ifNpcSetCustomBodyModel(exec)
            ClientScriptOpCode.CC_NPC_SETCUSTOMBODYMODEL -> ccNpcSetCustomBodyModel(exec)
            ClientScriptOpCode.IF_NPC_SETCUSTOMHEADMODEL -> ifNpcSetCustomHeadModel(exec)
            ClientScriptOpCode.CC_NPC_SETCUSTOMHEADMODEL -> ccNpcSetCustomHeadModel(exec)
            ClientScriptOpCode.IF_NPC_SETCUSTOMRECOL -> ifNpcSetCustomRecol(exec)
            ClientScriptOpCode.CC_NPC_SETCUSTOMRECOL -> ccNpcSetCustomRecol(exec)
            ClientScriptOpCode.IF_NPC_SETCUSTOMRETEX -> ifNpcSetCustomRetex(exec)
            ClientScriptOpCode.CC_NPC_SETCUSTOMRETEX -> ccNpcSetCustomRetex(exec)
            ClientScriptOpCode.MES -> sendMessage(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by InterfaceQueryOps")
        }
    }

    @JvmStatic
    fun ifCallonresize(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        CS2SharedOps.callonresize(component!!, executor)
    }

    @JvmStatic
    fun ifHasSubOverlay(executor: CS2Executor) {
        isInterfaceOpen(executor)
    }

    @JvmStatic
    fun ifNpcSetCustomRetex(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        npcSetCustomRetex(component!!, executor)
    }

    @JvmStatic
    fun ifGetGraphicDimensions(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        var width = -1
        var height = -1
        assert(component != null)
        val sprite: InteractiveComponentSprite? = component!!.addInteractiveSprite(Renderers.activeToolkit!!)
        if (sprite != null) {
            width = sprite.width
            height = sprite.height
        }
        executor.intStack[++executor.intStackPtr - 1] = width
        executor.intStack[++executor.intStackPtr - 1] = height
    }

    @JvmStatic
    fun ccNpcSetCustomHeadModel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        npcSetCustomHeadModel(component!!, executor)
    }

    @JvmStatic
    fun ifNpcSetCustomBodyModel(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        npcSetCustomBodyModel(component!!, executor)
    }

    @JvmStatic
    fun ifGetAngle2D(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.angle2d
    }

    @JvmStatic
    fun getCompWidth(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.width
    }

    @JvmStatic
    fun ifGetFontMetrics(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.fontId
    }

    @JvmStatic
    fun ifGetModelZoom(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteScale
    }

    @JvmStatic
    fun getCompText(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.stringStack[++executor.stringStackPtr - 1] = component!!.text
    }

    @JvmStatic
    fun ifGetInvCount(executor: CS2Executor) {
        val component: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(component != null)
        if (component!!.containerItemId != -1) {
            executor.intStack[++executor.intStackPtr - 1] = component!!.itemStackSize
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ifGetModelXof(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.offsetX
    }

    @JvmStatic
    fun ifGetOpBase(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        if (component!!.opBase == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = component!!.opBase
        }
    }

    @JvmStatic
    fun ifGetParentLayer(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        val parentInterface: Interface? = Interface.INTERFACES!![componentHash  ushr  16]
        assert(component != null)
        val parentLayer: Component? = Interface.getSub(parentInterface!!, component!!)
        val stack: IntArray? = executor.intStack
        val stackIdx = ++executor.intStackPtr - 1
        var parentIdHash: Int = 0
        if (parentLayer!! == null) {
            parentIdHash = -1
        } else {
            parentIdHash = parentLayer!!.idHash
        }
        stack!![stackIdx] = parentIdHash
    }

    @JvmStatic
    fun ifGetModelAngleZ(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteYaw
    }

    @JvmStatic
    fun ifGetScrollHeight(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollHeight
    }

    @JvmStatic
    fun ifNpcSetCustomHeadModel(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        npcSetCustomHeadModel(component!!, executor)
    }

    @JvmStatic
    fun ifGetLayer(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.parentId
    }

    @JvmStatic
    fun ifGetGraphic(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteId
    }

    @JvmStatic
    fun ifGetNextSubId(executor: CS2Executor) {
        val component: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(component != null)
        if (component!!.slotChildren == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            var count = component!!.slotChildren!!.size
            for (i in 0 until component!!.slotChildren!!.size) {
                if (component!!.slotChildren!![i] == null) {
                    count = i
                    break
                }
            }
            executor.intStack[++executor.intStackPtr - 1] = count
        }
    }

    @JvmStatic
    fun ifGetScrollY(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollY
    }

    @JvmStatic
    fun ifNpcSetCustomRecol(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        npcSetCustomRecol(component!!, executor)
    }

    @JvmStatic
    fun ifGetFontGraphic(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.fontId
    }

    @JvmStatic
    fun ifGetOp(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        var opIndex = executor.intStack[--executor.intStackPtr]
        --opIndex
        assert(component != null)
        if (component!!.options != null && opIndex < component!!.options!!.size && component!!.options!![opIndex] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = component!!.options!![opIndex]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ifGetModel(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = if (component!!.mediaType == MediaType.RAW_MODEL) component!!.primaryMediaId else -1
    }

    @JvmStatic
    fun getCompHidden(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = if (component!!.hidden) 1  else 0
    }

    @JvmStatic
    fun ifGetTransparency(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.transparency
    }

    @JvmStatic
    fun ifGetInvObject(executor: CS2Executor) {
        val component: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.containerItemId
    }

    @JvmStatic
    fun sendMessage(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        ChatBox.writeUnfilterableMessage(text!!)
    }

    @JvmStatic
    fun ifGetCharPosAtIndex(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        CS2SharedOps.getCharPosAtIndex(component!!, executor)
    }

    @JvmStatic
    fun ifGetColour(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.color
    }

    @JvmStatic
    fun ccNpcSetCustomRetex(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        npcSetCustomRetex(component!!, executor)
    }

    @JvmStatic
    fun ccNpcSetCustomRecol(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        npcSetCustomRecol(component!!, executor)
    }

    @JvmStatic
    fun ifGetCharIndexAtPos(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        CS2SharedOps.getCharIndexAtPos(component!!, executor)
    }

    @JvmStatic
    fun ifGetScrollWidth(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollWidth
    }

    @JvmStatic
    fun ifGetSubId(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val openSub: SubInterface? = Client.OPEN_INTERFACES!!.get(componentHash.toLong())
        if (openSub != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ifGetScrollX(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollX
    }

    @JvmStatic
    fun ccNpcSetCustomBodyModel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        npcSetCustomBodyModel(component!!, executor)
    }

    @JvmStatic
    fun ifHasSubModal(executor: CS2Executor) {
        isInterfaceOpen(executor)
    }

    @JvmStatic
    fun ifGetTargetMask(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = Client.getComponentSettings(component!!)!!.getUseOptionFlags()
    }

    @JvmStatic
    fun ifGetModelAngleY(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteRoll
    }

    @JvmStatic
    fun isInterfaceOpen(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val targetComponentHash = executor.intStack[executor.intStackPtr]
        val interfaceId = executor.intStack[executor.intStackPtr + 1]
        val openSub: SubInterface? = Client.OPEN_INTERFACES!!.get(targetComponentHash.toLong())
        if (openSub != null && interfaceId == openSub.subInterfaceId) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ifGety(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.y
    }

    @JvmStatic
    fun ifGetModelAngleX(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spritePitch
    }

    @JvmStatic
    fun ifGetx(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.x
    }

    @JvmStatic
    fun getCompHeight(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.height
    }

    @JvmStatic
    fun ifGetModelYof(executor: CS2Executor) {
        val componentHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(componentHash)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.offsetY
    }

    @JvmStatic
    fun npcSetCustomBodyModel(component: Component, executor: CS2Executor) {
        val modelId = executor.intStack[--executor.intStackPtr]
        val slot = executor.intStack[--executor.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_MODEL) {
            throw RuntimeException("")
        } else {
            val npcType: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcType!!, true)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (slot >= 0 && slot < npcType!!.modelIds!!.size) {
                component!!.npcCustomisation!!.modelIds!![slot] = modelId
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("" + slot)
            }
        }
    }

    @JvmStatic
    fun npcSetCustomRecol(component: Component, executor: CS2Executor) {
        val color = executor.intStack[--executor.intStackPtr]
        val slot = executor.intStack[--executor.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_MODEL && component!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            val npcType: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcType!!, component!!.mediaType == MediaType.NPC_MODEL)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (npcType!!.recolorDst != null && slot >= 0 && slot < npcType!!.recolorDst!!.size) {
                component!!.npcCustomisation!!.recolorDst!![slot] = color.toShort()
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

    @JvmStatic
    fun npcSetCustomHeadModel(component: Component, executor: CS2Executor) {
        val modelId = executor.intStack[--executor.intStackPtr]
        val slot = executor.intStack[--executor.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            val npcType: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcType!!, false)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (slot >= 0 && slot < npcType!!.headModels!!.size) {
                component!!.npcCustomisation!!.modelIds!![slot] = modelId
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

    @JvmStatic
    fun npcSetCustomRetex(component: Component, executor: CS2Executor) {
        val textureId = executor.intStack[--executor.intStackPtr]
        val slot = executor.intStack[--executor.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_MODEL && component!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            val npcType: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcType!!, component!!.mediaType == MediaType.NPC_MODEL)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (npcType!!.retextureDst != null && slot >= 0 && slot < npcType!!.retextureDst!!.size) {
                component!!.npcCustomisation!!.retextureDst!![slot] = textureId.toShort()
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

}
