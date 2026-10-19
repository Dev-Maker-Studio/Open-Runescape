package com.rs.game.content.skills.dungeoneering

import com.rs.game.content.skills.dungeoneering.rooms.BossRoom
import com.rs.game.content.skills.dungeoneering.rooms.HandledPuzzleRoom
import com.rs.game.content.skills.dungeoneering.rooms.HandledRoom
import com.rs.game.content.skills.dungeoneering.rooms.NormalRoom
import com.rs.game.content.skills.dungeoneering.rooms.StartRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.CrystalPuzzleRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.FishingFerretRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.FlipTilesRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.FremennikCampRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.LeverRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.PoltergeistRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.ReturnTheFlowRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.SlidingStatuesRoom
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.ToxinMaze
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.UnhappyGhostRoom
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

object DungeonConstants {

    @JvmField val MIN_CRIT_PATH = intArrayOf(5, 10, 19)
    @JvmField val MAX_CRIT_PATH = intArrayOf(7, 13, 23)

    @JvmField val OUTSIDE: Tile = Tile.of(3460, 3720, 1)
    const val ROTATIONS_COUNT = 4
    const val TINDERBOX = 17678
    const val SMUGGLER = 11226
    @JvmField val LADDERS = intArrayOf(49696, 49698, 49700, 53748, 55484)
    const val FLOORS_COUNT = 60
    const val GROUP_GATESTONE = 18829
    const val GATESTONE = 17489

    @JvmField val DUNGEON_DOORS = intArrayOf(50342, 50343, 50344, 53948, 55762)
    @JvmField val DUNGEON_BOSS_DOORS = intArrayOf(50350, 50351, 50352, 53950, 55764)

    @JvmField val DUNGEON_GUARDIAN_DOORS = intArrayOf(50346, 50347, 50348, 53949, 55763)

    @JvmField val MINING_RESOURCES = intArrayOf(49806, 49786, 49766, 53771, 55514)

    @JvmField val WOODCUTTING_RESOURCES = intArrayOf(49745, 49725, 49705, 53751, 55494)

    @JvmField val FARMING_RESOURCES = intArrayOf(49866, 49846, 49826, 53791, 55534)

    @JvmField val THIEF_CHEST_LOCKED = intArrayOf(49345, 49346, 49347, 54407, 32462)
    @JvmField val THIEF_CHEST_OPEN = intArrayOf(49348, 49349, 49350, 54408, 32679)
    @JvmField val THIEF_CHEST_XP = doubleArrayOf(25.5, 57.0, 115.0, 209.0, 331.5, 485.0, 661.5, 876.0, 1118.0, 1410.5)

    const val RUSTY_COINS = 18201
    const val BANANA = 18199

    @JvmField val CHARMS = intArrayOf(18017, 18018, 18019, 18020)

    @JvmField val ARROWS = intArrayOf(16427, 16432, 16437, 16442, 16447, 16452, 16457, 16462, 16467, 16472, 16477, 16482, 16487, 16492, 16497, 16502, 16507, 16512, 16517, 16522, 16527, 16532, 16537, 16542, 16547, 16552, 16557, 16562, 16567, 16572, 16577, 16582, 16587, 16592, 16597, 16602, 16607, 16612, 16617, 16622, 16627, 16632, 16637, 16642)

    @JvmField val RUNES = intArrayOf(17780, 17781, 17782, 17783, 17784, 17785, 17786, 17787, 17788, 17789, 17790, 17791, 17792, 17793)

    const val RUNE_ESSENCE = 17776
    const val FEATHER = 17796

    @JvmField val DUNGEON_KEY_DOORS = intArrayOf(50353, 50417, 50481, 53884, 55675)

