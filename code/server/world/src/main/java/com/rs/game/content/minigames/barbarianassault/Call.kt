package com.rs.game.content.minigames.barbarianassault

import org.darkan.core.gameval.gen.Obj

enum class Call(val role: Role, val hornOption: String, val shout: String, val received: List<String>, val toCall: String) {
    CONTROLLED(Role.ATTACKER, "Tell-style1", "Controlled/Bronze/Wind", listOf("Controlled/", "Bronze/Wind"), "Controlled"),
    ACCURATE(Role.ATTACKER, "Tell-style2", "Accurate/Iron/Water", listOf("Accurate/", "Iron/Water"), "Accurate"),
    AGGRESSIVE(Role.ATTACKER, "Tell-style3", "Aggressive/Steel/Earth", listOf("Aggressive/", "Steel/Earth"), "Aggressive"),
    DEFENSIVE(Role.ATTACKER, "Tell-style4", "Defensive/Mithril/Fire", listOf("Defensive/", "Mithril/Fire"), "Defensive"),
    RED_EGGS(Role.COLLECTOR, "Tell-red", "Red egg", listOf("Red eggs"), "Red eggs"),
    GREEN_EGGS(Role.COLLECTOR, "Tell-green", "Green egg", listOf("Green eggs"), "Green eggs"),
    BLUE_EGGS(Role.COLLECTOR, "Tell-blue", "Blue egg", listOf("Blue eggs"), "Blue eggs"),
    TOFU_BAIT(Role.DEFENDER, "Tell-tofu", "Tofu", listOf("Tofu"), "Tofu"),
    CRACKERS_BAIT(Role.DEFENDER, "Tell-crackers", "Crackers", listOf("Crackers"), "Crackers"),
    WORMS_BAIT(Role.DEFENDER, "Tell-worms", "Worms", listOf("Worms"), "Worms"),
    TOFU_POISON(Role.HEALER, "Tell-tofu", "Pois. Tofu", listOf("Poison tofu"), "Poison tofu"),
    WORMS_POISON(Role.HEALER, "Tell-worms", "Pois. Worms", listOf("Poison worms"), "Poison worms"),
    MEAT_POISON(Role.HEALER, "Tell-meat", "Pois. Meat", listOf("Poison meat"), "Poison meat");

    val eggColour: EggColour? get() = when (this) {
        RED_EGGS -> EggColour.RED
        GREEN_EGGS -> EggColour.GREEN
        BLUE_EGGS -> EggColour.BLUE
        else -> null
    }

    val bait: Int get() = when (this) {
        TOFU_BAIT -> Obj.barbassault_runner_food2
        CRACKERS_BAIT -> Obj.barbassault_runner_food
        WORMS_BAIT -> Obj.barbassault_runner_food3
        else -> -1
    }

    val poison: Int get() = when (this) {
        TOFU_POISON -> Obj.barbassault_poision_01
        WORMS_POISON -> Obj.barbassault_poision_02
        MEAT_POISON -> Obj.barbassault_poision_03
        else -> -1
    }

    companion object {
        fun forRole(role: Role) = entries.filter { it.role == role }

        fun forHorn(caller: Role, option: String) = entries.firstOrNull { it.role == caller.partner && it.hornOption.equals(option, ignoreCase = true) }
    }
}

enum class EggColour(val obj: Int, val displayName: String) {
    GREEN(Obj.barbassault_egg_01, "green"),
    RED(Obj.barbassault_egg_02, "red"),
    BLUE(Obj.barbassault_egg_03, "blue");

    companion object {
        fun forObj(obj: Int) = entries.firstOrNull { it.obj == obj }
    }
}
