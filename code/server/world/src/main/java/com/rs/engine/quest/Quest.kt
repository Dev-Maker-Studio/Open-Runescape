package com.rs.engine.quest

import com.rs.Settings
import com.rs.engine.quest.data.QuestDefinitions
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.Logger
import org.darkan.core.Utils
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.cache.StructDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Graphic
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Param
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varcstr
import org.darkan.core.gameval.gen.Enum as Enums
import org.darkan.core.gameval.gen.Fontmetrics
import org.darkan.core.model.IFEvents
import world.gregs.voidps.cache.definition.data.StructDefinition

enum class Quest(val id: Int, private val slotId: Int) {
    COOKS_ASSISTANT(6, 1),
    DEMON_SLAYER(51, 2),
    DORICS_QUEST(99, 3),
    DRAGON_SLAYER(66, 4),
    ERNEST_CHICKEN(15, 5),
    GOBLIN_DIPLOMACY(137, 6),
    IMP_CATCHER(72, 7),
    KNIGHTS_SWORD(152, 8),
    PIRATES_TREASURE(60, 9),
    PRINCE_ALI_RESCUE(64, 10),
    RESTLESS_GHOST(27, 11),
    PRISONER_OF_GLOUPHRIE(186, 12),
    RUNE_MYSTERIES(55, 13),
    KING_OF_DWARVES(185, 14),
    SHIELD_OF_ARRAV(63, 15),
    VAMPYRE_SLAYER(132, 16),
    GUNNARS_GROUND(179, 17),
    ANIMAL_MAGNETISM(43, 18),
    BETWEEN_A_ROCK(57, 19),
    BIG_CHOMPY_BIRD_HUNTING(102, 20),
    BIOHAZARD(128, 21),
    CABIN_FEVER(0, 22),
    CLOCK_TOWER(44, 23),
    CONTACT(158, 24),
    ZOGRE_FLESH_EATERS(104, 25),
    CREATURE_OF_FENKENSTRAIN(92, 26),
    DARKNESS_OF_HALLOWVALE(26, 27),
    DEATH_TO_DORGESHUUN(13, 28),
    DEATH_PLATEAU(140, 29),
    DESERT_TREASURE(135, 30),
    DEVIOUS_MINDS(123, 31),
    DIG_SITE(69, 32),
    DRUIDIC_RITUAL(111, 33),
    DWARF_CANNON(34, 34),
    EADGARS_RUSE(75, 35),
    EAGLES_PEAK(149, 36),
    ELEMENTAL_WORKSHOP_I(8, 37),
    ELEMENTAL_WORKSHOP_II(94, 38),
    ENAKHRAS_LAMENT(97, 39),
    ENLIGHTENED_JOURNEY(107, 40),
    EYES_OF_GLOUPHRIE(143, 41),
    FAIRY_TALE_I_GROWING_PAINS(73, 42),
    FAIRY_TALE_II_CURE_A_QUEEN(154, 43),
    FAMILY_CREST(116, 44),
    THE_FEUD(136, 45),
    FIGHT_ARENA(106, 46),
    FISHING_CONTEST(76, 47),
    FORGETTABLE_TALE_OF_A_DRUNKEN_DWARF(98, 48),
    FREMENNIK_TRIALS(50, 49),
    WATERFALL_QUEST(93, 50),
    GARDEN_OF_TRANQUILITY(118, 51),
    GERTRUDES_CAT(138, 52),
    GHOSTS_AHOY(82, 53),
    GIANT_DWARF(134, 54),
    THE_GOLEM(3, 55),
    GRAND_TREE(65, 56),
    HAND_IN_SAND(150, 57),
    HAUNTED_MINE(120, 58),
    HAZEEL_CULT(125, 59),
    HEROES_QUEST(38, 60),
    HOLY_GRAIL(112, 61),
    HORROR_FROM_DEEP(105, 62),
    ICTHLARINS_LITTLE_HELPER(141, 63),
    IN_AID_OF_MYREQUE(21, 64),
    IN_SEARCH_OF_MYREQUE(19, 65),
    JUNGLE_POTION(59, 66),
    LEGENDS_QUEST(119, 67),
    LOST_CITY(42, 68),
    LOST_TRIBE(37, 69),
    LUNAR_DIPLOMACY(24, 70),
    MAKING_HISTORY(124, 71),
    MERLINS_CRYSTAL(108, 72),
    MONKEY_MADNESS(130, 73),
    MONKS_FRIEND(147, 74),
    MOUNTAIN_DAUGHTER(151, 75),
    MOURNINGS_ENDS_PART_I(156, 76),
    MOURNINGS_ENDS_PART_II(129, 77),
    MURDER_MYSTERY(28, 78),
    MY_ARMS_BIG_ADVENTURE(22, 79),
    NATURE_SPIRIT(133, 80),
    OBSERVATORY_QUEST(54, 81),
    ONE_SMALL_FAVOUR(146, 82),
    PLAGUE_CITY(58, 83),
    PRIEST_IN_PERIL(9, 84),
    RAG_AND_BONE_MAN(68, 85),
    RAT_CATCHERS(67, 86),
    RECIPE_FOR_DISASTER(30, 87),
    ANOTHER_COOKS_QUEST(1000, 1000),
    FREEING_GOBLIN_GENERALS(1001, 1001),
    FREEING_MOUNTAIN_DWARF(1002, 1002),
    FREEING_EVIL_DAVE(1003, 1003),
    FREEING_LUMBRIDGE_SAGE(1004, 1004),
    FREEING_PIRATE_PETE(1005, 1005),
    FREEING_SKRACH_UGLOGWEE(1006, 1006),
    FREEING_SIR_AMIK_VARZE(1007, 1007),
    FREEING_KING_AWOWOGEI(1008, 1008),
    DEFEATING_THE_CULINAROMANCER(1009, 1009),
    RECRUITMENT_DRIVE(62, 88),
    REGICIDE(100, 89),
    ROVING_ELVES(110, 90),
    ROYAL_TROUBLE(127, 91),
    A_RUM_DEAL(101, 92),
    SCORPION_CATCHER(2, 93),
    SEA_SLUG(121, 94),
    SLUG_MENACE(12, 95),
    SHADES_OF_MORTTON(10, 96),
    SHADOW_OF_STORM(71, 97),
    SHEEP_HERDER(39, 98),
    SHILO_VILLAGE(70, 99),
    A_SOULS_BANE(36, 100),
    SPIRITS_OF_ELID(46, 101),
    SWAN_SONG(48, 102),
    TAI_BWO_WANNAI_TRIO(89, 103),
    A_TAIL_OF_TWO_CATS(45, 104),
    TEARS_OF_GUTHIX(81, 105),
    TEMPLE_OF_IKOV(126, 106),
    THRONE_OF_MISCELLANIA(157, 107),
    TOURIST_TRAP(113, 108),
    WITCHS_HOUSE(7, 109),
    TREE_GNOME_VILLAGE(78, 110),
    TRIBAL_TOTEM(40, 111),
    TROLL_ROMANCE(90, 112),
    TROLL_STRONGHOLD(85, 113),
    UNDERGROUND_PASS(31, 114),
    WANTED(103, 115),
    WATCHTOWER(16, 116),
    COLD_WAR(145, 117),
    FREMENNIK_ISLES(148, 118),
    TOWER_OF_LIFE(1, 119),
    GREAT_BRAIN_ROBBERY(88, 120),
    WHAT_LIES_BELOW(144, 121),
    OLAFS_QUEST(153, 122),
    ANOTHER_SLICE_OF_HAM(155, 123),
    DREAM_MENTOR(79, 124),
    GRIM_TALES(32, 125),
    KINGS_RANSOM(61, 126),
    PATH_OF_GLOUPHRIE(5, 127),
    BACK_TO_MY_ROOTS(49, 128),
    LAND_OF_GOBLINS(91, 129),
    DEALING_WITH_SCABARAS(25, 130),
    WOLF_WHISTLE(52, 131),
    AS_A_FIRST_RESORT(41, 132),
    CATAPULT_CONSTRUCTION(23, 133),
    KENNITHS_CONCERNS(96, 134),
    LEGACY_OF_SEERGAZE(47, 135),
    PERILS_OF_ICE_MOUNTAIN(109, 136),
    TOKTZKETDILL(139, 137),
    SMOKING_KILLS(80, 138),
    ROCKING_OUT(4, 139),
    SPIRIT_OF_SUMMER(14, 140),
    MEETING_HISTORY(142, 141),
    ALL_FIRED_UP(29, 142),
    SUMMERS_END(95, 143),
    DEFENDER_OF_VARROCK(56, 144),
    WHILE_GUTHIX_SLEEPS(86, 145),
    IN_PYRE_NEED(87, 146),
    MYTHS_OF_WHITE_LANDS(74, 148),
    GLORIOUS_MEMORIES(115, 149),
    TALE_OF_MUSPAH(18, 150),
    HUNT_FOR_RED_RAKTUBER(122, 151),
    CHOSEN_COMMANDER(114, 152),
    SWEPT_AWAY(20, 153),
    FUR_N_SEEK(33, 154),
    MISSING_MY_MUMMY(77, 155),
    CURSE_OF_ARRAV(117, 156),
    TEMPLE_AT_SENNTISTEN(168, 157),
    FAIRY_TALE_III_BATTLE_AT_ORKS_RIFT(175, 158),
    BLACK_KNIGHTS_FORTRESS(53, 159),
    FORGIVENESS_OF_A_CHAOS_DWARF(35, 160),
    WITHIN_LIGHT(167, 161),
    NOMADS_REQUIEM(170, 162),
    BLOOD_RUNS_DEEP(169, 163),
    RUNE_MECHANICS(171, 165),
    BUYERS_AND_CELLARS(174, 167),
    LOVE_STORY(11, 168),
    BLOOD_PACT(172, 170),
    QUIET_BEFORE_SWARM(177, 171),
    ELEMENTAL_WORKSHOP_III(176, 172),
    A_VOID_DANCE(83, 173),
    VOID_STARES_BACK(180, 174),
    RITUAL_OF_MAHJARRAT(194, 176),
    CARNILLEAN_RISING(203, 178),
    DO_NO_EVIL(183, 179),
    ELEMENTAL_WORKSHOP_IV(187, 180),
    A_CLOCKWORK_SYRINGE(188, 181),
    DEADLIEST_CATCH(191, 182),
    SALT_IN_WOUND(192, 183),
    BRANCHES_OF_DARKMEYER(193, 184),
    ONE_PIERCING_NOTE(196, 187),
    LET_THEM_EAT_PIE(200, 188),
    ELDER_KILN(201, 190),
    FIREMAKERS_CURSE(199, 191),
    SONG_FROM_DEPTHS(202, 192),
    HALLOWEEN_2010(184, 193);

