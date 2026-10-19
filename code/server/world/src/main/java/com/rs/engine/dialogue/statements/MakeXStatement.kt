package com.rs.engine.dialogue.statements

import com.rs.engine.dialogue.impl.MakeXItem
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varcstr

private const val MAKE_QUESTION = "How many would you like to make?"
private const val SELECT_QUESTION = "Select an item."
private const val MAKE_X_OP = 5

private val ITEM_VARCS = intArrayOf(
    Varc.varc_755, Varc.varc_756, Varc.varc_757, Varc.varc_758, Varc.varc_759,
    Varc.varc_760, Varc.varc_1139, Varc.varc_1140, Varc.varc_1141, Varc.varc_1142,
)
private val NAME_VARCSTRS = intArrayOf(
    Varcstr.varcstr_132, Varcstr.varcstr_133, Varcstr.varcstr_134, Varcstr.varcstr_135, Varcstr.varcstr_136,
    Varcstr.varcstr_137, Varcstr.varcstr_280, Varcstr.varcstr_281, Varcstr.varcstr_282, Varcstr.varcstr_283,
)
private val SLOTS = intArrayOf(
    Component.interface_905.component_905_14, Component.interface_905.component_905_15, Component.interface_905.component_905_16,
    Component.interface_905.component_905_17, Component.interface_905.component_905_18, Component.interface_905.component_905_19,
    Component.interface_905.component_905_20, Component.interface_905.component_905_26, Component.interface_905.component_905_21,
    Component.interface_905.component_905_22, Component.interface_905.component_905_23, Component.interface_905.component_905_24,
    Component.interface_905.component_905_25, Component.interface_905.component_905_27,
)

class MakeXStatement @JvmOverloads constructor(
    private val type: MakeXType,
    private val maxQuantity: Int,
    private val question: String,
    private val items: IntArray,
    private val options: Array<String>? = null,
) : Statement {

    enum class MakeXType {
        MAKE,
        MAKE_SET,
        COOK,
        ROAST,
        OFFER,
        SELL,
        BAKE,
        CUT,
        CRAFT,
        DEPOSIT,
        MAKE_INTERVAL,
        TELEPORT,
        SELECT,
        MAKE_SET_INTERVAL,
        TAKE,
        RETURN,
        HEAT,
        ADD
    }

    constructor(items: Array<MakeXItem>, maxQuantity: Int) : this(MakeXType.MAKE, maxQuantity, MAKE_QUESTION, items.map { it.itemId }.toIntArray())
    constructor(type: MakeXType, question: String, items: IntArray, maxQuantity: Int) : this(type, maxQuantity, question, items)
    constructor(type: MakeXType, items: IntArray, maxQuantity: Int) : this(type, maxQuantity, MAKE_QUESTION, items)
    constructor(type: MakeXType, items: IntArray, options: Array<String>?) : this(type, -50, SELECT_QUESTION, items, options)
    constructor(type: MakeXType, items: IntArray) : this(type, -50, SELECT_QUESTION, items)
    constructor(question: String, items: IntArray, maxQuantity: Int) : this(MakeXType.MAKE, maxQuantity, question, items)
    constructor(items: IntArray, maxQuantity: Int) : this(MakeXType.MAKE, maxQuantity, MAKE_QUESTION, items)
    constructor(items: IntArray, options: Array<String>?) : this(MakeXType.SELECT, -50, SELECT_QUESTION, items, options)
    constructor(items: IntArray) : this(MakeXType.SELECT, -50, SELECT_QUESTION, items)

    override fun send(player: Player) {
        player.packets.setIFRightClickOps(Interface.interface_916, Utils.componentIdFromHash(Component.interface_916.component_916_8), -1, 0, 0)
        player.packets.setIFText(Component.interface_916.component_916_6, question)
        player.packets.sendVarc(Varc.varc_754, type.ordinal)
        ITEM_VARCS.forEachIndexed { slot, varc ->
            if (slot >= items.size) {
                player.packets.sendVarc(varc, -1)
                return@forEachIndexed
            }
            player.packets.sendVarc(varc, items[slot])
            player.packets.sendVarcString(NAME_VARCSTRS[slot], options?.get(slot) ?: ItemDefinitions.getDefs(items[slot]).name)
        }
        setMaxQuantity(player, maxQuantity)
        setQuantity(player, maxQuantity)
        player.interfaceManager.sendChatBoxInterface(Interface.interface_905)
        player.interfaceManager.sendSubSpecific(true, Interface.interface_905, Utils.componentIdFromHash(Component.interface_905.component_905_4), Interface.interface_916)
    }

    internal fun makeX(player: Player, component: Int) {
        val slot = SLOTS.indexOf(component)
        if (slot !in items.indices || type == MakeXType.SELECT) return
        val conversation = player.conversation ?: return
        player.sendInputInteger("Enter amount:") { amount ->
            if (amount <= 0 || player.conversation !== conversation || conversation.current?.statement !== this) return@sendInputInteger
            setQuantity(player, amount)
            conversation.process(slot)
        }
    }

    override fun getOptionId(componentId: Int): Int = SLOTS.indexOfFirst { Utils.componentIdFromHash(it) == componentId }.coerceAtLeast(0)

    override fun close(player: Player) {}

    companion object {
        @JvmStatic
        fun setMaxQuantity(player: Player, maxQuantity: Int) {
            player.vars.setVarBit(Varbit.varbit_8094, maxQuantity)
        }

        @JvmStatic
        @JvmOverloads
        fun setQuantity(player: Player, quantity: Int, refresh: Boolean = true) {
            val maxQuantity = getMaxQuantity(player)
            val clamped = if (quantity > maxQuantity) maxQuantity else quantity.coerceAtLeast(0)
            if (refresh)
                player.vars.setVarBit(Varbit.varbit_8095, clamped)
        }

        @JvmStatic
        fun getMaxQuantity(player: Player): Int = player.vars.getVarBit(Varbit.varbit_8094)

        @JvmStatic
        fun getQuantity(player: Player): Int {
            if (player.vars.getVarBit(Varbit.varbit_8094) <= 0)
                return 60
            return player.vars.getVarBit(Varbit.varbit_8095)
        }
    }
}

@ServerStartupEvent
fun mapMakeXButtons() {
    onButtonClick(Interface.interface_916) { e ->
        val player = e.player
        when (e.component) {
            Component.interface_916.component_916_10 -> MakeXStatement.setQuantity(player, 1)
            Component.interface_916.component_916_11 -> MakeXStatement.setQuantity(player, 5)
            Component.interface_916.component_916_12 -> MakeXStatement.setQuantity(player, 10)
            Component.interface_916.component_916_8 -> MakeXStatement.setQuantity(player, MakeXStatement.getMaxQuantity(player))
            Component.interface_916.component_916_24 -> MakeXStatement.setQuantity(player, MakeXStatement.getQuantity(player) + 1)
            Component.interface_916.component_916_25 -> MakeXStatement.setQuantity(player, MakeXStatement.getQuantity(player) - 1)
        }
    }
    onButtonClick(Interface.interface_905) { e ->
        if (e.opNum != MAKE_X_OP) return@onButtonClick
        val statement = e.player.conversation?.current?.statement as? MakeXStatement ?: return@onButtonClick
        statement.makeX(e.player, e.component)
    }
}
