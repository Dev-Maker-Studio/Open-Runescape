package com.jagex.game.runetek5.client.menu

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.param.ParamType
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.RenderedEntity
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.entity.grounditem.GroundItem
import com.jagex.game.runetek5.entity.grounditem.GroundItemNode
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.scene.MapLoader
import com.jagex.game.runetek5.scene.InteractiveEntityCache
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.vardomain.VarProviders

/**
 * Builds mini menu (right-click context menu) entries for game entities.
 *
 * @author Brainpower - 27/01/2023
 *
 * Value 2000 = lower the priority of the attack option (move it down if on top)
 */

@JvmField
var SHOW_FACE_HERE = false

@JvmField
var USE_OPTIONS_FLAGS = 0

@JvmName("playerHovered")
fun playerHovered(player: PlayerEntity, onDifferentPlane: Boolean) {
    if (MiniMenu.entryCount >= 412) return

    if (PlayerUpdate.MY_PLAYER == player) {
        if (client.interfaceSelectionActive && (USE_OPTIONS_FLAGS and 0x10) != 0) {
            MiniMenu.addEntry(
                client.useOnType,
                "${client.useOnTarget} -> ${TextUtils.setTextColor(16777215)}${LocalizedText.SELF.translate(client.CURRENT_LANGUAGE)}",
                MiniMenu.useOnCursor,
                16,
                -1,
                0L,
                0,
                0,
                true,
                false,
                player.playerIndex.toLong(),
                false
            )
        }
    } else {
        val username = when (player.elo) {
            0 -> {
                var combatColorDisplayed = true
                if (PlayerUpdate.MY_PLAYER.cbLevelRelated != -1 && player.cbLevelRelated != -1) {
                    val cmbRelatedDelta = minOf(PlayerUpdate.MY_PLAYER.cbLevelRelated, player.cbLevelRelated)
                    val cmbDelta = Math.abs(PlayerUpdate.MY_PLAYER.combatLevel - player.combatLevel)
                    if (cmbDelta > cmbRelatedDelta) {
                        combatColorDisplayed = false
                    }
                }
                val level = if (client.CURRENT_GAME == Game.STELLAR_DAWN) {
                    LocalizedText.RATING_.translate(client.CURRENT_LANGUAGE)
                } else {
                    LocalizedText.MINIMENU_LEVEL.translate(client.CURRENT_LANGUAGE)
                }
                val combatColor = if (combatColorDisplayed) {
                    TextUtils.calculateCombatBasedColor(player.combatLevel, PlayerUpdate.MY_PLAYER.combatLevel)
                } else {
                    TextUtils.setTextColor(16777215)
                }
                if (player.combatLevel >= player.cbLevelWithSumm) {
                    "${player.getUsernameWithTitle()}$combatColor ($level${player.combatLevel})"
                } else {
                    "${player.getUsernameWithTitle()}$combatColor ($level${player.combatLevel}+${player.cbLevelWithSumm - player.combatLevel})"
                }
            }
            -1 -> player.getUsernameWithTitle()
            else -> "${player.getUsernameWithTitle()} (${LocalizedText.SKILL_.translate(client.CURRENT_LANGUAGE)}${player.elo})"
        }

        if (client.interfaceSelectionActive && !onDifferentPlane && (USE_OPTIONS_FLAGS and 0x8) != 0) {
            MiniMenu.addEntry(
                client.useOnType,
                "${client.useOnTarget} -> ${TextUtils.setTextColor(16777215)}$username",
                MiniMenu.useOnCursor,
                15,
                -1,
                player.playerIndex.toLong(),
                0,
                0,
                true,
                false,
                player.playerIndex.toLong(),
                false
            )
        }

        if (onDifferentPlane) {
            MiniMenu.addEntry(
                "${TextUtils.setTextColor(13421772)}$username",
                "",
                -1,
                -1,
                0,
                0L,
                0,
                0,
                false,
                true,
                player.playerIndex.toLong(),
                false
            )
        } else {
            for (index in 7 downTo 0) {
                client.PLAYER_OPTIONS[index]?.let { option ->
                    var attackClickType: Short = 0
                    if (Game.RUNESCAPE == client.CURRENT_GAME &&
                        option.equals(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE), ignoreCase = true)) {
                        attackClickType = if (client.REDUCE_ATTACK_PRIORITY && player.combatLevel > PlayerUpdate.MY_PLAYER.combatLevel) {
                            2000
                        } else if (PlayerUpdate.MY_PLAYER.teamId != 0 && player.teamId != 0) {
                            if (player.teamId == PlayerUpdate.MY_PLAYER.teamId) 2000 else 0
                        } else if (player.reduceAttackPriority) {
                            2000
                        } else {
                            0
                        }
                    } else if (client.PLAYER_OPTION_REDUCED_PRIORITY[index]) {
                        attackClickType = 2000
                    }

                    val clickType = (client.aShortArray7239[index] + attackClickType).toShort()
                    val cursor = if (client.PLAYER_OPTION_CURSORS[index] != -1) {
                        client.PLAYER_OPTION_CURSORS[index]
                    } else {
                        CursorManager.defaultAttackCursor
                    }

                    MiniMenu.addEntry(
                        option,
                        "${TextUtils.setTextColor(16777215)}$username",
                        cursor,
                        clickType.toInt(),
                        -1,
                        player.playerIndex.toLong(),
                        0,
                        0,
                        true,
                        false,
                        player.playerIndex.toLong(),
                        false
                    )
                }
            }
        }

        if (!onDifferentPlane) {
            var entry = MiniMenu.aClass482171.getFirst() as? MiniMenuEntry
            while (entry != null) {
                if (entry.clickType == 23) {
                    entry.aString9585 = "${TextUtils.setTextColor(16777215)}$username"
                    break
                }
                entry = MiniMenu.aClass482171.next() as? MiniMenuEntry
            }
        }
    }
}

