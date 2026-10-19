package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.CombatScriptsHandler;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import com.rs.utils.Ticks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rs.game.content.skills.dungeoneering.DungeonConstants.GuardianMonster.NECROMANCER;
import static com.rs.game.content.skills.dungeoneering.DungeonConstants.GuardianMonster.REBORN_MAGE;

public class AntiSilkHoodMages extends CombatScript {//default combat script

	@Override
	public Object[] getKeys() {
		return Stream.concat(Arrays.stream(NECROMANCER.getNPCIds()).boxed(), Arrays.stream(REBORN_MAGE.getNPCIds()).boxed()).toArray(Object[]::new);
	}

	@Override
	public int attack(NPC npc, Entity target) {
		if(target instanceof Player player && player.getEquipment().containsOneItem(17279, 15828)
				&& !player.getTempAttribs().getB("ShadowSilkSpellDisable")){
			sendAntiSilkHoodSpell(npc, player);
			return 5;//delay 5 ticks for spell
		}
		return CombatScriptsHandler.getDefaultCombat().apply(npc, target);
	}
	private void sendAntiSilkHoodSpell(NPC npc, final Player player) {
		int animation = 6293;
		if(Arrays.stream(REBORN_MAGE.getNPCIds()).anyMatch(i -> i == npc.getId()))
			animation = 11130;
		npc.sync(animation, 1059);
		WorldTasks.scheduleTimer(2, (ticks) -> {
			player.setNextSpotAnim(new SpotAnim(736, 0, 50));
			player.getTempAttribs().setB("ShadowSilkSpellDisable", true);
			player.sendMessage("<col=ff6f69>Your shadow silk hood loses its power...");
			WorldTasks.delay(Ticks.fromMinutes(2), () -> {
				if(player.hasStarted()) {
					if(player.getTempAttribs().getB("ShadowSilkSpellDisable"))
						player.sendMessage("<col=96ceb4>Your shadow silk hood returns its power...");
					player.getTempAttribs().setB("ShadowSilkSpellDisable", false);
				}
			});
			return false;
		});
	}
}
