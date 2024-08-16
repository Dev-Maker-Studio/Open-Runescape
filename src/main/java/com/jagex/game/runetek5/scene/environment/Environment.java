package com.jagex.game.runetek5.scene.environment;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.graphics.light.Skybox;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;

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
public class Environment {

    int sunColour;

    Vector3 sunPosition;

    float sunAmbient;

    float sunLight;

    float sunBacklight;

    int fogColour;

    int fogDepth;

    AbstractCubeTextureGenerator sunCubeTexture;

    float bloom;

    float brightpass;

    float whitePoint;

    Skybox skybox;

	public static int skyboxRotation;

    public Environment(Packet mapBuffer, EnvironmentManager environmentManager) {
        decode(mapBuffer, environmentManager);
    }

    public Environment() {
        reset();
    }

    public void decode(Packet buffer, EnvironmentManager manager) {
        int flags = buffer.readUnsignedByte();
        if (PreferenceManager.preferences.lightDetail.getValue() == 1 && Renderers.activeToolkit.getMaxLights() > 0) {
            if ((flags & 0x1) != 0) {
                this.sunColour = buffer.readInt();
            } else {
                this.sunColour = EnvironmentManager.DEFAULT_SUN_COLOR;
            }
            if ((flags & 0x2) != 0) {
                this.sunAmbient = buffer.readUnsignedShort() / 256.0F;
            } else {
                this.sunAmbient = 1.1523438F;
            }
            if ((flags & 0x4) != 0) {
                this.sunLight = buffer.readUnsignedShort() / 256.0F;
            } else {
                this.sunLight = 0.69921875F;
            }
            if ((flags & 0x8) != 0) {
                this.sunBacklight = buffer.readUnsignedShort() / 256.0F;
            } else {
                this.sunBacklight = 1.2F;
            }
        } else {
            if ((flags & 0x1) != 0) {
                buffer.readInt();
            }
            if ((flags & 0x2) != 0) {
                buffer.readUnsignedShort();
            }
            if ((flags & 0x4) != 0) {
                buffer.readUnsignedShort();
            }
            if ((flags & 0x8) != 0) {
                buffer.readUnsignedShort();
            }
            this.sunColour = EnvironmentManager.DEFAULT_SUN_COLOR;
            this.sunBacklight = 1.2F;
            this.sunLight = 0.69921875F;
            this.sunAmbient = 1.1523438F;
        }
        if ((flags & 0x10) != 0) {
            this.sunPosition = Vector3Pool.vector3From(buffer.readShort(), buffer.readShort(), buffer.readShort());
        } else {
            this.sunPosition = Vector3Pool.vector3From(-50.0f, -60.0f, -50.0f);
        }
        if ((flags & 0x20) != 0) {
            this.fogColour = buffer.readInt();
        } else {
            this.fogColour = EnvironmentManager.DEFAULT_FOG_COLOR;
        }
        if ((flags & 0x40) != 0) {
            this.fogDepth = buffer.readUnsignedShort();
        } else {
            this.fogDepth = 0;
        }
        if ((flags & 0x80) != 0) {
            int materialAtPositiveX = buffer.readUnsignedShort();
            int materialAtNegativeX = buffer.readUnsignedShort();
            int materialAtPositiveY = buffer.readUnsignedShort();
            int materialAtNegativeY = buffer.readUnsignedShort();
            int materialAtPositiveZ = buffer.readUnsignedShort();
            int materialAtNegativeZ = buffer.readUnsignedShort();
            this.sunCubeTexture = manager.getCubeTexture(materialAtPositiveX, materialAtNegativeX,
                    materialAtPositiveY, materialAtNegativeY, materialAtPositiveZ, materialAtNegativeZ);
        } else {
            this.sunCubeTexture = EnvironmentManager.defaultSunCubeTexture;
        }
    }

    public void decodeHdr(Packet mapBuffer) {
        this.bloom = (mapBuffer.readUnsignedByte() * 8) / 255.0F;
        this.brightpass = (mapBuffer.readUnsignedByte() * 8) / 255.0F;
        this.whitePoint = (mapBuffer.readUnsignedByte() * 8) / 255.0F;
    }

