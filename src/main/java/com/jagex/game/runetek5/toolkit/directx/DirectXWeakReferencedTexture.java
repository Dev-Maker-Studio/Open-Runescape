package com.jagex.game.runetek5.toolkit.directx;

import com.jagex.game.runetek5.framebuffer.DirectXFrameBuffer;
import com.jagex.game.runetek5.graphics.texture.Direct3DRectangleTexture;

import jagdx.IDirect3DTexture;
import jagdx.IUnknown;

/**
 * 
 * @author Brainpower - 02/05/2022
 *
 */
public class DirectXWeakReferencedTexture implements DirectXFrameBuffer, DirectXFrameBufferObject, AutoCloseable {

    public long surfaceLevel;
    public final int level;
    public final Direct3DRectangleTexture texture;

    public DirectXWeakReferencedTexture(Direct3DRectangleTexture texture, int level) {
        this.level = level;
        this.texture = texture;
        this.texture.dxToolkit.addWeakReference(this);
    }

    @Override
    public int getUseWidth() {
        return this.texture.getTextureWidth();
    }

    @Override
    public long getSurfaceLevel() {
        if (this.surfaceLevel == 0L) {
            this.surfaceLevel = IDirect3DTexture.GetSurfaceLevel(this.texture.handle, this.level);
        }

        return this.surfaceLevel;
    }

    @Override
    public int getUseHeight() {
        return this.texture.getTextureHeight();
    }

    @Override
    public void destroy() {
        if (this.surfaceLevel != 0L) {
            this.texture.dxToolkit.requestHandleCleanup(this.surfaceLevel);
            this.surfaceLevel = 0L;
        }

    }

    @Override
    public void close() {
        destroy();
    }


    @Override
    public void deallocate() {
        if (this.surfaceLevel != 0L) {
            IUnknown.Release(this.surfaceLevel);
            this.surfaceLevel = 0L;
        }

        this.texture.dxToolkit.clearReference(this);
    }
}