    var handler: QuestOutline? = null
        private set

    val isImplemented: Boolean get() = handler != null

    val isHoliday: Boolean get() = defs.type == QuestDefinitions.Type.HOLIDAY

    val defs: QuestDefinitions get() = QuestDefinitions.getQuestDefinitions(id)

    val structId: Int get() = EnumDefinitions.getEnum(Enums.enum_2252).getIntValue(slotId)

    val struct: StructDefinition get() = StructDefinitions.getStruct(structId)

    val seasonalBit: Int get() = if (structId == -1) -1 else struct.getIntValue(Param.ql4_seasonal_quest_bit, -1)

    val isSeasonal: Boolean get() = seasonalBit >= 0

    val questPointRewardLine: String
        get() {
            val qp = defs.questpointReward
            return "$qp Quest point${if (qp > 1) "s" else ""}<br>"
        }

    fun meetsReqs(player: Player): Boolean = meetsReqs(player, null)

    fun meetsReqs(player: Player, actionStr: String?): Boolean {
        var meetsRequirements = true
        val info = defs.extraInfo
        for (quest in info.preReqs) {
            if (!player.isQuestComplete(quest)) {
                if (actionStr != null && quest.isImplemented)
                    player.sendMessage("You must have completed " + quest.defs.name + ".")
                meetsRequirements = false
            }
        }
        val skillReqs = HashMap(info.preReqSkillReqs)
        info.skillReq.forEach { (skillId, level) -> skillReqs.merge(skillId, level, ::maxOf) }
        for ((skillId, level) in skillReqs) {
            if (player.skills.getLevelForXp(skillId) < level) {
                if (actionStr != null)
                    player.sendMessage("You need " + Utils.addArticle(Skills.SKILL_NAME[skillId]) + " level of " + level + ".")
                meetsRequirements = false
            }
        }
        if (!meetsRequirements && actionStr != null)
            player.sendMessage("You must meet the requirements for " + defs.name + " " + actionStr)
        return meetsRequirements
    }

