package com.jagex.game.runetek5.config.rsinterface;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.bas.BasTypeList;
import com.jagex.game.runetek5.config.identitykit.IdkTypeList;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.sequence.InterfaceSequence;
import com.jagex.game.runetek5.config.sequence.MovingSequence;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.config.skybox.SkyboxTypeList;
import com.jagex.game.runetek5.config.sun.SunTypeList;
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontCombo;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.font.FontProvider;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.graphics.light.Skybox;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.MediaType;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.Hashing;
import com.jagex.game.runetek5.utils.Utils;
import com.jagex.game.runetek5.vardomain.VarDomain;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

/**
 * @author Steven
 *
 */
public class Component {

    public enum StackMode {
        NONUM,
        ALWAYS_NUM,
        WHEN_MULTIPLE
    }

    public static int anInt1283;
    public static final int anInt1373 = 1;
    public static final int anInt1265 = 2;
    public static final LRUCache NATIVE_SPRITE_CACHE = new LRUCache(3000000, 200);
    public static final LRUCache MODEL_CACHE = new LRUCache(50);
    public static boolean missingMedia;
    public static final LRUCache INTERACTIVE_SPRITES = new LRUCache(8);
    public static final LRUCache COMPONENT_LIGHTING = new LRUCache(4);
    public int modelTintBlue;
    public NPCTypeCustomisation npcCustomisation;
    public int modelTintRed;
    public int modelTintGreen;
    public int modelTintScalar;
    public Object[] anObjectArray1407;
    public int[] anIntArray1408;
    public int serverTriggers;
    public Object[] anObjectArray1418;
    public int[] cycles;
    public int sunRotationX;
    public int sunRotationY;
    public int lightPositionX;
    public int lightPositionY;
    public int lightPositionZ;
    public Sequence animatingSequence;
    public Component[] slotChildren;
    public Component[] itemSlots;
    public ComponentType type;
    public String name;
    public int contentType;
    public int basePositionX;
    public int basePositionY;
    public int baseWidth;
    public int baseHeight;
    public byte aspectWidthType;
    public byte aspectHeightType;
    public byte aspectXType;
    public byte aspectYType;
    public int parent = -1;
    public boolean hidden;
    public int scrollWidth;
    public int scrollHeight;
    public boolean noClickThrough;
    public int spriteId = -1;
    public int angle2d;
    public MediaType mediaType = MediaType.RAW_MODEL;
    public int primaryMediaId; // incorrect, correct naming could be target?
    public boolean tiling;
    public int fontId = -1;
    public String text = "";
    public int color;
    public boolean alpha;
    public int transparency;
    public int borderThickness;
    public int spriteShadow;
    public int anInt1358;
    public int textHorizontalAli;
    public int textVerticalAli;
    public int lineWidth = 1;
    public boolean hasOrigin;
    public boolean monospaced = true;
    public boolean filled;
    public byte[][] keyTriggeringAction;
    public byte[][] triggeredKeyFunctionMasks;
    public int[] cycleSteps;
    public int[] anIntArray1267;
    public String useOnName;
    public boolean flipVertical;
    public boolean shadow;
    public boolean lineDirection;
    public String[] optionNames;
    public boolean usesOrthogonal;
    public int maxTextLines;
    public int[] opCursors;
    public boolean flipHorizontal;
    public String opName;
    public boolean priorityRender;
    public boolean hasInteraction;
    public int anInt1380;
    public int anInt1381;
    public int anInt1382;
    public String useOptionString;
    public int originX;
    public int originY;
    public int spritePitch;
    public int spriteRoll;
    public int spriteYaw;
    public int spriteScale = 100;
    public boolean clickMask = true;
    public int originZ;
    public int animation;
    public int targetOverCursor = -1;
    public int moveOverCursor = -1;
    public IFEvents targetParams;
    public int aspectWidth;
    public int targetLeaveCursor = -1;
    public Object[] onLoadScript;
    public Object[] onMouseHoverScript;
    public Object[] onMouseLeaveScript;
    public Object[] hookParams;
    public Object[] anObjectArray1400;
    public Object[] anObjectArray1397;
    public Object[] mouseLeaveScript;
    public Object[] anObjectArray1387;
    public Object[] anObjectArray1409;
    public Object[] params;
    public int aspectHeight;
    public Object[] anObjectArray1393;
    public Object[] popupScript;
    public Object[] anObjectArray1386;
    public Object[] anObjectArray1319;
    public Object[] anObjectArray1302;
    public Object[] anObjectArray1389;
    public Object[] anObjectArray1451;
    public Object[] anObjectArray1394;
    public Object[] anObjectArray1412;
    public Object[] anObjectArray1403;
    public Object[] anObjectArray1405;
    public int[] varps;
    public int[] mouseLeaveArrayParams;
    public int[] anIntArray1402;
    public int[] anIntArray1315;
    public int[] anIntArray1406;
    public Object[] anObjectArray1413;
    public Object[] anObjectArray1292;
    public Object[] anObjectArray1415;
    public Object[] anObjectArray1416;
    public Object[] anObjectArray1383;
    public Object[] anObjectArray1419;
    public Object[] anObjectArray1361;
    public Object[] hookrequestType0Params;
    public Object[] hookrequestType1Params;
    public Object[] anObjectArray1353;
    public Object[] anObjectArray1271;
    public boolean hasScript;
    public ParticleSystem particleSystem;
    public int idHash = -1;
    public int slotId = -1;
    public int x;
    public int y;
    public int width;
    public int height;
    public int aspectX = 1;
    public int aspectY = 1;
    public int scrollX;
    public int scrollY;
    public int secondaryMediaId = -1;
    public int offsetX;
    public int offsetY;
    public StackMode renderStack = StackMode.WHEN_MULTIPLE;
    public boolean textAntiMacro;
    public int clickType;
    public Component depthChildOverride;
    public int containerItemId;
    public int itemStackSize;
    public boolean wearCol;
    public int skyboxId;
    public int anInt1435;
    public boolean isHooked;
    public boolean aBool1286;
    public int anInt1442;
    public int anInt1443;
    public int anInt1444;
    public int anInt1445;
    public int anInt1446;
    public int anInt1447;
    public int anInt1448;
    public int cacheReference;
    public int lastRenderedCycle;
    public IterableNodeMap aClass4651365;
    public short[] recolorSrc;
    public short[] recolorDst;
    public short[] retextureSrc;
    public short[] retextureDst;
	public static final IFEvents DEFAULT_SETTINGS = new IFEvents(0, -1);
	public static final NodeCollection ITEMS = new NodeCollection();

