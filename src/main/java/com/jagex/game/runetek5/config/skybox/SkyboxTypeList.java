package com.jagex.game.runetek5.config.skybox;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.sun.Sun;
import com.jagex.game.runetek5.config.sun.SunType;
import com.jagex.game.runetek5.config.sun.SunTypeList;
import com.jagex.game.runetek5.graphics.light.Skybox;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class SkyboxTypeList {

    public final LRUCache cache = new LRUCache(16);
    public final Js5ResourceProvider skyboxResource;

    public SkyboxTypeList(Js5ResourceProvider skyboxResource) {
        this.skyboxResource = skyboxResource;
        this.skyboxResource.getArchiveFileCount(SharedConfigsType.SKYBOX.id);
    }

    public void cacheClean() {
        synchronized (this.cache) {
        	this.cache.clean(5);
        }
    }

    public void cacheReset() {
        synchronized (this.cache) {
        	this.cache.reset();
        }
    }

    public void cacheResize() {
        synchronized (this.cache) {
        	this.cache.resize();
        }
    }

    public SkyboxType list(int fileId) {
        SkyboxType skyboxType;
        synchronized (this.cache) {
            skyboxType = (SkyboxType) this.cache.get(fileId);
        }
        if (skyboxType != null) {
            return skyboxType;
        } 
        byte[] data;
        synchronized (this.skyboxResource) {
            data = this.skyboxResource.getFile(SharedConfigsType.SKYBOX.id, fileId);
        }
        skyboxType = new SkyboxType();
        if (data != null) {
            skyboxType.decode(new Packet(data));
        }
        synchronized (this.cache) {
        	this.cache.put(skyboxType, fileId);
            return skyboxType;
        }
    }

    public Skybox generateSkybox(int skyboxId, int x, int y, int z, SunTypeList suntypelist) {
        Sun[] suns = null;
        SkyboxType skyboxType = list(skyboxId);
        if (skyboxType.sunDefinitionIds != null) {
            suns = new Sun[skyboxType.sunDefinitionIds.length];
            for (int index = 0; index < suns.length; index++) {
                SunType sunType = suntypelist.list(skyboxType.sunDefinitionIds[index]);
                suns[index] = new Sun(sunType.actionType, sunType.materialId, sunType.positionX, sunType.positionY, sunType.positionZ, sunType.positionOffsetStep, sunType.displayColor, sunType.invertPosition, sunType.rotationX, sunType.rotationY, sunType.rotationZ);
            }
        }
        return new Skybox(skyboxType.materialId, suns, skyboxType.defaultSunIndex, x, y, z, skyboxType.backgroundMode, skyboxType.archiveId);
    }
}
