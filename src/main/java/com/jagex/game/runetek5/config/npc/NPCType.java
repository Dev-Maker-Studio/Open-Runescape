package com.jagex.game.runetek5.config.npc;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.bas.BasTypeList;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCDirection;
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.vardomain.VarDomain;

/**
 * [J]
 * @author Steven - 11/10/2023
 * 
 * [NXT]
	[X]jag::game::NPCType::DecodeType(jag::Packet &,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  0000000000469B30 000010DD 00000048 00000000 R . . . . T .
	[~]jag::game::NPCType::GetBodyModels(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::shared_ptr<jag::graphics::EffectState> const&,jag::game::NPCTypeCustomisation const*,jag::game::PlayerVarDomain const*,jag::game::ConfigProvider *) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000051D630 0000022C 000000A8 00000010 R . . . . . .
	[~]jag::game::NPCType::GetHeadModels(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::shared_ptr<jag::graphics::EffectState> const&,jag::game::NPCTypeCustomisation const*,jag::game::PlayerVarDomain const*,jag::game::ConfigProvider *) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000051DA30 0000021C 000000A8 00000010 R . . . . . .
	[~]jag::game::NPCType::GetModel(jag::game::NPCType::ModelType,jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::Array<int> const&,jag::game::NPCTypeCustomisation const*) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000051CBF0 00000A20 000000E8 00000081 R . . . . . .
	[X]jag::game::NPCType::GetMultiNPC(bool &,jag::game::PlayerVarDomain const&,jag::game::ConfigProvider &)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 .text  0000000000117380 00000208 00000038 00000000 R . . . . . .
	[-]jag::game::NPCType::NPCType(jag::game::ConfigTypeListPreloadPackedContainer<jag::game::NPCType> *,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  0000000000420F70 000003DF 00000018 00000000 R . . . . . .
	[X]jag::game::NPCType::PostDecode(void)  
 *
 */
public class NPCType {

    public static short[] npcRecolorPalette = new short[256];
    public int type;
    public String[] actions;
    public int[] modelIds;
    public byte gameType;
    public String name = "null";
    public int size = 1;
    public int npcBasId = -1;
    public int primaryCursor = -1;
    public int secondaryCursor = -1;
    public int primaryCursorActionIndex = -1;
    public int secondaryCursorActionIndex = -1;
    public int attackCursor = -1;
    public boolean displayOnMinimap = true;
    public int combatLevel = -1;
    public boolean highPriority;
    public boolean mediumPriority;
    public boolean lowPriority;
    public int headIcons = -1;
    public int overheadSprite = -1;
    public int turnDirection = 32;
    public boolean visible = true;
    public boolean delayMovement = true;
    public boolean shadowed = true;
    public short shadowColorSrc;
    public short shadowColorDst;
    public byte shadowAlphaSrc = -96;
    public byte shadowAlphaDst = -16;
    public byte walkingFlags;
    public int walkingSoundEffect = -1;
    public int teleportSoundEffect = -1;
    public int idleSoundEffect = -1;
    public int runningSoundEffect = -1;
    public int ambientSoundMinHearDistance;
    public int ambientSoundMaxHearDistance;
    public int ambientSoundVolume = 255;
    public int iconHeight = -1;
    public NPCDirection respawnDirection;
    public int mecId;
    public int shadowSize;
    public int ambientSoundMaxDelay;
    public int ambientSoundMinDelay;
    public int sizeShift;
    public boolean hasTint;
    public short[] recolorDst;
    public short[] retextureDst;
    public int[] headModels;
    public int[] alternativeIds;
    public int[] quests;
    public boolean instrumentSoundEffect;
    NpcTypeList npcTypeList;
    public byte tintOpacity;
    public int scaleXZ = 128;
    public int scaleY = 128;
    public int ambient;
    public int contrast;
    public int varpbit = -1;
    public int varp = -1;
    public short[] recolorSrc;
    public short[] retextureSrc;
    public byte[] recolorDPalette;
    public int[][] modelOffsets;
    public byte tintHue;
    public byte tintSaturation;
    public byte tintLightness;
    public IterableNodeMap<Node<?>> parameters;

    NPCType() {
        this.respawnDirection = NPCDirection.SOUTH;
        this.mecId = -1;
        this.gameType = -1;
        this.shadowSize = -1;
        this.ambientSoundMaxDelay = 256;
        this.ambientSoundMinDelay = 256;
        this.sizeShift = 0;
        this.hasTint = true;
    }

