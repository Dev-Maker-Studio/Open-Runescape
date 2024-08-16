package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.NodeCollection;

public class Node_Sub15_Sub4 extends PcmStream {

    public final NodeCollection aClass4829824 = new NodeCollection();
    public final NodeCollection aClass4829825 = new NodeCollection();
    public int anInt9826;
    public int anInt9827 = -1;

    public void method15274(Node node1, Node_Sub22 class282Sub222) {
        while (this.aClass4829825.tail != node1 && ((Node_Sub22) node1).anInt7676 <= class282Sub222.anInt7676) {
            node1 = node1.next;
        }

        Node.replace(class282Sub222, node1);
        this.anInt9827 = ((Node_Sub22) this.aClass4829825.tail.next).anInt7676;
    }

    public synchronized void addSubStream(PcmStream class282Sub151) {
        this.aClass4829824.method8098(class282Sub151);
    }

    public synchronized void unlink(PcmStream class282Sub151) {
        class282Sub151.unlink();
    }

    public void method15277() {
        if (this.anInt9826 > 0) {
            for (Node_Sub22 class282Sub221 = (Node_Sub22) this.aClass4829825.getFirst(); class282Sub221 != null; class282Sub221 = (Node_Sub22) this.aClass4829825.next()) {
                class282Sub221.anInt7676 -= this.anInt9826;
            }

            this.anInt9827 -= this.anInt9826;
            this.anInt9826 = 0;
        }

    }

    public void method15279(Node_Sub22 class282Sub221) {
        class282Sub221.unlink();
        class282Sub221.method12355();
        Node node2 = this.aClass4829825.tail.next;
        if (node2 == this.aClass4829825.tail) {
            this.anInt9827 = -1;
        } else {
            this.anInt9827 = ((Node_Sub22) node2).anInt7676;
        }

    }

    public void method15280(int i_1) {
        for (PcmStream class282Sub152 = (PcmStream) this.aClass4829824.getFirst(); class282Sub152 != null; class282Sub152 = (PcmStream) this.aClass4829824.next()) {
            class282Sub152.skip(i_1);
        }

    }

    @Override
    public int method12228() {
        return 0;
    }

    @Override
    public synchronized void fill(int[] ints_1, int i_2, int i_3) {
        while (true) {
            if (this.anInt9827 < 0) {
                method15282(ints_1, i_2, i_3);
            } else if (i_3 + this.anInt9826 < this.anInt9827) {
                this.anInt9826 += i_3;
                method15282(ints_1, i_2, i_3);
            } else {
                int i_4 = this.anInt9827 - this.anInt9826;
                method15282(ints_1, i_2, i_4);
                i_2 += i_4;
                i_3 -= i_4;
                this.anInt9826 += i_4;
                method15277();
                Node_Sub22 class282Sub225 = (Node_Sub22) this.aClass4829825.getFirst();
                synchronized (class282Sub225) {
                    int i_7 = class282Sub225.method12354(this);
                    if (i_7 < 0) {
                        class282Sub225.anInt7676 = 0;
                        method15279(class282Sub225);
                    } else {
                        class282Sub225.anInt7676 = i_7;
                        method15274(class282Sub225.next, class282Sub225);
                    }
                }

                if (i_3 != 0) {
                    continue;
                }
            }

            return;
        }
    }

    public void method15282(int[] ints_1, int i_2, int i_3) {
        for (PcmStream class282Sub154 = (PcmStream) this.aClass4829824.getFirst(); class282Sub154 != null; class282Sub154 = (PcmStream) this.aClass4829824.next()) {
            class282Sub154.process(ints_1, i_2, i_3);
        }

    }

    @Override
    public synchronized void skip(int i_1) {
        while (true) {
            if (this.anInt9827 < 0) {
                method15280(i_1);
            } else if (this.anInt9826 + i_1 < this.anInt9827) {
                this.anInt9826 += i_1;
                method15280(i_1);
            } else {
                int i_2 = this.anInt9827 - this.anInt9826;
                method15280(i_2);
                i_1 -= i_2;
                this.anInt9826 += i_2;
                method15277();
                Node_Sub22 class282Sub223 = (Node_Sub22) this.aClass4829825.getFirst();
                synchronized (class282Sub223) {
                    int i_5 = class282Sub223.method12354(this);
                    if (i_5 < 0) {
                        class282Sub223.anInt7676 = 0;
                        method15279(class282Sub223);
                    } else {
                        class282Sub223.anInt7676 = i_5;
                        method15274(class282Sub223.next, class282Sub223);
                    }
                }

                if (i_1 != 0) {
                    continue;
                }
            }

            return;
        }
    }

    @Override
    public PcmStream getNextStream() {
        return (PcmStream) this.aClass4829824.next();
    }

    @Override
    public PcmStream getFirstStream() {
        return (PcmStream) this.aClass4829824.getFirst();
    }

}