    enum class SkillDoors(
        val skillId: Int,
        private val closedThemeObjects: IntArray,
        private val openThemeObjects: IntArray?,
        val openAnim: Int,
        val openGfx: Int,
        val openObjectAnim: Int,
        val failObjectAnim: Int,
        val failAnim: Int,
        val failGfx: Int,
        val failMessage: String,
    ) {
        RUNED_DOOR(Constants.RUNECRAFTING, intArrayOf(50278, 50279, 50280, 53953, 55741), 791, 186, -1, -1, -1, -1, "You imbue the door with the wrong type of rune energy, and it reacts explosively."),
        BARRED_DOOR(Constants.STRENGTH, intArrayOf(50272, 50273, 50274, 53951, 55739), intArrayOf(50275, 50276, 50277, 53952, 55740), -1, -1, -1, -1, -1, -1, "You pull a muscle while attempting to move the plank."),
        PILE_OF_ROCKS(Constants.MINING, intArrayOf(50305, 50306, 50307, 53962, 55750), -2, -1, 13559, -1, -1, 2420, "You fail to mine the obstruction, and are harmed by falling debris."),
        FLAMMABLE_DEBRIS(Constants.FIREMAKING, intArrayOf(50314, 50315, 50316, 53965, 55753), 16700, -1, 13562, 13563, -1, -1, "The pile of debris fails to ignite. The same cannot be said for your clothes."),
        MAGICAL_BARRIER(Constants.MAGIC, intArrayOf(50329, 50330, 50331, 53970, 55758), 4411, -1, 13551, 13550, -1, -1, "You fail to dispel the barrier and take a surge of magical energy to the face."),
        DARK_SPIRIT(Constants.PRAYER, intArrayOf(50332, 50333, 50334, 53971, 55759), 645, -1, 13557, 13556, -1, -1, "The gods snub your prayer, and the dark spirit attacks you."),
        WOODEN_BARRICADE(Constants.WOODCUTTING, intArrayOf(50317, 50318, 50319, 53966, 55754), intArrayOf(50320, 50321, 50322, 53967, 55755), -3, -1, 13582, -1, -1, -1, "You swing the axe against the grain and are showered with sharp splinters of wood."),
        BROKEN_KEY_DOOR(Constants.SMITHING, intArrayOf(50308, 50309, 50310, 53963, 55751), intArrayOf(50311, 50312, 50313, 53964, 55752), 13759, -1, -1, -1, -1, -1, "You hit your hand with the hammer. Needless to say, the key is still broken."),
        BROKEN_PULLEY_DOOR(Constants.CRAFTING, intArrayOf(50299, 50300, 50301, 53960, 55748), intArrayOf(50302, 50303, 50304, 53961, 55749), 13547, -1, -1, -1, -1, -1, "The rope snaps again as you attempt to fix it, and you crush your hands in the mechanism."),
        LOCKED_DOOR(Constants.AGILITY, intArrayOf(-1, 50288, 50289, 53956, 55744), intArrayOf(50290, 50291, 50292, 53957, 55745), 14550, -1, -1, -1, -1, -1, "You miss the chain, and set off the trap."),
        PADLOCKED_DOOR(Constants.THIEVING, intArrayOf(-1, 50294, 50295, 53958, 55746), intArrayOf(50296, 50297, 50298, 53959, 55747), 14568, -1, -1, -1, 14569, -1, "You set off a booby trap inside the lock, and fail to pick it."),
        RAMOKEE_EXILE(Constants.SUMMONING, intArrayOf(-1, 50327, 50328, 53969, 55757), 725, 1207, -1, -1, -1, -1, "You fail to dismiss the rogue familiar, and it punches you in anger."),
        LIQUID_LOCK_DOOR(Constants.HERBLORE, intArrayOf(-1, 50336, 50337, 53972, 55760), intArrayOf(50338, 50339, 50340, 53973, 55761), 14568, -1, -1, -1, -1, -1, "You incorrectly mix the ingredients, making it explode."),
        VINE_COVERED_DOOR(Constants.FARMING, intArrayOf(-1, 50324, 50325, 53968, 55756), 2275, -1, 13572, -1, -1, -1, "You hurt your hands on the vicious thorns covering the vines."),
        COLLAPSING_DOORFRAME(Constants.CONSTRUCTION, intArrayOf(-1, 50282, 50283, 53954, 55742), intArrayOf(50284, 50285, 50286, 53955, 55743), 14566, -1, -1, -1, 14567, -1, "You dislodge some debris while attempting to fix the door, and it falls on you.");

        constructor(skillId: Int, closedThemeObjects: IntArray, openAnim: Int, openGfx: Int, openObjectAnim: Int, failObjectAnim: Int, failAnim: Int, failGfx: Int, failMessage: String) :
            this(skillId, closedThemeObjects, null, openAnim, openGfx, openObjectAnim, failObjectAnim, failAnim, failGfx, failMessage)

        fun getClosedObject(type: FloorType): Int = closedThemeObjects[type.ordinal]

        fun getOpenObject(type: FloorType): Int = openThemeObjects?.get(type.ordinal) ?: -1
    }

    enum class KeyDoors(val index: Int) {
        ORANGE_TRIANGLE(0),
        ORANGE_DIAMOND(1),
        ORANGE_RECTANGLE(2),
        ORANGE_PENTAGON(3),
        ORANGE_CORNER(4),
        ORANGE_CRESCENT(5),
        ORANGE_WEDGE(6),
        ORANGE_SHIELD(7),
        SILVER_TRIANGLE(8),
        SILVER_DIAMOND(9),
        SILVER_RECTANGLE(10),
        SILVER_PENTAGON(11),
        SILVER_CORNER(12),
        SILVER_CRESCENT(13),
        SILVER_WEDGE(14),
        SILVER_SHIELD(15),
        YELLOW_TRIANGLE(16),
        YELLOW_DIAMOND(17),
        YELLOW_RECTANGLE(18),
        YELLOW_PENTAGON(19),
        YELLOW_CORNER(20),
        YELLOW_CRESCENT(21),
        YELLOW_WEDGE(22),
        YELLOW_SHIELD(23),
        GREEN_TRIANGLE(24),
        GREEN_DIAMOND(25),
        GREEN_RECTANGLE(26),
        GREEN_PENTAGON(27),
        GREEN_CORNER(28),
        GREEN_CRESCENT(29),
        GREEN_WEDGE(30),
        GREEN_SHIELD(31),
        BLUE_TRIANGLE(32),
        BLUE_DIAMOND(33),
        BLUE_RECTANGLE(34),
        BLUE_PENTAGON(35),
        BLUE_CORNER(36),
        BLUE_CRESCENT(37),
        BLUE_WEDGE(38),
        BLUE_SHIELD(39),
        PURPLE_TRIANGLE(40),
        PURPLE_DIAMOND(41),
        PURPLE_RECTANGLE(42),
        PURPLE_PENTAGON(43),
        PURPLE_CORNER(44),
        PURPLE_CRESCENT(45),
        PURPLE_WEDGE(46),
        PURPLE_SHIELD(47),
        CRIMSON_TRIANGLE(48),
        CRIMSON_DIAMOND(49),
        CRIMSON_RECTANGLE(50),
        CRIMSON_PENTAGON(51),
        CRIMSON_CORNER(52),
        CRIMSON_CRESCENT(53),
        CRIMSON_WEDGE(54),
        CRIMSON_SHIELD(55),
        GOLD_TRIANGLE(56),
        GOLD_DIAMOND(57),
        GOLD_RECTANGLE(58),
        GOLD_PENTAGON(59),
        GOLD_CORNER(60),
        GOLD_CRESCENT(61),
        GOLD_WEDGE(62),
        GOLD_SHIELD(63);

