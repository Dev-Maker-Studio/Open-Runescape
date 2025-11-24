package com.jagex.game.runetek5.toolkit.directx

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.utils.Utils
import jagdx.IDirect3DDevice
import jagdx.IDirect3DIndexBuffer
import jagdx.IUnknown

class DirectXIndexBuffer(
    @JvmField val dxToolkit: DirectXNativeToolkit,
    @JvmField val actionFormat: GLActionFormat,
    @JvmField val aBool4236: Boolean
) : DirectXBuffer, AutoCloseable {

    @JvmField var handle: Long = 0
    @JvmField var sizeOccupied: Int = 0
    @JvmField var sizeRequired: Int = 0

    init {
        dxToolkit.addWeakReference(this)
    }

    override fun getCapacity(): Int {
        return sizeRequired
    }

    override fun getDataStorePointer(i_1: Int, i_2: Int): Long {
        return IDirect3DIndexBuffer.Lock(handle, i_1, i_2, if (aBool4236) 8192 else 0)
    }

    override fun unlock() {
        IDirect3DIndexBuffer.Unlock(handle)
    }

    override fun deallocate() {
        if (handle != 0L) {
            IUnknown.Release(handle)
            handle = 0L
        }

        sizeOccupied = 0
        sizeRequired = 0
        dxToolkit.clearReference(this)
    }

    override fun close() {
        cleanup()
    }

    override fun create(i_1: Int) {
        sizeRequired = actionFormat.bytesPerEntry * i_1
        if (sizeRequired > sizeOccupied) {
            if (handle != 0L) {
                IUnknown.Release(handle)
            }

            var mask = 8
            val type: Byte
            if (aBool4236) {
                type = 0
                mask = mask or 0x200
            } else {
                type = 1
            }

            handle = IDirect3DDevice.CreateIndexBuffer(dxToolkit.renderTarget, sizeRequired, mask, if (actionFormat == GLActionFormat.TEXTURE_FORMAT) 101 else 102, type.toInt())
            sizeOccupied = sizeRequired
        }
    }

    override fun upload(i_1: Int, i_2: Int, long_3: Long): Boolean {
        return Utils.equalOrAboveZero(IDirect3DIndexBuffer.Upload(handle, i_1, i_2, if (aBool4236) 8192 else 0, long_3))
    }

    fun cleanup() {
        if (handle != 0L) {
            dxToolkit.requestHandleCleanup(handle)
            handle = 0L
        }

        sizeOccupied = 0
        sizeRequired = 0
    }
}
