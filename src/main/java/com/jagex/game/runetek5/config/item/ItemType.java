package com.jagex.game.runetek5.config.item;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.utils.TextUtils;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

/**
 * 
 * @author Steven
 *
 * [NXT]
	[X] jag::game::ObjType::DecodeType(jag::Packet &,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  0000000000458610 00001783 000001C8 00000000 R . . . . T .
	jag::game::ObjType::GetAppearance(bool,jag::shared_ptr<jag::game::ObjTypeCustomisation> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  00000000000F1160 0000009A                   R . . . . . .
	jag::game::ObjType::GetModel(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::shared_ptr<jag::game::PlayerModel> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000516590 000005B1 000000E8 00000080 R . . . . . .
	[X]jag::game::ObjType::GetStackSizeAlt(int)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  00000000000F1040 0000011A                   R . . . . T .
	[-]jag::game::ObjType::ObjType(jag::game::ConfigTypeListPreloadPackedContainer<jag::game::ObjType> *,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  00000000003953B0 00000523 00000038 00000000 R . . . . . .
	[X]jag::game::ObjType::PostDecode(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  00000000000A1DE0 00000002                   R . . . . T .
	jag::game::ObjType::SetupDerivedObject(jag::game::DerivedObjType,jag::shared_ptr<jag::game::ObjType> const&,jag::shared_ptr<jag::game::ObjType> const&,char const*)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  00000000003912A0 00000507 00000048 00000000 R . . . . . .

 *
 */
public class ItemType {
    public static short[] recolorPalette = new short[256];
    public int value = 1;
    public int modelZoom = 2000;
    public int modelRotationX;
    public int modelRotationY;
    public int modelRotationZ;
    public int modelOffsetX;
    public int modelOffsetY;
    public String name = "null";
    public boolean membersOnly;
    public int wearPos = -1;
    public int wearPos2 = -1;
    public int wearPos3 = -1;
    public int teamId;
    public String[] groundActions;
    public int stackable;
    public String[] inventoryActions;
    public int multiStackSize = -1;
    public int tooltipColor;
    public boolean hasTooltipColor;
    public boolean tradeable;
    public int unknownInt6;
    public int certId = -1;
    public int certTemplateId = -1;
    public int lendTemplateId = -1;
    public int primaryCursorActionIndex = -1;
    public int primaryCursor = -1;
    public int secondaryCursorActionIndex = -1;
    public int secondaryCursor = -1;
    public int customCursorOp1 = -1;
    public int customCursorId1 = -1;
    public int customCursorOp2 = -1;
    public int customCursorId2 = -1;
    public int[] quests;
    public int pickSizeShift;
    public int bindTemplateId = -1;
    public ItemTypeList typeList;
    public int id;
    public int modelId;
    public short[] recolorSrc;
    public short[] recolorDst;
    public byte[] recolorDPalette;
    public short[] retexSrc;
    public short[] retexDst;
    public int maleEquip1 = -1;
    public int maleEquip2 = -1;
    public int maleEquip3 = -1;
    public int femaleEquip1 = -1;
    public int femaleEquip2 = -1;
    public int femaleEquip3 = -1;
    public int maleEquipmentOffsetX;
    public int femaleEquipmentOffsetX;
    public int maleEquipmentOffsetY;
    public int femaleEquipmentOffsetY;
    public int maleEquipmentOffsetZ;
    public int femaleEquipmentOffsetZ;
    public int maleHead1 = -1;
    public int maleHead2 = -1;
    public int femaleHead1 = -1;
    public int femaleHead2 = -1;
    public IterableNodeMap<Node<?>> parameters;
    public int resizeX = 128;
    public int[] stackIds;
    public int[] stackTriggerAmount;
    public int resizeY = 128;
    public int resizeZ = 128;
    public int ambient;
    public int contrast;
    public int lendId = -1;
    public int bindId = -1;
    
    
    /**
     * ***********************************************************|
     * 							Decode
     * ***********************************************************|
     */
    
    /**
     * [J]
     */
    public void decodeType(Packet stream) {
        while (true) {
            int opcode = stream.readUByte();
            if (opcode == 0) {
                return;
            }
            decodeOpcode(stream, opcode);
        }
    }