        val objectId: Int get() = getLowestObjectId() + index

        val keyId: Int get() = getLowestKeyId() + index * 2

        fun getDoorId(floorType: FloorType): Int = getLowestDoorId(floorType) + index

        companion object {
            private const val LOWEST_OBJECT_ID = 50208
            private const val LOWEST_KEY_ID = 18202

            @JvmStatic fun getLowestObjectId(): Int = LOWEST_OBJECT_ID

            @JvmStatic fun getLowestDoorId(floorType: FloorType): Int = DUNGEON_KEY_DOORS[floorType.ordinal]

            @JvmStatic fun getMaxObjectId(): Int = LOWEST_OBJECT_ID + entries.size

            @JvmStatic fun getMaxDoorId(floorType: FloorType): Int = DUNGEON_KEY_DOORS[floorType.ordinal] + entries.size

            @JvmStatic fun getLowestKeyId(): Int = LOWEST_KEY_ID
        }
    }

    enum class DoorType { NORMAL, GUARDIAN, SKILL, CHALLENGE, KEY }

    const val UNBALANCED_PARTY_PENALTY = 0.4
    @JvmField val DUNGEON_SIZE_BONUS = doubleArrayOf(0.0, 0.0792, 0.1583)
    @JvmField val COMPLEXITY_PENALTY_BASE = doubleArrayOf(0.32, 0.35, 0.37)
    const val MAX_BONUS_ROOM = 0.1267
    @JvmField val DUNGEON_DIFFICULTY_RATIO_BONUS = arrayOf(
        doubleArrayOf(0.0),
        doubleArrayOf(-0.076, 0.0507),
        doubleArrayOf(-0.152, 0.019, 0.095),
        doubleArrayOf(-0.228, -0.076, 0.0633, 0.1457),
        doubleArrayOf(-0.304, -0.1267, 0.0380, 0.114, 0.19),
    )

    enum class FloorType { Frozen, Abandoned, Furnished, Occult, Warped }

    enum class Size { Small, Medium, Large }

    @JvmField val NPC_COMBAT_LEVEL_COMPLEXITY_MUL = doubleArrayOf(0.70, 0.75, 0.80, 0.85, 0.95, 1.0)

    @JvmField val DUNGEON_RATIO = arrayOf(
        intArrayOf(4, 4),
        intArrayOf(4, 8),
        intArrayOf(8, 8),
        intArrayOf(2, 1),
    )

    const val EAST_DOOR = 2
    const val WEST_DOOR = 0
    const val NORTH_DOOR = 1
    const val SOUTH_DOOR = 3

    @JvmField val START_ROOM_MUSICS = intArrayOf(822, 797, 765, 884, 935)

    @JvmField val DANGEROUS_MUSICS = arrayOf(
        intArrayOf(832, 834, 827, 811, 824, 808, 831, 810, 833, 837),
        intArrayOf(779, 781, 799, 802, 794, 780, 771, 790, 775, 801),
        intArrayOf(748, 769, 741, 739, 742, 753, 761, 757, 747, 756),
        intArrayOf(873, 882, 880, 860, 886, 872, 885, 889, 870, 864),
        intArrayOf(930, 914, 917, 911, 927, 939, 938, 920, 925, 936),
    )

    @JvmField val SAFE_MUSICS = arrayOf(
        intArrayOf(804, 805, 806, 807, 809, 812, 813, 814, 812, 813, 814, 821, 822),
        intArrayOf(772, 773, 775, 776, 777, 778, 788, 789, 791, 792, 793, 795, 796),
        intArrayOf(740, 744, 745, 749, 750, 751, 752, 754, 755, 758, 760, 762, 764),
        intArrayOf(861, 862, 866, 867, 868, 869, 871, 874, 875, 878, 879, 881, 883),
        intArrayOf(912, 915, 918, 919, 921, 923, 926, 928, 929, 931, 932, 933, 934),
    )

    @JvmField val OTHER_DUNGEONEERING_MUSIC = intArrayOf(0, 738, 743, 746, 759, 767, 768, 782, 784, 785, 786, 787, 803, 815, 816, 817, 818, 819, 820, 863, 865, 876, 877, 890, 891, 892, 913, 916, 922, 924, 937, 940, 1089, 1090)

