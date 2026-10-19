package com.rs.game.model.entity.player

import com.rs.game.World
import com.rs.game.content.clans.ClansManager
import com.rs.utils.basId
import com.rs.utils.rotate180Animation
import com.rs.utils.rotate90LeftAnimation
import com.rs.utils.rotate90RightAnimation
import com.rs.utils.specialByte
import com.rs.utils.walkingAnimation
import org.darkan.core.Logger
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.cache.NPCDefinitions
import org.darkan.core.cache.StructDefinitions
import org.darkan.core.EnvVars
import org.darkan.core.model.Item
import org.darkan.core.net.prot.Codec
import org.darkan.core.net.prot.update.UpdateProtocol
import org.darkan.core.update.AppearanceSlot
import org.darkan.core.update.AppearanceTitle
import org.darkan.core.update.NpcRenderAnimations
import org.darkan.core.update.PlayerAppearanceData
import world.gregs.voidps.cache.definition.data.EnumDefinition
import world.gregs.voidps.cache.definition.data.ItemDefinition
import world.gregs.voidps.cache.definition.data.NPCDefinition
import world.gregs.voidps.cache.definition.data.StructDefinition

class Appearance {

    var title: Int = -1
        private set
    private var lookI: IntArray = IntArray(7)
    private var colour: IntArray = IntArray(10)
    var isMale: Boolean = true
        private set

    @Transient
    var appeareanceData: ByteArray? = null
        private set

    @Transient
    var mD5AppeareanceDataHash: ByteArray? = null
        private set

    @Transient
    private var transformedNpcId: Short = -1

    @Transient
    private var hidePlayer: Boolean = false

    @Transient
    private var player: Player? = null

    @Transient
    private var wornOverrides: MutableMap<Int, Int>? = null

    fun overrideWorn(slot: Int, obj: Int) {
        val overrides = wornOverrides ?: HashMap<Int, Int>().also { wornOverrides = it }
        if (overrides[slot] == obj) return
        overrides[slot] = obj
        generateAppearanceData()
    }

    fun clearWornOverride(slot: Int) {
        if (wornOverrides?.remove(slot) != null) generateAppearanceData()
    }

    private fun worn(slot: Int): AppearanceSlot = wornOverrides?.get(slot)?.let { AppearanceSlot.Worn(it) } ?: wornOrEmpty(player!!.equipment[slot])

    init {
        resetAppearance()
    }

    fun setPlayer(player: Player) {
        this.player = player
        transformedNpcId = -1
        if (lookI == null)
            resetAppearance()
    }

    fun transformIntoNPC(id: Int) {
        transformedNpcId = id.toShort()
        generateAppearanceData()
    }

    var isHidden: Boolean
        get() = hidePlayer
        set(hidden) {
            hidePlayer = hidden
            generateAppearanceData()
        }

    private fun getHatHairStyle(baseStyle: Int, isFaceMask: Boolean): Int {
        val lookup: EnumDefinition = EnumDefinitions.getEnum(if (isMale) MALE_HAIR_SLOT_LOOKUP else FEMALE_HAIR_SLOT_LOOKUP)
        val slot = lookup.getIntValue(baseStyle)
        val structLookup: EnumDefinition = EnumDefinitions.getEnum(if (isMale) MALE_HAIR_STRUCT_LOOKUP else FEMALE_HAIR_STRUCT_LOOKUP)
        val structID = structLookup.getIntValue(slot)
        return StructDefinitions.getStruct(structID).getIntValue(if (isFaceMask) HAIR_WITH_FACE_MASK_PARAM else HAIR_WITH_HAT_PARAM, -1)
    }

    /** Revision encoder that owns the appearance block's wire format. */
    private val updateProtocol: UpdateProtocol
        get() = Codec.get(EnvVars.majorVersion)?.updateProtocol
            ?: error("No update protocol registered for revision ${EnvVars.majorVersion}")

