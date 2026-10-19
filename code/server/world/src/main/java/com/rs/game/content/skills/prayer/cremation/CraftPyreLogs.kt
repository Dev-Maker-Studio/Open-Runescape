package com.rs.game.content.skills.prayer.cremation

import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.shadesofmortton.STAGE_CREMATE_SHADE
import com.rs.game.content.quests.shadesofmortton.morttonState
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj

private val sacredOilDoses = listOf(Obj.vial_empty, Obj.sacred_oil1, Obj.sacred_oil2, Obj.sacred_oil3, Obj.sacred_oil4)

class CraftPyreLogs(private val log: PyreLog) : PlayerAction() {
    private var makeX = 0

    override fun start(player: Player): Boolean {
        makeX = MakeXStatement.getQuantity(player)
        return player.skills.getLevel(Constants.FIREMAKING) >= log.level
    }

    override fun process(player: Player): Boolean = player.skills.getLevel(Constants.FIREMAKING) >= log.level

    override fun processWithDelay(player: Player): Int {
        if (!player.inventory.containsItem(log.baseLog, 1) || makeX <= 0) return -1
        val oils = player.inventory.items.array().filterNotNull().filter { it.id in sacredOilDoses.drop(1) }
        if (oils.sumOf { sacredOilDoses.indexOf(it.id) } < log.oilDoses) return -1
        makeX--
        player.inventory.deleteItem(log.baseLog, 1)
        var needed = log.oilDoses
        for (oil in oils) {
            if (needed <= 0) break
            val doses = sacredOilDoses.indexOf(oil.id)
            val consumed = minOf(doses, needed)
            oil.id = sacredOilDoses[doses - consumed]
            needed -= consumed
        }
        player.inventory.refresh()
        player.inventory.addItem(log.itemId, 1)
        player.skills.addXp(Constants.FIREMAKING, log.creationXP.toDouble())
        player.sendMessage("You use the sacred oil on the logs and get pyre logs.")
        if (player.getQuestStage(Quest.SHADES_OF_MORTTON) == STAGE_CREMATE_SHADE)
            player.morttonState().setB("pyreLogsMade", true)
        return 0
    }

    override fun stop(player: Player) {}
}
