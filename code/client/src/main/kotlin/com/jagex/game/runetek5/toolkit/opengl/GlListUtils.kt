package com.jagex.game.runetek5.toolkit.opengl

import jaggl.OpenGL
import com.jagex.game.runetek5.toolkit.opengl.*
/**
 * @author Brainpower 04/03/2023
 */
class GlListUtils(range: Int) {

    @JvmField
    val reservedRange: Int = OpenGL.glGenLists(range)

    fun callList(offset: Int) {
        OpenGL.glCallList(reservedRange + offset)
    }

    fun newList(offset: Int) {
        OpenGL.glNewList(reservedRange + offset, GL_COMPILE)
    }

    fun endList() {
        OpenGL.glEndList()
    }
}
