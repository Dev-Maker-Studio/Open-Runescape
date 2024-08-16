package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.NativeSpriteGenerator;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.sound.jagtheora.JagTheoraTheoraNode;
import com.jagex.game.runetek5.sound.jagtheora.JagTheoraVorbisNode;
import com.jagex.game.runetek5.sound.jagtheora.Node_Sub15_Sub1;
import com.jagex.game.runetek5.sound.jagtheora.VorbisFileReference;
import com.jagex.game.runetek5.sound.player.area.AmbientSound;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class VorbisNode extends Node<VorbisNode> {

	public boolean aBool7995;
	public boolean aBool7996;
	public boolean aBool8000;
	public final int fileId;
    public final VorbisFileReference vfReference;
    public final int anInt7997;
    public final boolean sendPacket;
	public static final NativeSpriteGenerator SPRITE_GENERATOR = new NativeSpriteGenerator();

    public VorbisNode(int fileId, VorbisFileReference file, int i_3, boolean bool4) {
        this.fileId = fileId;
        this.vfReference = file;
        this.anInt7997 = i_3;
        this.sendPacket = bool4;
    }

    public static void method6511() {
        for (VorbisNode class282Sub371 = StaticMethods.INDEX36_FILE_CACHE.first(); class282Sub371 != null; class282Sub371 = StaticMethods.INDEX36_FILE_CACHE.next()) {
            if (!class282Sub371.aBool7996) {
                StaticMethods.method1563(class282Sub371.fileId);
            } else {
                class282Sub371.aBool7996 = false;
            }
        }
    }

    public static void method15536() {
        for (VorbisNode class282Sub371 = StaticMethods.INDEX36_FILE_CACHE.first(); class282Sub371 != null; class282Sub371 = StaticMethods.INDEX36_FILE_CACHE.next()) {
            if (class282Sub371.vfReference.method4921()) {
                StaticMethods.method1563(class282Sub371.fileId);
            } else {
                class282Sub371.vfReference.method13447();

                try {
                    class282Sub371.vfReference.method4932();
                } catch (Exception exception4) {
                    PacketEncoder.sendErrorToServer("" + class282Sub371.fileId, exception4);
                    StaticMethods.method1563(class282Sub371.fileId);
                }

                if (!class282Sub371.aBool8000 && !class282Sub371.aBool7995) {
                    JagTheoraVorbisNode class282Sub41Sub42 = class282Sub371.vfReference.method4914();
                    if (class282Sub41Sub42 != null) {
                        Node_Sub15_Sub1 class282Sub15Sub13 = class282Sub41Sub42.method14845();
                        if (class282Sub15Sub13 != null) {
                            class282Sub15Sub13.method14820(class282Sub371.anInt7997);
                            AmbientSound.gameSounds.addSubStream(class282Sub15Sub13);
                            class282Sub371.aBool8000 = true;
                        }
                    }
                }
            }
        }

    }

    public static NativeSprite method8858(AbstractToolkit toolkit, int fileId) {
        VorbisNode vorbisNode = StaticMethods.INDEX36_FILE_CACHE.get(fileId);
        if (vorbisNode != null) {
            JagTheoraTheoraNode jagTheoraNode = vorbisNode.vfReference.method4918();
            vorbisNode.aBool7996 = true;
            if (jagTheoraNode != null) {
                VorbisNode.SPRITE_GENERATOR.toolkit = toolkit;
                return jagTheoraNode.getFrameSprite(VorbisNode.SPRITE_GENERATOR);
            }
        }
        return null;
    }
}
