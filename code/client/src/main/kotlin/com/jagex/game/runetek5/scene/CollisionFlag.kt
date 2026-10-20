package com.jagex.game.runetek5.scene

enum class CollisionFlag(@JvmField val flag: Int) {
    EMPTY(0xFFFFFFFF.toInt()),

    NW_OBJ(0x1),
    N_OBJ(0x2),
    NE_OBJ(0x4),
    E_OBJ(0x8),
    SE_OBJ(0x10),
    S_OBJ(0x20),
    SW_OBJ(0x40),
    W_OBJ(0x80),
    OBJ(0x100),

    NW_BOUND(0x200),
    N_BOUND(0x400),
    NE_BOUND(0x800),
    E_BOUND(0x1000),
    SE_BOUND(0x2000),
    S_BOUND(0x4000),
    SW_BOUND(0x8000),
    W_BOUND(0x10000),

    BLOCKED_OBJ(0x20000),
    BLOCKED_DECO(0x40000),
    BLOCKED(0x200000),

    NW_ALT_OBJ(0x400000),
    N_ALT_OBJ(0x800000),
    NE_ALT_OBJ(0x1000000),
    E_ALT_OBJ(0x2000000),
    SE_ALT_OBJ(0x4000000),
    S_ALT_OBJ(0x8000000),
    SW_ALT_OBJ(0x10000000),
    W_ALT_OBJ(0x20000000),
    ALT_OBJ(0x40000000)
}
