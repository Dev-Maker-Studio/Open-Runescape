package com.jagex.game.runetek5.config.sequence.spot;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

/**
 * Fully refactored
 * @author Brainpower - 21/05/2020
 *
 */
public class SpotAnimType {

    protected SpotAnimTypeList gfxTypeList;
    public int currentId;
    public int sequenceId = -1;
    public boolean replay;
    public int modelId;
    public int scaleXZ = 128;
    public int scaleY = 128;
    public int rotation;
    public int brightness;
    public int contrast;
    public byte contourType;
    public int contourModifier = -1;
    public short[] recolorSrc;
    public short[] recolorDst;
    public short[] retextureSrc;
    public short[] retextureDst;

    void decode(Packet stream) {
        while (true) {
            int opcode = stream.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            decode(stream, opcode);
        }
    }

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.modelId = stream.readBigSmart();
        } else if (opcode == 2) {
            this.sequenceId = stream.readBigSmart();
        } else if (opcode == 4) {
            this.scaleXZ = stream.readUnsignedShort();
        } else if (opcode == 5) {
            this.scaleY = stream.readUnsignedShort();
        } else if (opcode == 6) {
            this.rotation = stream.readUnsignedShort();
        } else if (opcode == 7) {
            this.brightness = stream.readUnsignedByte();
        } else if (opcode == 8) {
            this.contrast = stream.readUnsignedByte();
        } else if (opcode == 9) {
            this.contourType = 3;
            this.contourModifier = 8224;
        } else if (opcode == 10) {
            this.replay = true;
        } else if (opcode == 11) {
            this.contourType = 1;
        } else if (opcode == 12) {
            this.contourType = 4;
        } else if (opcode == 13) {
            this.contourType = 5;
        } else if (opcode == 14) {
            this.contourType = 2;
            this.contourModifier = stream.readUnsignedByte() * 256;
        } else if (opcode == 15) {
            this.contourType = 3;
            this.contourModifier = stream.readUnsignedShort();
        } else if (opcode == 16) {
            this.contourType = 3;
            this.contourModifier = stream.readInt();
		} else if (opcode == 40) {
			int colorCount = stream.readUnsignedByte();
			this.recolorSrc = new short[colorCount];
			this.recolorDst = new short[colorCount];
			for (int colorIndex = 0; colorIndex < colorCount; colorIndex++) {
				this.recolorSrc[colorIndex] = (short) stream.readUnsignedShort();
				this.recolorDst[colorIndex] = (short) stream.readUnsignedShort();
			}
		} else if (opcode == 41) {
			int textureCount = stream.readUnsignedByte();
			this.retextureSrc = new short[textureCount];
			this.retextureDst = new short[textureCount];
			for (int textureIndex = 0; textureIndex < textureCount; textureIndex++) {
				this.retextureSrc[textureIndex] = (short) stream.readUnsignedShort();
				this.retextureDst[textureIndex] = (short) stream.readUnsignedShort();
			}
		}
	}

    public boolean isModelLoaded() {
        return this.modelId == -1 || this.gfxTypeList.modelResource.isCached(this.modelId, 0);
    }
    
    public Model getAnimatedModel(AbstractToolkit toolkit, int initialFunctionMask, Sequence sequence, byte copyType) {
        return getAnimatedModel(toolkit, initialFunctionMask, false, null, null, 0, 0, 0, sequence, copyType);
    }

    public Model getAnimatedModel(AbstractToolkit toolkit, int initialFunctionMask, boolean requiresAdjustment, Ground groundCurrentLvl, Ground groundNextLvl, int sceneX, int averageHeight, int sceneY, Sequence sequence, byte copyType) {
        int displacementFunctionMask = initialFunctionMask;
        requiresAdjustment &= this.contourType != 0;
        if (sequence != null) {
            displacementFunctionMask = initialFunctionMask | sequence.getFunctionMask();
        }
        if (requiresAdjustment) {
            displacementFunctionMask |= this.contourType == 3 ? FULL_DISPLACEMENT : DISPLACE_Y;
        }
        if (this.scaleY != 128) {
            displacementFunctionMask |= DISPLACE_Y;
        }
        if (this.scaleXZ != 128 || this.rotation != 0) {
            displacementFunctionMask |= DISPLACE_X | DISPLACE_Z;
        }
        Model cachedModel;
        synchronized (this.gfxTypeList.modelCache) {
            cachedModel = (Model) this.gfxTypeList.modelCache.get(this.currentId |= toolkit.renderId << 29);
        }
        if (cachedModel == null || toolkit.mismatchingFunctionMask(cachedModel.getFunctionMask(), displacementFunctionMask) != 0) {
            if (cachedModel != null) {
                displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, cachedModel.getFunctionMask());
            }
            int adjustmentFunctionMask = displacementFunctionMask;
            if (this.recolorSrc != null) {
                adjustmentFunctionMask = displacementFunctionMask | RECOLOR;
            }
            if (this.retextureSrc != null) {
                adjustmentFunctionMask |= RETEXTURE;
            }
            Mesh mesh = Mesh.fromResource(this.gfxTypeList.modelResource, this.modelId);
            if (mesh == null) {
                return null;
            }
            if (mesh.version < 13) {
                mesh.upscale();
            }
            cachedModel = toolkit.createModel(mesh, adjustmentFunctionMask, this.gfxTypeList.preferenceMask, this.brightness + 64, this.contrast + 850);
            if (this.recolorSrc != null) {
                for (int index = 0; index < this.recolorSrc.length; index++) {
                    cachedModel.recolor(this.recolorSrc[index], this.recolorDst[index]);
                }
            }
            if (this.retextureSrc != null) {
                for (int index = 0; index < this.retextureSrc.length; index++) {
                    cachedModel.rematerial(this.retextureSrc[index], this.retextureDst[index]);
                }
            }
            cachedModel.applyShading(displacementFunctionMask);
            synchronized (this.gfxTypeList.modelCache) {
                this.gfxTypeList.modelCache.put(cachedModel, this.currentId |= toolkit.renderId << 29);
            }
        }
        Model model = cachedModel.copy(copyType, displacementFunctionMask, true);
        if (sequence != null) {
            sequence.animateRotatedModel(model, 0);
        }
        if (this.scaleXZ != 128 || this.scaleY != 128) {
            model.scale(this.scaleXZ, this.scaleY, this.scaleXZ);
        }
        if (this.rotation != 0) {
            if (this.rotation == 90) {
                model.yaxisRotateWithoutNormals(4096);
            }
            if (this.rotation == 180) {
                model.yaxisRotateWithoutNormals(8192);
            }
            if (this.rotation == 270) {
                model.yaxisRotateWithoutNormals(12288);
            }
        }
        if (requiresAdjustment) {
            model.contourToGround(this.contourType, this.contourModifier, groundCurrentLvl, groundNextLvl, sceneX, averageHeight, sceneY);
        }
        model.applyShading(initialFunctionMask);
        return model;
    }
}