    fun generateAppearanceData() {
        val player = this.player ?: return
        val pvpArea = World.isPvpArea(player)
        val showSkillTotal = player.tempAttribs.getB("showSkillTotal") && !pvpArea
        val npcDefs: NPCDefinition? = if (isNPC) NPCDefinitions.getDefs(transformedNpcId.toInt()) else null
        val hasTitle = title != 0 || player.getTitle() != null

        val appearance = PlayerAppearanceData(
            male = isMale,
            npcTransformSizeFlag = npcDefs != null && npcDefs.aBoolean2883,
            showSkillTotal = showSkillTotal,
            title = if (hasTitle) AppearanceTitle(player.formattedTitle, isTitleAfter(title) || player.isTitleAfter) else null,
            skullIcon = if (player.hasSkull()) player.skullId else -1,
            headIcon = if (player.tempAttribs.getI("customHeadIcon", -1) != -1)
                player.tempAttribs.getI("customHeadIcon", 0)
            else
                player.prayer.prayerHeadIcon,
            hidden = hidePlayer,
            transformedNpcId = transformedNpcId.toInt(),
            slots = if (npcDefs != null) emptyList() else buildSlots(player),
            meshOverrides = if (npcDefs != null) emptyList() else getMeshModifiers().map { it.toOverride() },
            colours = colour.copyOf(),
            renderEmote = renderEmote,
            displayName = player.displayName,
            combatLevel = if (pvpArea) player.skills.combatLevel else player.skills.combatLevelWithSummoning,
            totalLevel = player.skills.totalLevel,
            summoningCombatLevel = if (pvpArea) player.skills.combatLevelWithSummoning else 0,
            pvpCombatLevelThreshold = player.pvpCombatLevelThreshhold,
            npcRenderAnimations = npcDefs?.let {
                NpcRenderAnimations(
                    walk = it.walkingAnimation,
                    rotate180 = it.rotate180Animation,
                    rotate90Right = it.rotate90RightAnimation,
                    rotate90Left = it.rotate90LeftAnimation,
                    special = it.specialByte,
                )
            },
        )

        val encoded = updateProtocol.encodePlayerAppearance(appearance)
        appeareanceData = encoded.data
        mD5AppeareanceDataHash = encoded.hash
    }

    /**
     * Resolves the body/equipment slots in transmission order.
     *
     * Each entry is the *semantic* value for that slot; the core revision encoder decides how it
     * is framed on the wire.
     */
    private fun buildSlots(player: Player): List<AppearanceSlot> {
        val slots = ArrayList<AppearanceSlot>(13)

        for (index in 0 until 4)
            slots.add(wornOrEmpty(player.equipment[index]))

        val chest: Item? = player.equipment[Equipment.CHEST]
        slots.add(chest?.let { AppearanceSlot.Worn(it.id) } ?: AppearanceSlot.Body(lookI[2]))

        slots.add(wornOrEmpty(player.equipment[Equipment.SHIELD]))

        slots.add(
            when {
                lookI[3] != -1 && (chest == null || !Equipment.hideArms(chest)) -> AppearanceSlot.Body(lookI[3])
                chest != null && !Equipment.hideArms(chest) -> AppearanceSlot.Body(oldArms)
                else -> AppearanceSlot.Empty
            }
        )

        val legs: Item? = player.equipment[Equipment.LEGS]
        slots.add(legs?.let { AppearanceSlot.Worn(it.id) } ?: AppearanceSlot.Body(lookI[5]))

        slots.add(hairSlot(player.equipment[Equipment.HEAD]))

        val hands: Item? = player.equipment[Equipment.HANDS]
        slots.add(hands?.let { AppearanceSlot.Worn(it.id) } ?: AppearanceSlot.Body(lookI[4]))

        val feet: Item? = player.equipment[Equipment.FEET]
        slots.add(feet?.let { AppearanceSlot.Worn(it.id) } ?: AppearanceSlot.Body(lookI[6]))

        val beardBlocker: Item? = player.equipment[if (isMale) Equipment.HEAD else Equipment.CHEST]
        slots.add(
            if (isMale && lookI[1] != -1 && (beardBlocker == null || !Equipment.hideBeard(beardBlocker)))
                AppearanceSlot.Body(lookI[1])
            else
                AppearanceSlot.Empty
        )

        slots.add(worn(Equipment.AURA))
        return slots
    }

    /** Hair is hidden entirely by some helms, and remapped to a hat-compatible style by others. */
    private fun hairSlot(head: Item?): AppearanceSlot {
        if (lookI[0] == -1) return AppearanceSlot.Empty
        if (head == null) return AppearanceSlot.Body(lookI[0])
        if (Equipment.hideHair(head)) return AppearanceSlot.Empty
        val hatHairStyle = getHatHairStyle(lookI[0], head.definitions.faceMask())
        return if (hatHairStyle != -1) AppearanceSlot.Body(hatHairStyle) else AppearanceSlot.Empty
    }

    private fun wornOrEmpty(item: Item?): AppearanceSlot =
        if (item == null) AppearanceSlot.Empty else AppearanceSlot.Worn(item.id)

