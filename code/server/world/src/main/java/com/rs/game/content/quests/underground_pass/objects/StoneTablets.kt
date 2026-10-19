package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val FIRST_TEXT_COMPONENT = Utils.componentIdFromHash(Component.interface_221.component_221_2)
private const val TEXT_COMPONENT_COUNT = 12

private val STONE_TABLET_TRANSCRIPTS = mapOf(
    Loc.stone_tablet1_upass to listOf(
        "All those who thirst for knowledge,",
        "Bow down to the lord.",
        "All you that crave eternal life,",
        "Come and meet your god.",
        "",
        "For no man nor beast can cast a spell",
        "against the wake of eternal hell."
    ),
    Loc.stone_tablet2_upass to listOf(
        "Most men do live in fear of death",
        "that it might steal their soul.",
        "Some work and pray, to shield their life",
        "from the ravages of the cold.",
        "",
        "But only those who embrace the end",
        "can truly make their life extend.",
        "",
        "And when all hope begins to fade",
        "look above and use nature as your aid."
    ),
    Loc.stone_tablet3_upass to listOf(
        "And now our god has given us",
        "one who is from our own.",
        "A saviour who once sat upon",
        "his father's glorious throne.",
        "",
        "It is in your name that we will lead the attack,",
        "Iban, Son of Zamorak!"
    ),
    Loc.stone_tablet4_upass to listOf(
        "Here lies the sacred well,",
        "entrance to Iban's hell.",
        "He blesses all his disciples keen.",
        "",
        "'We bathe in pure evil' they yell.",
        "The force of darkness is strong,",
        "the wait for morning forever long.",
        "",
        "If a light should break the night",
        "the dark will rise to win the fight."
    ),
    Loc.stone_tablet5_upass to listOf(
        "Leave this battered corpse be,",
        "for now he lives as spirit alone.",
        "Turn and leave, run and flee",
        "before the soulless writhe and moan.",
        "",
        "It is the soil that shall rise",
        "to turn away the mites and flies.",
        "",
        "Only as flesh becomes ash,",
        "and wood becomes dust,",
        "...will Iban's corpse embrace",
        "nature's eternal lust."
    )
)

@ServerStartupEvent
fun mapUndergroundPassStoneTablets() {
    onObjectClick(*STONE_TABLET_TRANSCRIPTS.keys.toTypedArray()) { (player, obj) ->
        val transcript = STONE_TABLET_TRANSCRIPTS.getValue(obj.id)
        player.interfaceManager.sendInterface(Interface.interface_221)
        repeat(TEXT_COMPONENT_COUNT) { line ->
            player.packets.setIFText(
                Interface.interface_221,
                FIRST_TEXT_COMPONENT + line,
                transcript.getOrElse(line) { "" }
            )
        }
    }
}
