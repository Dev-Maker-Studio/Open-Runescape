package com.jagex.game.runetek5.sound.jagtheora;

import jagtheora.ogg.OggPacket;
import jagtheora.ogg.OggPage;
import jagtheora.ogg.OggStreamState;
import jagtheora.ogg.OggSyncState;

import java.io.IOException;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.Utils;

public abstract class JagTheoraManager {

    public boolean aBool3361;
    public JagTheoraTheoraNode aNodeSub41Sub33363;
    public JagTheoraVorbisNode aNodeSub41Sub43366;
    public String languageAbbreviation;
    public Node_Sub41_Sub2 aNodeSub41Sub23364;
    public boolean aBool3353;
    public boolean aBool3359;
    public byte[] buffer;
    public OggSyncState syncState;
    public OggPage page;
    public OggPacket packet;
    public IterableNodeMap cache;
    public boolean aBool3360;

    public JagTheoraManager() {
        if (!AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jagtheora")) {
            throw new RuntimeException("");
        } else {
            this.buffer = new byte[4096];
            this.syncState = new OggSyncState();
            this.page = new OggPage();
            this.packet = new OggPacket();
            this.cache = new IterableNodeMap(8);
        }
    }

    public boolean method4910() {
        if (this.aNodeSub41Sub43366 != null) {
            return !this.aNodeSub41Sub33363.isHeaderDecoded() || method4923() > this.aNodeSub41Sub33363.getGranuleTime();
        } else {
            double d_2 = this.aNodeSub41Sub33363.getFps();
            return d_2 == 0.0D || Utils.time() >= this.aNodeSub41Sub33363.getPacketDecodeTime() + 1000.0D / d_2;
        }
    }

    public Node_Sub41 method4911() throws IOException {
        if (this.aBool3360) {
            throw new IllegalStateException();
        } else if (this.aBool3361) {
            return null;
        } else {
            int i_2;
            while (this.syncState.pageOut(this.page) <= 0) {
                i_2 = decode(this.buffer);
                if (i_2 == -1) {
                    this.aBool3361 = true;
                    return null;
                }
                if (i_2 == 0) {
                    return null;
                }
                if (!this.syncState.write(this.buffer, i_2)) {
                    throw new RuntimeException("");
                }
            }
            i_2 = this.page.getSerialNumber();
            if (!this.page.isBOS()) {
                Node_Sub41 class282Sub419 = (Node_Sub41) this.cache.get(i_2);
                if (!class282Sub419.anOggStreamState8032.pageIn(this.page)) {
                    throw new IllegalStateException();
                } else {
                    return class282Sub419;
                }
            } else {
                OggStreamState state = new OggStreamState(i_2);
                if (!state.pageIn(this.page)) {
                    throw new IllegalStateException();
                } else if (state.packetPeek(this.packet) != 1) {
                    throw new IllegalStateException();
                } else {
                    Node_Sub41 obj4;
                    if (this.aNodeSub41Sub33363 == null && this.packet.isTheora()) {
                        this.aNodeSub41Sub33363 = new JagTheoraTheoraNode(state);
                        obj4 = this.aNodeSub41Sub33363;
                    } else if (this.aNodeSub41Sub43366 == null && this.packet.isVorbis()) {
                        this.aNodeSub41Sub43366 = new JagTheoraVorbisNode(state);
                        obj4 = this.aNodeSub41Sub43366;
                    } else {
                        byte[] bytes_5 = this.packet.getData();
                        StringBuilder stringbuilder6 = new StringBuilder();
                        for (int i_7 = 1; i_7 < bytes_5.length && Character.isLetterOrDigit((char) bytes_5[i_7]); i_7++) {
                            stringbuilder6.append((char) bytes_5[i_7]);
                        }
                        String string_8 = stringbuilder6.toString();
                        if ("kate".equals(string_8)) {
                            obj4 = new Node_Sub41_Sub2(state);
                        } else {
                            obj4 = new Node_Sub41_Sub1(state);
                        }
                    }
                    this.cache.put(obj4, i_2);
                    return obj4;
                }
            }
        }
    }

    public void method4913() throws IOException {
        while (this.aNodeSub41Sub33363.anOggStreamState8032.packetOut(this.packet) != 1) {
            Node_Sub41 class282Sub412 = method4911();
            if (class282Sub412 == null) {
                if (this.aBool3361) {
                    method4916();
                }
                return;
            }
            if (class282Sub412 == this.aNodeSub41Sub23364) {
                method4942();
            }
        }
        this.aNodeSub41Sub33363.method13361(this.packet);
    }

    public JagTheoraVorbisNode method4914() {
        return this.aNodeSub41Sub43366;
    }

    public void setLanguage(String languageAbbreviation) {
        this.languageAbbreviation = languageAbbreviation;
        if (this.languageAbbreviation == null) {
            this.aNodeSub41Sub23364 = null;
        } else {
            if (this.aNodeSub41Sub23364 != null && !this.languageAbbreviation.equals(this.aNodeSub41Sub23364.getLanguageAbbreviation())) {
                this.aNodeSub41Sub23364 = null;
            }
            if (this.aNodeSub41Sub23364 == null) {
                for (Node_Sub41 class282Sub413 = (Node_Sub41) this.cache.first(); class282Sub413 != null; class282Sub413 = (Node_Sub41) this.cache.next()) {
                    if (class282Sub413 instanceof Node_Sub41_Sub2 class282Sub41Sub24) {
                        if (this.languageAbbreviation.equals(class282Sub41Sub24.getLanguageAbbreviation())) {
                            this.aNodeSub41Sub23364 = class282Sub41Sub24;
                            break;
                        }
                    }
                }
            }
        }
    }