    private fun getMeshModifiers(): ArrayList<ItemMeshModifier> {
        val player = this.player!!
        val modifiers = ArrayList<ItemMeshModifier>()
        var slotFlag = -1
        for (slotId in 0 until Equipment.SIZE) {
            if (Equipment.DISABLED_SLOTS[slotId] != 0) continue
            slotFlag++
            if (player.equipment.getId(slotId) == -1 || wornOverrides?.containsKey(slotId) == true) continue
            val item: Item = player.equipment[slotId] ?: continue
            val defs: ItemDefinition = item.definitions ?: continue

            when (defs.id) {
                1833, 1835, 1171, 2637 -> {
                    val mod = ItemMeshModifier(defs, slotFlag)
                    var add = false
                    if (item.getMetaDataI("drTOr", -1) > 0) {
                        mod.addTextures(item.getMetaDataI("drTOr", -1), item.getMetaDataI("drTOr", -1), item.getMetaDataI("drTOr", -1))
                        add = true
                    }
                    if (item.getMetaDataI("drCOr", -1) > 0) {
                        mod.addColors(item.getMetaDataI("drCOr", -1))
                        add = true
                    }
                    if (add) modifiers.add(mod)
                }
                20767, 20768 -> {
                    modifiers.add(
                        ItemMeshModifier(defs, slotFlag)
                            .addColors(*player.maxedCapeCustomized)
                    )
                }
                20769, 20770, 20771, 20772 -> {
                    modifiers.add(
                        ItemMeshModifier(defs, slotFlag)
                            .addColors(*player.completionistCapeCustomized)
                    )
                }
                20708, 20709 -> {
                    if (player.clan != null) {
                        modifiers.add(
                            ItemMeshModifier(defs, slotFlag)
                                .addColors(*player.clan.getMotifColors())
                                .addTextures(*ClansManager.getMotifTextures(player.clan))
                        )
                    }
                }
            }

            if (slotId == Equipment.AURA && player.auraManager.isActive) {
                modifiers.add(
                    ItemMeshModifier(defs, slotFlag)
                        .addBodyModels(player.auraManager.auraModelId, player.auraManager.auraModelId2)
                )
            }
        }
        return modifiers
    }

    val size: Int
        get() = if (transformedNpcId >= 0) NPCDefinitions.getDefs(transformedNpcId.toInt()).size else 1

    fun setBAS(id: Int) {
        player!!.bas = id
        generateAppearanceData()
    }

    fun isTitleAfter(title: Int): Boolean {
        if (title in 32..37) return true
        if (title == 64) return false
        if (title in 58..65) return true
        return title == 40 || title == 43 || title == 45 || title == 47 || title == 49 || title == 53 || title == 55 || title == 56 || title == 72 || title == 73
    }

    val transformedNPC: Int
        get() = transformedNpcId.toInt()

    val isNPC: Boolean
        get() = transformedNpcId.toInt() != -1

    val renderEmote: Int
        get() {
            val player = this.player!!
            if (player.bas >= 0) return player.bas
            if (transformedNpcId >= 0) return NPCDefinitions.getDefs(transformedNpcId.toInt()).basId
            return player.equipment.weaponBAS
        }

    fun resetAppearance() {
        lookI = IntArray(7)
        colour = IntArray(10)
        male()
    }

    fun hasDefaultLook(): Boolean {
        val default = Appearance()
        return isMale && lookI.contentEquals(default.lookI) && colour.contentEquals(default.colour)
    }

    val oldArms: Int
        get() = if (isMale) 26 else 61

    fun male() {
        lookI[0] = 310
        lookI[1] = 16
        lookI[2] = 452
        lookI[3] = -1
        lookI[4] = 371
        lookI[5] = 627
        lookI[6] = 433
        colour[0] = 12
        colour[1] = 218
        colour[2] = 218
        colour[3] = 180
        colour[4] = 110
        colour[5] = 0
        colour[6] = 0
        colour[7] = 0
        colour[8] = 0
        colour[9] = 0
        isMale = true
    }

    fun female() {
        lookI[0] = 274
        lookI[1] = -1
        lookI[2] = 561
        lookI[3] = -1
        lookI[4] = 514
        lookI[5] = 482
        lookI[6] = 547
        colour[0] = 12
        colour[1] = 218
        colour[2] = 218
        colour[3] = 180
        colour[4] = 110
        colour[5] = 0
        colour[6] = 0
        colour[7] = 0
        colour[8] = 0
        colour[9] = 0
        isMale = false
    }

    fun setLook(i: Int, i2: Int) {
        lookI[i] = i2
        if (i == 2) verifyArms()
    }

    fun setColor(i: Int, i2: Int) {
        colour[i] = i2.toByte().toInt()
    }

    fun setMale(male: Boolean) {
        isMale = male
    }

