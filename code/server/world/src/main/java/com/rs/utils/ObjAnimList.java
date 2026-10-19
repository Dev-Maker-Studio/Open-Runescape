package com.rs.utils;

import org.darkan.core.cache.BASDefinitions;
import org.darkan.core.cache.NPCDefinitions;
import org.darkan.core.cache.ObjectDefinitions;
import org.darkan.core.cache.SpotAnimDefinitions;
import org.darkan.core.cache.AnimationDefinitions;
import world.gregs.voidps.cache.definition.data.NPCDefinition;
import world.gregs.voidps.cache.definition.data.AnimationDefinition;
import world.gregs.voidps.cache.definition.data.SpotAnimDefinition;
import world.gregs.voidps.cache.definition.data.ObjectDefinition;
import world.gregs.voidps.cache.definition.data.BASDefinition;
import org.darkan.core.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObjAnimList {

	private static final Set<Integer> USED = new HashSet<>();
	private static final List<Integer> OBJECT_ANIMS = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		//Cache.init();

		init();

		System.out.println(OBJECT_ANIMS.size());
	}

	public static boolean inited() {
		return USED.size() > 0;
	}

	public static boolean isUsed(int anim) {
		return USED.contains(anim);
	}

	public static void init() {
		for (int npcId = 0;npcId < Utils.getNPCDefinitionsSize();npcId++) {
			NPCDefinition npcDef = NPCDefinitions.getDefs(npcId);
			if (npcDef.getRenderEmote() != -1)
				if (BASDefinitions.getDefs(npcDef.getRenderEmote()).getStandAnimation() != -1)
					if (AnimationDefinitions.getDefs(BASDefinitions.getDefs(npcDef.getRenderEmote()).getStandAnimation()).getFrames() != null) {
						int skeleton = AnimationDefinitions.getDefs(BASDefinitions.getDefs(npcDef.getRenderEmote()).getStandAnimation()).getFrames()[0];
						for (int i = 0; i < Utils.getAnimationDefinitionsSize(); i++) {
							AnimationDefinition check = AnimationDefinitions.getDefs(i);
							if ((check == null) || check.getFrames() == null || check.getFrames()[0] == -1)
								continue;
							if (check.getFrames()[0] == skeleton)
								USED.add(i);
						}
					}
		}

		for (int spotAnimId = 0;spotAnimId < Utils.getSpotAnimDefinitionsSize();spotAnimId++) {
			SpotAnimDefinition defs = SpotAnimDefinitions.getDefs(spotAnimId);
			if (defs.getAnimationId() != -1)
				USED.add(defs.getAnimationId());
		}

		for (int objectId = 0;objectId < Utils.getObjectDefinitionsSize();objectId++) {
			ObjectDefinition defs = ObjectDefinitions.getDefs(objectId);
			if (defs.getAnimations() != null)
				for (int animId : defs.getAnimations())
					USED.add(animId);
		}

		for (int i = 0; i < Utils.getAnimationDefinitionsSize(); i++)
			if (!USED.contains(i))
				OBJECT_ANIMS.add(i);
	}

}
