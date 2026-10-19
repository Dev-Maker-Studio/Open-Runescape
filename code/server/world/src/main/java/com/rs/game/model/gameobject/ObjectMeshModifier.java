package com.rs.game.model.gameobject;

import com.rs.utils.KtExtensionsKt;
import org.darkan.core.Logger;
import org.darkan.core.model.ObjectShape;
import world.gregs.voidps.cache.definition.data.ObjectDefinition;

import java.util.Arrays;

public class ObjectMeshModifier {

	public static final ObjectMeshModifier RESET = new ObjectMeshModifier();

	private transient GameObject object;
	private int slot = -1;
	private int[] modelIds;
	private int[] modifiedColors;
	private int[] modifiedTextures;

	private ObjectMeshModifier() {

	}

	public ObjectMeshModifier(GameObject object) {
		this.object = object;
		ObjectDefinition defs = object.getDefinitions();
		ObjectShape[] types = KtExtensionsKt.getTypes(defs);
		if (types != null) {
            for (int i = 0; i < types.length; i++) {
                if (object.getType() == types[i]) {
                	slot = i;
                    break;
                }
            }
        }
		if (slot == -1)
			Logger.error(ObjectMeshModifier.class, "constructor()", "Object " + object + " has no slot type.");
	}

	private int[][] getDefModelIds() {
		ObjectDefinition defs = object.getDefinitions();
		return defs.getModelIds();
	}

	private short[] getDefModifiedColours() {
		return object.getDefinitions().getModifiedColours();
	}

	private short[] getDefModifiedTextureColours() {
		return object.getDefinitions().getModifiedTextureColours();
	}

	public ObjectMeshModifier setModel(int index, int modelId) {
		int[][] defModelIds = getDefModelIds();
		if (modelIds == null) {
			modelIds = new int[defModelIds[slot].length];
			System.arraycopy(defModelIds[slot], 0, modelIds, 0, modelIds.length);
		}
		if (index < 0 || index >= modelIds.length)
			throw new RuntimeException("Index " + index + " for models of [" + slot + "] " + object.getDefinitions().getId() + " is out of bounds.");
		if (modelId == -1)
			modelIds[index] = defModelIds[slot][index];
		else
			modelIds[index] = modelId;
		return this;
	}

	public ObjectMeshModifier addModels(int... models) {
		int[][] defModelIds = getDefModelIds();
		if (Arrays.equals(models, defModelIds[slot]))
			return this;
		modelIds = new int[defModelIds[slot].length];
		System.arraycopy(defModelIds[slot], 0, modelIds, 0, modelIds.length);

		for (int i = 0;i < defModelIds[slot].length;i++) {
			if (i >= models.length)
				continue;
			modelIds[i] = models[i];
		}
		return this;
	}

	public ObjectMeshModifier setColor(int index, int color) {
		short[] defColors = getDefModifiedColours();
		if (modifiedColors == null && defColors != null) {
			modifiedColors = new int[defColors.length];
			for (int i = 0;i < modifiedColors.length;i++)
				modifiedColors[i] = defColors[i];
		}
		if (modifiedColors == null || index < 0 || index >= modifiedColors.length)
			throw new RuntimeException("Index " + index + " for models of " + object.getDefinitions().getId() + " is out of bounds.");
		if (color == -1)
			modifiedColors[index] = defColors[index];
		else
			modifiedColors[index] = (short) color;
		return this;
	}

	public ObjectMeshModifier addColors(int... colors) {
		short[] defColors = getDefModifiedColours();
		if (defColors != null) {
			modifiedColors = new int[defColors.length];
			for (int i = 0;i < modifiedColors.length;i++)
				modifiedColors[i] = defColors[i];
		}
		if (modifiedColors == null)
			return this;
		for (int i = 0;i < modifiedColors.length;i++) {
			if (i >= colors.length)
				break;
			modifiedColors[i] = (short) colors[i];
		}
		return this;
	}

	public ObjectMeshModifier setTexture(int index, int texId) {
		short[] defTextures = getDefModifiedTextureColours();
		if (modifiedTextures == null && defTextures != null) {
			modifiedTextures = new int[defTextures.length];
			for (int i = 0;i < modifiedTextures.length;i++)
				modifiedTextures[i] = defTextures[i];
		}
		if (modifiedTextures == null || index < 0 || index >= modifiedTextures.length)
			throw new RuntimeException("Index " + index + " for models of " + object.getDefinitions().getId() + " is out of bounds.");
		if (texId == -1)
			modifiedTextures[index] = defTextures[index];
		else
			modifiedTextures[index] = (short) texId;
		return this;
	}

	public ObjectMeshModifier addTextures(int... textures) {
		short[] defTextures = getDefModifiedTextureColours();
		if (defTextures != null) {
			modifiedTextures = new int[defTextures.length];
			for (int i = 0;i < modifiedTextures.length;i++)
				modifiedTextures[i] = defTextures[i];
		}
		if (modifiedTextures == null)
			return this;
		for (int i = 0;i < modifiedTextures.length;i++) {
			if (i >= textures.length)
				break;
			modifiedTextures[i] = (short) textures[i];
		}
		return this;
	}

	public GameObject getObject() {
		return object;
	}

	public int[] getModelIds() {
		return modelIds;
	}

	public int[] getModifiedColors() {
		return modifiedColors;
	}

	public int[] getModifiedTextures() {
		return modifiedTextures;
	}
}
