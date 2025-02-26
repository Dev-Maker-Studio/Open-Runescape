package com.jagex.game.runetek5.graphics.hint;

import java.util.Deque;
import java.util.LinkedList;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.hint.HintTrailComparable;
import com.jagex.game.runetek5.entity.hint.HintTrailGraphNode;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class HintTrail {

    public static int trailPreferenceFlags;

    public Deque<HintTrailGraphNode> hintTrialGraphNodes;

    public final int modelId;

    public final int length;

    public final int[] placementX;

    public final int[] placementY;

    public HintTrail(AbstractToolkit toolkit, Packet buffer, int modelId) {
        this.modelId = modelId;
        this.length = buffer.readSmart();
        this.placementX = new int[this.length];
        this.placementY = new int[this.length];
        int offsetX = buffer.readUShort();
        int offsetY = buffer.readUShort();
        for (int index = 0; index < this.length; index++) {
        	this.placementX[index] = offsetX + buffer.readByte();
        	this.placementY[index] = offsetY + buffer.readByte();
        }
        getStationaryModel(toolkit);
    }

    public Model getStationaryModel(AbstractToolkit toolkit) {
        Mesh mesh = Mesh.fromResource(Resource.MODEL, this.modelId);
        if (mesh == null) {
            return null;
        } 
        if (mesh.version < 13) {
            mesh.upscale();
        }
        return toolkit.createModel(mesh, HAS_TEXTURE_COORDS, trailPreferenceFlags, 64, 768);
    }

    public void addTo(Scene scene) {
        if (scene != null) {
            if (this.length > 0) {
                buildList(scene);
                for (HintTrailGraphNode hintTrialNode : this.hintTrialGraphNodes) {
                    scene.addInteractive(hintTrialNode, false);
                }
            }
        }
    }

    public void removeFromScene(Scene scene) {
        if (scene != null && this.hintTrialGraphNodes != null) {
            for (HintTrailGraphNode trailGraphNode : this.hintTrialGraphNodes) {
                scene.removeInteractiveOnCondition(trailGraphNode.plane, trailGraphNode.startX, trailGraphNode.startZ, new HintTrailComparable(trailGraphNode));
            }
        }
    }

    public void buildList(Scene scene) {
    	this.hintTrialGraphNodes = new LinkedList<>();
        SettingsBits settingsBits = Resource.mapRegion.getSettingsBits();
        CoordGrid base = Resource.mapRegion.getBaseCoordinates();
        CoordGrid previous = new CoordGrid(PlayerUpdate.MY_PLAYER_PLANE, this.placementX[0], this.placementY[0]);
        for (int index = 1; index < this.length; index++) {
            CoordGrid current = new CoordGrid(PlayerUpdate.MY_PLAYER_PLANE, this.placementX[index], this.placementY[index]);
            while (previous.x != current.x || previous.y != current.y) {
                if (previous.x < current.x) {
                    previous.x++;
                } else if (previous.x > current.x) {
                    previous.x--;
                }
                if (previous.y < current.y) {
                    previous.y++;
                } else if (previous.y > current.y) {
                    previous.y--;
                }
                int plane = PlayerUpdate.MY_PLAYER_PLANE;
                int x = previous.x - base.x;
                int y = previous.y - base.y;
                if (x >= 0 && x < scene.sceneDimensionX && y >= 0 && y < scene.sceneDimensionY) {
                    int absX = (x << 9) + 256;
                    int absY = (y << 9) + 256;
                    if (settingsBits.areRoofsHidden(x, y)) {
                        plane++;
                    }
                    this.hintTrialGraphNodes.add(new HintTrailGraphNode(scene, this, PlayerUpdate.MY_PLAYER_PLANE, plane, absX, MapRegion.tileheightForAbsolutePosition(absX, absY, PlayerUpdate.MY_PLAYER_PLANE), absY));
                }
            }
            previous = current;
        }
    }

	public static void resetCache(int preferenceFlags) {
	    trailPreferenceFlags = preferenceFlags;
	}
}
