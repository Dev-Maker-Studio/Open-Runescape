package com.jagex.game.runetek5.config;

public enum DefaultsFile {

    SCENE_CUBE_TEXTURES(1),
    UNUSED2(2),
    ENTITY(3),
    TITLESCREEN_MUSIC_INFO(4),
    UNUSED5(5),
    EQUIPMENT(6),
    INTERACTIONS(7),
    CUTSCENE(8);

    public final int fileId;

    DefaultsFile(int fileId) {
        this.fileId = fileId;
    }
}
