package com.rs.game.model.entity.player.managers

import com.rs.Settings
import com.rs.game.World
import com.rs.game.model.entity.ForceTalk
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.Task
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.EmoteEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.cache.NPCDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Struct
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Animation
import org.darkan.core.model.Rights
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile

private class CapeEmote(val seq: Int, val spotanim: Int, vararg val capes: Int)

private val SKILLCAPE_EMOTES = listOf(
    CapeEmote(Seq.skill_cape_attack, Spotanim.skill_cape_attack_spotanim, Obj.skillcape_attack, Obj.skillcape_attack_trimmed, Obj.obj_25324),
    CapeEmote(Seq.skill_cape_defend, Spotanim.skillcape_defend_spotanim, Obj.skillcape_defence, Obj.skillcape_defence_trimmed, Obj.obj_25326),
    CapeEmote(Seq.skillcapes_human_strength, Spotanim.skillcapes_spot_strength, Obj.skillcape_strength, Obj.skillcape_strength_trimmed, Obj.obj_25325),
    CapeEmote(Seq.skillcapes_human_constitution, Spotanim.skillcapes_spot_constitution, Obj.skillcape_hitpoints, Obj.skillcape_hitpoints_trimmed, Obj.obj_25332),
    CapeEmote(Seq.skillcapes_human_range, Spotanim.skillcapes_spot_range, Obj.obj_9756, Obj.obj_9757, Obj.obj_25327),
    CapeEmote(Seq.skillcapes_player_magic, Spotanim.skillcapes_magic_spotanim_update, Obj.skillcape_magic, Obj.skillcape_magic_trimmed, Obj.obj_25329),
    CapeEmote(Seq.skillcapes_human_prayer, Spotanim.spotanim_829, Obj.skillcape_prayer, Obj.skillcape_prayer_trimmed, Obj.obj_25328),
    CapeEmote(Seq.skillcapes_cooking_player_anim, Spotanim.skillcapes_cooking_spotanim, Obj.skillcape_cooking, Obj.skillcape_cooking_trimmed, Obj.obj_25344),
    CapeEmote(Seq.skillcapes_woodcutting_player_anim, Spotanim.skillcapes_woodcutting_spotanim, Obj.skillcape_woodcutting, Obj.obj_9808, Obj.obj_25346),
    CapeEmote(Seq.skillcapes_player_fletching_bow, Spotanim.skillcapes_fletching_bow_spotanim, Obj.skillcape_fletching, Obj.skillcape_fletching_trimmed, Obj.obj_25337),
    CapeEmote(Seq.skillcapes_fishing_player_anim, Spotanim.skillcapes_fishing_spotanim, Obj.skillcape_fishing, Obj.skillcape_fishing_trimmed, Obj.obj_25343),
    CapeEmote(Seq.skillcapes_human_firemaking, Spotanim.skillcapes_spot_firemaking, Obj.skillcape_firemaking, Obj.skillcape_firemaking_trimmed, Obj.obj_25345),
    CapeEmote(Seq.skillcapes_crafting_player_anim, Spotanim.skillcapes_crafting_spotanim, Obj.skillcape_crafting, Obj.skillcape_crafting_trimmed, Obj.obj_25336),
    CapeEmote(Seq.skillcapes_player_smithing, Spotanim.skillcapes_smithing_spotanim, Obj.skillcape_smithing, Obj.skillcape_smithing_trimmed, Obj.obj_25342),
    CapeEmote(Seq.seq_4941, Spotanim.skillcapes_mining_spotanim, Obj.skillcape_mining, Obj.skillcape_mining_trimmed, Obj.obj_25341),
    CapeEmote(Seq.skillcapes_human_herblore, Spotanim.skillcapes_spot_herblore, Obj.skillcape_herblore, Obj.skillcape_herblore_trimmed, Obj.obj_25334),
    CapeEmote(Seq.skillcapes_human_agility, Spotanim.skillcapes_spot_agility, Obj.skillcape_agility, Obj.skillcape_agility_trimmed, Obj.obj_25333),
    CapeEmote(Seq.skill_cape_thieving, Spotanim.skill_cape_thieving_spotanim, Obj.skillcape_thieving, Obj.skillcape_thieving_trimmed, Obj.obj_25335),
    CapeEmote(Seq.skill_cape_slayer, Spotanim.skillcape_slayer_turoth_spotanim, Obj.skillcape_slayer, Obj.skillcape_slayer_trimmed, Obj.obj_25338),
    CapeEmote(Seq.seq_4963, Spotanim.spotanim_825, Obj.skillcape_farming, Obj.skillcape_farming_trimmed, Obj.obj_25347),
    CapeEmote(Seq.skillcapes_player_runecrafting, Spotanim.skillcapes_runecrafting_spotanim, Obj.obj_9765, Obj.obj_9766, Obj.obj_25330),
    CapeEmote(Seq.skillcapes_construction_player_anim, Spotanim.skillcapes_construction_spotanim, Obj.obj_9789, Obj.obj_9790, Obj.obj_25331),
    CapeEmote(Seq.skill_cape_summoning, Spotanim.skillcapes_summoning_particle_spot, Obj.skillcape_summoning, Obj.skillcape_summoning_trimmed, Obj.obj_25348),
    CapeEmote(Seq.skillcapes_human_hunting, Spotanim.skillcapes_spot_hunting, Obj.skillcape_hunting, Obj.skillcape_hunting_trimmed, Obj.obj_25339),
    CapeEmote(Seq.skillcapes_player_quest_cape, Spotanim.skillcapes_quest_cape_spotanim, Obj.obj_9813),
).flatMap { emote -> emote.capes.map { it to emote } }.toMap()

