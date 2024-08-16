package com.jagex.game.runetek5.client.input.mouse;

import java.awt.Component;
import java.awt.event.*;

import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.utils.Utils;

public class MouseManager extends AbstractMouseManager implements MouseListener, MouseMotionListener, MouseWheelListener {

    public int clickMask;

    public NodeCollection aClass4827941 = new NodeCollection();

    public NodeCollection clientMouseActions = new NodeCollection();

    public final boolean canvasInitialized;

    public int anInt7938;

    public int anInt7939;

    public int anInt7940;

    public int mouseX;

    public int mouseY;

    public Component canvasComponent;

    public MouseManager(Component component) {
    	this.setCanvas(component);
        this.canvasInitialized = true;
    }

    @Override
    public synchronized void mouseReleased(MouseEvent mouseEvent) {
        int mask = getClickmask(mouseEvent);
        if ((this.clickMask & mask) == 0) {
            mask = this.clickMask;
        }
        if ((mask & 0x1) != 0) {
        	this.queueAction(3, mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount());
        }
        if ((mask & 0x4) != 0) {
        	this.queueAction(5, mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount());
        }
        if ((mask & 0x2) != 0) {
        	this.queueAction(4, mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount());
        }
        this.clickMask &= ~mask;
        if (mouseEvent.isPopupTrigger()) {
            mouseEvent.consume();
        }
    }

    @Override
    public synchronized void mouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            mouseEvent.consume();
        }
    }

    @Override
    public synchronized void method3589() {
    	this.anInt7938 = this.mouseX;
    	this.anInt7939 = this.mouseY;
    	this.anInt7940 = this.clickMask;
        NodeCollection nodecollection2 = this.aClass4827941;
        this.aClass4827941 = this.clientMouseActions;
        this.clientMouseActions = nodecollection2;
        this.clientMouseActions.remove_all();
    }

    public void changePosition(int x, int y) {
    	this.mouseX = x;
    	this.mouseY = y;
        if (this.canvasInitialized) {
        	this.queueAction(-1, x, y, 0);
        }
    }

    @Override
    public boolean rightButtonDown() {
        return (this.anInt7940 & 0x2) != 0;
    }

    @Override
    public boolean leftButtonDown() {
        return (this.anInt7940 & 0x1) != 0;
    }

    @Override
    public synchronized void mouseMoved(MouseEvent mouseEvent) {
        changePosition(mouseEvent.getX(), mouseEvent.getY());
    }

    @Override
    public boolean middleButtonDown() {
        return (this.anInt7940 & 0x4) != 0;
    }

    public void setCanvas(Component canvasComponent) {
    	this.resetCanvasListeners();
    	this.canvasComponent = canvasComponent;
    	this.canvasComponent.addMouseListener(this);
    	this.canvasComponent.addMouseMotionListener(this);
    	this.canvasComponent.addMouseWheelListener(this);
    }

    @Override
    public int getMouseX() {
        return this.anInt7938;
    }

    @Override
    public void reset() {
    	this.resetCanvasListeners();
    }

    public void resetCanvasListeners() {
        if (this.canvasComponent != null) {
        	this.canvasComponent.removeMouseWheelListener(this);
        	this.canvasComponent.removeMouseMotionListener(this);
        	this.canvasComponent.removeMouseListener(this);
        	this.canvasComponent = null;
        	this.anInt7940 = 0;
        	this.anInt7939 = 0;
        	this.anInt7938 = 0;
        	this.clickMask = 0;
        	this.mouseY = 0;
        	this.mouseX = 0;
        	this.aClass4827941 = null;
        	this.clientMouseActions = null;
        }
    }

    @Override
    public synchronized void mouseEntered(MouseEvent mouseEvent) {
        changePosition(mouseEvent.getX(), mouseEvent.getY());
    }

    @Override
    public MouseRecord nextSubmission() {
        return (MouseRecord) this.aClass4827941.popHead();
    }

    public int getClickmask(MouseEvent mouseEvent) {
        return mouseEvent.getButton() == 1 ? (mouseEvent.isMetaDown() ? 4 : 1) : (mouseEvent.getButton() == 2 ? 2 : (mouseEvent.getButton() == 3 ? 4 : 0));
    }

    @Override
    public synchronized void mousePressed(MouseEvent mouseEvent) {
        int mask = getClickmask(mouseEvent);
        if (mask == 1) {
            queueAction(0, mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount());
        } else if (mask == 4) {
            queueAction(2, mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount());
        } else if (mask == 2) {
            queueAction(1, mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount());
        }
        this.clickMask |= mask;
        if (mouseEvent.isPopupTrigger()) {
            mouseEvent.consume();
        }
    }

    public void queueAction(int type, int x, int y, int meta) {
        MouseActionClient clickMouseAction = MouseActionClient.insert(type, x, y, Utils.time(), meta);
        this.clientMouseActions.addFirst(clickMouseAction);
    }

    @Override
    public synchronized void mouseWheelMoved(MouseWheelEvent wheelEvent) {
        int x = wheelEvent.getX();
        int y = wheelEvent.getY();
        int rotation = wheelEvent.getWheelRotation();
        if (wheelEvent.isControlDown()) {
            if (wheelEvent.getWheelRotation() == -1 && Camera.cameraZoom >= -50) {
                Camera.cameraZoom -= 30;
            }
            if (wheelEvent.getWheelRotation() == 1 && Camera.cameraZoom < 1700) {
                Camera.cameraZoom += 30;
            }
        }
        queueAction(6, x, y, rotation);
        wheelEvent.consume();
    }

    @Override
    public int getMouseY() {
        return this.anInt7939;
    }

    @Override
    public synchronized void mouseExited(MouseEvent mouseEvevent) {
        changePosition(mouseEvevent.getX(), mouseEvevent.getY());
    }

    @Override
    public synchronized void mouseDragged(MouseEvent mouseEvent) {
        changePosition(mouseEvent.getX(), mouseEvent.getY());
    }
}
