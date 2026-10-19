package com.rs.game.content.minigames.barbarianassault

import org.darkan.core.gameval.gen.Npc

enum class PenanceFamily(val displayName: String) {
    FIGHTER("Fighter"),
    RANGER("Ranger"),
    RUNNER("Runner"),
    HEALER("Healer"),
}

class FamilySpec(val npc: Int, val quota: Int, val cap: Int)

enum class Wave(val fighter: FamilySpec, val ranger: FamilySpec, val runner: FamilySpec, val healer: FamilySpec) {
    ONE(FamilySpec(Npc.npc_5044, 4, 2), FamilySpec(Npc.npc_5229, 4, 2), FamilySpec(Npc.npc_5220, 2, 2), FamilySpec(Npc.npc_5238, 2, 2)),
    TWO(FamilySpec(Npc.npc_5045, 5, 2), FamilySpec(Npc.npc_5230, 4, 2), FamilySpec(Npc.npc_5221, 3, 2), FamilySpec(Npc.npc_5239, 3, 3)),
    THREE(FamilySpec(Npc.npc_5213, 5, 2), FamilySpec(Npc.npc_5231, 6, 3), FamilySpec(Npc.npc_5222, 4, 2), FamilySpec(Npc.npc_5240, 3, 2)),
    FOUR(FamilySpec(Npc.npc_5214, 6, 3), FamilySpec(Npc.npc_5232, 6, 3), FamilySpec(Npc.npc_5223, 4, 3), FamilySpec(Npc.npc_5241, 4, 3)),
    FIVE(FamilySpec(Npc.npc_5215, 6, 3), FamilySpec(Npc.npc_5233, 6, 4), FamilySpec(Npc.npc_5224, 5, 4), FamilySpec(Npc.npc_5242, 5, 4)),
    SIX(FamilySpec(Npc.npc_5216, 6, 3), FamilySpec(Npc.npc_5234, 7, 4), FamilySpec(Npc.npc_5225, 6, 4), FamilySpec(Npc.npc_5243, 6, 4)),
    SEVEN(FamilySpec(Npc.npc_5217, 7, 4), FamilySpec(Npc.npc_5235, 7, 4), FamilySpec(Npc.npc_5226, 6, 5), FamilySpec(Npc.npc_5244, 7, 4)),
    EIGHT(FamilySpec(Npc.npc_5218, 7, 4), FamilySpec(Npc.npc_5236, 8, 4), FamilySpec(Npc.npc_5227, 7, 5), FamilySpec(Npc.npc_5245, 7, 5)),
    NINE(FamilySpec(Npc.npc_5219, 8, 4), FamilySpec(Npc.npc_5237, 8, 5), FamilySpec(Npc.npc_5228, 9, 5), FamilySpec(Npc.npc_5246, 8, 6)),
    TEN(FamilySpec(Npc.npc_5217, 7, 4), FamilySpec(Npc.npc_5235, 7, 4), FamilySpec(Npc.npc_5226, 6, 5), FamilySpec(Npc.npc_5244, 7, 4));

    val number get() = ordinal + 1
    val queen get() = this == TEN
    val next: Wave? get() = entries.getOrNull(ordinal + 1)

    fun spec(family: PenanceFamily) = when (family) {
        PenanceFamily.FIGHTER -> fighter
        PenanceFamily.RANGER -> ranger
        PenanceFamily.RUNNER -> runner
        PenanceFamily.HEALER -> healer
    }

    val ordinaryTotal get() = PenanceFamily.entries.sumOf { spec(it).quota }

    companion object {
        fun of(number: Int) = entries[(number - 1).coerceIn(0, entries.lastIndex)]
    }
}
