package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.quest.Quest
import com.rs.game.content.holidayevents.halloween.hw09.Halloween2009
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp

enum class Hw10Stage(val value: Int) {
    NOT_STARTED(0),
    STARTED(1),
    PREPARING(2),
    PARTY(10),
    COMPLETE(20);

    companion object {
        fun of(value: Int) = entries.lastOrNull { it.value <= value } ?: NOT_STARTED
    }
}

enum class GrimState { NOT_ASKED, AUTOGRAPH_REQUESTED, AUTOGRAPH_DELIVERED }

enum class WebState { NOT_ASKED, SEARCHING, PUZZLE, SOLVED }

enum class WerewolfState { NOT_ASKED, MOONSHINE_REQUESTED, MOONSHINE_DELIVERED }

enum class SpiderPosition { HIDING, OFF_TILE, ON_TILE }

enum class Hw10Task { MOONSHINE, AUTOGRAPH, SPIDERS }

var Player.hw10Stage: Hw10Stage
    get() = Hw10Stage.of(questManager.getStage(Quest.HALLOWEEN_2010))
    set(stage) = questManager.setStage(Quest.HALLOWEEN_2010, stage.value)

var Player.grimState: GrimState
    get() = GrimState.entries[vars.getVarBit(Varbit.hw10_grim).coerceAtMost(GrimState.entries.lastIndex)]
    set(state) = vars.saveVarBit(Varbit.hw10_grim, state.ordinal)

var Player.webState: WebState
    get() = WebState.entries[vars.getVarBit(Varbit.hw10_djweb)]
    set(state) = vars.saveVarBit(Varbit.hw10_djweb, state.ordinal)

var Player.werewolfState: WerewolfState
    get() = WerewolfState.entries[vars.getVarBit(Varbit.hw10_werewolf).coerceAtMost(WerewolfState.entries.lastIndex)]
    set(state) = vars.saveVarBit(Varbit.hw10_werewolf, state.ordinal)

var Player.zabethSigned: Boolean
    get() = vars.getVarBit(Varbit.hw10_zabeth) == 1
    set(signed) = vars.saveVarBit(Varbit.hw10_zabeth, if (signed) 1 else 0)

var Player.edgarSceneWatched: Boolean
    get() = vars.getVarBit(Varbit.hw10_edgar_check) == 1
    set(watched) = vars.saveVarBit(Varbit.hw10_edgar_check, if (watched) 1 else 0)

var Player.moonshineIngredientsTaken: Boolean
    get() = vars.getVarBit(Varbit.hw10_moonshine_path) == 1
    set(taken) = vars.saveVarBit(Varbit.hw10_moonshine_path, if (taken) 1 else 0)

fun Player.isDone(task: Hw10Task) = when (task) {
    Hw10Task.MOONSHINE -> werewolfState == WerewolfState.MOONSHINE_DELIVERED
    Hw10Task.AUTOGRAPH -> grimState == GrimState.AUTOGRAPH_DELIVERED
    Hw10Task.SPIDERS -> webState == WebState.SOLVED
}

val Player.hw10TasksDone get() = Hw10Task.entries.all { isDone(it) }

fun Player.finishesHw10Tasks(task: Hw10Task) = Hw10Task.entries.all { it == task || isDone(it) }

enum class EekState { CARRYING, OWNED, NEVER_MET }

val Player.hasEek get() = equipment.weaponId == Obj.hw09_eek_the_spider || inventory.containsItem(Obj.hw09_eek_the_spider)

val Player.eekState: EekState
    get() = when {
        hasEek -> EekState.CARRYING
        getI(Halloween2009.STAGE_KEY, 0) >= 10 || Obj.hw09_eek_the_spider in diangoReclaim -> EekState.OWNED
        else -> EekState.NEVER_MET
    }

val Player.did2009 get() = eekState != EekState.NEVER_MET

fun DialogueBuilder.emoteLearned(name: String) = item(Obj.hw06_pumpkin_head, "You've just learned the <col=ff0000>$name</col> emote!")

fun Player.resetHw10() {
    vanishHw10Items()
    vars.saveVar(Varp.hw10_var, 0)
    questManager.resetQuest(Quest.HALLOWEEN_2010)
    emotesManager.refreshListConfigs()
}