    public void method4916() {
        for (Node_Sub41 class282Sub412 = (Node_Sub41) this.cache.first(); class282Sub412 != null; class282Sub412 = (Node_Sub41) this.cache.next()) {
            if (class282Sub412 != this.aNodeSub41Sub33363) {
                while (class282Sub412.anOggStreamState8032.packetOut() == 1) {
                    class282Sub412.method13361(this.packet);
                }
            }
        }
        if (this.aNodeSub41Sub33363 != null) {
            for (int i_3 = 0; i_3 < 10 && method4910(); i_3++) {
                if (this.aNodeSub41Sub33363.anOggStreamState8032.packetOut() != 1) {
                    method4947();
                    break;
                }
                this.aNodeSub41Sub33363.method13361(this.packet);
            }
        }
    }

    public JagTheoraTheoraNode method4918() {
        return this.aNodeSub41Sub33363;
    }

    public boolean method4921() {
        return (this.aBool3360 || this.aBool3361) && (this.aNodeSub41Sub43366 == null || this.aNodeSub41Sub43366.method14847() <= 0);
    }

    public double method4923() {
        return this.aNodeSub41Sub43366 != null ? this.aNodeSub41Sub43366.method14848() : (this.aNodeSub41Sub33363 != null ? this.aNodeSub41Sub33363.getGranuleTime() : 0.0D);
    }

    public void method4924(boolean bool1) {
        if (this.aNodeSub41Sub43366 != null) {
            Node_Sub15_Sub1 class282Sub15Sub13 = this.aNodeSub41Sub43366.method14845();
            if (class282Sub15Sub13 != null) {
                class282Sub15Sub13.method14816(bool1);
            }
        }
        this.aBool3353 = !this.aBool3353;
    }

    public void method4932() throws IOException {
        if (!this.aBool3353) {
            for (; !this.aBool3360; this.aBool3359 = false) {
                Node_Sub41 class282Sub412;
                if (!this.aBool3359) {
                    class282Sub412 = method4911();
                    if (class282Sub412 == null) {
                        if (this.aBool3361) {
                            method4916();
                        }
                        break;
                    }
                    this.aBool3359 = true;
                } else {
                    class282Sub412 = (Node_Sub41) this.cache.get(this.page.getSerialNumber());
                }
                if (class282Sub412 == this.aNodeSub41Sub43366) {
                    if (this.aNodeSub41Sub43366.method14847() >= 50) {
                        break;
                    }
                    while (this.aNodeSub41Sub43366.anOggStreamState8032.packetOut(this.packet) == 1) {
                        this.aNodeSub41Sub43366.method13361(this.packet);
                        method4942();
                        if (this.aNodeSub41Sub33363 != null) {
                            double d_3 = this.aNodeSub41Sub33363.getGranuleTime();
                            for (int i_5 = 0; i_5 < 10 && method4910(); i_5++) {
                                method4913();
                                if (this.aBool3360) {
                                    return;
                                }
                            }
                            if (d_3 < this.aNodeSub41Sub33363.getGranuleTime()) {
                                return;
                            }
                        }
                        if (this.aNodeSub41Sub43366.method14847() >= 50) {
                            return;
                        }
                    }
                } else if (class282Sub412 instanceof Node_Sub41_Sub2) {
                    method4942();
                } else if (class282Sub412 != this.aNodeSub41Sub33363) {
                    for (; class282Sub412.anOggStreamState8032.packetOut(this.packet) == 1; class282Sub412.method13361(this.packet)) {
                    }
                } else if (this.aNodeSub41Sub43366 == null && !this.aBool3353) {
                    for (int i_6 = 0; i_6 < 10 && method4910(); i_6++) {
                        method4913();
                        if (this.aBool3360) {
                            return;
                        }
                    }
                    return;
                }
            }
        }
    }

    public Node_Sub41_Sub2 method4935() {
        return this.aNodeSub41Sub23364;
    }

    public void method4942() {
        for (Node_Sub41 class282Sub412 = (Node_Sub41) this.cache.first(); class282Sub412 != null; class282Sub412 = (Node_Sub41) this.cache.next()) {
            if (class282Sub412 instanceof Node_Sub41_Sub2 class282Sub41Sub23) {
                while ((class282Sub41Sub23.anInt8031 <= 8 || method4923() > class282Sub41Sub23.method14705()) && class282Sub41Sub23.anOggStreamState8032.packetOut(this.packet) == 1) {
                    class282Sub41Sub23.method13361(this.packet);
                }
            }
        }
        setLanguage(this.languageAbbreviation);
    }

    public void method4947() {
        if (!this.aBool3360) {
            for (Node_Sub41 class282Sub412 = (Node_Sub41) this.cache.first(); class282Sub412 != null; class282Sub412 = (Node_Sub41) this.cache.next()) {
                class282Sub412.reset();
                class282Sub412.anOggStreamState8032.resetPeer();
            }
            this.packet.resetPeer();
            this.page.resetPeer();
            this.syncState.resetPeer();
            this.aBool3360 = true;
        }
    }

    public abstract int decode(byte[] var1);
}
