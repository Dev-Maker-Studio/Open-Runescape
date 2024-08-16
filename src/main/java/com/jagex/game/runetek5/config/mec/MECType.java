package com.jagex.game.runetek5.config.mec;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.*;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.vardomain.VarDomain;

public class MECType {

    public int mectypeId;
    public int defaultIconArchive = -1;
    public int mouseOverIconArchive = -1;
    public String areaName;
    public int defaultTextColor;
    public int mouseOverTextColor = -1;
    public int baseTextzoom;
    public boolean displayedOnWorldmap = true;
    public boolean displayedOnMinimap;
    public boolean hasRandomisedMinimapPosition = true;
    public int[] offsets;
    public boolean visible = true;
    public String minimenuName;
    public int lineColor;
    public int[] colors;
    public int anInt2718 = -1;
    public byte[] colorPointers;
    public int outlineColor;
    public int backgroundColor;
    public int anInt2748 = -1;
    public int anInt2749 = -1;
    public int anInt2756 = -1;
    public int anInt2750;
    public int anInt2752;
    public int minOffsetX = Integer.MAX_VALUE;
    public int maxOffsetX = Integer.MIN_VALUE;
    public int minOffsetY = Integer.MAX_VALUE;
    public int maxOffsetY = Integer.MIN_VALUE;
    public final String[] minimenuActions = new String[5];
    MECTypeList initialType;
    public int primaryVarpbit = -1;
    public int primaryVarp = -1;
    public int primaryVariableMinValue;
    public int primaryVariableMaxValue;
    public int spriteId = -1;
    public int secondaryVarpbit = -1;
    public int secondaryVarp = -1;
    public int secondaryVariableMinValue;
    public int secondaryVariableMaxValue;
    public IterableNodeMap<Node<?>> parameters;

