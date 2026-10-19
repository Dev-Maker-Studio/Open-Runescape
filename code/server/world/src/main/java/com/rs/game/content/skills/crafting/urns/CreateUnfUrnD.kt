package com.rs.game.content.skills.crafting.urns

import com.rs.engine.dialogue.Conversation
import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants

class CreateUnfUrnD(player: Player) : Conversation(player) {

    private val categories = listOf(
        Constants.MINING to "Mining Urns",
        Constants.COOKING to "Cooking Urns",
        Constants.FISHING to "Fishing Urns",
        Constants.SMITHING to "Smithing Urns",
        Constants.WOODCUTTING to "Woodcutting Urns",
        Constants.PRAYER to "Prayer Urns",
        Constants.FARMING to "Farming Urns",
        Constants.HUNTER to "Hunter Urns",
        Constants.RUNECRAFTING to "Runecrafting Urns"
    )

    init {
        val urns = categories.map { (skill, _) -> Urn.forSkill(skill) }
        addNext(
            MakeXStatement(urns.map { it.last().started }.toIntArray(), categories.map { it.second }.toTypedArray()),
            *urns.map { MakeUnfOp(player, it) }.toTypedArray()
        )
        create()
    }
}
