package jaggl

import com.jagex.game.runetek5.graphics.modern.dump.FrameDump
import com.jagex.game.runetek5.toolkit.context.GlxContext
import org.lwjgl.opengl.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import java.awt.Canvas
import java.util.Hashtable

class OpenGL {
    companion object {
        @JvmField
        val ASSOCIATED_THREADS: Hashtable<Thread, OpenGL> = Hashtable()

        private val legacyCalls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

        private fun <T> legacy(name: String, value: T): T {
            if (legacyCalls.add(name)) {
                println("[GL] legacy call $name ignored")
                Thread.dumpStack()
            }
            return value
        }

        private inline fun <R> inInts(a: IntArray, o: Int, n: Int, f: (Long) -> R): R {
            val b = memAlloc(n * 4)
            try {
                b.asIntBuffer().put(a, o, n)
                return f(memAddress(b))
            } finally {
                memFree(b)
            }
        }

        private inline fun <R> inFloats(a: FloatArray, o: Int, n: Int, f: (Long) -> R): R {
            val b = memAlloc(n * 4)
            try {
                b.asFloatBuffer().put(a, o, n)
                return f(memAddress(b))
            } finally {
                memFree(b)
            }
        }

        private inline fun <R> inBytes(a: ByteArray?, o: Int, n: Int, f: (Long) -> R): R {
            if (a == null) return f(0L)
            val b = memAlloc(n)
            try {
                b.put(a, o, n).flip()
                return f(memAddress(b))
            } finally {
                memFree(b)
            }
        }

        private inline fun outInts(a: IntArray, o: Int, n: Int, f: (Long) -> Unit) {
            val b = memAlloc(n * 4)
            try {
                f(memAddress(b))
                b.asIntBuffer().get(a, o, n)
            } finally {
                memFree(b)
            }
        }

        private inline fun outBytes(a: ByteArray, o: Int, n: Int, f: (Long) -> Unit) {
            val b = memAlloc(n)
            try {
                f(memAddress(b))
                b.get(a, o, n)
            } finally {
                memFree(b)
            }
        }

        private fun left(size: Int, o: Int, want: Long): Int = minOf((size - o).toLong(), want).toInt()

        @JvmStatic fun glEnable(capability: Int) = if (isCoreCapability(capability)) GL11C.glEnable(capability) else legacy("glEnable(0x${capability.toString(16)})", Unit)
        @JvmStatic fun glDisable(capability: Int) = if (isCoreCapability(capability)) GL11C.glDisable(capability) else legacy("glDisable(0x${capability.toString(16)})", Unit)
        @JvmStatic fun glCullFace(mode: Int) = GL11C.glCullFace(mode)
        @JvmStatic fun glHint(target: Int, hint: Int) = if (target == 0x0C54) legacy("glHint(GL_FOG_HINT)", Unit) else GL11C.glHint(target, hint)
        @JvmStatic fun glFinish() = GL11C.glFinish()
        @JvmStatic fun glFlush() = GL11C.glFlush()
        @JvmStatic fun glClear(mask: Int) {
            FrameDump.clear(mask)
            GL11C.glClear(mask)
        }
        @JvmStatic fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) = GL11C.glClearColor(red, green, blue, alpha)
        @JvmStatic fun glClearDepth(depth: Float) = GL11C.glClearDepth(depth.toDouble())
        @JvmStatic fun glDepthFunc(func: Int) = GL11C.glDepthFunc(func)
        @JvmStatic fun glDepthMask(flag: Boolean) = GL11C.glDepthMask(flag)
        @JvmStatic fun glDepthRange(near: Float, far: Float) = GL11C.glDepthRange(near.toDouble(), far.toDouble())
        @JvmStatic fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) = GL11C.glColorMask(red, green, blue, alpha)
        @JvmStatic fun glReadBuffer(mode: Int) = GL11C.glReadBuffer(mode)
        @JvmStatic fun glDrawBuffer(mode: Int) = GL11C.glDrawBuffer(mode)
        @JvmStatic fun glDrawBuffersARB(n: Int, buffers: IntArray, offset: Int) = inInts(buffers, offset, n) { GL20C.nglDrawBuffers(n, it) }
        @JvmStatic fun glViewport(x: Int, y: Int, width: Int, height: Int) = GL11C.glViewport(x, y, width, height)
        @JvmStatic fun glScissor(x: Int, y: Int, width: Int, height: Int) = GL11C.glScissor(x, y, width, height)
        @JvmStatic fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long) = GL20C.glVertexAttribPointer(index, size, type, normalized, stride, pointer)
        @JvmStatic fun glEnableVertexAttribArray(index: Int) = GL20C.glEnableVertexAttribArray(index)
        @JvmStatic fun glDisableVertexAttribArray(index: Int) = GL20C.glDisableVertexAttribArray(index)
        @JvmStatic fun glBindAttribLocation(program: Int, index: Int, name: String) = GL20C.glBindAttribLocation(program, index, name)
        @JvmStatic fun glGenBuffersARB(n: Int, buffers: IntArray, offset: Int) { for (i in 0 until n) buffers[offset + i] = GL15C.glGenBuffers() }
        @JvmStatic fun glBindBufferARB(target: Int, buffer: Int) = GL15C.glBindBuffer(target, buffer)
        @JvmStatic fun glBufferDataARBa(target: Int, size: Int, data: Long, usage: Int) = GL15C.nglBufferData(target, size.toLong(), data, usage)
        @JvmStatic fun glBufferDataARBub(target: Int, size: Int, data: ByteArray?, offset: Int, usage: Int) = inBytes(data, offset, size) { GL15C.nglBufferData(target, size.toLong(), it, usage) }
        @JvmStatic fun glBufferSubDataARBa(target: Int, offset: Int, size: Int, data: Long) = GL15C.nglBufferSubData(target, offset.toLong(), size.toLong(), data)
        @JvmStatic fun glBufferSubDataARBub(target: Int, offset: Int, size: Int, data: ByteArray, offset2: Int) = inBytes(data, offset2, size) { GL15C.nglBufferSubData(target, offset.toLong(), size.toLong(), it) }
        @JvmStatic fun glMapBufferARB(target: Int, access: Int): Long = GL15C.nglMapBuffer(target, access)
        @JvmStatic fun glUnmapBufferARB(bufferObjectID: Int): Boolean = GL15C.glUnmapBuffer(bufferObjectID)
        @JvmStatic fun glDeleteBuffersARB(n: Int, buffers: IntArray, offset: Int) { for (i in 0 until n) GL15C.glDeleteBuffers(buffers[offset + i]) }
        @JvmStatic fun glLineWidth(width: Float) = GL11C.glLineWidth(width)
        @JvmStatic fun glPointSize(size: Float) = GL11C.glPointSize(size)
        @JvmStatic fun glPolygonMode(face: Int, mode: Int) = if (face == GL11C.GL_FRONT_AND_BACK) GL11C.glPolygonMode(face, mode) else legacy("glPolygonMode(face 0x${face.toString(16)})", Unit)
        @JvmStatic fun glDrawArrays(mode: Int, first: Int, count: Int) {
            FrameDump.nativeDraw()
            GL11C.glDrawArrays(mode, first, count)
        }
        @JvmStatic fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long) {
            FrameDump.nativeDraw()
            GL11C.glDrawElements(mode, count, type, indices)
        }
        @JvmStatic fun glGenTextures(n: Int, textures: IntArray, offset: Int) { for (i in 0 until n) textures[offset + i] = GL11C.glGenTextures() }
        @JvmStatic fun glDeleteTextures(n: Int, textures: IntArray, offset: Int) { for (i in 0 until n) GL11C.glDeleteTextures(textures[offset + i]) }
        @JvmStatic fun glActiveTexture(texture: Int) = GL13C.glActiveTexture(texture)
        @JvmStatic fun glBindTexture(target: Int, texture: Int) = GL11C.glBindTexture(target, texture)
        @JvmStatic fun glTexParameterf(target: Int, pname: Int, param: Float) = GL11C.glTexParameterf(target, pname, param)
        @JvmStatic fun glTexParameteri(target: Int, pname: Int, param: Int) = GL11C.glTexParameteri(target, pname, param)
        @JvmStatic fun glGetTexImagei(target: Int, level: Int, format: Int, type: Int, pixels: IntArray, offset: Int) = outInts(pixels, offset, pixels.size - offset) { GL11C.nglGetTexImage(target, level, format, type, it) }
        @JvmStatic fun glGetTexImageub(target: Int, level: Int, format: Int, type: Int, pixels: ByteArray, offset: Int) = outBytes(pixels, offset, pixels.size - offset) { GL11C.nglGetTexImage(target, level, format, type, it) }
        @JvmStatic fun glGetTexImage(target: Int, level: Int, format: Int, type: Int, pixels: ByteArray) = outBytes(pixels, 0, pixels.size) { GL11C.nglGetTexImage(target, level, format, type, it) }
        @JvmStatic fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int): Float = GL11C.glGetTexLevelParameterf(target, level, pname)
        @JvmStatic fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int): Int = GL11C.glGetTexLevelParameteri(target, level, pname)
        @JvmStatic fun glTexImage1Dub(target: Int, level: Int, internalformat: Int, width: Int, border: Int, format: Int, type: Int, pixels: ByteArray, offset: Int) = inBytes(pixels, offset, left(pixels.size, offset, width * 16L)) { GL11C.nglTexImage1D(target, level, coreInternal(target, internalformat), width, border, coreFormat(format), type, it) }
        @JvmStatic fun glCopyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int) = GL11C.glCopyTexImage2D(target, level, coreInternal(target, internalFormat), x, y, width, height, border)
        @JvmStatic fun glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int) = GL11C.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
        @JvmStatic fun glTexImage2Di(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: IntArray, offset: Int) = inInts(pixels, offset, left(pixels.size, offset, width.toLong() * height)) { GL11C.nglTexImage2D(target, level, coreInternal(target, internalFormat), width, height, border, coreFormat(format), type, it) }
        @JvmStatic fun glTexImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, data: FloatArray, offset: Int) = inFloats(data, offset, left(data.size, offset, width.toLong() * height * 4)) { GL11C.nglTexImage2D(target, level, coreInternal(target, internalFormat), width, height, border, coreFormat(format), type, it) }
        @JvmStatic fun glTexImage2Dub(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, data: ByteArray?, offset: Int) = inBytes(data, offset, if (data == null) 0 else left(data.size, offset, width.toLong() * height * 16)) { GL11C.nglTexImage2D(target, level, coreInternal(target, internalFormat), width, height, border, coreFormat(format), type, it) }
        @JvmStatic fun glTexSubImage2Di(target: Int, level: Int, offsetX: Int, offsetY: Int, width: Int, height: Int, format: Int, type: Int, pixels: IntArray, pixelOffset: Int) = inInts(pixels, pixelOffset, left(pixels.size, pixelOffset, width.toLong() * height)) { GL11C.nglTexSubImage2D(target, level, offsetX, offsetY, width, height, coreFormat(format), type, it) }
        @JvmStatic fun glTexSubImage2Df(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, data: FloatArray, offset: Int) = inFloats(data, offset, left(data.size, offset, width.toLong() * height * 4)) { GL11C.nglTexSubImage2D(target, level, xoffset, yoffset, width, height, coreFormat(format), type, it) }
        @JvmStatic fun glTexSubImage2Dub(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray, offset: Int) = inBytes(data, offset, left(data.size, offset, width.toLong() * height * 16)) { GL11C.nglTexSubImage2D(target, level, xoffset, yoffset, width, height, coreFormat(format), type, it) }
        @JvmStatic fun glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int) = GL12C.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)
        @JvmStatic fun glTexImage3Dub(target: Int, level: Int, internal: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, type: Int, data: ByteArray?, offset: Int) = inBytes(data, offset, if (data == null) 0 else left(data.size, offset, width.toLong() * height * depth * 16)) { GL12C.nglTexImage3D(target, level, coreInternal(target, internal), width, height, depth, border, coreFormat(format), type, it) }
        @JvmStatic fun glCompressedTexImage2Dub(target: Int, level: Int, internal: Int, width: Int, height: Int, border: Int, imageSize: Int, data: ByteArray, offset: Int) = inBytes(data, offset, imageSize) { GL13C.nglCompressedTexImage2D(target, level, internal, width, height, border, imageSize, it) }
        @JvmStatic fun glReadPixelsi(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: IntArray, offset: Int) = outInts(pixels, offset, left(pixels.size, offset, width.toLong() * height)) { GL11C.nglReadPixels(x, y, width, height, format, type, it) }
        @JvmStatic fun glReadPixelsub(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: ByteArray, offset: Int) = outBytes(pixels, offset, left(pixels.size, offset, width.toLong() * height * 4)) { GL11C.nglReadPixels(x, y, width, height, format, type, it) }
        @JvmStatic fun glPixelStorei(parameter: Int, value: Int) = GL11C.glPixelStorei(parameter, value)
        @JvmStatic fun glBlendFunc(sfactor: Int, dfactor: Int) = GL11C.glBlendFunc(sfactor, dfactor)
        @JvmStatic fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) = GL14C.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
        @JvmStatic fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float) = GL14C.glBlendColor(red, green, blue, alpha)
        @JvmStatic fun glGenFramebuffersEXT(n: Int, framebuffers: IntArray, offset: Int) { for (i in 0 until n) framebuffers[offset + i] = GL30C.glGenFramebuffers() }
        @JvmStatic fun glDeleteFramebuffersEXT(n: Int, framebuffers: IntArray, offset: Int) { for (i in 0 until n) GL30C.glDeleteFramebuffers(framebuffers[offset + i]) }
        @JvmStatic fun glBindFramebufferEXT(target: Int, framebuffer: Int) = GL30C.glBindFramebuffer(target, framebuffer)
        @JvmStatic fun glFramebufferTexture2DEXT(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) = GL30C.glFramebufferTexture2D(target, attachment, textarget, texture, level)
        @JvmStatic fun glFramebufferTexture3DEXT(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int, zoffset: Int) = GL30C.glFramebufferTextureLayer(target, attachment, texture, level, zoffset)
        @JvmStatic fun glFramebufferRenderbufferEXT(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) = GL30C.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
        @JvmStatic fun glCheckFramebufferStatusEXT(target: Int): Int = GL30C.glCheckFramebufferStatus(target)
        @JvmStatic fun glBlitFramebufferEXT(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int) = FrameDump.blit(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)
        @JvmStatic fun glGenerateMipmapEXT(target: Int) = GL30C.glGenerateMipmap(target)
        @JvmStatic fun glGenRenderbuffersEXT(n: Int, renderbuffers: IntArray, offset: Int) { for (i in 0 until n) renderbuffers[offset + i] = GL30C.glGenRenderbuffers() }
        @JvmStatic fun glDeleteRenderbuffersEXT(n: Int, renderbuffers: IntArray, offset: Int) { for (i in 0 until n) GL30C.glDeleteRenderbuffers(renderbuffers[offset + i]) }
        @JvmStatic fun glBindRenderbufferEXT(target: Int, renderbuffer: Int) = GL30C.glBindRenderbuffer(target, renderbuffer)
        @JvmStatic fun glRenderbufferStorageEXT(target: Int, internalFormat: Int, width: Int, height: Int) = GL30C.glRenderbufferStorage(target, internalFormat, width, height)
        @JvmStatic fun glRenderbufferStorageMultisampleEXT(target: Int, samples: Int, internalFormat: Int, width: Int, height: Int) = GL30C.glRenderbufferStorageMultisample(target, samples, internalFormat, width, height)
        @JvmStatic fun glCreateProgram(): Int = GL20C.glCreateProgram()
        @JvmStatic fun glCreateShader(shaderType: Int): Int = GL20C.glCreateShader(shaderType)
        @JvmStatic fun glDeleteProgram(programHandle: Int) = GL20C.glDeleteProgram(programHandle)
        @JvmStatic fun glDeleteShader(shaderHandle: Int) = GL20C.glDeleteShader(shaderHandle)
        @JvmStatic fun glShaderSource(shaderHandle: Int, shaderSource: String) = GL20C.glShaderSource(shaderHandle, shaderSource)
        @JvmStatic fun glCompileShader(shaderHandle: Int) = GL20C.glCompileShader(shaderHandle)
        @JvmStatic fun glAttachShader(programHandle: Int, shaderHandle: Int) = GL20C.glAttachShader(programHandle, shaderHandle)
        @JvmStatic fun glDetachShader(programHandle: Int, shaderHandle: Int) = GL20C.glDetachShader(programHandle, shaderHandle)
        @JvmStatic fun glLinkProgram(programHandle: Int) = GL20C.glLinkProgram(programHandle)
        @JvmStatic fun glUseProgram(programHandle: Int) = GL20C.glUseProgram(programHandle)
        @JvmStatic fun glGetUniformLocation(programHandle: Int, uniformName: String): Int = GL20C.glGetUniformLocation(programHandle, uniformName)
        @JvmStatic fun glUniform1i(location: Int, value: Int) = GL20C.glUniform1i(location, value)
        @JvmStatic fun glUniform1f(location: Int, value: Float) = GL20C.glUniform1f(location, value)
        @JvmStatic fun glUniform2f(location: Int, value1: Float, value2: Float) = GL20C.glUniform2f(location, value1, value2)
        @JvmStatic fun glUniform3f(location: Int, value1: Float, value2: Float, value3: Float) = GL20C.glUniform3f(location, value1, value2, value3)
        @JvmStatic fun glUniform4f(location: Int, value1: Float, value2: Float, value3: Float, value4: Float) = GL20C.glUniform4f(location, value1, value2, value3, value4)
        @JvmStatic fun glUniform2fv(location: Int, count: Int, values: FloatArray, offset: Int) = inFloats(values, offset, count * 2) { GL20C.nglUniform2fv(location, count, it) }
        @JvmStatic fun glUniform3fv(location: Int, count: Int, values: FloatArray, offset: Int) = inFloats(values, offset, count * 3) { GL20C.nglUniform3fv(location, count, it) }
        @JvmStatic fun glUniform4fv(location: Int, count: Int, values: FloatArray, offset: Int) = inFloats(values, offset, count * 4) { GL20C.nglUniform4fv(location, count, it) }
        @JvmStatic fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, values: FloatArray, offset: Int) = inFloats(values, offset, count * 4) { GL20C.nglUniformMatrix2fv(location, count, transpose, it) }
        @JvmStatic fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, values: FloatArray, offset: Int) = inFloats(values, offset, count * 9) { GL20C.nglUniformMatrix3fv(location, count, transpose, it) }
        @JvmStatic fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, values: FloatArray, offset: Int) = inFloats(values, offset, count * 16) { GL20C.nglUniformMatrix4fv(location, count, transpose, it) }
        @JvmStatic fun glGetProgramiv(programHandle: Int, pname: Int, params: IntArray, offset: Int) { params[offset] = GL20C.glGetProgrami(programHandle, pname) }
        @JvmStatic fun glGetShaderiv(shaderHandle: Int, pname: Int, params: IntArray, offset: Int) { params[offset] = GL20C.glGetShaderi(shaderHandle, pname) }
        @JvmStatic fun glGetProgramInfoLog(programHandle: Int, maxLength: Int, length: IntArray, lengthOffset: Int, infoLog: ByteArray, infoLogOffset: Int) = copyLog(GL20C.glGetProgramInfoLog(programHandle, maxLength), maxLength, length, lengthOffset, infoLog, infoLogOffset)
        @JvmStatic fun glGetShaderInfoLog(shaderHandle: Int, maxLength: Int, length: IntArray, lengthOffset: Int, infoLog: ByteArray, infoLogOffset: Int) = copyLog(GL20C.glGetShaderInfoLog(shaderHandle, maxLength), maxLength, length, lengthOffset, infoLog, infoLogOffset)
        @JvmStatic fun glGetError(): Int = GL11C.glGetError()
        @JvmStatic fun glGetString(name: Int): String = if (name == GL11C.GL_EXTENSIONS) extensionString() else GL11C.glGetString(name) ?: ""
        @JvmStatic fun glGetFloatv(pname: Int, params: FloatArray, offset: Int) = MemoryStack.stackPush().use {
            val b = it.mallocFloat(16)
            GL11C.glGetFloatv(pname, b)
            b.get(params, offset, minOf(16, params.size - offset))
        }
        @JvmStatic fun glGetIntegerv(pname: Int, params: IntArray, offset: Int) = MemoryStack.stackPush().use {
            val b = it.mallocInt(16)
            GL11C.glGetIntegerv(pname, b)
            b.get(params, offset, minOf(16, params.size - offset))
        }
        @JvmStatic fun glStencilFunc(func: Int, ref: Int, mask: Int) = GL11C.glStencilFunc(func, ref, mask)
        @JvmStatic fun glStencilOp(fail: Int, zfail: Int, zpass: Int) = GL11C.glStencilOp(fail, zfail, zpass)

        private fun copyLog(log: String, maxLength: Int, length: IntArray, lengthOffset: Int, infoLog: ByteArray, infoLogOffset: Int) {
            val bytes = log.toByteArray()
            val n = minOf(bytes.size, maxLength, infoLog.size - infoLogOffset)
            System.arraycopy(bytes, 0, infoLog, infoLogOffset, n)
            length[lengthOffset] = n
        }

        private fun extensionString(): String {
            val count = GL11C.glGetInteger(GL30C.GL_NUM_EXTENSIONS)
            return (0 until count).joinToString(" ") { GL30C.glGetStringi(GL11C.GL_EXTENSIONS, it) ?: "" }
        }

        private const val GL_R8 = 0x8229
        private const val GL_RG8 = 0x822B
        private const val GL_RED = 0x1903
        private const val GL_GREEN = 0x1904
        private const val GL_RG = 0x8227

        private class Swizzle(val internal: Int, val rgba: IntArray)

        private val LEGACY_INTERNAL: Map<Int, Swizzle> = mapOf(
            0x1906 to Swizzle(GL_R8, intArrayOf(0, 0, 0, GL_RED)),
            0x803C to Swizzle(GL_R8, intArrayOf(0, 0, 0, GL_RED)),
            0x1909 to Swizzle(GL_R8, intArrayOf(GL_RED, GL_RED, GL_RED, 1)),
            0x8040 to Swizzle(GL_R8, intArrayOf(GL_RED, GL_RED, GL_RED, 1)),
            0x190A to Swizzle(GL_RG8, intArrayOf(GL_RED, GL_RED, GL_RED, GL_GREEN)),
            0x8045 to Swizzle(GL_RG8, intArrayOf(GL_RED, GL_RED, GL_RED, GL_GREEN)),
            0x8049 to Swizzle(GL_R8, intArrayOf(GL_RED, GL_RED, GL_RED, GL_RED)),
            0x804B to Swizzle(GL_R8, intArrayOf(GL_RED, GL_RED, GL_RED, GL_RED))
        )

        private fun coreInternal(target: Int, internal: Int): Int {
            val swizzle = LEGACY_INTERNAL[internal] ?: return internal
            for (channel in 0 until 4) {
                GL11C.glTexParameteri(target, 0x8E42 + channel, swizzle.rgba[channel])
            }
            return swizzle.internal
        }

        private fun coreFormat(format: Int): Int = when (format) {
            0x1906, 0x1909, 0x8049 -> GL_RED
            0x190A -> GL_RG
            else -> format
        }

        private val CORE_CAPABILITIES = setOf(
            0x0BE2, 0x0B44, 0x0B71, 0x0BD0, 0x0B20, 0x0B41, 0x0C11, 0x0B90, 0x0BF2, 0x809D, 0x809E, 0x809F, 0x80A0,
            0x8037, 0x2A02, 0x2A01, 0x8DB9, 0x8642, 0x92E0, 0x8242, 0x884F, 0x8F9D, 0x864F,
            0x3000, 0x3001, 0x3002, 0x3003, 0x3004, 0x3005, 0x3006, 0x3007
        )

        private fun isCoreCapability(capability: Int) = capability in CORE_CAPABILITIES

        private val CORE_PROMOTED = setOf(
            "GL_ARB_multitexture", "GL_ARB_vertex_buffer_object", "GL_ARB_multisample", "GL_ARB_vertex_shader",
            "GL_ARB_fragment_shader", "GL_EXT_texture3D", "GL_ARB_texture_rectangle", "GL_ARB_texture_cube_map",
            "GL_ARB_texture_float", "GL_EXT_framebuffer_object", "GL_EXT_framebuffer_blit", "GL_EXT_framebuffer_multisample",
            "GL_ARB_half_float_pixel"
        )

    }

    @JvmField
    var peer: Long = 0

    @JvmField
    var supportedExtensions: Hashtable<String, String>? = null

    @JvmField
    var threadPeer: Thread? = null

    private val context = GlxContext()
    private var drawable = 0L
    private var capabilities: GLCapabilities? = null

    fun supportsExtension(extensionName: String): Boolean {
        if (supportedExtensions == null) {
            supportedExtensions = Hashtable()
            glGetString(GL11C.GL_EXTENSIONS).split(' ').filter { it.isNotEmpty() }.forEach { supportedExtensions!![it] = it }
        }
        return extensionName in CORE_PROMOTED || supportedExtensions!!.containsKey(extensionName)
    }

    @Synchronized
    fun attach(): Boolean {
        val thread = Thread.currentThread()
        if (attachPeer()) {
            val released = ASSOCIATED_THREADS.put(thread, this)
            if (released != null) {
                released.threadPeer = null
            }
            threadPeer = thread
            return true
        }
        return false
    }

    @Synchronized
    fun detach(): Boolean {
        if (threadPeer != Thread.currentThread()) {
            return false
        }
        detachPeer()
        ASSOCIATED_THREADS.remove(threadPeer)
        threadPeer = null
        return true
    }

    fun init(canvas: Canvas, redSize: Int, greenSize: Int, blueSize: Int, alphaSize: Int, depthSize: Int, stencilSize: Int): Long {
        if (GL.getFunctionProvider() == null) {
            GL.create()
        }
        val debug = System.getProperty("render.gldebug") != null
        val version = System.getProperty("render.glversion")?.split('.')?.map { it.toInt() } ?: listOf(4, 3)
        val window = context.create(canvas, redSize, greenSize, blueSize, depthSize, version[0], version[1], debug)
        if (window == 0L) {
            return 0L
        }
        peer = window
        drawable = window
        if (!attach()) {
            return 0L
        }
        capabilities = GL.createCapabilities()
        GL30C.glBindVertexArray(GL30C.glGenVertexArrays())
        println("[GL] ${GL11C.glGetString(GL11C.GL_VERSION)} | profile ${if (GL11C.glGetInteger(GL32C.GL_CONTEXT_PROFILE_MASK) and GL32C.GL_CONTEXT_CORE_PROFILE_BIT != 0) "core" else "compatibility"} forward-compatible | ${GL11C.glGetString(GL11C.GL_RENDERER)} | ${context.javaClass.simpleName}")
        return window
    }

    fun arePbuffersAvailable(): Boolean = false

    fun attachPeer(): Boolean {
        if (!context.makeCurrent(drawable)) {
            return false
        }
        capabilities?.let {
            GL.setCapabilities(it)
        }
        return true
    }

    fun detachPeer() {
        context.releaseCurrent()
        GL.setCapabilities(null)
    }

    fun prepareSurface(canvas: Canvas): Long = context.surface(canvas)

    fun releaseSurface(canvas: Canvas, surfaceHandle: Long) {}

    fun setSurface(surfaceHandle: Long): Boolean {
        drawable = surfaceHandle
        return context.makeCurrent(surfaceHandle)
    }

    fun swapBuffers(surfaceHandle: Long) = context.swap(surfaceHandle)

    fun setSwapInterval(interval: Int) = context.swapInterval(interval)

    fun release() {
        detachPeer()
        context.destroy()
        peer = 0
    }
}