    fun openQuestInfo(player: Player, promptStart: Boolean) {
        player.packets.sendVarc(Varc.varc_699, structId)
        player.interfaceManager.sendInterface(Interface.interface_1243)
        if (promptStart) {
            player.packets.setIFHidden(Interface.interface_1243, component(Component.interface_1243.component_1243_45), false)
            player.packets.setIFHidden(Interface.interface_1243, component(Component.interface_1243.component_1243_57), true)
            player.packets.setIFHidden(Interface.interface_1243, component(Component.interface_1243.component_1243_56), true)
            player.packets.setIFEvents(IFEvents(Interface.interface_1243, component(Component.interface_1243.component_1243_46), -1, -1).enableContinueButton())
            player.packets.setIFEvents(IFEvents(Interface.interface_1243, component(Component.interface_1243.component_1243_51), -1, -1).enableContinueButton())
        } else
            player.packets.setIFHidden(Interface.interface_1243, component(Component.interface_1243.component_1243_58), false)
        val reqStr = getRequirementsString(player)
        player.packets.sendVarcString(Varcstr.varcstr_359, reqStr)
        if (isImplemented && defs.extraInfo.usesNewInterface())
            return
        val questState = if (player.isQuestStarted(this)) 1 else 0
        var height = 0

        val startPointDesc = getStartLocationDescription(player)
        player.packets.sendRunScript(Cs2.cs2_4249, Component.interface_1243.component_1243_17, Component.interface_1243.component_1243_18, Component.interface_1243.component_1243_19, -1, -1, questState, height, "<col=ebe076>Start point:</col>", startPointDesc)
        height += Utils.getIFStringHeightAbs(startPointDesc, Fontmetrics.p12_full, 374)

        player.packets.sendRunScript(Cs2.cs2_4249, Component.interface_1243.component_1243_20, Component.interface_1243.component_1243_21, Component.interface_1243.component_1243_22, -1, -1, questState, height, "<col=ebe076>Requirements:</col>", reqStr)
        height += Utils.getIFStringHeightAbs(reqStr, Fontmetrics.p12_full, 357)

        val requiredItemsStr = handler?.requiredItemsString ?: "None."
        player.packets.sendRunScript(Cs2.cs2_4249, Component.interface_1243.component_1243_23, Component.interface_1243.component_1243_24, Component.interface_1243.component_1243_25, Component.interface_1243.component_1243_26, Component.interface_1243.component_1243_31, questState, height, "<col=ebe076>Required items:</col>", requiredItemsStr)
        height += Utils.getIFStringHeightAbs(requiredItemsStr, Fontmetrics.p12_full, 351)

        val combatStuffStr = handler?.getCombatInformationString(player) ?: "None."
        player.packets.sendRunScript(Cs2.cs2_4249, Component.interface_1243.component_1243_32, Component.interface_1243.component_1243_33, Component.interface_1243.component_1243_34, -1, -1, questState, height, "<col=ebe076>Combat:</col>", combatStuffStr)
        height += Utils.getIFStringHeightAbs(combatStuffStr, Fontmetrics.p12_full, 393)

        val rewardsStr = handler?.getRewardsString(player) ?: questPointRewardLine.replace("<br>", "")
        player.packets.sendRunScript(Cs2.cs2_4249, Component.interface_1243.component_1243_35, Component.interface_1243.component_1243_36, Component.interface_1243.component_1243_37, Component.interface_1243.component_1243_38, Component.interface_1243.component_1243_43, questState, height, "<col=ebe076>Rewards:</col>", rewardsStr)

        player.packets.sendRunScript(Cs2.proc_scrollbar_vertical, Component.interface_1243.component_1243_44, Component.interface_1243.component_1243_16, Graphic.aif_scrollbar_dragger_2_3, Graphic.aif_scrollbar_dragger_2_0, Graphic.aif_scrollbar_dragger_2_1, Graphic.aif_scrollbar_dragger_2_2, Graphic.aif_scrollbar_arrow_2_1, Graphic.aif_scrollbar_arrow_2_0)
    }

