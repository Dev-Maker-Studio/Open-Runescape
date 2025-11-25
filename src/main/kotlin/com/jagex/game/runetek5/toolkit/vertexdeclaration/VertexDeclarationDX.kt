package com.jagex.game.runetek5.toolkit.vertexdeclaration

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import jagdx.IDirect3DDevice
import jagdx.IUnknown
import java.nio.ByteBuffer

class VertexDeclarationDX(
    val aGraphicalRendererSub2Sub29178: DirectXNativeToolkit,
    vertexDeclarationComponents: Array<VertexDeclarationComponent>
) : VertexDeclaration(vertexDeclarationComponents), AutoCloseable {

    @JvmField var pointer: Long = 0

    init {
        var textureCoordCount: Byte = 0
        val buffer = aGraphicalRendererSub2Sub29178.directBuffer
        buffer.clear()

        for (streamIndex in vertexDeclarationComponents.indices) {
            var offset: Short = 0
            val component = vertexDeclarationComponents[streamIndex]

            for (actionIndex in 0 until component.getSize()) {
                val action = component.getAction(actionIndex)
                if (action == VertexDeclarationAction.aClass69695) {
                    method14506(buffer, streamIndex.toShort(), offset, 2, 0, 0, 0)
                } else if (action == VertexDeclarationAction.NORMAL_POINTER_ACTION) {
                    method14506(buffer, streamIndex.toShort(), offset, 2, 0, 3, 0)
                } else if (action == VertexDeclarationAction.aClass69690) {
                    method14506(buffer, streamIndex.toShort(), offset, 4, 0, 10, 0)
                } else {
                    val dataType: Byte
                    val method: Byte
                    val usage: Byte
                    val usageIndex: Byte
                    when (action) {
                        VertexDeclarationAction.aClass69691 -> {
                            dataType = 0
                            method = 0
                            usage = 5
                            usageIndex = textureCoordCount++
                            method14506(buffer, streamIndex.toShort(), offset, dataType, method, usage, usageIndex)
                        }
                        VertexDeclarationAction.aClass69692 -> {
                            dataType = 1
                            method = 0
                            usage = 5
                            usageIndex = textureCoordCount++
                            method14506(buffer, streamIndex.toShort(), offset, dataType, method, usage, usageIndex)
                        }
                        VertexDeclarationAction.aClass69693 -> {
                            dataType = 2
                            method = 0
                            usage = 5
                            usageIndex = textureCoordCount++
                            method14506(buffer, streamIndex.toShort(), offset, dataType, method, usage, usageIndex)
                        }
                        VertexDeclarationAction.aClass69694 -> {
                            dataType = 3
                            method = 0
                            usage = 5
                            usageIndex = textureCoordCount++
                            method14506(buffer, streamIndex.toShort(), offset, dataType, method, usage, usageIndex)
                        }
                    }
                }

                offset = (offset + action.pointerOffsetStep).toShort()
            }
        }

        method14506(buffer, 255, 0, 17, 0, 0, 0)
        pointer = IDirect3DDevice.CreateVertexDeclaration(aGraphicalRendererSub2Sub29178.renderTarget, aGraphicalRendererSub2Sub29178.directBufferAddress)
        aGraphicalRendererSub2Sub29178.addWeakReference(this)
    }

    override fun deallocate() {
        if (pointer != 0L) {
            IUnknown.Release(pointer)
            pointer = 0L
        }

        aGraphicalRendererSub2Sub29178.clearReference(this)
    }

    fun method14505() {
        if (pointer != 0L) {
            aGraphicalRendererSub2Sub29178.requestHandleCleanup(pointer)
            pointer = 0L
        }
    }

    override fun close() {
        method14505()
    }

    fun method14506(buffer: ByteBuffer, stream: Short, offset: Short, dataType: Byte, method: Byte, usage: Byte, usageIndex: Byte) {
        buffer.putShort(stream)
        buffer.putShort(offset)
        buffer.put(dataType)
        buffer.put(method)
        buffer.put(usage)
        buffer.put(usageIndex)
    }
}