    fun setHairStyle(i: Int) {
        lookI[0] = i
    }

    fun setTopStyle(i: Int) {
        lookI[2] = i
        verifyArms()
    }

    val topStyle: Int
        get() = lookI[2]

    var armsStyle: Int
        get() = lookI[3]
        set(i) { lookI[3] = i }

    var wristsStyle: Int
        get() = lookI[4]
        set(i) { lookI[4] = i }

    fun setLegsStyle(i: Int) {
        lookI[5] = i
    }

    val hairStyle: Int
        get() = lookI[0]

    var beardStyle: Int
        get() = lookI[1]
        set(i) { lookI[1] = i }

    var facialHair: Int
        get() = lookI[1]
        set(i) { lookI[1] = i }

    var skinColor: Int
        get() = colour[4]
        set(color) { colour[4] = color.toByte().toInt() }

    var hairColor: Int
        get() = colour[0]
        set(color) { colour[0] = color.toByte().toInt() }

    fun setTopColor(color: Int) {
        colour[1] = color.toByte().toInt()
    }

    fun setLegsColor(color: Int) {
        colour[2] = color.toByte().toInt()
    }

    fun setBootsStyle(i: Int) {
        lookI[6] = i
    }

    fun setBootsColor(color: Int) {
        colour[3] = color.toByte().toInt()
    }

    fun setTitle(title: Int) {
        this.title = title
        generateAppearanceData()
    }

    fun verifyArms() {
        val ts = topStyle
        val setId = getSetByStyle(ts, 3, !isMale)
        if (setId != -1) {
            val set: StructDefinition = StructDefinitions.getStruct(setId)
            armsStyle = set.getIntValue(1183, -1)
            wristsStyle = set.getIntValue(1184, -1)
        } else {
            if (EnumDefinitions.getEnum(if (isMale) 711 else 693).getKeyForValue(armsStyle) == -1)
                armsStyle = if (isMale) 26 else 61
            if (EnumDefinitions.getEnum(if (isMale) 749 else 751).getKeyForValue(wristsStyle) == -1)
                wristsStyle = if (isMale) 34 else 68
        }
    }

    fun printDebug() {
        for (i in lookI.indices)
            Logger.debug(Appearance::class.java, "printDebug", "lookI[$i] = ${lookI[i]};")
        for (i in colour.indices)
            Logger.debug(Appearance::class.java, "printDebug", "colour[$i] = ${colour[i]};")
    }

    companion object {
        const val FEMALE_HAIR_STRUCT_LOOKUP = 2341
        const val MALE_HAIR_STRUCT_LOOKUP = 2338
        const val MALE_HAIR_SLOT_LOOKUP = 2339
        const val FEMALE_HAIR_SLOT_LOOKUP = 2342
        const val HAIR_WITH_HAT_PARAM = 790
        const val HAIR_WITH_FACE_MASK_PARAM = 791

        @JvmStatic
        private fun getSetByStyle(styleID: Int, styleSlot: Int, female: Boolean): Int {
            val sets: EnumDefinition = EnumDefinitions.getEnum(5735)
            for (slot in sets.getSize() - 1 downTo 0) {
                val structId = sets.getIntValue(slot)
                if (structId != -1) {
                    val struct: StructDefinition = StructDefinitions.getStruct(structId)
                    var v7 = 0
                    var setId = getSetStruct(struct, 0, female)
                    while (setId != -1) {
                        val setStyles: StructDefinition = StructDefinitions.getStruct(setId)
                        when (styleSlot) {
                            3 -> if (setStyles.getIntValue(1182, -1) == styleID) return setId
                            4 -> if (setStyles.getIntValue(1183, -1) == styleID) return setId
                            5 -> if (setStyles.getIntValue(1184, -1) == styleID) return setId
                            6 -> if (setStyles.getIntValue(1185, -1) == styleID) return setId
                            else -> return -1
                        }
                        v7++
                        setId = getSetStruct(struct, v7, female)
                    }
                }
            }
            return -1
        }

        @JvmStatic
        private fun getSetStruct(struct: StructDefinition, slot: Int, female: Boolean): Int {
            return when (slot) {
                0 -> struct.getIntValue(if (female) 1175 else 1169, -1)
                1 -> struct.getIntValue(if (female) 1176 else 1170, -1)
                2 -> struct.getIntValue(if (female) 1177 else 1171, -1)
                3 -> struct.getIntValue(if (female) 1178 else 1172, -1)
                4 -> struct.getIntValue(if (female) 1179 else 1173, -1)
                5 -> struct.getIntValue(if (female) 1180 else 1174, -1)
                else -> -1
            }
        }
    }
}
