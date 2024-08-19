package com.jagex.game.runetek5.config.object;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.vardomain.VarDomain;

/**
 * [J]
 * @author Steven
 * 
 * [NXT]
	[-]jag::game::LocType::DataSerialiseWrite<jag::Packet>(jag::DataSerialise<jag::Packet> &) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000015C7C0 00000C44 00000148 00000000 R . . . . . .
	[X] jag::game::LocType::DecodeType(jag::Packet &,uint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000004688E0 00001245 00000058 00000000 R . . . . T .
	[-]jag::game::LocType::GetHillData(jag::Array<jag::Array<int>> &,jag::Array<jag::Array<int>> &,jag::game::LocType const&,jag::game::HeightMap const&,int,uint,uint,jag::Rotation90,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 .text  00000000003E79D0 000003DD 000000B8 00000019 R . . . . . .
	[-]jag::game::LocType::GetLocationHeight(jag::game::LocType const&,jag::game::HeightMap const&,int,uint,uint,jag::Rotation90,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  0000000000161A60 000002FA 00000098 0000000C R . . . . . .
	[-]jag::game::LocType::GetLocationOnFlatGround(jag::game::LocType const&,int,jag::game::HeightMap const&,int,uint,uint,jag::Rotation90,bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000161D60 00000123 00000060 00000011 R . . . . . .
	[-]jag::game::LocType::GetMaxRemoveRoofLevel(jag::game::LinkMap const&,int,uint,uint,jag::Rotation90)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000C1880 000000E4 00000038 00000000 R . . . . . .
	[~]jag::game::LocType::GetModels(jag::graphics::RT7Core *,jag::game::AssetBuilder &,jag::game::BuildRequest<jag::graphics::Model,jag::game::BuildModelMain> &,jag::game::LocShape,jag::Rotation90,bool,jag::game::LocTypeCustomisation const*) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  0000000000515BE0 00000991 00000118 000000B0 R . . . . . .
	[X] jag::game::LocType::GetMultiLoc(jag::shared_ptr<jag::game::LocType> &,jag::game::PlayerVarDomain const*,jag::game::ConfigProvider *)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  00000000001112F0 000002AA 00000028 00000000 R . . . . . .
	[-]jag::game::LocType::LocType(jag::game::ConfigTypeListPreloadPackedContainer<jag::game::LocType> *,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  0000000000420040 0000038F 00000018 00000000 R . . . . . .
	[-]jag::game::LocType::MakeLocationSoundData(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               .text  00000000000441C8 0000021F 00000038 00000001 R . . . . . .
	[X] jag::game::LocType::PostDecode(void)    
 *
 */
public class ObjectType {

    public static short[] locRecolorPalette = new short[256];
    public int id;
    
    /**
     * The right click / mini menu actions
     * to interact with this object
     */
    public String[] actions;
    
    public byte[] shapes;
    public int[][] objectModelIds;
    
    /**
     * Hover / minimenu name
     * No actions if 'null'
     */
    public String name = "null";
    
    /**
     * Tiles in width taken up by the model
     */
    public int sizeX = 1;
    
    /**
     * Tiles in length taken up by the model
     */
    public int sizeY = 1;
    
    /**
     * Is an entity blocked from passing through
     * 0 = no
     * 1 = yes (explicit)
     * 2 = yes (default)
     */
    public int blocksMovement = 2;
    
    /**
     * Does an entity have to path around the object
     * when using projectiles (I.E. range/mage)
     */
    public boolean blocksProjectiles = true;
    
    /**
     * Does the object have menu interactions
     * -1 = no (default)
     * 0 = no (explicit)
     * 1 = yes
     */
    public int hasActions = -1;
    
    public boolean delayShading;
    public int occlusionMode = -1;
    public int decorDisplacement = 64;
    public boolean inverted;
    /**
     * Draw low detail (any mode) shadows under objects
     * (shadow only applies on tile below)
     * Shadow is static - always placed the same way
     */
    public boolean staticShadow = true;
    public boolean forceDisplayDecoration;
    public boolean ignoreClipOnAltRoute;
    public int supportsItems = -1;
    public int soundId = -1;
    public int soundRadius;
    public int soundMinInterval;
    public int soundMaxInterval;
    public int[] soundGroupIds;
    public int[] alternativeIds;
    /**
     * Only rendered if textures are enabled
     */
    public boolean requiresTextures;
    /**
     * Draw high detail (gpu mode) shadows under objects
     * (shadow only applies on tile below)
     * Shadow is dynamic - based of the position of sun
     */
    public boolean dynamicShadow = true;
    
