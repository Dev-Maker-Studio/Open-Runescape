package com.jagex.game.runetek5.client.input.keyboard;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.utils.Utils;

/**
 * 
 * @author Brainpower - 19/04/2022
 *
 */
public class CanvasKeyRecorder extends KeyRecorder implements KeyListener, FocusListener {

    public static final int[] REMAPPED_KEYS = {0, 0, 0, 0, 0, 0, 0, 0, 85, 80, 84, 0, 91, 0, 0, 0, 81, 82, 86, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 83, 104, 105, 103, 102, 96, 98, 97, 99, 0, 0, 0, 0, 0, 0, 0, 25, 16, 17, 18, 19, 20, 21, 22, 23, 24, 0, 0, 0, 0, 0, 0, 0, 48, 68, 66, 50, 34, 51, 52, 53, 39, 54, 55, 56, 70, 69, 40, 41, 32, 35, 49, 36, 38, 67, 33, 65, 37, 64, 0, 0, 0, 0, 0, 228, 231, 227, 233, 224, 219, 225, 230, 226, 232, 89, 87, 0, 88, 229, 90, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0, 0, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public final NodeCollection keysAwaitingProcessing = new NodeCollection();

    public final NodeCollection keyRecordEntries = new NodeCollection();

    public final boolean[] keysHeld = new boolean[112];

    public Component component;

    public CanvasKeyRecorder(Component component) {
        remap();
        setComponent(component);
    }

    public static KeyRecorder create(Component component) {
        return new CanvasKeyRecorder(component);
    }

    public static void remap() {
        REMAPPED_KEYS[44] = 71;
        REMAPPED_KEYS[45] = 26;
        REMAPPED_KEYS[46] = 72;
        REMAPPED_KEYS[47] = 73;
        REMAPPED_KEYS[59] = 57;
        REMAPPED_KEYS[61] = 27;
        REMAPPED_KEYS[91] = 42;
        REMAPPED_KEYS[92] = 74;
        REMAPPED_KEYS[93] = 43;
        REMAPPED_KEYS[192] = 28;
        REMAPPED_KEYS[222] = 58;
        REMAPPED_KEYS[520] = 59;
    }

    @Override
    public boolean held(int index) {
        return (index >= 0 && index < 112) && this.keysHeld[index];
    }

    public void addKeyRecordEntry(int type, char character, int code) {
        KeyRecordEntry entry = new KeyRecordEntry();
        entry.type = type;
        entry.keyCharacter = character;
        entry.keyCode = code;
        entry.timeSubmitted = Utils.time();
        this.keyRecordEntries.addFirst(entry);
    }

    @Override
    public synchronized void prepareAwaitingKeys() {
        this.keysAwaitingProcessing.remove_all();
        for (KeyRecordEntry entry = (KeyRecordEntry) this.keyRecordEntries.popHead(); entry != null; entry = (KeyRecordEntry) this.keyRecordEntries.popHead()) {
            entry.flags = getFlags();
            if (entry.type == 0) {
                if (!this.keysHeld[entry.keyCode]) {
                    KeyRecordEntry keyHeldEntry = new KeyRecordEntry();
                    keyHeldEntry.type = 0;
                    keyHeldEntry.keyCharacter = 0xffff;
                    keyHeldEntry.keyCode = entry.keyCode;
                    keyHeldEntry.timeSubmitted = entry.timeSubmitted;
                    keyHeldEntry.flags = entry.flags;
                    this.keysAwaitingProcessing.addFirst(keyHeldEntry);
                    this.keysHeld[entry.keyCode] = true;
                }
                entry.type = 2;
                this.keysAwaitingProcessing.addFirst(entry);
            } else if (entry.type == 1) {
                if (this.keysHeld[entry.keyCode]) {
                    this.keysAwaitingProcessing.addFirst(entry);
                    this.keysHeld[entry.keyCode] = false;
                }
            } else if (entry.type == -1) {
                for (int index = 0; index < 112; index++) {
                    if (this.keysHeld[index]) {
                        KeyRecordEntry unicodeKey = new KeyRecordEntry();
                        unicodeKey.type = 1;
                        unicodeKey.keyCharacter = 0xffff;
                        unicodeKey.keyCode = index;
                        unicodeKey.timeSubmitted = entry.timeSubmitted;
                        unicodeKey.flags = entry.flags;
                        this.keysAwaitingProcessing.addFirst(unicodeKey);
                        this.keysHeld[index] = false;
                    }
                }
            } else if (entry.type == 3) {
                this.keysAwaitingProcessing.addFirst(entry);
            }
        }
    }

    @Override
    public KeyRecord popHead() {
        return (KeyRecord) this.keysAwaitingProcessing.popHead();
    }

    public void onKeyPress(KeyEvent keyEvent, int type) {
        int keyCode = keyEvent.getKeyCode();
        if (keyCode != 0) {
            if (keyCode >= 0 && keyCode < REMAPPED_KEYS.length) {
                keyCode = REMAPPED_KEYS[keyCode];
                if (type == 0 && (keyCode & 0x80) != 0) {
                    keyCode = 0;
                } else {
                    keyCode &= -129;
                }
            } else {
                keyCode = 0;
            }
        } 
        if (keyCode != 0) {
            addKeyRecordEntry(type, (char) 0xffff, keyCode);
            keyEvent.consume();
        }
    }

    @Override
    public synchronized void keyPressed(KeyEvent keyEvent) {
        onKeyPress(keyEvent, 0);
    }

    @Override
    public synchronized void keyReleased(KeyEvent keyEvent) {
        onKeyPress(keyEvent, 1);
    }

    @Override
    public void focusGained(FocusEvent focusevent1) {
    }

    @Override
    public synchronized void focusLost(FocusEvent focusEvent) {
        addKeyRecordEntry(-1, '\u0000', 0);
    }

    public void reset() {
        if (this.component != null) {
            this.component.removeKeyListener(this);
            this.component.removeFocusListener(this);
            this.component = null;
            for (int index = 0; index < 112; index++) {
                this.keysHeld[index] = false;
            }
            this.keysAwaitingProcessing.remove_all();
            this.keyRecordEntries.remove_all();
        }
    }

    @Override
    public synchronized void keyTyped(KeyEvent keyEvent) {
        char keyChar = keyEvent.getKeyChar();
        if (keyChar != 65535 && CanvasKeyRecorder.isValidKeyChar(keyChar)) {
            addKeyRecordEntry(3, keyChar, -1);
            keyEvent.consume();
        }
    }

    @Override
    public void addKeyListeners() {
        reset();
    }

    public void setComponent(Component component) {
        reset();
        this.component = component;
        this.component.addKeyListener(this);
        this.component.addFocusListener(this);
    }

    public int getFlags() {
        int flags = 0;
        if (this.keysHeld[81]) {
            flags |= 0x1;
        }
        if (this.keysHeld[82]) {
            flags |= 0x4;
        }
        if (this.keysHeld[86]) {
            flags |= 0x2;
        }
        return flags;
    }

	public static boolean isValidKeyChar(char typedChar) {
	    if ((typedChar <= 0 || typedChar >= 128) && (typedChar < 160 || typedChar > 255)) {
	        if (typedChar != 0) {
	            char[] unicodeTable = Utils.UNICODE_TABLE;
	            for (int index = 0; index < unicodeTable.length; index++) {
	                char unicode = unicodeTable[index];
	                if (typedChar == unicode) {
	                    return true;
	                }
	            }
	        }
	        return false;
	    } 
	    return true;	    
	}
}