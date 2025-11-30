package com.jagex.game.runetek5.client.input.mouse

import java.awt.Component
import java.awt.event.*

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.utils.Utils

class MouseManager(component: Component) : AbstractMouseManager(), MouseListener, MouseMotionListener, MouseWheelListener {

    @JvmField var clickMask: Int = 0

    @JvmField var aClass4827941: NodeCollection? = NodeCollection()

    @JvmField var clientMouseActions: NodeCollection? = NodeCollection()

    @JvmField val canvasInitialized: Boolean

    @JvmField var anInt7938: Int = 0

    @JvmField var anInt7939: Int = 0

    @JvmField var anInt7940: Int = 0

    @JvmField var mouseX: Int = 0

    @JvmField var mouseY: Int = 0

    @JvmField var canvasComponent: Component? = null

    init {
        setCanvas(component)
        canvasInitialized = true
    }

    @Synchronized
    override fun mouseReleased(mouseEvent: MouseEvent) {
        var mask = getClickmask(mouseEvent)
        if ((clickMask and mask) == 0) {
            mask = clickMask
        }
        if ((mask and 0x1) != 0) {
            queueAction(3, mouseEvent.x, mouseEvent.y, mouseEvent.clickCount)
        }
        if ((mask and 0x4) != 0) {
            queueAction(5, mouseEvent.x, mouseEvent.y, mouseEvent.clickCount)
        }
        if ((mask and 0x2) != 0) {
            queueAction(4, mouseEvent.x, mouseEvent.y, mouseEvent.clickCount)
        }
        clickMask = clickMask and mask.inv()
        if (mouseEvent.isPopupTrigger) {
            mouseEvent.consume()
        }
    }

    @Synchronized
    override fun mouseClicked(mouseEvent: MouseEvent) {
        if (mouseEvent.isPopupTrigger) {
            mouseEvent.consume()
        }
    }

    @Synchronized
    override fun method3589() {
        anInt7938 = mouseX
        anInt7939 = mouseY
        anInt7940 = clickMask
        val nodecollection2 = aClass4827941
        aClass4827941 = clientMouseActions
        clientMouseActions = nodecollection2
        clientMouseActions!!.remove_all()
    }

    fun changePosition(x: Int, y: Int) {
        mouseX = x
        mouseY = y
        if (canvasInitialized) {
            queueAction(-1, x, y, 0)
        }
    }

    override fun rightButtonDown(): Boolean {
        return (anInt7940 and 0x2) != 0
    }

    override fun leftButtonDown(): Boolean {
        return (anInt7940 and 0x1) != 0
    }

    @Synchronized
    override fun mouseMoved(mouseEvent: MouseEvent) {
        changePosition(mouseEvent.x, mouseEvent.y)
    }

    override fun middleButtonDown(): Boolean {
        return (anInt7940 and 0x4) != 0
    }

    fun setCanvas(canvasComponent: Component) {
        resetCanvasListeners()
        this.canvasComponent = canvasComponent
        this.canvasComponent!!.addMouseListener(this)
        this.canvasComponent!!.addMouseMotionListener(this)
        this.canvasComponent!!.addMouseWheelListener(this)
    }

    override fun getMouseX(): Int {
        return anInt7938
    }

    override fun reset() {
        resetCanvasListeners()
    }

    fun resetCanvasListeners() {
        if (canvasComponent != null) {
            canvasComponent!!.removeMouseWheelListener(this)
            canvasComponent!!.removeMouseMotionListener(this)
            canvasComponent!!.removeMouseListener(this)
            canvasComponent = null
            anInt7940 = 0
            anInt7939 = 0
            anInt7938 = 0
            clickMask = 0
            mouseY = 0
            mouseX = 0
            aClass4827941 = null
            clientMouseActions = null
        }
    }

    @Synchronized
    override fun mouseEntered(mouseEvent: MouseEvent) {
        changePosition(mouseEvent.x, mouseEvent.y)
    }

    override fun nextSubmission(): MouseRecord? {
        return aClass4827941!!.popHead() as MouseRecord?
    }

    fun getClickmask(mouseEvent: MouseEvent): Int {
        return if (mouseEvent.button == 1) {
            if (mouseEvent.isMetaDown) 4 else 1
        } else if (mouseEvent.button == 2) {
            2
        } else if (mouseEvent.button == 3) {
            4
        } else {
            0
        }
    }

    @Synchronized
    override fun mousePressed(mouseEvent: MouseEvent) {
        val mask = getClickmask(mouseEvent)
        if (mask == 1) {
            queueAction(0, mouseEvent.x, mouseEvent.y, mouseEvent.clickCount)
        } else if (mask == 4) {
            queueAction(2, mouseEvent.x, mouseEvent.y, mouseEvent.clickCount)
        } else if (mask == 2) {
            queueAction(1, mouseEvent.x, mouseEvent.y, mouseEvent.clickCount)
        }
        clickMask = clickMask or mask
        if (mouseEvent.isPopupTrigger) {
            mouseEvent.consume()
        }
    }

    fun queueAction(type: Int, x: Int, y: Int, meta: Int) {
        val clickMouseAction = MouseActionClient.insert(type, x, y, Utils.time(), meta)
        clientMouseActions!!.addFirst(clickMouseAction)
    }

    @Synchronized
    override fun mouseWheelMoved(wheelEvent: MouseWheelEvent) {
        val x = wheelEvent.x
        val y = wheelEvent.y
        val rotation = wheelEvent.wheelRotation
        if (wheelEvent.isControlDown) {
            if (wheelEvent.wheelRotation == -1 && Camera.cameraZoom >= -50) {
                Camera.cameraZoom -= 30
            }
            if (wheelEvent.wheelRotation == 1 && Camera.cameraZoom < 1700) {
                Camera.cameraZoom += 30
            }
        }
        queueAction(6, x, y, rotation)
        wheelEvent.consume()
    }

    override fun getMouseY(): Int {
        return anInt7939
    }

    @Synchronized
    override fun mouseExited(mouseEvevent: MouseEvent) {
        changePosition(mouseEvevent.x, mouseEvevent.y)
    }

    @Synchronized
    override fun mouseDragged(mouseEvent: MouseEvent) {
        changePosition(mouseEvent.x, mouseEvent.y)
    }
}
