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
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1
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
            ClientScriptOpCode.IF_GETSPRITESCALE -> ifGetSpriteScale(exec)
            ClientScriptOpCode.IF_GETSPRITEPITCH -> ifGetSpritePitch(exec)
            ClientScriptOpCode.IF_GETSPRITEYAW -> ifGetSpriteYaw(exec)
            ClientScriptOpCode.IF_GETSPRITEROLL -> ifGetSpriteRoll(exec)
            ClientScriptOpCode.IF_GETTRANS -> ifGetTransparency(exec)
            ClientScriptOpCode.IF_GETOFFSETX -> ifGetOffsetX(exec)
            ClientScriptOpCode.IF_GETOFFSETY -> ifGetOffsetY(exec)
            ClientScriptOpCode.IF_GETGRAPHIC -> ifGetGraphic(exec)
            ClientScriptOpCode.IF_GET2DANGLE -> ifGetAngle2D(exec)
            ClientScriptOpCode.IF_GETMODEL -> ifGetModel(exec)
            ClientScriptOpCode.IF_GETFONTGRAPHIC -> ifGetFontGraphic(exec)
            ClientScriptOpCode.IF_GETGRAPHICDIMENSIONS -> method7643(exec)
            ClientScriptOpCode.IF_GETFONTMETRICS -> method6142(exec)
            ClientScriptOpCode.IF_GETCONTAINERITEMID -> ifGetContainerItemId(exec)
            ClientScriptOpCode.IF_GETITEMSTACKSIZE -> ifGetItemStackSize(exec)
            ClientScriptOpCode.IF_ISSUBOPEN -> ifGetSubId(exec)
            ClientScriptOpCode.IF_GETSUBCOUNT -> ifGetSubCount(exec)
            ClientScriptOpCode.IF_ISOPEN -> ifIsopen(exec)
            ClientScriptOpCode.CC_ISOPEN -> ccIsopen(exec)
            ClientScriptOpCode.IF_GETOPTIONFLAGS -> ifGetOptionFlags(exec)
            ClientScriptOpCode.IF_GETOP -> ifGetOp(exec)
            ClientScriptOpCode.IF_GETOPBASE -> ifGetOpBase(exec)
            ClientScriptOpCode.IF_CALLONRESIZE -> ifCallonresize(exec)
            ClientScriptOpCode.IF_GETCHARINDEXATPOS -> ifGetCharIndexAtPos(exec)
            ClientScriptOpCode.IF_GETCHARPOSATINDEX -> ifGetCharPosAtIndex(exec)
            ClientScriptOpCode.IF_NPC_SETCUSTOMBODYMODEL -> method6670(exec)
            ClientScriptOpCode.CC_NPC_SETCUSTOMBODYMODEL -> method1967(exec)
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
        CS2SharedOps.method3251(component!!, executor)
    }

    @JvmStatic
    fun ccIsopen(executor: CS2Executor) {
        method1573(executor)
    }

    @JvmStatic
    fun ifNpcSetCustomRetex(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        method3601(component!!, executor)
    }

    @JvmStatic
    fun method7643(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(i_2)
        var i_4 = -1
        var i_5 = -1
        assert(component != null)
        val class1196: InteractiveComponentSprite? = component!!.addInteractiveSprite(Renderers.activeToolkit!!)
        if (class1196 != null) {
            i_4 = class1196.width
            i_5 = class1196.height
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
        executor.intStack[++executor.intStackPtr - 1] = i_5
    }

    @JvmStatic
    fun ccNpcSetCustomHeadModel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        method1494(component!!, executor)
    }

    @JvmStatic
    fun method6670(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        method14680(component!!, executor)
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
    fun method6142(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.fontId
    }

    @JvmStatic
    fun ifGetSpriteScale(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteScale
    }

    @JvmStatic
    fun getCompText(executor: CS2Executor) {
        val component = executor.intStack[--executor.intStackPtr]
        val icomponentdefinitions3: Component? = Component.fromHash(component)
        assert(icomponentdefinitions3 != null)
        executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3!!.text
    }

    @JvmStatic
    fun ifGetItemStackSize(executor: CS2Executor) {
        val component: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(component != null)
        if (component!!.containerItemId != -1) {
            executor.intStack[++executor.intStackPtr - 1] = component!!.itemStackSize
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ifGetOffsetX(executor: CS2Executor) {
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
        val i_2 = executor.intStack[--executor.intStackPtr]
        val icomponentdefinitions3: Component? = Component.fromHash(i_2)
        val parentInterface: Interface? = Interface.INTERFACES!![i_2  ushr  16]
        assert(icomponentdefinitions3 != null)
        val icomponentdefinitions5: Component? = Interface.getSub(parentInterface!!, icomponentdefinitions3!!)
        val stack: IntArray? = executor.intStack
        val i_7 = ++executor.intStackPtr - 1
        var i_8: Int = 0
        if (icomponentdefinitions5!! == null) {
            i_8 = -1
        } else {
            i_8 = icomponentdefinitions5!!.idHash
        }
        stack!![i_7] = i_8
    }

    @JvmStatic
    fun ifGetSpriteYaw(executor: CS2Executor) {
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
        method1494(component!!, executor)
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
    fun ifGetSubCount(executor: CS2Executor) {
        val component: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(component != null)
        if (component!!.slotChildren == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            var i_3 = component!!.slotChildren!!.size
            for (i_4 in 0 until component!!.slotChildren!!.size) {
                if (component!!.slotChildren!![i_4] == null) {
                    i_3 = i_4
                    break
                }
            }
            executor.intStack[++executor.intStackPtr - 1] = i_3
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
        method15207(component!!, executor)
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
        val i_2 = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(i_2)
        var i_4 = executor.intStack[--executor.intStackPtr]
        --i_4
        assert(component != null)
        if (component!!.options != null && i_4 < component!!.options!!.size && component!!.options!![i_4] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = component!!.options!![i_4]
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
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = if (component!!.hidden) 1  else 0
    }

    @JvmStatic
    fun ifGetTransparency(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.transparency
    }

    @JvmStatic
    fun ifGetContainerItemId(executor: CS2Executor) {
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
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        CS2SharedOps.method5001(component!!, executor)
    }

    @JvmStatic
    fun ifGetColour(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.color
    }

    @JvmStatic
    fun ccNpcSetCustomRetex(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        method3601(component!!, executor)
    }

    @JvmStatic
    fun ccNpcSetCustomRecol(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        method15207(component!!, executor)
    }

    @JvmStatic
    fun ifGetCharIndexAtPos(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        CS2SharedOps.method3965(component!!, executor)
    }

    @JvmStatic
    fun ifGetScrollWidth(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollWidth
    }

    @JvmStatic
    fun ifGetSubId(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub443: SubInterface? = Client.OPEN_INTERFACES!!.get(value.toLong())
        if (class282Sub443 != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ifGetScrollX(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollX
    }

    @JvmStatic
    fun method1967(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        method14680(component!!, executor)
    }

    @JvmStatic
    fun ifIsopen(executor: CS2Executor) {
        method1573(executor)
    }

    @JvmStatic
    fun ifGetOptionFlags(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = Client.getComponentSettings(component!!)!!.getUseOptionFlags()
    }

    @JvmStatic
    fun ifGetSpriteRoll(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteRoll
    }

    @JvmStatic
    fun method1573(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val class282Sub444: SubInterface? = Client.OPEN_INTERFACES!!.get(i_2.toLong())
        if (class282Sub444 != null && i_3 == class282Sub444.subInterfaceId) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ifGety(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.y
    }

    @JvmStatic
    fun ifGetSpritePitch(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.spritePitch
    }

    @JvmStatic
    fun ifGetx(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.x
    }

    @JvmStatic
    fun getCompHeight(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.height
    }

    @JvmStatic
    fun ifGetOffsetY(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        assert(component != null)
        executor.intStack[++executor.intStackPtr - 1] = component!!.offsetY
    }

    @JvmStatic
    fun method14680(component: Component, cs2executor1: CS2Executor) {
        val i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        val i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_MODEL) {
            throw RuntimeException("")
        } else {
            val npcdefinitions5: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, true)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (i_4 >= 0 && i_4 < npcdefinitions5!!.modelIds!!.size) {
                component!!.npcCustomisation!!.modelIds[i_4] = i_3
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("" + i_4)
            }
        }
    }

    @JvmStatic
    fun method15207(component: Component, cs2executor1: CS2Executor) {
        val i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        val i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_MODEL && component!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            val npcdefinitions5: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, component!!.mediaType == MediaType.NPC_MODEL)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (npcdefinitions5!!.recolorDst != null && i_4 >= 0 && i_4 < npcdefinitions5!!.recolorDst!!.size) {
                component!!.npcCustomisation!!.recolorDst!![i_4] = i_3.toShort()
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

    @JvmStatic
    fun method1494(component: Component, cs2executor1: CS2Executor) {
        val i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        val i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            val npcdefinitions5: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, false)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (i_4 >= 0 && i_4 < npcdefinitions5!!.headModels!!.size) {
                component!!.npcCustomisation!!.modelIds[i_4] = i_3
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

    @JvmStatic
    fun method3601(component: Component, cs2executor1: CS2Executor) {
        val i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        val i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (component!!.mediaType != MediaType.NPC_MODEL && component!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            val npcdefinitions5: NPCType? = TypeLists.NPC!!.list(component!!.primaryMediaId)
            if (component!!.npcCustomisation == null) {
                component!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, component!!.mediaType == MediaType.NPC_MODEL)
            }
            component!!.npcCustomisation!!.uid = CS2Executor.generateUniqueId()
            if (npcdefinitions5!!.retextureDst != null && i_4 >= 0 && i_4 < npcdefinitions5!!.retextureDst!!.size) {
                component!!.npcCustomisation!!.retextureDst!![i_4] = i_3.toShort()
                Component.redrawComponent(component!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

}
