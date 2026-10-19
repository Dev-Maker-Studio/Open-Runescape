package com.rs.game.content.skills.cooking

import com.rs.engine.dialogue.Conversation
import com.rs.engine.dialogue.statements.MakeXStatement
import com.rs.engine.dialogue.statements.MakeXStatement.MakeXType
import com.rs.game.content.skills.cooking.Cooking.Cookables
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.model.Item

class CookingD(player: Player, cookable: Cookables?, gameObject: GameObject) : Conversation(player) {
    init {
        val locName = gameObject.getDefinitions(player).name.lowercase()
        val fire = locName.contains("fire")
        val refusal = when {
            cookable == null -> if (fire) "You can't cook that on a fire." else if (locName.contains("range")) "You can't cook that on a range." else "You can't cook that."
            cookable.isFireOnly && !fire && !(cookable.isSpitRoastRequired && locName.contains("spit")) -> "You may only cook this on a fire."
            cookable.isRangeOnly && !listOf("range", "stove", "oven", "cooker").any { locName.contains(it) } -> "You need to cook this on a range."
            cookable.isSpitRoastRequired && !locName.contains("spit") -> "You may only cook this on an iron spit."
            player.skills.getLevel(Constants.COOKING) < cookable.level ->
                "You need a Cooking level of ${cookable.level} to cook ${Utils.addArticle(Item(cookable.productItem[0]).definitions.name.lowercase())}."
            else -> null
        }
        if (refusal != null || cookable == null) addSimple(refusal)
        else {
            val makeX = addNext(MakeXStatement(MakeXType.COOK, player.inventory.getAmountOf(cookable.rawItem.id), "Choose how many you wish to cook,<br>then click on the item to begin.", cookable.productItem, null))
            for (option in cookable.productItem.indices)
                makeX.addNext(Runnable { player.actionManager.setAction(Cooking(option, gameObject, cookable, MakeXStatement.getQuantity(player))) })
        }
        create()
    }
}
