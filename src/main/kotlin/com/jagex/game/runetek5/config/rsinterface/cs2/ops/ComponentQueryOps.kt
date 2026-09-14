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

object ComponentQueryOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.CC_GETX -> ccGetx(exec)
            ClientScriptOpCode.CC_GETY -> ccGety(exec)
            ClientScriptOpCode.CC_GETWIDTH -> ccGetWidth(exec)
            ClientScriptOpCode.CC_GETHEIGHT -> ccGetHeight(exec)
            ClientScriptOpCode.CC_GETHIDE -> ccGetHide(exec)
            ClientScriptOpCode.CC_GETLAYER -> ccGetLayer(exec)
            ClientScriptOpCode.CC_GETPARENTLAYER -> ccGetSubId(exec)
            ClientScriptOpCode.CC_GETCOLOUR -> ccGetColour(exec)
            ClientScriptOpCode.CC_GETSCROLLX -> ccGetScrollX(exec)
            ClientScriptOpCode.CC_GETSCROLLY -> ccGetScrollY(exec)
            ClientScriptOpCode.CC_GETTEXT -> ccGetText(exec)
            ClientScriptOpCode.CC_GETSCROLLWIDTH -> ccGetScrollWidth(exec)
            ClientScriptOpCode.CC_GETSCROLLHEIGHT -> ccGetScrollHeight(exec)
            ClientScriptOpCode.CC_GETMODELZOOM -> ccGetModelZoom(exec)
            ClientScriptOpCode.CC_GETMODELANGLE_X -> ccGetModelAngleX(exec)
            ClientScriptOpCode.CC_GETMODELANGLE_Z -> ccGetModelAngleZ(exec)
            ClientScriptOpCode.CC_GETMODELANGLE_Y -> ccGetModelAngleY(exec)
            ClientScriptOpCode.CC_GETTRANS -> ccGetTrans(exec)
            ClientScriptOpCode.CC_GETMODELXOF -> ccGetModelXof(exec)
            ClientScriptOpCode.CC_GETMODELYOF -> ccGetModelYof(exec)
            ClientScriptOpCode.CC_GETGRAPHIC -> ccGetSpriteId(exec)
            ClientScriptOpCode.CC_PARAM -> ccParam(exec)
            ClientScriptOpCode.CC_GET2DANGLE -> ccGetAngle2D(exec)
            ClientScriptOpCode.CC_GETMODEL -> ccGetModel(exec)
            ClientScriptOpCode.CC_GETFONTGRAPHIC -> ccGetFontGraphic(exec)
            ClientScriptOpCode.CC_GETGRAPHICDIMENSIONS -> ccGetGraphicDimensions(exec)
            ClientScriptOpCode.CC_GETFONTMETRICS -> ccGetFontMetrics(exec)
            ClientScriptOpCode.CC_GETINVOBJECT -> ccGetInvObject(exec)
            ClientScriptOpCode.CC_GETINVCOUNT -> ccGetInvCount(exec)
            ClientScriptOpCode.CC_GETID -> ccGetId(exec)
            ClientScriptOpCode.CC_GETTARGETMASK -> ccGetTargetMask(exec)
            ClientScriptOpCode.CC_GETOP -> ccGetOp(exec)
            ClientScriptOpCode.CC_GETOPBASE -> ccGetOpBase(exec)
            ClientScriptOpCode.CC_CALLONRESIZE -> ccCallonresize(exec)
            ClientScriptOpCode.CC_GETCHARINDEXATPOS -> ccGetCharIndexAtPos(exec)
            ClientScriptOpCode.CC_GETCHARPOSATINDEX -> ccGetCharPosAtIndex(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by ComponentQueryOps")
        }
    }

    @JvmStatic
    fun ccGetModelAngleX(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.spritePitch
    }

    @JvmStatic
    fun ccGetOpBase(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        if (component!!.opBase == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = component!!.opBase
        }
    }

    @JvmStatic
    fun ccGety(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.y
    }

    @JvmStatic
    fun ccGetScrollX(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollX
    }

    @JvmStatic
    fun ccGetModel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = if (component!!.mediaType == MediaType.RAW_MODEL) component!!.primaryMediaId else -1
    }

    @JvmStatic
    fun ccGetInvObject(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.containerItemId
    }

    @JvmStatic
    fun ccGetOp(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        var value = executor.intStack[--executor.intStackPtr]
        --value
        val options = component!!.options
        if (options != null && value < options.size && options[value] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = options[value]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ccGetScrollY(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollY
    }

    @JvmStatic
    fun ccGetScrollHeight(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollHeight
    }

    @JvmStatic
    fun ccGetCharPosAtIndex(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        CS2SharedOps.getCharPosAtIndex(component!!, executor)
    }

    @JvmStatic
    fun ccGetTargetMask(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = Client.getComponentSettings(component!!)!!.getUseOptionFlags()
    }

    @JvmStatic
    fun ccGetSpriteId(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteId
    }

    @JvmStatic
    fun ccGetCharIndexAtPos(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        CS2SharedOps.getCharIndexAtPos(component!!, executor)
    }

    @JvmStatic
    fun ccGetx(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.x
    }

    @JvmStatic
    fun ccGetWidth(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.width
    }

    @JvmStatic
    fun ccGetSubId(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val icomponentdefinitions3: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        val icomponentdefinitions5: Component? = Interface.getSub(parentInterface!!, icomponentdefinitions3!!)
        executor.intStack[++executor.intStackPtr - 1] = if (icomponentdefinitions5!! == null) -1 else icomponentdefinitions5!!.idHash
    }

    @JvmStatic
    fun ccGetModelXof(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.offsetX
    }

    @JvmStatic
    fun ccGetGraphicDimensions(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        var spriteWidth = -1
        var spriteHeight = -1
        val sprite: InteractiveComponentSprite? = component!!.addInteractiveSprite(Renderers.activeToolkit!!)
        if (sprite != null) {
            spriteWidth = sprite.width
            spriteHeight = sprite.height
        }
        executor.intStack[++executor.intStackPtr - 1] = spriteWidth
        executor.intStack[++executor.intStackPtr - 1] = spriteHeight
    }

    @JvmStatic
    fun ccGetHide(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = if (component!!.hidden) 1  else 0
    }

    @JvmStatic
    fun ccGetScrollWidth(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.scrollWidth
    }

    @JvmStatic
    fun ccGetModelYof(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.offsetY
    }

    @JvmStatic
    fun ccGetLayer(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.parentId
    }

    @JvmStatic
    fun ccGetModelAngleY(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteRoll
    }

    @JvmStatic
    fun ccGetModelZoom(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteScale
    }

    @JvmStatic
    fun ccGetColour(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.color
    }

    @JvmStatic
    fun ccGetFontGraphic(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.fontId
    }

    @JvmStatic
    fun ccGetModelAngleZ(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.spriteYaw
    }

    @JvmStatic
    fun ccGetTrans(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.transparency
    }

    @JvmStatic
    fun ccCallonresize(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        CS2SharedOps.callonresize(component!!, executor)
    }

    @JvmStatic
    fun ccGetText(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        executor.stringStack[++executor.stringStackPtr - 1] = component!!.text
    }

    @JvmStatic
    fun ccGetAngle2D(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.angle2d
    }

    @JvmStatic
    fun ccGetId(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.slotId
    }

    @JvmStatic
    fun ccGetInvCount(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        if (component!!.containerItemId != -1) {
            executor.intStack[++executor.intStackPtr - 1] = component!!.itemStackSize
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ccGetFontMetrics(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.fontId
    }

    @JvmStatic
    fun ccGetHeight(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStack[++executor.intStackPtr - 1] = component!!.height
    }

    @JvmStatic
    fun ccParam(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val value = executor.intStack[--executor.intStackPtr]
        val attributedefault5: ParamType? = TypeLists.PARAMS!!.list(value)
        if (attributedefault5!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = component!!.getStringParam(value, attributedefault5!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = component!!.getIntParam(value, attributedefault5!!.defaultInt)
        }
    }

}