    enum class GuardianMonster(private val floors: Array<FloorType>, vararg npcIdRanges: IntArray) {
        FORGOTTEN_WARRIOR(arrayOf(FloorType.Frozen, FloorType.Abandoned, FloorType.Furnished, FloorType.Occult, FloorType.Warped), Utils.range(10397, 10459), Utils.range(10246, 10308), Utils.range(10843, 10905), Utils.range(10507, 10548)),
        FORGOTTEN_RANGER(arrayOf(FloorType.Frozen, FloorType.Abandoned, FloorType.Furnished, FloorType.Occult, FloorType.Warped), Utils.range(10320, 10363)),
        FORGOTTEN_MAGE(arrayOf(FloorType.Frozen, FloorType.Abandoned, FloorType.Furnished, FloorType.Occult, FloorType.Warped), Utils.range(10560, 10603)),
        MYSTERIOUS_SHADE(arrayOf(FloorType.Frozen, FloorType.Abandoned, FloorType.Furnished, FloorType.Occult, FloorType.Warped), Utils.range(10831, 10842), intArrayOf(7398)),

        GIANT_RAT(arrayOf(FloorType.Frozen, FloorType.Abandoned, FloorType.Furnished, FloorType.Occult), Utils.range(10480, 10486)),
        DUNGEON_SPIDER(arrayOf(FloorType.Abandoned, FloorType.Furnished, FloorType.Occult), Utils.range(10496, 10505), intArrayOf(9382)),
        SKELETON_MELEE(arrayOf(FloorType.Abandoned, FloorType.Occult, FloorType.Warped), Utils.range(10629, 10669)),
        SKELETON_RANGED(arrayOf(FloorType.Abandoned, FloorType.Occult, FloorType.Warped), Utils.range(10670, 10681)),
        SKELETON_MAGIC(arrayOf(FloorType.Abandoned, FloorType.Occult, FloorType.Warped), Utils.range(10682, 10693)),
        ZOMBIE_MELEE(arrayOf(FloorType.Abandoned, FloorType.Occult, FloorType.Warped), Utils.range(10364, 10374)),
        ZOMBIE_RANGED(arrayOf(FloorType.Abandoned, FloorType.Occult, FloorType.Warped), Utils.range(10375, 10385)),
        BAT(arrayOf(FloorType.Abandoned, FloorType.Furnished, FloorType.Occult), Utils.range(10906, 10909)),
        GIANT_BAT(arrayOf(FloorType.Abandoned, FloorType.Furnished, FloorType.Occult), Utils.range(10910, 10914)),
        ANIMATED_PICKAXE(arrayOf(FloorType.Abandoned, FloorType.Warped), Utils.range(10168, 10177)),
        ANIMATED_BOOK(arrayOf(FloorType.Occult, FloorType.Warped), Utils.range(10744, 10753)),
        LESSER_DEMON(arrayOf(FloorType.Occult, FloorType.Warped), Utils.range(10492, 10495)),
        GREATER_DEMON(arrayOf(FloorType.Occult, FloorType.Warped), Utils.range(10718, 10721)),
        BLACK_DEMON(arrayOf(FloorType.Occult, FloorType.Warped), Utils.range(10722, 10725)),

        ICE_WARRIOR(arrayOf(FloorType.Frozen), Utils.range(10225, 10235)),
        ICE_SPIDER(arrayOf(FloorType.Frozen), Utils.range(10157, 10167)),
        ICE_TROLL(arrayOf(FloorType.Frozen), Utils.range(10782, 10790)),
        ICE_GIANT(arrayOf(FloorType.Frozen), Utils.range(10762, 10769)),
        ICE_ELEMENTAL(arrayOf(FloorType.Frozen), Utils.range(10460, 10468)),
        THROWER_TROLL(arrayOf(FloorType.Frozen), Utils.range(10791, 10796)),
        HYDRA(arrayOf(FloorType.Frozen), Utils.range(10610, 10618)),
        FROST_DRAGON(arrayOf(FloorType.Frozen), Utils.range(10770, 10775)),

        EARTH_WARRIOR(arrayOf(FloorType.Abandoned), Utils.range(10178, 10187)),
        GREEN_DRAGON(arrayOf(FloorType.Abandoned), Utils.range(10604, 10609)),
        HILL_GIANT(arrayOf(FloorType.Abandoned), Utils.range(10706, 10717)),
        GIANT_SKELETON(arrayOf(FloorType.Abandoned), Utils.range(10469, 10479)),
        HOBGOBLIN(arrayOf(FloorType.Abandoned), Utils.range(10236, 10245)),

        GUARD_DOG(arrayOf(FloorType.Furnished), Utils.range(10726, 10735)),
        BRUTE(arrayOf(FloorType.Furnished), Utils.range(10797, 10814)),
        IRON_DRAGON(arrayOf(FloorType.Furnished), Utils.range(10776, 10781)),

        RED_DRAGON(arrayOf(FloorType.Occult), Utils.range(10815, 10820)),
        FIRE_GIANT(arrayOf(FloorType.Occult), Utils.range(10754, 10761)),
        HELLHOUND(arrayOf(FloorType.Occult), Utils.range(10188, 10195)),
        NECROMANCER(arrayOf(FloorType.Occult), Utils.range(10196, 10206)),
        GHOST(arrayOf(FloorType.Occult), Utils.range(10821, 10830)),

