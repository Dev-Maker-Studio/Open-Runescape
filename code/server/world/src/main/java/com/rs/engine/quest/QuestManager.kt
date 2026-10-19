package com.rs.engine.quest

import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.cache.IComponentDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.AttribMap
import org.darkan.core.model.IFEvents

class QuestManager {
    @Transient
    var player: Player? = null
        set(value) {
            field = value
            if (MAX_QUESTPOINTS == 0)
                for (quest in Quest.entries)
                    if (quest.isImplemented)
                        MAX_QUESTPOINTS += quest.defs.questpointReward
        }
    private var sort = 0
    private var filter = false
    private var hideDone = false
    private val questStages: MutableMap<Int, Int> = HashMap()
    private val questAttribs: MutableMap<Int, AttribMap> = HashMap()

    private val owner: Player get() = player!!

    fun showQuestDetailInterface(quest: Quest?) {
        quest ?: return
        if (!owner.isQuestStarted(quest) || owner.isQuestComplete(quest)) {
            quest.openQuestInfo(owner, false)
            return
        }
        val handler = quest.handler ?: return
        val fontId = IComponentDefinitions.getInterfaceComponent(Interface.interface_275, component(Component.interface_275.component_275_10))!!.fontId
        val stage = getStage(quest)
        val journalLines = ArrayList<String>()
        for (i in 0 until stage)
            for (line in handler.getJournalLines(owner, i))
                journalLines.addAll(wrapJournalLine("<str>$line", fontId))
        for (line in handler.getJournalLines(owner, stage))
            journalLines.addAll(wrapJournalLine(line, fontId))
        owner.interfaceManager.sendInterface(Interface.interface_275)
        owner.packets.sendRunScriptReverse(Cs2.cs2_1207, journalLines.size)
        owner.packets.setIFText(Interface.interface_275, component(Component.interface_275.component_275_1), quest.defs.extraInfo.name)
        val firstLine = component(Component.interface_275.component_275_10)
        for (i in firstLine until firstLine + 279)
            owner.packets.setIFText(Interface.interface_275, i, journalLines.getOrNull(i - firstLine) ?: " ")
    }

    fun unlockQuestTabOptions() {
        owner.packets.setIFRightClickOps(Interface.interface_190, component(Component.interface_190.component_190_15), 0, 201, 0, 1, 2, 3)
        owner.packets.setIFEvents(IFEvents(Interface.interface_190, component(Component.interface_190.component_190_38), 0, 3).enableRightClickOption(0))
        owner.packets.sendVarc(Varc.varc_234, 0)
        updateOptions()
    }

    fun getStage(quest: Quest): Int = questStages[quest.id] ?: 0

    fun setStage(quest: Quest, stage: Int) {
        if (!quest.isImplemented)
            return
        questStages[quest.id] = stage
        sendQuestStage(quest)
        owner.schedule {
            wait(1)
            owner.packets.sendRunScript(Cs2.ql4_refresh)
        }
    }

    fun completeQuest(quest: Quest) {
        val handler = quest.handler ?: return
        if (isComplete(quest))
            return
        setStage(quest, handler.completedStage)
        questAttribs.remove(quest.id)
        handler.complete(owner)
        sendQuestStage(quest)
        sendQuestPoints()
    }

    fun resetQuest(quest: Quest) {
        if (!quest.isImplemented)
            return
        questAttribs.remove(quest.id)
        setStage(quest, 0)
    }

    fun getAttribs(quest: Quest): AttribMap = questAttribs.getOrPut(quest.id) { AttribMap() }

    fun completedAllQuests(): Boolean = Quest.entries.none { it.isImplemented && !it.isHoliday && !isComplete(it) }

    fun isComplete(quest: Quest, actionForUnimplemented: String?): Boolean {
        val handler = quest.handler ?: return quest.meetsReqs(owner, actionForUnimplemented)
        if (getStage(quest) == handler.completedStage)
            return true
        if (actionForUnimplemented != null)
            owner.sendMessage("You must have completed " + quest.defs.name + " " + actionForUnimplemented)
        return false
    }

    fun isComplete(quest: Quest): Boolean = isComplete(quest, null)

    val questPoints: Int
        get() = Quest.entries.filter { it.isImplemented && isComplete(it) }.sumOf { it.defs.questpointReward }

    fun sendQuestPoints() {
        owner.vars.setVar(Varp.qp, questPoints)
        owner.vars.setVar(Varp.varp_904, MAX_QUESTPOINTS)
    }

    fun updateAllQuestStages() {
        for (quest in Quest.entries)
            sendQuestStage(quest)
    }

    fun sendQuestStage(quest: Quest) {
        when {
            !quest.isImplemented || isComplete(quest) -> quest.defs.sendCompleted(owner)
            getStage(quest) > 0 -> quest.defs.sendStarted(owner)
            else -> quest.defs.sendNotStarted(owner)
        }
        quest.handler?.updateStage(owner, getStage(quest))
    }

    fun updateOptions() {
        owner.vars.setVarBit(Varbit.ql4_perm_sort, sort)
        owner.vars.setVarBit(Varbit.ql4_perm_filter, if (filter) 0 else 1)
        owner.vars.setVarBit(Varbit.ql4_perm_donefilter, if (hideDone) 0 else 1)
        SeasonalQuests.sendVars(owner)
        owner.vars.syncVarsToClient()
        owner.packets.sendRunScript(Cs2.ql4_refresh)
    }

    fun setSort(sort: Int) {
        this.sort = sort
        updateOptions()
    }

    fun toggleFilter() {
        filter = !filter
        updateOptions()
    }

    fun toggleHideDone() {
        hideDone = !hideDone
        updateOptions()
    }

    companion object {
        @JvmField
        var MAX_QUESTPOINTS = 0
    }
}

@ServerStartupEvent
fun mapQuestTab() {
    onButtonClick(Interface.interface_190) { e ->
        val quests = e.player.questManager
        when (e.component) {
            Component.interface_190.component_190_38 -> quests.setSort(e.slotId)
            Component.interface_190.component_190_3 -> quests.toggleFilter()
            Component.interface_190.component_190_7 -> quests.toggleHideDone()
            Component.interface_190.component_190_15 -> quests.showQuestDetailInterface(Quest.forSlot(e.slotId))
        }
    }
}

private fun component(hash: Int) = Utils.componentIdFromHash(hash)

private val JOURNAL_TAG = Regex("<(/?)(str|u|col)(=[^>]*)?>")

private fun wrapJournalLine(line: String, fontId: Int): List<String> {
    var carried = ""
    return Utils.splitTextIntoLines(line, fontId, 380).map { subLine ->
        val text = carried + subLine
        carried = openJournalTags(text)
        text
    }
}

private fun openJournalTags(text: String): String {
    val open = LinkedHashMap<String, String>()
    for (tag in JOURNAL_TAG.findAll(text)) {
        val name = tag.groupValues[2]
        if (tag.groupValues[1] == "/") open.remove(name) else open[name] = tag.value
    }
    return open.values.joinToString("")
}