    public boolean replaySequence = true;
    
    /**
     * Does a player have to be a member to interact
     * with this object
     */
    public boolean members;
    
    public boolean mapIconRotates;
    public boolean hasAnimation;
    public int primaryCursorActionIndex = -1;
    public int primaryCursor = -1;
    public int secondaryCursorActionIndex = -1;
    public int secondaryCursor = -1;
    public int mapIconRotation;
    public int mapIconId = -1;
    public int soundVolume = 255;
    public boolean mapIconFlipped;
    public int mapCategoryid = -1;
    public int[] quests;
    public int groundDecorationHeight;
    public boolean instrumentSoundEffect;
    public boolean instrumentAmbientSound;
    public int cullY = 960;
    public int cullXZ;
    public int ambientSoundMaxDelay = 256;
    public int ambientSoundMinDelay = 256;
    public boolean transforms;
    
    /**
     * Not sure if this is correct
     */
    public int ambientSoundMaxHearDistance;
    
    /**
     * Tints the object based on the tint
     * set for the current scene
     */
    public boolean dynamicTint;
    
    /**
     * The TypeList used for loading LocTypes,
     * used for fetching if player is member
     * and usage of caches stored there
     */
    ObjectTypeList locTypeList;
    
    /**
     * The type of ground adjustment that has to
     * be performed on the model to display as intended
     */
    public byte groundContourType;
    
    /**
     * The animations the object performs by default.
     * One in the case of static animating, 
     * multiple in the case of randomized animation display
     */
    public int[] animations;
    
    public int ambient;
    public int contrast;
    
    /**
     * Model face colors to find and replace
     * with the same index within {@link #recolorDst}
     */
    public short[] recolorSrc;
    
    /**
     * Model face colors that are replacements for colors
     * with the same index within {@link #recolorSrc}
     */
    public short[] recolorDst;
    
    /**
     * Model face materials to find and replace
     * with the same index within {@link #retextureDst}
     */
    public short[] retextureSrc;
    
    /**
     * Model face materials that are replacements for materials
     * with the same index within {@link #retextureSrc}
     */
    public short[] retextureDst;
    
    
    public byte[] recolorDPalette;
    
    /**
     * Scaling factor for vertices x applied to {@link #objectModelIds}
     * Only scales when value != 128. Default usage of 128 to make minor 
     * scaling easy (Example 129 --> (129 >> 7) * 100 --> 0.78125% scaled
     */
    public int resizeX = 128;
    
    /**
     * Scaling factor for vertices y applied to {@link #objectModelIds}
     * Only scales when value != 128. Default usage of 128 to make minor 
     * scaling easy (Example 129 --> (129 >> 7) * 100 --> 0.78125% scaled
     */
    public int resizeY = 128;
    
    /**
     * Scaling factor for vertices z applied to {@link #objectModelIds}
     * Only scales when value != 128. Default usage of 128 to make minor 
     * scaling easy (Example 129 --> (129 >> 7) * 100 --> 0.78125% scaled
     */
    public int resizeZ = 128;
    public int offsetX;
    public int offsetY;
    public int offsetZ;
    public int varpbitId = -1;
    public int varpId = -1;
    public int goundContourModifier = -1;
    public int[] animationOdds;
    public byte tintHue;
    public byte tintSaturation;
    public byte tintLightness;
    public byte tintOpacity;
    public int shadowOffsetX;
    public int shadowOffsetY;
    public int shadowOffsetZ;
    public IterableNodeMap<Node<?>> parameters;