    public Component() {
        this.targetParams = Component.DEFAULT_SETTINGS;
        this.hasInteraction = false;
        this.useOnName = "";
        this.clickType = -1;
        this.depthChildOverride = null;
        this.anInt1380 = 0;
        this.anInt1381 = 0;
        this.anInt1382 = anInt1283;
        this.useOptionString = "";
        this.hasScript = false;
        this.containerItemId = -1;
        this.itemStackSize = 0;
        this.wearCol = false;
        this.skyboxId = -1;
        this.anInt1435 = -1;
        this.animation = -1;
        this.isHooked = false;
        this.aBool1286 = false;
        this.anInt1442 = -1;
        this.anInt1443 = 0;
        this.anInt1444 = 0;
        this.anInt1445 = 0;
        this.anInt1446 = 0;
        this.anInt1447 = 0;
        this.anInt1448 = 0;
        this.cacheReference = -1;
        this.lastRenderedCycle = -1;
    }

    public static Component fromHash(int hash) {
        int interfaceId = hash >> 16;
        if (Interface.INTERFACES[interfaceId] == null || Interface.INTERFACES[interfaceId].getComponent(hash) == null) {
            boolean loaded = Interface.interfaceLoaded(interfaceId, null);
            if (!loaded) {
                return null;
            }
        }
        return Interface.INTERFACES[interfaceId].getComponent(hash);
    }
    
