package com.jagex.game.runetek5.toolkit.vertexdeclaration;

public class VertexDeclarationComponent {

    public long bitpacked;
    public int size;

    public VertexDeclarationComponent(VertexDeclarationAction[] actions) {
        for (int index = 0; index < actions.length; index++) {
            pack(actions[index]);
        }
    }

    public VertexDeclarationComponent(VertexDeclarationAction action) {
        this.bitpacked = action.slot;
        this.size = 1;
    }

    public int getSize() {
        return this.size;
    }

    public VertexDeclarationAction getAction(int i_1) {
        return VertexDeclarationAction.forType(method1299(i_1));
    }

    public int method1299(int i_1) {
        return (int) (this.bitpacked >> VertexDeclarationAction.anInt702 * i_1) & 0xf;
    }

    public void pack(VertexDeclarationAction action) {
        this.bitpacked |= (long) action.slot << VertexDeclarationAction.anInt702 * this.size++;
    }
}