@JvmName("tileHovered")
fun tileHovered(toolkit: AbstractToolkit, mouseX: Int, mouseY: Int) {
    val cameraMatrix = Camera.aClass384158
    if (mouseX < 0 || mouseY < 0 || cameraMatrix == null) {
        return
    }

    val regionBase = Resource.mapRegion?.getBaseCoordinates() ?: return
    val matrix445 = toolkit.method8449()
    toolkit.setModelMatrix(cameraMatrix)
    toolkit.setSunPosition(Camera.aClass294155)
    toolkit.setClipping(MiniMenu.anInt180, MiniMenu.anInt181, MiniMenu.anInt180 + Camera.anInt176, Camera.anInt184 + MiniMenu.anInt181)
    toolkit.setDrawableArea(MiniMenu.anInt180, MiniMenu.anInt181, Camera.anInt176, Camera.anInt184)
    matrix445.fromMatrix(Camera.aClass294155)
    matrix445.multiplyBy(cameraMatrix)
    matrix445.invert()

    val i_3 = mouseX - MiniMenu.anInt180
    val i_4 = mouseY - MiniMenu.anInt181

    if (Resource.mapRegion?.getScene() != null && (!client.interfaceSelectionActive || (USE_OPTIONS_FLAGS and 0x40) != 0)) {
        var x = -1
        var y = -1

        val f_10 = 2.0f * i_3 / Camera.anInt176 - 1.0f
        val f_11 = 2.0f * i_4 / Camera.anInt184 - 1.0f

        matrix445.scale(f_10, f_11, -1.0f, StaticMethods.aFloatArray194)
        val f_12 = StaticMethods.aFloatArray194[0] / StaticMethods.aFloatArray194[3]
        val f_13 = StaticMethods.aFloatArray194[1] / StaticMethods.aFloatArray194[3]
        val f_14 = StaticMethods.aFloatArray194[2] / StaticMethods.aFloatArray194[3]

        matrix445.scale(f_10, f_11, 1.0f, StaticMethods.aFloatArray194)
        val f_15 = StaticMethods.aFloatArray194[0] / StaticMethods.aFloatArray194[3]
        val f_16 = StaticMethods.aFloatArray194[1] / StaticMethods.aFloatArray194[3]
        val f_17 = StaticMethods.aFloatArray194[2] / StaticMethods.aFloatArray194[3]

        val f_18 = MiniMenu.method288(f_12, f_13, f_14, f_15, f_16, f_17, 4)

        if (f_18 > 0.0f) {
            val f_19 = f_15 - f_12
            val f_20 = f_17 - f_14
            val posX = (f_19 * f_18 + f_12).toInt()
            val posY = (f_18 * f_20 + f_14).toInt()
            x = (posX + (PlayerUpdate.MY_PLAYER.getSize() - 1 shl 8)) shr 9
            y = (posY + (PlayerUpdate.MY_PLAYER.getSize() - 1 shl 8)) shr 9
            val plane = PlayerUpdate.MY_PLAYER.plane
            if (plane < 3 && ((Resource.mapRegion?.getSettingsBits()?.tileFunctionMasks?.get(1)?.get(posX shr 9)?.get(posY shr 9)?.toInt() ?: 0) and 0x2) != 0) {
                // Empty block
            }
        }

        if (x != -1 && y != -1) {
            if (client.interfaceSelectionActive && (USE_OPTIONS_FLAGS and 0x40) != 0) {
                val component = Component.listIcomponent(client.anInt56, client.anInt7345)
                if (component != null) {
                    MiniMenu.addEntry(
                        client.useOnType,
                        " -> ",
                        MiniMenu.useOnCursor,
                        59,
                        -1,
                        0L,
                        x,
                        y,
                        true,
                        false,
                        (x or y).toLong(),
                        true
                    )
                } else {
                    InterfaceComponentRenderer.cancelInterfaceSelection()
                }
            } else {
                if (SHOW_FACE_HERE) {
                    MiniMenu.addEntry(
                        LocalizedText.FACE_HERE.translate(client.CURRENT_LANGUAGE)!!,
                        "",
                        -1,
                        60,
                        -1,
                        0L,
                        x,
                        y,
                        true,
                        false,
                        (x or y).toLong(),
                        true
                    )
                }
                MiniMenu.addEntry(
                    InterfaceRenderer.translatedUptext,
                    "",
                    CursorManager.activeCursorId,
                    23,
                    -1,
                    0L,
                    x,
                    y,
                    true,
                    false,
                    (x or y).toLong(),
                    true
                )
            }
        }
    }

    val scene = Resource.mapRegion?.getScene() ?: return
    val interactiveEntities = scene.interactiveEntities
    val renderedEntity = interactiveEntities.entities

    for (reference in renderedEntity) {
        if ((client.playingAltGame || reference.entity?.plane == PlayerUpdate.MY_PLAYER.plane) &&
            reference.hovered(toolkit, mouseX, mouseY)) {

            val entity = reference.entity ?: continue

            val startX: Int
            val startZ: Int

            if (entity is AbstractInteractiveEntity) {
                startX = entity.startX.toInt()
                startZ = entity.startZ.toInt()
            } else {
                val abs = entity.getAbsolutePosition()?.coords ?: continue
                startX = abs.x.toInt() shr 9
                startZ = abs.z.toInt() shr 9
            }

            when (entity) {
                is PlayerEntity -> {
                    val playerSize = entity.getSize()
                    val pAbs = entity.getAbsolutePosition()?.coords ?: continue

                    if (((playerSize and 0x1) == 0 && (pAbs.x.toInt() and 0x1FF) == 0 && (pAbs.z.toInt() and 0x1FF) == 0) ||
                        ((playerSize and 0x1) == 1 && (pAbs.x.toInt() and 0x1FF) == 0x100 && (pAbs.z.toInt() and 0x1FF) == 0x100)) {

                        val plrStartX = pAbs.x.toInt() - (entity.getSize() - 1 shl 8)
                        val plrStartZ = pAbs.z.toInt() - (entity.getSize() - 1 shl 8)

                        for (actionCode in 0 until client.NPC_UPDATE_INDEX) {
                            val npcNode = client.NPC_MAP?.get(client.NPC_UPDATE_INDICES[actionCode].toLong()) as? ObjectNode
                            if (npcNode != null) {
                                val npcEntity = npcNode.value as? NpcEntity
                                val npcDef = npcEntity?.definitions
                                if (npcEntity != null && npcDef != null && npcEntity.clientCycles != client.cycles && npcEntity.isHidden) {
                                    val nAbs = npcEntity.getAbsolutePosition()?.coords ?: continue
                                    val npcAbsX = nAbs.x.toInt() - (npcDef.size - 1 shl 8)
                                    val npcAbsY = nAbs.z.toInt() - (npcDef.size - 1 shl 8)

                                    if (npcAbsX >= plrStartX &&
                                        npcDef.size <= entity.getSize() - (npcAbsX - plrStartX shr 9) &&
                                        npcAbsY >= plrStartZ &&
                                        npcDef.size <= entity.getSize() - (npcAbsY - plrStartZ shr 9)) {
                                        npcHovered(npcEntity, entity.plane != PlayerUpdate.MY_PLAYER.plane)
                                        npcEntity.clientCycles = client.cycles
                                    }
                                }
                            }
                        }

                        val numIndices = PlayerUpdate.NUM_OUT_PLAYER_INDICES
                        val indices = PlayerUpdate.OUT_PLAYER_INDICES

                        for (index in 0 until numIndices) {
                            val playerEntity = PlayerUpdate.players[indices[index]]
                            if (playerEntity != null && playerEntity.clientCycles != client.cycles &&
                                entity != playerEntity && playerEntity.isHidden) {

                                val playerAbsPos = playerEntity.getAbsolutePosition()?.coords ?: continue
                                val playerAbsX = playerAbsPos.x.toInt() - (playerEntity.getSize() - 1 shl 8)
                                val playerAbsZ = playerAbsPos.z.toInt() - (playerEntity.getSize() - 1 shl 8)

                                if (playerAbsX >= plrStartX &&
                                    playerEntity.getSize() <= entity.getSize() - (playerAbsX - plrStartX shr 9) &&
                                    playerAbsZ >= plrStartZ &&
                                    playerEntity.getSize() <= entity.getSize() - (playerAbsZ - plrStartZ shr 9)) {
                                    playerHovered(playerEntity, entity.plane != PlayerUpdate.MY_PLAYER.plane)
                                    playerEntity.clientCycles = client.cycles
                                }
                            }
                        }
                    }

                    if (entity.clientCycles == client.cycles) {
                        continue
                    }
                    playerHovered(entity, entity.plane != PlayerUpdate.MY_PLAYER.plane)
                    entity.clientCycles = client.cycles
                }

                is NpcEntity -> {
                    val entityDef = entity.definitions
                    if (entityDef != null) {
                        val npcAbsPos = entity.getAbsolutePosition()?.coords ?: continue

                        if (((entityDef.size and 0x1) == 0 && (npcAbsPos.x.toInt() and 0x1FF) == 0 && (npcAbsPos.z.toInt() and 0x1FF) == 0) ||
                            ((entityDef.size and 0x1) == 1 && (npcAbsPos.x.toInt() and 0x1FF) == 0x100 && (npcAbsPos.z.toInt() and 0x1FF) == 0x100)) {

                            val npcAbsStartX = npcAbsPos.x.toInt() - (entityDef.size - 1 shl 8)
                            val npcAbsStartZ = npcAbsPos.z.toInt() - (entityDef.size - 1 shl 8)

                            for (npcIndex in 0 until client.NPC_UPDATE_INDEX) {
                                val npcNode = client.NPC_MAP?.get(client.NPC_UPDATE_INDICES[npcIndex].toLong()) as? ObjectNode
                                if (npcNode != null) {
                                    val npc = npcNode.value as? NpcEntity
                                    val npcDef2 = npc?.definitions
                                    if (npc != null && npcDef2 != null && npc.clientCycles != client.cycles && entity != npc && npc.isHidden) {
                                        val vector337 = npc.getAbsolutePosition()?.coords ?: continue
                                        val i_12 = vector337.x.toInt() - (npcDef2.size - 1 shl 8)
                                        val i_13 = vector337.z.toInt() - (npcDef2.size - 1 shl 8)

                                        if (i_12 >= npcAbsStartX &&
                                            npcDef2.size <= entityDef.size - (i_12 - npcAbsStartX shr 9) &&
                                            i_13 >= npcAbsStartZ &&
                                            npcDef2.size <= entityDef.size - (i_13 - npcAbsStartZ shr 9)) {
                                            npcHovered(npc, entity.plane != PlayerUpdate.MY_PLAYER.plane)
                                            npc.clientCycles = client.cycles
                                        }
                                    }
                                }
                            }

                            val numIndices = PlayerUpdate.NUM_OUT_PLAYER_INDICES
                            val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES

                            for (index in 0 until numIndices) {
                                val player = PlayerUpdate.players[playerIndices[index]]
                                if (player != null && player.clientCycles != client.cycles && player.isHidden) {
                                    val abs = player.getAbsolutePosition()?.coords ?: continue
                                    val posX2 = abs.x.toInt() - (player.getSize() - 1 shl 8)
                                    val posZ2 = abs.z.toInt() - (player.getSize() - 1 shl 8)

                                    if (posX2 >= npcAbsStartX &&
                                        player.getSize() <= entityDef.size - (posX2 - npcAbsStartX shr 9) &&
                                        posZ2 >= npcAbsStartZ &&
                                        player.getSize() <= entityDef.size - (posZ2 - npcAbsStartZ shr 9)) {
                                        playerHovered(player, entity.plane != PlayerUpdate.MY_PLAYER.plane)
                                        player.clientCycles = client.cycles
                                    }
                                }
                            }
                        }

                        if (entity.clientCycles == client.cycles) {
                            continue
                        }
                        npcHovered(entity, entity.plane != PlayerUpdate.MY_PLAYER.plane)
                        entity.clientCycles = client.cycles
                    }
                }

                is GroundItem -> {
                    val posX = startX + regionBase.x
                    val posY = startZ + regionBase.y
                    val groundItems = client.GROUND_ITEMS?.get(
                        (entity.plane.toLong() shl 28) or (posY.toLong() shl 14) or posX.toLong()
                    ) as? GroundItemWrapper

                    if (groundItems != null) {
                        var actionIndex2 = 0
                        var groundItem = groundItems.groundItems.first() as? GroundItemNode

                        while (groundItem != null) {
                            val objtype = TypeLists.OBJ?.list(groundItem.id) ?: break
                            val color = when {
                                objtype.hasTooltipColor -> objtype.tooltipColor
                                objtype.membersOnly -> TypeLists.INTERACTION_DEFAULTS?.tooltipColorP2p ?: 0
                                else -> TypeLists.INTERACTION_DEFAULTS?.tooltipColorF2p ?: 0
                            }

                            if (client.interfaceSelectionActive && entity.plane == PlayerUpdate.MY_PLAYER.plane) {
                                val param = if (MiniMenu.USE_INTERFACE_ID != -1) {
                                    TypeLists.PARAMS?.list(MiniMenu.USE_INTERFACE_ID)
                                } else null

                                if ((USE_OPTIONS_FLAGS and 0x1) != 0 &&
                                    (param == null || objtype.getCs2Integer(MiniMenu.USE_INTERFACE_ID, param.defaultInt) != param.defaultInt)) {
                                    MiniMenu.addEntry(
                                        client.useOnType,
                                        "${client.useOnTarget} -> ${TextUtils.setTextColor(color)}${objtype.name}",
                                        MiniMenu.useOnCursor,
                                        17,
                                        -1,
                                        groundItem.id.toLong(),
                                        startX,
                                        startZ,
                                        true,
                                        false,
                                        actionIndex2.toLong(),
                                        false
                                    )
                                }
                            }

                            if (entity.plane == PlayerUpdate.MY_PLAYER.plane) {
                                val groundActions = objtype.groundActions
                                for (groundActionIndex in groundActions.indices.reversed()) {
                                    groundActions[groundActionIndex]?.let { action ->
                                        val clickType = when (groundActionIndex) {
                                            0 -> 18
                                            1 -> 19
                                            2 -> 20
                                            3 -> 21
                                            4 -> 22
                                            5 -> 1004
                                            else -> 0
                                        }

                                        val cursor = when (groundActionIndex) {
                                            objtype.primaryCursorActionIndex -> objtype.primaryCursor
                                            objtype.secondaryCursorActionIndex -> objtype.secondaryCursor
                                            else -> CursorManager.defaultAttackCursor
                                        }

                                        MiniMenu.addEntry(
                                            action,
                                            "${TextUtils.setTextColor(color)}${objtype.name}",
                                            cursor,
                                            clickType,
                                            -1,
                                            groundItem.id.toLong(),
                                            startX,
                                            startZ,
                                            true,
                                            false,
                                            actionIndex2.toLong(),
                                            false
                                        )
                                    }
                                }
                            }

                            groundItem = groundItems.groundItems.getNext() as? GroundItemNode
                            actionIndex2++
                        }
                    }
                }

                is GameObject -> {
                    var loctype: ObjectType? = Resource.mapRegion?.getLoctypeList()?.list(entity.id)
                    if (loctype?.alternativeIds != null) {
                        loctype = loctype.getMultiLoc(VarProviders.PLAYER_VAR_PROVIDER)
                    }

                    if (loctype == null) {
                        continue
                    }

                    if (client.interfaceSelectionActive && entity.plane == PlayerUpdate.MY_PLAYER.plane) {
                        val attributedefault62 = if (MiniMenu.USE_INTERFACE_ID != -1) {
                            TypeLists.PARAMS?.list(MiniMenu.USE_INTERFACE_ID)
                        } else null

                        if ((USE_OPTIONS_FLAGS and 0x4) != 0 &&
                            (attributedefault62 == null || loctype.getParamsInt(MiniMenu.USE_INTERFACE_ID, attributedefault62.defaultInt) != attributedefault62.defaultInt)) {
                            MiniMenu.addEntry(
                                client.useOnType,
                                "${client.useOnTarget} -> ${TextUtils.setTextColor(65535)}${loctype.name}",
                                MiniMenu.useOnCursor,
                                2,
                                -1,
                                MapLoader.calculateLocUid(entity, startX, startZ),
                                startX,
                                startZ,
                                true,
                                false,
                                entity.hashCode().toLong(),
                                false
                            )
                        }
                    }

                    if (entity.plane != PlayerUpdate.MY_PLAYER.plane) {
                        continue
                    }

                    val actions = loctype.actions ?: continue

                    for (actionIndex in actions.indices.reversed()) {
                        actions[actionIndex]?.let { action ->
                            val clickType = when (actionIndex) {
                                0 -> 3
                                1 -> 4
                                2 -> 5
                                3 -> 6
                                4 -> 1001
                                5 -> 1002
                                else -> 0
                            }

                            val cursor = when (actionIndex) {
                                loctype.primaryCursorActionIndex -> loctype.primaryCursor
                                loctype.secondaryCursorActionIndex -> loctype.secondaryCursor
                                else -> CursorManager.defaultAttackCursor
                            }

                            MiniMenu.addEntry(
                                action,
                                "${TextUtils.setTextColor(65535)}${loctype.name}",
                                cursor,
                                clickType,
                                -1,
                                MapLoader.calculateLocUid(entity, startX, startZ),
                                startX,
                                startZ,
                                true,
                                false,
                                entity.hashCode().toLong(),
                                false
                            )
                        }
                    }
                }
            }
        }
    }
}

