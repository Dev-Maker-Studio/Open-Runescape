package com.rs.game.content.skills.crafting

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import org.darkan.core.Constants
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

class GemTipCutting(private val gem: GemTips, private var quantity: Int) : PlayerAction() {

    enum class GemTips(
        val cut: Int,
        val experience: Double,
        val levelRequired: Int,
        val emote: Int,
        val boltTips: Int,
        val tipsPerGem: Int = 12
    ) {
        OPAL(Obj.opal, 1.5, 11, Seq.human_diamondcutting, Obj.opal_bolttips),
        PEARL(Obj.smalloysterpearls, 3.2, 41, Seq.human_diamondcutting, Obj.pearl_bolttips, 6),
        PEARLS(Obj.bigoysterpearls, 3.2, 41, Seq.human_diamondcutting, Obj.pearl_bolttips, 24),
        JADE(Obj.jade, 2.0, 26, Seq.human_diamondcutting, Obj.xbows_bolt_tips_jade),
        RED_TOPAZ(Obj.red_topaz, 3.0, 48, Seq.human_rubycutting, Obj.xbows_bolt_tips_redtopaz),
        SAPPHIRE(Obj.sapphire, 4.0, 56, Seq.human_sapphirecutting, Obj.xbows_bolt_tips_sapphire),
        EMERALD(Obj.emerald, 5.5, 58, Seq.human_emeraldcutting, Obj.xbows_bolt_tips_emerald),
        RUBY(Obj.ruby, 6.5, 63, Seq.human_rubycutting, Obj.xbows_bolt_tips_ruby),
        DIAMOND(Obj.diamond, 8.0, 65, Seq.human_opalcutting, Obj.xbows_bolt_tips_diamond),
        DRAGONSTONE(Obj.dragonstone, 10.0, 71, Seq.human_dragonstonecutting, Obj.obj_9193),
        ONYX(Obj.onyx, 25.0, 73, Seq.human_onyxcutting, Obj.xbows_bolt_tips_onyx)
    }

    private fun checkAll(player: Player): Boolean {
        if (player.skills.getLevel(Constants.FLETCHING) < gem.levelRequired) {
            player.simpleDialogue("You need a fletching level of ${gem.levelRequired} to cut that gem.")
            return false
        }
        if (!player.inventory.containsOneItem(gem.cut)) {
            player.simpleDialogue("You don't have any ${gemName()} to cut.")
            return false
        }
        return true
    }

    override fun start(player: Player): Boolean {
        if (!checkAll(player)) return false
        setActionDelay(player, 2)
        player.anim(gem.emote)
        return true
    }

    override fun process(player: Player): Boolean = checkAll(player)

    override fun processWithDelay(player: Player): Int {
        player.inventory.deleteItem(gem.cut, 1)
        player.inventory.addItem(gem.boltTips, gem.tipsPerGem)
        player.skills.addXp(Constants.FLETCHING, gem.experience)
        player.sendMessage("You cut the ${gemName()}.", true)
        quantity--
        if (quantity <= 0) return -1
        player.anim(gem.emote)
        return 2
    }

    override fun stop(player: Player) {
        setActionDelay(player, 3)
    }

    private fun gemName(): String = ItemDefinitions.getDefs(gem.cut).name.lowercase()

    companion object {
        @JvmStatic
        fun cut(player: Player, gem: GemTips) {
            player.actionManager.setAction(GemTipCutting(gem, player.inventory.getNumberOf(gem.cut)))
        }
    }
}
