package com.rs.game.model.entity.npc.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.WorldProjectile;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Logger;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.annotations.ServerStartupEvent.Priority;
import kotlin.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

@PluginEventHandler
public class CombatScriptsHandler {

	private static final HashMap<Object, BiFunction<NPC, Entity, Integer>> MAPPED_SCRIPTS = new HashMap<>();

	static {
		MAPPED_SCRIPTS.put("DEFAULT", (npc, target) -> {
			NPCCombatDefinitions defs = npc.getCombatDefinitions();
			CombatStyle attackStyle = defs.getAttackStyle();
			if (attackStyle == CombatStyle.MELEE)
				CombatScript.delayHit(npc, 0, target, Hit.melee(npc, CombatScript.getMaxHit(npc, npc.getMaxHit(), attackStyle, target)));
			else {
				int damage = CombatScript.getMaxHit(npc, npc.getMaxHit(), attackStyle, target);
				WorldProjectile p = World.sendProjectile(npc, target, defs.getAttackProjectile(), new Pair<>(32, 32), 30, 5, 2);
				CombatScript.delayHit(npc, p.getTaskDelay(), target, attackStyle == CombatStyle.RANGE ? Hit.range(npc, damage) : Hit.magic(npc, damage));
			}
			if (defs.getAttackGfx() != -1)
				npc.setNextSpotAnim(new SpotAnim(defs.getAttackGfx()));
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			npc.soundEffect(target, npc.getCombatDefinitions().getAttackSound(), true);
			return npc.getAttackSpeed();
		});
	}

	@ServerStartupEvent(Priority.FILE_IO)
	public static void loadScripts() {
		List<Class<CombatScript>> classes;
		try {
			classes = Utils.getSubClasses("com.rs", CombatScript.class);
			for (Class<?> c : classes) {
				if (c.isAnonymousClass()) // next
					continue;
				Object o = c.getDeclaredConstructor().newInstance();
				if (!(o instanceof CombatScript script))
					continue;
				for (Object key : script.getKeys()) {
					if (key instanceof Object[] arr) {
						for (Object val : arr)
							MAPPED_SCRIPTS.put(val, script::attack);
					} else
						MAPPED_SCRIPTS.put(key, script::attack);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Logger.info(CombatScriptsHandler.class, "loadScripts", "Loaded combat scripts for " + MAPPED_SCRIPTS.size() + " NPCs...");
	}

	public static int attack(final NPC npc, final Entity target) {
		BiFunction<NPC, Entity, Integer> script = MAPPED_SCRIPTS.get(npc.getId());
		if (script == null) {
			script = MAPPED_SCRIPTS.get(npc.getDefinitions().getName());
			if (script == null)
				script = getDefaultCombat();
		}
		return script.apply(npc, target);
	}

	public static BiFunction<NPC, Entity, Integer> getDefaultCombat() {
		return MAPPED_SCRIPTS.get("DEFAULT");
	}

	public static void addCombatScript(Object npcNameOrId, BiFunction<NPC, Entity, Integer> script) {
		MAPPED_SCRIPTS.put(npcNameOrId, script);
	}

	public static void addCombatScript(Object[] npcNameOrIds, BiFunction<NPC, Entity, Integer> script) {
		for (Object key : npcNameOrIds)
			MAPPED_SCRIPTS.put(key, script);
	}
}
