package com.jagex.game.runetek5.scene.environment;

import java.util.Arrays;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.light.Skybox;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.Utils;

public class EnvironmentManager {

    static final int DEFAULT_SUN_COLOR = 16777215;
    static final int DEFAULT_FOG_COLOR = 13156520;
    public static final int ENVIRONMENT_UPDATE_DURATION = 5145;
    static Skybox DEFAULT_DIRECTIONAL_LIGHT;
    public long lastEnvironmentUpdate;
    public int totalTransitionDuration;
    public int transitionDurationLeft = -1;
    public boolean directEnvironmentTransition;
    public final Vector3 sunPosition = new Vector3(-50.0f, -60.0f, -50.0f);
    public final LRUCache cubeTextureCache = new LRUCache(9);
    public final LRUCache skyboxCache = new LRUCache(9);
    public final AbstractToolkit activeToolkit;
    public final Environment[][] chunkEnvironment;
    public final Environment defaultEnvironment;
    public final Environment environment;
    public final Environment referenceSceneEnvironment;
    public final Environment referenceChunkEnvironment;
	public static int[] cubeTextureIds;
	public static AbstractCubeTextureGenerator defaultSunCubeTexture;

    public EnvironmentManager(AbstractToolkit toolkit, int widthInChunks, int lengthInChunks) {
        this.activeToolkit = toolkit;
        this.chunkEnvironment = new Environment[widthInChunks][lengthInChunks];
        if (EnvironmentManager.cubeTextureIds != null) {
            EnvironmentManager.defaultSunCubeTexture = getCubeTexture(cubeTextureIds[0], cubeTextureIds[1],
            		cubeTextureIds[2], cubeTextureIds[3], cubeTextureIds[4], cubeTextureIds[5]);
        }
        DEFAULT_DIRECTIONAL_LIGHT = null;
        this.defaultEnvironment = new Environment();
        this.environment = new Environment();
        this.referenceSceneEnvironment = new Environment();
        this.referenceChunkEnvironment = new Environment();
        requestDirectEnvironmentTransition();
    }

