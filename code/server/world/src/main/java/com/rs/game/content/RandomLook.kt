@file:JvmName("RandomLook")

package com.rs.game.content

import com.rs.game.model.entity.player.Player
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.cache.StructDefinitions
import org.darkan.core.gameval.gen.Enum
import org.darkan.core.gameval.gen.Param
import kotlin.random.Random

private const val DESIGN_VARIANTS = 6

fun randomizeLook(player: Player) {
    val male = Random.nextBoolean()
    if (male != player.appearance.isMale) PlayerLook.setGender(player, male)
    PlayerLook.setDesign(player, Random.nextInt(EnumDefinitions.getEnum(Enum.enum_3278).getSize()), Random.nextInt(DESIGN_VARIANTS))
    PlayerLook.setSkin(player, Random.nextInt(EnumDefinitions.getEnum(Enum.player_kit_skin_index_to_basecolour).getSize()))
    val hairStyles = EnumDefinitions.getEnum(if (male) Enum.enum_3304 else Enum.enum_3302)
    hairStyles.getIntValue(Random.nextInt(hairStyles.getSize())).takeIf { it != 0 }?.let { struct ->
        player.appearance.setHairStyle(StructDefinitions.getStruct(struct).getIntValue(Param.hair_default))
        if (!male) player.appearance.beardStyle = player.appearance.hairStyle
    }
    val hairColours = EnumDefinitions.getEnum(Enum.player_kit_hair_index_to_basecolour)
    player.appearance.hairColor = hairColours.getIntValue(Random.nextInt(hairColours.getSize()))
    player.appearance.generateAppearanceData()
}
