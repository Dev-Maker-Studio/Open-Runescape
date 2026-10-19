package com.rs.game.content.bosses.corp

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.gameobject.GameObject
import world.gregs.voidps.type.Tile

class CorporealBeastController : Controller() {
    override fun start() {
    }

    override fun processObjectClick1(obj: GameObject): Boolean {
        if (obj.id == 37929 || obj.id == 38811) {
            removeController()
            player.stopAll()
            player.tele(Tile.of(2970, 4384, player.plane))
            return false
        }
        return true
    }

    override fun process() {
        if (player.getRegionId() != 11844) removeController()
    }

    override fun onTeleported(type: TeleType?) {
        removeController()
    }

    override fun sendDeath(): Boolean {
        player.dangerousDeath {
            player.sendPVEItemsOnDeath(null, false)
        }
        return false
    }

    override fun login(): Boolean {
        return false // so doesnt remove script
    }

    override fun logout(): Boolean {
        return false // so doesnt remove script
    }
}