    void decode(Packet buffer) {
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
            this.defaultIconArchive = buffer.readBigSmart();
        } else if (opcode == 2) {
            this.mouseOverIconArchive = buffer.readBigSmart();
        } else if (opcode == 3) {
            this.areaName = buffer.readString();
        } else if (opcode == 4) {
            this.defaultTextColor = buffer.read24BitUnsignedInteger();
        } else if (opcode == 5) {
            this.mouseOverTextColor = buffer.read24BitUnsignedInteger();
        } else if (opcode == 6) {
            this.baseTextzoom = buffer.readUnsignedByte();
        } else if (opcode == 7) {
            int flags = buffer.readUnsignedByte();
            if ((flags & 0x1) == 0) {
                this.displayedOnWorldmap = false;
            }
            if ((flags & 0x2) == 2) {
                this.displayedOnMinimap = true;
            }
        } else if (opcode == 8) {
            this.hasRandomisedMinimapPosition = buffer.readUnsignedByte() == 1;
        } else if (opcode == 9) {
            this.primaryVarpbit = buffer.readUnsignedShort();
            if (this.primaryVarpbit == 65535) {
                this.primaryVarpbit = -1;
            }
            this.primaryVarp = buffer.readUnsignedShort();
            if (this.primaryVarp == 65535) {
                this.primaryVarp = -1;
            }
            this.primaryVariableMinValue = buffer.readInt();
            this.primaryVariableMaxValue = buffer.readInt();
        } else if (opcode >= 10 && opcode <= 14) {
            this.minimenuActions[opcode - 10] = buffer.readString();
        } else if (opcode == 15) {
            int count = buffer.readUnsignedByte();
            this.offsets = new int[count * 2];
            for (int index = 0; index < count * 2; index++) {
                this.offsets[index] = buffer.readShort();
            }
            this.lineColor = buffer.readInt();
            int numColors = buffer.readUnsignedByte();
            this.colors = new int[numColors];
            for (int index = 0; index < this.colors.length; index++) {
                this.colors[index] = buffer.readInt();
            }
            this.colorPointers = new byte[count];
            for (int index = 0; index < count; index++) {
                this.colorPointers[index] = buffer.readByte();
            }
        } else if (opcode == 16) {
            this.visible = false;
        } else if (opcode == 17) {
            this.minimenuName = buffer.readString();
        } else if (opcode == 18) {
            this.spriteId = buffer.readBigSmart();
        } else if (opcode == 19) {
            this.anInt2718 = buffer.readUnsignedShort();
        } else if (opcode == 20) {
            this.secondaryVarpbit = buffer.readUnsignedShort();
            if (this.secondaryVarpbit == 65535) {
                this.secondaryVarpbit = -1;
            }
            this.secondaryVarp = buffer.readUnsignedShort();
            if (this.secondaryVarp == 65535) {
                this.secondaryVarp = -1;
            }
            this.secondaryVariableMinValue = buffer.readInt();
            this.secondaryVariableMaxValue = buffer.readInt();
        } else if (opcode == 21) {
            this.outlineColor = buffer.readInt();
        } else if (opcode == 22) {
            this.backgroundColor = buffer.readInt();
        } else if (opcode == 23) {
            this.anInt2748 = buffer.readUnsignedByte();
            this.anInt2749 = buffer.readUnsignedByte();
            this.anInt2756 = buffer.readUnsignedByte();
        } else if (opcode == 24) {
            this.anInt2750 = buffer.readShort();
            this.anInt2752 = buffer.readShort();
        } else if (opcode == 249) {
            int numParameters = buffer.readUnsignedByte();
            if (this.parameters == null) {
                int capacity = MathUtils.nextPowerOfTwo(numParameters);
                this.parameters = new IterableNodeMap<Node<?>>(capacity);
            }
            for (int index = 0; index < numParameters; index++) {
                boolean isString = buffer.readUnsignedByte() == 1;
                int key = buffer.read24BitUnsignedInteger();
                Node<?> value;
                if (isString) {
                    value = new ObjectNode(buffer.readString());
                } else {
                    value = new IntNode(buffer.readInt());
                }
                this.parameters.put(value, key);
            }
        }
    }

    void postDecode() {
        if (this.offsets != null) {
            for (int index = 0; index < this.offsets.length; index += 2) {
                if (this.offsets[index] < this.minOffsetX) {
                    this.minOffsetX = this.offsets[index];
                } else if (this.offsets[index] > this.maxOffsetX) {
                    this.maxOffsetX = this.offsets[index];
                }
                if (this.offsets[index + 1] < this.minOffsetY) {
                    this.minOffsetY = this.offsets[index + 1];
                } else if (this.offsets[index + 1] > this.maxOffsetY) {
                    this.maxOffsetY = this.offsets[index + 1];
                }
            }
        }
    }

    public boolean shouldDraw(VarDomain varProvider) {
        int primaryVariable;
        if (this.primaryVarp != -1) {
            primaryVariable = varProvider.getVarValue(this.primaryVarp);
        } else {
            if (this.primaryVarpbit == -1) {
                return true;
            }
            primaryVariable = varProvider.getVarBitValue(this.primaryVarpbit);
        }
        if (primaryVariable >= this.primaryVariableMinValue
        		&& primaryVariable <= this.primaryVariableMaxValue) {
            int secondaryVariable;
            if (this.secondaryVarp != -1) {
                secondaryVariable = varProvider.getVarValue(this.secondaryVarp);
            } else {
                if (this.secondaryVarpbit == -1) {
                    return true;
                }
                secondaryVariable = varProvider.getVarBitValue(this.secondaryVarpbit);
            }
            return secondaryVariable >= this.secondaryVariableMinValue
            		&& secondaryVariable <= this.secondaryVariableMaxValue;
        } 
        return false;
    }

    public String getStringParameter(int key, String defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } 
        ObjectNode value = (ObjectNode) this.parameters.get(key);
        return value == null ? defaultValue : (String) value.value;
    }

    public int getIntParameter(int key, int defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } 
        IntNode value = (IntNode) this.parameters.get(key);
        return value == null ? defaultValue : value.value;
    }

    public NativeSprite getSprite(AbstractToolkit toolkit) {
        NativeSprite sprite = (NativeSprite) this.initialType.spriteCache.get(this.spriteId | 0x20000 | (long) toolkit.renderId << 29);
        if (sprite == null) {
            this.initialType.spriteResource.isFileCached(this.spriteId);
            SpriteType spriteType = SpriteType.listSingle(this.initialType.spriteResource, this.spriteId, 0);
            if (spriteType != null) {
                sprite = toolkit.generateSprite(spriteType, true);
                this.initialType.spriteCache.put(sprite, this.spriteId | 0x20000 | (long) toolkit.renderId << 29);
            }
        }
        return sprite;
    }

    public NativeSprite getIcon(AbstractToolkit toolkit, boolean isMouseOver) {
        int archiveId = isMouseOver ? this.mouseOverIconArchive : this.defaultIconArchive;
        int spriteReference = archiveId | toolkit.renderId << 29;
        NativeSprite sprite = (NativeSprite) this.initialType.spriteCache.get(spriteReference);
        if (sprite != null) {
            return sprite;
        } 
        if (!this.initialType.spriteResource.isFileCached(archiveId)) {
            return null;
        } 
        SpriteType spriteType = SpriteType.listSingle(this.initialType.spriteResource, archiveId, 0);
        if (spriteType != null) {
            sprite = toolkit.generateSprite(spriteType, true);
            this.initialType.spriteCache.put(sprite, spriteReference);
        }
        return sprite;
    }
}
