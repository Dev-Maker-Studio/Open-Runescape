package com.rs.game.content.minigames.greatorb

import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Model
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Spotanim

enum class OrbTeam(
    val varbitValue: Int,
    val displayName: String,
    val leader: Int,
    val hat: Int,
    val attractor: Int,
    val repeller: Int,
    val barrierGenerator: Int,
    val barrier: Int,
    val orb: Int,
    val robes: IntArray,
    val attractSpotanim: Int,
    val repelSpotanim: Int,
    val barrierSpotanim: Int,
    val resultModel: Int,
    val panel: Int,
    val timer: Int,
    val ownScore: Int,
    val enemyScore: Int,
) {
    YELLOW(
        1, "yellow", Npc.rcguild_wizard_vief, Obj.rcguild_hat_yellow_ingame, Obj.rcguild_attracter_yellow, Obj.rcguild_repeller_yellow,
        Obj.rcguild_barrier_yellow, Loc.rcguild_magic_barrier_yellow, Npc.npc_8021,
        intArrayOf(Obj.rcguild_robetop_yellow, Obj.rcguild_robebottom_yellow, Obj.rcguild_gloves_yellow),
        Spotanim.rcguild_attract_ready_2, Spotanim.rcguild_repel_ready_2, Spotanim.rcguild_magic_barrier_spawn_2, Model.model_41480,
        Component.interface_781.component_781_14, Component.interface_781.component_781_42,
        Component.interface_781.component_781_47, Component.interface_781.component_781_48,
    ),
    GREEN(
        2, "green", Npc.rcguild_wizard_acantha, Obj.rcguild_hat_green_ingame, Obj.rcguild_attracter_green, Obj.rcguild_repeller_green,
        Obj.rcguild_barrier_green, Loc.rcguild_magic_barrier_green, Npc.npc_8025,
        intArrayOf(Obj.rcguild_robetop_green, Obj.rcguild_robebottom_green, Obj.rcguild_gloves_green),
        Spotanim.rcguild_attract_ready, Spotanim.rcguild_repel_ready, Spotanim.rcguild_magic_barrier_spawn, Model.model_41479,
        Component.interface_781.component_781_13, Component.interface_781.component_781_33,
        Component.interface_781.component_781_38, Component.interface_781.component_781_39,
    );

    val opponent get() = if (this == YELLOW) GREEN else YELLOW
    val tools get() = intArrayOf(hat, attractor, repeller, barrierGenerator)

    companion object {
        val allTools = entries.flatMap { it.tools.toList() }.toIntArray()

        fun forVarbit(value: Int) = entries.firstOrNull { it.varbitValue == value }
        fun forLeader(npc: Int) = entries.firstOrNull { it.leader == npc }
        fun forOrb(npc: Int) = entries.firstOrNull { it.orb == npc }
        fun forBarrier(loc: Int) = entries.firstOrNull { it.barrier == loc }
    }
}
