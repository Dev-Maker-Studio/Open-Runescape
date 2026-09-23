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

object CS2SharedOps {

    @JvmStatic
    fun substring(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        val startIndex = executor.intStack[executor.intStackPtr]
        val endIndex = executor.intStack[executor.intStackPtr + 1]
        executor.stringStack[++executor.stringStackPtr - 1] = text!!.substring(startIndex, endIndex)
    }

    @JvmStatic
    fun toString(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = Integer.toString(value)
    }

    @JvmStatic
    fun setModelZoom(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.spriteScale = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOpCharSlot(component: Component, opSlot: Int, charCode: Int) {
        if (component!!.keyCharCodes == null) {
            if (charCode <= 0) {
                return
            }
            component!!.keyTriggeringAction = arrayOfNulls(11)
            component!!.keyTriggerParams = arrayOfNulls(11)
            component!!.cycleSteps = IntArray(11)
            component!!.keyCharCodes = IntArray(11)
        }
        component!!.keyCharCodes!![opSlot] = charCode
        if (charCode > 0) {
            component!!.hasInteraction = true
        } else {
            component!!.hasInteraction = false
            val keyTriggeringActionArr = component!!.keyTriggeringAction!!
            for (slot in 0 until keyTriggeringActionArr.size) {
                if (keyTriggeringActionArr[slot] != null || component!!.keyCharCodes!![slot] > 0) {
                    component!!.hasInteraction = true
                    break
                }
            }
        }
    }

    @JvmStatic
    fun setScrollPos(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 2
        component!!.scrollX = executor.intStack[executor.intStackPtr]
        if (component!!.scrollX > component!!.scrollWidth - component!!.width) {
            component!!.scrollX = component!!.scrollWidth - component!!.width
        }
        if (component!!.scrollX < 0) {
            component!!.scrollX = 0
        }
        component!!.scrollY = executor.intStack[executor.intStackPtr + 1]
        if (component!!.scrollY > component!!.scrollHeight - component!!.height) {
            component!!.scrollY = component!!.scrollHeight - component!!.height
        }
        if (component!!.scrollY < 0) {
            component!!.scrollY = 0
        }
        Component.redrawComponent(component)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfscrollposUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOnMouseEnter(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onMouseOver = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOpChar(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 2
        val opSlot = executor.intStack[executor.intStackPtr] - 1
        val charCode = executor.intStack[executor.intStackPtr + 1]
        if (opSlot >= 0 && opSlot <= 9) {
            setOpCharSlot(component!!, opSlot, charCode)
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun setOnHold(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onHold = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnMouseRepeat(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onMouseMove = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnMouseLeave(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val triggers: IntArray? = popIntArrayParam(paramTypes, executor)
        if (triggers != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.mouseLeaveScript = popParamsDynamic(paramTypes, executor)
        component!!.mouseLeaveArrayParams = triggers
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnSubchange(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onSubChange = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setHFlip(component: Component, executor: CS2Executor) {
        component!!.flipHorizontal = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setComponentModelOrigin(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 2
        component!!.originX = executor.intStack[executor.intStackPtr]
        component!!.originY = executor.intStack[executor.intStackPtr + 1]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnClickRepeat(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onClickRepeat = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setModelOrthog(component: Component, executor: CS2Executor) {
        component!!.usesOrthogonal = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnChatTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onChatTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTextShadow(component: Component, executor: CS2Executor) {
        component!!.shadow = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnScrollWheel(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val triggers: IntArray? = popIntArrayParam(paramTypes, executor)
        if (triggers != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onScrollWheel = popParamsDynamic(paramTypes, executor)
        component!!.mouseWheelArray = triggers
        component!!.hasScript = true
    }

    @JvmStatic
    fun setGraphicShadow(component: Component, executor: CS2Executor) {
        component!!.spriteShadow = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setFill(component: Component, executor: CS2Executor) {
        component!!.filled = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun ifSetColor(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.color = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfcolorUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOnVarTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val triggers: IntArray? = popIntArrayParam(paramTypes, executor)
        if (triggers != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onVarpTransmit = popParamsDynamic(paramTypes, executor)
        component!!.varps = triggers
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnStockTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onStockTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setModelTint(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 4
        component!!.modelTintRed = executor.intStack[executor.intStackPtr]
        component!!.modelTintGreen = executor.intStack[executor.intStackPtr + 1]
        component!!.modelTintBlue = executor.intStack[executor.intStackPtr + 2]
        component!!.modelTintScalar = executor.intStack[executor.intStackPtr + 3]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOutline(component: Component, executor: CS2Executor) {
        component!!.borderThickness = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnTargetLeave(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onTargetLeave = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnMouseExit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onMouseLeaveScript = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTiling(component: Component, executor: CS2Executor) {
        component!!.tiling = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnInvTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val triggers: IntArray? = popIntArrayParam(paramTypes, executor)
        if (triggers != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onKey = popParamsDynamic(paramTypes, executor)
        component!!.keyPressArray = triggers
        component!!.hasScript = true
    }

    @JvmStatic
    fun setParamString(component: Component, executor: CS2Executor) {
        val paramId = executor.intStack[--executor.intStackPtr]
        val stringValue: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (!paramType!!.typeName.equals(stringValue!!)) {
            component!!.setStringParam(paramId, stringValue)
        } else {
            component!!.removeParam(paramId)
        }
    }

    @JvmStatic
    fun setScrollSize(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 2
        component!!.scrollWidth = executor.intStack[executor.intStackPtr]
        component!!.scrollHeight = executor.intStack[executor.intStackPtr + 1]
        Component.redrawComponent(component!!)
        if (component!!.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, component, false)
        }
    }

    @JvmStatic
    fun createComponent(parentInterface: Interface, parentComponentIndex: Int, componentTypeId: Int, slotId: Int, useSecondaryHook: Boolean, executor: CS2Executor) {
        if (componentTypeId == 0) {
            throw RuntimeException()
        } else {
            val parentComponent: Component? = parentInterface.components[parentComponentIndex]
            if (parentComponent!!.slotChildren == null) {
                @Suppress("UNCHECKED_CAST")
                parentComponent!!.slotChildren = arrayOfNulls<Component>(slotId + 1) as Array<Component>?
                @Suppress("UNCHECKED_CAST")
                parentComponent!!.itemSlots = parentComponent!!.slotChildren as Array<Component?>?
            }
            if (parentComponent!!.slotChildren!!.size <= slotId) {
                @Suppress("UNCHECKED_CAST")
                val expandedSlotChildren: Array<Component>? = arrayOfNulls<Component>(slotId + 1) as Array<Component>?
                if (parentComponent!!.slotChildren == parentComponent!!.itemSlots) {
                    System.arraycopy(parentComponent!!.slotChildren, 0, expandedSlotChildren, 0, parentComponent!!.slotChildren!!.size)
                    parentComponent!!.slotChildren = expandedSlotChildren
                    @Suppress("UNCHECKED_CAST")
                    parentComponent!!.itemSlots = expandedSlotChildren as Array<Component?>?
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val expandedItemSlots: Array<Component?>? = arrayOfNulls<Component>(slotId + 1)
                    for (slot in 0 until parentComponent!!.slotChildren!!.size) {
                        expandedSlotChildren!![slot] = parentComponent!!.slotChildren!![slot]
                        expandedItemSlots!![slot] = parentComponent!!.itemSlots!![slot]
                    }
                    parentComponent!!.slotChildren = expandedSlotChildren
                    parentComponent!!.itemSlots = expandedItemSlots
                }
            }
            if (slotId > 0 && parentComponent!!.slotChildren!![slotId - 1] == null) {
                throw RuntimeException("" + (slotId - 1))
            } else {
                val newComponent: Component? = Component()
                newComponent!!.type = ComponentType.forId(componentTypeId)
                newComponent!!.idHash = parentComponent!!.idHash
                newComponent!!.parentId = parentComponent!!.idHash
                newComponent!!.slotId = slotId
                parentComponent!!.slotChildren!![slotId] = newComponent
                if (parentComponent!!.slotChildren != parentComponent!!.itemSlots) {
                    parentComponent!!.itemSlots!![slotId] = newComponent
                }
                var hookTarget: CS2Interface? = null
                if (useSecondaryHook) {
                    hookTarget = executor.hookedInterface2
                } else {
                    hookTarget = executor.hookedInterface1
                }
                hookTarget!!.inter = parentInterface
                hookTarget!!.comp = newComponent
                Component.redrawComponent(parentComponent!!)
            }
        }
    }

    @JvmStatic
    fun setAlpha(component: Component, executor: CS2Executor) {
        var alphaEnabled: Boolean = false
        alphaEnabled = executor.intStack[--executor.intStackPtr] == 1
        component!!.alpha = alphaEnabled
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setDragDeadZone(component: Component, executor: CS2Executor) {
        component!!.dragDeadzone = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun setGraphic(component: Component, parentInterface: Interface, executor: CS2Executor) {
        val spriteId = executor.intStack[--executor.intStackPtr]
        if (spriteId != component!!.spriteId) {
            component!!.spriteId = spriteId
            Component.redrawComponent(component!!)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfgraphicUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setDragDeadTime(component: Component, executor: CS2Executor) {
        component!!.dragDeadTime = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun setOnKey(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onKeyType = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setModelLighting(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 4
        component!!.skyboxId = executor.intStack[executor.intStackPtr]
        component!!.lightPositionX = executor.intStack[executor.intStackPtr + 1]
        component!!.lightPositionY = executor.intStack[executor.intStackPtr + 2]
        component!!.lightPositionZ = executor.intStack[executor.intStackPtr + 3]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setHide(component: Component, parentInterface: Interface, executor: CS2Executor) {
        val hidden = executor.intStack[--executor.intStackPtr] == 1
        if (hidden != component!!.hidden) {
            component!!.hidden = hidden
            Component.redrawComponent(component!!)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfhide(component!!.idHash)
        }
    }

    @JvmStatic
    fun setVideoSprite(component: Component, parentInterface: Interface, executor: CS2Executor) {
        if (component!!.type == ComponentType.SPRITE) {
            setVideoFileId(component!!, parentInterface!!, executor)
        }
    }

    @JvmStatic
    fun setOnClanSettingsTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onClanSettingsTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setMouseOverCursor(component: Component, executor: CS2Executor) {
        component!!.moveOverCursor = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun setOnMiscTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onMiscTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTriggerKey(component: Component, opSlot: Int, keyCodes: ByteArray?, keyModifiers: ByteArray?) {
        if (component!!.keyTriggeringAction == null) {
            if (keyCodes == null) {
                return
            }
            component!!.keyTriggeringAction = arrayOfNulls(11)
            component!!.keyTriggerParams = arrayOfNulls(11)
            component!!.cycleSteps = IntArray(11)
            component!!.keyCharCodes = IntArray(11)
        }
        component!!.keyTriggeringAction!![opSlot] = keyCodes
        if (keyCodes != null) {
            component!!.hasInteraction = true
        } else {
            component!!.hasInteraction = false
            for (slot in 0 until component!!.keyTriggeringAction!!.size) {
                if (component!!.keyTriggeringAction!![slot] != null || component!!.keyCharCodes!![slot] > 0) {
                    component!!.hasInteraction = true
                    break
                }
            }
        }
        component!!.keyTriggerParams!![opSlot] = keyModifiers
    }

    @JvmStatic
    fun ifSetNPCHead(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.mediaType = MediaType.NPC_HEAD
        component!!.npcCustomisation = null
        component!!.primaryMediaId = executor.intStack[--executor.intStackPtr]
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setTextAntimacro(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.textAntiMacro = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedClearIftextantimacro(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOnDragComplete(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onDragComplete = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setClickmask(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.clickMask = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfclickmaskUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOnTimer(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onTimer = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setItemIFComp(component: Component, parentInterface: Interface, wearCol: Boolean, renderStack: Component.StackMode, executor: CS2Executor) {
        executor.intStackPtr -= 2
        val itemId = executor.intStack[executor.intStackPtr]
        val stackSize = executor.intStack[executor.intStackPtr + 1]
        if (component.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfitemUpdate(component.idHash)
            PulseEventQueue.delayedIfangleUpdate(component.idHash)
            PulseEventQueue.delayedIfoffsetUpdate(component.idHash)
        }
        if (itemId == -1) {
            component.mediaType = MediaType.RAW_MODEL
            component.primaryMediaId = -1
            component.containerItemId = -1
        } else {
            component.containerItemId = itemId
            component.itemStackSize = stackSize
            component.wearCol = wearCol
            val itemType: ItemType? = TypeLists.OBJ!!.list(itemId)
            component.spritePitch = itemType!!.modelRotationX
            component.spriteRoll = itemType!!.modelRotationY
            component.spriteYaw = itemType!!.modelRotationZ
            component.offsetX = itemType!!.modelOffsetX
            component.offsetY = itemType!!.modelOffsetY
            component.spriteScale = itemType!!.modelZoom
            component.renderStack = renderStack
            if (component.aspectWidth > 0) {
                component.spriteScale = component.spriteScale * 32 / component.aspectWidth
            } else if (component.baseWidth > 0) {
                component.spriteScale = component.spriteScale * 32 / component.baseWidth
            }
        }
    }

    @JvmStatic
    fun setComponentText(component: Component, parentInterface: Interface, executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (!text.equals(component!!.text)) {
            component!!.text = text ?: ""
            Component.redrawComponent(component)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIftextUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOpBase(component: Component, executor: CS2Executor) {
        component!!.opBase = executor.stringStack[--executor.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            component!!.opBase += "(opBase)"
    }

    @JvmStatic
    fun setOpCursor(component: Component, executor: CS2Executor) {
        val cursorId = executor.intStack[--executor.intStackPtr]
        val opSlot = executor.intStack[--executor.intStackPtr]
        if (opSlot >= 1 && opSlot <= 10) {
            component!!.addCursor(opSlot - 1, cursorId)
        }
    }

    @JvmStatic
    fun setOnDrag(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onDrag = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setComponentTextAlign(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 3
        component!!.textHorizontalAli = executor.intStack[executor.intStackPtr]
        component!!.textVerticalAli = executor.intStack[executor.intStackPtr + 1]
        component!!.lineSpacing = executor.intStack[executor.intStackPtr + 2]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnDialogAbort(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onSubClose = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOpTChar(component: Component, executor: CS2Executor) {
        val targetOpSlot = 10
        val charCode = executor.intStack[--executor.intStackPtr]
        setOpCharSlot(component!!, targetOpSlot, charCode)
    }

    @JvmStatic
    fun setVideoText(component: Component, parentInterface: Interface, executor: CS2Executor) {
        if (component!!.type == ComponentType.TEXT) {
            setVideoFileId(component!!, parentInterface!!, executor)
        }
    }

    @JvmStatic
    fun set2DAngle(component: Component, executor: CS2Executor) {
        component!!.angle2d = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnVarClanTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onClanVarTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setIfFont(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.fontId = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIffontUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setModelLightingSunRotation(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 2
        component!!.sunRotationX = executor.intStack[executor.intStackPtr]
        component!!.sunRotationY = executor.intStack[executor.intStackPtr + 1]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setComponentPosition(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 4
        component!!.basePositionX = executor.intStack[executor.intStackPtr]
        component!!.basePositionY = executor.intStack[executor.intStackPtr + 1]
        var aspectXType = executor.intStack[executor.intStackPtr + 2]
        if (aspectXType < 0) {
            aspectXType = 0
        } else if (aspectXType > 5) {
            aspectXType = 5
        }
        var aspectYType = executor.intStack[executor.intStackPtr + 3]
        if (aspectYType < 0) {
            aspectYType = 0
        } else if (aspectYType > 5) {
            aspectYType = 5
        }
        component!!.aspectXType = aspectXType.toByte()
        component!!.aspectYType = aspectYType.toByte()
        Component.redrawComponent(component!!)
        relayoutAgainstParent(parentInterface, component)
        if (component!!.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, component, false)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfpositionUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setParamInt(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 2
        val paramId = executor.intStack[executor.intStackPtr]
        val value = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (value != paramType!!.defaultInt) {
            component!!.setIntParam(paramId, value)
        } else {
            component!!.removeParam(paramId)
        }
    }

    @JvmStatic
    fun setModelItemContainer(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 4
        component!!.primaryMediaId = executor.intStack[executor.intStackPtr]
        component!!.secondaryMediaId = executor.intStack[executor.intStackPtr + 1]
        if (executor.intStack[executor.intStackPtr + 2] == 1) {
            component!!.mediaType = MediaType.ITEM_CONTAINER_FEMALE
        } else {
            component!!.mediaType = MediaType.ITEM_CONTAINER_MALE
        }
        component!!.wearCol = executor.intStack[executor.intStackPtr + 3] == 1
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOnCamfinished(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onCamFinished = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setComponentSize(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 4
        component.baseWidth = executor.intStack[executor.intStackPtr]
        component.baseHeight = executor.intStack[executor.intStackPtr + 1]
        component.aspectWidth = 0
        component.aspectHeight = 0
        var aspectWidthType = executor.intStack[executor.intStackPtr + 2]
        if (aspectWidthType < 0) {
            aspectWidthType = 0
        } else if (aspectWidthType > 4) {
            aspectWidthType = 4
        }
        var aspectHeightType = executor.intStack[executor.intStackPtr + 3]
        if (aspectHeightType < 0) {
            aspectHeightType = 0
        } else if (aspectHeightType > 4) {
            aspectHeightType = 4
        }
        component.aspectWidthType = aspectWidthType.toByte()
        component.aspectHeightType = aspectHeightType.toByte()
        Component.redrawComponent(component)
        relayoutAgainstParent(parentInterface, component)
        if (component.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, component, false)
        }
    }

    @JvmStatic
    fun setComponentModel(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.mediaType = MediaType.RAW_MODEL
        component!!.primaryMediaId = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setRetex(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 3
        val retexIndex = executor.intStack[executor.intStackPtr]
        val srcTexture = executor.intStack[executor.intStackPtr + 1].toShort()
        val dstTexture = executor.intStack[executor.intStackPtr + 2].toShort()
        if (retexIndex >= 0 && retexIndex < 5) {
            component!!.retexture(retexIndex, srcTexture, dstTexture)
            Component.redrawComponent(component!!)
            if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
                PulseEventQueue.delayedIfretex(component!!.idHash, retexIndex)
            }
        }
    }

    @JvmStatic
    fun setOnResize(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onResize = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnRelease(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onRelease = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnFriendsChatTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onFriendsChatTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTargetVerb(component: Component, executor: CS2Executor) {
        component!!.targetVerb = executor.stringStack[--executor.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            component!!.targetVerb += "(targetVerb)"
    }

    @JvmStatic
    fun getCharIndexAtPos(component: Component, executor: CS2Executor) {
        val fontMetrics: FontMetrics? = component!!.getFontMetrics(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
        val y = executor.intStack[--executor.intStackPtr]
        val x = executor.intStack[--executor.intStackPtr]
        val charIndex = fontMetrics!!.getIndexAtPosition(component!!.text, component!!.width, component!!.lineSpacing, x, y, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
        executor.intStack[++executor.intStackPtr - 1] = charIndex
    }

    @JvmStatic
    fun setPlayerHeadSelf(component: Component, parentInterface: Interface) {
        component!!.mediaType = MediaType.PLAYER_HEAD
        component!!.primaryMediaId = PlayerUpdate.myPlayerIndex
        component!!.secondaryMediaId = 0
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setResumeText(component: Component, executor: CS2Executor) {
        component!!.resumeText = executor.stringStack[--executor.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            component!!.resumeText += "(resumeText)"
    }

    @JvmStatic
    fun setPlayerModelSelf(component: Component, parentInterface: Interface) {
        component!!.mediaType = MediaType.PLAYER_MODEL
        component!!.primaryMediaId = PlayerUpdate.myPlayerIndex
        component!!.secondaryMediaId = 0
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setLineWid(component: Component, executor: CS2Executor) {
        component!!.lineWidth = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setModelAngle(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 6
        component!!.offsetX = executor.intStack[executor.intStackPtr]
        component!!.offsetY = executor.intStack[executor.intStackPtr + 1]
        component!!.spritePitch = executor.intStack[executor.intStackPtr + 2]
        component!!.spriteRoll = executor.intStack[executor.intStackPtr + 3]
        component!!.spriteYaw = executor.intStack[executor.intStackPtr + 4]
        component!!.spriteScale = executor.intStack[executor.intStackPtr + 5]
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(component!!.idHash)
            PulseEventQueue.delayedIfoffsetUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setVFlip(component: Component, executor: CS2Executor) {
        var flipVertical: Boolean = false
        flipVertical = executor.intStack[--executor.intStackPtr] == 1
        component!!.flipVertical = flipVertical
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun clearScriptHooks(component: Component) {
        component!!.reset()
    }

    @JvmStatic
    fun setPlayerModel(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.mediaType = MediaType.PLAYER_MODEL
        component!!.primaryMediaId = executor.intStack[--executor.intStackPtr]
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setLineDirection(component: Component, executor: CS2Executor) {
        val direction = executor.intStack[--executor.intStackPtr]
        component!!.lineDirection = direction == 1
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOp(component: Component, executor: CS2Executor) {
        val opSlot = executor.intStack[--executor.intStackPtr] - 1
        if (opSlot >= 0 && opSlot <= 9) {
            component!!.addOptionName(opSlot, executor.stringStack[--executor.stringStackPtr] as String?)
        } else {
            --executor.stringStackPtr
        }
    }

    @JvmStatic
    fun setModelAnim(component: Component, parentInterface: Interface, executor: CS2Executor) {
        val sequenceId = executor.intStack[--executor.intStackPtr]
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
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfanimUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setTransparency(component: Component, executor: CS2Executor) {
        component!!.transparency = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun setOnClanChannelTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onClanChannelTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setVideoFileId(component: Component, parentInterface: Interface, executor: CS2Executor) {
        val videoFileId = executor.intStack[--executor.intStackPtr]
        if (videoFileId != component!!.videoFileId) {
            component!!.videoFileId = videoFileId
            Component.redrawComponent(component!!)
        }
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.queueT17Event(component!!.idHash)
        }
    }

    @JvmStatic
    fun setMaxLines(component: Component, executor: CS2Executor) {
        component!!.maxTextLines = executor.intStack[--executor.intStackPtr]
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun callonresize(component: Component, executor: CS2Executor) {
        if (executor.hookRequestCount >= 10) {
            throw RuntimeException()
        } else {
            if (component!!.onResize != null) {
                val hookRequest: HookRequest? = HookRequest()
                hookRequest!!.source = component
                hookRequest!!.params = component!!.onResize
                hookRequest!!.requestId = executor.hookRequestCount + 1
                Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
            }
        }
    }

    @JvmStatic
    fun getCharPosAtIndex(component: Component, executor: CS2Executor) {
        val fontMetrics: FontMetrics? = component!!.getFontMetrics(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
        val charIndex = executor.intStack[--executor.intStackPtr]
        val charPosition: Point? = fontMetrics!!.getPositionAtIndex(component!!.text, component!!.width, component!!.lineSpacing, charIndex, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
        executor.intStack[++executor.intStackPtr - 1] = charPosition!!.x
        executor.intStack[++executor.intStackPtr - 1] = charPosition!!.y
    }

    @JvmStatic
    fun setOnStatTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val triggers: IntArray? = popIntArrayParam(paramTypes, executor)
        if (triggers != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onStatTransmit = popParamsDynamic(paramTypes, executor)
        component!!.statTransmitFilter = triggers
        component!!.hasScript = true
    }

    @JvmStatic
    fun setComponentAspect(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 2
        component!!.aspectX = executor.intStack[executor.intStackPtr]
        component!!.aspectY = executor.intStack[executor.intStackPtr + 1]
        Component.redrawComponent(component!!)
        relayoutAgainstParent(parentInterface, component)
        if (component!!.type == ComponentType.CONTAINER) {
            Component.updateEntityLayout(parentInterface, component, false)
        }
    }

    @JvmStatic
    fun relayoutAgainstParent(parentInterface: Interface, component: Component) {
        val parentComponent: Component? = Interface.getSub(parentInterface, component)
        var parentWidth: Int = 0
        var parentHeight: Int = 0
        if (parentComponent == null) {
            parentWidth = GameShell.screenCanvasWidth
            parentHeight = GameShell.screenCanvasHeight
        } else {
            parentWidth = parentComponent!!.width
            parentHeight = parentComponent!!.height
        }
        InterfaceRenderer.initSizes(component!!, parentWidth, parentHeight, false)
        InterfaceComponentRenderer.componentPositionAdjustment(component!!, parentWidth, parentHeight)
    }

    @JvmStatic
    fun setDepthChild(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 2
        val depthChildIdHash = executor.intStack[executor.intStackPtr]
        val depthChildSlotId = executor.intStack[executor.intStackPtr + 1]
        if (depthChildIdHash == -1 && depthChildSlotId == -1) {
            component!!.depthChildOverride = null
        } else {
            component!!.depthChildOverride = Component.listIcomponent(depthChildIdHash, depthChildSlotId)
        }
    }

    @JvmStatic
    fun setOnOp(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.hookParams = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTargetCursors(component: Component, executor: CS2Executor) {
        component!!.targetLeaveCursor = executor.intStack[--executor.intStackPtr]
        component!!.targetOverCursor = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun setOnMouseOver(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component.popupScript = popParamsDynamic(paramTypes, executor)
        component.hasScript = true
    }

    @JvmStatic
    fun setDragRenderBehaviour(component: Component, executor: CS2Executor) {
        val dragType: Component.DragType? = Component.DragType.forId(executor.intStack[--executor.intStackPtr])
        if (dragType != null)
            component.dragType = dragType
    }

    @JvmStatic
    fun setOnOpt(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.params = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setNoClickThrough(component: Component, executor: CS2Executor) {
        component!!.preventClickThrough = executor.intStack[--executor.intStackPtr] == 1
    }

    @JvmStatic
    fun setOpTKey(component: Component, executor: CS2Executor) {
        executor.intStackPtr -= 2
        val targetOpSlot = 10
        @Suppress("UNCHECKED_CAST")
        val keyCodes: ByteArray? = executor.globalArrays[executor.intStack[executor.intStackPtr]] as ByteArray?
        @Suppress("UNCHECKED_CAST")
        val keyModifiers: ByteArray? = executor.globalArrays[executor.intStack[executor.intStackPtr + 1]] as ByteArray?
        setTriggerKey(component!!, targetOpSlot, keyCodes, keyModifiers)
    }

    @JvmStatic
    fun setOpKey(component: Component, keyCodes: ByteArray?, keyModifiers: ByteArray?, executor: CS2Executor) {
        val opSlot = executor.intStack[--executor.intStackPtr] - 1
        if (opSlot >= 0 && opSlot <= 9) {
            setTriggerKey(component!!, opSlot, keyCodes, keyModifiers)
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun setOnClick(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onClick = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setOnFriendTransmit(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onFriendTransmit = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setNpcModel(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.mediaType = MediaType.NPC_MODEL
        component!!.npcCustomisation = null
        component!!.primaryMediaId = executor.intStack[--executor.intStackPtr]
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun clearOps(component: Component) {
        component!!.options = null
    }

    @JvmStatic
    fun setOnTargetEnter(component: Component, executor: CS2Executor) {
        var paramTypes: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, executor) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        component!!.onTargetEnter = popParamsDynamic(paramTypes, executor)
        component!!.hasScript = true
    }

    @JvmStatic
    fun setTargetOpCursor(component: Component, executor: CS2Executor) {
        component!!.targetOpCursor = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun setRecol(component: Component, parentInterface: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 3
        val recolorIndex = executor.intStack[executor.intStackPtr]
        val srcColor = executor.intStack[executor.intStackPtr + 1].toShort()
        val dstColor = executor.intStack[executor.intStackPtr + 2].toShort()
        if (recolorIndex >= 0 && recolorIndex < 5) {
            component!!.recolor(recolorIndex, srcColor, dstColor)
            Component.redrawComponent(component!!)
            if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
                PulseEventQueue.delayedIfrecol(component!!.idHash, recolorIndex)
            }
        }
    }

    @JvmStatic
    fun setFontMono(component: Component, parentInterface: Interface, executor: CS2Executor) {
        component!!.monospaced = executor.intStack[--executor.intStackPtr] == 1
        Component.redrawComponent(component!!)
        if (component!!.slotId == -1 && !parentInterface.preventInteraction) {
            PulseEventQueue.delayedIfmonospacedUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun popIntArrayParam(paramTypes: String?, executor: CS2Executor): IntArray? {
        var triggers: IntArray? = null
        if (!paramTypes.isNullOrEmpty() && paramTypes[paramTypes.length - 1] == 'Y') {
            var size = executor.intStack[--executor.intStackPtr]
            if (size > 0) {
                triggers = IntArray(size)
                while (size-- > 0) {
                    triggers[size] = executor.intStack[--executor.intStackPtr]
                }
            }
        }
        return triggers
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
