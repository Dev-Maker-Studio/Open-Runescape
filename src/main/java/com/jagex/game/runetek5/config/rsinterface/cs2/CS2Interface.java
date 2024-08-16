package com.jagex.game.runetek5.config.rsinterface.cs2;

import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.Interface;

public class CS2Interface {
    Interface inter;
    Component comp;

    boolean setChild(int interfaceId, int slotId) {
        Component def = Component.listIcomponent(interfaceId, slotId);
        if (def != null) {
            this.inter = Interface.INTERFACES[interfaceId >> 16];
            this.comp = def;
            return true;
        }
        reset();
        return false;
    }

    boolean load(Interface rsinterface, int componentId) {
        if (rsinterface != null) {
            Component component = rsinterface.getComponent(componentId);
            if (component != null) {
                this.inter = rsinterface;
                this.comp = component;
                return true;
            }
        }

        reset();
        return false;
    }

    public void reset() {
        this.inter = null;
        this.comp = null;
    }

    Component method8772() {
        return this.inter.getComponent(this.comp.idHash);
    }

}