    private fun getStartLocationDescription(player: Player): String {
        val handler = handler ?: return "This quest is not yet implemented. Unimplemented quests will automatically complete upon all requirements being met for them.<br><br>" +
                "You currently have " + (if (player.isQuestComplete(this)) "<col=00FF00>COMPLETED</col>" else "<col=FF0000>NOT COMPLETED</col>") + " this quest.<br> "
        if (!Settings.getConfig().isDebug) return handler.startLocationDescription
        val start = defs.extraInfo.startLocation
        return handler.startLocationDescription + "<br>[" + start.x + ", " + start.y + ", " + start.plane + "]"
    }

    fun getRequirementsString(player: Player): String {
        val lines = StringBuilder()
        val info = defs.extraInfo
        for (preReq in info.preReqs)
            lines.append(Utils.strikeThroughIf(preReq.defs.extraInfo.name) { player.isQuestComplete(preReq) }).append("<br>")
        for ((skillId, level) in info.skillReq) {
            if (level == 0)
                continue
            lines.append(Utils.strikeThroughIf("$level " + Skills.SKILL_NAME[skillId]) { player.skills.getLevelForXp(skillId) >= level }).append("<br>")
        }
        if (info.qpReq > 0)
            lines.append(info.qpReq).append(" quest points<br>")
        return lines.toString().ifEmpty { "None." }
    }

    companion object {
        private val QUESTS_BY_ID = HashMap<Int, Quest>()
        private val QUESTS_BY_SLOTID = HashMap<Int, Quest>()

        init {
            for (quest in entries) {
                QUESTS_BY_ID[quest.id] = quest
                if (quest.slotId >= 0)
                    QUESTS_BY_SLOTID[quest.slotId] = quest
            }
            initializeHandlers()
        }

        @JvmStatic
        fun initializeHandlers() {
            try {
                for (clazz in Utils.getClassesWithAnnotation("com.rs", QuestHandler::class.java)) {
                    val annotation = clazz.getAnnotation(QuestHandler::class.java)
                    if (annotation == null || clazz.superclass != QuestOutline::class.java)
                        continue
                    annotation.quest.handler = clazz.getConstructor().newInstance() as QuestOutline
                }
            } catch (e: Exception) {
                Logger.handle(Quest::class.java, "initializeHandlers", e)
            }
        }

        @JvmStatic
        fun forId(id: Int): Quest? = QUESTS_BY_ID[id]

        @JvmStatic
        fun forSlot(id: Int): Quest? = QUESTS_BY_SLOTID[id]
    }
}

private fun component(hash: Int) = Utils.componentIdFromHash(hash)
