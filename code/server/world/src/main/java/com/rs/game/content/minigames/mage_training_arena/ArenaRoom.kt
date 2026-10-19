package com.rs.game.content.minigames.mage_training_arena

import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Varbit

enum class ArenaRoom(val portal: Int, val level: Int, val points: Int, val cap: Int, val overlay: Int, val pointText: Int, val plane: Int, val entranceX: Int, val entranceY: Int) {
    TELEKINETIC(Loc.magictraining_teledoor, 33, Varbit.magictraining_tele_points, 4000, Interface.interface_198, Component.interface_198.component_198_4, 0, 8, 54),
    ALCHEMY(Loc.magictraining_alchemdoor, 21, Varbit.magictraining_alchem_points, 8000, Interface.interface_194, Component.interface_194.component_194_3, 2, 36, 24),
    ENCHANTMENT(Loc.magictraining_enchantdoor, 7, Varbit.magictraining_ench_points, 16000, Interface.interface_195, Component.interface_195.component_195_9, 0, 35, 39),
    GRAVEYARD(Loc.magictraining_gravedoor, 15, Varbit.magictraining_grave_points, 4000, Interface.interface_196, Component.interface_196.component_196_12, 1, 35, 39)
}
