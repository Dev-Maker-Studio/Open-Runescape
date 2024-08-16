package com.jagex.game.runetek5.toolkit.vertexdeclaration;

import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.utils.BufferUtils;

public class VertexDeclarationAction {

    public static final VertexDeclarationAction aClass69695;
    public static final VertexDeclarationAction NORMAL_POINTER_ACTION;
    public static final VertexDeclarationAction aClass69690;
    public static final VertexDeclarationAction aClass69691;
    public static final VertexDeclarationAction aClass69692;
    public static final VertexDeclarationAction aClass69693;
    public static final VertexDeclarationAction aClass69694;
    public static final VertexDeclarationAction aClass69696;
    public static final VertexDeclarationAction aClass69700;
    public static final VertexDeclarationAction aClass69699;
    public static final VertexDeclarationAction aClass69688;
    public static final VertexDeclarationAction aClass69697;
    static final int anInt702;

    static {
        NORMAL_POINTER_ACTION = new VertexDeclarationAction(0, 1, 3, GLActionFormat.FLOAT_32_ARB);
        aClass69696 = new VertexDeclarationAction(1, 7, 4, GLActionFormat.FLOAT_32_ARB);
        aClass69694 = new VertexDeclarationAction(2, 6, 4, GLActionFormat.FLOAT_32_ARB);
        aClass69699 = new VertexDeclarationAction(3, 9, 4, GLActionFormat.FLOAT_32_ARB);
        aClass69692 = new VertexDeclarationAction(4, 4, 2, GLActionFormat.FLOAT_32_ARB);
        aClass69688 = new VertexDeclarationAction(5, 10, 3, GLActionFormat.FLOAT_32_ARB);
        aClass69697 = new VertexDeclarationAction(6, 11, 3, GLActionFormat.FLOAT_32_ARB);
        aClass69695 = new VertexDeclarationAction(7, 0, 3, GLActionFormat.FLOAT_32_ARB);
        aClass69691 = new VertexDeclarationAction(8, 3, 1, GLActionFormat.FLOAT_32_ARB);
        aClass69693 = new VertexDeclarationAction(9, 5, 3, GLActionFormat.FLOAT_32_ARB);
        aClass69690 = new VertexDeclarationAction(10, 2, 4, GLActionFormat.PIXEL_FORMAT);
        aClass69700 = new VertexDeclarationAction(11, 8, 4, GLActionFormat.FLOAT_32_ARB);
        anInt702 = BufferUtils.ilog(16);
    }

    public int openglAction;
    public int pointerOffsetStep;
    int slot;
    public int anInt705;
    public GLActionFormat actionFormatOpengl;

    public VertexDeclarationAction(int openglAction, int slot, int i_3, GLActionFormat actionFormat) {
        this.openglAction = openglAction;
        this.slot = slot;
        this.anInt705 = i_3;
        this.actionFormatOpengl = actionFormat;
        this.pointerOffsetStep = this.actionFormatOpengl.bytesPerEntry * this.anInt705;
        if (this.slot >= 16) {
            throw new RuntimeException();
        }
    }

    static VertexDeclarationAction forType(int type) {
        switch (type) {
            case 0:
                return aClass69695;
            case 1:
                return NORMAL_POINTER_ACTION;
            case 2:
                return aClass69690;
            case 3:
                return aClass69691;
            case 4:
                return aClass69692;
            case 5:
                return aClass69693;
            case 6:
                return aClass69694;
            default:
                return null;
        }
    }

}
