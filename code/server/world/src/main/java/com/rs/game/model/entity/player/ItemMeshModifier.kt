package com.rs.game.model.entity.player

import org.darkan.core.update.BodyModelOverride
import org.darkan.core.update.HeadModelOverride
import org.darkan.core.update.ItemMeshOverride
import world.gregs.voidps.cache.definition.data.ItemDefinition

class ItemMeshModifier(
    private val defs: ItemDefinition,
    private val slotFlag: Int
) {
    private var maleBody: IntArray? = null
    private var femaleBody: IntArray? = null
    private var maleHead: IntArray? = null
    private var femaleHead: IntArray? = null
    private var modifiedColors: IntArray? = null
    private var modifiedTextures: IntArray? = null

    fun addBodyModels(maleBodies: IntArray, femaleBodies: IntArray): ItemMeshModifier {
        maleBody = IntArray(3)
        femaleBody = IntArray(3)
        maleBody!![0] = defs.primaryMaleModel
        maleBody!![1] = defs.secondaryMaleModel
        maleBody!![2] = defs.tertiaryMaleModel
        femaleBody!![0] = defs.primaryFemaleModel
        femaleBody!![1] = defs.secondaryFemaleModel
        femaleBody!![2] = defs.tertiaryFemaleModel

        for (i in maleBody!!.indices) {
            if (i >= maleBodies.size) continue
            maleBody!![i] = maleBodies[i]
            femaleBody!![i] = femaleBodies[i]
        }
        return this
    }

    fun addBodyModels(vararg bodies: Int): ItemMeshModifier {
        return addBodyModels(bodies, bodies)
    }

    fun addHeadModels(maleHeads: IntArray, femaleHeads: IntArray): ItemMeshModifier {
        maleHead = IntArray(2)
        femaleHead = IntArray(2)
        maleHead!![0] = defs.primaryMaleDialogueHead
        maleHead!![1] = defs.secondaryMaleDialogueHead
        femaleHead!![0] = defs.primaryFemaleDialogueHead
        femaleHead!![1] = defs.secondaryFemaleDialogueHead

        for (i in maleHead!!.indices) {
            if (i >= maleHeads.size) continue
            maleHead!![i] = maleHeads[i]
            femaleHead!![i] = femaleHeads[i]
        }
        return this
    }

    fun addHeadModels(vararg heads: Int): ItemMeshModifier {
        return addHeadModels(heads, heads)
    }

    fun addColors(vararg colors: Int): ItemMeshModifier {
        val mc = defs.modifiedColours
        if (mc != null) {
            modifiedColors = IntArray(mc.size) { mc[it].toInt() and 0xFFFF }
        }
        if (modifiedColors == null) return this
        for (i in modifiedColors!!.indices) {
            if (i >= colors.size) break
            modifiedColors!![i] = colors[i]
        }
        return this
    }

    fun addTextures(vararg textures: Int): ItemMeshModifier {
        val mt = defs.modifiedTextureColours
        if (mt != null) {
            modifiedTextures = IntArray(mt.size) { mt[it].toInt() and 0xFFFF }
        }
        if (modifiedTextures == null) return this
        for (i in modifiedTextures!!.indices) {
            if (i >= textures.size) break
            modifiedTextures!![i] = textures[i]
        }
        return this
    }

    /**
     * Converts this modifier into the core [ItemMeshOverride] schema.
     *
     * Which model/colour/texture values apply is decided here; how they are framed on the wire is
     * decided by the revision encoder in core.
     */
    fun toOverride(): ItemMeshOverride = ItemMeshOverride(
        slot = slotFlag,
        bodyModels = bodyModelOverride(),
        headModels = headModelOverride(),
        colours = modifiedColors,
        textures = modifiedTextures,
    )

    private fun bodyModelOverride(): BodyModelOverride? {
        val male = maleBody ?: return null
        val female = femaleBody ?: return null
        return BodyModelOverride(
            male = male,
            female = female,
            includeSecondary = defs.secondaryMaleModel != -1 || defs.secondaryFemaleModel != -1,
            includeTertiary = defs.tertiaryMaleModel != -1 || defs.tertiaryFemaleModel != -1,
        )
    }

    private fun headModelOverride(): HeadModelOverride? {
        val male = maleHead ?: return null
        val female = femaleHead ?: return null
        return HeadModelOverride(
            male = male,
            female = female,
            includeSecondary = defs.secondaryMaleDialogueHead != -1 || defs.secondaryFemaleDialogueHead != -1,
        )
    }

}