@JvmName("interfaceComponentHovered")
fun interfaceComponentHovered(component: Component) {
    if (client.interfaceSelectionActive) {
        val param = if (MiniMenu.USE_INTERFACE_ID != -1) {
            TypeLists.PARAMS?.list(MiniMenu.USE_INTERFACE_ID)
        } else null

        if (client.getComponentSettings(component).isTargetableByUse() &&
            (USE_OPTIONS_FLAGS and 0x20) != 0 &&
            (param == null || component.method1997(MiniMenu.USE_INTERFACE_ID, param.defaultInt) != param.defaultInt)) {
            MiniMenu.addEntry(
                client.useOnType,
                "${client.useOnTarget} -> ${component.opBase}",
                MiniMenu.useOnCursor,
                58,
                component.containerItemId,
                0L,
                component.slotId,
                component.idHash,
                true,
                false,
                (component.slotId or component.idHash).toLong(),
                false
            )
        }
    }

    for (option in 9 downTo 5) {
        val action = Component.componentMenuOption(component, option)
        if (action != null) {
            MiniMenu.addEntry(
                action,
                component.opBase,
                Component.getCursor(component, option),
                1007,
                component.containerItemId,
                (option + 1).toLong(),
                component.slotId,
                component.idHash,
                true,
                false,
                (component.slotId or component.idHash).toLong(),
                false
            )
        }
    }

    val action = Interface.getUseOptionString(component)
    if (action != null) {
        MiniMenu.addEntry(
            action,
            component.opBase,
            component.targetOpCursor,
            25,
            component.containerItemId,
            0L,
            component.slotId,
            component.idHash,
            true,
            false,
            (component.slotId or component.idHash).toLong(),
            false
        )
    }

    for (option in 4 downTo 0) {
        val menuAction = Component.componentMenuOption(component, option)
        if (menuAction != null) {
            MiniMenu.addEntry(
                menuAction,
                component.opBase,
                Component.getCursor(component, option),
                57,
                component.containerItemId,
                (option + 1).toLong(),
                component.slotId,
                component.idHash,
                true,
                false,
                (component.slotId or component.idHash).toLong(),
                false
            )
        }
    }

    if (client.getComponentSettings(component).continueOptionEnabled()) {
        val text = component.resumeText ?: LocalizedText.CONTINUE.translate(client.CURRENT_LANGUAGE)!!
        MiniMenu.addEntry(
            text!!,
            "",
            -1,
            30,
            component.containerItemId,
            0L,
            component.slotId,
            component.idHash,
            true,
            false,
            (component.slotId or component.idHash).toLong(),
            false
        )
    }
}