private val SEAL_FORMS = intArrayOf(Npc.xmas10_seal, Npc.xmas10_seal_02, Npc.xmas10_seal_03)

private val DUNGEONEERING_CAPE_FORMS = listOf(Npc.rand_skillcape_player_melee to Seq.rand_player_skillcape_2nd_part_melee, Npc.rand_skillcape_player_mage to Seq.rand_player_skillcape_2nd_part_mage, Npc.rand_skillcape_player_ranger to Seq.rand_player_skillcape_2nd_part_ranger)

@ServerStartupEvent
fun mapEmotes() {
    onButtonClick(Interface.interface_590, Interface.interface_464) { e ->
        if (e.interfaceId == Interface.interface_590 && e.component != Component.interface_590.component_590_8) return@onButtonClick
        EmotesManager.Emote.forSlot(e.slotId)?.let(e.player.emotesManager::useBookEmote)
    }
}

class EmotesManager {
    private val unlocked = ArrayList<Emote>()

    @Transient
    private lateinit var player: Player

    @Transient
    private var nextEmoteEnd = 0L

    enum class Emote(
        val slotId: Int,
        val mapId: Int,
        val displayName: String,
        @JvmField val varpbit: Int = -1,
        @JvmField val value: Int = if (varpbit == -1) -1 else 1,
        val anim: Animation? = null,
        val spotAnim: SpotAnim? = null,
    ) {
        YES(0, Struct.emotes2_yes, "Yes", anim = Animation(Seq.emote_yes)),
        NO(1, Struct.emotes2_no, "No", anim = Animation(Seq.emote_no)),
        BOW(2, Struct.emotes2_bow, "Bow", anim = Animation(Seq.emote_bow)),
        ANGRY(3, Struct.emotes2_angry, "Angry", anim = Animation(Seq.emote_angry)),
        THINK(4, Struct.emotes2_think, "Think", anim = Animation(Seq.emote_think)),
        WAVE(5, Struct.emotes2_wave, "Wave", anim = Animation(Seq.emote_wave)),
        SHRUG(6, Struct.emotes2_shrug, "Shrug", anim = Animation(Seq.emote_shrug)),
        CHEER(7, Struct.emotes2_cheer, "Cheer", anim = Animation(Seq.emote_cheer)),
        BECKON(8, Struct.emotes2_beckon, "Beckon", anim = Animation(Seq.emote_beckon)),
        LAUGH(9, Struct.emotes2_laugh, "Laugh", anim = Animation(Seq.emote_laugh)),
        JUMP_FOR_JOY(10, Struct.emotes2_jump_with_joy, "Jump For Joy", anim = Animation(Seq.emote_jump_with_joy)),
        YAWN(11, Struct.emotes2_yawn, "Yawn", anim = Animation(Seq.emote_yawn)),
        DANCE(12, Struct.emotes2_dance, "Dance", anim = Animation(Seq.emote_dance)),
        JIG(13, Struct.emotes2_dance_scottish, "Jig", anim = Animation(Seq.emote_dance_scottish)),
        TWIRL(14, Struct.emotes2_dance_spin, "Twirl", anim = Animation(Seq.emote_dance_spin)),
        HEADBANG(15, Struct.emotes2_dance_headbang, "Headbang", anim = Animation(Seq.emote_dance_headbang)),
        CRY(16, Struct.emotes2_cry, "Cry", anim = Animation(Seq.emote_cry)),
        BLOW_KISS(17, Struct.emotes2_blow_kiss, "Blow Kiss", anim = Animation(Seq.emote_blow_kiss), spotAnim = SpotAnim(Spotanim.emote_update_blow_kiss_spot)),
        PANIC(18, Struct.emotes2_panic, "Panic", anim = Animation(Seq.emote_panic)),
        RASPBERRY(19, Struct.emotes2_ya_boo_sucks, "Raspberry", anim = Animation(Seq.emote_ya_boo_sucks)),
        CLAP(20, Struct.emotes2_clap, "Clap", anim = Animation(Seq.emote_clap)),
        SALUTE(21, Struct.emotes2_fremmenik_salute, "Salute", anim = Animation(Seq.emote_fremmenik_salute)),
        GOBLIN_BOW(22, Struct.struct_1830, "Goblin Bow", Varbit.lost_tribe_quest, 7, Animation(Seq.human_cave_goblin_bow)),
        GOBLIN_SALUTE(23, Struct.struct_1831, "Goblin Salute", Varbit.lost_tribe_quest, 7, Animation(Seq.human_cave_goblin_dance)),
        GLASS_BOX(24, Struct.struct_1817, "Glass Box", Varbit.emote_glassbox, anim = Animation(Seq.emote_glass_box)),
        CLIMB_ROPE(25, Struct.struct_1818, "Climb Rope", Varbit.emote_climbrope, anim = Animation(Seq.emote_climbing_rope)),
        LEAN(26, Struct.struct_1819, "Lean", Varbit.emote_lean, anim = Animation(Seq.emote_mime_lean)),
        GLASS_WALL(27, Struct.struct_1806, "Glass Wall", Varbit.emote_glasswall, anim = Animation(Seq.emote_glass_wall)),
        IDEA(28, Struct.struct_1838, "Idea", Varbit.sos_emote_idea, anim = Animation(Seq.emote_lightbulb)),
        STOMP(29, Struct.struct_1839, "Stomp", Varbit.sos_emote_stamp, anim = Animation(Seq.emote_stampfeet)),
        FLAP(30, Struct.struct_1836, "Flap", Varbit.sos_emote_flap, anim = Animation(Seq.emote_panic_flap)),
        SLAP_HEAD(31, Struct.struct_1837, "Slap Head", Varbit.sos_emote_doh, anim = Animation(Seq.emote_slap_head)),
        ZOMBIE_WALK(32, Struct.struct_1834, "Zombie Walk", Varbit.emote_zombie_walk, anim = Animation(Seq.zombie_walk_emote)),
        ZOMBIE_DANCE(33, Struct.struct_1833, "Zombie Dance", Varbit.emote_zombie_dance, anim = Animation(Seq.zombie_dance)),
        ZOMBIE_HAND(34, Struct.struct_1835, "Zombie Hand", Varbit.hw07_prog, 12, Animation(Seq.hw07_arm_from_the_ground_emote), SpotAnim(Spotanim.hw07_arm_from_the_ground_spotanim_2)),
        SCARED(35, Struct.struct_1832, "Scared", Varbit.emote_terrified, anim = Animation(Seq.terrified_emote)),
        BUNNY_HOP(36, Struct.struct_1840, "Bunny-hop", Varbit.emote_bunny_hop, anim = Animation(Seq.rabbit_emote)),
        CAPE(37, Struct.struct_1855, "Cape", Varbit.emote_skillcape),
        SNOWMAN_DANCE(38, Struct.struct_1841, "Snowman Dance", Varbit.emote_xmas07, anim = Animation(Seq.xmas_07_player_emote)),
        AIR_GUITAR(39, Struct.struct_1842, "Air Guitar", Varbit.emote_music, anim = Animation(Seq.music_emote), spotAnim = SpotAnim(Spotanim.music_emote_spot)),
        SAFETY_FIRST(40, Struct.struct_1843, "Safety First", Varbit.emote_sops, anim = Animation(Seq.sops_security_emote_player), spotAnim = SpotAnim(Spotanim.sops_security_emote)),
        EXPLORE(41, Struct.struct_1844, "Explore", Varbit.emote_atlum, anim = Animation(Seq.atlum_player_navigation_emote), spotAnim = SpotAnim(Spotanim.atlum_navigation_emote_spot)),
        TRICK(42, Struct.struct_1845, "Trick", Varbit.swept_hw08_emote, anim = Animation(Seq.swept_player_scare_emote), spotAnim = SpotAnim(Spotanim.swept_player_scare_emote_spot)),
        FREEZE(43, Struct.struct_1846, "Freeze", Varbit.emote_xmas08, anim = Animation(Seq.xmas08_player_freeze), spotAnim = SpotAnim(Spotanim.xmas08_player_freeze_spot)),
        GIVE_THANKS(44, Struct.struct_1847, "Give Thanks", Varbit.emote_tg08),
        AROUND_THE_WORLD(45, Struct.struct_1848, "Around the World in Eggty Days", Varbit.easter11_earned_sunbeam_crown, anim = Animation(Seq.easter09_dash_emote_player), spotAnim = SpotAnim(Spotanim.easter09_dash_emote_player_spot)),
        DRAMATIC_POINT(46, Struct.struct_1849, "Dramatic Point", Varbit.xmas09_emote, anim = Animation(Seq.xmas09_point_at_grave), spotAnim = SpotAnim(Spotanim.xmas09_emote_spotanim)),
        FAINT(47, Struct.struct_1850, "Faint", Varbit.csi_complete_6, anim = Animation(Seq.csi_player_fainting)),
        PUPPET_MASTER(48, Struct.struct_1851, "Puppet Master", Varbit.emote_puppet_master, anim = Animation(Seq.hw10_player_possession_emote), spotAnim = SpotAnim(Spotanim.spotanim_2837)),
        TASKMASTER(49, Struct.struct_1853, "Taskmaster", Varbit.task_completed_total, 534),
        SEAL_OF_APPROVAL(50, Struct.struct_1852, "Seal of Approval", Varbit.xmas10_emote),
        CAT_FIGHT(51, Struct.struct_2658, "Cat Fight", anim = Animation(Seq.emote_loy_catfight)),
        TALK_TO_THE_HAND(52, Struct.struct_2654, "Talk to the Hand", anim = Animation(Seq.emote_loy_talk_to_the_hand)),
        SHAKE_HANDS(53, Struct.struct_2651, "Shake Hands", anim = Animation(Seq.emote_loy_handshake)),
        HIGH_FIVE(54, Struct.struct_2652, "High Five", anim = Animation(Seq.emote_loy_highfive)),
        FACE_PALM(55, Struct.struct_2657, "Face-palm", anim = Animation(Seq.emote_loy_facepalm)),
        SURRENDER(56, Struct.struct_2661, "Surrender", anim = Animation(Seq.emote_loy_surrender_white_flag)),
        LEVITATE(57, Struct.struct_2653, "Levitate", anim = Animation(Seq.emote_loy_levitating)),
        MUSCLE_MAN_POSE(58, Struct.struct_2650, "Muscle-man Pose", anim = Animation(Seq.emote_loy_muscleman)),
        ROFL(59, Struct.struct_2659, "ROFL", anim = Animation(Seq.emote_loy_rofl)),
        BREATHE_FIRE(60, Struct.struct_2660, "Breathe Fire", anim = Animation(Seq.emote_loy_fire_breathing), spotAnim = SpotAnim(Spotanim.emote_loy_fire_breathing_spotanim)),
        STORM(61, Struct.struct_2655, "Storm", anim = Animation(Seq.emote_loy_weather_storm), spotAnim = SpotAnim(Spotanim.spotanim_365)),
        SNOW(62, Struct.struct_2656, "Snow", anim = Animation(Seq.emote_loy_weather_snow), spotAnim = SpotAnim(Spotanim.emote_loy_weather_snow_spotanim)),
        INVOKE_SPRING(63, Struct.struct_1854, "Invoke Spring", Varbit.easter11_earned_sunbeam_crown, anim = Animation(Seq.seq_15357), spotAnim = SpotAnim(Spotanim.easter11_emote_daisies_spotanim)),
        HEAD_IN_THE_SAND(64, Struct.struct_2662, "Head in the Sand", anim = Animation(Seq.seq_12926), spotAnim = SpotAnim(Spotanim.emote_loy2_head_in_sand_spotanim)),
        HULA_HOOP(65, Struct.struct_2663, "Hula Hoop", anim = Animation(Seq.emote_loy2_hula_hoop)),
        DISAPPEAR(66, Struct.struct_2664, "Disappear", anim = Animation(Seq.seq_12929), spotAnim = SpotAnim(Spotanim.emote_loy2_player_disappear_in_smoke_spotanim)),
        GHOST(67, Struct.struct_2665, "Ghost", anim = Animation(Seq.seq_12932), spotAnim = SpotAnim(Spotanim.emote_loy2_player_ghost_emote_spotanim)),
        BRING_IT(68, Struct.struct_2666, "Bring It!", anim = Animation(Seq.seq_12934)),
        PALM_FIST(69, Struct.struct_2667, "Palm-fist", anim = Animation(Seq.seq_12931)),
        KNEEL(70, Struct.clan_emote_stage_kneel, "Kneel", anim = Animation(Seq.clan_emote_down_on_one_knee)),
        BEGGING(71, Struct.clan_emote_stage_begging, "Begging", anim = Animation(Seq.clan_emote_begging)),
        STIR_COULDRON(72, Struct.clan_emote_stage_stir_cauldron, "Stir Cauldron", anim = Animation(Seq.clan_emote_stir_cauldron)),
        CHEER2(73, Struct.clan_emote_stage_cheering, "Cheer", anim = Animation(Seq.clan_emote_cheering)),
        TANTRUM(74, Struct.clan_emote_stage_tantrum, "Tantrum", anim = Animation(Seq.clan_emote_tantrum)),
        DRAMATIC_DEATH(75, Struct.clan_emote_stage_death, "Dramatic Death", anim = Animation(Seq.clan_emote_death)),
        JUMP_YELL(76, Struct.clan_emote_stage_jump_yell, "Jump & Yell", anim = Animation(Seq.clan_emote_jump_yell)),
        POINT(77, Struct.clan_emote_stage_pointing, "Point", anim = Animation(Seq.clan_emote_pointing)),
        PUNCH(78, Struct.clan_emote_stage_punch, "Punch", anim = Animation(Seq.clan_emote_punch)),
        RAISE_HAND(79, Struct.clan_emote_stage_raising_hand, "Raise Hand", anim = Animation(Seq.clan_emote_raising_hand)),
        MAKE_SPEECH(80, Struct.clan_emote_stage_speech, "Make Speech", anim = Animation(Seq.clan_emote_speech)),
        SWORD_FIGHT(81, Struct.clan_emote_stage_sword_fight, "Sword Fight", anim = Animation(Seq.clan_emote_sword_fighting)),
        RAISE_HAND_SIT(82, Struct.clan_emote_sitting_raise_hand, "Raise Hand (Sitting)", anim = Animation(Seq.clan_emote_sitting_raising_hand)),
        WAVE_SIT(83, Struct.clan_emote_sitting_wave, "Wave (Sitting)", anim = Animation(Seq.clan_emote_sitting_wave)),
        CHEER_SIT(84, Struct.clan_emote_sitting_cheering, "Cheer (Sitting)", anim = Animation(Seq.clan_emote_sitting_cheering)),
        THROW_TOMATO_SIT(85, Struct.clan_emote_sitting_throw_tom, "Throw Tomato (Sitting)", anim = Animation(Seq.clan_emote_sitting_fruit_throw)),
        THROW_FLOWERS(86, Struct.clan_emote_sitting_throw_flower, "Throw Flowers (Sitting)", anim = Animation(Seq.clan_emote_sitting_flower_throw)),
        AGREE(87, Struct.clan_emote_sitting_nodding, "Agree (Sitting)", anim = Animation(Seq.clan_emote_sitting_nodding)),
        POINT_SIT(88, Struct.clan_emote_sitting_pointing, "Point (Sitting)", anim = Animation(Seq.clan_emote_sitting_pointing)),
        WHISTLE(89, Struct.clan_emote_sitting_whistle, "Whistle (Sitting)", anim = Animation(Seq.clan_emote_sitting_whistle)),
        THUMBSUP_SIT(90, Struct.struct_2706, "Thumbs-Up (Sitting)", anim = Animation(Seq.clan_emote_sitting_thumbs_up)),
        THUMBSDOWN_SIT(91, Struct.struct_2707, "Thumbs-Down (Sitting)", anim = Animation(Seq.clan_emote_sitting_thumbs_down)),
        CLAP_SIT(92, Struct.clan_emote_sitting_clap, "Clap (Sitting)", anim = Animation(Seq.clan_emote_sitting_clapping)),
        LIVING_BORROWED_TIME(93, Struct.struct_2749, "Living on Borrowed Time", Varbit.hw11_main, 15),
        TROUBADOUR_DANCE(94, Struct.struct_2812, "Troubadour dance", Varbit.opn_quest, 100, Animation(Seq.opn_player_female_dance_male_version)),
        EVIL_LAUGH(95, Struct.struct_2935, "Evil Laugh"),
        GOLF_CLAP(96, Struct.struct_2936, "Golf Clap", anim = Animation(Seq.seq_15520)),
        LOLCANO(97, Struct.struct_2937, "LOLcano"),
        INFERNAL_POWER(98, Struct.struct_2938, "Infernal Power", anim = Animation(Seq.seq_15529), spotAnim = SpotAnim(Spotanim.spotanim_2197)),
        DIVINE_POWER(99, Struct.struct_2939, "Divine Power", anim = Animation(Seq.seq_15524), spotAnim = SpotAnim(Spotanim.spotanim_2195)),
        YOURE_DEAD(100, Struct.struct_2940, "You're Dead", anim = Animation(Seq.seq_14195)),
        SCREAM(101, Struct.struct_2941, "Scream"),
        TORNADO(102, Struct.struct_2942, "Tornado", anim = Animation(Seq.seq_15530), spotAnim = SpotAnim(Spotanim.spotanim_2196)),
        CHAOTIC_COOKERY(103, Struct.struct_2943, "Chaotic Cookery", Varbit.xmas11_emote, anim = Animation(Seq.xmas11_player_chaotic_cookery_emote), spotAnim = SpotAnim(Spotanim.spotanim_2239)),
        ROFLCOPTER(104, Struct.struct_9780, "ROFLcopter"),
        NATURES_MIGHT(105, Struct.struct_9781, "Nature's Might", anim = Animation(Seq.loy4_player_natures_might_emote), spotAnim = SpotAnim(Spotanim.loy4_player_natures_might_emote_spotanim)),
        INNER_POWER(106, Struct.struct_9782, "Inner Power", anim = Animation(Seq.loy4_player_inner_power), spotAnim = SpotAnim(Spotanim.loy4_player_inner_power_spotanim)),
        WEREWOLF_TRANSFORMATION(107, Struct.struct_9783, "Werewolf Transformation"),
        CELEBRATE(108, Struct.emotes2_djub, "Celebrate", anim = Animation(Seq.djub_player_emote_confetti_celebrate), spotAnim = SpotAnim(Spotanim.djub_player_confetti_spotanim)),
        BREAKDANCE(109, Struct.struct_11187, "Breakdance", anim = Animation(Seq.seq_17079)),
        MAHJARRAT_TRANS(110, Struct.struct_11188, "Mahjarrat Transformation", anim = Animation(Seq.seq_17103), spotAnim = SpotAnim(Spotanim.spotanim_3222)),
        BREAK_WIND(111, Struct.struct_11189, "Break Wind", anim = Animation(Seq.seq_17076), spotAnim = SpotAnim(Spotanim.spotanim_3226)),
        BACKFLIP(112, Struct.mtx_batch1_anim_ninja_emote, "Backflip", anim = Animation(Seq.seq_17101), spotAnim = SpotAnim(Spotanim.mtx_player_assassin_flip_emote_spotanim)),
        GRAVEDIGGER(113, Struct.struct_11191, "Gravedigger", anim = Animation(Seq.seq_17077), spotAnim = SpotAnim(Spotanim.spotanim_3219)),
        FROG_TRANS(114, Struct.struct_11192, "Frog Transformation", anim = Animation(Seq.seq_17080), spotAnim = SpotAnim(Spotanim.spotanim_3220)),
        MEXICAN_WAVE(115, Struct.megagames_mexican_wave, "Mexican Wave", anim = Animation(Seq.seq_17163)),
        SPORTSMAN(116, Struct.megagames_sportsman, "Sportsman", anim = Animation(Seq.seq_17166));

        companion object {
            private val BY_SLOT = entries.associateBy { it.slotId }

            @JvmStatic
            fun forSlot(slotId: Int): Emote? = BY_SLOT[slotId]
        }
    }

