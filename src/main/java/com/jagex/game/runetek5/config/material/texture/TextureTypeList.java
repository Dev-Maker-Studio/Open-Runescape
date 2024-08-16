package com.jagex.game.runetek5.config.material.texture;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpCombine;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpBoxBlur;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpColorize;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpColorFill;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpMonochromeFill;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpCurve;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpWaveform;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpPerlinNoise;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpColorGradient;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpHorizontalGradient;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpSprite;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpFlip;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpInterpolate;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpWeave;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpVoronoiNoise;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpHerringbone;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpHslAdjust;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpKaleidoscope;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpMonochrome;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpBrightness;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpBinary;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpSquareWaveform;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpRasterizer;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpRange;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpMandelbrot;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpEmboss;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpColorEdgeDetector;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpMonochromeEdgeDetector;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOp37;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpLineNoise;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpBricks;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpNoise;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpTiledSprite;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpTexture;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpTile;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpPolarDistortion;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpClamp;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpInvert;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpVerticalGradient;
import com.jagex.game.runetek5.graphics.light.Skybox;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * @author Brainpower - 02/06/2020
 * @author OpenOSRS - All operation names
 *
 */
public class TextureTypeList {
	
	public static final int[] BRIGHTNESS_TABLE = new int[256];
	public static final int TEXTURE_GROUP_ID = -1;
	
	public static Js5ResourceProvider modelResource;
	public static double currentBrightness = -1.0;
	
	public static Js5ResourceProvider textureSpriteResource;
	public static MaterialProvider materialProvider;
	
	public static void initialize(Js5ResourceProvider modelResource, MaterialProvider materialProvider) {
	    TextureTypeList.modelResource = modelResource;
	    Skybox.materialProvider = materialProvider;
	}
	
	public static TextureOperation listOperation(Packet stream) {
	    stream.readUnsignedByte();
	    int implementationType = stream.readUnsignedByte();
	    TextureOperation textureNode = implementationForType(implementationType);
	    textureNode.imageCacheCapacity = stream.readUnsignedByte();
	    int opcodeCount = stream.readUnsignedByte();
	    for (int index = 0; index < opcodeCount; index++) {
	        int opcode = stream.readUnsignedByte();
	        textureNode.decodeOpcode(opcode, stream);
	    }
	    textureNode.postDecode();
	    return textureNode;
	}
	
	public static void setBrightness(double brightness) {
	    if (brightness != TextureTypeList.currentBrightness) {
	        for (int grade = 0; grade < 256; grade++) {
	            int value = (int) (Math.pow(grade / 255.0D, brightness) * 255.0D);
	            TextureTypeList.BRIGHTNESS_TABLE[grade] = Math.min(255, value);
	        }
	        TextureTypeList.currentBrightness = brightness;
	    }
	}

	public static TextureOperation implementationForType(int type) {
	    switch (type) {
	        case 0:
	            return new TextureOpMonochromeFill();
	        case 1:
	            return new TextureOpColorFill();
	        case 2:
	            return new TextureOpHorizontalGradient();
	        case 3:
	            return new TextureOpVerticalGradient();
	        case 4:
	            return new TextureOpBricks();
	        case 5:
	            return new TextureOpBoxBlur();
	        case 6:
	            return new TextureOpClamp();
	        case 7:
	            return new TextureOpCombine();
	        case 8:
	            return new TextureOpCurve();
	        case 9:
	            return new TextureOpFlip();
	        case 10:
	            return new TextureOpColorGradient();
	        case 11:
	            return new TextureOpColorize();
	        case 12:
	            return new TextureOpWaveform();
	        case 13:
	            return new TextureOpNoise();
	        case 14:
	            return new TextureOpWeave();
	        case 15:
	            return new TextureOpVoronoiNoise();
	        case 16:
	            return new TextureOpHerringbone();
	        case 17:
	            return new TextureOpHslAdjust();
	        case 18:
	            return new TextureOpTiledSprite();
	        case 19:
	            return new TextureOpPolarDistortion();
	        case 20:
	            return new TextureOpTile();
	        case 21:
	            return new TextureOpInterpolate();
	        case 22:
	            return new TextureOpInvert();
	        case 23:
	            return new TextureOpKaleidoscope();
	        case 24:
	            return new TextureOpMonochrome();
	        case 25:
	            return new TextureOpBrightness();
	        case 26:
	            return new TextureOpBinary();
	        case 27:
	            return new TextureOpSquareWaveform();
	        case 28:
	            return new TextureOpIrregularBricks();
	        case 29:
	            return new TextureOpRasterizer();
	        case 30:
	            return new TextureOpRange();
	        case 31:
	            return new TextureOpMandelbrot();
	        case 32:
	            return new TextureOpEmboss();
	        case 33:
	            return new TextureOpColorEdgeDetector();
	        case 34:
	            return new TextureOpPerlinNoise();
	        case 35:
	            return new TextureOpMonochromeEdgeDetector();
	        case 36:
	            return new TextureOpTexture();
	        case 37:
	            return new TextureOp37();
	        case 38:
	            return new TextureOpLineNoise();
	        case 39:
	            return new TextureOpSprite();
	        default:
	            return null;
	    }
	}
}
