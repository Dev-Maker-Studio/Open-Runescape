package com.jagex.game.runetek5.config.cutscene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.cutscene.action.*
import com.jagex.game.runetek5.network.packet.AbstractMousePacket
import com.jagex.game.runetek5.staticmethods.StaticMethods

object CutsceneTypeList {

    @JvmField
    var activeCutsceneLoadingstage: CutsceneLoadingStage? = null

    @JvmField
    var activeCutsceneId: Int = -1

    @JvmField
    val CUTSCENE_AREAS = NodeCollection()

    @JvmField
    var anInt825: Int = 0

    @JvmField
    var CUTSCENE_BUFFER: Packet? = null

    @JvmField
    var CUTSCENE_ENTITIES: Array<CutsceneEntity>? = null

    @JvmField
    var CUTSCENE_CAMERA_MOVEMENTS: Array<CutsceneCameraMovement>? = null

    @JvmField
    var CUTSCENE_OBJECTS: Array<CutsceneObject>? = null

    @JvmField
    var CUTSCENE_ENTITY_MOVEMENTS: Array<CutsceneEntityMovement>? = null

    @JvmField
    var CUTSCENE_MISC_ACTIONS: Array<CutsceneAction>? = null

    @JvmField
    val CONFIG_CUTSCENES: IterableNodeMap<IntNode> = IterableNodeMap(32)

    @JvmStatic
    fun isFullyCached(cutsceneId: Int): Boolean {
        if (cutsceneId != activeCutsceneId || activeCutsceneLoadingstage == null) {
            resetCutsceneConfigs()
            activeCutsceneLoadingstage = CutsceneLoadingStage.LOADING_DEFINITIONS
            activeCutsceneId = cutsceneId
        }
        if (activeCutsceneLoadingstage == CutsceneLoadingStage.LOADING_DEFINITIONS) {
            val data = Resource.CUTSCENE!!.getFile(cutsceneId)
            if (data == null) {
                return false
            }
            val buffer = Packet(data)
            readValueLoop(buffer)
            val numAreas = buffer.readUByte()
            for (index in 0 until numAreas) {
                CUTSCENE_AREAS.addFirst(CutsceneArea(buffer))
            }
            val numCameraMovements = buffer.readUSmart()
            CUTSCENE_CAMERA_MOVEMENTS = Array(numCameraMovements) { index ->
                CutsceneCameraMovement(buffer)
            }
            val numEntities = buffer.readUSmart()
            CUTSCENE_ENTITIES = Array(numEntities) { index ->
                CutsceneEntity(buffer, index)
            }
            val numObjects = buffer.readUSmart()
            CUTSCENE_OBJECTS = Array(numObjects) { index ->
                CutsceneObject(buffer)
            }
            val numEntityMovements = buffer.readUSmart()
            CUTSCENE_ENTITY_MOVEMENTS = Array(numEntityMovements) { index ->
                CutsceneEntityMovement(buffer)
            }
            val numCutsceneActions = buffer.readUSmart()
            CUTSCENE_MISC_ACTIONS = Array(numCutsceneActions) { index ->
                listCutsceneAction(buffer)
            }
            activeCutsceneLoadingstage = CutsceneLoadingStage.CACHING_ASSETS
        }
        if (CutsceneLoadingStage.CACHING_ASSETS == activeCutsceneLoadingstage) {
            var fullyCached = true
            val entities = CUTSCENE_ENTITIES
            for (index in entities!!.indices) {
                val entity = entities[index]
                if (!entity.isFullyCached()) {
                    fullyCached = false
                }
            }
            val actions = CUTSCENE_MISC_ACTIONS
            for (index in actions!!.indices) {
                val action = actions[index]
                if (!action.isFullyCached()) {
                    fullyCached = false
                }
            }
            val objects = CUTSCENE_OBJECTS
            for (index in objects!!.indices) {
                val obj = objects[index]
                if (!obj.isFullyCached()) {
                    fullyCached = false
                }
            }
            if (!fullyCached) {
                return false
            }
            activeCutsceneLoadingstage = CutsceneLoadingStage.FINISHED
        }
        return true
    }

    @JvmStatic
    fun listCutsceneAction(buffer: Packet): CutsceneAction {
        val id = buffer.readUByte()
        val type = getActionType(id)
        var action: CutsceneAction? = null
        when (type!!.action) {
            0 -> action = CutsceneDestroyObject(buffer)
            1 -> action = CutsceneResetEntity(buffer)
            2 -> action = CutsceneApplyHitmark(buffer)
            3, 7, 21 -> {}
            4 -> action = CutsceneMoveEntity(buffer)
            5 -> action = CutscenePlayJingle(buffer)
            6 -> action = CutscenePlayVorbis(buffer)
            8 -> action = CutsceneUncenteredCameraMovement(buffer)
            9 -> action = CutsceneBasicMovement(buffer)
            10 -> action = CutsceneSetVariable(buffer, false)
            11 -> action = CutsceneDirectCameraTransition(buffer)
            12 -> action = CutsceneSetHintDetails(buffer)
            13 -> action = CutsceneAnimateObject(buffer)
            14 -> action = CutscenePositionedGfx(buffer)
            15 -> action = CutsceneProjectile(buffer, 0, 0)
            16 -> action = CutsceneFinished(buffer)
            17 -> action = CutsceneTileMessage(buffer)
            18 -> action = CutscenePlaySong(buffer)
            19 -> action = CutsceneProjectile(buffer, 1, 0)
            20 -> action = CutsceneReplaceObject(buffer)
            22 -> action = CutsceneExecuteScript(buffer)
            23 -> action = CutscenePlaySynth(buffer)
            24 -> action = CutsceneProjectile(buffer, 0, 1)
            25 -> action = CutsceneEntitySpotAnimation(buffer)
            26 -> action = CutsceneAnimatedMovement(buffer)
            27 -> action = CutsceneSetVariable(buffer, true)
            28 -> action = CutsceneProjectile(buffer, 1, 1)
            29 -> action = CutsceneRotateEntity(buffer)
            30 -> action = CutsceneFadeScreen(buffer)
        }
        return action!!
    }

    @JvmStatic
    fun readValueLoop(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            when (opcode) {
                0 -> {
                    anInt825 = buffer.readUShort()
                    StaticMethods.anInt824 = buffer.readUShort()
                }
                255 -> return
            }
        }
    }

    @JvmStatic
    fun getActionType(id: Int): CutsceneActionType? {
        val actionTypes = CutsceneActionType.values()
        for (index in actionTypes.indices) {
            val actionType = actionTypes[index]
            if (id == actionType.id) {
                return actionType
            }
        }
        return null
    }

    @JvmStatic
    fun resetCutsceneConfigs() {
        CONFIG_CUTSCENES.clear()
        CUTSCENE_AREAS.remove_all()
        CUTSCENE_CAMERA_MOVEMENTS = null
        CUTSCENE_ENTITIES = null
        CUTSCENE_OBJECTS = null
        CUTSCENE_ENTITY_MOVEMENTS = null
        CUTSCENE_MISC_ACTIONS = null
        CutsceneEntity.drawPriority = 1
        anInt825 = 0
        StaticMethods.anInt824 = 0
        activeCutsceneLoadingstage = null
        activeCutsceneId = -1
        if (StaticMethods.aBool826) {
            Client.aShort7214 = StaticMethods.aShort828
            Client.aShort7474 = StaticMethods.aShort829
            Client.aShort7276 = AbstractMousePacket.aShort7079
            Client.aShort7441 = StaticMethods.aShort830
            StaticMethods.aBool826 = false
        }
    }
}
