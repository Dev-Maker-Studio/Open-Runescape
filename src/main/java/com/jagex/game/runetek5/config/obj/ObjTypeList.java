package com.jagex.game.runetek5.config.obj;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.param.ParamTypeList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteDrawQueue;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.SoftwareItemRender;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.tools.LocalizedText;

public class ObjTypeList {

    public final SpriteDrawQueue spriteDrawQueue = new SpriteDrawQueue();
    public final int totalItemCount;
    public boolean isMember;
    public int preferenceFlags;
    public final LRUCache typeCache = new LRUCache(64);
    public final LRUCache modelCache = new LRUCache(50);
    public final SoftwareItemRender softwareItemRenderer = new SoftwareItemRender();
    public final Game game;
    public final Language language;
    public final ParamTypeList paramTypeList;
    public final Js5ResourceProvider objResource;
    public final Js5ResourceProvider meshResource;
    public final String[] defaultGroundActions;
    public final String[] defaultInventoryActions;

    public ObjTypeList(Game game, Language language, ParamTypeList paramTypeList, Js5ResourceProvider objResource, Js5ResourceProvider meshResource) {
    	this.game = game;
        this.language = language;
        this.isMember = true;
        this.paramTypeList = paramTypeList;
        this.objResource = objResource;
        this.meshResource = meshResource;
        if (objResource != null) {
            int lastArchiveId = objResource.getArchiveCount() - 1;
            this.totalItemCount = lastArchiveId * SharedConfigsType.ITEMS.archiveFileCapacity() + this.objResource.getArchiveFileCount(lastArchiveId);
        } else {
        	this.totalItemCount = 0;
        }

        if (this.game == Game.RUNESCAPE) {
        	this.defaultGroundActions = new String[]{null, null, LocalizedText.TAKE.translate(this.language), null, null, LocalizedText.EXAMINE.translate(this.language)};
        } else {
        	this.defaultGroundActions = new String[]{null, null, LocalizedText.TAKE.translate(this.language), null, null, null};
        }

        this.defaultInventoryActions = new String[]{null, null, null, null, LocalizedText.DROP.translate(this.language)};
    }

    public ObjType list(int itemId) {
        ObjType objType;
        synchronized (this.typeCache) {
            objType = (ObjType) this.typeCache.get(itemId);
        }
        if (objType != null) {
            return objType;
        } 
        byte[] data;
        synchronized (this.objResource) {
            data = this.objResource.getFile(SharedConfigsType.ITEMS.archiveId(itemId), SharedConfigsType.ITEMS.archiveFileId(itemId));
        }
        objType = new ObjType();
        objType.typeList = this;
        objType.id = itemId;
        objType.groundActions = this.defaultGroundActions.clone();
        objType.inventoryActions = this.defaultInventoryActions.clone();
        if (data != null) {
            objType.decodeType(new Packet(data));
        }
        objType.postDecode();
        if (objType.certTemplateId != -1) {
            objType.generateNote(list(objType.certTemplateId), list(objType.certId));
        }
        if (objType.lendTemplateId != -1) {
            objType.generateLend(list(objType.lendTemplateId), list(objType.lendId));
        }
        if (objType.bindTemplateId != -1) {
            objType.generateBind(list(objType.bindTemplateId), list(objType.bindId));
        }
        if (!this.isMember && objType.membersOnly) {
            objType.teamId = 0;
            objType.groundActions = this.defaultGroundActions;
            objType.inventoryActions = this.defaultInventoryActions;
            objType.tradeable = false;
            objType.quests = null;
            if (objType.parameters != null) {
                boolean validCs2 = false;
                for (Node<?> cs2Node = objType.parameters.first(); cs2Node != null; cs2Node = objType.parameters.next()) {
                    ParamType param = this.paramTypeList.list((int) cs2Node.pointer);
                    if (param.autoDisable) {
                        cs2Node.unlink();
                    } else {
                        validCs2 = true;
                    }
                }
                if (!validCs2) {
                    objType.parameters = null;
                }
            }
        }
        synchronized (this.typeCache) {
            this.typeCache.put(objType, itemId);
            return objType;
        }
    }