    fun setPlayer(player: Player) {
        this.player = player
    }

    fun unlockEmote(emote: Emote): Boolean {
        if (emote in unlocked) return false
        unlocked.add(emote)
        refreshListConfigs()
        return true
    }

    fun lockEmote(emote: Emote): Boolean {
        if (!unlocked.remove(emote)) return false
        if (emote.varpbit != -1) player.vars.setVarBit(emote.varpbit, 0)
        refreshListConfigs()
        return true
    }

    fun hasUnlocked(emote: Emote) = emote in unlocked

    fun unlockedEmote(emote: Emote) =
        player.hasRights(Rights.ADMIN) || emote.ordinal <= Emote.SALUTE.ordinal || emote == Emote.CELEBRATE || emote == Emote.CAPE || emote in unlocked

    fun useBookEmote(emote: Emote) {
        if (player.inCombat(10000) || player.hasBeenHit(10000)) {
            player.sendMessage("You can't do this while you're under combat.")
            return
        }
        player.stopAll(false)
        player.treasureTrailsManager.useEmote(emote)
        if (!unlockedEmote(emote)) {
            if (emote == Emote.AIR_GUITAR) player.simpleDialogue("This emote can be acessed by unlocking ${Settings.AIR_GUITAR_MUSICS_COUNT} pieces of music.")
            else player.simpleDialogue("You haven't unlocked ${emote.displayName} yet.")
            return
        }
        if (World.getServerTicks() < nextEmoteEnd) {
            player.sendMessage("You're already doing an emote!")
            return
        }
        PluginManager.handle(EmoteEvent(player, emote))
        val anim = emote.anim
        val male = player.appearance.isMale
        when {
            anim != null -> {
                player.anim(anim)
                if (emote == Emote.AIR_GUITAR) player.jingle(302)
                emote.spotAnim?.let { player.spotAnim(it) }
            }
            emote == Emote.TASKMASTER -> {
                player.anim(if (male) Seq.task_system_player_male_emote else Seq.task_system_player_female_emote)
                player.spotAnim(Spotanim.spotanim_2930)
            }
            emote == Emote.LOLCANO -> {
                player.anim(if (male) Seq.loy3_player_lolcano_emote else Seq.seq_15533)
                player.spotAnim(Spotanim.spotanim_2191)
            }
            emote == Emote.SCREAM -> player.anim(if (male) Seq.seq_15526 else Seq.seq_15527)
            emote == Emote.ROFLCOPTER -> {
                player.anim(if (male) Seq.loy4_player_roflcopter_emote else Seq.loy4_player_roflcopter_emote_female)
                player.spotAnim(Spotanim.spotanim_3010)
            }
            emote == Emote.WEREWOLF_TRANSFORMATION -> {
                player.anim(Seq.loy4_werewolf_emote_player)
                player.spotAnim(Spotanim.spotanim_3013)
                player.spotAnim(Spotanim.spotanim_3016)
            }
            emote == Emote.EVIL_LAUGH -> {
                player.anim(if (male) Seq.loy3_player_evil_laugh_emote else Seq.loy3_player_evil_laugh_emote_female)
                player.spotAnim(-1)
            }
            emote == Emote.LIVING_BORROWED_TIME -> livingOnBorrowedTime()
            emote == Emote.CAPE -> {
                useSkillcapeEmote()
                return
            }
            emote == Emote.GIVE_THANKS -> giveThanks()
            emote == Emote.SEAL_OF_APPROVAL -> sealOfApproval()
        }
        setNextEmoteEnd()
    }