    public void decodeOpcode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.modelId = stream.readBigSmart();
        } else if (opcode == 2) {
            this.name = stream.readString();
        } else if (opcode == 4) {
            this.modelZoom = stream.readUShort();
        } else if (opcode == 5) {
            this.modelRotationX = stream.readUShort();
        } else if (opcode == 6) {
            this.modelRotationY = stream.readUShort();
        } else if (opcode == 7) {
            this.modelOffsetX = stream.readUShort();
            if (this.modelOffsetX > 32767) {
                this.modelOffsetX -= 65536;
            }
        } else if (opcode == 8) {
            this.modelOffsetY = stream.readUShort();
            if (this.modelOffsetY > 32767) {
                this.modelOffsetY -= 65536;
            }
        } else if (opcode == 11) {
            this.stackable = 1;
        } else if (opcode == 12) {
            this.value = stream.readInt();
        } else if (opcode == 13) {
            this.wearPos = stream.readUByte();
        } else if (opcode == 14) {
            this.wearPos2 = stream.readUByte();
        } else if (opcode == 16) {
            this.membersOnly = true;
        } else if (opcode == 18) {
            this.multiStackSize = stream.readUShort();
        } else if (opcode == 23) {
            this.maleEquip1 = stream.readBigSmart();
        } else if (opcode == 24) {
            this.maleEquip2 = stream.readBigSmart();
        } else if (opcode == 25) {
            this.femaleEquip1 = stream.readBigSmart();
        } else if (opcode == 26) {
            this.femaleEquip2 = stream.readBigSmart();
        } else if (opcode == 27) {
            this.wearPos3 = stream.readUByte();
        } else if (opcode >= 30 && opcode < 35) {
            this.groundActions[opcode - 30] = stream.readString();
        } else if (opcode >= 35 && opcode < 40) {
            this.inventoryActions[opcode - 35] = stream.readString();
        } else if (opcode == 40) {
            int count = stream.readUByte();
            this.recolorSrc = new short[count];
            this.recolorDst = new short[count];
            for (int index = 0; index < count; index++) {
                this.recolorSrc[index] = (short) stream.readUShort();
                this.recolorDst[index] = (short) stream.readUShort();
            }
        } else if (opcode == 41) {
        	int count = stream.readUByte();
            this.retexSrc = new short[count];
            this.retexDst = new short[count];
            for (int index = 0; index < count; index++) {
                this.retexSrc[index] = (short) stream.readUShort();
                this.retexDst[index] = (short) stream.readUShort();
            }
        } else if (opcode == 42) {
        	int count = stream.readUByte();
            this.recolorDPalette = new byte[count];
            for (int index = 0; index < count; index++) {
                this.recolorDPalette[index] = stream.readByte();
            }
        } else if (opcode == 43) {
            this.tooltipColor = stream.readInt();
            this.hasTooltipColor = true;
        } else if (opcode == 65) {
            this.tradeable = true;
        } else if (opcode == 78) {
            this.maleEquip3 = stream.readBigSmart();
        } else if (opcode == 79) {
            this.femaleEquip3 = stream.readBigSmart();
        } else if (opcode == 90) {
            this.maleHead1 = stream.readBigSmart();
        } else if (opcode == 91) {
            this.femaleHead1 = stream.readBigSmart();
        } else if (opcode == 92) {
            this.maleHead2 = stream.readBigSmart();
        } else if (opcode == 93) {
            this.femaleHead2 = stream.readBigSmart();
        } else if (opcode == 95) {
            this.modelRotationZ = stream.readUShort();
        } else if (opcode == 96) {
            this.unknownInt6 = stream.readUByte();
        } else if (opcode == 97) {
            this.certId = stream.readUShort();
        } else if (opcode == 98) {
            this.certTemplateId = stream.readUShort();
        } else if (opcode >= 100 && opcode < 110) {
            if (this.stackIds == null) {
                this.stackIds = new int[10];
                this.stackTriggerAmount = new int[10];
            }
            this.stackIds[opcode - 100] = stream.readUShort();
            this.stackTriggerAmount[opcode - 100] = stream.readUShort();
        } else if (opcode == 110) {
            this.resizeX = stream.readUShort();
        } else if (opcode == 111) {
            this.resizeY = stream.readUShort();
        } else if (opcode == 112) {
            this.resizeZ = stream.readUShort();
        } else if (opcode == 113) {
            this.ambient = stream.readByte();
        } else if (opcode == 114) {
            this.contrast = stream.readByte();
        } else if (opcode == 115) {
            this.teamId = stream.readUByte();
        } else if (opcode == 121) {
            this.lendId = stream.readUShort();
        } else if (opcode == 122) {
            this.lendTemplateId = stream.readUShort();
        } else if (opcode == 125) {
            this.maleEquipmentOffsetX = stream.readByte() << 2;
            this.maleEquipmentOffsetY = stream.readByte() << 2;
            this.maleEquipmentOffsetZ = stream.readByte() << 2;
        } else if (opcode == 126) {
            this.femaleEquipmentOffsetX = stream.readByte() << 2;
            this.femaleEquipmentOffsetY = stream.readByte() << 2;
            this.femaleEquipmentOffsetZ = stream.readByte() << 2;
        } else if (opcode == 127) {
            this.primaryCursorActionIndex = stream.readUByte();
            this.primaryCursor = stream.readUShort();
        } else if (opcode == 128) {
            this.secondaryCursorActionIndex = stream.readUByte();
            this.secondaryCursor = stream.readUShort();
        } else if (opcode == 129) {
            this.customCursorOp1 = stream.readUByte();
            this.customCursorId1 = stream.readUShort();
        } else if (opcode == 130) {
            this.customCursorOp2 = stream.readUByte();
            this.customCursorId2 = stream.readUShort();
        } else if (opcode == 132) {
        	int count = stream.readUByte();
            this.quests = new int[count];
            for (int index = 0; index < count; index++) {
                this.quests[index] = stream.readUShort();
            }
        } else if (opcode == 134) {
            this.pickSizeShift = stream.readUByte();
        } else if (opcode == 139) {
            this.bindId = stream.readUShort();
        } else if (opcode == 140) {
            this.bindTemplateId = stream.readUShort();
        } else if (opcode == 249) {
        	int count = stream.readUByte();
            if (this.parameters == null) {
            	int index = MathUtils.nextPowerOfTwo(count);
                this.parameters = new IterableNodeMap<Node<?>>(index);
            }
            for (int index = 0; index < count; index++) {
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
    
    public void postDecode() {
    	// NOOP
    }    

    public Model getAnimatedModel(AbstractToolkit toolkit, int initialFunctionMask, int stackSize, PlayerAppearance playerAppearance, Sequence animatingSequence, int tintHue, int tintSaturation, int tintLightness, int tintOpacity) {
        if (this.stackIds != null && stackSize > 1) {
            int itemId = -1;
            for (int stackIndex = 0; stackIndex < 10; stackIndex++) {
                if (stackSize >= this.stackTriggerAmount[stackIndex] && this.stackTriggerAmount[stackIndex] != 0) {
                    itemId = this.stackIds[stackIndex];
                }
            }
            if (itemId != -1) {
                return this.typeList.list(itemId).getAnimatedModel(toolkit, initialFunctionMask, 1, playerAppearance, animatingSequence, tintHue, tintSaturation, tintLightness, tintOpacity);
            }
        }
        int copiedMask = initialFunctionMask;
        if (animatingSequence != null) {
            copiedMask = initialFunctionMask | animatingSequence.getFunctionMask();
        }
        Model model;
        synchronized (this.typeList.modelCache) {
            model = (Model) this.typeList.modelCache.get(this.id | (long) toolkit.renderId << 29);
        }
        if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), copiedMask) != 0) {
            if (model != null) {
                copiedMask = toolkit.mergeFunctionMasks(copiedMask, model.getFunctionMask());
            }
            int adjustmentFunctionMask = copiedMask;
            if (this.retexSrc != null) {
                adjustmentFunctionMask = copiedMask | RETEXTURE;
            }
            if (this.recolorSrc != null || playerAppearance != null) {
                adjustmentFunctionMask |= RECOLOR;
            }
            if (this.resizeX != 128) {
                adjustmentFunctionMask |= DISPLACE_X;
            }
            if (this.resizeX != 128) {
                adjustmentFunctionMask |= DISPLACE_Y;
            }
            if (this.resizeX != 128) {
                adjustmentFunctionMask |= DISPLACE_Z;
            }
            Mesh mesh = Mesh.fromResource(this.typeList.meshResource, this.modelId);
            if (mesh == null) {
                return null;
            }
            if (mesh.version < 13) {
                mesh.upscale();
            }
            model = toolkit.createModel(mesh, adjustmentFunctionMask, this.typeList.preferenceFlags, this.ambient + 64, this.contrast * 5 + 850);
            if (this.resizeX != 128 || this.resizeY != 128 || this.resizeZ != 128) {
                model.scale(this.resizeX, this.resizeY, this.resizeZ);
            }
            if (this.recolorSrc != null) {
                for (int recolorIndex = 0; recolorIndex < this.recolorSrc.length; recolorIndex++) {
                    if (this.recolorDPalette != null && recolorIndex < this.recolorDPalette.length) {
                        model.recolor(this.recolorSrc[recolorIndex], recolorPalette[this.recolorDPalette[recolorIndex] & 0xff]);
                    } else {
                        model.recolor(this.recolorSrc[recolorIndex], this.recolorDst[recolorIndex]);
                    }
                }
            }
            if (this.retexSrc != null) {
                for (int rextureIndex = 0; rextureIndex < this.retexSrc.length; rextureIndex++) {
                    model.rematerial(this.retexSrc[rextureIndex], this.retexDst[rextureIndex]);
                }
            }
            if (playerAppearance != null) {
                for (int paletteIndex = 0; paletteIndex < 10; paletteIndex++) {
                    for (int colorIndex = 0; colorIndex < PlayerAppearance.recolorPaletteSrc[paletteIndex].length; colorIndex++) {
                        if (playerAppearance.recolorDstIndices[paletteIndex] < PlayerAppearance.recolorPaletteDst[paletteIndex][colorIndex].length) {
                            model.recolor(PlayerAppearance.recolorPaletteSrc[paletteIndex][colorIndex], PlayerAppearance.recolorPaletteDst[paletteIndex][colorIndex][playerAppearance.recolorDstIndices[paletteIndex]]);
                        }
                    }
                }
            }
            model.applyShading(copiedMask);
            synchronized (this.typeList.modelCache) {
                this.typeList.modelCache.put(model, this.id | (long) toolkit.renderId << 29);
            }
        }
        if (animatingSequence != null || tintOpacity != 0) {
            model = model.copy((byte) 1, copiedMask, true);
            if (animatingSequence != null) {
                animatingSequence.animateRotatedModel(model, 0);
            }
            if (tintOpacity != 0) {
                model.tint(tintHue, tintSaturation, tintLightness, tintOpacity);
            }
        }
        model.applyShading(initialFunctionMask);
        return model;
    }

    public int[] getSpritePixels(AbstractToolkit mainToolkit, AbstractToolkit itemToolkit, int amount, int outlineSize, int shadowColor, boolean zoomedIn, Component.StackMode renderAmounts, AbstractFontRenderer fontRenderer, PlayerAppearance model) {
        Mesh inventoryMesh = Mesh.fromResource(this.typeList.meshResource, this.modelId);
        if (inventoryMesh == null) {
            return null;
        } 
        if (inventoryMesh.version < 13) {
            inventoryMesh.upscale();
        }
        if (this.recolorSrc != null) {
            for (int recolorIndex = 0; recolorIndex < this.recolorSrc.length; recolorIndex++) {
                if (this.recolorDPalette != null && recolorIndex < this.recolorDPalette.length) {
                    inventoryMesh.recolor(this.recolorSrc[recolorIndex], recolorPalette[this.recolorDPalette[recolorIndex] & 0xff]);
                } else {
                    inventoryMesh.recolor(this.recolorSrc[recolorIndex], this.recolorDst[recolorIndex]);
                }
            }
        }
        if (this.retexSrc != null) {
            for (int rextureIndex = 0; rextureIndex < this.retexSrc.length; rextureIndex++) {
                inventoryMesh.retexture(this.retexSrc[rextureIndex], this.retexDst[rextureIndex]);
            }
        }
        if (model != null) {
            for (int paletteIndex = 0; paletteIndex < 10; paletteIndex++) {
                for (int colorIndex = 0; colorIndex < PlayerAppearance.recolorPaletteSrc[paletteIndex].length; colorIndex++) {
                    if (model.recolorDstIndices[paletteIndex] < PlayerAppearance.recolorPaletteDst[paletteIndex][colorIndex].length) {
                        inventoryMesh.recolor(PlayerAppearance.recolorPaletteSrc[paletteIndex][colorIndex], PlayerAppearance.recolorPaletteDst[paletteIndex][colorIndex][model.recolorDstIndices[paletteIndex]]);
                    }
                }
            }
        }
        int adjustmentFunctionMask = HAS_TEXTURE_COORDS;
        boolean hasScaling = false;
        if (this.resizeX != 128 || this.resizeY != 128 || this.resizeZ != 128) {
            hasScaling = true;
            adjustmentFunctionMask |= FULL_DISPLACEMENT;
        }
        Model inventoryModel = mainToolkit.createModel(inventoryMesh, adjustmentFunctionMask, DISABLE_TEXTURES, this.ambient + 64, this.contrast * 5 + 768);
        if (!inventoryModel.areTexturesCached()) {
            return null;
        } 
        if (hasScaling) {
            inventoryModel.scale(this.resizeX, this.resizeY, this.resizeZ);
        }
        NativeSprite sprite = null;
        if (this.certTemplateId != -1) {
            sprite = this.typeList.getSprite(mainToolkit, itemToolkit, this.certId, 10, 1, 0, true, true, Component.StackMode.NONUM, fontRenderer, model);
            if (sprite == null) {
                return null;
            }
        } else if (this.lendTemplateId != -1) {
            sprite = this.typeList.getSprite(mainToolkit, itemToolkit, this.lendId, amount, outlineSize, shadowColor, false, true, Component.StackMode.NONUM, fontRenderer, model);
            if (sprite == null) {
                return null;
            }
        } else if (this.bindTemplateId != -1) {
            sprite = this.typeList.getSprite(mainToolkit, itemToolkit, this.bindId, amount, outlineSize, shadowColor, false, true, Component.StackMode.NONUM, fontRenderer, model);
            if (sprite == null) {
                return null;
            }
        }
        int zoom;
        if (zoomedIn) {
            zoom = (int) (1.5D * this.modelZoom) << 2;
        } else if (outlineSize == 2) {
            zoom = (int) (this.modelZoom * 1.04D) << 2;
        } else {
            zoom = this.modelZoom << 2;
        }
        Matrix44 modelMatrix = mainToolkit.copyModelMatrix();
        Matrix44 matrix4418 = mainToolkit.method8449();
        matrix4418.method6531(16.0F, 16.0F, 512.0F, 512.0F, 50.0F, 2.14748365E9F, mainToolkit.getDisplay().getWidth(), mainToolkit.getDisplay().getHeight());
        mainToolkit.setModelMatrix(matrix4418);
        mainToolkit.setDrawableArea(0, 0, mainToolkit.getDisplay().getWidth(), mainToolkit.getDisplay().getHeight());
        Matrix43 sunPosition = new Matrix43();
        mainToolkit.setSunPosition(sunPosition);
        mainToolkit.updateSun(0.95F + (float) (Math.random() / 10.0D));
        mainToolkit.applyLighting(16777215, 0.95F + (float) (Math.random() / 10.0D), 0.95F + (float) (Math.random() / 10.0D), -50.0f, -10.0f, -50.0f);
        Matrix43 itemMatrix = mainToolkit.getRenderingMatrix();
        itemMatrix.rotateMatrix(0.0F, 0.0F, 1.0F, MathUtils.getTheta(-this.modelRotationZ << 3));
        itemMatrix.rotate(0.0F, 1.0F, 0.0F, MathUtils.getTheta(this.modelRotationY << 3));
        itemMatrix.translate((this.modelOffsetX << 2),
        		((zoom * MathUtils.SINE[this.modelRotationX << 3] >> 14) - inventoryModel.getMinY() / 2 + (this.modelOffsetY << 2)),
        		((zoom * MathUtils.COSINE[this.modelRotationX << 3] >> 14) + (this.modelOffsetY << 2)));
        itemMatrix.rotate(1.0F, 0.0F, 0.0F, MathUtils.getTheta(this.modelRotationX << 3));
        mainToolkit.setClipping(0, 0, 36, 32);
        mainToolkit.applyClearFlags(2, 0);
        mainToolkit.drawFilledRectangle(0, 0, 36, 32, 0, 0);
        mainToolkit.calculateFog(0, -1, 0);
        inventoryModel.drawMatrixThreaded(itemMatrix, null, ModelFunctionMaskGL.SMOOTH_EDGES);
        mainToolkit.setModelMatrix(modelMatrix);
        int[] pixels = mainToolkit.copyPixelRectangle(0, 0, 36, 32);
        if (outlineSize >= 1) {
            pixels = outline(pixels, -16777214);
            if (outlineSize >= 2) {
                pixels = outline(pixels, -1);
            }
        }
        if (shadowColor != 0) {
            shadow(pixels, shadowColor);
        }
        if (this.lendTemplateId != -1) {
            sprite.draw(0, 0);
        } else if (this.bindTemplateId != -1) {
            sprite.draw(0, 0);
        }
        mainToolkit.createNativeSprite(pixels, 36, 36, 32).draw(0, 0);
        if (this.certTemplateId != -1) {
            sprite.draw(0, 0);
        }
        if (renderAmounts == Component.StackMode.ALWAYS_NUM || renderAmounts == Component.StackMode.WHEN_MULTIPLE && (this.stackable == 1 || amount != 1) && amount != -1) {
            fontRenderer.renderPlain(TextUtils.formatAmount(amount, this.typeList.language), 0, 9, -256, -16777215);
        }
        pixels = mainToolkit.copyPixelRectangle(0, 0, 36, 32);
        for (int pixelIndex = 0; pixelIndex < pixels.length; pixelIndex++) {
            if ((pixels[pixelIndex] & 0xffffff) == 0) {
                pixels[pixelIndex] = 0;
            } else {
                pixels[pixelIndex] |= ~0xffffff;
            }
        }
        return pixels;      
    }

    public boolean isBodyCached(boolean female, ItemTypeCustomisation customisation) {
        int primaryModel;
        int secondaryModel;
        int tertiaryModel;
        if (female) {
            if (customisation != null) {
                primaryModel = customisation.femaleBody[0];
                secondaryModel = customisation.femaleBody[1];
                tertiaryModel = customisation.femaleBody[2];
            } else {
                primaryModel = this.femaleEquip1;
                secondaryModel = this.femaleEquip2;
                tertiaryModel = this.femaleEquip3;
            }
        } else if (customisation != null) {
            primaryModel = customisation.maleBody[0];
            secondaryModel = customisation.maleBody[1];
            tertiaryModel = customisation.maleBody[2];
        } else {
            primaryModel = this.maleEquip1;
            secondaryModel = this.maleEquip2;
            tertiaryModel = this.maleEquip3;
        }
        if (primaryModel == -1) {
            return true;
        } 
        boolean fullyCached = this.typeList.meshResource.isCached(primaryModel, 0);
        if (secondaryModel != -1 && !this.typeList.meshResource.isCached(secondaryModel, 0)) {
            fullyCached = false;
        }
        if (tertiaryModel != -1 && !this.typeList.meshResource.isCached(tertiaryModel, 0)) {
            fullyCached = false;
        }
        return fullyCached;
    }

    public Mesh getBodyMesh(boolean female, ItemTypeCustomisation meshModifier) {
        int primaryModel;
        int secondaryModel;
        int tertiaryModel;
        if (female) {
            if (meshModifier != null) {
                primaryModel = meshModifier.femaleBody[0];
                secondaryModel = meshModifier.femaleBody[1];
                tertiaryModel = meshModifier.femaleBody[2];
            } else {
                primaryModel = this.femaleEquip1;
                secondaryModel = this.femaleEquip2;
                tertiaryModel = this.femaleEquip3;
            }
        } else if (meshModifier != null) {
            primaryModel = meshModifier.maleBody[0];
            secondaryModel = meshModifier.maleBody[1];
            tertiaryModel = meshModifier.maleBody[2];
        } else {
            primaryModel = this.maleEquip1;
            secondaryModel = this.maleEquip2;
            tertiaryModel = this.maleEquip3;
        }
        if (primaryModel == -1) {
            return null;
        } 
        Mesh primaryMesh = Mesh.fromResource(this.typeList.meshResource, primaryModel);
        if (primaryMesh == null) {
            return null;
        } 
        if (primaryMesh.version < 13) {
            primaryMesh.upscale();
        }
        if (secondaryModel != -1) {
            Mesh secondaryMesh = Mesh.fromResource(this.typeList.meshResource, secondaryModel);
            if (secondaryMesh.version < 13) {
                secondaryMesh.upscale();
            }
            if (tertiaryModel != -1) {
                Mesh tertiaryMesh = Mesh.fromResource(this.typeList.meshResource, tertiaryModel);
                if (tertiaryMesh.version < 13) {
                    tertiaryMesh.upscale();
                }
                Mesh[] meshesToCombine = {primaryMesh, secondaryMesh, tertiaryMesh};
                primaryMesh = new Mesh(meshesToCombine, 3);
            } else {
                Mesh[] meshesToCombine = {primaryMesh, secondaryMesh};
                primaryMesh = new Mesh(meshesToCombine, 2);
            }
        }
        if (!female && (this.maleEquipmentOffsetX != 0 || this.maleEquipmentOffsetY != 0 || this.maleEquipmentOffsetZ != 0)) {
            primaryMesh.translate(this.maleEquipmentOffsetX, this.maleEquipmentOffsetY, this.maleEquipmentOffsetZ);
        }
        if (female && (this.femaleEquipmentOffsetX != 0 || this.femaleEquipmentOffsetY != 0 || this.femaleEquipmentOffsetZ != 0)) {
            primaryMesh.translate(this.femaleEquipmentOffsetX, this.femaleEquipmentOffsetY, this.femaleEquipmentOffsetZ);
        }
        short[] rextureDestination;
        if (this.recolorSrc != null) {
            if (meshModifier != null && meshModifier.recolDst != null) {
                rextureDestination = meshModifier.recolDst;
            } else {
                rextureDestination = this.recolorDst;
            }
            for (int recolorIndex = 0; recolorIndex < this.recolorSrc.length; recolorIndex++) {
                primaryMesh.recolor(this.recolorSrc[recolorIndex], rextureDestination[recolorIndex]);
            }
        }
        if (this.retexSrc != null) {
            if (meshModifier != null && meshModifier.retexDst != null) {
                rextureDestination = meshModifier.retexDst;
            } else {
                rextureDestination = this.retexDst;
            }
            for (int retextureIndex = 0; retextureIndex < this.retexSrc.length; retextureIndex++) {
                primaryMesh.retexture(this.retexSrc[retextureIndex], rextureDestination[retextureIndex]);
            }
        }
        return primaryMesh;
    }

    public boolean isHeadCached(boolean bool1, ItemTypeCustomisation meshModifier) {
        int primaryHeadModel;
        int secondaryHeadModel;
        if (bool1) {
            if (meshModifier != null) {
                primaryHeadModel = meshModifier.femaleHead[0];
                secondaryHeadModel = meshModifier.femaleHead[1];
            } else {
                primaryHeadModel = this.femaleHead1;
                secondaryHeadModel = this.femaleHead2;
            }
        } else if (meshModifier != null) {
            primaryHeadModel = meshModifier.maleHead[0];
            secondaryHeadModel = meshModifier.maleHead[1];
        } else {
            primaryHeadModel = this.maleHead1;
            secondaryHeadModel = this.maleHead2;
        }
        if (primaryHeadModel == -1) {
            return true;
        } 
        boolean fullyCached = this.typeList.meshResource.isCached(primaryHeadModel, 0);
        if (secondaryHeadModel != -1 && !this.typeList.meshResource.isCached(secondaryHeadModel, 0)) {
            fullyCached = false;
        }
        return fullyCached;
    }

    public Mesh getHeadMesh(boolean isFemale, ItemTypeCustomisation meshModifier) {
        int primaryHeadModel;
        int secondaryHealModel;
        if (isFemale) {
            if (meshModifier != null) {
                primaryHeadModel = meshModifier.femaleHead[0];
                secondaryHealModel = meshModifier.femaleHead[1];
            } else {
                primaryHeadModel = this.femaleHead1;
                secondaryHealModel = this.femaleHead2;
            }
        } else if (meshModifier != null) {
            primaryHeadModel = meshModifier.maleHead[0];
            secondaryHealModel = meshModifier.maleHead[1];
        } else {
            primaryHeadModel = this.maleHead1;
            secondaryHealModel = this.maleHead2;
        }
        if (primaryHeadModel == -1) {
            return null;
        } 
        Mesh primaryHeadMesh = Mesh.fromResource(this.typeList.meshResource, primaryHeadModel);
        if (primaryHeadMesh.version < 13) {
            primaryHeadMesh.upscale();
        }
        if (secondaryHealModel != -1) {
            Mesh secondaryHeadMesh = Mesh.fromResource(this.typeList.meshResource, secondaryHealModel);
            if (secondaryHeadMesh.version < 13) {
                secondaryHeadMesh.upscale();
            }
            Mesh[] meshesToCombine = {primaryHeadMesh, secondaryHeadMesh};
            primaryHeadMesh = new Mesh(meshesToCombine, 2);
        }
        if (this.recolorSrc != null) {
        	short[] recolorDestination;
            if (meshModifier != null && meshModifier.recolDst != null) {
                recolorDestination = meshModifier.recolDst;
            } else {
                recolorDestination = this.recolorDst;
            }
            for (int recolorIndex = 0; recolorIndex < this.recolorSrc.length; recolorIndex++) {
                primaryHeadMesh.recolor(this.recolorSrc[recolorIndex], recolorDestination[recolorIndex]);
            }
        }
        if (this.retexSrc != null) {
        	short[] rextureDestination;
            if (meshModifier != null && meshModifier.retexDst != null) {
                rextureDestination = meshModifier.retexDst;
            } else {
                rextureDestination = this.retexDst;
            }
            for (int rextureIndex = 0; rextureIndex < this.retexSrc.length; rextureIndex++) {
                primaryHeadMesh.retexture(this.retexSrc[rextureIndex], rextureDestination[rextureIndex]);
            }
        }
        return primaryHeadMesh;
    }
    
    /**
     * [J]
     */
    public ItemType getStackSizeAlt(int stackSize) {
        if (this.stackIds != null && stackSize > 1) {
            int stackId = -1;
            for (int stackIndex = 0; stackIndex < 10; stackIndex++) {
                if (stackSize >= this.stackTriggerAmount[stackIndex] && this.stackTriggerAmount[stackIndex] != 0) {
                    stackId = this.stackIds[stackIndex];
                }
            }
            if (stackId != -1) {
                return this.typeList.list(stackId);
            }
        }
        return this;
    }
    
    public void generateLend(ItemType template, ItemType original) {
        this.value = 0;
        this.modelId = template.modelId;
        this.modelZoom = template.modelZoom;
        this.modelRotationX = template.modelRotationX;
        this.modelRotationY = template.modelRotationY;
        this.modelRotationZ = template.modelRotationZ;
        this.modelOffsetX = template.modelOffsetX;
        this.modelOffsetY = template.modelOffsetY;
        this.recolorSrc = original.recolorSrc;
        this.recolorDst = original.recolorDst;
        this.recolorDPalette = original.recolorDPalette;
        this.retexSrc = original.retexSrc;
        this.retexDst = original.retexDst;
        this.name = original.name;
        this.membersOnly = original.membersOnly;
        this.wearPos = original.wearPos;
        this.wearPos2 = original.wearPos2;
        this.wearPos3 = original.wearPos3;
        this.maleEquip1 = original.maleEquip1;
        this.maleEquip2 = original.maleEquip2;
        this.maleEquip3 = original.maleEquip3;
        this.femaleEquip1 = original.femaleEquip1;
        this.femaleEquip2 = original.femaleEquip2;
        this.femaleEquip3 = original.femaleEquip3;
        this.maleEquipmentOffsetX = original.maleEquipmentOffsetX;
        this.femaleEquipmentOffsetX = original.femaleEquipmentOffsetX;
        this.maleEquipmentOffsetY = original.maleEquipmentOffsetY;
        this.femaleEquipmentOffsetY = original.femaleEquipmentOffsetY;
        this.maleEquipmentOffsetZ = original.maleEquipmentOffsetZ;
        this.femaleEquipmentOffsetZ = original.femaleEquipmentOffsetZ;
        this.maleHead1 = original.maleHead1;
        this.maleHead2 = original.maleHead2;
        this.femaleHead1 = original.femaleHead1;
        this.femaleHead2 = original.femaleHead2;
        this.teamId = original.teamId;
        this.groundActions = original.groundActions;
        this.parameters = original.parameters;
        this.inventoryActions = new String[5];
        if (original.inventoryActions != null) {
            System.arraycopy(original.inventoryActions, 0, this.inventoryActions, 0, 4);
        }
        this.inventoryActions[4] = LocalizedText.DISCARD.translate(this.typeList.language);
    }
    
    public void generateBind(ItemType template, ItemType original) {
        this.value = 0;
        this.modelId = template.modelId;
        this.modelZoom = template.modelZoom;
        this.modelRotationX = template.modelRotationX;
        this.modelRotationY = template.modelRotationY;
        this.modelRotationZ = template.modelRotationZ;
        this.modelOffsetX = template.modelOffsetX;
        this.modelOffsetY = template.modelOffsetY;
        this.recolorSrc = original.recolorSrc;
        this.recolorDst = original.recolorDst;
        this.recolorDPalette = original.recolorDPalette;
        this.retexSrc = original.retexSrc;
        this.retexDst = original.retexDst;
        this.name = original.name;
        this.membersOnly = original.membersOnly;
        this.wearPos = original.wearPos;
        this.wearPos2 = original.wearPos2;
        this.wearPos3 = original.wearPos3;
        this.maleEquip1 = original.maleEquip1;
        this.maleEquip2 = original.maleEquip2;
        this.maleEquip3 = original.maleEquip3;
        this.femaleEquip1 = original.femaleEquip1;
        this.femaleEquip2 = original.femaleEquip2;
        this.femaleEquip3 = original.femaleEquip3;
        this.maleEquipmentOffsetX = original.maleEquipmentOffsetX;
        this.femaleEquipmentOffsetX = original.femaleEquipmentOffsetX;
        this.maleEquipmentOffsetY = original.maleEquipmentOffsetY;
        this.femaleEquipmentOffsetY = original.femaleEquipmentOffsetY;
        this.maleEquipmentOffsetZ = original.maleEquipmentOffsetZ;
        this.femaleEquipmentOffsetZ = original.femaleEquipmentOffsetZ;
        this.maleHead1 = original.maleHead1;
        this.maleHead2 = original.maleHead2;
        this.femaleHead1 = original.femaleHead1;
        this.femaleHead2 = original.femaleHead2;
        this.teamId = original.teamId;
        this.groundActions = original.groundActions;
        this.parameters = original.parameters;
        this.stackable = original.stackable;
        this.inventoryActions = new String[5];
        if (original.inventoryActions != null) {
            System.arraycopy(original.inventoryActions, 0, this.inventoryActions, 0, 4);
        }
        this.inventoryActions[4] = LocalizedText.DISCARD_2.translate(this.typeList.language);
    }

    public void generateNote(ItemType template, ItemType original) {
        this.modelId = template.modelId;
        this.modelZoom = template.modelZoom;
        this.modelRotationX = template.modelRotationX;
        this.modelRotationY = template.modelRotationY;
        this.modelRotationZ = template.modelRotationZ;
        this.modelOffsetX = template.modelOffsetX;
        this.modelOffsetY = template.modelOffsetY;
        this.recolorSrc = template.recolorSrc;
        this.recolorDst = template.recolorDst;
        this.recolorDPalette = template.recolorDPalette;
        this.retexSrc = template.retexSrc;
        this.retexDst = template.retexDst;
        this.name = original.name;
        this.membersOnly = original.membersOnly;
        this.value = original.value;
        this.stackable = 1;
    }
    
    public int getCs2Integer(int key, int defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } 
        IntNode integerNode = (IntNode) this.parameters.get(key);
        return integerNode == null ? defaultValue : integerNode.value;
    }

    public String getCs2String(int key, String defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } 
        ObjectNode objectNode = (ObjectNode) this.parameters.get(key);
        return objectNode == null ? defaultValue : (String) objectNode.value;
    }
    
    public void shadow(int[] pixels, int color) {
        for (int descendingX = 31; descendingX > 0; --descendingX) {
            int pixelIndexStart = descendingX * 36;
            for (int descendingY = 35; descendingY > 0; --descendingY) {
                if (pixels[descendingY + pixelIndexStart] == 0 && pixels[descendingY + pixelIndexStart - 1 - 36] != 0) {
                    pixels[pixelIndexStart + descendingY] = color;
                }
            }
        }
    }
    
    public static int[] outline(int[] pixels, int outlineColor) {
        int[] outlinedPixels = new int[1152];
        int pixelIndex = 0;
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 36; y++) {
                int pixel = pixels[pixelIndex];
                if (pixel == 0) {
                    if (y > 0 && pixels[pixelIndex - 1] != 0) {
                        pixel = outlineColor;
                    } else if (x > 0 && pixels[pixelIndex - 36] != 0) {
                        pixel = outlineColor;
                    } else if (y < 35 && pixels[pixelIndex + 1] != 0) {
                        pixel = outlineColor;
                    } else if (x < 31 && pixels[pixelIndex + 36] != 0) {
                        pixel = outlineColor;
                    }
                }
                outlinedPixels[pixelIndex++] = pixel;
            }
        }
        return outlinedPixels;
    }
}
