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

object ComponentLifecycleOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.CC_CREATE -> ccCreate(exec)
            ClientScriptOpCode.CC_DELETE -> ccDelete(exec)
            ClientScriptOpCode.CC_DELETEALL -> clearCompChildren(exec)
            ClientScriptOpCode.CC_SETCHILD_SLOT -> ccSetChildSlot(exec)
            ClientScriptOpCode.CC_SETCHILD -> ccSetChild(exec)
            ClientScriptOpCode.IF_SENDTOFRONT -> ifSetFront(true, exec)
            ClientScriptOpCode.IF_SENDTOBACK -> ifSetFront(false, exec)
            ClientScriptOpCode.CC_SENDTOFRONT -> ccSetFront(true, exec)
            ClientScriptOpCode.CC_SENDTOBACK -> ccSetFront(false, exec)
            ClientScriptOpCode.IF_RESUME_PAUSEBUTTON -> ifResumePauseButton(exec)
            ClientScriptOpCode.CC_RESUME_PAUSEBUTTON -> ccResumePauseButton(exec)
            ClientScriptOpCode.BASEIDKIT -> setBaseIdentiKit(exec)
            ClientScriptOpCode.BASECOLOUR -> setBaseColor(exec)
            ClientScriptOpCode.SETGENDER -> setGender(exec)
            ClientScriptOpCode.SETOBJ -> setItem(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by ComponentLifecycleOps")
        }
    }

    @JvmStatic
    fun ccSetFront(bool: Boolean, executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        if (bool) {
            Interface.moveComponentToFront(parentInterface!!, component)
        } else {
            Interface.moveComponentToBack(parentInterface!!, component)
        }
    }

    @JvmStatic
    fun ifSetFront(bool: Boolean, executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        val parentInterface: Interface? = Interface.INTERFACES!![(value shr 16)]
        if (bool) {
            Interface.moveComponentToFront(parentInterface!!, component)
        } else {
            Interface.moveComponentToBack(parentInterface!!, component)
        }

    }

    @JvmStatic
    fun ccSetChildSlot(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val ifComp = executor.intStack[executor.intStackPtr]
        val slotId = executor.intStack[executor.intStackPtr + 1]
        var inter: CS2Interface? = null
        if (executor.hasIntOperand) {
            inter = executor.hookedInterface2
        } else {
            inter = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (slotId != -1 && inter!!.setChild(ifComp, slotId)) 1 else 0
    }

    @JvmStatic
    fun setItem(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val playerAppearance = PlayerUpdate.MY_PLAYER!!.playerAppearance
        if (playerAppearance != null) {
            playerAppearance!!.addAppearanceItem(i_2, i_3, TypeLists.OBJ!!)
        }
    }

    @JvmStatic
    fun setBaseIdentiKit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val playerAppearance = PlayerUpdate.MY_PLAYER!!.playerAppearance
        if (playerAppearance != null) {
            val i_4: Int = 0
            for (i_4 in 0 until CS2Interpreter.MALE_IDKIT_SLOTS.size) {
                if (CS2Interpreter.MALE_IDKIT_SLOTS[i_4] == i_2) {
                    playerAppearance!!.addAppearanceIdentity(i_4, i_3, TypeLists.IDK!!)
                    return
                }
            }
            for (i_4 in 0 until CS2Interpreter.FEMALE_IDKIT_SLOTS.size) {
                if (CS2Interpreter.FEMALE_IDKIT_SLOTS[i_4] == i_2) {
                    playerAppearance!!.addAppearanceIdentity(i_4, i_3, TypeLists.IDK!!)
                    break
                }
            }
        }
    }

    @JvmStatic
    fun ccCreate(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Component.fromHash(i_2)
        CS2SharedOps.method2738(Interface.INTERFACES!![i_2 ushr 16]!!, i_2 and 0xffff, i_3, i_4, executor.hasIntOperand, executor)
    }

    @JvmStatic
    fun ifResumePauseButton(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(value)
        method6942(component!!)
    }

    @JvmStatic
    fun clearCompChildren(executor: CS2Executor) {
        val component: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(component != null)
        component!!.slotChildren = null
        component!!.itemSlots = null
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun ccDelete(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        if (target!!.comp!!.slotId == -1) {
            throw RuntimeException("")
        } else {
            val component: Component? = target!!.getComponentFromInterface()
            @Suppress("UNCHECKED_CAST")
            (component!!.slotChildren as Array<Component?>?)!![target!!.comp!!.slotId] = null
            Component.redrawComponent(component)
        }
    }

    @JvmStatic
    fun ccResumePauseButton(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        method6942(component!!)
    }

    @JvmStatic
    fun setGender(executor: CS2Executor) {
        val flag = executor.intStack[--executor.intStackPtr] != 0
        if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null) {
            PlayerUpdate.MY_PLAYER!!.playerAppearance!!.changeGender(flag)
        }
    }

    @JvmStatic
    fun setBaseColor(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null) {
            PlayerUpdate.MY_PLAYER!!.playerAppearance!!.recolor(i_2, i_3)
        }
    }

    @JvmStatic
    fun ccSetChild(executor: CS2Executor) {
        val ifComp = executor.intStack[--executor.intStackPtr]
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (target!!.setChild(ifComp, -1)) 1 else 0
    }

    @JvmStatic
    fun method6942(component: Component) {
        if (Client.getComponentSettings(component)!!.continueOptionEnabled() && Client.activeDialogueContinue == null) {
            PacketEncoder.continueInterfacePressed(component!!.idHash, component!!.slotId)
            Client.activeDialogueContinue = Component.listIcomponent(component!!.idHash, component!!.slotId)
            assert(Client.activeDialogueContinue != null)
            Component.redrawComponent(Client.activeDialogueContinue!!)
        }
    }

}
