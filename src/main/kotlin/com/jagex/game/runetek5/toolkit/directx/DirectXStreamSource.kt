package com.jagex.game.runetek5.toolkit.directx

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.utils.Utils
import jaclib.memory.Source
import jagdx.IDirect3DDevice
import jagdx.IDirect3DVertexBuffer
import jagdx.IUnknown

class DirectXStreamSource(
    val directxToolkit: DirectXNativeToolkit,
    val aBool3552: Boolean
) : NativeStream, AutoCloseable {

    @JvmField var capacity: Int = 0
    @JvmField var aByte3547: Byte = 0
    @JvmField var handle: Long = 0
    @JvmField var anInt3548: Int = 0

    init {
        directxToolkit.addWeakReference(this)
    }

    override fun createFromSource(size: Int, type: Int, source: Source): Boolean {
        return create(size, type) && Utils.equalOrAboveZero(
            IDirect3DVertexBuffer.Upload(handle, 0, capacity, if (aBool3552) 8192 else 0, source.getBufferAddress())
        )
    }

    fun method5317(): Int {
        return aByte3547.toInt()
    }

    override fun create(size: Int, type: Int): Boolean {
        capacity = size
        aByte3547 = type.toByte()
        if (capacity > anInt3548) {
            var flags = 8
            val pool: Byte
            if (aBool3552) {
                pool = 0
                flags = flags or 0x200
            } else {
                pool = 1
            }

            if (handle != 0L) {
                IUnknown.Release(handle)
            }

            handle = IDirect3DDevice.CreateVertexBuffer(directxToolkit.renderTarget, capacity, flags, 0, pool.toInt())
            anInt3548 = capacity
        }

        return handle != 0L
    }

    override fun getDataStorePointer(offset: Int, size: Int): Long {
        return IDirect3DVertexBuffer.Lock(handle, offset, size, if (aBool3552) 8192 else 0)
    }

    override fun unlock() {
        IDirect3DVertexBuffer.Unlock(handle)
    }

    override fun upload(offset: Int, size: Int, address: Long): Boolean {
        return Utils.equalOrAboveZero(
            IDirect3DVertexBuffer.Upload(handle, offset, size, if (aBool3552) 8192 else 0, address)
        )
    }

    override fun deallocate() {
        if (handle != 0L) {
            IUnknown.Release(handle)
            handle = 0L
        }

        anInt3548 = 0
        capacity = 0
        directxToolkit.clearReference(this)
    }

    fun cleanup() {
        if (handle != 0L) {
            directxToolkit.requestHandleCleanup(handle)
            handle = 0L
            anInt3548 = 0
            capacity = 0
        }
    }

    override fun close() {
        cleanup()
    }

    override fun getCapacity(): Int {
        return capacity
    }
}