    private fun livingOnBorrowedTime() {
        val grim = NPC(Npc.npc_14388, Tile.of(player.x, player.y + 1, player.plane))
        World.addNPC(grim)
        player.lock()
        grim.setNextFaceEntity(player)
        player.setNextFaceEntity(grim)
        WorldTasks.scheduleTimer(1) { tick ->
            if (tick >= 10 || player.hasFinished()) return@scheduleTimer false
            when (tick) {
                0 -> {
                    grim.anim(Seq.seq_13964)
                    player.spotAnim(Spotanim.spotanim_1766)
                    player.anim(Seq.hw11_emote_player_hourglass_update)
                }
                8 -> {
                    grim.setFinished(true)
                    World.removeNPC(grim)
                    grim.setNextFaceEntity(null)
                }
                9 -> {
                    player.setNextForceTalk(ForceTalk("Phew! Close call."))
                    player.setNextFaceEntity(null)
                    player.unlock()
                }
            }
            true
        }
    }

    private fun giveThanks() {
        WorldTasks.scheduleLooping(object : Task() {
            private var step = 0

            override fun run() {
                when (step) {
                    0 -> {
                        player.anim(Seq.tg08_turkey_emote_dance_player_transmog)
                        player.spotAnim(Spotanim.smokepuff_2)
                    }
                    1 -> {
                        player.anim(Seq.tg08_turkey_dance_emote)
                        player.appearance.transformIntoNPC(Npc.tg08_turkey_dance_emote)
                    }
                    6 -> {
                        player.anim(Seq.tg08_turkey_emote_dance_player_revert)
                        player.spotAnim(Spotanim.smokepuff_2)
                        player.appearance.transformIntoNPC(-1)
                        stop()
                    }
                }
                step++
            }
        }, 0, 1)
    }

