package com.rs.game.content.skills.slayer;

import org.darkan.core.gameval.gen.Npc;
import org.darkan.core.cache.NPCDefinitions;

import java.util.HashMap;
import java.util.Map;

public enum SlayerMonsters {
	GELATINOUS_ABOMINATION(Npc.npc_14849, 1),
	CRAWLING_HAND(Npc.npc_1648, 5),
	CAVE_BUG(Npc.swamp_cave_bug, 7),
	CAVE_CRAWLER(Npc.smki_new_crawler, 10),
	BANSHEE(Npc.slayer_banshee_1, 15),
	CAVE_SLIME(Npc.swamp_cave_slime, 17),
	ROCKSLUG(Npc.npc_1631, 20),
	DESERT_LIZARD(Npc.npc_2804, 22),
	COCKATRICE(Npc.slayer_cockatrice, 25),
	PYREFIED(Npc.slayer_pyrefiend_1, 30),
	MOGRE(Npc.mudskipper_ogre, 32),
	HARPIE_BUG_SWARM(Npc.slayer_harpiebugswarm, 33),
	WALL_BEAST(Npc.swamp_wallbeast_combat, 35),
	KILLERWATT(Npc.slayer_killerwatt, 37),
	MOLANISK(Npc.molanisk, 39),
	TERROR_DOG(Npc.lotr_terror_dog_large, 40),
	BASILISK(Npc.slayer_basilisk, 40),
	FEVER_SPIDER(Npc.deal_fever_spiders1, 42),
	INFERNAL_MAGE(Npc.slayer_infernal_mage_1, 45),
	BRINE_RAT(Npc.olaf2_brine_rats, 47),
	BLOODVELD(Npc.slayer_bloodveld, 50),
	JELLY(Npc.slayer_jelly_1, 52),
	TUROTH(Npc.slayer_turoth_dad, 55),
	WARPED_TERRORBIRD(Npc.pog_mutated_terror_bird1_inv, 56),
	WARPED_TORTOISE(Npc.pog_mutated_tortoise_invulnerable, 56),
	MUTATED_ZYGOMITE(Npc.slayer_mutated_zygomite_adolescent, 57),
	MUTATED_ZYGOMITE2(Npc.slayer_mutated_zygomite_adult, 57),
	CAVE_HORROR(Npc.npc_4353, 58),
	ABERRANT_SPECTRE(Npc.slayer_abberant_spectre_1, 60),
	RUM_PUMPED_CRAB(Npc.acs_rum_crab_1, 61),
	SPIRITUAL_RANGER(Npc.godwars_spiritual_saradomin_ranger, 63),
	DUST_DEVIL(Npc.slayer_dustdevil, 65),
	SPIRITUAL_WARRIOR(Npc.godwars_spiritual_saradomin_warrior, 68),
	KURASK(Npc.slayer_kursk_1, 70),
	SKELETAL_WYVERN(Npc.skeletal_wyvern1, 72),
	JUNGLE_STRYKEWYRM(Npc.strykewyrm, 73),
	GARGOYLE(Npc.slayer_gargoyle_1, 75),
	DESERT_STRYKEWYRM(Npc.strykewyrm_desert, 77),
	AQUANITE(Npc.slayer_aquanite, 78),
	NECHRYAEL(Npc.slayer_nechryael, 80),
	JADINKO_BABY(Npc.jaro_mutated_jadinko_baby, 80),
	GRIFOLAROO(Npc.grifolaroo_1, 82),
	SPIRITUAL_MAGE(Npc.godwars_spiritual_zamorak_mage, 83),
	SPIRITUAL_MAGE2(Npc.godwars_spiritual_armadyl_mage, 83),
	SPIRITUAL_MAGE3(Npc.godwars_spiritual_saradomin_mage, 83),
	SPIRITUAL_MAGE4(Npc.godwars_spiritual_bandos_mage, 83),
	ABYSSAL_DEMON(Npc.slayer_abyssal, 85),
	JADINKO_GUARD(Npc.jaro_mutated_jadinko_guard, 86),
	GRIFOLAPINE(Npc.grifolapine_1, 88),
	DARK_BEAST(Npc.mourning_dark_beast, 90),
	JADINKO_MALE(Npc.jaro_mutated_jadinko_male, 91),
	ICE_STRYKEWYRM(Npc.strykewyrm_ice, 93),
	GANODERMIC_RUNT(Npc.ganodermic_runt_1, 95),
	GANODERMIC_BEAST(Npc.ganodermic_beast_1, 95);

	private static final Map<String, SlayerMonsters> monsters = new HashMap<>();

	public static SlayerMonsters forId(int id) {
		return monsters.get(NPCDefinitions.getDefs(id).getName());
	}

	static {
		for (SlayerMonsters monster : SlayerMonsters.values())
			monsters.put(NPCDefinitions.getDefs(monster.id).getName(), monster);
	}

	private final int id;
	private final int req;

	private SlayerMonsters(int id, int req) {
		this.id = id;
		this.req = req;
	}

	public int getId() {
		return id;
	}

	public int getRequirement() {
		return req;
	}
}
