package com.rs.game.content.world.areas.global;

import org.darkan.core.cache.ObjectDefinitions;
import world.gregs.voidps.cache.definition.data.ObjectDefinition;
import com.rs.game.World;
import com.rs.game.content.combat.PlayerCombatKt;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;
import com.rs.utils.Ticks;

@PluginEventHandler
public class SpiderWebs {
    public static ObjectClickHandler handleSpiderwebs = new ObjectClickHandler(new Object[] { "Spiderweb", "Web" }, e -> {
        Player player = e.getPlayer();
        GameObject object = e.getObj();
        ObjectDefinition objectDef = object.getDefinitions();

        switch (e.getObj().getDefinitions().getName()) {
            case "Spiderweb" -> {
                if (object.getRotation() == 2) {
                    player.lock(2);
                    if (Utils.getRandomInclusive(1) == 0) {
                        player.addWalkSteps(
                            player.getX(),
                            player.getY() < object.getY() ? object.getY() + 2 : object.getY() - 1,
                            -1,
                            false
                        );
                        player.sendMessage("You squeeze through the web.");
                    } else {
                        player.sendMessage("You fail to squeeze through the web; perhaps you should try again.");
                    }
                }
            }
            case "Web" -> {
                if (objectDef.hasOption(1, "Slash")) {
                    player.anim(
                        PlayerCombatKt.getWeaponAttackEmote(
                            player.getEquipment().getWeaponId(),
                            player.getCombatDefinitions().getAttackStyle()
                        )
                    );
                    slashWeb(player, object);
                }
            }
        }
    });

    private static void slashWeb(Player player, GameObject object) {
        if (Utils.getRandomInclusive(1) == 0) {
            if (World.removeObjectTemporary(object, Ticks.fromMinutes(1)))
                player.sendMessage("You slash through the web!");
        } else
            player.sendMessage("You fail to cut through the web.");
    }
}
