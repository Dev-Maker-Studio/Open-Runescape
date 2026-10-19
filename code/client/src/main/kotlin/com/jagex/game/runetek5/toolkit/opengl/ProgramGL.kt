package com.jagex.game.runetek5.toolkit.opengl

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GL_VERTEX_PROGRAM_ARB
import jaggl.OpenGL

class ProgramGL(
    @JvmField val toolkit: OpenGlToolkit,
    @JvmField val programHandle: Int
) {

    @Suppress("removal")
    @Throws(Throwable::class)
    protected fun finalize() {
        this.toolkit.requestArbProgramCleanup(this.programHandle)
    }

    companion object {
        @JvmField val HANDLE: IntArray = IntArray(1)

        @JvmStatic
        fun generateProgram(toolkit: OpenGlToolkit, programCode: String): ProgramGL? {
            val programLocation = OpenGL.glGenProgramARB()
            OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, programLocation)
            OpenGL.glProgramStringARB(GL_VERTEX_PROGRAM_ARB, 34933, programCode)
            OpenGL.glGetIntegerv(34379, HANDLE, 0)
            return if (HANDLE[0] != -1) {
                OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0)
                null
            } else {
                OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0)
                ProgramGL(toolkit, programLocation)
            }
        }
    }
}