    public void updateAtmosphere(MapRegion mapregion) {
        if (!GameState.isLoggedOut(client.gameState) && !GameState.inLobby(client.gameState)) {
            int x = -1;
            int y = -1;
            if (PlayerUpdate.MY_PLAYER != null) {
                x = PlayerUpdate.MY_PLAYER.movementBaseX[0] >> 3;
                y = PlayerUpdate.MY_PLAYER.movementBaseY[0] >> 3;
            }
            if (x >= 0 && x < mapregion.getSceneSizeX() >> 3 && y >= 0 && y < mapregion.getSceneSizeY() >> 3) {
                chunkEnvironmentUpdate(x, y, ENVIRONMENT_UPDATE_DURATION);
            } else {
                chunkEnvironmentUpdate(mapregion.getSceneSizeX() >> 4, mapregion.getSceneSizeY() >> 4, 0);
            }
        } else {
            chunkEnvironmentUpdate(Camera.cameraPositionAbsoluteX >> 12, Camera.cameraPositionAbsoluteY >> 12, ENVIRONMENT_UPDATE_DURATION);
        }
        transitionSceneEnvironment();
        applySun();
        setSceneBloomFilter();
        calculateFog();
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public void buildEnvironment(EnvironmentManager defaults) {
        this.environment.copyFrom(defaults.environment);
        this.referenceSceneEnvironment.copyFrom(defaults.referenceSceneEnvironment);
        this.referenceChunkEnvironment.copyFrom(defaults.referenceChunkEnvironment);
        this.lastEnvironmentUpdate = defaults.lastEnvironmentUpdate;
        this.totalTransitionDuration = defaults.totalTransitionDuration;
        this.transitionDurationLeft = defaults.transitionDurationLeft;
    }

    public void reset() {
        this.cubeTextureCache.reset();
        this.skyboxCache.reset();
    }

    public void resetChunkEnvironments() {
        for (int index = 0; index < this.chunkEnvironment.length; index++) {
            Arrays.fill(this.chunkEnvironment[index], this.defaultEnvironment);
        }
    }

    public void initializeEnvironment(int chunkX, int chunkY) {
        Environment environment = this.chunkEnvironment[chunkX][chunkY];
        if (environment != null) {
            this.sunPosition.copy(environment.sunPosition);
        }
        applySun();
    }

    public void requestDirectEnvironmentTransition() {
        this.directEnvironmentTransition = true;
    }

    public void applySun() {
        this.activeToolkit.updateSun((0.7F + PreferenceManager.preferences.brightness.getValue() * 0.1F + Resource.mapRegion.getRandomColorOffset()) * this.environment.sunAmbient);
        this.activeToolkit.applyLighting(this.environment.sunColour, this.environment.sunLight, this.environment.sunBacklight, ((int) this.sunPosition.x << 2), ((int) this.sunPosition.y << 2), ((int) this.sunPosition.z << 2));
        this.activeToolkit.setSunCubeTexture(this.environment.sunCubeTexture);
    }

    Skybox getSkybox(int skyboxId, int x, int y, int z) {
        long uid = (x & 0xffffL) << 48 | (y & 0xffffL) << 32 | (z & 0xffffL) << 16 | skyboxId & 0xffffL;
        Skybox skybox = (Skybox) this.skyboxCache.get(uid);
        if (skybox == null) {
            skybox = TypeLists.SKYBOX.generateSkybox(skyboxId, x, y, z, TypeLists.SUN);
            this.skyboxCache.put(skybox, uid);
        }
        return skybox;
    }

    public void setSceneBloomFilter() {
        this.activeToolkit.updateBloomFilter(this.environment.whitePoint, this.environment.bloom, this.environment.brightpass);
    }

    public void prepareLights() {
        this.activeToolkit.updateSun((0.7F + PreferenceManager.preferences.brightness.getValue() * 0.1F + Resource.mapRegion.getRandomColorOffset()) * 1.1523438F);
        this.activeToolkit.applyLighting(DEFAULT_SUN_COLOR, 0.69921875F, 1.2F, -200.0f, -240.0f, -200.0f);
        this.activeToolkit.calculateFog(DEFAULT_FOG_COLOR, -1, 0);
        this.activeToolkit.setSunCubeTexture(EnvironmentManager.defaultSunCubeTexture);
    }

    public void addChunkEnvironment(int chunkX, int chunkY, Environment environment) {
        this.chunkEnvironment[chunkX][chunkY] = environment;
    }

    AbstractCubeTextureGenerator getCubeTexture(int materialAtPositiveX, int materialAtNegativeX,
                                                int materialAtPositiveY, int materialAtNegativeY, int materialAtPositiveZ, int materialAtNegativeZ) {
    	
        long hash = 67481L * materialAtPositiveX ^
        		97549L * materialAtNegativeX ^
        		475427L * materialAtPositiveY ^
        		986053L * materialAtNegativeY ^
        		32147369L * materialAtPositiveZ ^
        		76724863L * materialAtNegativeZ;
        
        AbstractCubeTextureGenerator cubeTexture = (AbstractCubeTextureGenerator) this.cubeTextureCache.get(hash);
        if (cubeTexture == null) {
            cubeTexture = this.activeToolkit.createCubeTexture(materialAtPositiveX, materialAtNegativeX,
                    materialAtPositiveY, materialAtNegativeY, materialAtPositiveZ, materialAtNegativeZ);
            this.cubeTextureCache.put(cubeTexture, hash);
        }
        return cubeTexture;
    }

    public void transitionSceneEnvironment() {
        if (this.transitionDurationLeft >= 0) {
            long currentTime = Utils.time();
            this.transitionDurationLeft = (int) (this.transitionDurationLeft - (currentTime - this.lastEnvironmentUpdate));
            if (this.transitionDurationLeft > 0) {
                this.environment.transition(this.activeToolkit, this.referenceSceneEnvironment, this.referenceChunkEnvironment, (float) (this.totalTransitionDuration - this.transitionDurationLeft) / this.totalTransitionDuration);
            } else {
                this.environment.copyFrom(this.referenceChunkEnvironment);
                if (this.environment.skybox != null) {
                    this.environment.skybox.method4209();
                }
                this.transitionDurationLeft = -1;
            }
            this.lastEnvironmentUpdate = currentTime;
        }
    }

    public void chunkEnvironmentUpdate(int chunkX, int chunkY, int durationMs) {
        Environment chunkAtmosphere = this.chunkEnvironment[chunkX][chunkY];
        if (chunkAtmosphere == null) {
            chunkAtmosphere = this.defaultEnvironment;
        }
        if (this.directEnvironmentTransition) {
            this.directEnvironmentTransition = false;
            durationMs = 0;
        }
        if (!this.referenceChunkEnvironment.isEqual(chunkAtmosphere)) {
            this.referenceChunkEnvironment.copyFrom(chunkAtmosphere);
            this.lastEnvironmentUpdate = Utils.time();
            this.totalTransitionDuration = this.transitionDurationLeft = durationMs;
            if (this.totalTransitionDuration != 0) {
                this.referenceSceneEnvironment.copyFrom(this.environment);
                if (this.environment.skybox != null) {
                    if (this.environment.skybox.method4210()) {
                        this.environment.skybox = this.referenceSceneEnvironment.skybox = this.environment.skybox.get();
                    }
                    if (this.environment.skybox != null && this.environment.skybox != this.referenceChunkEnvironment.skybox) {
                        this.environment.skybox.method4208(this.referenceChunkEnvironment.skybox);
                    }
                }
            }
        }
    }

    public void calculateFog() {
        byte off = 0;
        int density = (this.environment.fogDepth + 256 << 2) + off;
        this.activeToolkit.calculateFog(this.environment.fogColour, PreferenceManager.preferences.fog.getValue() == 1 ? density : -1, 0);
    }

	public static void decodeCubeTextures(byte[] data) {
	    Packet buffer = new Packet(data);
	    while (true) {
	        int opcode = buffer.readUnsignedByte();
	        if (opcode == 0) {
	            return;
	        }
	        if (opcode == 1) {
	            int[] textures = EnvironmentManager.cubeTextureIds = new int[6];
	            textures[0] = buffer.readUnsignedShort();
	            textures[1] = buffer.readUnsignedShort();
	            textures[2] = buffer.readUnsignedShort();
	            textures[3] = buffer.readUnsignedShort();
	            textures[4] = buffer.readUnsignedShort();
	            textures[5] = buffer.readUnsignedShort();
	        }
	    }
	}

	public static int localOffsetX(int i_0, int i_1, int rotation, int i_3, int i_4, int i_5) {
	    int i_31 = i_3;
	    int i_41 = i_4;
	    if ((i_5 & 0x1) == 1) {
	        int i_7 = i_31;
	        i_31 = i_41;
	        i_41 = i_7;
	    }
	    rotation &= 0x3;
	    return rotation == 0 ? i_0 : (rotation == 1 ? i_1 : (rotation == 2 ? 7 - i_0 - (i_31 - 1) : 7 - i_1 - (i_41 - 1)));
	}

	public static int localOffsetY(int i_0, int i_1, int rotation, int i_3, int i_4, int angle) {
	    int i_31 = i_3;
	    int i_41 = i_4;
	    if ((angle & 0x1) == 1) {
	        int i_7 = i_31;
	        i_31 = i_41;
	        i_41 = i_7;
	    }
	    rotation &= 0x3;
	    return rotation == 0 ? i_1 : (rotation == 1 ? 7 - i_0 - (i_31 - 1) : (rotation == 2 ? 7 - i_1 - (i_41 - 1) : i_0));
	}

	public static int rotatedX(int x, int z, int rotation) {
	    rotation &= 0x3;
	    return rotation == 0 ? x : (rotation == 1 ? z : (rotation == 2 ? 4095 - x : 4095 - z));
	}

	public static int rotatedZ(int x, int z, int rotation) {
	    rotation &= 0x3;
	    return rotation == 0 ? z : (rotation == 1 ? 4095 - x : (rotation == 2 ? 4095 - z : x));
	}

	public static int rotateTerrainBlockY(int i_0, int i_1, int rotation) {
	    rotation &= 0x3;
	    return rotation == 0 ? i_1 : (rotation == 1 ? 7 - i_0 : (rotation == 2 ? 7 - i_1 : i_0));
	}

	public static int rotateTerrainBlockX(int i_0, int i_1, int rotation) {
	    rotation &= 0x3;
	    return rotation == 0 ? i_0 : (rotation == 1 ? i_1 : (rotation == 2 ? 7 - i_0 : 7 - i_1));
	}
}