    public void decodeSkybox(Packet mapBuffer, EnvironmentManager sceneDefaults) {
        int skyboxId = mapBuffer.readUnsignedShort();
        int x = mapBuffer.readShort();
        int y = mapBuffer.readShort();
        int z = mapBuffer.readShort();
        int rotation = mapBuffer.readUnsignedShort();
        Environment.skyboxRotation = rotation;
        this.skybox = sceneDefaults.getSkybox(skyboxId, x, y, z);
    }
    
    public void reset() {
        this.sunColour = EnvironmentManager.DEFAULT_SUN_COLOR;
        this.sunPosition = Vector3Pool.vector3From(-50.0f, -60.0f, -50.0f);
        this.sunAmbient = 1.1523438F;
        this.sunLight = 0.69921875F;
        this.sunBacklight = 1.2F;
        this.fogColour = EnvironmentManager.DEFAULT_FOG_COLOR;
        this.fogDepth = 0;
        this.sunCubeTexture = EnvironmentManager.defaultSunCubeTexture;
        this.bloom = 1.0F;
        this.brightpass = 0.25F;
        this.whitePoint = 1.0F;
        this.skybox = EnvironmentManager.DEFAULT_DIRECTIONAL_LIGHT;
    }

    void copyFrom(Environment src) {
        this.sunColour = src.sunColour;
        this.sunAmbient = src.sunAmbient;
        this.sunLight = src.sunLight;
        this.sunBacklight = src.sunBacklight;
        this.sunPosition.copy(src.sunPosition);
        this.fogColour = src.fogColour;
        this.fogDepth = src.fogDepth;
        this.sunCubeTexture = src.sunCubeTexture;
        this.bloom = src.bloom;
        this.brightpass = src.brightpass;
        this.whitePoint = src.whitePoint;
        this.skybox = src.skybox;
    }

    void transition(AbstractToolkit toolkit, Environment transparent, Environment opaque, float transparencyPercent) {
        this.sunColour = ColorUtil.mixColorsWithTransparency(transparent.sunColour, opaque.sunColour, 255.0F * transparencyPercent);
        this.sunLight = transparent.sunLight + (opaque.sunLight - transparent.sunLight) * transparencyPercent;
        this.sunBacklight = transparent.sunBacklight + (opaque.sunBacklight - transparent.sunBacklight) * transparencyPercent;
        this.sunAmbient = transparent.sunAmbient + (opaque.sunAmbient - transparent.sunAmbient) * transparencyPercent;
        this.whitePoint = transparent.whitePoint + (opaque.whitePoint - transparent.whitePoint) * transparencyPercent;
        this.bloom = transparent.bloom + (opaque.bloom - transparent.bloom) * transparencyPercent;
        this.brightpass = transparent.brightpass + (opaque.brightpass - transparent.brightpass) * transparencyPercent;
        this.fogColour = ColorUtil.mixColorsWithTransparency(transparent.fogColour, opaque.fogColour, 255.0F * transparencyPercent);
        this.fogDepth = transparent.fogDepth + (int) ((opaque.fogDepth - transparent.fogDepth) * transparencyPercent);
        if (transparent.sunCubeTexture != opaque.sunCubeTexture) {
            this.sunCubeTexture = toolkit.drawQuad(transparent.sunCubeTexture, opaque.sunCubeTexture, transparencyPercent, this.sunCubeTexture);
        }
        if (transparent.skybox != opaque.skybox) {
            if (transparent.skybox == null) {
                this.skybox = opaque.skybox;
                this.skybox.method4207((int) (transparencyPercent * 255.0F), 0);
            } else {
                this.skybox = transparent.skybox;
                this.skybox.method4207((int) (transparencyPercent * 255.0F), 255);
            }
        }
    }

    
	boolean isEqual(Environment other) {
		return this.sunColour == other.sunColour && other.sunAmbient == this.sunAmbient
				&& this.sunLight == other.sunLight && other.sunBacklight == this.sunBacklight
				&& other.brightpass == this.brightpass && other.bloom == this.bloom
				&& this.whitePoint == other.whitePoint && this.fogColour == other.fogColour
				&& this.fogDepth == other.fogDepth && this.sunCubeTexture == other.sunCubeTexture
				&& this.skybox == other.skybox;
	}
    
    public int getFogColor() {
        return this.fogColour;
    }

    public Skybox getSkybox() {
        return this.skybox;
    }
}
