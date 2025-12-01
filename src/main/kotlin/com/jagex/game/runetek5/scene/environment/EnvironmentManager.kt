package com.jagex.game.runetek5.scene.environment

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.light.Skybox
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.Utils
import java.util.Arrays

class EnvironmentManager(
    @JvmField val activeToolkit: AbstractToolkit,
    widthInChunks: Int,
    lengthInChunks: Int
) {

    @JvmField
    var lastEnvironmentUpdate: Long = 0

    @JvmField
    var totalTransitionDuration: Int = 0

    @JvmField
    var transitionDurationLeft: Int = -1

    @JvmField
    var directEnvironmentTransition: Boolean = false

    @JvmField
    val sunPosition: Vector3 = Vector3(-50.0f, -60.0f, -50.0f)

    @JvmField
    val cubeTextureCache: LRUCache = LRUCache(9)

    @JvmField
    val skyboxCache: LRUCache = LRUCache(9)

    @JvmField
    val chunkEnvironment: Array<Array<Environment?>>

    @JvmField
    val defaultEnvironment: Environment

    @JvmField
    val environment: Environment

    @JvmField
    val referenceSceneEnvironment: Environment

    @JvmField
    val referenceChunkEnvironment: Environment

    init {
        this.chunkEnvironment = Array(widthInChunks) { arrayOfNulls(lengthInChunks) }
        if (cubeTextureIds != null) {
            defaultSunCubeTexture = getCubeTexture(
                cubeTextureIds!![0], cubeTextureIds!![1],
                cubeTextureIds!![2], cubeTextureIds!![3], cubeTextureIds!![4], cubeTextureIds!![5]
            )
        }
        DEFAULT_DIRECTIONAL_LIGHT = null
        this.defaultEnvironment = Environment()
        this.environment = Environment()
        this.referenceSceneEnvironment = Environment()
        this.referenceChunkEnvironment = Environment()
        requestDirectEnvironmentTransition()
    }

    fun updateAtmosphere(mapregion: MapRegion) {
        if (!GameState.isLoggedOut(client.gameState) && !GameState.inLobby(client.gameState)) {
            var x = -1
            var y = -1
            if (PlayerUpdate.MY_PLAYER != null) {
                x = PlayerUpdate.MY_PLAYER!!.movementBaseX[0] shr 3
                y = PlayerUpdate.MY_PLAYER!!.movementBaseY[0] shr 3
            }
            if (x >= 0 && x < mapregion.getSceneSizeX() shr 3 && y >= 0 && y < mapregion.getSceneSizeY() shr 3) {
                chunkEnvironmentUpdate(x, y, ENVIRONMENT_UPDATE_DURATION)
            } else {
                chunkEnvironmentUpdate(mapregion.getSceneSizeX() shr 4, mapregion.getSceneSizeY() shr 4, 0)
            }
        } else {
            chunkEnvironmentUpdate(Camera.cameraPositionAbsoluteX shr 12, Camera.cameraPositionAbsoluteY shr 12, ENVIRONMENT_UPDATE_DURATION)
        }
        // Only perform GL-affecting operations on the render thread.
        if (!mapregion.asyncMaploading) {
            transitionSceneEnvironment()
            applySun()
            setSceneBloomFilter()
            calculateFog()
        }
    }

    fun getEnvironment(): Environment {
        return this.environment
    }

    fun buildEnvironment(defaults: EnvironmentManager) {
        this.environment.copyFrom(defaults.environment)
        this.referenceSceneEnvironment.copyFrom(defaults.referenceSceneEnvironment)
        this.referenceChunkEnvironment.copyFrom(defaults.referenceChunkEnvironment)
        this.lastEnvironmentUpdate = defaults.lastEnvironmentUpdate
        this.totalTransitionDuration = defaults.totalTransitionDuration
        this.transitionDurationLeft = defaults.transitionDurationLeft
    }

    fun reset() {
        this.cubeTextureCache.reset()
        this.skyboxCache.reset()
    }

    fun resetChunkEnvironments() {
        for (index in this.chunkEnvironment.indices) {
            Arrays.fill(this.chunkEnvironment[index], this.defaultEnvironment)
        }
    }

    fun initializeEnvironment(chunkX: Int, chunkY: Int) {
        val environment = this.chunkEnvironment[chunkX][chunkY]
        if (environment != null && environment.sunPosition != null) {
            this.sunPosition.copy(environment.sunPosition!!)
        }
    }

    fun requestDirectEnvironmentTransition() {
        this.directEnvironmentTransition = true
    }

    fun applySun() {
        this.activeToolkit.updateSun((0.7f + PreferenceManager.preferences.brightness.getValue() * 0.1f + Resource.mapRegion!!.getRandomColorOffset()) * this.environment.sunAmbient)
        this.activeToolkit.applyLighting(this.environment.sunColour, this.environment.sunLight, this.environment.sunBacklight, (this.sunPosition.x.toInt() shl 2).toFloat(), (this.sunPosition.y.toInt() shl 2).toFloat(), (this.sunPosition.z.toInt() shl 2).toFloat())
        this.activeToolkit.setSunCubeTexture(this.environment.sunCubeTexture)
    }

    fun getSkybox(skyboxId: Int, x: Int, y: Int, z: Int): Skybox {
        val uid = (x.toLong() and 0xffffL) shl 48 or ((y.toLong() and 0xffffL) shl 32) or ((z.toLong() and 0xffffL) shl 16) or (skyboxId.toLong() and 0xffffL)
        var skybox = this.skyboxCache.get(uid) as Skybox?
        if (skybox == null) {
            skybox = TypeLists.SKYBOX!!.generateSkybox(skyboxId, x, y, z, TypeLists.SUN!!)
            this.skyboxCache.put(skybox, uid)
        }
        return skybox
    }

    fun setSceneBloomFilter() {
        this.activeToolkit.updateBloomFilter(this.environment.whitePoint, this.environment.bloom, this.environment.brightpass)
    }

    fun prepareLights() {
        this.activeToolkit.updateSun((0.7f + PreferenceManager.preferences.brightness.getValue() * 0.1f + Resource.mapRegion!!.getRandomColorOffset()) * 1.1523438f)
        this.activeToolkit.applyLighting(DEFAULT_SUN_COLOR, 0.69921875f, 1.2f, -200.0f, -240.0f, -200.0f)
        this.activeToolkit.calculateFog(DEFAULT_FOG_COLOR, -1, 0)
        this.activeToolkit.setSunCubeTexture(defaultSunCubeTexture)
    }

    fun addChunkEnvironment(chunkX: Int, chunkY: Int, environment: Environment) {
        this.chunkEnvironment[chunkX][chunkY] = environment
    }

    fun getCubeTexture(
        materialAtPositiveX: Int, materialAtNegativeX: Int,
        materialAtPositiveY: Int, materialAtNegativeY: Int, materialAtPositiveZ: Int, materialAtNegativeZ: Int
    ): AbstractCubeTextureGenerator? {
        val hash = 67481L * materialAtPositiveX xor
                97549L * materialAtNegativeX xor
                475427L * materialAtPositiveY xor
                986053L * materialAtNegativeY xor
                32147369L * materialAtPositiveZ xor
                76724863L * materialAtNegativeZ
        var cubeTexture = this.cubeTextureCache[hash] as AbstractCubeTextureGenerator?
        if (cubeTexture == null) {
            cubeTexture = this.activeToolkit.createCubeTexture(
                materialAtPositiveX, materialAtNegativeX,
                materialAtPositiveY, materialAtNegativeY, materialAtPositiveZ, materialAtNegativeZ
            )
            this.cubeTextureCache.put(cubeTexture, hash)
        }
        return cubeTexture
    }

    fun transitionSceneEnvironment() {
        if (this.transitionDurationLeft >= 0) {
            val currentTime = Utils.time()
            this.transitionDurationLeft = (this.transitionDurationLeft - (currentTime - this.lastEnvironmentUpdate)).toInt()
            if (this.transitionDurationLeft > 0) {
                this.environment.transition(this.activeToolkit, this.referenceSceneEnvironment, this.referenceChunkEnvironment, (this.totalTransitionDuration - this.transitionDurationLeft).toFloat() / this.totalTransitionDuration)
            } else {
                this.environment.copyFrom(this.referenceChunkEnvironment)
                if (this.environment.skybox != null) {
                    this.environment.skybox!!.method4209()
                }
                this.transitionDurationLeft = -1
            }
            this.lastEnvironmentUpdate = currentTime
        }
    }

    fun chunkEnvironmentUpdate(chunkX: Int, chunkY: Int, durationMs: Int) {
        var durationMsMod = durationMs
        var chunkAtmosphere = this.chunkEnvironment[chunkX][chunkY]
        if (chunkAtmosphere == null) {
            chunkAtmosphere = this.defaultEnvironment
        }
        if (this.directEnvironmentTransition) {
            this.directEnvironmentTransition = false
            durationMsMod = 0
        }
        if (!this.referenceChunkEnvironment.isEqual(chunkAtmosphere)) {
            this.referenceChunkEnvironment.copyFrom(chunkAtmosphere)
            this.lastEnvironmentUpdate = Utils.time()
            this.transitionDurationLeft = durationMsMod
            this.totalTransitionDuration = this.transitionDurationLeft
            if (this.totalTransitionDuration != 0) {
                this.referenceSceneEnvironment.copyFrom(this.environment)
                if (this.environment.skybox != null) {
                    if (this.environment.skybox!!.method4210()) {
                        this.referenceSceneEnvironment.skybox = this.environment.skybox!!.get()
                        this.environment.skybox = this.referenceSceneEnvironment.skybox
                    }
                    if (this.environment.skybox != null && this.environment.skybox != this.referenceChunkEnvironment.skybox) {
                        this.environment.skybox!!.method4208(this.referenceChunkEnvironment.skybox)
                    }
                }
            }
        }
    }

    fun calculateFog() {
        val off: Byte = 0
        val density = (this.environment.fogDepth + 256 shl 2) + off
        this.activeToolkit.calculateFog(this.environment.fogColour, if (PreferenceManager.preferences.fog.getValue() == 1) density else -1, 0)
    }

    companion object {
        const val DEFAULT_SUN_COLOR = 16777215
        const val DEFAULT_FOG_COLOR = 13156520
        const val ENVIRONMENT_UPDATE_DURATION = 5145

        @JvmField
        var DEFAULT_DIRECTIONAL_LIGHT: Skybox? = null

        @JvmField
        var cubeTextureIds: IntArray? = null

        @JvmField
        var defaultSunCubeTexture: AbstractCubeTextureGenerator? = null

        @JvmStatic
        fun decodeCubeTextures(data: ByteArray) {
            val buffer = Packet(data)
            while (true) {
                val opcode = buffer.readUByte()
                if (opcode == 0) {
                    return
                }
                if (opcode == 1) {
                    val textures = IntArray(6)
                    cubeTextureIds = textures
                    textures[0] = buffer.readUShort()
                    textures[1] = buffer.readUShort()
                    textures[2] = buffer.readUShort()
                    textures[3] = buffer.readUShort()
                    textures[4] = buffer.readUShort()
                    textures[5] = buffer.readUShort()
                }
            }
        }

        @JvmStatic
        fun localOffsetX(i_0: Int, i_1: Int, rotation: Int, i_3: Int, i_4: Int, i_5: Int): Int {
            var i_31 = i_3
            var i_41 = i_4
            if ((i_5 and 0x1) == 1) {
                val i_7 = i_31
                i_31 = i_41
                i_41 = i_7
            }
            val rotationMasked = rotation and 0x3
            return if (rotationMasked == 0) i_0 else if (rotationMasked == 1) i_1 else if (rotationMasked == 2) 7 - i_0 - (i_31 - 1) else 7 - i_1 - (i_41 - 1)
        }

        @JvmStatic
        fun localOffsetY(i_0: Int, i_1: Int, rotation: Int, i_3: Int, i_4: Int, angle: Int): Int {
            var i_31 = i_3
            var i_41 = i_4
            if ((angle and 0x1) == 1) {
                val i_7 = i_31
                i_31 = i_41
                i_41 = i_7
            }
            val rotationMasked = rotation and 0x3
            return if (rotationMasked == 0) i_1 else if (rotationMasked == 1) 7 - i_0 - (i_31 - 1) else if (rotationMasked == 2) 7 - i_1 - (i_41 - 1) else i_0
        }

        @JvmStatic
        fun rotatedX(x: Int, z: Int, rotation: Int): Int {
            val rotationMasked = rotation and 0x3
            return if (rotationMasked == 0) x else if (rotationMasked == 1) z else if (rotationMasked == 2) 4095 - x else 4095 - z
        }

        @JvmStatic
        fun rotatedZ(x: Int, z: Int, rotation: Int): Int {
            val rotationMasked = rotation and 0x3
            return if (rotationMasked == 0) z else if (rotationMasked == 1) 4095 - x else if (rotationMasked == 2) 4095 - z else x
        }

        @JvmStatic
        fun rotateTerrainBlockY(i_0: Int, i_1: Int, rotation: Int): Int {
            val rotationMasked = rotation and 0x3
            return if (rotationMasked == 0) i_1 else if (rotationMasked == 1) 7 - i_0 else if (rotationMasked == 2) 7 - i_1 else i_0
        }

        @JvmStatic
        fun rotateTerrainBlockX(i_0: Int, i_1: Int, rotation: Int): Int {
            val rotationMasked = rotation and 0x3
            return if (rotationMasked == 0) i_0 else if (rotationMasked == 1) i_1 else if (rotationMasked == 2) 7 - i_0 else 7 - i_1
        }
    }
}