    private fun sealOfApproval() {
        WorldTasks.scheduleLooping(object : Task() {
            private val form = SEAL_FORMS[(Math.random() * 3).toInt()]
            private var step = 0

            override fun run() {
                when (step) {
                    0 -> {
                        player.anim(Seq.xmas10_player_transform_to_seal)
                        player.spotAnim(Spotanim.smokepuff_6)
                    }
                    1 -> {
                        player.anim(Seq.xmas10_seal_clapping_emote)
                        player.appearance.transformIntoNPC(form)
                    }
                    2 -> player.anim(Seq.xmas10_seal_transform_to_player)
                    3 -> {
                        player.anim(Seq.xmas10_player_seal_transform_to_player)
                        player.spotAnim(Spotanim.smokepuff_6)
                        player.appearance.transformIntoNPC(-1)
                        stop()
                    }
                }
                step++
            }
        }, 0, 1)
    }

    private fun useSkillcapeEmote() {
        val capeId = player.equipment.capeId
        SKILLCAPE_EMOTES[capeId]?.let {
            player.anim(it.seq)
            player.spotAnim(it.spotanim)
            return
        }
        when (capeId) {
            Obj.skillcape_dungeoneering, Obj.skillcape_dungeoneering_trimmed -> dungeoneeringCape()
            Obj.skillcape_120_dungeoneering, Obj.dummy_skillcape_120_dungeoneering -> masterDungeoneeringCape()
            Obj.skillcape_veteran -> veteranCape(Seq.cod_veteran_skillcape, Spotanim.cod_veteran_skillcape_spotanim)
            Obj.skillcape_veteran_10_year -> veteranCape(Seq._10_year_cape_emote, Spotanim._10_year_cape_emote_spotanim)
            Obj.skillcape_classic -> {
                if (player.controllerManager.controller != null) {
                    player.sendMessage("You cannot do this here!")
                    return
                }
                val random = Utils.getRandomInclusive(2)
                player.anim(Seq.cod_classic_skillcape)
                player.spotAnim(if (random == 0) Spotanim.cod_classic_skillcape_spotanim_3 else Spotanim.cod_classic_skillcape_spotanim_2)
            }
            Obj.skillcape_max -> maxCape()
            Obj.skillcape_completionist, Obj.obj_20771 -> completionistCape(capeId)
            else -> player.sendMessage("You need to be wearing a skillcape in order to perform this emote.")
        }
    }

