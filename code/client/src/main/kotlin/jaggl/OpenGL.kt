/* OpenGL - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaggl

import java.awt.Canvas
import java.util.Hashtable

class OpenGL {
    companion object {
        @JvmField
        val ASSOCIATED_THREADS: Hashtable<Thread, OpenGL> = Hashtable()

        @JvmStatic
        external fun glEnable(capability: Int)

        @JvmStatic
        external fun glDisable(capability: Int)

        @JvmStatic
        external fun glCullFace(mode: Int)

        @JvmStatic
        external fun glPushAttrib(mask: Int)

        @JvmStatic
        external fun glPopAttrib()

        @JvmStatic
        external fun glShadeModel(mode: Int)

        @JvmStatic
        external fun glHint(target: Int, hint: Int)

        @JvmStatic
        external fun glFinish()

        @JvmStatic
        external fun glFlush()

        @JvmStatic
        external fun glClear(mask: Int)

        @JvmStatic
        external fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)

        @JvmStatic
        external fun glClearDepth(depth: Float)

        @JvmStatic
        external fun glDepthFunc(func: Int)

        @JvmStatic
        external fun glDepthMask(flag: Boolean)

        @JvmStatic
        external fun glDepthRange(near: Float, far: Float)

        @JvmStatic
        external fun glAlphaFunc(func: Int, ref: Float)

        @JvmStatic
        external fun glColorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean)

        @JvmStatic
        external fun glReadBuffer(mode: Int)

        @JvmStatic
        external fun glDrawBuffer(mode: Int)

        @JvmStatic
        external fun glDrawBuffersARB(n: Int, buffers: IntArray, offset: Int)

        @JvmStatic
        external fun glMatrixMode(mode: Int)

        @JvmStatic
        external fun glPushMatrix()

        @JvmStatic
        external fun glPopMatrix()

        @JvmStatic
        external fun glLoadIdentity()

        @JvmStatic
        external fun glOrtho(left: Double, right: Double, bottom: Double, top: Double, nearVal: Double, farVal: Double)

        @JvmStatic
        external fun glFrustum(left: Double, right: Double, bottom: Double, top: Double, nearVal: Double, farVal: Double)

        @JvmStatic
        external fun glTranslatef(x: Float, y: Float, z: Float)

        @JvmStatic
        external fun glRotatef(angle: Float, x: Float, y: Float, z: Float)

        @JvmStatic
        external fun glScalef(x: Float, y: Float, z: Float)

        @JvmStatic
        external fun glLoadMatrixf(matrix: FloatArray, offset: Int)

        @JvmStatic
        external fun glMultMatrixf(matrix: FloatArray, offset: Int)

        @JvmStatic
        external fun glViewport(x: Int, y: Int, width: Int, height: Int)

        @JvmStatic
        external fun glScissor(x: Int, y: Int, width: Int, height: Int)

        @JvmStatic
        external fun glGenLists(range: Int): Int

        @JvmStatic
        external fun glDeleteLists(list: Int, range: Int)

        @JvmStatic
        external fun glNewList(list: Int, mode: Int)

        @JvmStatic
        external fun glEndList()

        @JvmStatic
        external fun glCallList(list: Int)

        @JvmStatic
        external fun glBegin(mode: Int)

        @JvmStatic
        external fun glVertex2i(x: Int, y: Int)

        @JvmStatic
        external fun glVertex2f(x: Float, y: Float)

        @JvmStatic
        external fun glVertex3f(x: Float, y: Float, z: Float)

        @JvmStatic
        external fun glNormal3f(x: Float, y: Float, z: Float)

        @JvmStatic
        external fun glColor3f(r: Float, g: Float, b: Float)

        @JvmStatic
        external fun glColor4f(r: Float, g: Float, b: Float, a: Float)

        @JvmStatic
        external fun glColor3ub(r: Byte, g: Byte, b: Byte)

        @JvmStatic
        external fun glColor4ub(r: Byte, g: Byte, b: Byte, a: Byte)

        @JvmStatic
        external fun glTexCoord2i(s: Int, t: Int)

        @JvmStatic
        external fun glTexCoord3i(s: Int, t: Int, r: Int)

        @JvmStatic
        external fun glTexCoord2f(s: Float, t: Float)

        @JvmStatic
        external fun glTexCoord3f(s: Float, t: Float, r: Float)

        @JvmStatic
        external fun glMultiTexCoord2i(unit: Int, s: Int, t: Int)

        @JvmStatic
        external fun glMultiTexCoord3i(unit: Int, s: Int, t: Int, r: Int)

        @JvmStatic
        external fun glMultiTexCoord2f(textureUnit: Int, s: Float, t: Float)

        @JvmStatic
        external fun glRasterPos2i(x: Int, y: Int)

        @JvmStatic
        external fun glEnd()

        @JvmStatic
        external fun glEnableClientState(capability: Int)

        @JvmStatic
        external fun glDisableClientState(capability: Int)

        @JvmStatic
        external fun glClientActiveTexture(texture: Int)

        @JvmStatic
        external fun glVertexPointer(size: Int, type: Int, stride: Int, pointer: Long)

        @JvmStatic
        external fun glNormalPointer(type: Int, stride: Int, pointer: Long)

        @JvmStatic
        external fun glColorPointer(size: Int, type: Int, stride: Int, pointer: Long)

        @JvmStatic
        external fun glTexCoordPointer(size: Int, type: Int, stride: Int, pointer: Long)

        @JvmStatic
        external fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, pointer: Long)

        @JvmStatic
        external fun glEnableVertexAttribArray(index: Int)

        @JvmStatic
        external fun glDisableVertexAttribArray(index: Int)

        @JvmStatic
        external fun glBindAttribLocation(program: Int, index: Int, name: String)

        @JvmStatic
        external fun glGenBuffersARB(n: Int, buffers: IntArray, offset: Int)

        @JvmStatic
        external fun glBindBufferARB(target: Int, buffer: Int)

        @JvmStatic
        external fun glBufferDataARBa(target: Int, size: Int, data: Long, usage: Int)

        @JvmStatic
        external fun glBufferDataARBub(target: Int, size: Int, data: ByteArray?, offset: Int, usage: Int)

        @JvmStatic
        external fun glBufferSubDataARBa(target: Int, offset: Int, size: Int, data: Long)

        @JvmStatic
        external fun glBufferSubDataARBub(target: Int, offset: Int, size: Int, data: ByteArray, offset2: Int)

        @JvmStatic
        external fun glMapBufferARB(target: Int, access: Int): Long

        @JvmStatic
        external fun glUnmapBufferARB(bufferObjectID: Int): Boolean

        @JvmStatic
        external fun glDeleteBuffersARB(n: Int, buffers: IntArray, offset: Int)

        @JvmStatic
        external fun glLineWidth(width: Float)

        @JvmStatic
        external fun glPointSize(size: Float)

        @JvmStatic
        external fun glPolygonMode(face: Int, mode: Int)

        @JvmStatic
        external fun glDrawArrays(mode: Int, first: Int, count: Int)

        @JvmStatic
        external fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long)

        @JvmStatic
        external fun glGenTextures(n: Int, textures: IntArray, offset: Int)

        @JvmStatic
        external fun glDeleteTextures(n: Int, textures: IntArray, offset: Int)

        @JvmStatic
        external fun glActiveTexture(texture: Int)

        @JvmStatic
        external fun glBindTexture(target: Int, texture: Int)

        @JvmStatic
        external fun glTexEnvi(target: Int, pname: Int, param: Int)

        @JvmStatic
        external fun glTexEnvf(target: Int, pname: Int, param: Float)

        @JvmStatic
        external fun glTexEnvfv(target: Int, pname: Int, params: FloatArray, offset: Int)

        @JvmStatic
        external fun glTexGeni(coord: Int, pname: Int, param: Int)

        @JvmStatic
        external fun glTexGenfv(coord: Int, pname: Int, params: FloatArray, offset: Int)

        @JvmStatic
        external fun glTexParameterf(target: Int, pname: Int, param: Float)

        @JvmStatic
        external fun glTexParameteri(target: Int, pname: Int, param: Int)

        @JvmStatic
        external fun glGetTexImagei(target: Int, level: Int, format: Int, type: Int, pixels: IntArray, offset: Int)

        @JvmStatic
        external fun glGetTexImageub(target: Int, level: Int, format: Int, type: Int, pixels: ByteArray, offset: Int)

        @JvmStatic
        external fun glGetTexLevelParameterfv(target: Int, level: Int, pname: Int): Float

        @JvmStatic
        external fun glGetTexLevelParameteriv(target: Int, level: Int, pname: Int): Int

        @JvmStatic
        external fun glGetTexImage(target: Int, level: Int, format: Int, type: Int, pixels: ByteArray)

        @JvmStatic
        external fun glTexImage1Dub(target: Int, level: Int, internalformat: Int, width: Int, border: Int, format: Int, type: Int, pixels: ByteArray, offset: Int)

        @JvmStatic
        external fun glCopyTexImage2D(target: Int, level: Int, internalFormat: Int, x: Int, y: Int, width: Int, height: Int, border: Int)

        @JvmStatic
        external fun glCopyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int)

        @JvmStatic
        external fun glTexImage2Di(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: IntArray, offset: Int)

        @JvmStatic
        external fun glTexImage2Df(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, data: FloatArray, offset: Int)

        @JvmStatic
        external fun glTexImage2Dub(target: Int, level: Int, internalFormat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, data: ByteArray?, offset: Int)

        @JvmStatic
        external fun glTexSubImage2Di(target: Int, level: Int, offsetX: Int, offsetY: Int, width: Int, height: Int, format: Int, type: Int, pixels: IntArray, pixelOffset: Int)

        @JvmStatic
        external fun glTexSubImage2Df(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, data: FloatArray, offset: Int)

        @JvmStatic
        external fun glTexSubImage2Dub(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, data: ByteArray, offset: Int)

        @JvmStatic
        external fun glCopyTexSubImage3D(target: Int, level: Int, xoffset: Int, yoffset: Int, zoffset: Int, x: Int, y: Int, width: Int, height: Int)

        @JvmStatic
        external fun glTexImage3Dub(target: Int, level: Int, internal: Int, width: Int, height: Int, depth: Int, border: Int, format: Int, type: Int, data: ByteArray?, offset: Int)

        @JvmStatic
        external fun glCompressedTexImage2Dub(target: Int, level: Int, internal: Int, width: Int, height: Int, border: Int, imageSize: Int, data: ByteArray, offset: Int)

        @JvmStatic
        external fun glCopyPixels(x: Int, y: Int, width: Int, height: Int, type: Int)

        @JvmStatic
        external fun glReadPixelsi(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: IntArray, offset: Int)

        @JvmStatic
        external fun glReadPixelsub(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: ByteArray, offset: Int)

        @JvmStatic
        external fun glDrawPixelsi(width: Int, height: Int, format: Int, pixels: IntArray, offset: Int)

        @JvmStatic
        external fun glDrawPixelsub(x: Int, y: Int, width: Int, height: Int, pixels: ByteArray, offset: Int)

        @JvmStatic
        external fun glPixelZoom(xfactor: Float, yfactor: Float)

        @JvmStatic
        external fun glPixelStorei(parameter: Int, value: Int)

        @JvmStatic
        external fun glPixelTransferf(parameter: Int, value: Float)

        @JvmStatic
        external fun glColorMaterial(face: Int, mode: Int)

        @JvmStatic
        external fun glLightf(light: Int, pname: Int, param: Float)

        @JvmStatic
        external fun glLightfv(light: Int, pname: Int, params: FloatArray, offset: Int)

        @JvmStatic
        external fun glLightModelfv(pname: Int, params: FloatArray, offset: Int)

        @JvmStatic
        external fun glMaterialfv(face: Int, pname: Int, params: FloatArray, offset: Int)

        @JvmStatic
        external fun glFogi(pname: Int, param: Int)

        @JvmStatic
        external fun glFogf(pname: Int, param: Float)

        @JvmStatic
        external fun glFogfv(pname: Int, params: FloatArray, offset: Int)

        @JvmStatic
        external fun glBlendFunc(sfactor: Int, dfactor: Int)

        @JvmStatic
        external fun glBlendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)

        @JvmStatic
        external fun glBlendColor(red: Float, green: Float, blue: Float, alpha: Float)

        @JvmStatic
        external fun glGenFramebuffersEXT(n: Int, framebuffers: IntArray, offset: Int)

        @JvmStatic
        external fun glDeleteFramebuffersEXT(n: Int, framebuffers: IntArray, offset: Int)

        @JvmStatic
        external fun glBindFramebufferEXT(target: Int, framebuffer: Int)

        @JvmStatic
        external fun glFramebufferTexture2DEXT(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int)

        @JvmStatic
        external fun glFramebufferTexture3DEXT(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int, zoffset: Int)

        @JvmStatic
        external fun glFramebufferRenderbufferEXT(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int)

        @JvmStatic
        external fun glCheckFramebufferStatusEXT(target: Int): Int

        @JvmStatic
        external fun glBlitFramebufferEXT(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int)

        @JvmStatic
        external fun glGenerateMipmapEXT(target: Int)

        @JvmStatic
        external fun glGenRenderbuffersEXT(n: Int, renderbuffers: IntArray, offset: Int)

        @JvmStatic
        external fun glDeleteRenderbuffersEXT(n: Int, renderbuffers: IntArray, offset: Int)

        @JvmStatic
        external fun glBindRenderbufferEXT(target: Int, renderbuffer: Int)

        @JvmStatic
        external fun glRenderbufferStorageEXT(target: Int, internalFormat: Int, width: Int, height: Int)

        @JvmStatic
        external fun glRenderbufferStorageMultisampleEXT(target: Int, samples: Int, internalFormat: Int, width: Int, height: Int)

        @JvmStatic
        external fun glGenProgramARB(): Int

        @JvmStatic
        external fun glDeleteProgramARB(program: Int)

        @JvmStatic
        external fun glBindProgramARB(target: Int, program: Int)

        @JvmStatic
        external fun glProgramStringARB(target: Int, format: Int, string: String)

        @JvmStatic
        external fun glProgramRawARB(target: Int, format: Int, string: ByteArray)

        @JvmStatic
        external fun glProgramLocalParameter4fARB(target: Int, index: Int, x: Float, y: Float, z: Float, w: Float)

        @JvmStatic
        external fun glProgramLocalParameter4fvARB(target: Int, index: Int, params: FloatArray?, offset: Int)

        @JvmStatic
        external fun glGetProgramivARB(programHandle: Int, pname: Int, params: IntArray, offset: Int)

        @JvmStatic
        external fun glCreateProgram(): Int

        @JvmStatic
        external fun glCreateShader(shaderType: Int): Int

        @JvmStatic
        external fun glDeleteProgram(programHandle: Int)

        @JvmStatic
        external fun glDeleteShader(shaderHandle: Int)

        @JvmStatic
        external fun glShaderSource(shaderHandle: Int, shaderSource: String)

        @JvmStatic
        external fun glCompileShader(shaderHandle: Int)

        @JvmStatic
        external fun glAttachShader(programHandle: Int, shaderHandle: Int)

        @JvmStatic
        external fun glDetachShader(programHandle: Int, shaderHandle: Int)

        @JvmStatic
        external fun glLinkProgram(programHandle: Int)

        @JvmStatic
        external fun glUseProgram(programHandle: Int)

        @JvmStatic
        external fun glGetUniformLocation(programHandle: Int, uniformName: String): Int

        @JvmStatic
        external fun glUniform1i(location: Int, value: Int)

        @JvmStatic
        external fun glUniform1f(location: Int, value: Float)

        @JvmStatic
        external fun glUniform2f(location: Int, value1: Float, value2: Float)

        @JvmStatic
        external fun glUniform3f(location: Int, value1: Float, value2: Float, value3: Float)

        @JvmStatic
        external fun glUniform4f(location: Int, value1: Float, value2: Float, value3: Float, value4: Float)

        @JvmStatic
        external fun glUniform2fv(location: Int, count: Int, values: FloatArray, offset: Int)

        @JvmStatic
        external fun glUniform3fv(location: Int, count: Int, values: FloatArray, offset: Int)

        @JvmStatic
        external fun glUniform4fv(location: Int, count: Int, values: FloatArray, offset: Int)

        @JvmStatic
        external fun glUniformMatrix2fv(location: Int, count: Int, transpose: Boolean, values: FloatArray, offset: Int)

        @JvmStatic
        external fun glUniformMatrix3fv(location: Int, count: Int, transpose: Boolean, values: FloatArray, offset: Int)

        @JvmStatic
        external fun glUniformMatrix4fv(location: Int, count: Int, transpose: Boolean, values: FloatArray, offset: Int)

        @JvmStatic
        external fun glGetProgramiv(programHandle: Int, pname: Int, params: IntArray, offset: Int)

        @JvmStatic
        external fun glGetShaderiv(shaderHandle: Int, pname: Int, params: IntArray, offset: Int)

        @JvmStatic
        external fun glGetProgramInfoLog(programHandle: Int, maxLength: Int, length: IntArray, lengthOffset: Int, infoLog: ByteArray, infoLogOffset: Int)

        @JvmStatic
        external fun glGetShaderInfoLog(shaderHandle: Int, maxLength: Int, length: IntArray, lengthOffset: Int, infoLog: ByteArray, infoLogOffset: Int)

        @JvmStatic
        external fun glGetError(): Int

        @JvmStatic
        external fun glGetString(name: Int): String

        @JvmStatic
        external fun glGetFloatv(pname: Int, params: FloatArray, offset: Int)

        @JvmStatic
        external fun glGetIntegerv(pname: Int, params: IntArray, offset: Int)

        @JvmStatic
        external fun glStencilFunc(func: Int, ref: Int, mask: Int)

        @JvmStatic
        external fun glStencilOp(fail: Int, zfail: Int, zpass: Int)

    }

    @JvmField
    var peer: Long = 0

    @JvmField
    var supportedExtensions: Hashtable<String, String>? = null

    @JvmField
    var threadPeer: Thread? = null

    fun supportsExtension(extensionName: String): Boolean {
        if (this.supportedExtensions == null) {
            this.supportedExtensions = Hashtable()
            val oglString = glGetString(7939)
            var charIndex = 0
            while (true) {
                val oglWord = oglString.indexOf(' ' , charIndex)
                if (oglWord == -1) {
                    break
                }
                val oglWord2 = oglString.substring(charIndex, oglWord).trim()
                if (oglWord2.isNotEmpty()) {
                    this.supportedExtensions!![oglWord2] = oglWord2
                }
                charIndex = 1 + oglWord
            }
            val string8 = oglString.substring(charIndex).trim()
            if (string8.isNotEmpty()) {
                this.supportedExtensions!![string8] = string8
            }
        }
        return this.supportedExtensions!!.containsKey(extensionName)
    }

    @Synchronized
    fun attach(): Boolean {
        val thread = Thread.currentThread()
        if (attachPeer()) {
            val released = ASSOCIATED_THREADS.put(thread, this)
            if (released != null) {
                released.threadPeer = null
            }
            this.threadPeer = thread
            return true
        }
        return false
    }

    @Synchronized
    fun detach(): Boolean {
        if (this.threadPeer != Thread.currentThread()) {
            return false
        }
        detachPeer()
        val tp = this.threadPeer
        if (tp != null) {
            ASSOCIATED_THREADS.remove(tp)
        }
        this.threadPeer = null
        return true
    }

    external fun init(canvas: Canvas, redSize: Int, greenSize: Int, blueSize: Int, alphaSize: Int, depthSize: Int, stencilSize: Int): Long

    external fun arePbuffersAvailable(): Boolean

    external fun attachPeer(): Boolean

    external fun detachPeer()

    external fun prepareSurface(canvas: Canvas): Long

    external fun surfaceResized(surfaceHandle: Long)

    external fun releaseSurface(canvas: Canvas, surfaceHandle: Long)

    external fun setSurface(surfaceHandle: Long): Boolean

    external fun createPbuffer(width: Int, height: Int): Long

    external fun releasePbuffer(pbufferHandle: Long)

    external fun setPbuffer(pbufferHandle: Long)

    external fun swapBuffers(surfaceHandle: Long)

    external fun setSwapInterval(interval: Int)

    external fun release()

}
