package com.jagex.game.runetek5.scene.environment

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.graphics.light.Skybox
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.ColorUtil

/**
 *
 * @author Brainpower - 30/03/2022
 * @author Praesul team - 22/08/2023
 *
 * Replaced some of the naming with the naming from the Praesul team as it was clearer
 * Reference: https://rune-server.org/runescape-development/rs2-client/show-off/695325-praesul-private-map-editor.html
 *
 *
 */
class Environment {

    @JvmField var sunColour: Int = 0

    @JvmField var sunPosition: Vector3? = null

    @JvmField var sunAmbient: Float = 0f

    @JvmField var sunLight: Float = 0f

    @JvmField var sunBacklight: Float = 0f

    @JvmField var fogColour: Int = 0

    @JvmField var fogDepth: Int = 0

    @JvmField var sunCubeTexture: AbstractCubeTextureGenerator? = null

    @JvmField var bloom: Float = 0f

    @JvmField var brightpass: Float = 0f

    @JvmField var whitePoint: Float = 0f

    @JvmField var skybox: Skybox? = null

    constructor(mapBuffer: Packet, environmentManager: EnvironmentManager) {
        decode(mapBuffer, environmentManager)
    }

    constructor() {
        reset()
    }

    fun decode(buffer: Packet, manager: EnvironmentManager) {
        val flags = buffer.readUByte()
        if (PreferenceManager.preferences!!.lightDetail!!.getValue() == 1 && Renderers.activeToolkit!!.getMaxLights() > 0) {
            if ((flags and 0x1) != 0) {
                this.sunColour = buffer.readInt()
            } else {
                this.sunColour = EnvironmentManager.DEFAULT_SUN_COLOR
            }
            if ((flags and 0x2) != 0) {
                this.sunAmbient = buffer.readUShort() / 256.0F
            } else {
                this.sunAmbient = 1.1523438F
            }
            if ((flags and 0x4) != 0) {
                this.sunLight = buffer.readUShort() / 256.0F
            } else {
                this.sunLight = 0.69921875F
            }
            if ((flags and 0x8) != 0) {
                this.sunBacklight = buffer.readUShort() / 256.0F
            } else {
                this.sunBacklight = 1.2F
            }
        } else {
            if ((flags and 0x1) != 0) {
                buffer.readInt()
            }
            if ((flags and 0x2) != 0) {
                buffer.readUShort()
            }
            if ((flags and 0x4) != 0) {
                buffer.readUShort()
            }
            if ((flags and 0x8) != 0) {
                buffer.readUShort()
            }
            this.sunColour = EnvironmentManager.DEFAULT_SUN_COLOR
            this.sunBacklight = 1.2F
            this.sunLight = 0.69921875F
            this.sunAmbient = 1.1523438F
        }
        if ((flags and 0x10) != 0) {
            this.sunPosition = Vector3Pool.vector3From(buffer.readShort().toFloat(), buffer.readShort().toFloat(), buffer.readShort().toFloat())
        } else {
            this.sunPosition = Vector3Pool.vector3From(-50.0f, -60.0f, -50.0f)
        }
        if ((flags and 0x20) != 0) {
            this.fogColour = buffer.readInt()
        } else {
            this.fogColour = EnvironmentManager.DEFAULT_FOG_COLOR
        }
        if ((flags and 0x40) != 0) {
            this.fogDepth = buffer.readUShort()
        } else {
            this.fogDepth = 0
        }
        if ((flags and 0x80) != 0) {
            val materialAtPositiveX = buffer.readUShort()
            val materialAtNegativeX = buffer.readUShort()
            val materialAtPositiveY = buffer.readUShort()
            val materialAtNegativeY = buffer.readUShort()
            val materialAtPositiveZ = buffer.readUShort()
            val materialAtNegativeZ = buffer.readUShort()
            this.sunCubeTexture = manager.getCubeTexture(materialAtPositiveX, materialAtNegativeX,
                    materialAtPositiveY, materialAtNegativeY, materialAtPositiveZ, materialAtNegativeZ)
        } else {
            this.sunCubeTexture = EnvironmentManager.defaultSunCubeTexture
        }
    }

    fun decodeHdr(mapBuffer: Packet) {
        this.bloom = (mapBuffer.readUByte() * 8) / 255.0F
        this.brightpass = (mapBuffer.readUByte() * 8) / 255.0F
        this.whitePoint = (mapBuffer.readUByte() * 8) / 255.0F
    }

