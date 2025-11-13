package com.jagex.game.runetek5.toolkit.vertexdeclaration

import com.jagex.game.runetek5.toolkit.companion.Binding

abstract class VertexDeclaration protected constructor(
    @JvmField val vertexDeclarationComponents: Array<VertexDeclarationComponent>
) : Binding