@JvmName("npcHovered")
fun npcHovered(npc: NpcEntity, samePlaneAsPlayer: Boolean) {
    if (MiniMenu.entryCount >= 412) return

    var npcType: NPCType? = npc.definitions
    var name = npc.modifiedName

    if (npcType?.alternativeIds != null) {
        npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER) ?: return
        name = npcType.name
    }

    if (npcType?.visible == true) {
        if (npc.modifiedCbLvl != 0) {
            val level = if (client.CURRENT_GAME == Game.STELLAR_DAWN) {
                LocalizedText.RATING_.translate(client.CURRENT_LANGUAGE)
            } else {
                LocalizedText.MINIMENU_LEVEL.translate(client.CURRENT_LANGUAGE)
            }
            name = "$name${TextUtils.calculateCombatBasedColor(npc.modifiedCbLvl, PlayerUpdate.MY_PLAYER.combatLevel)} ($level${npc.modifiedCbLvl})"
        }

        if (client.interfaceSelectionActive && !samePlaneAsPlayer) {
            val paramtype = if (MiniMenu.USE_INTERFACE_ID != -1) {
                TypeLists.PARAMS?.list(MiniMenu.USE_INTERFACE_ID)
            } else null

            if ((USE_OPTIONS_FLAGS and 0x2) != 0 &&
                (paramtype == null || npcType.getCs2Value(MiniMenu.USE_INTERFACE_ID, paramtype.defaultInt) != paramtype.defaultInt)) {
                MiniMenu.addEntry(
                    client.useOnType,
                    "${client.useOnTarget} -> ${TextUtils.setTextColor(16776960)}$name",
                    MiniMenu.useOnCursor,
                    8,
                    -1,
                    npc.playerIndex.toLong(),
                    0,
                    0,
                    true,
                    false,
                    npc.playerIndex.toLong(),
                    false
                )
            }
        }

        if (!samePlaneAsPlayer) {
            var npcOptions = npcType.actions
            if (client.prefixMinimenuOps) {
                npcOptions = MiniMenu.prefixWithIndex(npcOptions)
            }

            if (npcOptions != null) {
                for (actionIndex in npcOptions.indices.reversed()) {
                    val option = npcOptions[actionIndex]
                    if (option != null &&
                        (npcType.gameType.toInt() == 0 ||
                         (!option.equals(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE), ignoreCase = true) &&
                          !option.equals(LocalizedText.EXAMINE.translate(client.CURRENT_LANGUAGE), ignoreCase = true)))) {

                        val clickType = when (actionIndex) {
                            0 -> 9
                            1 -> 10
                            2 -> 11
                            3 -> 12
                            4 -> 13
                            5 -> 1003
                            else -> 0
                        }

                        val cursor = when (actionIndex) {
                            npcType.primaryCursorActionIndex -> npcType.primaryCursor
                            npcType.secondaryCursorActionIndex -> npcType.secondaryCursor
                            else -> CursorManager.defaultAttackCursor
                        }

                        val actualCursor = if (option.equals(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE), ignoreCase = true)) {
                            npcType.attackCursor
                        } else {
                            cursor
                        }

                        MiniMenu.addEntry(
                            option,
                            "${TextUtils.setTextColor(16776960)}$name",
                            actualCursor,
                            clickType,
                            -1,
                            npc.playerIndex.toLong(),
                            0,
                            0,
                            true,
                            false,
                            npc.playerIndex.toLong(),
                            false
                        )
                    }
                }

                if (npcType.gameType.toInt() == 1) {
                    for (actionIndex in npcOptions.indices) {
                        val option = npcOptions[actionIndex]
                        if (option != null &&
                            (option.equals(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE), ignoreCase = true) ||
                             option.equals(LocalizedText.EXAMINE.translate(client.CURRENT_LANGUAGE), ignoreCase = true))) {

                            val attackClickPriority = if (npc.modifiedCbLvl > PlayerUpdate.MY_PLAYER.combatLevel) 2000 else 0

                            var clickType = when (actionIndex) {
                                0 -> 9
                                1 -> 10
                                2 -> 11
                                3 -> 12
                                4 -> 13
                                5 -> 1003
                                else -> 0
                            }

                            if (clickType != 0) {
                                clickType += attackClickPriority
                            }

                            val cursor = when (actionIndex) {
                                npcType.primaryCursorActionIndex -> npcType.primaryCursor
                                npcType.secondaryCursorActionIndex -> npcType.secondaryCursor
                                else -> CursorManager.defaultAttackCursor
                            }

                            val actualCursor = if (option.equals(LocalizedText.ATTACK.translate(client.CURRENT_LANGUAGE), ignoreCase = true)) {
                                npcType.attackCursor
                            } else {
                                cursor
                            }

                            MiniMenu.addEntry(
                                option,
                                "${TextUtils.setTextColor(16776960)}$name",
                                actualCursor,
                                clickType,
                                -1,
                                npc.playerIndex.toLong(),
                                0,
                                0,
                                true,
                                false,
                                npc.playerIndex.toLong(),
                                false
                            )
                        }
                    }
                }
            }
        }
    }
}
