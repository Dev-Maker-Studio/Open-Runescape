package com.rs.game.content.minigames.barbarianassault

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

const val MAX_ROLE_LEVEL = 5

enum class Role(
    val displayName: String,
    val icons: IntArray,
    val horns: IntArray,
    val hud: Int,
    val waveLine: Int,
    val receivedLines: IntArray,
    val callLine: Int,
    val partnerLine: Int,
    val levelVarbit: Int,
    val pointsVarbit: Int,
    val pointsCap: Int,
) {
    ATTACKER(
        "Attacker",
        intArrayOf(Obj.barbassault_playericon_attacker_01, Obj.barbassault_playericon_attacker_02, Obj.barbassault_playericon_attacker_03, Obj.barbassault_playericon_attacker_04, Obj.barbassault_playericon_attacker_05),
        intArrayOf(Obj.barbassault_att_horn_01, Obj.barbassault_att_horn_02, Obj.barbassault_att_horn_03, Obj.barbassault_att_horn_04, Obj.barbassault_att_horn_05),
        Interface.interface_485,
        Component.interface_485.component_485_2,
        intArrayOf(Component.interface_485.component_485_4, Component.interface_485.component_485_5),
        Component.interface_485.component_485_7,
        Component.interface_485.component_485_9,
        Varbit.barbassult_rolelevel_att,
        Varbit.varbit_3256,
        511,
    ),
    DEFENDER(
        "Defender",
        intArrayOf(Obj.barbassault_playericon_defender_01, Obj.barbassault_playericon_defender_02, Obj.barbassault_playericon_defender_03, Obj.barbassault_playericon_defender_04, Obj.barbassault_playericon_defender_05),
        intArrayOf(Obj.barbassault_defender_horn),
        Interface.interface_487,
        Component.interface_487.component_487_1,
        intArrayOf(Component.interface_487.component_487_3),
        Component.interface_487.component_487_5,
        Component.interface_487.component_487_7,
        Varbit.barbassult_rolelevel_def,
        Varbit.varbit_3263,
        511,
    ),
    COLLECTOR(
        "Collector",
        intArrayOf(Obj.barbassault_playericon_collector_01, Obj.barbassault_playericon_collector_02, Obj.barbassault_playericon_collector_03, Obj.barbassault_playericon_collector_04, Obj.barbassault_playericon_collector_05),
        intArrayOf(Obj.barbassault_horn_collector),
        Interface.interface_486,
        Component.interface_486.component_486_2,
        intArrayOf(Component.interface_486.component_486_4),
        Component.interface_486.component_486_6,
        Component.interface_486.component_486_8,
        Varbit.barbassult_rolelevel_col,
        Varbit.varbit_3261,
        511,
    ),
    HEALER(
        "Healer",
        intArrayOf(Obj.barbassault_playericon_healer_01, Obj.barbassault_playericon_healer_02, Obj.barbassault_playericon_healer_03, Obj.barbassault_playericon_healer_04, Obj.barbassault_playericon_healer_05),
        intArrayOf(Obj.barbassault_heal_horn_01, Obj.barbassault_heal_horn_02, Obj.barbassault_heal_horn_03, Obj.barbassault_heal_horn_04, Obj.barbassault_heal_horn_05),
        Interface.interface_488,
        Component.interface_488.component_488_30,
        intArrayOf(Component.interface_488.component_488_32),
        Component.interface_488.component_488_34,
        Component.interface_488.component_488_36,
        Varbit.barbassult_rolelevel_heal,
        Varbit.varbit_3262,
        1023,
    );

    val partner: Role get() = when (this) {
        ATTACKER -> COLLECTOR
        COLLECTOR -> ATTACKER
        DEFENDER -> HEALER
        HEALER -> DEFENDER
    }

    fun level(player: Player) = player.vars.getVarBit(levelVarbit).coerceIn(1, MAX_ROLE_LEVEL)

    fun horn(player: Player) = horns[(level(player) - 1).coerceAtMost(horns.lastIndex)]

    fun icon(player: Player) = icons[level(player) - 1]

    fun points(player: Player) = player.vars.getVarBit(pointsVarbit)

    fun addPoints(player: Player, amount: Int): Int {
        val before = points(player)
        val after = (before + amount).coerceIn(0, pointsCap)
        player.vars.saveVarBit(pointsVarbit, after)
        return after - before
    }

    companion object {
        val allHorns: Set<Int> = entries.flatMap { it.horns.toList() }.toSet()

        fun forHorn(obj: Int) = entries.firstOrNull { obj in it.horns }

        fun forName(name: String) = entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
    }
}