    public NativeSprite getSprite(AbstractToolkit mainToolkit, AbstractToolkit itemToolkit, int itemId, int itemStacksize, int outlineSize, int shadowColor, boolean zoomedIn, boolean useMainToolkit, int renderStack, AbstractFontRenderer fontRenderer, PlayerAppearance playerAppearance) {
        if (!useMainToolkit) {
            NativeSprite sprite = createNativeSprite(itemToolkit, itemId, itemStacksize, outlineSize, shadowColor, renderStack, playerAppearance);
            if (sprite != null) {
                return sprite;
            }
        }
        ObjType objType = list(itemId);
        if (itemStacksize > 1 && objType.stackIds != null) {
            int stackId = -1;
            for (int index = 0; index < 10; index++) {
                if (itemStacksize >= objType.stackTriggerAmount[index] && objType.stackTriggerAmount[index] != 0) {
                    stackId = objType.stackIds[index];
                }
            }
            if (stackId != -1) {
                objType = list(stackId);
            }
        }
        int[] sprites = objType.getSpritePixels(mainToolkit, itemToolkit, itemStacksize, outlineSize, shadowColor, zoomedIn, renderStack, fontRenderer, playerAppearance);
        if (sprites == null) {
            return null;
        } 
        NativeSprite nativeSprite;
        if (useMainToolkit) {
            nativeSprite = mainToolkit.createNativeSprite(sprites, 36, 36, 32);
        } else {
            nativeSprite = itemToolkit.createNativeSprite(sprites, 36, 36, 32);
        }
        
        // this block gets hit on every display mode full & fixed screen
        if (!useMainToolkit) {
            SoftwareItemRender itemRenderer = new SoftwareItemRender();
            itemRenderer.rendererId = itemToolkit.renderId;
            itemRenderer.itemId = itemId;
            itemRenderer.itemAmount = itemStacksize;
            itemRenderer.outlineSize = outlineSize;
            itemRenderer.shadowColor = shadowColor;
            itemRenderer.renderStack = renderStack;
            itemRenderer.hasPlayerAppearance = playerAppearance != null;
            this.spriteDrawQueue.insert(nativeSprite, itemRenderer);
        }
        return nativeSprite;
    }

    public void setMembersOnly(boolean isMember) {
        if (this.isMember != isMember) {
            this.isMember = isMember;
            resetCaches();
        }
    }

    public void resetCaches() {
        synchronized (this.typeCache) {
            this.typeCache.reset();
        }
        synchronized (this.modelCache) {
            this.modelCache.reset();
        }
        synchronized (this.spriteDrawQueue) {
            this.spriteDrawQueue.reset();
        }
    }

    public void resetRecolorCache() {
        synchronized (this.spriteDrawQueue) {
            this.spriteDrawQueue.reset();
        }
    }

    public void resetModelCache() {
        synchronized (this.modelCache) {
            this.modelCache.reset();
        }
    }

    public void resetCache(int preferenceFlags) {
        this.preferenceFlags = preferenceFlags;
        synchronized (this.modelCache) {
            this.modelCache.reset();
        }
    }

    public void processCycle() {
        synchronized (this.typeCache) {
            this.typeCache.clean(5);
        }
        synchronized (this.modelCache) {
            this.modelCache.clean(5);
        }
        synchronized (this.spriteDrawQueue) {
            this.spriteDrawQueue.processCycle();
        }
    }

    public NativeSprite createNativeSprite(AbstractToolkit toolkit, int itemId, int itemAmount, int outlineSize, int shadowColor, int renderStack, PlayerAppearance playerAppearance) {
        this.softwareItemRenderer.rendererId = toolkit.renderId;
        this.softwareItemRenderer.itemId = itemId;
        this.softwareItemRenderer.itemAmount = itemAmount;
        this.softwareItemRenderer.outlineSize = outlineSize;
        this.softwareItemRenderer.shadowColor = shadowColor;
        this.softwareItemRenderer.renderStack = renderStack;
        this.softwareItemRenderer.hasPlayerAppearance = playerAppearance != null;
        return this.spriteDrawQueue.getNativeSprite(this.softwareItemRenderer);
    }

    public void clearCacheQueues() {
        synchronized (this.typeCache) {
            this.typeCache.resize();
        }
        synchronized (this.modelCache) {
            this.modelCache.resize();
        }
        synchronized (this.spriteDrawQueue) {
            this.spriteDrawQueue.remove();
        }
    }
}
