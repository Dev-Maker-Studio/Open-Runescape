package com.jagex.game.runetek5.client;

public class SkillConstants {

    public static final int[] SKILL_MAX_LEVEL = {99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 120};
    public static final int[] SKILL_XP_FOR_LEVEL = new int[120];

    static {
        int sum = 0;
        for (int level = 0; level < 120; level++) {
            int mod = level + 1;
            int offset = (int) (mod + 300.0D * Math.pow(2.0D, mod / 7.0D));
            sum += offset;
            SKILL_XP_FOR_LEVEL[level] = sum / 4;
        }
    }
}