    private fun veteranCape(seq: Int, spotanim: Int) {
        if (player.controllerManager.controller != null) {
            player.sendMessage("You cannot do this here!")
            return
        }
        player.anim(seq)
        player.spotAnim(spotanim)
    }

    private fun dungeoneeringCape() {
        if (player.isLocked) return
        val form = DUNGEONEERING_CAPE_FORMS.getOrNull(Utils.random(0, 3))
        player.anim(Seq.rand_player_skillcape_1st_part)
        player.spotAnim(Spotanim.rand_player_skillcape_spotanim)
        player.lock()
        WorldTasks.scheduleLooping(object : Task() {
            private var step = 0

            override fun run() {
                if (step == 1 && form != null) {
                    player.appearance.transformIntoNPC(form.first)
                    player.anim(form.second)
                }
                if (step == 6) player.appearance.transformIntoNPC(-1)
                if (step == 8) {
                    player.unlock()
                    stop()
                }
                step++
            }
        }, 0, 0)
    }

    private fun masterDungeoneeringCape() {
        if (player.isLocked) return
        player.setNextFaceTile(Tile.of(player.x, player.y - 1, player.plane))
        player.lock()
        WorldTasks.scheduleLooping(object : Task() {
            private var step = 0

            override fun run() {
                if (step == 1) {
                    player.appearance.transformIntoNPC(Npc.rand_skillcape_player_ranger)
                    player.anim(Seq.vkqc_archer_attack)
                    World.sendProjectile(player, Tile.of(player.x, player.y - 1, player.plane), Spotanim.spotanim_2781, 30 to 30, 6, 5, 1)
                    World.sendSpotAnim(Tile.of(player.x, player.y - 1, player.plane), SpotAnim(Spotanim.rand_120_cape_hobgoblin_south))
                }
                if (step == 3) {
                    player.appearance.transformIntoNPC(Npc.rand_skillcape_player_mage)
                    player.anim(Seq.rand_player_120_cape_2nd_part)
                    player.spotAnim(Spotanim.rand_120_cape_mage_player_spotanim)
                    World.sendSpotAnim(Tile.of(player.x + 1, player.y - 1, player.plane), SpotAnim(Spotanim.rand_120_cape_zombie_south))
                }
                if (step == 5) {
                    player.appearance.transformIntoNPC(Npc.rand_skillcape_player_melee)
                    player.setNextAnimation(Animation(Seq.rand_player_120_cape_3rd_part, 15))
                    World.sendSpotAnim(Tile.of(player.x, player.y - 1, player.plane), SpotAnim(Spotanim.rand_120_cape_eyesquid_south))
                    World.sendSpotAnim(Tile.of(player.x, player.y + 1, player.plane), SpotAnim(Spotanim.rand_120_cape_archmage_south))
                }
                if (step == 9) player.spotAnim(Spotanim.rand_player_skillcape_spotanim)
                if (step == 10) {
                    player.spotAnim(-1)
                    player.appearance.transformIntoNPC(-1)
                    player.unlock()
                    stop()
                }
                step++
            }
        }, 0, 0)
    }

