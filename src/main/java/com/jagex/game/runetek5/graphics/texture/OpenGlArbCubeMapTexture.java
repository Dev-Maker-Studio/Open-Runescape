package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
import jaggl.OpenGL;

public class OpenGlArbCubeMapTexture extends OpenGlTexture {

	public final int largestMaterialSize;

	public OpenGlArbCubeMapTexture(OpenGlToolkit toolkit, GLFormat controller, GLActionFormat format, int largestMaterialSize) {
		super(toolkit, GL_TEXTURE_CUBE_MAP, controller, format, largestMaterialSize * largestMaterialSize * 6, false);
		this.largestMaterialSize = largestMaterialSize;
		this.activeToolkit.setActiveTexture(this);

		for (int index = 0; index < 6; index++) {
			OpenGL.glTexImage2Dub(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0,
					OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, super.aClass761651), largestMaterialSize, largestMaterialSize, 0,
					OpenGlToolkit.format(this.aClass1501650), GL_UNSIGNED_BYTE, null, 0);
		}

		setLinearTextureFilter(true);
	}

	public OpenGlArbCubeMapTexture(OpenGlToolkit toolkit, GLFormat controller, GLActionFormat format, int largestMaterialSize,
			boolean mipmapping, int[][] pixelsForTextures) {
		super(toolkit, GL_TEXTURE_CUBE_MAP, controller, format, largestMaterialSize * largestMaterialSize * 6, mipmapping);
		this.largestMaterialSize = largestMaterialSize;
		this.activeToolkit.setActiveTexture(this);
		if (mipmapping) {
			for (int index = 0; index < 6; index++) {
				createMipmappedCubeTexture(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X,
						OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), largestMaterialSize, largestMaterialSize,
						this.activeToolkit.pixelType, pixelsForTextures[index]);
			}
		} else {
			for (int index = 0; index < 6; index++) {
				OpenGL.glTexImage2Di(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0,
						OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), largestMaterialSize, largestMaterialSize, 0,
						GL_BGRA, this.activeToolkit.pixelType, pixelsForTextures[index], 0);
			}
		}

		setLinearTextureFilter(true);
	}

	public OpenGlArbCubeMapTexture(OpenGlToolkit toolkit, GLFormat controller, GLActionFormat format,
			byte[][] data, GLFormat controllerB) {
		super(toolkit, GL_TEXTURE_CUBE_MAP, controller, format, 24576, false);
		this.largestMaterialSize = 64;
		this.activeToolkit.setActiveTexture(this);

		for (int index = 0; index < 6; index++) {
			OpenGL.glTexImage2Dub(index + GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0,
					OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), 64, 64, 0,
					OpenGlToolkit.format(controllerB), GL_UNSIGNED_BYTE, data[index], 0);
		}
		setLinearTextureFilter(true);
	}

	public FrameBuffer getAttachableTexture(int i_1) {
		return new OpenGlAttachableCubeMapTexture(this, i_1);
	}

}