    fun decodeSkybox(mapBuffer: Packet, sceneDefaults: EnvironmentManager) {
        val skyboxId = mapBuffer.readUShort()
        val x = mapBuffer.readShort()
        val y = mapBuffer.readShort()
        val z = mapBuffer.readShort()
        val rotation = mapBuffer.readUShort()
        skyboxRotation = rotation
        this.skybox = sceneDefaults.getSkybox(skyboxId, x, y, z)
    }

    fun reset() {
        this.sunColour = EnvironmentManager.DEFAULT_SUN_COLOR
        this.sunPosition = Vector3Pool.vector3From(-50.0f, -60.0f, -50.0f)
        this.sunAmbient = 1.1523438F
        this.sunLight = 0.69921875F
        this.sunBacklight = 1.2F
        this.fogColour = EnvironmentManager.DEFAULT_FOG_COLOR
        this.fogDepth = 0
        this.sunCubeTexture = EnvironmentManager.defaultSunCubeTexture
        this.bloom = 1.0F
        this.brightpass = 0.25F
        this.whitePoint = 1.0F
        this.skybox = EnvironmentManager.DEFAULT_DIRECTIONAL_LIGHT
    }

    fun copyFrom(src: Environment) {
        this.sunColour = src.sunColour
        this.sunAmbient = src.sunAmbient
        this.sunLight = src.sunLight
        this.sunBacklight = src.sunBacklight
        this.sunPosition!!.copy(src.sunPosition!!)
        this.fogColour = src.fogColour
        this.fogDepth = src.fogDepth
        this.sunCubeTexture = src.sunCubeTexture
        this.bloom = src.bloom
        this.brightpass = src.brightpass
        this.whitePoint = src.whitePoint
        this.skybox = src.skybox
    }

    fun transition(toolkit: AbstractToolkit, transparent: Environment, opaque: Environment, transparencyPercent: Float) {
        this.sunColour = ColorUtil.mixColorsWithTransparency(transparent.sunColour, opaque.sunColour, 255.0F * transparencyPercent)
        this.sunLight = transparent.sunLight + (opaque.sunLight - transparent.sunLight) * transparencyPercent
        this.sunBacklight = transparent.sunBacklight + (opaque.sunBacklight - transparent.sunBacklight) * transparencyPercent
        this.sunAmbient = transparent.sunAmbient + (opaque.sunAmbient - transparent.sunAmbient) * transparencyPercent
        this.whitePoint = transparent.whitePoint + (opaque.whitePoint - transparent.whitePoint) * transparencyPercent
        this.bloom = transparent.bloom + (opaque.bloom - transparent.bloom) * transparencyPercent
        this.brightpass = transparent.brightpass + (opaque.brightpass - transparent.brightpass) * transparencyPercent
        this.fogColour = ColorUtil.mixColorsWithTransparency(transparent.fogColour, opaque.fogColour, 255.0F * transparencyPercent)
        this.fogDepth = transparent.fogDepth + ((opaque.fogDepth - transparent.fogDepth) * transparencyPercent).toInt()
        if (transparent.sunCubeTexture != opaque.sunCubeTexture) {
            this.sunCubeTexture = toolkit.drawQuad(transparent.sunCubeTexture, opaque.sunCubeTexture, transparencyPercent, this.sunCubeTexture)
        }
        if (transparent.skybox != opaque.skybox) {
            if (transparent.skybox == null) {
                this.skybox = opaque.skybox
                this.skybox!!.interpolateAlpha((transparencyPercent * 255.0F).toInt(), 0)
            } else {
                this.skybox = transparent.skybox
                this.skybox!!.interpolateAlpha((transparencyPercent * 255.0F).toInt(), 255)
            }
        }
    }


    fun isEqual(other: Environment): Boolean {
        return this.sunColour == other.sunColour && other.sunAmbient == this.sunAmbient
                && this.sunLight == other.sunLight && other.sunBacklight == this.sunBacklight
                && other.brightpass == this.brightpass && other.bloom == this.bloom
                && this.whitePoint == other.whitePoint && this.fogColour == other.fogColour
                && this.fogDepth == other.fogDepth && this.sunCubeTexture == other.sunCubeTexture
                && this.skybox == other.skybox
    }

    fun getFogColor(): Int {
        return this.fogColour
    }

    fun getSkybox(): Skybox? {
        return this.skybox
    }

    companion object {
        @JvmField var skyboxRotation: Int = 0
    }
}
