package com.jagex.game.runetek5.config

enum class SharedConfigsType(
    @JvmField val id: Int,
    @JvmField val bit: Int = 0
) {
    UNDERLAYS(1),
    UNUSED_2(2),
    IDENTITY_KIT(3),
    OVERLAYS(4),
    INVENTORIES(5),
    OBJECTS(6, 8),
    UNUSED_7(7),
    ENUMS(8, 8),
    NPCS(9, 7),
    ITEMS(10, 8),
    PARAMS(11),
    SEQ(12, 7),
    GFX(13, 8),
    VARPBIT(14, 10),
    VARC_STRING(15),
    VARS(16),
    UNUSED_17(17),
    UNUSED_18(18),
    VARC(19),
    UNUSED_20(20),
    UNUSED_21(21),
    UNUSED_22(22),
    UNUSED_23(23),
    VARN(24),
    VARNBIT(25),
    STRUCTS(26),
    UNUSED_27(27),
    UNUSED_28(28),
    SKYBOX(29),
    SUN(30),
    LIGHT_INTENSITIES(31),
    RENDER_ANIMS(32),
    CURSORS(33),
    MAP_SPRITES(34),
    QUESTS(35),
    MAP_AREAS(36),
    UNUSED_37(37),
    UNUSED_38(38),
    UNUSED_39(39),
    UNUSED_40(40),
    UNUSED_41(41),
    UNUSED_42(42),
    UNUSED_43(43),
    UNUSED_44(44),
    UNUSED_45(45),
    HITSPLATS(46),
    CLAN_VAR(47),
    UNUSED_48(48),
    UNUSED_49(49),
    UNUSED_50(50),
    UNUSED_51(51),
    UNUSED_53(53),
    CLAN_VAR_SETTINGS(54),
    UNUSED_70(70),
    HITBARS(72),
    UNUSED_73(73),
    UNUSED_74(74);

    fun archiveFileCapacity(): Int {
        return 1 shl bit
    }

    fun archiveId(fileId: Int): Int {
        return fileId ushr bit
    }

    fun archiveFileId(fileId: Int): Int {
        return fileId and ((1 shl bit) - 1)
    }
}