    private fun maxCape() {
        if (player.controllerManager.controller != null) {
            player.sendMessage("Dont annoy other players!")
            return
        }
        val size = NPCDefinitions.getDefs(Npc.skillcape_rock_creature).size
        val spawnTile = Tile.of(player.x + 1, player.y, player.plane).takeIf { World.floorAndWallsFree(it, size) }
            ?: player.getNearestTeleTile(size)
            ?: return player.sendMessage("Need more space to perform this skillcape emote.")
        nextEmoteEnd = World.getServerTicks() + 25
        WorldTasks.scheduleLooping(object : Task() {
            private var step = 0
            private lateinit var npc: NPC

            override fun run() {
                when (step) {
                    0 -> {
                        npc = NPC(Npc.skillcape_rock_creature, spawnTile)
                        npc.anim(Seq.cod_max_skillcape_rock_creature_invisible)
                        npc.spotAnim(Spotanim.cod_max_cape_start_boulder_drop_spotanim)
                        player.anim(Seq.cod_max_cape_start)
                        npc.setNextFaceEntity(player)
                        player.setNextFaceEntity(npc)
                    }
                    2 -> {
                        npc.anim(Seq.cod_max_skillcape_rock_creature_no_face_no_arm)
                        npc.spotAnim(Spotanim.cod_max_cape_woodcutting_boulder_spotanim)
                        player.anim(Seq.cod_max_cape_wood_cutting)
                    }
                    3 -> {
                        npc.spotAnim(Spotanim.cod_max_cape_mining_boulder_spotanim)
                        player.anim(Seq.seq_1181)
                    }
                    4 -> player.anim(Seq.cod_max_cape_fletching_farming)
                    5 -> {
                        npc.anim(Seq.cod_max_skillscape_rock_creature_left_arm_grow)
                        player.anim(Seq.cod_max_cape_smithing_crafting)
                    }
                    6 -> {
                        player.anim(Seq.seq_1251)
                        player.spotAnim(Spotanim.cod_max_cape_summoning_turmoil_spotanim)
                        npc.anim(Seq.seq_1454)
                        npc.spotAnim(Spotanim.spotanim_1504)
                    }
                    11 -> {
                        player.anim(Seq.cod_max_cape_agility)
                        player.spotAnim(Spotanim.spotanim_1686)
                        player.spotAnim(Spotanim.cod_max_cape_chinchompa_explosion_spotanim)
                        npc.anim(Seq.cod_max_skillscape_rock_creature_attack)
                    }
                    16 -> {
                        player.setNextFaceEntity(null)
                        npc.finish()
                        stop()
                    }
                }
                step++
            }
        }, 0, 1)
    }

