package com.jagex.game.runetek5.toolkit.vertexdeclaration

class VertexDeclarationComponent {

    @JvmField var bitpacked: Long = 0
    @JvmField var size: Int = 0

    constructor(actions: Array<VertexDeclarationAction>) {
        for (index in actions.indices) {
            pack(actions[index])
        }
    }

    constructor(action: VertexDeclarationAction) {
        this.bitpacked = action.slot.toLong()
        this.size = 1
    }

    fun getSize(): Int {
        return this.size
    }

    fun getAction(i_1: Int): VertexDeclarationAction {
        return VertexDeclarationAction.forType(method1299(i_1))
    }

    fun method1299(i_1: Int): Int {
        return ((this.bitpacked shr (VertexDeclarationAction.anInt702 * i_1)) and 0xf).toInt()
    }

    fun pack(action: VertexDeclarationAction) {
        this.bitpacked = this.bitpacked or (action.slot.toLong() shl (VertexDeclarationAction.anInt702 * this.size++))
    }
}
