package com.jagex.game.runetek5.toolkit.vertexdeclaration;

import com.jagex.game.runetek5.toolkit.companion.Binding;

public abstract class VertexDeclaration implements Binding {

    public final VertexDeclarationComponent[] vertexDeclarationComponents;

    protected VertexDeclaration(VertexDeclarationComponent[] arr_1) {
        this.vertexDeclarationComponents = arr_1;
    }

}