    /**
     * [J]
     */
    void decodeType(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            decodeOpcode(buffer, opcode);
        }
    }

    public void decodeOpcode(Packet buffer, int opcode) {
        if (opcode == 1) {
            int count = buffer.readUnsignedByte();
            this.shapes = new byte[count];
            this.objectModelIds = new int[count][];
            for (int shape = 0; shape < count; shape++) {
                this.shapes[shape] = buffer.readByte();
                int models = buffer.readUnsignedByte();
                this.objectModelIds[shape] = new int[models];
                for (int model = 0; model < models; model++) {
                    this.objectModelIds[shape][model] = buffer.readBigSmart();
                }
            }
        } else if (opcode == 2) {
            this.name = buffer.readString();
        } else if (opcode == 14) {
            this.sizeX = buffer.readUnsignedByte();
        } else if (opcode == 15) {
            this.sizeY = buffer.readUnsignedByte();
        } else if (opcode == 17) {
            this.blocksMovement = 0;
            this.blocksProjectiles = false;
        } else if (opcode == 18) {
            this.blocksProjectiles = false;
        } else if (opcode == 19) {
            this.hasActions = buffer.readUnsignedByte();
        } else if (opcode == 21) {
            this.groundContourType = 1;
        } else if (opcode == 22) {
            this.delayShading = true;
        } else if (opcode == 23) {
            this.occlusionMode = 1;
        } else if (opcode == 24) {
            int animation = buffer.readBigSmart();
            if (animation != -1) {
                this.animations = new int[]{animation};
            }
        } else if (opcode == 27) {
            this.blocksMovement = 1;
        } else if (opcode == 28) {
            this.decorDisplacement = buffer.readUnsignedByte() << 2;
        } else if (opcode == 29) {
            this.ambient = buffer.readByte();
        } else if (opcode == 39) {
            this.contrast = buffer.readByte() * 5;
        } else if (opcode >= 30 && opcode < 35) {
            this.actions[opcode - 30] = buffer.readString();
        } else if (opcode == 40) {
            int count = buffer.readUnsignedByte();
            this.recolorSrc = new short[count];
            this.recolorDst = new short[count];
            for (int index = 0; index < count; index++) {
                this.recolorSrc[index] = (short) buffer.readUnsignedShort();
                this.recolorDst[index] = (short) buffer.readUnsignedShort();
            }
        } else if (opcode == 41) {
            int count = buffer.readUnsignedByte();
            this.retextureSrc = new short[count];
            this.retextureDst = new short[count];
            for (int index = 0; index < count; index++) {
                this.retextureSrc[index] = (short) buffer.readUnsignedShort();
                this.retextureDst[index] = (short) buffer.readUnsignedShort();
            }
        } else if (opcode == 42) {
            int count = buffer.readUnsignedByte();
            this.recolorDPalette = new byte[count];
            for (int index = 0; index < count; index++) {
                this.recolorDPalette[index] = buffer.readByte();
            }
        } else if (opcode == 62) {
            this.inverted = true;
        } else if (opcode == 64) {
            this.staticShadow = false;
        } else if (opcode == 65) {
            this.resizeX = buffer.readUnsignedShort();
        } else if (opcode == 66) {
            this.resizeY = buffer.readUnsignedShort();
        } else if (opcode == 67) {
            this.resizeZ = buffer.readUnsignedShort();
        } else if (opcode == 69) {
            buffer.readUnsignedByte();
        } else if (opcode == 70) {
            this.offsetX = buffer.readShort() << 2;
        } else if (opcode == 71) {
            this.offsetY = buffer.readShort() << 2;
        } else if (opcode == 72) {
            this.offsetZ = buffer.readShort() << 2;
        } else if (opcode == 73) {
            this.forceDisplayDecoration = true;
        } else if (opcode == 74) {
            this.ignoreClipOnAltRoute = true;
        } else if (opcode == 75) {
            this.supportsItems = buffer.readUnsignedByte();
        } else if (opcode == 77 || opcode == 92) {
            this.varpbitId = buffer.readUnsignedShort();
            if (this.varpbitId == 65535) {
                this.varpbitId = -1;
            }
            this.varpId = buffer.readUnsignedShort();
            if (this.varpId == 65535) {
                this.varpId = -1;
            }
            int lastAlt = -1;
            if (opcode == 92) {
                lastAlt = buffer.readBigSmart();
            }
            int count = buffer.readUnsignedByte();
            this.alternativeIds = new int[count + 2];
            for (int index = 0; index <= count; index++) {
                this.alternativeIds[index] = buffer.readBigSmart();
            }
            this.alternativeIds[count + 1] = lastAlt;
        } else if (opcode == 78) {
            this.soundId = buffer.readUnsignedShort();
            this.soundRadius = buffer.readUnsignedByte();
        } else if (opcode == 79) {
            this.soundMinInterval = buffer.readUnsignedShort();
            this.soundMaxInterval = buffer.readUnsignedShort();
            this.soundRadius = buffer.readUnsignedByte();
            int count = buffer.readUnsignedByte();
            this.soundGroupIds = new int[count];
            for (int index = 0; index < count; index++) {
                this.soundGroupIds[index] = buffer.readUnsignedShort();
            }
        } else if (opcode == 81) {
            this.groundContourType = 2;
            this.goundContourModifier = buffer.readUnsignedByte() * 256;
        } else if (opcode == 82) {
            this.requiresTextures = true;
        } else if (opcode == 88) {
            this.dynamicShadow = false;
        } else if (opcode == 89) {
            this.replaySequence = false;
        } else if (opcode == 91) {
            this.members = true;
        } else if (opcode == 93) {
            this.groundContourType = 3;
            this.goundContourModifier = buffer.readUnsignedShort();
        } else if (opcode == 94) {
            this.groundContourType = 4;
        } else if (opcode == 95) {
            this.groundContourType = 5;
            this.goundContourModifier = buffer.readShort();
        } else if (opcode == 97) {
            this.mapIconRotates = true;
        } else if (opcode == 98) {
            this.hasAnimation = true;
        } else if (opcode == 99) {
            this.primaryCursorActionIndex = buffer.readUnsignedByte();
            this.primaryCursor = buffer.readUnsignedShort();
        } else if (opcode == 100) {
            this.secondaryCursorActionIndex = buffer.readUnsignedByte();
            this.secondaryCursor = buffer.readUnsignedShort();
        } else if (opcode == 101) {
            this.mapIconRotation = buffer.readUnsignedByte();
        } else if (opcode == 102) {
            this.mapIconId = buffer.readUnsignedShort();
        } else if (opcode == 103) {
            this.occlusionMode = 0;
        } else if (opcode == 104) {
            this.soundVolume = buffer.readUnsignedByte();
        } else if (opcode == 105) {
            this.mapIconFlipped = true;
        } else if (opcode == 106) {
            int numAnimations = buffer.readUnsignedByte();
            int sum = 0;
            this.animations = new int[numAnimations];
            this.animationOdds = new int[numAnimations];
            for (int index = 0; index < numAnimations; index++) {
                this.animations[index] = buffer.readBigSmart();
                sum += this.animationOdds[index] = buffer.readUnsignedByte();
            }
            for (int index = 0; index < numAnimations; index++) {
                this.animationOdds[index] = this.animationOdds[index] * 65535 / sum;
            }
        } else if (opcode == 107) {
            this.mapCategoryid = buffer.readUnsignedShort();
        } else if (opcode >= 150 && opcode < 155) {
            this.actions[opcode - 150] = buffer.readString();
            if (!this.locTypeList.playerIsMember) {
                this.actions[opcode - 150] = null;
            }
        } else if (opcode == 160) {
            int count = buffer.readUnsignedByte();
            this.quests = new int[count];
            for (int index = 0; index < count; index++) {
                this.quests[index] = buffer.readUnsignedShort();
            }
        } else if (opcode == 162) {
            this.groundContourType = 3;
            this.goundContourModifier = buffer.readInt();
        } else if (opcode == 163) {
            this.tintHue = buffer.readByte();
            this.tintSaturation = buffer.readByte();
            this.tintLightness = buffer.readByte();
            this.tintOpacity = buffer.readByte();
        } else if (opcode == 164) {
            this.shadowOffsetX = buffer.readShort();
        } else if (opcode == 165) {
            this.shadowOffsetY = buffer.readShort();
        } else if (opcode == 166) {
            this.shadowOffsetZ = buffer.readShort();
        } else if (opcode == 167) {
            this.groundDecorationHeight = buffer.readUnsignedShort();
        } else if (opcode == 168) {
            this.instrumentSoundEffect = true; // ambient sound
        } else if (opcode == 169) {
            this.instrumentAmbientSound = true;
        } else if (opcode == 170) {
            this.cullY = buffer.readUnsignedSmart();
        } else if (opcode == 171) {
            this.cullXZ = buffer.readUnsignedSmart();
        } else if (opcode == 173) {
            this.ambientSoundMaxDelay = buffer.readUnsignedShort();
            this.ambientSoundMinDelay = buffer.readUnsignedShort();
        } else if (opcode == 177) {
            this.transforms = true;
        } else if (opcode == 178) {
            this.ambientSoundMaxHearDistance = buffer.readUnsignedByte();
        } else if (opcode == 189) {
            this.dynamicTint = true;
        } else if (opcode == 249) {
            int count = buffer.readUnsignedByte();
            if (this.parameters == null) {
                int capacity = MathUtils.nextPowerOfTwo(count);
                this.parameters = new IterableNodeMap<Node<?>>(capacity);
            }
            for (int index = 0; index < count; index++) {
                boolean isString = buffer.readUnsignedByte() == 1;
                int key = buffer.read24BitUnsignedInteger();
                Object value;
                if (isString) {
                    value = new ObjectNode(buffer.readString());
                } else {
                    value = new IntNode(buffer.readInt());
                }
                this.parameters.put((Node<?>) value, key);
            }
        }
    }

    /**
     * [J]
     */
    void postDecode() {
        if (this.hasActions == -1) {
            this.hasActions = 0;
            if (this.shapes != null && this.shapes.length == 1 && this.shapes[0] == ObjectShapes.SCENERY_INTERACT.shape) {
                this.hasActions = 1;
            }
            for (int index = 0; index < 5; index++) {
                if (this.actions[index] != null) {
                    this.hasActions = 1;
                    break;
                }
            }
        }
        if (this.supportsItems == -1) {
            this.supportsItems = this.blocksMovement != 0 ? 1 : 0;
        }
        if (isAnimated() || this.hasAnimation || this.alternativeIds != null) {
            this.transforms = true;
        }
    }
    
    public synchronized TypeResult getPair(AbstractToolkit toolkit, int initialFunctionMask, int shape, int rotation, Ground currentLevel, Ground nextLevel, int sceneX, int averageHeight, int sceneY, boolean hasShadow, ObjectTypeCustomisation locCustomisation) {
        if (ObjectShapes.isWallDecoration(shape)) {
            shape = ObjectShapes.STRAIGHT_INSIDE_WALL_DEC.shape;
        }
        long uid = ((long) shape << 3) + rotation + ((long) this.id << 10);
        uid |= (long) toolkit.renderId << 29;
        if (locCustomisation != null) {
            uid |= locCustomisation.id << 32;
        }
        int adjustedFunctionMask = initialFunctionMask;
        if (this.groundContourType == 3) {
            adjustedFunctionMask = initialFunctionMask | DISPLACE_X | DISPLACE_Y | DISPLACE_Z;
        } else {
            if (this.groundContourType != 0 || this.shadowOffsetY != 0) {
                adjustedFunctionMask = initialFunctionMask | DISPLACE_Y;
            }
            if (this.shadowOffsetX != 0) {
                adjustedFunctionMask |= DISPLACE_X;
            }
            if (this.shadowOffsetZ != 0) {
                adjustedFunctionMask |= DISPLACE_Z;
            }
        }
        if (hasShadow) {
            adjustedFunctionMask |= SHADOWED;
        }
        TypeResult pair;
        synchronized (this.locTypeList.typeresultCache) {
            pair = (TypeResult) this.locTypeList.typeresultCache.get(uid);
        }
        Model model = (Model) (pair != null ? pair.mainObject : null);
        Shadow shadow = null;
        if (model != null && toolkit.mismatchingFunctionMask(model.getFunctionMask(), adjustedFunctionMask) == 0) {
            shadow = (Shadow) pair.companionObject;
            if (hasShadow && shadow == null) {
                shadow = (Shadow) (pair.companionObject = model.createShadowSprite(null));
            }
        } else {
            if (model != null) {
                adjustedFunctionMask = toolkit.mergeFunctionMasks(adjustedFunctionMask, model.getFunctionMask());
            }
            int rotatingFunctionMask = adjustedFunctionMask;
            if (shape == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
                rotatingFunctionMask = adjustedFunctionMask | DISPLACE_X | DISPLACE_Z;
            }
            model = getStationaryModel(toolkit, rotatingFunctionMask, shape, rotation, locCustomisation);
            if (model == null) {
                return null;
            }
            if (shape == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
                model.yaxisRotateWithoutNormals(2048);
            }
            if (hasShadow) {
                shadow = model.createShadowSprite(null);
            }
            model.applyShading(adjustedFunctionMask);
            pair = new TypeResult(model, shadow);
            synchronized (this.locTypeList.typeresultCache) {
                this.locTypeList.typeresultCache.put(pair, uid);
            }
        }
        boolean hasGroundContour = this.groundContourType != 0 && (currentLevel != null || nextLevel != null);
        boolean hasTranslation = this.shadowOffsetX != 0 || this.shadowOffsetY != 0 || this.shadowOffsetZ != 0;
        if (hasGroundContour || hasTranslation) {
            model = model.copy((byte) 0, adjustedFunctionMask, true);
            if (hasGroundContour && this.goundContourModifier < 16384) {
                model.contourToGround(this.groundContourType, this.goundContourModifier, currentLevel, nextLevel, sceneX, averageHeight, sceneY);
            }
            if (hasTranslation) {
                model.translate(this.shadowOffsetX, this.shadowOffsetY, this.shadowOffsetZ);
            }
            model.applyShading(initialFunctionMask);
        } else {
            model = model.copy((byte) 0, initialFunctionMask, true);
        }
        this.locTypeList.locresult.mainObject = model;
        this.locTypeList.locresult.companionObject = shadow;
        return this.locTypeList.locresult;
    }

    public Model getStationaryModel(AbstractToolkit toolkit, int functionMask, int type, int rotation, ObjectTypeCustomisation locCustomisation) {
        int adjustedAmbient = 64 + this.ambient;
        int adjustedContrast = 850 + this.contrast;
        int initialFunctionMask = functionMask;
        boolean invert = this.inverted || type == ObjectShapes.WALL_WHOLE_CORNER.shape && rotation > 3;
        if (invert)
            functionMask |= INVERT;
        if (rotation == 0) {
            if (this.resizeX != 128 || this.offsetX != 0)
                functionMask |= DISPLACE_X;
            if (this.resizeZ != 128 || this.offsetZ != 0)
                functionMask |= DISPLACE_Z;
        } else
            functionMask |= ROTATE_WITH_NORMALS;
        if (this.resizeY != 128 || this.offsetY != 0)
            functionMask |= DISPLACE_Y;
        if (this.recolorSrc != null)
            functionMask |= RECOLOR;
        if (this.retextureSrc != null)
            functionMask |= RETEXTURE;
        if (this.tintOpacity != 0)
            functionMask |= TINT;
        Model createdModel = null;
        if (this.shapes != null) {
            int matchingShape = -1;
            for (int shapeIndex = 0; shapeIndex < this.shapes.length; shapeIndex++) {
                if (type == this.shapes[shapeIndex]) {
                    matchingShape = shapeIndex;
                    break;
                }
            }
            if (matchingShape == -1)
                return null;
            int[] modelsByShape = ((locCustomisation != null && locCustomisation.objectModelIds != null)
            		? locCustomisation.objectModelIds : this.objectModelIds[matchingShape]);
            int shapeModelCount = modelsByShape.length;
            if (shapeModelCount > 0) {
                long uid = toolkit.renderId;
                for (int modelIndex = 0; modelIndex < shapeModelCount; modelIndex++)
                    uid = modelsByShape[modelIndex] + 67783L * uid;
                synchronized (this.locTypeList.modelsByShapeCache) {
                    createdModel = (Model) this.locTypeList.modelsByShapeCache.get(uid);
                }
                if (createdModel != null) {
                    if (adjustedAmbient != createdModel.getAmbient())
                        functionMask |= ALTERS_AMBIENT;
                    if (adjustedContrast != createdModel.getContrast())
                        functionMask |= ALTERS_CONTRAST;
                }
                if (createdModel == null || toolkit.mismatchingFunctionMask(createdModel.getFunctionMask(), functionMask) != 0) {
                    int adjustedFunctionMask = functionMask | 0x1f01f;
                    if (createdModel != null)
                        adjustedFunctionMask = toolkit.mergeFunctionMasks(adjustedFunctionMask, createdModel.getFunctionMask());
                    Mesh mesh = null;
                    synchronized (this.locTypeList.meshCombineCache) {
                        for (int shapeIndex = 0; shapeIndex < shapeModelCount; shapeIndex++) {
                            synchronized (this.locTypeList.meshResource) {
                                mesh = Mesh.fromResource(this.locTypeList.meshResource, modelsByShape[shapeIndex]);
                            }
                            if (mesh == null) {
                                return null;
                            }
                            if (mesh.version < 13)
                                mesh.upscale();
                            if (shapeModelCount > 1)
                                this.locTypeList.meshCombineCache[shapeIndex] = mesh;
                        }
                        if (shapeModelCount > 1)
                            mesh = new Mesh(this.locTypeList.meshCombineCache, shapeModelCount);
                    }
                    createdModel = toolkit.createModel(mesh, adjustedFunctionMask, this.locTypeList.preferenceMask, adjustedAmbient, adjustedContrast);
                    synchronized (this.locTypeList.modelsByShapeCache) {
                        this.locTypeList.modelsByShapeCache.put(createdModel, uid);
                    }
                }
            }
        }
        if (createdModel == null)
            return null;
        Model model = createdModel.copy((byte) 0, functionMask, true);
        if (adjustedAmbient != createdModel.getAmbient())
            model.setAmbient(adjustedAmbient);
        if (adjustedContrast != createdModel.getContrast())
            model.setContrast(adjustedContrast);
        if (invert)
            model.mirror();
        if (ObjectShapes.STRAIGHT_INSIDE_WALL_DEC.shape == type && rotation > 3) {
            model.rotateWithNormals(2048);
            model.translate(180, 0, -180);
        }
        rotation &= 0x3;
        if (rotation == 1)
            model.rotateWithNormals(4096);
        else if (rotation == 2)
            model.rotateWithNormals(8192);
        else if (rotation == 3)
            model.rotateWithNormals(12288);
        if (this.recolorSrc != null) {
            short[] destinationColor;
            if (locCustomisation != null && locCustomisation.recolorDst != null)
                destinationColor = locCustomisation.recolorDst;
            else
                destinationColor = this.recolorDst;
            for (int recolorIndex = 0; recolorIndex < this.recolorSrc.length; recolorIndex++) {
                if (this.recolorDPalette != null && recolorIndex < this.recolorDPalette.length)
                    model.recolor(this.recolorSrc[recolorIndex], locRecolorPalette[(this.recolorDPalette[recolorIndex] & 0xff)]);
                else
                    model.recolor(this.recolorSrc[recolorIndex], destinationColor[recolorIndex]);
            }
        }
        if (this.retextureSrc != null) {
            short[] retextureDestination;
            if (locCustomisation != null && locCustomisation.retexDst != null)
                retextureDestination = locCustomisation.retexDst;
            else
                retextureDestination = this.retextureDst;
            for (int retextureIndex = 0; retextureIndex < this.retextureSrc.length; retextureIndex++)
                model.rematerial(this.retextureSrc[retextureIndex], retextureDestination[retextureIndex]);
        }
        if (this.tintOpacity != 0)
            model.tint(this.tintHue, this.tintSaturation, this.tintLightness, this.tintOpacity & 0xff);
        if (this.resizeX != 128 || this.resizeY != 128 || this.resizeZ != 128)
            model.scale(this.resizeX, this.resizeY, this.resizeZ);
        if (this.offsetX != 0 || this.offsetY != 0 || this.offsetZ != 0)
            model.translate(this.offsetX, this.offsetY, this.offsetZ);
        model.applyShading(initialFunctionMask);
        return model;
    }

    public synchronized Model getAnimatedModel(AbstractToolkit toolkit, int functionMask, int type, int rotation, Ground currentLvl, Ground nextLvl, int sceneX, int averageHeight, int sceneY, Sequence sequence, ObjectTypeCustomisation locCustomisation) {
        if (ObjectShapes.isWallDecoration(type)) {
            type = ObjectShapes.STRAIGHT_INSIDE_WALL_DEC.shape;
        }
        long uid = ((long) type << 3) + rotation + ((long) this.id << 10);
        uid |= (long) toolkit.renderId << 29;
        if (locCustomisation != null) {
            uid |= locCustomisation.id << 32;
        }
        if (sequence != null) {
            functionMask |= sequence.getFunctionMask();
        }
        if (this.groundContourType == 3) {
            functionMask |= DISPLACE_X | DISPLACE_Y | DISPLACE_Z;
        } else {
            if (this.groundContourType != 0 || this.shadowOffsetY != 0) {
                functionMask |= DISPLACE_Y;
            }
            if (this.shadowOffsetX != 0) {
                functionMask |= DISPLACE_X;
            }
            if (this.shadowOffsetZ != 0) {
                functionMask |= DISPLACE_Z;
            }
        }
        if (type == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
            functionMask |= DISPLACE_X | DISPLACE_Z;
        }
        Model model;
        synchronized (this.locTypeList.uidToModelCache) {
            model = (Model) this.locTypeList.uidToModelCache.get(uid);
        }
        if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), functionMask) != 0) {
            if (model != null) {
                functionMask = toolkit.mergeFunctionMasks(functionMask, model.getFunctionMask());
            }
            model = getStationaryModel(toolkit, functionMask, type, rotation, locCustomisation);
            if (model == null) {
                return null;
            }            
            synchronized (this.locTypeList.uidToModelCache) {
                this.locTypeList.uidToModelCache.put(model, uid);
            }
        }
        boolean applyShading = false;
        if (sequence != null) {
            model = model.copy((byte) 1, functionMask, true);
            applyShading = true;
            sequence.animateRotatedModel(model, rotation & 0x3);
        }
        if (type == ObjectShapes.SCENERY_INTERACT.shape && rotation > 3) {
            if (!applyShading) {
                model = model.copy((byte) 3, functionMask, true);
                applyShading = true;
            }
            model.yaxisRotateWithoutNormals(2048);
        }
        if (this.groundContourType != 0) {
            if (!applyShading) {
                model = model.copy((byte) 3, functionMask, true);
                applyShading = true;
            }
            model.contourToGround(this.groundContourType, this.goundContourModifier, currentLvl, nextLvl, sceneX, averageHeight, sceneY);
        }
        if (this.shadowOffsetX != 0 || this.shadowOffsetY != 0 || this.shadowOffsetZ != 0) {
            if (!applyShading) {
                model = model.copy((byte) 3, functionMask, true);
                applyShading = true;
            }
            model.translate(this.shadowOffsetX, this.shadowOffsetY, this.shadowOffsetZ);
        }
        if (applyShading) {
            model.applyShading(functionMask);
        }
        return model;
    }
    
    /**
     * [J]
     */
    public ObjectType getMultiLoc(VarDomain vars) {
        int index = -1;
        if (this.varpbitId != -1) {
            index = vars.getVarBitValue(this.varpbitId);
        } else if (this.varpId != -1) {
            index = vars.getVarValue(this.varpId);
        }
        if (index >= 0 && index < this.alternativeIds.length - 1 && this.alternativeIds[index] != -1) {
            return this.locTypeList.list(this.alternativeIds[index]);
        } 
        int id = this.alternativeIds[this.alternativeIds.length - 1];
        return id != -1 ? this.locTypeList.list(id) : null;
    }

    public boolean fullyCached() {
        if (this.objectModelIds == null) {
            return true;
        } 
        boolean cached = true;
        synchronized (this.locTypeList.meshResource) {
            for (int shape = 0; shape < this.objectModelIds.length; shape++) {
                for (int model = 0; model < this.objectModelIds[shape].length; model++) {
                    cached &= this.locTypeList.meshResource.isCached(this.objectModelIds[shape][model], 0);
                }
            }
            return cached;
        }
    }
    
    public static boolean isTypeCached(ObjectTypeList loctypelist, int id, int type) {
        ObjectType loctype = loctypelist.list(id);
        if (type == 11) {
            type = 10;
        }
        if (type >= 5 && type <= 8) {
            type = 4;
        }
        return loctype.isTypeCached(type);
    }
    
    public boolean isTypeCached(int shape) {
        if (this.objectModelIds == null) {
            return true;
        } 
        boolean cached = true;
        synchronized (this.locTypeList.meshResource) {
            for (int shapeIndex = 0; shapeIndex < this.shapes.length; shapeIndex++) {
                if (this.shapes[shapeIndex] == shape) {
                    for (int model = 0; model < this.objectModelIds[shapeIndex].length; model++) {
                        if (!this.locTypeList.meshResource.isCached(this.objectModelIds[shapeIndex][model], 0)) {
                            cached = false;
                        }
                    }
                }
            }
            return cached;
        }
    }

    public boolean hasSound() {
        if (this.alternativeIds == null) {
            return this.soundId != -1 || this.soundGroupIds != null;
        }
        for (int alternativeIndex = 0; alternativeIndex < this.alternativeIds.length; alternativeIndex++) {
            if (this.alternativeIds[alternativeIndex] != -1) {
                ObjectType loctype = this.locTypeList.list(this.alternativeIds[alternativeIndex]);
                if (loctype.soundId != -1 || loctype.soundGroupIds != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public int getParamsInt(int key, int defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } 
        IntNode intnode = (IntNode) this.parameters.get(key);
        return intnode == null ? defaultValue : intnode.value;
    }
    
    public String getParamsString(int paramsKey, String defaultString) {
        if (this.parameters == null) {
            return defaultString;
        } 
        ObjectNode objectNode = (ObjectNode) this.parameters.get(paramsKey);
        return objectNode == null ? defaultString : (String) objectNode.value;
    }

    public int getRandomAnimation() {
        if (this.animations != null) {
            if (this.animations.length <= 1) {
                return this.animations[0];
            }
            int randomCache = (int) (Math.random() * 65535.0D);
            for (int animationIndex = 0; animationIndex < this.animations.length; animationIndex++) {
                if (randomCache <= this.animationOdds[animationIndex]) {
                    return this.animations[animationIndex];
                }
                randomCache -= this.animationOdds[animationIndex];
            }
        }
        return -1;
    }

    public boolean hasSequence(int animationToFind) {
        if (this.animations != null && animationToFind != -1) {
            for (int animationIndex = 0; animationIndex < this.animations.length; animationIndex++) {
                if (this.animations[animationIndex] == animationToFind) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean hasAdditionalAnimations() {
        return this.animations != null && this.animations.length > 1;
    }
    
    public boolean isAnimated() {
        return this.animations != null;
    }

    public int[] getAnimations() {
        return this.animations;
    }
}