    private fun completionistCape(capeId: Int) {
        if (!World.floorAndWallsFree(player.tile, 3)) {
            player.sendMessage("Need more space to perform this skillcape emote.")
            return
        }
        if (player.controllerManager.controller != null) {
            player.sendMessage("Dont annoy other players!")
            return
        }
        nextEmoteEnd = World.getServerTicks() + 20
        WorldTasks.scheduleLooping(object : Task() {
            private var step = 0

            override fun run() {
                when (step) {
                    0 -> {
                        player.anim(Seq.seq_356)
                        player.spotAnim(Spotanim.cod_completionist_cape_human_spotanim)
                    }
                    2 -> {
                        player.appearance.transformIntoNPC(if (capeId == Obj.skillcape_completionist) Npc.skillcape_obsidian_dragon else Npc.skillcape_gold_dragon)
                        player.anim(Seq.seq_1174)
                        player.spotAnim(Spotanim.cod_completionist_cape_dragon_spotanim)
                    }
                    4 -> player.packets.sendCameraShake(3, 25, 50, 25, 50)
                    5 -> player.packets.sendStopCameraShake()
                    8 -> {
                        player.appearance.transformIntoNPC(-1)
                        player.anim(Seq.cod_completionist_skillcape_end)
                        stop()
                    }
                }
                step++
            }
        }, 0, 1)
    }

    fun setNextEmoteEnd() {
        nextEmoteEnd = player.lastAnimationEnd - 1
    }

    fun setNextEmoteEnd(ticks: Int) {
        nextEmoteEnd = World.getServerTicks() + ticks
    }

    fun refreshListConfigs() {
        for (emote in unlocked)
            if (emote.varpbit != -1) player.vars.setVarBit(emote.varpbit, emote.value)
    }

    val isAnimating: Boolean
        get() = World.getServerTicks() < nextEmoteEnd

    fun unlockEmotesBook() {
        player.packets.setIFRightClickOps(Interface.interface_590, Utils.componentIdFromHash(Component.interface_590.component_590_8), 0, Emote.entries.size, 0, 1)
    }
}