        GIANT_RAT_WARP(arrayOf(FloorType.Warped), Utils.range(12914, 12920)),
        GUARD_DOG_WARP(arrayOf(FloorType.Warped), Utils.range(12922, 12931)),
        BRUTE_WARP(arrayOf(FloorType.Furnished), Utils.range(12932, 12940)),
        BLACK_DRAGON(arrayOf(FloorType.Warped), Utils.range(10219, 10224)),
        REBORN_MAGE(arrayOf(FloorType.Warped), Utils.range(12903, 12913)),
        REBORN_WARRIOR(arrayOf(FloorType.Warped), Utils.range(12941, 12951)),
        ANKOU(arrayOf(FloorType.Warped), Utils.range(10736, 10743));

        private val npcIds: IntArray = npcIdRanges.flatMap { it.asList() }.toIntArray()

        fun getNPCIds(): IntArray = npcIds

        companion object {
            private val MAP = HashMap<Int, GuardianMonster>()
            private val FLOOR_MAP = HashMap<FloorType, MutableSet<GuardianMonster>>()

            init {
                for (g in entries) {
                    for (floor in g.floors)
                        FLOOR_MAP.getOrPut(floor) { HashSet() }.add(g)
                    for (id in g.npcIds)
                        MAP[id] = g
                }
            }

            @JvmStatic fun forId(npcId: Int): GuardianMonster? = MAP[npcId]

            @JvmStatic fun forFloor(floorId: FloorType): MutableSet<GuardianMonster>? = FLOOR_MAP[floorId]
        }
    }

    @JvmField val HUNTER_CREATURES = intArrayOf(11086, 11087, 11088, 11089, 11090, 11091, 11092, 11093, 11094, 11095)

    @JvmField val START_ROOMS = arrayOf(
        StartRoom(14, 632, EAST_DOOR, WEST_DOOR, NORTH_DOOR, SOUTH_DOOR),
        StartRoom(14, 624, SOUTH_DOOR),
        StartRoom(14, 626, WEST_DOOR, SOUTH_DOOR),
        StartRoom(14, 630, WEST_DOOR, NORTH_DOOR, SOUTH_DOOR),
    )

