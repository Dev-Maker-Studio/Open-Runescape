package com.jagex.game.runetek5.graphics.model;

public class ModelFunctionMaskDX {

    ModelFunctionMaskDX() {
        throw new Error();
    }

    public static boolean copyVerticesY(int i_0) {
        return (i_0 & 0x22) != 0;
    }

    public static boolean copyVerticesZ(int i_0) {
        return (i_0 & 0x34) != 0;
    }

    public static boolean method981(int i_0) {
        return (i_0 & 0x37) != 0;
    }

    public static boolean method982(int i_0) {
        return (i_0 & 0x220) == 544 | (i_0 & 0x18) != 0;
    }

    public static boolean method983(int i_0, int i_1) {
        return (i_0 & 0x800) != 0 && (i_1 & 0x37) != 0;
    }

    public static boolean copyAlpha(int i_0) {
        return (i_0 & 0x100) != 0;
    }

    public static boolean deepCloneUv() {
        return false;
    }

    public static boolean deepCloneNormals(int i_0) {
        return (i_0 & 0x10) != 0;
    }

    public static boolean method987(int i_0, int i_1) {
        return copyColors(i_0) || method1051(i_0, i_1);
    }

    public static boolean copyBillboardBackgrounds(int i_0) {
        return (i_0 & 0xc580) != 0;
    }

    public static boolean method989(int i_0) {
        return (i_0 & 0x10000) != 0;
    }

    public static boolean method990(int i_0) {
        return (i_0 & 0x800) != 0;
    }

    public static boolean deepCloneMaterials(int i_0) {
        return (i_0 & 0x8000) != 0;
    }

    public static boolean method993(int i_0) {
        return (i_0 & 0x800) != 0;
    }

    public static boolean copyVerticesX(int i_0) {
        return (i_0 & 0x21) != 0;
    }

    public static boolean method995(int i_0) {
        return (copyVerticesX(i_0) | copyVerticesY(i_0) | copyVerticesZ(i_0)) & method990(i_0);
    }

    public static boolean method996(int i_0, int i_1) {
        boolean bool2 = (i_1 & 0x37) != 0 ? deepCloneLights(i_0, i_1) : method1051(i_0, i_1);
        return (i_0 & 0x10000) != 0 | method982(i_0) | bool2;
    }

    public static boolean hasNormalsBuffer(int i_0) {
        return deepCloneNormals(i_0) & method1043(i_0);
    }

    public static boolean method1000(int i_0) {
        return (i_0 & 0x70000) != 0 || copyVerticesX(i_0) || method995(i_0);
    }

    public static boolean method1001(int i_0) {
        return (i_0 & 0x70000) != 0 || copyVerticesY(i_0) || method995(i_0);
    }

    public static boolean method1002(int i_0) {
        return (i_0 & 0x70000) != 0 || copyVerticesZ(i_0) || method995(i_0);
    }

    public static boolean method1004(int i_0, int i_1) {
        return (i_0 & 0x40000) != 0 | copyAlpha(i_0) || method1051(i_0, i_1);
    }

    public static boolean method1005(int i_0) {
        return deepCloneUv() || method1056(i_0);
    }

    public static boolean is0x180(int i_0) {
        return (i_0 & 0x180) != 0;
    }

    public static boolean is0x20(int i_0) {
        return (i_0 & 0x20) != 0;
    }

    public static boolean is0x400(int i_0) {
        return (i_0 & 0x400) != 0;
    }

    public static boolean method1009(int i_0) {
        return (i_0 & 0x60000) != 0 | deepCloneNormals(i_0) || hasNormalsBuffer(i_0);
    }

    public static boolean method1010(int i_0, int i_1) {
        return (i_0 & 0x800) != 0 | deepCloneMaterials(i_0) || method1051(i_0, i_1);
    }

    public static boolean deepCloneLights(int i_0, int i_1) {
        return method983(i_0, i_1) & ((i_0 & 0x2000) != 0 | method982(i_0) | method989(i_0));
    }

    public static boolean copyColors(int i_0) {
        return (i_0 & 0x84080) != 0;
    }

    public static boolean method1043(int i_0) {
        return (i_0 & 0x800) != 0;
    }

    public static boolean method1051(int functionMask, int preferenceMask) {
        return method1059(functionMask) && ((functionMask & 0x9000) != 0 | copyColors(functionMask) | copyAlpha(functionMask) || (preferenceMask & 0x37) == 0 & ((functionMask & 0x2000) != 0 | method982(functionMask) | method989(functionMask)));
    }

    public static boolean method1056(int i_0) {
        return deepCloneUv() & method993(i_0);
    }

    public static boolean method1059(int i_0) {
        return (i_0 & 0x800) != 0;
    }

}