    void decodeType(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            decodeOpcode(buffer, opcode);
        }
    }

    public Model getAnimatedBodymodel(AbstractToolkit toolkit, int functionMask, BasTypeList basType, VarDomain varProvider, Sequence animation5, Sequence animation6, Sequence[] arr_7, int[] ints_8, int i_9, NPCTypeCustomisation customisation) {
        return getAnimatedBodymodel(toolkit, functionMask, basType, varProvider, animation5, animation6, arr_7, ints_8, i_9, customisation, this.npcBasId, true);
    }

    public int getCs2Value(int mapKey, int defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } else {
            IntNode node = (IntNode) this.parameters.get(mapKey);
            return node == null ? defaultValue : node.value;
        }
    }

    void setGameType() {
        if (this.modelIds == null) {
            this.modelIds = new int[0];
        }
        if (this.gameType == -1) {
            if (this.npcTypeList.game == Game.RUNESCAPE) {
                this.gameType = 1;
            } else {
                this.gameType = 0;
            }
        }
    }

    public Model getAnimatedBodymodel(AbstractToolkit toolkit, int functionMask, BasTypeList bastypelist, VarDomain varProvider, Sequence animationSequence, Sequence idleSequence, Sequence[] animatingSequence, int[] submeshRotations, int submeshRotationOffset, NPCTypeCustomisation npcCustomisation, int basId, boolean ignoreOverrideBas) {
        if (this.alternativeIds != null) {
            NPCType npctype = getMultiNpc(varProvider);
            return npctype == null ? null : npctype.getAnimatedBodymodel(toolkit, functionMask, bastypelist, varProvider, animationSequence, idleSequence, animatingSequence, submeshRotations, submeshRotationOffset, npcCustomisation, basId, ignoreOverrideBas);
        }
        int displacementFunctionMask = functionMask;
        if (this.scaleY != 128) {
            displacementFunctionMask = functionMask | DISPLACE_Y;
        }
        if (this.scaleXZ != 128) {
            displacementFunctionMask |= DISPLACE_X | DISPLACE_Z;
        }
        boolean requiresAnimating = false;
        int sequenceCount = animatingSequence != null ? animatingSequence.length : 0;
        for (int sequenceIndex = 0; sequenceIndex < sequenceCount; sequenceIndex++) {
            if (animatingSequence[sequenceIndex] != null) {
                displacementFunctionMask |= animatingSequence[sequenceIndex].getFunctionMask();
                requiresAnimating = true;
            }
        }
        if (animationSequence != null) {
            displacementFunctionMask |= animationSequence.getFunctionMask();
            requiresAnimating = true;
        }
        if (idleSequence != null) {
            displacementFunctionMask |= idleSequence.getFunctionMask();
            requiresAnimating = true;
        }
        long npcHash = this.type | (long) toolkit.renderId << 16;
        if (npcCustomisation != null) {
            npcHash |= npcCustomisation.uid << 24;
        }
        Model model;
        synchronized (this.npcTypeList.bodymodelCache) {
            model = (Model) this.npcTypeList.bodymodelCache.get(npcHash);
        }
        BasType basType = null;
        if (!ignoreOverrideBas && basId != -1) {
            basType = bastypelist.list(basId);
        } else if (this.npcBasId != -1) {
            basType = bastypelist.list(this.npcBasId);
        }
        if (model == null || (model.getFunctionMask() & displacementFunctionMask) != displacementFunctionMask) {
            if (model != null) {
                displacementFunctionMask |= model.getFunctionMask();
            }
            int appearanceFunctionMask = displacementFunctionMask;
            if (this.recolorSrc != null) {
                appearanceFunctionMask = displacementFunctionMask | RECOLOR;
            }
            if (this.retextureSrc != null) {
                appearanceFunctionMask |= RETEXTURE;
            }
            if (this.tintOpacity != 0) {
                appearanceFunctionMask |= TINT;
            }
            int[] npcModelIds = npcCustomisation != null && npcCustomisation.modelIds != null ? npcCustomisation.modelIds : this.modelIds;
            boolean missingModels = false;
            synchronized (this.npcTypeList.modelResource) {
                int index = 0;
                while (index < npcModelIds.length) {
                    if (npcModelIds[index] != -1 && !this.npcTypeList.modelResource.isCached(npcModelIds[index], 0)) {
                        missingModels = true;
                    }
                    index++;
                }
            }
            if (missingModels) {
                return null;
            }
            Mesh[] meshes = new Mesh[npcModelIds.length];
            for (int meshIndex = 0; meshIndex < npcModelIds.length; meshIndex++) {
                if (npcModelIds[meshIndex] != -1) {
                    synchronized (this.npcTypeList.modelResource) {
                        meshes[meshIndex] = Mesh.fromResource(this.npcTypeList.modelResource, npcModelIds[meshIndex]);
                    }
                    if (meshes[meshIndex] != null) {
                        if (meshes[meshIndex].version < 13) {
                            meshes[meshIndex].upscale();
                        }
                        if (this.modelOffsets != null && this.modelOffsets[meshIndex] != null) {
                            meshes[meshIndex].translate(this.modelOffsets[meshIndex][0], this.modelOffsets[meshIndex][1], this.modelOffsets[meshIndex][2]);
                        }
                    }
                }
            }
            if (basType != null && basType.objVerticeTransformations != null) {
                for (int meshIndex = 0; meshIndex < basType.objVerticeTransformations.length; meshIndex++) {
                    if (meshIndex < meshes.length && meshes[meshIndex] != null) {
                        int translateX = 0;
                        int translateY = 0;
                        int translateZ = 0;
                        int rotateX = 0;
                        int rotateY = 0;
                        int rotateZ = 0;
                        if (basType.objVerticeTransformations[meshIndex] != null) {
                            translateX = basType.objVerticeTransformations[meshIndex][0];
                            translateY = basType.objVerticeTransformations[meshIndex][1];
                            translateZ = basType.objVerticeTransformations[meshIndex][2];
                            rotateX = basType.objVerticeTransformations[meshIndex][3] << 3;
                            rotateY = basType.objVerticeTransformations[meshIndex][4] << 3;
                            rotateZ = basType.objVerticeTransformations[meshIndex][5] << 3;
                        }
                        if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
                            meshes[meshIndex].rotateWithoutNormals(rotateX, rotateY, rotateZ);
                        }
                        if (translateX != 0 || translateY != 0 || translateZ != 0) {
                            meshes[meshIndex].translate(translateX, translateY, translateZ);
                        }
                    }
                }
            }
            Mesh mesh;
            if (meshes.length == 1) {
                mesh = meshes[0];
            } else {
                mesh = new Mesh(meshes, meshes.length);
            }
            model = toolkit.createModel(mesh, appearanceFunctionMask, this.npcTypeList.preferenceFlags, this.ambient + 64, this.contrast * 5 + 850);
            if (this.recolorSrc != null) {
                short[] recolorDestination;
                if (npcCustomisation != null && npcCustomisation.recolorDst != null) {
                    recolorDestination = npcCustomisation.recolorDst;
                } else {
                    recolorDestination = this.recolorDst;
                }
                for (int colorIndex = 0; colorIndex < this.recolorSrc.length; colorIndex++) {
                    if (this.recolorDPalette != null && colorIndex < this.recolorDPalette.length) {
                        model.recolor(this.recolorSrc[colorIndex], npcRecolorPalette[this.recolorDPalette[colorIndex] & 0xff]);
                    } else {
                        model.recolor(this.recolorSrc[colorIndex], recolorDestination[colorIndex]);
                    }
                }
            }
            if (this.retextureSrc != null) {
                short[] retextureDestination;
                if (npcCustomisation != null && npcCustomisation.retextureDst != null) {
                    retextureDestination = npcCustomisation.retextureDst;
                } else {
                    retextureDestination = this.retextureDst;
                }
                for (int textureIndex = 0; textureIndex < this.retextureSrc.length; textureIndex++) {
                    model.rematerial(this.retextureSrc[textureIndex], retextureDestination[textureIndex]);
                }
            }
            if (this.tintOpacity != 0) {
                model.tint(this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity & 0xff);
            }
            model.applyShading(displacementFunctionMask);
            synchronized (this.npcTypeList.bodymodelCache) {
                this.npcTypeList.bodymodelCache.put(model, npcHash);
            }
        }
        Model copiedModel = model.copy((byte) 4, displacementFunctionMask, true);
        boolean modifiesSubmesh = false;
        if (submeshRotations != null) {
            for (int index = 0; index < 12; index++) {
                if (submeshRotations[index] != -1) {
                    modifiesSubmesh = true;
                    break;
                }
            }
        }
        if (requiresAnimating || modifiesSubmesh) {
            Matrix43[] submeshMatrices = null;
            if (basType != null) {
                submeshMatrices = basType.getSubmeshMatrices();
            }
            if (modifiesSubmesh && submeshMatrices != null) {
                for (int index = 0; index < 12; index++) {
                    if (submeshMatrices[index] != null) {
                        copiedModel.offsetSubmesh(submeshMatrices[index], 1 << index, true);
                    }
                }
            }
            int seqPtr = 0;
            for (int submesh = 1; seqPtr < sequenceCount; submesh <<= 1) {
                if (animatingSequence[seqPtr] != null) {
                    animatingSequence[seqPtr].animateUnrotatedModel(copiedModel, submesh);
                }
                seqPtr++;
            }
            if (modifiesSubmesh) {
                for (int index = 0; index < 12; index++) {
                    if (submeshRotations[index] != -1) {
                        int rotation = submeshRotations[index] - submeshRotationOffset;
                        rotation &= 0x3fff;
                        Matrix43 matrix = new Matrix43();
                        matrix.rotateMatrix(0.0F, 1.0F, 0.0F, MathUtils.getTheta(rotation));
                        copiedModel.offsetSubmesh(matrix, 1 << index, false);
                    }
                }
            }
            if (modifiesSubmesh && submeshMatrices != null) {
                for (int index = 0; index < 12; index++) {
                    if (submeshMatrices[index] != null) {
                        copiedModel.offsetSubmesh(submeshMatrices[index], 1 << index, false);
                    }
                }
            }
            if (animationSequence != null && idleSequence != null) {
                Sequence.doubleAnimation(copiedModel, animationSequence, idleSequence);
            } else if (animationSequence != null) {
                animationSequence.animateRotatedModel(copiedModel, 0);
            } else if (idleSequence != null) {
                idleSequence.animateRotatedModel(copiedModel, 0);
            }
            if (this.scaleXZ != 128 || this.scaleY != 128) {
                copiedModel.scale(this.scaleXZ, this.scaleY, this.scaleXZ);
            }
            copiedModel.applyShading(functionMask);
        }
        return copiedModel;
    }

    public Model getAnimatedHeadmodel(AbstractToolkit toolkit, int initialFunctionMask, VarDomain varProvider, Sequence headSequence, NPCTypeCustomisation npcCustomisation) {
        if (this.alternativeIds != null) {
            NPCType npctype = getMultiNpc(varProvider);
            return npctype == null ? null : npctype.getAnimatedHeadmodel(toolkit, initialFunctionMask, varProvider, headSequence, npcCustomisation);
        } 
        if (this.headModels == null && (npcCustomisation == null || npcCustomisation.modelIds == null)) {
            return null;
        } 
        int flagsCopy = initialFunctionMask;
        if (headSequence != null) {
            flagsCopy = initialFunctionMask | headSequence.getFunctionMask();
        }
        long npcHash = this.type | (long) toolkit.renderId << 16;
        if (npcCustomisation != null) {
            npcHash |= npcCustomisation.uid << 24;
        }
        Model model;
        synchronized (this.npcTypeList.headmodelCache) {
            model = (Model) this.npcTypeList.headmodelCache.get(npcHash);
        }
        if (model == null || (model.getFunctionMask() & flagsCopy) != flagsCopy) {
            if (model != null) {
                flagsCopy |= model.getFunctionMask();
            }
            int adjustmentFunctionMask = flagsCopy;
            if (this.recolorSrc != null) {
                adjustmentFunctionMask = flagsCopy | RECOLOR;
            }
            if (this.retextureSrc != null) {
                adjustmentFunctionMask |= RETEXTURE;
            }
            if (this.tintOpacity != 0) {
                adjustmentFunctionMask |= TINT;
            }
            int[] npcHeadModelIds = npcCustomisation != null && npcCustomisation.modelIds != null ? npcCustomisation.modelIds : this.headModels;
            boolean missingModels = false;
            synchronized (this.npcTypeList.modelResource) {
                int meshIndex = 0;
                while (meshIndex < npcHeadModelIds.length) {
                    if (!this.npcTypeList.modelResource.isCached(npcHeadModelIds[meshIndex], 0)) {
                        missingModels = true;
                    }
                    meshIndex++;
                }
            }
            if (missingModels) {
                return null;
            }
            Mesh[] meshes = new Mesh[npcHeadModelIds.length];
            synchronized (this.npcTypeList.modelResource) {
                for (int meshIndex = 0; meshIndex < npcHeadModelIds.length; meshIndex++) {
                    meshes[meshIndex] = Mesh.fromResource(this.npcTypeList.modelResource, npcHeadModelIds[meshIndex]);
                }
            }
            for (int meshIndex = 0; meshIndex < npcHeadModelIds.length; meshIndex++) {
                if (meshes[meshIndex] != null && meshes[meshIndex].version < 13) {
                    meshes[meshIndex].upscale();
                }
            }
            Mesh mesh;
            if (meshes.length == 1) {
                mesh = meshes[0];
            } else {
                mesh = new Mesh(meshes, meshes.length);
            }
            model = toolkit.createModel(mesh, adjustmentFunctionMask, this.npcTypeList.preferenceFlags, 64, 768);
            if (this.recolorSrc != null) {
            	short[] recolorDestination;
                if (npcCustomisation != null && npcCustomisation.recolorDst != null) {
                    recolorDestination = npcCustomisation.recolorDst;
                } else {
                    recolorDestination = this.recolorDst;
                }
                for (int colorIndex = 0; colorIndex < this.recolorSrc.length; colorIndex++) {
                    if (this.recolorDPalette != null && colorIndex < this.recolorDPalette.length) {
                        model.recolor(this.recolorSrc[colorIndex], npcRecolorPalette[this.recolorDPalette[colorIndex] & 0xff]);
                    } else {
                        model.recolor(this.recolorSrc[colorIndex], recolorDestination[colorIndex]);
                    }
                }
            }
            if (this.retextureSrc != null) {
            	short[] retextureDestination;
                if (npcCustomisation != null && npcCustomisation.retextureDst != null) {
                    retextureDestination = npcCustomisation.retextureDst;
                } else {
                    retextureDestination = this.retextureDst;
                }
                for (int textureIndex = 0; textureIndex < this.retextureSrc.length; textureIndex++) {
                    model.rematerial(this.retextureSrc[textureIndex], retextureDestination[textureIndex]);
                }
            }
            if (this.tintOpacity != 0) {
                model.tint(this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity & 0xff);
            }
            model.applyShading(flagsCopy);
            synchronized (this.npcTypeList.headmodelCache) {
                this.npcTypeList.headmodelCache.put(model, npcHash);
            }
        }
        if (headSequence != null) {
            model = model.copy((byte) 1, flagsCopy, true);
            headSequence.animateRotatedModel(model, 0);
        }
        model.applyShading(initialFunctionMask);
        return model;
    }

    public boolean isBodyCached() {
        if (this.modelIds == null) {
            return true;
        } 
        boolean fullyCached = true;
        int[] models = this.modelIds;
        for (int index = 0; index < models.length; index++) {
            int archiveId = models[index];
            if (!this.npcTypeList.modelResource.isCached(archiveId, 0)) {
                fullyCached = false;
            }
        }
        return fullyCached;
    }

    public String getCS2Param(int key, String defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } 
        ObjectNode objectnode = (ObjectNode) this.parameters.get(key);
        return objectnode == null ? defaultValue : (String) objectnode.value;
    }

    /**
     * [J]
     */
    public NPCType getMultiNpc(VarDomain vars) {
        int index = -1;
        if (this.varpbit != -1) {
            index = vars.getVarBitValue(this.varpbit);
        } else if (this.varp != -1) {
            index = vars.getVarValue(this.varp);
        }
        if (index >= 0 && index < this.alternativeIds.length - 1 && this.alternativeIds[index] != -1) {
            return this.npcTypeList.list(this.alternativeIds[index]);
        } 
        int morpedId = this.alternativeIds[this.alternativeIds.length - 1];
        return morpedId != -1 ? this.npcTypeList.list(morpedId) : null;
    }

    public boolean hasTransformations(VarDomain vars) {
        if (this.alternativeIds == null) {
            return true;
        } 
        int index = -1;
        if (this.varpbit != -1) {
            index = vars.getVarBitValue(this.varpbit);
        } else if (this.varp != -1) {
            index = vars.getVarValue(this.varp);
        }
        if (index >= 0 && index < this.alternativeIds.length - 1 && this.alternativeIds[index] != -1) {
            return true;
        } 
        int morpedId = this.alternativeIds[this.alternativeIds.length - 1];
        return morpedId != -1;
    }

    public boolean hasSoundEffects() {
        if (this.alternativeIds == null) {
            return this.walkingSoundEffect != -1 || this.idleSoundEffect != -1 || this.runningSoundEffect != -1;
        } 
        for (int index = 0; index < this.alternativeIds.length; index++) {
            if (this.alternativeIds[index] != -1) {
                NPCType morped = this.npcTypeList.list(this.alternativeIds[index]);
                if (morped.walkingSoundEffect != -1 || morped.idleSoundEffect != -1 || morped.runningSoundEffect != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    public void decodeOpcode(Packet stream, int opcode) {
        if (opcode == 1) {
            int modelCount = stream.readUByte();
            this.modelIds = new int[modelCount];
            for (int index = 0; index < modelCount; index++) {
                this.modelIds[index] = stream.readBigSmart();
            }
        } else if (opcode == 2) {
            this.name = stream.readString();
        } else if (opcode == 12) {
            this.size = stream.readUByte();
        } else if (opcode >= 30 && opcode < 35) {
            this.actions[opcode - 30] = stream.readString();
        } else if (opcode == 40) {
            int recolorCount = stream.readUByte();
            this.recolorSrc = new short[recolorCount];
            this.recolorDst = new short[recolorCount];
            for (int index = 0; index < recolorCount; index++) {
                this.recolorSrc[index] = (short) stream.readUShort();
                this.recolorDst[index] = (short) stream.readUShort();
            }
        } else if (opcode == 41) {
            int retextureCount = stream.readUByte();
            this.retextureSrc = new short[retextureCount];
            this.retextureDst = new short[retextureCount];
            for (int index = 0; index < retextureCount; index++) {
                this.retextureSrc[index] = (short) stream.readUShort();
                this.retextureDst[index] = (short) stream.readUShort();
            }
        } else if (opcode == 42) {
            int recolorCount = stream.readUByte();
            this.recolorDPalette = new byte[recolorCount];
            for (int index = 0; index < recolorCount; index++) {
                this.recolorDPalette[index] = stream.readByte();
            }
        } else if (opcode == 60) {
            int headmodelCount = stream.readUByte();
            this.headModels = new int[headmodelCount];
            for (int index = 0; index < headmodelCount; index++) {
                this.headModels[index] = stream.readBigSmart();
            }
        } else if (opcode == 93) {
            this.displayOnMinimap = false;
        } else if (opcode == 95) {
            this.combatLevel = stream.readUShort();
        } else if (opcode == 97) {
            this.scaleXZ = stream.readUShort();
        } else if (opcode == 98) {
            this.scaleY = stream.readUShort();
        } else if (opcode == 99) {
            this.highPriority = true;
        } else if (opcode == 100) {
            this.ambient = stream.readByte();
        } else if (opcode == 101) {
            this.contrast = stream.readByte();
        } else if (opcode == 102) {
            this.headIcons = stream.readUShort();
        } else if (opcode == 103) {
            this.turnDirection = stream.readUShort();
        } else if (opcode == 106 || opcode == 118) {
            this.varpbit = stream.readUShort();
            if (this.varpbit == 65535) {
                this.varpbit = -1;
            }
            this.varp = stream.readUShort();
            if (this.varp == 65535) {
                this.varp = -1;
            }
            int lastAlt = -1;
            if (opcode == 118) {
                lastAlt = stream.readUShort();
                if (lastAlt == 65535) {
                    lastAlt = -1;
                }
            }
            int length = stream.readUByte();
            this.alternativeIds = new int[length + 2];
            for (int index = 0; index <= length; index++) {
                this.alternativeIds[index] = stream.readUShort();
                if (this.alternativeIds[index] == 65535) {
                    this.alternativeIds[index] = -1;
                }
            }
            this.alternativeIds[length + 1] = lastAlt;
        } else if (opcode == 107) {
            this.visible = false;
        } else if (opcode == 109) {
            this.delayMovement = false;
        } else if (opcode == 111) {
            this.shadowed = false;
        } else if (opcode == 113) {
            this.shadowColorSrc = (short) stream.readUShort();
            this.shadowColorDst = (short) stream.readUShort();
        } else if (opcode == 114) {
            this.shadowAlphaSrc = stream.readByte();
            this.shadowAlphaDst = stream.readByte();
        } else if (opcode == 119) {
            this.walkingFlags = stream.readByte();
        } else if (opcode == 121) {
            this.modelOffsets = new int[this.modelIds.length][];
            int numOffsetAdjustments = stream.readUByte();
            for (int index = 0; index < numOffsetAdjustments; index++) {
                int model = stream.readUByte();
                int[] translate = this.modelOffsets[model] = new int[3];
                translate[0] = stream.readByte();
                translate[1] = stream.readByte();
                translate[2] = stream.readByte();
            }
        } else if (opcode == 123) {
            this.iconHeight = stream.readUShort();
        } else if (opcode == 125) {
            this.respawnDirection = (NPCDirection) Identifiable.identify(NPCDirection.values(), stream.readByte());
        } else if (opcode == 127) {
            this.npcBasId = stream.readUShort();
        } else if (opcode == 128) {
            Identifiable.identify(MovementType.values(), stream.readUByte());
        } else if (opcode == 134) { 
            this.walkingSoundEffect = stream.readUShort();
            if (this.walkingSoundEffect == 65535) {
                this.walkingSoundEffect = -1;
            }
            this.teleportSoundEffect = stream.readUShort();
            if (this.teleportSoundEffect == 65535) {
                this.teleportSoundEffect = -1;
            }
            this.idleSoundEffect = stream.readUShort();
            if (this.idleSoundEffect == 65535) {
                this.idleSoundEffect = -1;
            }
            this.runningSoundEffect = stream.readUShort();
            if (this.runningSoundEffect == 65535) {
                this.runningSoundEffect = -1;
            }
            this.ambientSoundMinHearDistance = stream.readUByte();
        } else if (opcode == 135) {
            this.primaryCursorActionIndex = stream.readUByte();
            this.primaryCursor = stream.readUShort();
        } else if (opcode == 136) {
            this.secondaryCursorActionIndex = stream.readUByte();
            this.secondaryCursor = stream.readUShort();
        } else if (opcode == 137) {
            this.attackCursor = stream.readUShort();
        } else if (opcode == 138) {
            this.overheadSprite = stream.readBigSmart();
        } else if (opcode == 140) {
            this.ambientSoundVolume = stream.readUByte();
        } else if (opcode == 141) {
            this.lowPriority = true;
        } else if (opcode == 142) {
            this.mecId = stream.readUShort();
        } else if (opcode == 143) {
            this.mediumPriority = true;
        } else if (opcode >= 150 && opcode < 155) {
            this.actions[opcode - 150] = stream.readString();
            if (!this.npcTypeList.isMember) {
                this.actions[opcode - 150] = null;
            }
        } else if (opcode == 155) {
            this.tintHue = stream.readByte();
            this.tintSaturation = stream.readByte();
            this.tintLightness = stream.readByte();
            this.tintOpacity = stream.readByte();
        } else if (opcode == 158) {
            this.gameType = 1;
        } else if (opcode == 159) {
            this.gameType = 0;
        } else if (opcode == 160) {
            int i_4 = stream.readUByte();
            this.quests = new int[i_4];
            for (int i_5 = 0; i_5 < i_4; i_5++) {
                this.quests[i_5] = stream.readUShort();
            }
        } else if (opcode == 162) {
            this.instrumentSoundEffect = true;
        } else if (opcode == 163) {
            this.shadowSize = stream.readUByte();
        } else if (opcode == 164) {
            this.ambientSoundMaxDelay = stream.readUShort();
            this.ambientSoundMinDelay = stream.readUShort();
        } else if (opcode == 165) {
            this.sizeShift = stream.readUByte();
        } else if (opcode == 168) {
            this.ambientSoundMaxHearDistance = stream.readUByte();
        } else if (opcode == 169) {
            this.hasTint = false;
        } else if (opcode == 249) {
            int paramCount = stream.readUByte();
            if (this.parameters == null) {
                int capacity = MathUtils.nextPowerOfTwo(paramCount);
                this.parameters = new IterableNodeMap<Node<?>>(capacity);
            }
            for (int index = 0; index < paramCount; index++) {
                boolean isString = stream.readUByte() == 1;
                int key = stream.readUMedium();
                Object parameter;
                if (isString) {
                    parameter = new ObjectNode(stream.readString());
                } else {
                    parameter = new IntNode(stream.readInt());
                }
                this.parameters.put((Node<?>) parameter, key);
            }
        }
    }
}