    @JvmField val NORMAL_ROOMS = arrayOf(
        NormalRoom(8, 240, spot(3, 7), SOUTH_DOOR),
        NormalRoom(8, 242, spot(3, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(8, 244, spot(3, 12), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(8, 246, spot(4, 13), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(8, 248, spot(3, 12), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(8, 256, spot(12, 12), SOUTH_DOOR),
        NormalRoom(8, 258, spot(12, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(8, 260, spot(11, 6), SOUTH_DOOR, NORTH_DOOR),
        NormalRoom(8, 262, spot(12, 6), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR),
        NormalRoom(8, 264, spot(12, 12), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(10, 240, spot(3, 3), SOUTH_DOOR),
        NormalRoom(10, 242, spot(7, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(10, 244, spot(2, 4), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(10, 246, spot(2, 12), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(10, 248, spot(2, 12), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(10, 256, spot(8, 12), SOUTH_DOOR),
        NormalRoom(10, 258, spot(12, 2), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(10, 260, spot(3, 4), SOUTH_DOOR, NORTH_DOOR),
        NormalRoom(10, 262, spot(7, 8), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR),
        NormalRoom(10, 264, spot(5, 10), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(12, 240, spot(3, 7), SOUTH_DOOR),
        NormalRoom(12, 242, spot(11, 7), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(12, 244, spot(3, 12), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(12, 246, spot(10, 7), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(12, 248, spot(10, 7), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(12, 256, spot(9, 7), SOUTH_DOOR),
        NormalRoom(12, 258, spot(3, 6), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(12, 260, spot(4, 10), SOUTH_DOOR, NORTH_DOOR),
        NormalRoom(12, 262, spot(9, 7), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR),
        NormalRoom(12, 264, spot(3, 6), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(14, 240, spot(7, 10), SOUTH_DOOR),
        NormalRoom(14, 256, spot(4, 13), SOUTH_DOOR),
        NormalRoom(14, 258, spot(12, 9), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(14, 260, spot(12, 13), SOUTH_DOOR, NORTH_DOOR),
        NormalRoom(14, 262, spot(7, 7), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR),
        NormalRoom(14, 264, spot(11, 13), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(16, 240, spot(7, 10), SOUTH_DOOR),
        NormalRoom(16, 256, spot(4, 12), SOUTH_DOOR),
        NormalRoom(16, 258, spot(7, 11), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(16, 260, spot(4, 3), SOUTH_DOOR, NORTH_DOOR),
        NormalRoom(16, 262, spot(7, 8), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR),
        NormalRoom(16, 264, spot(7, 7), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(18, 240, spot(7, 10), SOUTH_DOOR),
        NormalRoom(18, 260, spot(5, 12), SOUTH_DOOR, NORTH_DOOR),
        NormalRoom(18, 264, spot(3, 12), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(20, 240, spot(9, 13), SOUTH_DOOR),
        NormalRoom(20, 258, spot(11, 9), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(22, 240, spot(8, 9), SOUTH_DOOR),
        NormalRoom(22, 256, spot(7, 10), SOUTH_DOOR),
        NormalRoom(22, 258, spot(11, 10), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(22, 260, spot(7, 8), SOUTH_DOOR, NORTH_DOOR),
        NormalRoom(22, 262, spot(7, 7), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR),
        NormalRoom(22, 264, spot(7, 7), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(24, 242, spot(7, 7), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(24, 258, spot(12, 10), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(24, 264, spot(7, 7), SOUTH_DOOR, NORTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(26, 242, spot(9, 9), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(28, 246, spot(11, 8), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(30, 244, spot(3, 6), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(32, 244, spot(3, 12), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(34, 242, spot(12, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(36, 248, spot(2, 13), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(38, 246, spot(12, 10), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(40, 244, spot(3, 12), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(42, 244, spot(7, 7), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(44, 244, spot(7, 7), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(46, 244, spot(7, 7), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(48, 244, spot(7, 7), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(50, 244, spot(7, 7), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(52, 240, spot(7, 11), SOUTH_DOOR),
        NormalRoom(54, 240, spot(8, 11), SOUTH_DOOR),
        NormalRoom(56, 240, spot(3, 10), SOUTH_DOOR),
        NormalRoom(58, 240, spot(3, 12), SOUTH_DOOR),
        NormalRoom(60, 240, spot(9, 12), SOUTH_DOOR),
        NormalRoom(62, 240, spot(12, 12), SOUTH_DOOR),
        NormalRoom(64, 240, spot(2, 12), SOUTH_DOOR),
        NormalRoom(66, 240, spot(8, 10), SOUTH_DOOR),
        NormalRoom(68, 240, spot(3, 7), SOUTH_DOOR),
        NormalRoom(70, 240, spot(8, 12), SOUTH_DOOR),
        NormalRoom(72, 240, spot(12, 7), SOUTH_DOOR),
        NormalRoom(74, 240, spot(4, 10), SOUTH_DOOR),
        NormalRoom(76, 240, spot(8, 7), SOUTH_DOOR),
        NormalRoom(78, 240, spot(3, 4), SOUTH_DOOR),
        NormalRoom(62, 242, spot(13, 11), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(62, 244, spot(2, 3), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(62, 246, spot(13, 11), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(62, 248, spot(2, 3), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(64, 242, spot(7, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(64, 244, spot(13, 12), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(64, 246, spot(7, 7), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(64, 248, spot(7, 7), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(66, 242, spot(12, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(66, 244, spot(3, 4), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(66, 246, spot(12, 12), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(66, 248, spot(3, 3), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(68, 242, spot(12, 8), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(68, 244, spot(5, 8), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(68, 246, spot(5, 8), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(70, 242, spot(11, 10), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(70, 244, spot(8, 7), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(70, 246, spot(8, 7), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(72, 242, spot(9, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(72, 244, spot(10, 8), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(72, 246, spot(11, 8), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(72, 248, spot(8, 5), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(74, 242, spot(3, 6), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(74, 244, spot(7, 5), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(74, 246, spot(3, 7), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(74, 248, spot(2, 6), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(76, 242, spot(11, 9), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(76, 244, spot(7, 7), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(76, 246, spot(7, 7), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(76, 248, spot(7, 11), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        NormalRoom(78, 242, spot(12, 12), SOUTH_DOOR, WEST_DOOR),
        NormalRoom(78, 244, spot(12, 12), NORTH_DOOR, SOUTH_DOOR),
        NormalRoom(78, 246, spot(12, 3), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR),
        NormalRoom(78, 248, spot(2, 3), NORTH_DOOR, SOUTH_DOOR, WEST_DOOR, EAST_DOOR)
    )

    enum class MapRoomIcon(val graphicId: Int, val isOpen: Boolean, vararg val doorDirections: Int) {
        MAP_1(2791, true, SOUTH_DOOR),
        MAP_2(2792, true, WEST_DOOR),
        MAP_3(2793, true, NORTH_DOOR),
        MAP_4(2794, true, EAST_DOOR),
        MAP_5(2795, true, WEST_DOOR, SOUTH_DOOR),
        MAP_6(2796, true, WEST_DOOR, NORTH_DOOR),
        MAP_7(2797, true, NORTH_DOOR, EAST_DOOR),
        MAP_8(2798, true, SOUTH_DOOR, EAST_DOOR),
        MAP_9(2799, true, SOUTH_DOOR, WEST_DOOR, NORTH_DOOR),
        MAP_10(2800, true, WEST_DOOR, NORTH_DOOR, EAST_DOOR),
        MAP_11(2801, true, SOUTH_DOOR, NORTH_DOOR, EAST_DOOR),
        MAP_12(2802, true, SOUTH_DOOR, WEST_DOOR, EAST_DOOR),
        MAP_13(2803, true, SOUTH_DOOR, NORTH_DOOR, EAST_DOOR, WEST_DOOR),
        MAP_14(2804, true, SOUTH_DOOR, NORTH_DOOR),
        MAP_15(2805, true, WEST_DOOR, EAST_DOOR),
        MAP_16(2787, false, SOUTH_DOOR),
        MAP_17(2788, false, WEST_DOOR),
        MAP_18(2789, false, NORTH_DOOR),
        MAP_19(2790, false, EAST_DOOR);

        fun hasSouthDoor(): Boolean = hasDoor(SOUTH_DOOR)

        fun hasNorthDoor(): Boolean = hasDoor(NORTH_DOOR)

        fun hasWestDoor(): Boolean = hasDoor(WEST_DOOR)

        fun hasEastDoor(): Boolean = hasDoor(EAST_DOOR)

        fun hasDoor(direction: Int): Boolean = direction in doorDirections
    }

    @JvmField val PUZZLE_DOOR_ORDER = arrayOf(
        intArrayOf(SOUTH_DOOR, WEST_DOOR),
        intArrayOf(SOUTH_DOOR, EAST_DOOR),
        intArrayOf(SOUTH_DOOR, NORTH_DOOR),
        intArrayOf(SOUTH_DOOR, NORTH_DOOR, WEST_DOOR),
        intArrayOf(SOUTH_DOOR, EAST_DOOR, WEST_DOOR),
        intArrayOf(SOUTH_DOOR, EAST_DOOR, NORTH_DOOR),
        intArrayOf(SOUTH_DOOR, EAST_DOOR, NORTH_DOOR, WEST_DOOR),
    )

    enum class Puzzle(val chunkX: Int, private val clazz: Class<out VisibleRoom>, private val keys: Array<IntArray>? = null, private val allowResources: Boolean = true) {
        CRYSTAL_PUZZLE(12, CrystalPuzzleRoom::class.java, all(spot(13, 5)), false),
        TOXIN_MAZE(20, ToxinMaze::class.java, all(spot(7, 8)), false),
        FLIP_TILES(24, FlipTilesRoom::class.java, list(spot(1, 3), spot(1, 12), spot(1, 3), spot(14, 11), spot(14, 11), spot(1, 9), spot(1, 3))),
        FREMENNIK_CAMP(26, FremennikCampRoom::class.java, list(spot(6, 14), spot(10, 2), spot(14, 6), spot(5, 12), spot(13, 10), spot(3, 5), spot(13, 10))),
        FISHING_FERRET(32, FishingFerretRoom::class.java, all(spot(2, 1)), false),
        LEVERS(38, LeverRoom::class.java, list(spot(1, 5), spot(10, 14), spot(1, 5), spot(10, 14), spot(1, 5), spot(10, 14), spot(1, 5)), false),
        SLIDING_STATUES(42, SlidingStatuesRoom::class.java, all(spot(1, 3)), false),
        POLTERGEIST(52, PoltergeistRoom::class.java, list(spot(1, 10), spot(2, 10), spot(3, 5), spot(2, 4), spot(13, 4), spot(2, 12), spot(3, 4)), false),
        UNHAPPY_GHOST(60, UnhappyGhostRoom::class.java, list(spot(2, 3), spot(3, 5), spot(3, 5), spot(2, 2), spot(3, 3), spot(2, 2), spot(4, 3)), false),
        RETURN_THE_FLOW(66, ReturnTheFlowRoom::class.java, list(spot(3, 3), spot(2, 12), spot(2, 2), spot(4, 2), spot(3, 3), spot(2, 2), spot(2, 2)));

        fun newInstance(): VisibleRoom? = try {
            clazz.getConstructor().newInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("error creating puzzle instance")
            null
        }

        fun isAvailableOnFloorType(floorType: FloorType): Boolean = !(this == POLTERGEIST && floorType == FloorType.Frozen)

        fun allowResources(): Boolean = allowResources

        fun getKeySpot(i: Int): IntArray? = if (keys == null || i >= keys.size) null else keys[i]
    }

    @JvmField val PUZZLE_ROOMS: Array<HandledRoom> = Array(Puzzle.entries.size * PUZZLE_DOOR_ORDER.size) {
        HandledPuzzleRoom(it % PUZZLE_DOOR_ORDER.size, Puzzle.entries[it / PUZZLE_DOOR_ORDER.size])
    }

    @JvmField val BOSS_ROOMS = arrayOf(
        arrayOf(
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10024, 5, 10, false, BOSS_NPC) }, 820, 9, 26, 626),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9929, 7, 11, false, BOSS_NPC) }, 819, 6, 26, 624),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9912, 7, 11, false, BOSS_NPC) }, 818, 3, 26, 626),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10040, 7, 11, false, BOSS_NPC) }, 815, 1, 24, 626),
            BossRoom({ dungeon, reference ->
                dungeon.spawnNPC(reference, 9948, 5, 10, false, BOSS_NPC)
                dungeon.spawnObject(reference, 49283, ObjectShape.SCENERY_INTERACT, 3, 0, 11)
                if (dungeon.party.team.size >= 2)
                    dungeon.spawnObject(reference, 49283, ObjectShape.SCENERY_INTERACT, 0, 11, 11)
            }, 817, 1, 28, 624),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9965, 8, 12, false, BOSS_NPC) }, 816, 1, 30, 624),
        ),
        arrayOf(
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10128, 6, 8, false, BOSS_NPC) }, 786, 33, 26, 642),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10143, 6, 6, false, BOSS_NPC) }, 783, 30, 28, 640),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10111, 6, 8, false, BOSS_NPC) }, 784, 15, 26, 640),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10073, 5, 10, false, BOSS_NPC) }, 785, 12, 24, 642),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10059, 6, 8, false, BOSS_NPC) }, 787, 12, 24, 640),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 10058, 5, 2, false, BOSS_NPC) }, 782, 12, 30, 640),
        ),
        arrayOf(
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9725, 6, 6, false, BOSS_NPC) }, 759, 26, 24, 656),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9753, 6, 6, false, BOSS_NPC) }, 768, 23, 24, 658),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9842, 8, 7, false, BOSS_NPC) }, 738, 20, 26, 658),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9898, 6, 6, false, BOSS_NPC) }, 767, 18, 28, 656),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9782, 6, 9, false, BOSS_NPC) }, 743, 18, 30, 656),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 9767, 7, 7, false, BOSS_NPC) }, 746, 18, 26, 656),
        ),
        arrayOf(
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 11872, 5, 5, false, BOSS_NPC) }, 890, 45, 26, 672),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 11925, 8, 7, false, BOSS_NPC) }, 892, 42, 24, 674),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 11737, 7, 10, false, BOSS_NPC) }, 876, 39, 30, 672),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 11708, 5, 5, false, BOSS_NPC) }, 891, 36, 28, 672),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 11812, 6, 8, false, BOSS_NPC) }, 877, 36, 26, 674),
            BossRoom({ dungeon, reference ->
                dungeon.spawnNPC(reference, 11940, 5, 6, false, BOSS_NPC)
                dungeon.spawnNPC(reference, 12044, 7, 9, false, BOSS_NPC)
                dungeon.spawnNPC(reference, 11999, 7, 6, false, BOSS_NPC)
            }, 865, 36, 24, 672),
        ),
        arrayOf(
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 12752, 6, 9, false, BOSS_NPC) }, 924, 57, 24, 690),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 12478, 5, 6, false, BOSS_NPC) }, 937, 54, 26, 690),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 12886, 8, 6, false, BOSS_NPC) }, 916, 51, 30, 688),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 12865, 5, 6, false, BOSS_NPC) }, 940, 48, 24, 688),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 12848, 7, 5, false, BOSS_NPC) }, 913, 48, 28, 688),
            BossRoom({ dungeon, reference -> dungeon.spawnNPC(reference, 12737, 8, 8, false, BOSS_NPC) }, 922, 48, 26, 688),
        ),
    )

    @JvmStatic
    fun isDungeonSong(musicId: Int): Boolean =
        musicId in START_ROOM_MUSICS ||
            DANGEROUS_MUSICS.any { musicId in it } ||
            SAFE_MUSICS.any { musicId in it } ||
            musicId in OTHER_DUNGEONEERING_MUSIC

    @JvmField val WALL_BASE_X_Y = arrayOf(
        intArrayOf(14, 1, 0, 14),
        intArrayOf(1, 1, 14, 0),
        intArrayOf(1, 1, 0, 14),
        intArrayOf(1, 14, 14, 0),
    )

    const val SET_RESOURCES_MAX_TRY = 300

    const val FISH_SPOT_OBJECT_ID = 49922
    const val FISH_SPOT_NPC_ID = 2859

    @JvmField val FARMING_CYCLES = longArrayOf(0, 30000, 60000, Int.MAX_VALUE.toLong())
    const val FARMING_PATCH_BEGINING = 50040
    const val FARMING_PATCH_END = 50189
    const val EMPTY_FARMING_PATCH = 50038

    const val NORMAL_NPC = 0
    const val GUARDIAN_NPC = 1
    const val FISH_SPOT_NPC = 2
    const val BOSS_NPC = 3
    const val FORGOTTEN_WARRIOR = 4
    const val SLAYER_NPC = 5
    const val HUNTER_NPC = 6
    const val PUZZLE_NPC = 7

    @JvmField var GROUP_GATESTONE_RUNES = intArrayOf(17792, 3)

    @JvmField val HOARDSTALKER_ITEMS = arrayOf(
        intArrayOf(17630, 17424, 17682, 17448, 17797),
        intArrayOf(17632, 17426, 17684, 17450, 17799),
        intArrayOf(17634, 17428, 17686, 17452, 17801),
        intArrayOf(17636, 17430, 17688, 17454, 17803),
        intArrayOf(17638, 17432, 17700, 17456, 17805),
        intArrayOf(17640, 17434, 17702, 17458, 17807),
        intArrayOf(17642, 17436, 17704, 17460, 17809),
        intArrayOf(17644, 17438, 17706, 17462, 17811),
        intArrayOf(17646, 17440, 17708, 17464, 17813),
        intArrayOf(17648, 17442, 17710, 17466, 17815),
    )

    @JvmField val SLAYER_LEVELS = intArrayOf(5, 10, 17, 30, 41, 52, 63, 71, 80, 90, 99)
}

private fun spot(x: Int, y: Int): IntArray = intArrayOf(x, y)

private fun all(spot: IntArray): Array<IntArray> = arrayOf(spot, spot, spot, spot, spot, spot, spot)

private fun list(vararg list: IntArray): Array<IntArray>? {
    if (list.size != 7) {
        System.err.println("Error in key list " + list.contentToString())
        return null
    }
    return arrayOf(*list)
}
