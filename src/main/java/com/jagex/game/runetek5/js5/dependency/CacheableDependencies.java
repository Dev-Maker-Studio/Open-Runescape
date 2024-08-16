package com.jagex.game.runetek5.js5.dependency;

public class CacheableDependencies {
    public static final Dependency DEFAULTS;
    public static final Dependency JACLIB;
    public static final Dependency JAGGL;
    public static final Dependency JAGDX;
    public static final Dependency SW3D;
    public static final Dependency HW3D;
    public static final Dependency JAGTHEORA;
    public static final Dependency SHADERS;
    public static final Dependency MATERIALS;
    public static final Dependency CONFIG;
    public static final Dependency LOC;
    public static final Dependency ENUM;
    public static final Dependency NPC;
    public static final Dependency OBJ;
    public static final Dependency SEQ;
    public static final Dependency GFX;
    public static final Dependency VARBIT;
    public static final Dependency QC_MSG;
    public static final Dependency QC_MENU;
    public static final Dependency TEXTURE_MEDIA;
    public static final Dependency PARTICLES;
    public static final Dependency BILLBOARDS;
    public static final Dependency HUFFMAN;
    public static final Dependency INTERFACES;
    public static final Dependency CS2;
    public static final Dependency FONTS;
    public static final Dependency WORLD_MAP;
    static {
        DEFAULTS = new Dependency(DependencyType.CONFIG);
        JACLIB = new Dependency(DependencyType.NATIVE);
        JAGGL = new Dependency(DependencyType.NATIVE);
        JAGDX = new Dependency(DependencyType.NATIVE);
        SW3D = new Dependency(DependencyType.NATIVE);
        HW3D = new Dependency(DependencyType.NATIVE);
        JAGTHEORA = new Dependency(DependencyType.NATIVE);
        SHADERS = new Dependency(DependencyType.CONFIG);
        MATERIALS = new Dependency(DependencyType.CONFIG);
        CONFIG = new Dependency(DependencyType.CONFIG);
        LOC = new Dependency(DependencyType.CONFIG);
        ENUM = new Dependency(DependencyType.CONFIG);
        NPC = new Dependency(DependencyType.CONFIG);
        OBJ = new Dependency(DependencyType.CONFIG);
        SEQ = new Dependency(DependencyType.CONFIG);
        GFX = new Dependency(DependencyType.CONFIG);
        VARBIT = new Dependency(DependencyType.CONFIG);
        QC_MSG = new Dependency(DependencyType.CONFIG);
        QC_MENU = new Dependency(DependencyType.CONFIG);
        TEXTURE_MEDIA = new Dependency(DependencyType.CONFIG);
        PARTICLES = new Dependency(DependencyType.CONFIG);
        BILLBOARDS = new Dependency(DependencyType.CONFIG);
        HUFFMAN = new Dependency(DependencyType.ENCODING);
        INTERFACES = new Dependency(DependencyType.CONFIG);
        CS2 = new Dependency(DependencyType.CONFIG);
        FONTS = new Dependency(DependencyType.CONFIG);
        WORLD_MAP = new Dependency(DependencyType.LABELS);
    }
    
    public static Dependency[] getAll() {
        return new Dependency[]{DEFAULTS, JACLIB, JAGGL, JAGDX, SW3D, HW3D, JAGTHEORA, SHADERS, MATERIALS, CONFIG, LOC, ENUM, NPC, OBJ, SEQ, GFX, VARBIT, QC_MSG, QC_MENU, TEXTURE_MEDIA, PARTICLES, BILLBOARDS, HUFFMAN, INTERFACES, CS2, FONTS, WORLD_MAP};
    }
}