    public Model getAnimatedMediaModel(AbstractToolkit toolkit, int functionMask, BasTypeList bastypelist, IdkTypeList idktypelist, NpcTypeList npctypelist, ItemTypeList objtypelist, VarDomain varprovider, Sequence sequence, PlayerAppearance playerappearance) {
        missingMedia = false;
        if (this.mediaType == MediaType.UNDEFINED) {
            return null;
        } 
        if (this.mediaType == MediaType.RAW_MODEL && this.primaryMediaId == -1) {
            return null;
        } 
        if (this.mediaType == MediaType.RAW_MODEL) {
            if (sequence != null) {
                functionMask |= sequence.getFunctionMask();
            }
            long crc64 = -1L;
            long[] crcTable = Hashing.CRC64_TABLE;
            if (this.recolorSrc != null) {
                for (int index = 0; index < this.recolorSrc.length; index++) {
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ (this.recolorSrc[index] >> 8)) & 0xffL)];
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ this.recolorSrc[index]) & 0xffL)];
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ (this.recolorDst[index] >> 8)) & 0xffL)];
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ this.recolorDst[index]) & 0xffL)];
                }
                functionMask |= RECOLOR;
            }
            if (this.retextureSrc != null) {
                for (int index = 0; index < this.retextureSrc.length; index++) {
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ (this.retextureSrc[index] >> 8)) & 0xffL)];
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ this.retextureSrc[index]) & 0xffL)];
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ (this.retextureDst[index] >> 8)) & 0xffL)];
                    crc64 = crc64 >>> 8 ^ crcTable[(int) ((crc64 ^ this.retextureDst[index]) & 0xffL)];
                }
                functionMask |= RETEXTURE;
            }
            long uid = (long) toolkit.renderId << 59 | (long) this.mediaType.getId() << 54 | (long) this.primaryMediaId << 38 | crc64 & 0x3fffffffffL;
            Model model = (Model) MODEL_CACHE.get(uid);
            if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), functionMask) != 0) {
                if (model != null) {
                    functionMask = toolkit.mergeFunctionMasks(functionMask, model.getFunctionMask());
                }
                Mesh mesh = Mesh.fromResource(Resource.MESH, this.primaryMediaId);
                if (mesh == null) {
                    missingMedia = true;
                    return null;
                }
                if (mesh.version < 13) {
                    mesh.upscale();
                }
                model = toolkit.createModel(mesh, functionMask, Interface.preferenceMask, 64, 768);
                if (this.recolorSrc != null) {
                    for (int index = 0; index < this.recolorSrc.length; index++) {
                        model.recolor(this.recolorSrc[index], this.recolorDst[index]);
                    }
                }
                if (this.retextureSrc != null) {
                    for (int index = 0; index < this.retextureSrc.length; index++) {
                        model.rematerial(this.retextureSrc[index], this.retextureDst[index]);
                    }
                }
                MODEL_CACHE.put(model, uid);
            }
            if (sequence != null) {
                model = model.copy((byte) 1, functionMask, true);
                sequence.animateRotatedModel(model, 0);
            }
            model.applyShading(functionMask);
            return model;
        } else {
            if (this.mediaType == MediaType.NPC_HEAD) {
            	Model npcHeadModel = npctypelist.list(this.primaryMediaId).getAnimatedHeadmodel(toolkit, functionMask, varprovider, sequence, this.npcCustomisation);
                if (npcHeadModel == null) {
                    missingMedia = true;
                    return null;
                } 
                return npcHeadModel;
            } else if (this.mediaType == MediaType.PLAYER_HEAD) {
                if (playerappearance == null) {
                    return null;
                } 
            	Model decoratedHeadModel = playerappearance.getAnimatedHeadModel(toolkit, functionMask, idktypelist, npctypelist, objtypelist, varprovider, sequence);
                if (decoratedHeadModel == null) {
                    missingMedia = true;
                    return null;
                } 
                return decoratedHeadModel;
            } else if (this.mediaType == MediaType.ITEM) {
                ItemType objtype = objtypelist.list(this.primaryMediaId);
                Model objModel = objtype.getAnimatedModel(toolkit, functionMask, 10, playerappearance, sequence, 0, 0, 0, 0);
                if (objModel == null) {
                    missingMedia = true;
                    return null;
                } 
                return objModel;
            } else if (this.mediaType == MediaType.NPC_MODEL) {
            	Model npcBodyModel = npctypelist.list(this.primaryMediaId).getAnimatedBodymodel(toolkit, functionMask, bastypelist, varprovider, sequence, null, null, null, 0, this.npcCustomisation);
                if (npcBodyModel == null) {
                    missingMedia = true;
                    return null;
                } 
                return npcBodyModel;
            } else if (this.mediaType == MediaType.PLAYER_HEAD_IGNOREWORN) {
                if (playerappearance == null) {
                    return null;
                } 
                int primaryHead = this.primaryMediaId >>> 16;
                int secondaryHead = this.primaryMediaId & 0xffff;
                int tertiaryHead = this.secondaryMediaId;
                Model animatedHeadModel = playerappearance.getPlainAnimatedHeadModel(toolkit, functionMask, idktypelist, sequence, primaryHead, secondaryHead, tertiaryHead);
                if (animatedHeadModel == null) {
                    missingMedia = true;
                    return null;
                } 
                return animatedHeadModel;
            } 
            return null;
        }
    }
    

    public NativeSprite draw(AbstractToolkit toolkit) {
        missingMedia = false;
        long spriteHash = ((this.flipHorizontal ? 1L : 0L) << 39) + ((this.alpha ? 1L : 0L) << 35) + this.spriteId + ((long) this.borderThickness << 36) + ((this.flipVertical ? 1L : 0L) << 38) + ((long) this.spriteShadow << 40);
        NativeSprite nativeSprite = (NativeSprite) NATIVE_SPRITE_CACHE.get(spriteHash);
        if (nativeSprite != null) {
            return nativeSprite;
        } 
        SpriteType spriteType = SpriteType.listSingle(StaticMethods.SPRITES_INDEX, this.spriteId, 0);
        if (spriteType == null) {
            missingMedia = true;
            return null;
        } 
        if (this.flipVertical) {
            spriteType.flipVertical();
        }
        if (this.flipHorizontal) {
            spriteType.flipHorizontal();
        }
        if (this.borderThickness > 0) {
            spriteType.drawSmallBorder(this.borderThickness);
        } else if (this.spriteShadow != 0) {
            spriteType.drawSmallBorder(1);
        }
        if (this.borderThickness >= 1) {
            spriteType.drawThickBorder(1);
        }
        if (this.borderThickness >= 2) {
            spriteType.drawThickBorder(16777215);
        }
        if (this.spriteShadow != 0) {
            spriteType.outline(-16777216 | this.spriteShadow);
        }
        nativeSprite = toolkit.generateSprite(spriteType, true);
        NATIVE_SPRITE_CACHE.put(nativeSprite, spriteHash, nativeSprite.getWidth() * nativeSprite.getHeight() * 4);
        return nativeSprite;
    }
    
	public static Component listIcomponent(int interfaceId, int slotId) {
	    Component component = fromHash(interfaceId);
	    if (slotId == -1)
	        return component;
	    if (component != null && component.slotChildren != null && slotId < component.slotChildren.length)
	        return component.slotChildren[slotId];
	    return null;
	}

    public void recolor(int index, short srcColor, short dstColor) {
        if (index < 5) {
            if (this.recolorSrc == null) {
                this.recolorSrc = new short[5];
                this.recolorDst = new short[5];
            }
            this.recolorSrc[index] = srcColor;
            this.recolorDst[index] = dstColor;
        }
    }

    public void retexture(int index, short srcTexture, short dstTexture) {
        if (index < 5) {
            if (this.retextureSrc == null) {
                this.retextureSrc = new short[5];
                this.retextureDst = new short[5];
            }
            this.retextureSrc[index] = srcTexture;
            this.retextureDst[index] = dstTexture;
        }
    }

    public static void prepareSequenceOnInterface(Component component) {
        if (component.contentType == ComponentContentType.CONTENT_TYPE_328) {
            if (PlayerUpdate.MY_PLAYER.displayName == null) {
                component.primaryMediaId = 0;
                component.secondaryMediaId = 0;
            } else {
                component.spritePitch = 150;
                component.spriteRoll = (int) (Math.sin(client.cycles / 40.0D) * 256.0D) & 0x7ff;
                component.mediaType = MediaType.PLAYER_MODEL;
                component.primaryMediaId = PlayerUpdate.myPlayerIndex;
                component.secondaryMediaId = Utils.stringToInt(PlayerUpdate.MY_PLAYER.displayName);
                MovingSequence movingSequence = PlayerUpdate.MY_PLAYER.movingSequence;
                if (movingSequence != null) {
                    if (component.animatingSequence == null) {
                        component.animatingSequence = new InterfaceSequence();
                    }
                    component.animation = movingSequence.getId();
                    component.animatingSequence.copy(movingSequence);
                } else {
                    component.animatingSequence = null;
                }
            }
        }
    }

    public static void processCacheCycles() {
        NATIVE_SPRITE_CACHE.clean(50);
        MODEL_CACHE.clean(50);
        INTERACTIVE_SPRITES.clean(50);
        COMPONENT_LIGHTING.clean(50);
    }

    public static void update(Component[] components, int parent) {
        for (int index = 0; index < components.length; index++) {
            Component component = components[index];
            if (component != null && component.parent == parent && !InterfaceRenderer.isComponentHidden(component)) {
                if (component.type == ComponentType.CONTAINER) {
                    update(components, component.idHash);
                    if (component.itemSlots != null) {
                        update(component.itemSlots, component.idHash);
                    }
                    SubInterface subinterface = client.OPEN_INTERFACES.get(component.idHash);
                    if (subinterface != null) {
                        Component.update(subinterface.subInterfaceId);
                    }
                }
                if (component.type == ComponentType.MODEL && component.animation != -1) {
                    if (component.animatingSequence == null) {
                        component.animatingSequence = new InterfaceSequence();
                        component.animatingSequence.update(component.animation);
                    }
                    if (component.animatingSequence.progressCycle(client.animatingStep) && component.animatingSequence.needsReset()) {
                        component.animatingSequence.reset();
                    }
                }
            }
        }
    }

    public static void resetCaches() {
        NATIVE_SPRITE_CACHE.reset();
        MODEL_CACHE.reset();
        INTERACTIVE_SPRITES.reset();
        COMPONENT_LIGHTING.reset();
    }

    public static int getCursor(Component component, int option) {
        return !client.listComponentSettings(component).clickOptionEnabled(option) && component.params == null
        		? -1 : (component.opCursors != null && component.opCursors.length > option ? component.opCursors[option] : -1);
    }

    public static void drawComponentObjSprite(Component component) {
        if (component.type == ComponentType.SPRITE && component.containerItemId != -1) {
            InterfaceComponentRenderer.drawObjSprite(Renderers.activeToolkit, component);
        }
    }

    public static void clearQueues() {
        NATIVE_SPRITE_CACHE.resize();
        MODEL_CACHE.resize();
        INTERACTIVE_SPRITES.resize();
        COMPONENT_LIGHTING.resize();
    }

    public static String componentMenuOption(Component component, int option) {
        return !client.listComponentSettings(component).clickOptionEnabled(option)
        		&& component.params == null ? null 
        		: (component.optionNames != null && component.optionNames.length > option && component.optionNames[option] != null 
        			&& !component.optionNames[option].trim().isEmpty() 
        				? component.optionNames[option] 
        				: (client.DISPLAY_HIDDEN_MENU_OPTIONS ? "Hidden-" + option : null));
    }

    public static Component getDepthChild(Component icomponentdefinitions0) {
        Component icomponentdefinitions2 = client.method11634(icomponentdefinitions0);
        if (icomponentdefinitions2 == null) {
            icomponentdefinitions2 = icomponentdefinitions0.depthChildOverride;
        }

        return icomponentdefinitions2;
    }

    public void decode(Packet buffer) {
        int i_3 = buffer.readUnsignedByte();
        if (i_3 == 255) {
            i_3 = -1;
        }
        int typeId = buffer.readUnsignedByte();
        if ((typeId & 0x80) != 0) {
            typeId &= 0x7f;
            this.name = buffer.readString();
        }
        this.type = ComponentType.forId(typeId);
        this.contentType = buffer.readUnsignedShort();
        this.basePositionX = buffer.readShort();
        this.basePositionY = buffer.readShort();
        this.baseWidth = buffer.readUnsignedShort();
        this.baseHeight = buffer.readUnsignedShort();
        this.aspectWidthType = buffer.readByte();
        this.aspectHeightType = buffer.readByte();
        this.aspectXType = buffer.readByte();
        this.aspectYType = buffer.readByte();
        this.parent = buffer.readUnsignedShort();
        if (this.parent == 65535) {
            this.parent = -1;
        } else {
            this.parent += this.idHash & -65536;
        }
        int interationMask = buffer.readUnsignedByte();
        this.hidden = (interationMask & 0x1) != 0;
        if (i_3 >= 0) {
            this.noClickThrough = (interationMask & 0x2) != 0;
        }
        if (this.type == ComponentType.CONTAINER) {
            this.scrollWidth = buffer.readUnsignedShort();
            this.scrollHeight = buffer.readUnsignedShort();
            if (i_3 < 0) {
                this.noClickThrough = buffer.readUnsignedByte() == 1;
            }
        }
        if (this.type == ComponentType.SPRITE) {
            this.spriteId = buffer.readInt();
            this.angle2d = buffer.readUnsignedShort();
            int displayMask = buffer.readUnsignedByte();
            this.tiling = (displayMask & 0x1) != 0;
            this.alpha = (displayMask & 0x2) != 0;
            this.transparency = buffer.readUnsignedByte();
            this.borderThickness = buffer.readUnsignedByte();
            this.spriteShadow = buffer.readInt();
            this.flipVertical = buffer.readUnsignedByte() == 1;
            this.flipHorizontal = buffer.readUnsignedByte() == 1;
            this.color = buffer.readInt();
            if (i_3 >= 3) {
                this.clickMask = buffer.readUnsignedByte() == 1;
            }
        }
        if (this.type == ComponentType.MODEL) {
            this.mediaType = MediaType.RAW_MODEL;
            this.primaryMediaId = buffer.readBigSmart();
            int flag2 = buffer.readUnsignedByte();
            boolean bool6 = (flag2 & 0x1) == 1;
            this.hasOrigin = (flag2 & 0x2) == 2;
            this.usesOrthogonal = (flag2 & 0x4) == 4;
            this.priorityRender = (flag2 & 0x8) == 8;
            if (bool6) {
                this.originX = buffer.readShort();
                this.originY = buffer.readShort();
                this.spritePitch = buffer.readUnsignedShort();
                this.spriteRoll = buffer.readUnsignedShort();
                this.spriteYaw = buffer.readUnsignedShort();
                this.spriteScale = buffer.readUnsignedShort();
            } else if (this.hasOrigin) {
                this.originX = buffer.readShort();
                this.originY = buffer.readShort();
                this.originZ = buffer.readShort();
                this.spritePitch = buffer.readUnsignedShort();
                this.spriteRoll = buffer.readUnsignedShort();
                this.spriteYaw = buffer.readUnsignedShort();
                this.spriteScale = buffer.readShort();
            }
            this.animation = buffer.readBigSmart();
            if (this.aspectWidthType != 0) {
                this.aspectWidth = buffer.readUnsignedShort();
            }
            if (this.aspectHeightType != 0) {
                this.aspectHeight = buffer.readUnsignedShort();
            }
        }
        if (this.type == ComponentType.TEXT) {
            this.fontId = buffer.readBigSmart();
            if (i_3 >= 2) {
                this.monospaced = buffer.readUnsignedByte() == 1;
            }
            this.text = buffer.readString();
            if (this.text.toLowerCase().contains("runescape")) {
                this.text = this.text.replace("runescape", "Darkan");
                this.text = this.text.replace("RuneScape", "Darkan");
                this.text = this.text.replace("Runescape", "Darkan");
            }
            this.anInt1358 = buffer.readUnsignedByte();
            this.textHorizontalAli = buffer.readUnsignedByte();
            this.textVerticalAli = buffer.readUnsignedByte();
            this.shadow = buffer.readUnsignedByte() == 1;
            this.color = buffer.readInt();
            this.transparency = buffer.readUnsignedByte();
            if (i_3 >= 0) {
                this.maxTextLines = buffer.readUnsignedByte();
            }
        }
        if (this.type == ComponentType.FIGURE) {
            this.color = buffer.readInt();
            this.filled = buffer.readUnsignedByte() == 1;
            this.transparency = buffer.readUnsignedByte();
        }
        if (this.type == ComponentType.LINE) {
            this.lineWidth = buffer.readUnsignedByte();
            this.color = buffer.readInt();
            this.lineDirection = buffer.readUnsignedByte() == 1;
        }
        int optionMask = buffer.read24BitUnsignedInteger();
        int i_16 = buffer.readUnsignedByte();
        int i_7;
        if (i_16 != 0) {
            this.keyTriggeringAction = new byte[11][];
            this.triggeredKeyFunctionMasks = new byte[11][];
            this.cycleSteps = new int[11];
            for (this.anIntArray1267 = new int[11]; i_16 != 0; i_16 = buffer.readUnsignedByte()) {
                i_7 = (i_16 >> 4) - 1;
                i_16 = i_16 << 8 | buffer.readUnsignedByte();
                i_16 &= 0xfff;
                if (i_16 == 4095) {
                    i_16 = -1;
                }
                byte b_8 = buffer.readByte();
                if (b_8 != 0) {
                    this.hasInteraction = true;
                }
                byte b_9 = buffer.readByte();
                this.cycleSteps[i_7] = i_16;
                this.keyTriggeringAction[i_7] = new byte[]{b_8};
                this.triggeredKeyFunctionMasks[i_7] = new byte[]{b_9};
            }
        }
        this.useOnName = buffer.readString();
        i_7 = buffer.readUnsignedByte();
        int i_17 = i_7 & 0xf;
        int i_18 = i_7 >> 4;
        int i_10;
        if (i_17 > 0) {
            this.optionNames = new String[i_17];
            for (i_10 = 0; i_10 < i_17; i_10++) {
                this.optionNames[i_10] = buffer.readString();
            }
        }
        int i_11;
        if (i_18 > 0) {
            i_10 = buffer.readUnsignedByte();
            this.opCursors = new int[i_10 + 1];
            for (i_11 = 0; i_11 < this.opCursors.length; i_11++) {
                this.opCursors[i_11] = -1;
            }
            this.opCursors[i_10] = buffer.readUnsignedShort();
        }
        if (i_18 > 1) {
            i_10 = buffer.readUnsignedByte();
            this.opCursors[i_10] = buffer.readUnsignedShort();
        }
        this.opName = buffer.readString();
        if (this.opName.isEmpty()) {
            this.opName = null;
        }
        this.anInt1380 = buffer.readUnsignedByte();
        this.anInt1381 = buffer.readUnsignedByte();
        this.anInt1382 = buffer.readUnsignedByte();
        this.useOptionString = buffer.readString();
        i_10 = -1;
        if (Component.getUseOptionFlags(optionMask) != 0) {
            i_10 = buffer.readUnsignedShort();
            if (i_10 == 65535) {
                i_10 = -1;
            }
            this.targetOverCursor = buffer.readUnsignedShort();
            if (this.targetOverCursor == 65535) {
                this.targetOverCursor = -1;
            }
            this.targetLeaveCursor = buffer.readUnsignedShort();
            if (this.targetLeaveCursor == 65535) {
                this.targetLeaveCursor = -1;
            }
        }
        if (i_3 >= 0) {
            this.moveOverCursor = buffer.readUnsignedShort();
            if (this.moveOverCursor == 65535) {
                this.moveOverCursor = -1;
            }
        }
        this.targetParams = new IFEvents(optionMask, i_10);
        if (i_3 >= 0) {
            i_11 = buffer.readUnsignedByte();
            int i_12;
            int i_13;
            int i_14;
            for (i_12 = 0; i_12 < i_11; i_12++) {
                i_13 = buffer.read24BitUnsignedInteger();
                i_14 = buffer.readInt();
                this.aClass4651365.put(new IntNode(i_14), i_13);
            }
            i_12 = buffer.readUnsignedByte();
            for (i_13 = 0; i_13 < i_12; i_13++) {
                i_14 = buffer.read24BitUnsignedInteger();
                String string_15 = buffer.readGJString();
                this.aClass4651365.put(new ObjectNode(string_15), i_14);
            }
        }
        this.onLoadScript = decodeScript(buffer);
        this.onMouseHoverScript = decodeScript(buffer);
        this.onMouseLeaveScript = decodeScript(buffer);
        this.hookParams = decodeScript(buffer);
        this.anObjectArray1400 = decodeScript(buffer);
        this.anObjectArray1397 = decodeScript(buffer);
        this.mouseLeaveScript = decodeScript(buffer);
        this.anObjectArray1387 = decodeScript(buffer);
        this.anObjectArray1409 = decodeScript(buffer);
        this.params = decodeScript(buffer);
        if (i_3 >= 0) {
            this.anObjectArray1393 = decodeScript(buffer);
        }
        this.popupScript = decodeScript(buffer);
        this.anObjectArray1386 = decodeScript(buffer);
        this.anObjectArray1319 = decodeScript(buffer);
        this.anObjectArray1302 = decodeScript(buffer);
        this.anObjectArray1389 = decodeScript(buffer);
        this.anObjectArray1451 = decodeScript(buffer);
        this.anObjectArray1394 = decodeScript(buffer);
        this.anObjectArray1412 = decodeScript(buffer);
        this.anObjectArray1403 = decodeScript(buffer);
        this.anObjectArray1405 = decodeScript(buffer);
        this.varps = decodeScript3(buffer);
        this.mouseLeaveArrayParams = decodeScript3(buffer);
        this.anIntArray1402 = decodeScript3(buffer);
        this.anIntArray1315 = decodeScript3(buffer);
        this.anIntArray1406 = decodeScript3(buffer);
    }

    public void reset() {
        this.onLoadScript = null;
        this.anObjectArray1386 = null;
        this.anObjectArray1319 = null;
        this.anObjectArray1302 = null;
        this.anObjectArray1389 = null;
        this.onMouseHoverScript = null;
        this.popupScript = null;
        this.onMouseLeaveScript = null;
        this.anObjectArray1451 = null;
        this.anObjectArray1394 = null;
        this.anObjectArray1400 = null;
        this.hookParams = null;
        this.anObjectArray1397 = null;
        this.varps = null;
        this.mouseLeaveScript = null;
        this.mouseLeaveArrayParams = null;
        this.anObjectArray1387 = null;
        this.anIntArray1402 = null;
        this.anObjectArray1403 = null;
        this.anIntArray1315 = null;
        this.anObjectArray1405 = null;
        this.anIntArray1406 = null;
        this.anObjectArray1409 = null;
        this.params = null;
        this.anObjectArray1393 = null;
        this.anObjectArray1412 = null;
        this.anObjectArray1413 = null;
        this.anObjectArray1292 = null;
        this.anObjectArray1415 = null;
        this.anObjectArray1416 = null;
        this.anObjectArray1383 = null;
        this.anObjectArray1419 = null;
        this.anObjectArray1361 = null;
        this.hookrequestType0Params = null;
        this.hookrequestType1Params = null;
        this.anObjectArray1353 = null;
        this.anObjectArray1271 = null;
    }

    public AbstractFontRenderer getFontRenderer(FontCombo fontCombo, FontProvider fontProvider) {
        AbstractFontRenderer fontRenderer = (AbstractFontRenderer) fontCombo.list(fontProvider, this.fontId, false, this.monospaced);
        missingMedia = fontRenderer == null;
        return fontRenderer;
    }

    public FontMetrics getFontMetrics(FontCombo fontCombo, FontProvider fontProvider) {
        FontMetrics font = fontCombo.list(fontProvider, this.fontId);
        missingMedia = font == null;
        return font;
    }

    public Object[] decodeScript(Packet buffer) {
        int scriptCount = buffer.readUnsignedByte();
        if (scriptCount == 0) {
            return null;
        } 
        Object[] components = new Object[scriptCount];
        for (int index = 0; index < scriptCount; index++) {
            int type = buffer.readUnsignedByte();
            if (type == 0) {
                components[index] = Integer.valueOf(buffer.readInt());
            } else if (type == 1) {
                components[index] = buffer.readString();
            }
        }
        this.hasScript = true;
        return components;
    }
    
    public int[] decodeScript3(Packet buffer) {
        int count = buffer.readUnsignedByte();
        if (count == 0) {
            return null;
        } 
        int[] ints_4 = new int[count];
        for (int i_5 = 0; i_5 < count; i_5++) {
            ints_4[i_5] = buffer.readInt();
        }
        return ints_4;    
    }

    public void cycleParticles(AbstractToolkit toolkit, Model model, Matrix43 matrix4x4, int cycle) {
        model.updateParticlePosition(matrix4x4);
        EmissiveTriangle[] emitters = model.getEmissiveTriangles();
        EffectiveVertex[] effectiveVertices = model.getEffectiveVertices();
        if ((this.particleSystem == null || this.particleSystem.isDisabled) && (emitters != null || effectiveVertices != null)) {
            this.particleSystem = ParticleSystem.get(cycle, false);
        }
        if (this.particleSystem != null) {
            this.particleSystem.performCycle(toolkit, cycle, emitters, effectiveVertices);
        }
    }

    public void addOptionName(int index, String optionName) {
        if (this.optionNames == null || this.optionNames.length <= index) {
            String[] replacementOptionNames = new String[index + 1];
            if (this.optionNames != null) {
                System.arraycopy(this.optionNames, 0, replacementOptionNames, 0, this.optionNames.length);
            }
            this.optionNames = replacementOptionNames;
        }
        this.optionNames[index] = optionName;
    }

    public void method1995(int i_1) {
        if (this.aClass4651365 != null) {
            Node node3 = this.aClass4651365.get(i_1);
            if (node3 != null) {
                node3.unlink();
            }
        }
    }

    public int method1997(int i_1, int i_2) {
        if (this.aClass4651365 == null) {
            return i_2;
        } else {
            IntNode class282Sub384 = (IntNode) this.aClass4651365.get(i_1);
            return class282Sub384 == null ? i_2 : class282Sub384.value;
        }
    }

    public String method1998(int i_1, String string_2) {
        if (this.aClass4651365 == null) {
            return string_2;
        } 
        ObjectNode class282Sub474 = (ObjectNode) this.aClass4651365.get(i_1);
        return class282Sub474 == null ? string_2 : (String) class282Sub474.value;
    }

    public void method1999(int i_1, int i_2) {
        if (this.aClass4651365 == null) {
            this.aClass4651365 = new IterableNodeMap(16);
            this.aClass4651365.put(new IntNode(i_2), i_1);
        } else {
            IntNode class282Sub384 = (IntNode) this.aClass4651365.get(i_1);
            if (class282Sub384 == null) {
                this.aClass4651365.put(new IntNode(i_2), i_1);
            } else {
                class282Sub384.value = i_2;
            }
        }
    }

    public void method2000(int i_1, String string_2) {
        if (this.aClass4651365 == null) {
            this.aClass4651365 = new IterableNodeMap(16);
        } else {
            ObjectNode class282Sub474 = (ObjectNode) this.aClass4651365.get(i_1);
            if (class282Sub474 != null) {
                class282Sub474.unlink();
            }
        }
        this.aClass4651365.put(new ObjectNode(string_2), i_1);
    }



    public Skybox calculateLighting(SkyboxTypeList skyboxtypelist, SunTypeList suntypelist) {
        if (this.skyboxId == -1) {
            return null;
        } 
        long uid = (this.lightPositionX & 0xffffL) << 48 | (this.lightPositionY & 0xffffL) << 32 | (this.lightPositionZ & 0xffffL) << 16 | this.skyboxId & 0xffffL;
        Skybox lighting = (Skybox) COMPONENT_LIGHTING.get(uid);
        if (lighting == null) {
            lighting = skyboxtypelist.generateSkybox(this.skyboxId, this.lightPositionX, this.lightPositionY, this.lightPositionZ, suntypelist);
            COMPONENT_LIGHTING.put(lighting, uid);
        }
        return lighting;
    }

    public void addCursor(int cursorCount, int cursor) {
        if (this.opCursors == null || this.opCursors.length <= cursorCount) {
            int[] cursors = new int[cursorCount + 1];
            if (this.opCursors != null) {
                System.arraycopy(this.opCursors, 0, cursors, 0, this.opCursors.length);
                for (int index = this.opCursors.length; index < cursorCount; index++) {
                    cursors[index] = -1;
                }
            }
            this.opCursors = cursors;
        }
        this.opCursors[cursorCount] = cursor;
    }

    public InteractiveComponentSprite addInteractiveSprite(AbstractToolkit toolkit) {
        long uid = (long) this.idHash << 32 | this.slotId & 0xffffffffL;
        InteractiveComponentSprite sprite = (InteractiveComponentSprite) INTERACTIVE_SPRITES.get(uid);
        if (sprite != null) {
            if (sprite.spriteId != this.spriteId) {
                INTERACTIVE_SPRITES.removeByUid(uid);
                sprite = null;
            }
            if (sprite != null) {
                return sprite;
            }
        }
        SpriteType spriteType = SpriteType.listSingle(StaticMethods.SPRITES_INDEX, this.spriteId, 0);
        if (spriteType == null) {
            return null;
        } 
        int sizeX = spriteType.width + spriteType.minX + spriteType.borderX;
        int sizeY = spriteType.height + spriteType.minY + spriteType.borderY;
        int[] rowStartX = new int[sizeY];
        int[] rowWidth = new int[sizeY];
        for (int y = 0; y < spriteType.height; y++) {
            int clipLeft = 0;
            int clipRight = spriteType.width;
            for (int x = 0; x < spriteType.width; x++) {
                if (spriteType.pixels[x + y * spriteType.width] != 0) {
                    clipLeft = x;
                    break;
                }
            }
            for (int x = spriteType.width - 1; x >= clipLeft; --x) {
                if (spriteType.pixels[x + y * spriteType.width] != 0) {
                    clipRight = x + 1;
                    break;
                }
            }
            rowStartX[y + spriteType.minY] = clipLeft + spriteType.minX;
            rowWidth[y + spriteType.minY] = clipRight - clipLeft;
        }
        ComponentSurface surface = toolkit.createComponentSurface(sizeX, sizeY, rowStartX, rowWidth);
        if (surface == null) {
            return null;
        } 
        sprite = new InteractiveComponentSprite(sizeX, sizeY, rowWidth, rowStartX, surface, this.spriteId);
        INTERACTIVE_SPRITES.put(sprite, uid);
        return sprite;
    }


	public static int getUseOptionFlags(int settings) {
	    return settings >> 11 & 0x7f;
	}

	public static void runScriptsIfLoaded(int interfaceId, int[] xteas) {
	    if (interfaceId != -1 && Interface.interfaceLoaded(interfaceId, xteas)) {
	        Component[] components = Interface.INTERFACES[interfaceId].components;
	        runComponentOnloadScripts(components);
	    }
	}

	public static void update(int interfaceId) {
	    if (Interface.interfaceLoaded(interfaceId, null)) {
	        update(Interface.INTERFACES[interfaceId].components, -1);
	    }
	}

	public static void method3629(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
	    if (!Interface.interfaceLoaded(i_0, null)) {
	        if (i_7 != -1) {
	            client.IS_COMPONENT_ON_TOP[i_7] = true;
	        } else {
	            for (int i_9 = 0; i_9 < 107; i_9++) {
	                client.IS_COMPONENT_ON_TOP[i_9] = true;
	            }
	        }
	    } else {
	        InterfaceComponentRenderer.drawComponents(Interface.INTERFACES[i_0].getComponents(), -1, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_7 < 0);
	    }
	}

	public static void method483(Interface interface0, Component icomponentdefinitions1, boolean bool2) {
	    InterfaceComponentRenderer.updateEntityOnInterface(interface0.components, icomponentdefinitions1, bool2);
	}

	public static void runComponentOnloadScripts(Component[] components) {
	    for (int index = 0; index < components.length; index++) {
	        Component component = components[index];
	        if (component.onLoadScript != null) {
	            HookRequest hookRequests = new HookRequest();
	            hookRequests.source = component;
	            hookRequests.params = component.onLoadScript;
	            CS2Executor.execute_hook_inner(hookRequests, 2000000);
	        }
	    }
	}

	public static void resetCache(int i_0) {
	    Interface.preferenceMask = i_0;
	    MODEL_CACHE.reset();
	}


	public static void redrawComponent(Component component) {
	    if (component.lastRenderedCycle == client.anInt7408) {
	        client.IS_COMPONENT_ON_TOP[component.cacheReference] = true;
	    }
	}
}
