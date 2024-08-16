package com.jagex.game.runetek5.config.cutscene;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneApplyHitmark;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneEntitySpotAnimation;
import com.jagex.game.runetek5.config.cutscene.action.CutscenePositionedGfx;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneDirectCameraTransition;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneExecuteScript;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneSetVariable;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneAnimatedMovement;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneResetEntity;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneFadeScreen;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneReplaceObject;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneSetHintDetails;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneMoveEntity;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneDestroyObject;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneAnimateObject;
import com.jagex.game.runetek5.config.cutscene.action.CutscenePlaySong;
import com.jagex.game.runetek5.config.cutscene.action.CutscenePlayJingle;
import com.jagex.game.runetek5.config.cutscene.action.CutscenePlayVorbis;
import com.jagex.game.runetek5.config.cutscene.action.CutscenePlaySynth;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneProjectile;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneRotateEntity;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneFinished;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneTileMessage;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneUncenteredCameraMovement;
import com.jagex.game.runetek5.network.packet.AbstractMousePacket;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public class CutsceneTypeList {
	
	public static CutsceneLoadingStage activeCutsceneLoadingstage;
	public static int activeCutsceneId = -1;
	public static final NodeCollection CUTSCENE_AREAS = new NodeCollection();
	public static int anInt825;
	public static Packet CUTSCENE_BUFFER;
	public static CutsceneEntity[] CUTSCENE_ENTITIES;
	public static CutsceneCameraMovement[] CUTSCENE_CAMERA_MOVEMENTS;
	public static CutsceneObject[] CUTSCENE_OBJECTS;
	public static CutsceneEntityMovement[] CUTSCENE_ENTITY_MOVEMENTS;
	public static CutsceneAction[] CUTSCENE_MISC_ACTIONS;
	public static final IterableNodeMap<IntNode> CONFIG_CUTSCENES = new IterableNodeMap<IntNode>(32);

    public static boolean isFullyCached(int cutsceneId) {
        if (cutsceneId != CutsceneTypeList.activeCutsceneId || CutsceneTypeList.activeCutsceneLoadingstage == null) {
            CutsceneTypeList.resetCutsceneConfigs();
            CutsceneTypeList.activeCutsceneLoadingstage = CutsceneLoadingStage.LOADING_DEFINITIONS;
            CutsceneTypeList.activeCutsceneId = cutsceneId;
        }
        if (CutsceneTypeList.activeCutsceneLoadingstage == CutsceneLoadingStage.LOADING_DEFINITIONS) {
            byte[] data = Resource.CUTSCENE.getFile(cutsceneId);
            if (data == null) {
                return false;
            }
            Packet buffer = new Packet(data);
            readValueLoop(buffer);
            int numAreas = buffer.readUnsignedByte();
            for (int index = 0; index < numAreas; index++) {
                CutsceneTypeList.CUTSCENE_AREAS.addFirst(new CutsceneArea(buffer));
            }
            int numCameraMovements = buffer.readUnsignedSmart();
            CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS = new CutsceneCameraMovement[numCameraMovements];
            for (int index = 0; index < numCameraMovements; index++) {
                CutsceneTypeList.CUTSCENE_CAMERA_MOVEMENTS[index] = new CutsceneCameraMovement(buffer);
            }
            int numEntities = buffer.readUnsignedSmart();
            CutsceneTypeList.CUTSCENE_ENTITIES = new CutsceneEntity[numEntities];
            
            for (int index = 0; index < numEntities; index++) {
                CutsceneTypeList.CUTSCENE_ENTITIES[index] = new CutsceneEntity(buffer, index);
            }
            int numObjects = buffer.readUnsignedSmart();
            CutsceneTypeList.CUTSCENE_OBJECTS = new CutsceneObject[numObjects];
            for (int index = 0; index < numObjects; index++) {
                CutsceneTypeList.CUTSCENE_OBJECTS[index] = new CutsceneObject(buffer);
            }
            int numEntityMovements = buffer.readUnsignedSmart();
            CutsceneTypeList.CUTSCENE_ENTITY_MOVEMENTS = new CutsceneEntityMovement[numEntityMovements];
            for (int index = 0; index < numEntityMovements; index++) {
                CutsceneTypeList.CUTSCENE_ENTITY_MOVEMENTS[index] = new CutsceneEntityMovement(buffer);
            }
            int numCutsceneActions = buffer.readUnsignedSmart();
            CutsceneTypeList.CUTSCENE_MISC_ACTIONS = new CutsceneAction[numCutsceneActions];
            for (int index = 0; index < numCutsceneActions; index++) {
                CutsceneTypeList.CUTSCENE_MISC_ACTIONS[index] = listCutsceneAction(buffer);
            }
            CutsceneTypeList.activeCutsceneLoadingstage = CutsceneLoadingStage.CACHING_ASSETS;
        }
        if (CutsceneLoadingStage.CACHING_ASSETS == CutsceneTypeList.activeCutsceneLoadingstage) {
            boolean fullyCached = true;
            CutsceneEntity[] entities = CutsceneTypeList.CUTSCENE_ENTITIES;
            for (int index = 0; index < entities.length; index++) {
                CutsceneEntity entity = entities[index];
                if (!entity.isFullyCached()) {
                    fullyCached = false;
                }
            }
            CutsceneAction[] actions = CutsceneTypeList.CUTSCENE_MISC_ACTIONS;
            for (int index = 0; index < actions.length; index++) {
                CutsceneAction action = actions[index];
                if (!action.isFullyCached()) {
                    fullyCached = false;
                }
            }
            CutsceneObject[] objects = CutsceneTypeList.CUTSCENE_OBJECTS;
            for (int index = 0; index < objects.length; index++) {
                CutsceneObject object = objects[index];
                if (!object.isFullyCached()) {
                    fullyCached = false;
                }
            }
            if (!fullyCached) {
                return false;
            }
            CutsceneTypeList.activeCutsceneLoadingstage = CutsceneLoadingStage.FINISHED;
        }
        return true;
    }

    public static CutsceneAction listCutsceneAction(Packet buffer) {
        int id = buffer.readUnsignedByte();
        CutsceneActionType type = getActionType(id);
        CutsceneAction action = null;
        assert type != null;
        switch (type.action) {
            case 0:
                action = new CutsceneDestroyObject(buffer);
                break;
            case 1:
                action = new CutsceneResetEntity(buffer);
                break;
            case 2:
                action = new CutsceneApplyHitmark(buffer);
            case 3:
            case 7:
            case 21:
            default:
                break;
            case 4:
                action = new CutsceneMoveEntity(buffer);
                break;
            case 5:
                action = new CutscenePlayJingle(buffer);
                break;
            case 6:
                action = new CutscenePlayVorbis(buffer);
                break;
            case 8:
                action = new CutsceneUncenteredCameraMovement(buffer);
                break;
            case 9:
                action = new CutsceneBasicMovement(buffer);
                break;
            case 10:
                action = new CutsceneSetVariable(buffer, false);
                break;
            case 11:
                action = new CutsceneDirectCameraTransition(buffer);
                break;
            case 12:
                action = new CutsceneSetHintDetails(buffer);
                break;
            case 13:
                action = new CutsceneAnimateObject(buffer);
                break;
            case 14:
                action = new CutscenePositionedGfx(buffer);
                break;
            case 15:
                action = new CutsceneProjectile(buffer, 0, 0);
                break;
            case 16:
                action = new CutsceneFinished(buffer);
                break;
            case 17:
                action = new CutsceneTileMessage(buffer);
                break;
            case 18:
                action = new CutscenePlaySong(buffer);
                break;
            case 19:
                action = new CutsceneProjectile(buffer, 1, 0);
                break;
            case 20:
                action = new CutsceneReplaceObject(buffer);
                break;
            case 22:
                action = new CutsceneExecuteScript(buffer);
                break;
            case 23:
                action = new CutscenePlaySynth(buffer);
                break;
            case 24:
                action = new CutsceneProjectile(buffer, 0, 1);
                break;
            case 25:
                action = new CutsceneEntitySpotAnimation(buffer);
                break;
            case 26:
                action = new CutsceneAnimatedMovement(buffer);
                break;
            case 27:
                action = new CutsceneSetVariable(buffer, true);
                break;
            case 28:
                action = new CutsceneProjectile(buffer, 1, 1);
                break;
            case 29:
                action = new CutsceneRotateEntity(buffer);
                break;
            case 30:
                action = new CutsceneFadeScreen(buffer);
        }
        return action;
    }

    public static void readValueLoop(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            switch (opcode) {
                case 0:
                    CutsceneTypeList.anInt825 = buffer.readUnsignedShort();
                    StaticMethods.anInt824 = buffer.readUnsignedShort();
                    break;
                case 255:
                    return;
            }
        }
    }

    public static CutsceneActionType getActionType(int id) {
        CutsceneActionType[] actionTypes = CutsceneActionType.values();
        for (int index = 0; index < actionTypes.length; index++) {
            CutsceneActionType actionType = actionTypes[index];
            if (id == actionType.id) {
                return actionType;
            }
        }
        return null;
    }
    
	public static void resetCutsceneConfigs() {
	    CONFIG_CUTSCENES.clear();
	    CUTSCENE_AREAS.remove_all();
	    CUTSCENE_CAMERA_MOVEMENTS = null;
	    CUTSCENE_ENTITIES = null;
	    CUTSCENE_OBJECTS = null;
	    CUTSCENE_ENTITY_MOVEMENTS = null;
	    CUTSCENE_MISC_ACTIONS = null;
	    CutsceneEntity.drawPriority = 1;
	    anInt825 = 0;
	    StaticMethods.anInt824 = 0;
	    activeCutsceneLoadingstage = null;
	    activeCutsceneId = -1;
	    if (StaticMethods.aBool826) {
	        client.aShort7214 = StaticMethods.aShort828;
	        client.aShort7474 = StaticMethods.aShort829;
	        client.aShort7276 = AbstractMousePacket.aShort7079;
	        client.aShort7441 = StaticMethods.aShort830;
	        StaticMethods.aBool826 = false;
	    }
	}
}
