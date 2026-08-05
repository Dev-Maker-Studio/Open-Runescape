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

object CS2SharedOps {

    @JvmStatic
    fun substring(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        val i_3 = executor.intStack[executor.intStackPtr]
        val i_4 = executor.intStack[executor.intStackPtr + 1]
        executor.stringStack[++executor.stringStackPtr - 1] = text!!.substring(i_3, i_4)
    }

    @JvmStatic
    fun toString(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = Integer.toString(value)
    }

    @JvmStatic
    fun method15511(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.spriteScale = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method4883(component: Component, i_1: Int, i_2: Int) {
        if (component!!.keyCharCodes == null) {
            if (i_2 <= 0) {
                return
            }
            component!!.keyTriggeringAction = Array(11) { ByteArray(0) }
            component!!.keyTriggerParams = Array(11) { ByteArray(0) }
            component!!.cycleSteps = IntArray(11)
            component!!.keyCharCodes = IntArray(11)
        }
        component!!.keyCharCodes!![i_1] = i_2
        if (i_2 > 0) {
            component!!.hasInteraction = true
        } else {
            component!!.hasInteraction = false
            val keyTriggeringActionArr = component!!.keyTriggeringAction!!
            for (i_5 in 0 until keyTriggeringActionArr.size) {
                if (keyTriggeringActionArr[i_5] != null || component!!.keyCharCodes!![i_5] > 0) {
                    component!!.hasInteraction = true
                    break
                }
            }
        }
    }

    @JvmStatic
    fun method1852(component: Component, intr: Interface, cs2: CS2Executor) {
        cs2.intStackPtr -= 2
        component!!.scrollX = cs2.intStack[cs2.intStackPtr]
        if (component!!.scrollX > component!!.scrollWidth - component!!.width) {
            component!!.scrollX = component!!.scrollWidth - component!!.width
        }
        if (component!!.scrollX < 0) {
            component!!.scrollX = 0
        }
        component!!.scrollY = cs2.intStack[cs2.intStackPtr + 1]
        if (component!!.scrollY > component!!.scrollHeight - component!!.height) {
            component!!.scrollY = component!!.scrollHeight - component!!.height
        }
        if (component!!.scrollY < 0) {
            component!!.scrollY = 0
        }
        Component.redrawComponent(component)
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIfscrollposUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method6289(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onMouseOver = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method7927(component: Component, cs2executor1: CS2Executor) {
        cs2executor1.intStackPtr -= 2
        val i_3 = cs2executor1.intStack[cs2executor1.intStackPtr] - 1
        val i_4 = cs2executor1.intStack[cs2executor1.intStackPtr + 1]
        if (i_3 >= 0 && i_3 <= 9) {
            method4883(component!!, i_3, i_4)
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun method3078(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onHold = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method3672(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onMouseMove = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnMouseLeave(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        val stack: IntArray? = popIntArrayParam(text, cs2executor2)
        if (stack != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.mouseLeaveScript = popParamsDynamic(text, cs2executor2)
        component!!.mouseLeaveArrayParams = stack
        component!!.hasScript = true
    }

    @JvmStatic
    fun method7889(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onSubChange = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method774(component: Component, cs2executor2: CS2Executor) {
        component!!.flipHorizontal = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setComponentModelOrigin(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        component!!.originX = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.originY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method2874(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onClickRepeat = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method7267(component: Component, cs2executor2: CS2Executor) {
        component!!.usesOrthogonal = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method5009(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onChatTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method7142(component: Component, cs2executor2: CS2Executor) {
        component!!.shadow = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method6288(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        val stack: IntArray? = popIntArrayParam(text, cs2executor2)
        if (stack != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onScrollWheel = popParamsDynamic(text, cs2executor2)
        component!!.mouseWheelArray = stack
        component!!.hasScript = true
    }

    @JvmStatic
    fun method2915(component: Component, cs2executor2: CS2Executor) {
        component!!.spriteShadow = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method8754(component: Component, cs2executor2: CS2Executor) {
        component!!.filled = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun ifSetColor(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.color = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfcolorUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method6689(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        val stack: IntArray? = popIntArrayParam(text, cs2executor2)
        if (stack != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onVarpTransmit = popParamsDynamic(text, cs2executor2)
        component!!.varps = stack
        component!!.hasScript = true
    }

    @JvmStatic
    fun method6690(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onStockTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method4159(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        component!!.modelTintRed = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.modelTintGreen = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        component!!.modelTintBlue = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        component!!.modelTintScalar = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method4160(component: Component, cs2executor2: CS2Executor) {
        component!!.borderThickness = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method11221(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onTargetLeave = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method11223(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onMouseLeaveScript = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method501(component: Component, cs2executor2: CS2Executor) {
        component!!.tiling = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method502(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        val stack: IntArray? = popIntArrayParam(text, cs2executor2)
        if (stack != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onKey = popParamsDynamic(text, cs2executor2)
        component!!.keyPressArray = stack
        component!!.hasScript = true
    }

    @JvmStatic
    fun method505(component: Component, cs2executor2: CS2Executor) {
        val value = cs2executor2.intStack[--cs2executor2.intStackPtr]
        val text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        val attributedefault6: ParamType? = TypeLists.PARAMS!!.list(value)
        if (!attributedefault6!!.typeName.equals(text!!)) {
            component!!.setStringParam(value, text)
        } else {
            component!!.removeParam(value)
        }
    }

    @JvmStatic
    fun method506(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        component!!.scrollWidth = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.scrollHeight = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(component!!)
        if (component!!.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, component, false)
        }
    }

    @JvmStatic
    fun method2738(parentInterface: Interface, i_1: Int, i_2: Int, i_3: Int, flag: Boolean, cs2executor5: CS2Executor) {
        if (i_2 == 0) {
            throw RuntimeException()
        } else {
            val icomponentdefinitions7: Component? = parentInterface.components[i_1]
            if (icomponentdefinitions7!!.slotChildren == null) {
                @Suppress("UNCHECKED_CAST")
                icomponentdefinitions7!!.slotChildren = arrayOfNulls<Component>(i_3 + 1) as Array<Component>?
                @Suppress("UNCHECKED_CAST")
                icomponentdefinitions7!!.itemSlots = icomponentdefinitions7!!.slotChildren as Array<Component?>?
            }
            if (icomponentdefinitions7!!.slotChildren!!.size <= i_3) {
                @Suppress("UNCHECKED_CAST")
                val arr_8: Array<Component>? = arrayOfNulls<Component>(i_3 + 1) as Array<Component>?
                if (icomponentdefinitions7!!.slotChildren == icomponentdefinitions7!!.itemSlots) {
                    System.arraycopy(icomponentdefinitions7!!.slotChildren, 0, arr_8, 0, icomponentdefinitions7!!.slotChildren!!.size)
                    icomponentdefinitions7!!.slotChildren = arr_8
                    @Suppress("UNCHECKED_CAST")
                    icomponentdefinitions7!!.itemSlots = arr_8 as Array<Component?>?
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val arr_11: Array<Component?>? = arrayOfNulls<Component>(i_3 + 1)
                    for (i_10 in 0 until icomponentdefinitions7!!.slotChildren!!.size) {
                        arr_8!![i_10] = icomponentdefinitions7!!.slotChildren!![i_10]
                        arr_11!![i_10] = icomponentdefinitions7!!.itemSlots!![i_10]
                    }
                    icomponentdefinitions7!!.slotChildren = arr_8
                    icomponentdefinitions7!!.itemSlots = arr_11
                }
            }
            if (i_3 > 0 && icomponentdefinitions7!!.slotChildren!![i_3 - 1] == null) {
                throw RuntimeException("" + (i_3 - 1))
            } else {
                val icomponentdefinitions12: Component? = Component()
                icomponentdefinitions12!!.type = ComponentType.forId(i_2)
                icomponentdefinitions12!!.idHash = icomponentdefinitions7!!.idHash
                icomponentdefinitions12!!.parentId = icomponentdefinitions7!!.idHash
                icomponentdefinitions12!!.slotId = i_3
                icomponentdefinitions7!!.slotChildren!![i_3] = icomponentdefinitions12
                if (icomponentdefinitions7!!.slotChildren != icomponentdefinitions7!!.itemSlots) {
                    icomponentdefinitions7!!.itemSlots!![i_3] = icomponentdefinitions12
                }
                var target: CS2Interface? = null
                if (flag) {
                    target = cs2executor5.hookedInterface2
                } else {
                    target = cs2executor5.hookedInterface1
                }
                target!!.inter = parentInterface
                target!!.comp = icomponentdefinitions12
                Component.redrawComponent(icomponentdefinitions7!!)
            }
        }
    }

    @JvmStatic
    fun method2741(component: Component, cs2executor2: CS2Executor) {
        var flag: Boolean = false
        flag = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        component!!.alpha = flag
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method8387(component: Component, cs2executor2: CS2Executor) {
        component!!.dragDeadzone = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method8390(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        val value = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (value != component!!.spriteId) {
            component!!.spriteId = value
            Component.redrawComponent(component!!)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfgraphicUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method15256(component: Component, cs2executor2: CS2Executor) {
        component!!.dragDeadTime = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method4651(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onKeyType = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method6313(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        component!!.skyboxId = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.lightPositionX = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        component!!.lightPositionY = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        component!!.lightPositionZ = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method786(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        val flag = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        if (flag != component!!.hidden) {
            component!!.hidden = flag
            Component.redrawComponent(component!!)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfhide(component!!.idHash)
        }
    }

    @JvmStatic
    fun method789(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        if (component!!.type == ComponentType.SPRITE) {
            method5459(component!!, parentInterface!!, cs2executor2)
        }
    }

    @JvmStatic
    fun method5017(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onClanSettingsTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method2955(component: Component, cs2executor2: CS2Executor) {
        component!!.moveOverCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method4772(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onMiscTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTriggerKey(component: Component, i_1: Int, bytes_2: ByteArray?, bytes_3: ByteArray?) {
        if (component!!.keyTriggeringAction == null) {
            if (bytes_2 == null) {
                return
            }
            component!!.keyTriggeringAction = Array(11) { ByteArray(0) }
            component!!.keyTriggerParams = Array(11) { ByteArray(0) }
            component!!.cycleSteps = IntArray(11)
            component!!.keyCharCodes = IntArray(11)
        }
        component!!.keyTriggeringAction!![i_1] = bytes_2!!
        if (bytes_2 != null) {
            component!!.hasInteraction = true
        } else {
            component!!.hasInteraction = false
            for (i_6 in 0 until component!!.keyTriggeringAction!!.size) {
                if (component!!.keyTriggeringAction!![i_6] != null || component!!.keyCharCodes!![i_6] > 0) {
                    component!!.hasInteraction = true
                    break
                }
            }
        }
        component!!.keyTriggerParams!![i_1] = bytes_3!!
    }

    @JvmStatic
    fun ifSetNPCHead(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.mediaType = MediaType.NPC_HEAD
        component!!.npcCustomisation = null
        component!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method2960(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.textAntiMacro = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedClearIftextantimacro(component!!.idHash)
        }
    }

    @JvmStatic
    fun method2593(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onDragComplete = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method13468(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.clickMask = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfclickmaskUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method4136(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onTimer = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setItemIFComp(ifComp: Component, parentInterface: Interface, wearCol: Boolean, renderStack: Component.StackMode, cs2executor4: CS2Executor) {
        cs2executor4.intStackPtr -= 2
        val itemId = cs2executor4.intStack[cs2executor4.intStackPtr]
        val value = cs2executor4.intStack[cs2executor4.intStackPtr + 1]
        if (ifComp.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfitemUpdate(ifComp.idHash)
            PulseEventQueue.delayedIfangleUpdate(ifComp.idHash)
            PulseEventQueue.delayedIfoffsetUpdate(ifComp.idHash)
        }
        if (itemId == -1) {
            ifComp.mediaType = MediaType.RAW_MODEL
            ifComp.primaryMediaId = -1
            ifComp.containerItemId = -1
        } else {
            ifComp.containerItemId = itemId
            ifComp.itemStackSize = value
            ifComp.wearCol = wearCol
            val itemdefinitions8: ItemType? = TypeLists.OBJ!!.list(itemId)
            ifComp.spritePitch = itemdefinitions8!!.modelRotationX
            ifComp.spriteRoll = itemdefinitions8!!.modelRotationY
            ifComp.spriteYaw = itemdefinitions8!!.modelRotationZ
            ifComp.offsetX = itemdefinitions8!!.modelOffsetX
            ifComp.offsetY = itemdefinitions8!!.modelOffsetY
            ifComp.spriteScale = itemdefinitions8!!.modelZoom
            ifComp.renderStack = renderStack
            if (ifComp.aspectWidth > 0) {
                ifComp.spriteScale = ifComp.spriteScale * 32 / ifComp.aspectWidth
            } else if (ifComp.baseWidth > 0) {
                ifComp.spriteScale = ifComp.spriteScale * 32 / ifComp.baseWidth
            }
        }
    }

    @JvmStatic
    fun setComponentText(component: Component, intr: Interface, cs2: CS2Executor) {
        val text: String? = cs2.stringStack[--cs2.stringStackPtr] as String?
        if (!text.equals(component!!.text)) {
            component!!.text = text ?: ""
            Component.redrawComponent(component)
        }
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIftextUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOpBase(component: Component, cs2executor2: CS2Executor) {
        component!!.opBase = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            component!!.opBase += "(opBase)"
    }

    @JvmStatic
    fun method14482(component: Component, cs2executor2: CS2Executor) {
        val i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        val i_5 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (i_5 >= 1 && i_5 <= 10) {
            component!!.addCursor(i_5 - 1, i_4)
        }
    }

    @JvmStatic
    fun method1083(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onDrag = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setComponentTextAlign(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 3
        component!!.textHorizontalAli = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.textVerticalAli = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        component!!.lineSpacing = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method2170(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onSubClose = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method14605(component: Component, cs2executor1: CS2Executor) {
        val b_3 = 10
        val value = cs2executor1.intStack[--cs2executor1.intStackPtr]
        method4883(component!!, b_3, value)
    }

    @JvmStatic
    fun method6186(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        if (component!!.type == ComponentType.TEXT) {
            method5459(component!!, parentInterface!!, cs2executor2)
        }
    }

    @JvmStatic
    fun method8310(component: Component, cs2executor2: CS2Executor) {
        component!!.angle2d = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method542(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onClanVarTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setIfFont(component: Component, intr: Interface, cs2: CS2Executor) {
        component!!.fontId = cs2.intStack[--cs2.intStackPtr]
        Component.redrawComponent(component)
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIffontUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method7729(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        component!!.sunRotationX = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.sunRotationY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setComponentPosition(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        component!!.basePositionX = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.basePositionY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        var i_4 = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        if (i_4 < 0) {
            i_4 = 0
        } else if (i_4 > 5) {
            i_4 = 5
        }
        var i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        if (i_5 < 0) {
            i_5 = 0
        } else if (i_5 > 5) {
            i_5 = 5
        }
        component!!.aspectXType = i_4.toByte()
        component!!.aspectYType = i_5.toByte()
        Component.redrawComponent(component!!)
        method913(parentInterface, component)
        if (component!!.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, component, false)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfpositionUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method1454(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        val i_4 = cs2executor2.intStack[cs2executor2.intStackPtr]
        val i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        val attributedefault6: ParamType? = TypeLists.PARAMS!!.list(i_4)
        if (i_5 != attributedefault6!!.defaultInt) {
            component!!.setIntParam(i_4, i_5)
        } else {
            component!!.removeParam(i_4)
        }
    }

    @JvmStatic
    fun method1455(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        component!!.primaryMediaId = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.secondaryMediaId = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        if (cs2executor2.intStack[cs2executor2.intStackPtr + 2] == 1) {
            component!!.mediaType = MediaType.ITEM_CONTAINER_FEMALE
        } else {
            component!!.mediaType = MediaType.ITEM_CONTAINER_MALE
        }
        component!!.wearCol = cs2executor2.intStack[cs2executor2.intStackPtr + 3] == 1
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method14653(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onCamFinished = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method12260(icDefs: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 4
        icDefs.baseWidth = executor.intStack[executor.intStackPtr]
        icDefs.baseHeight = executor.intStack[executor.intStackPtr + 1]
        icDefs.aspectWidth = 0
        icDefs.aspectHeight = 0
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        if (i_4 < 0) {
            i_4 = 0
        } else if (i_4 > 4) {
            i_4 = 4
        }
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        if (i_5 < 0) {
            i_5 = 0
        } else if (i_5 > 4) {
            i_5 = 4
        }
        icDefs.aspectWidthType = i_4.toByte()
        icDefs.aspectHeightType = i_5.toByte()
        Component.redrawComponent(icDefs)
        method913(parentInterface, icDefs)
        if (icDefs.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, icDefs, false)
        }
    }

    @JvmStatic
    fun method7555(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.mediaType = MediaType.RAW_MODEL
        component!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method6010(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 3
        val value = cs2executor2.intStack[cs2executor2.intStackPtr]
        val s_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1].toShort()
        val s_6 = cs2executor2.intStack[cs2executor2.intStackPtr + 2].toShort()
        if (value >= 0 && value < 5) {
            component!!.retexture(value, s_5, s_6)
            Component.redrawComponent(component!!)
            if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
                PulseEventQueue.delayedIfretex(component!!.idHash, value)
            }
        }
    }

    @JvmStatic
    fun method1845(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onResize = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method3365(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onRelease = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method3366(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onFriendsChatTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTargetVerb(component: Component, cs2executor2: CS2Executor) {
        component!!.targetVerb = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            component!!.targetVerb += "(targetVerb)"
    }

    @JvmStatic
    fun method3965(component: Component, cs2executor1: CS2Executor) {
        val fontmetrics3: FontMetrics? = component!!.getFontMetrics(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
        val i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        val i_5 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        val i_6 = fontmetrics3!!.getIndexAtPosition(component!!.text, component!!.width, component!!.lineSpacing, i_5, i_4, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = i_6
    }

    @JvmStatic
    fun method3987(component: Component, parentInterface: Interface) {
        component!!.mediaType = MediaType.PLAYER_HEAD
        component!!.primaryMediaId = PlayerUpdate.myPlayerIndex
        component!!.secondaryMediaId = 0
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setResumeText(component: Component, cs2executor2: CS2Executor) {
        component!!.resumeText = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            component!!.resumeText += "(resumeText)"
    }

    @JvmStatic
    fun method12401(component: Component, parentInterface: Interface) {
        component!!.mediaType = MediaType.PLAYER_MODEL
        component!!.primaryMediaId = PlayerUpdate.myPlayerIndex
        component!!.secondaryMediaId = 0
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method14892(component: Component, cs2executor2: CS2Executor) {
        component!!.lineWidth = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method4276(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 6
        component!!.offsetX = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.offsetY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        component!!.spritePitch = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        component!!.spriteRoll = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        component!!.spriteYaw = cs2executor2.intStack[cs2executor2.intStackPtr + 4]
        component!!.spriteScale = cs2executor2.intStack[cs2executor2.intStackPtr + 5]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(component!!.idHash)
            PulseEventQueue.delayedIfoffsetUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method4280(component: Component, cs2executor2: CS2Executor) {
        var flag: Boolean = false
        flag = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        component!!.flipVertical = flag
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method12630(component: Component) {
        component!!.reset()
    }

    @JvmStatic
    fun method5066(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.mediaType = MediaType.PLAYER_MODEL
        component!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method13450(component: Component, cs2executor2: CS2Executor) {
        val value = cs2executor2.intStack[--cs2executor2.intStackPtr]
        component!!.lineDirection = value == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method5291(component: Component, cs2executor2: CS2Executor) {
        val value = cs2executor2.intStack[--cs2executor2.intStackPtr] - 1
        if (value >= 0 && value <= 9) {
            component!!.addOptionName(value, cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String)
        } else {
            --cs2executor2.stringStackPtr
        }
    }

    @JvmStatic
    fun method797(component: Component, intr: Interface, cs2: CS2Executor) {
        val sequenceId = cs2.intStack[--cs2.intStackPtr]
        if (sequenceId != component!!.animation) {
            if (sequenceId != -1) {
                if (component!!.animatingSequence == null) {
                    component!!.animatingSequence = InterfaceSequence()
                }
                component!!.animatingSequence!!.update(sequenceId)
            } else {
                component!!.animatingSequence = null
            }
            component!!.animation = sequenceId
            Component.redrawComponent(component)
        }
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIfanimUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setTransparency(component: Component, cs2executor2: CS2Executor) {
        component!!.transparency = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method5455(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onClanChannelTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method5459(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        val value = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (value != component!!.videoFileId) {
            component!!.videoFileId = value
            Component.redrawComponent(component!!)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.queueT17Event(component!!.idHash)
        }
    }

    @JvmStatic
    fun method569(component: Component, cs2executor2: CS2Executor) {
        component!!.maxTextLines = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun method3251(component: Component, cs2executor1: CS2Executor) {
        if (cs2executor1.hookRequestCount >= 10) {
            throw RuntimeException()
        } else {
            if (component!!.onResize != null) {
                val hookrequest3: HookRequest? = HookRequest()
                hookrequest3!!.source = component
                hookrequest3!!.params = component!!.onResize
                hookrequest3!!.requestId = cs2executor1.hookRequestCount + 1
                Client.PENDING_HOOK_REQUESTS.addFirst(hookrequest3)
            }
        }
    }

    @JvmStatic
    fun method5001(component: Component, cs2executor1: CS2Executor) {
        val fontmetrics3: FontMetrics? = component!!.getFontMetrics(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
        val value = cs2executor1.intStack[--cs2executor1.intStackPtr]
        val point5: Point? = fontmetrics3!!.getPositionAtIndex(component!!.text, component!!.width, component!!.lineSpacing, value, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = point5!!.x
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = point5!!.y
    }

    @JvmStatic
    fun method3338(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        val stack: IntArray? = popIntArrayParam(text, cs2executor2)
        if (stack != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onStatTransmit = popParamsDynamic(text, cs2executor2)
        component!!.statTransmitFilter = stack
        component!!.hasScript = true
    }

    @JvmStatic
    fun setComponentAspect(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        component!!.aspectX = cs2executor2.intStack[cs2executor2.intStackPtr]
        component!!.aspectY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(component!!)
        method913(parentInterface, component)
        if (component!!.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, component, false)
        }
    }

    @JvmStatic
    fun method913(parentInterface: Interface, icomponentdefinitions1: Component) {
        val icomponentdefinitions3: Component? = Interface.getSub(parentInterface, icomponentdefinitions1)
        var i_4: Int = 0
        var i_5: Int = 0
        if (icomponentdefinitions3 == null) {
            i_4 = GameShell.screenCanvasWidth
            i_5 = GameShell.screenCanvasHeight
        } else {
            i_4 = icomponentdefinitions3!!.width
            i_5 = icomponentdefinitions3!!.height
        }
        InterfaceRenderer.initSizes(icomponentdefinitions1!!, i_4, i_5, false)
        InterfaceComponentRenderer.componentPositionAdjustment(icomponentdefinitions1!!, i_4, i_5)
    }

    @JvmStatic
    fun setDepthChild(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        val i_4 = cs2executor2.intStack[cs2executor2.intStackPtr]
        val i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        if (i_4 == -1 && i_5 == -1) {
            component!!.depthChildOverride = null
        } else {
            component!!.depthChildOverride = Component.listIcomponent(i_4, i_5)
        }
    }

    @JvmStatic
    fun method14586(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.hookParams = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method6045(component: Component, cs2executor2: CS2Executor) {
        component!!.targetLeaveCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
        component!!.targetOverCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun setOnMouseOver(iCompDefs: Component, cs2executor2: CS2Executor) {
        var paramTypes: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, cs2executor2) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        iCompDefs.popupScript = popParamsDynamic(paramTypes, cs2executor2)
        iCompDefs.hasScript = true
    }

    @JvmStatic
    fun method869(component: Component, cs2executor2: CS2Executor) {
        val value: Component.DragType? = Component.DragType.forId(cs2executor2.intStack[--cs2executor2.intStackPtr])
        if (value != null)
            component.dragType = value
    }

    @JvmStatic
    fun method910(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.params = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setNoClickThrough(component: Component, cs2executor2: CS2Executor) {
        component!!.preventClickThrough = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
    }

    @JvmStatic
    fun method12209(component: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        val b_4 = 10
        @Suppress("UNCHECKED_CAST")
        val bytes_5: ByteArray? = cs2executor2.globalArrays[cs2executor2.intStack[cs2executor2.intStackPtr]] as ByteArray?
        @Suppress("UNCHECKED_CAST")
        val bytes_6: ByteArray? = cs2executor2.globalArrays[cs2executor2.intStack[cs2executor2.intStackPtr + 1]] as ByteArray?
        setTriggerKey(component!!, b_4, bytes_5, bytes_6)
    }

    @JvmStatic
    fun method1838(component: Component, bytes_1: ByteArray?, bytes_2: ByteArray?, cs2executor3: CS2Executor) {
        val value = cs2executor3.intStack[--cs2executor3.intStackPtr] - 1
        if (value >= 0 && value <= 9) {
            setTriggerKey(component!!, value, bytes_1, bytes_2)
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun method15409(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onClick = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method6158(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onFriendTransmit = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun method11337(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.mediaType = MediaType.NPC_MODEL
        component!!.npcCustomisation = null
        component!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method15474(component: Component) {
        component!!.options = null
    }

    @JvmStatic
    fun method1642(component: Component, cs2executor2: CS2Executor) {
        var text: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(text, cs2executor2) != null) {
            text = text!!.substring(0, text!!.length - 1)
        }
        component!!.onTargetEnter = popParamsDynamic(text, cs2executor2)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTargetOpCursor(component: Component, cs2executor2: CS2Executor) {
        component!!.targetOpCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method7667(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 3
        val value = cs2executor2.intStack[cs2executor2.intStackPtr]
        val s_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1].toShort()
        val s_6 = cs2executor2.intStack[cs2executor2.intStackPtr + 2].toShort()
        if (value >= 0 && value < 5) {
            component!!.recolor(value, s_5, s_6)
            Component.redrawComponent(component!!)
            if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
                PulseEventQueue.delayedIfrecol(component!!.idHash, value)
            }
        }
    }

    @JvmStatic
    fun method6697(component: Component, parentInterface: Interface, cs2executor2: CS2Executor) {
        component!!.monospaced = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfmonospacedUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun popIntArrayParam(paramTypes: String?, cs2executor1: CS2Executor): IntArray? {
        var intArr: IntArray? = null
        if (!paramTypes.isNullOrEmpty() && paramTypes[paramTypes.length - 1] == 'Y') {
            var size = cs2executor1.intStack[--cs2executor1.intStackPtr]
            if (size > 0) {
                intArr = IntArray(size)
                while (size-- > 0) {
                    intArr[size] = cs2executor1.intStack[--cs2executor1.intStackPtr]
                }
            }
        }
        return intArr
    }

    @JvmStatic
    fun popParamsDynamic(paramTypes: String?, executor: CS2Executor): Array<Any?>? {
        if (paramTypes == null) return null
        var params: Array<Any?>? = arrayOfNulls(paramTypes.length + 1)
        for (i in params!!.size - 1 downTo 1) {
            if (paramTypes[i - 1] == 's') {
                params[i] = executor.stringStack[--executor.stringStackPtr]
            } else if (paramTypes[i - 1] == '\u00a7') {
                params[i] = java.lang.Long.valueOf(executor.longStack[--executor.longStackPtr])
            } else {
                params[i] = Integer.valueOf(executor.intStack[--executor.intStackPtr])
            }
        }
        val scriptId = executor.intStack[--executor.intStackPtr]
        if (scriptId != -1) {
            params[0] = Integer.valueOf(scriptId)
        } else {
            params = null
        }
        return params
    }

}
