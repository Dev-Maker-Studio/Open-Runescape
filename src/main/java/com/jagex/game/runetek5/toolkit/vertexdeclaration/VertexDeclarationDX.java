package com.jagex.game.runetek5.toolkit.vertexdeclaration;

import jagdx.IDirect3DDevice;
import jagdx.IUnknown;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;

public class VertexDeclarationDX extends VertexDeclaration {

    public long pointer;
    public final DirectXNativeToolkit aGraphicalRendererSub2Sub29178;

    public VertexDeclarationDX(DirectXNativeToolkit class505Sub2Sub21, VertexDeclarationComponent[] arr_2) {
        super(arr_2);
        this.aGraphicalRendererSub2Sub29178 = class505Sub2Sub21;
        byte b_3 = 0;
        ByteBuffer bytebuffer4 = this.aGraphicalRendererSub2Sub29178.directBuffer;
        bytebuffer4.clear();

        for (short s_5 = 0; s_5 < super.vertexDeclarationComponents.length; s_5++) {
            short s_6 = 0;
            VertexDeclarationComponent class727 = super.vertexDeclarationComponents[s_5];

            for (int i_8 = 0; i_8 < class727.getSize(); i_8++) {
                VertexDeclarationAction class699 = class727.getAction(i_8);
                if (class699 == VertexDeclarationAction.aClass69695) {
                    method14506(bytebuffer4, s_5, s_6, (byte) 2, (byte) 0, (byte) 0, (byte) 0);
                } else if (class699 == VertexDeclarationAction.NORMAL_POINTER_ACTION) {
                    method14506(bytebuffer4, s_5, s_6, (byte) 2, (byte) 0, (byte) 3, (byte) 0);
                } else if (class699 == VertexDeclarationAction.aClass69690) {
                    method14506(bytebuffer4, s_5, s_6, (byte) 4, (byte) 0, (byte) 10, (byte) 0);
                } else {
                    byte b_14;
                    byte b_15;
                    byte b_16;
                    byte b_17;
                    if (class699 == VertexDeclarationAction.aClass69691) {
                        b_14 = 0;
                        b_15 = 0;
                        b_16 = 5;
                        b_17 = b_3++;
                        method14506(bytebuffer4, s_5, s_6, b_14, b_15, b_16, b_17);
                    } else if (class699 == VertexDeclarationAction.aClass69692) {
                        b_14 = 1;
                        b_15 = 0;
                        b_16 = 5;
                        b_17 = b_3++;
                        method14506(bytebuffer4, s_5, s_6, b_14, b_15, b_16, b_17);
                    } else if (class699 == VertexDeclarationAction.aClass69693) {
                        b_14 = 2;
                        b_15 = 0;
                        b_16 = 5;
                        b_17 = b_3++;
                        method14506(bytebuffer4, s_5, s_6, b_14, b_15, b_16, b_17);
                    } else if (class699 == VertexDeclarationAction.aClass69694) {
                        b_14 = 3;
                        b_15 = 0;
                        b_16 = 5;
                        b_17 = b_3++;
                        method14506(bytebuffer4, s_5, s_6, b_14, b_15, b_16, b_17);
                    }
                }

                s_6 += class699.pointerOffsetStep;
            }
        }

        method14506(bytebuffer4, (short) 255, (short) 0, (byte) 17, (byte) 0, (byte) 0, (byte) 0);
        this.pointer = IDirect3DDevice.CreateVertexDeclaration(this.aGraphicalRendererSub2Sub29178.renderTarget, this.aGraphicalRendererSub2Sub29178.directBufferAddress);
        this.aGraphicalRendererSub2Sub29178.addWeakReference(this);
    }

    @Override
    public void deallocate() {
        if (this.pointer != 0L) {
            IUnknown.Release(this.pointer);
            this.pointer = 0L;
        }

        this.aGraphicalRendererSub2Sub29178.clearReference(this);
    }

    public void method14505() {
        if (this.pointer != 0L) {
            this.aGraphicalRendererSub2Sub29178.requestHandleCleanup(this.pointer);
            this.pointer = 0L;
        }

    }

    @Override
    protected void finalize() {
        method14505();
    }

    public void method14506(ByteBuffer bytebuffer1, short s_2, short s_3, byte b_4, byte b_5, byte b_6, byte b_7) {
        bytebuffer1.putShort(s_2);
        bytebuffer1.putShort(s_3);
        bytebuffer1.put(b_4);
        bytebuffer1.put(b_5);
        bytebuffer1.put(b_6);
        bytebuffer1.put(b_7);
    }

}
