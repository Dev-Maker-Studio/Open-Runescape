package com.jagex.game.runetek5.staticmethods;

import java.util.Random;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.CheckEmailRequest;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.menu.ColoredMiniMenuEntry;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuBuilder;
import com.jagex.game.runetek5.client.menu.MiniMenuEntry;
import com.jagex.game.runetek5.client.menu.MenuInteraction;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.cutscene.*;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement;
import com.jagex.game.runetek5.config.material.texture.operation.Class149;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpRasterizer;
import com.jagex.game.runetek5.config.midi.SoundBankCache;
import com.jagex.game.runetek5.config.midi.SongReference;
import com.jagex.game.runetek5.config.midi.VorbisNode;
import com.jagex.game.runetek5.config.midi.instrument.MusicSample;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interpreter;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontCombo;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.hint.HintRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.network.packet.AbstractMousePacket;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.tile.TileMessage;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.trigger.sound.SoundTrigger;
import com.jagex.game.runetek5.utils.*;

public class StaticMethods {

    public static final float[] aFloatArray194 = new float[4];
    public static final Matrix44 aClass384185 = new Matrix44();
    public static final IterableNodeMap<VorbisNode> INDEX36_FILE_CACHE = new IterableNodeMap<VorbisNode>(4);
    public static int WORLD_LIST_IDK;
    public static int anInt2982;
    public static NativeSprite aNativeSprite4110;
    public static FontCombo initializedFontcombos;
    public static int anInt5760;
    public static NativeSprite aNativeSprite612;
    public static int anInt833;
    public static short aShort828;
    public static short aShort829;
    public static short aShort830;
    public static int anInt824;
    public static boolean aBool826;
    public static int anInt72 = -2;
    public static int anInt106 = -2;
    public static int anInt107 = -2;
    public static int anInt108 = -1;
    public static int anInt109 = -1;
    public static int anInt113;
    public static boolean aBool74;
    public static int anInt76 = -1;
    public static String aString99 = "";
    public static String aString102 = "";
    public static int anInt118;
    public static Loader initialApplet;
    public static long aLong10459;
    public static Js5ResourceProvider SPRITES_INDEX;

    public static void method6907() {
        Resource.mapRegion.resetInteractiveEntities();
        ComponentMinimap.reset();
        TypeLists.resetCachedData(true);
        StaticMethods.method4250();
        System.gc();
        Renderers.activeToolkit.applyClearFlags(2, 0);
    }
    
    public static void method813(int i_0, int i_1, int i_2, int i_3) {
        int i_5 = 0;
        int i_6 = i_2;
        int i_7 = -i_2;
        int i_8 = -1;
        ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_1], i_0 - i_2, i_0 + i_2, i_3);
        while (i_6 > i_5) {
            i_8 += 2;
            i_7 += i_8;
            ++i_5;
            if (i_7 >= 0) {
                --i_6;
                i_7 -= i_6 << 1;
                int[] ints_9 = Class149.anIntArrayArray7072[i_6 + i_1];
                int[] ints_10 = Class149.anIntArrayArray7072[i_1 - i_6];
                int i_11 = i_0 + i_5;
                int i_12 = i_0 - i_5;
                ArrayUtils.setArrayValuesTo(ints_9, i_12, i_11, i_3);
                ArrayUtils.setArrayValuesTo(ints_10, i_12, i_11, i_3);
            }
            int i_13 = i_0 + i_6;
            int i_14 = i_0 - i_6;
            int[] ints_15 = Class149.anIntArrayArray7072[i_5 + i_1];
            int[] ints_16 = Class149.anIntArrayArray7072[i_1 - i_5];
            ArrayUtils.setArrayValuesTo(ints_15, i_14, i_13, i_3);
            ArrayUtils.setArrayValuesTo(ints_16, i_14, i_13, i_3);
        }
    }
	
    public static void drawTileMessages(int x, int y) {
        for (TileMessage msg = (TileMessage) client.TILE_MESSAGES.getFirst(); msg != null; msg = (TileMessage) client.TILE_MESSAGES.getNext()) {
            if (client.cycles >= msg.lastCycle) {
                msg.unlinkNode();
            } else {
                HintRenderer.calculateHintArrowPlacement(msg.plane, (msg.absoluteX << 9) + 256, (msg.absoluteY << 9) + 256, msg.tileHeight * 2);
                MiniMenu.minimenuHeader.renderCentered(msg.minimenuText, (int) (x + client.HINT_POSITION[0]), (int) (y + client.HINT_POSITION[1]), -16777216 | msg.minimenuColor, 0);
            }
        }
    }

    public static void method6731(int i_0, int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        if (i_0 >= TextureOpRasterizer.anInt7071 && i_1 <= TextureOpRasterizer.anInt7069 && i_2 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068) {
            ArrayUtils.method3230(i_0, i_1, i_2, i_3, i_4, i_5, i_6);
        } else {
            ArrayUtils.method4034(i_0, i_1, i_2, i_3, i_4, i_5, i_6);
        }
    }

    public static void method3936(int[][] ints_0) {
        Class149.anIntArrayArray7072 = ints_0;
    }

    public static int method7914(long long_0) {
        return (int) (long_0 / 86400000L) - 11745;
    }

    public static void method7039() {
        InterfaceComponentRenderer.aClass118Array3772 = null;
        Component.method3629(client.BASE_WINDOW_ID, 0, 0, GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379, 0, 0, -1);
        if (InterfaceComponentRenderer.aClass118Array3772 != null) {
            Component[] arr_1 = InterfaceComponentRenderer.aClass118Array3772;
            int i_2 = -1412584499;
            byte b_3 = 0;
            byte b_4 = 0;
            int i_5 = GameShell.screenCanvasWidth;
            int i_6 = client.screenCanvasHeight * -969250379;
            int i_7 = InterfaceComponentRenderer.anInt4822;
            int i_8 = InterfaceComponentRenderer.anInt5157;
            int i_9;
            if (client.aClass1187247 == client.frameComponent) {
                i_9 = -1;
            } else {
                i_9 = client.aClass1187247.cacheReference;
            }

            InterfaceComponentRenderer.drawComponents(arr_1, i_2, b_3, b_4, i_5, i_6, i_7, i_8, i_9, true);
            InterfaceComponentRenderer.aClass118Array3772 = null;
        }

    }

    public static int method755(char var_3) {
        return 68 | 22528 | 5505024 | (var_3 & 0xff) << 24;
    }

    public static String method4824(byte[] bytes_0, int i_1, int i_2) {
        char[] arr_4 = new char[i_2];
        int i_5 = 0;
        int i_6 = i_1;
        int i_9;
        for (int i_7 = i_2 + i_1; i_6 < i_7; arr_4[i_5++] = (char) i_9) {
            int i_8 = bytes_0[i_6++] & 0xff;
            if (i_8 < 128) {
                if (i_8 == 0) {
                    i_9 = 65533;
                } else {
                    i_9 = i_8;
                }
            } else if (i_8 < 192) {
                i_9 = 65533;
            } else if (i_8 < 224) {
                if (i_6 < i_7 && (bytes_0[i_6] & 0xc0) == 128) {
                    i_9 = (i_8 & 0x1f) << 6 | bytes_0[i_6++] & 0x3f;
                    if (i_9 < 128) {
                        i_9 = 65533;
                    }
                } else {
                    i_9 = 65533;
                }
            } else if (i_8 < 240) {
                if (i_6 + 1 < i_7 && (bytes_0[i_6] & 0xc0) == 128 && (bytes_0[i_6 + 1] & 0xc0) == 128) {
                    i_9 = (i_8 & 0xf) << 12 | (bytes_0[i_6++] & 0x3f) << 6 | bytes_0[i_6++] & 0x3f;
                    if (i_9 < 2048) {
                        i_9 = 65533;
                    }
                } else {
                    i_9 = 65533;
                }
            } else if (i_8 < 248) {
                if (i_6 + 2 < i_7 && (bytes_0[i_6] & 0xc0) == 128 && (bytes_0[i_6 + 1] & 0xc0) == 128 && (bytes_0[i_6 + 2] & 0xc0) == 128) {
                    i_9 = (i_8 & 0x7) << 18 | (bytes_0[i_6++] & 0x3f) << 12 | (bytes_0[i_6++] & 0x3f) << 6 | bytes_0[i_6++] & 0x3f;
                    i_9 = 65533;
                } else {
                    i_9 = 65533;
                }
            } else {
                i_9 = 65533;
            }
        }
        return new String(arr_4, 0, i_5);
    }

    public static int method5360(int randomInteger, int limit) {
        int offset = randomInteger >> 31 & limit - 1;
        return (randomInteger + (randomInteger >>> 31)) % limit + offset;
    }

    public static boolean method6176(int i_0) {
        return i_0 == 44 || i_0 == 45 || i_0 == 46 || i_0 == 47 || i_0 == 48 || i_0 == 49 || i_0 == 50 || i_0 == 51 || i_0 == 52 || i_0 == 53 || i_0 == 15;
    }

    public static void method6772(int i_0, int i_1, int i_2, int i_3, int i_4) {
        if (i_0 >= TextureOpRasterizer.anInt7071 && i_1 <= TextureOpRasterizer.anInt7069 && i_2 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068) {
            method1564(i_0, i_1, i_2, i_3, i_4);
        } else {
            ArrayUtils.method7728(i_0, i_1, i_2, i_3, i_4);
        }

    }

    public static void method8209(AbstractToolkit toolkit, int i_1, int i_2, int i_3, int i_4, int i_5) {
        if ((aNativeSprite4110 == null || StaticMedia.aNativeSprite5339 == null || StaticMethods.aNativeSprite1571 == null) && Resource.SPRITES.isFileCached(StaticMethods.anInt3145) && Resource.SPRITES.isFileCached(StaticMethods.anInt5396) && Resource.SPRITES.isFileCached(StaticMethods.anInt148)) {
            SpriteType spriteType = SpriteType.listSingle(Resource.SPRITES, StaticMethods.anInt5396, 0);
            StaticMedia.aNativeSprite5339 = toolkit.generateSprite(spriteType, true);
            spriteType.flipHorizontal();
            StaticMedia.aNativeSprite3452 = toolkit.generateSprite(spriteType, true);
            aNativeSprite4110 = toolkit.generateSprite(SpriteType.listSingle(Resource.SPRITES, StaticMethods.anInt3145, 0), true);
            SpriteType class9111 = SpriteType.listSingle(Resource.SPRITES, StaticMethods.anInt148, 0);
            StaticMethods.aNativeSprite1571 = toolkit.generateSprite(class9111, true);
            class9111.flipHorizontal();
            StaticMethods.aNativeSprite9315 = toolkit.generateSprite(class9111, true);
        }
        if (aNativeSprite4110 != null && StaticMedia.aNativeSprite5339 != null && StaticMethods.aNativeSprite1571 != null) {
            int i_7 = (i_3 - StaticMethods.aNativeSprite1571.getWidth() * 2) / aNativeSprite4110.getWidth();
            int i_8;
            for (i_8 = 0; i_8 < i_7; i_8++) {
                aNativeSprite4110.draw(i_1 + StaticMethods.aNativeSprite1571.getWidth() + i_8 * aNativeSprite4110.getWidth(), i_2 + i_4 - aNativeSprite4110.getHeight());
            }
            i_8 = (i_4 - i_5 - StaticMethods.aNativeSprite1571.getHeight()) / StaticMedia.aNativeSprite5339.getHeight();
            for (int i_9 = 0; i_9 < i_8; i_9++) {
                StaticMedia.aNativeSprite5339.draw(i_1, i_5 + i_2 + i_9 * StaticMedia.aNativeSprite5339.getHeight());
                StaticMedia.aNativeSprite3452.draw(i_3 + i_1 - StaticMedia.aNativeSprite3452.getWidth(), i_5 + i_2 + i_9 * StaticMedia.aNativeSprite5339.getHeight());
            }
            StaticMethods.aNativeSprite1571.draw(i_1, i_2 + i_4 - StaticMethods.aNativeSprite1571.getHeight());
            StaticMethods.aNativeSprite9315.draw(i_3 + i_1 - StaticMethods.aNativeSprite1571.getWidth(), i_2 + i_4 - StaticMethods.aNativeSprite1571.getHeight());
        }
    }

    public static void method14572(int i_0, int i_1, int i_2, int i_3, int i_4) {
        ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_2++], i_0, i_1, i_4);
        ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_3--], i_0, i_1, i_4);

        for (int i_6 = i_2; i_6 <= i_3; i_6++) {
            int[] ints_7 = Class149.anIntArrayArray7072[i_6];
            ints_7[i_0] = ints_7[i_1] = i_4;
        }

    }

    public static void method14501(int i_0, int i_1) {
        SongPlayer.method2580(i_0, i_1);
    }

    public static boolean method1169(char var_0) {
        return var_0 >= 65 && var_0 <= 90 || var_0 >= 97 && var_0 <= 122;
    }

    public static CheckEmailRequest method14668() {
        return AccountCreationStage.checkemailRequeststatus == null ? CheckEmailRequest.DEFAULT : AccountCreationStage.checkemailRequeststatus;
    }

    public static void method1564(int i_0, int i_1, int i_2, int i_3, int i_4) {
        for (int i_6 = i_2; i_6 <= i_3; i_6++) {
            ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072[i_6], i_0, i_1, i_4);
        }
    }

    public static int method304() {
        return WorldMapSub1.requestedMapZoom == 3.0D ? 37 : (WorldMapSub1.requestedMapZoom == 4.0D ? 50 : (WorldMapSub1.requestedMapZoom == 6.0D ? 75 : (WorldMapSub1.requestedMapZoom == 8.0D ? 100 : 200)));
    }

    public static void method3850(Matrix44 matrix440, float centerX, float centerY, float f_4, float f_5, int width, int length) {
        int viewRangeEnd = Resource.mapRegion.getViewRangeEnd();
        int viewRangeStart = Resource.mapRegion.getViewRangeStart();
        matrix440.method6531(centerX, centerY, f_4, f_5, viewRangeStart, viewRangeEnd, width, length);
    }

    public static int method488(int i_0, int i_1) {
        return i_1 < 0 ? i_0 : (int) (i_0 * Math.sqrt(1.220703125E-4D * (16384 - i_1)) + 0.5D);
    }

    public static void method458(int i_0) {
        WorldMapSub1.someMecId = i_0;
        WorldMapSub1.anInt8026 = -1;
        JSObjectManager.anInt4185 = 3;
        ServerEnvironment.anInt9282 = 100;
    }

    public static void method8020() {
        MusicSample.method7042();
        method15420();
        SoundEffectPlayer.updatePlayers();
        AreaSoundPlayer.method5961();
    }

    public static boolean method12402(char var_0) {
        return var_0 >= 32 && var_0 <= 126 || (var_0 >= 160 && var_0 <= 255 || (var_0 == 8364 || var_0 == 338 || var_0 == 8212 || var_0 == 339 || var_0 == 376));
    }

    public static void method8146() {
        if (!client.aBool7269) {
            client.aFloat7266 += (-24.0f - client.aFloat7266) / 2.0F;
            client.sendCameraAngleToServer = true;
            client.aBool7269 = true;
        }
    }

    public static void method15430(int i_0) {
        if (GameState.method6693()) {
            if (i_0 != anInt76) {
                LoginManager.aLong77 = -1L;
            }

            anInt76 = i_0;
            client.LOBBY_CONNECTION_CONTEXT.closeConnection();
            client.setGameState(GameState.UNK_14);
        }

    }

    public static void method16114() {
        if (!client.aBool7331) {
            client.aFloat7284 += (-12.0f - client.aFloat7284) / 2.0F;
            client.sendCameraAngleToServer = true;
            client.aBool7331 = true;
        }

    }

    public static void method6292(int i_0, int i_1, int i_2, int i_3) {
        if (i_0 - i_2 >= TextureOpRasterizer.anInt7071 && i_0 + i_2 <= TextureOpRasterizer.anInt7069 && i_1 - i_2 >= TextureOpRasterizer.anInt7070 && i_2 + i_1 <= TextureOpRasterizer.anInt7068) {
        	method813(i_0, i_1, i_2, i_3);
        } else {
            ArrayUtils.method4866(i_0, i_1, i_2, i_3);
        }
    }

    public static void method14263(int i_0, int i_1) {
        WorldMapSub1.anInt8014 = i_0 - WorldMapSub1.mapSquareAbsX;
        WorldMapSub1.anInt8016 = i_1 - WorldMapSub1.mapSquareAbsY;
    }

    public static void queryItemNoParams(String name, boolean tradeable) {
        StaticMethods.queryItemParamInt(name, tradeable, -1, -1);
    }

    public static void method15420() {
        try {
            if (SoundResourceManager.anInt1730 == 1) {
                int volume = SoundResourceManager.aNodeSub15Sub21735.getMusicVolume();
                if (volume > 0 && SoundResourceManager.aNodeSub15Sub21735.dataLoaded()) {
                    volume -= CutsceneBasicMovement.anInt9440;
                    if (volume < 0) {
                        volume = 0;
                    }
                    SoundResourceManager.aNodeSub15Sub21735.setMusicVolume(volume);
                    return;
                }
                SoundResourceManager.aNodeSub15Sub21735.turnOffSound();
                SoundResourceManager.aNodeSub15Sub21735.unlinkMusicPatches();
                if (AreaSoundPlayer.MUSIC_RESOURCE != null) {
                    SoundResourceManager.anInt1730 = 2;
                } else {
                    SoundResourceManager.anInt1730 = 0;
                }
                StaticMethods.aNodeSub79563 = null;
                StaticMethods.aClass2505450 = null;
            }
            if (SoundResourceManager.anInt1730 == 3) {
                int volume = SoundResourceManager.aNodeSub15Sub21735.getMusicVolume();
                if (volume < AreaSoundPlayer.SOUND_LEVEL && SoundResourceManager.aNodeSub15Sub21735.dataLoaded()) {
                    volume += StaticMethods.anInt4664;
                    if (volume > AreaSoundPlayer.SOUND_LEVEL) {
                        volume = AreaSoundPlayer.SOUND_LEVEL;
                    }
                    SoundResourceManager.aNodeSub15Sub21735.setMusicVolume(volume);
                } else {
                	StaticMethods.anInt4664 = 0;
                    SoundResourceManager.anInt1730 = 0;
                }
            }
        } catch (Exception exception3) {
            exception3.printStackTrace();
            SoundResourceManager.aNodeSub15Sub21735.turnOffSound();
            SongReference.reset();
        }
    }

    public static int method15390(int i_0, int i_1) {
        return i_1 < 0 ? -i_0 : (int) (i_0 * Math.sqrt(1.220703125E-4D * i_1) + 0.5D);
    }

    public static void method3953() {
        aShort828 = client.aShort7214;
        aShort829 = client.aShort7474;
        AbstractMousePacket.aShort7079 = client.aShort7276;
        aShort830 = client.aShort7441;
        aBool826 = true;
        if (anInt824 != 0 && CutsceneTypeList.anInt825 != 0) {
            client.aShort7276 = 334;
            client.aShort7441 = 334;
            client.aShort7474 = client.aShort7214 = (short) (anInt824 * 512 / CutsceneTypeList.anInt825);
        }
    }

    public static boolean method6140() {
        return SoundResourceManager.anInt1730 != 0;
    }

    public static void method4164(int i_0, int i_1) {
        if (MiniMenu.anInt167 == 1) {
            MenuInteraction.processMenuclick(MiniMenu.aCacheableNodeSub7438, i_0, i_1);
        } else if (MiniMenu.anInt167 == 2) {
            StaticMethods.method4653(i_0, i_1);
        }
        MiniMenu.anInt167 = 0;
        MiniMenu.aCacheableNodeSub7438 = null;
    }

    public static void method4166() {
        if (!client.aBool7331) {
            client.aFloat7284 += (12.0F - client.aFloat7284) / 2.0F;
            client.sendCameraAngleToServer = true;
            client.aBool7331 = true;
        }
    }

    public static boolean method4180(GameState i_0) {
        return i_0 == GameState.UNK_7 || i_0 == GameState.UNK_6;
    }

    public static void method1630(int i_0, int i_1, int i_2, int i_4, int i_6, ColoredMiniMenuEntry class282Sub50Sub157, AbstractFontRenderer fontrenderer8, FontMetrics fontmetrics9, int i_10, int i_11) {
	    int i_101 = i_10;
	    if (i_0 > i_2 && i_0 < i_2 + i_4 && i_1 > i_6 - fontmetrics9.topPadding - 1 && i_1 < fontmetrics9.bottomPadding + i_6) {
	        i_101 = i_11;
	    }
	    String string_13 = MiniMenu.formatTag(class282Sub50Sub157);
	    fontrenderer8.method386(string_13, i_2 + 3, i_6, i_101, client.RANDOMIZER, client.randomSeed, StaticMedia.chatIcons, StaticMedia.chatScaleHeights);
	}

	public static void method4653(int mouseX, int mouseY) {
	    if (client.ENTITY_DEFAULTS.aBool7044 || MiniMenu.entryCount != 1 && (!MiniMenuBuilder.SHOW_FACE_HERE || MiniMenu.entryCount != 2 || !MiniMenu.aCacheableNodeSub712.action.equals(LocalizedText.FACE_HERE.translate(client.CURRENT_LANGUAGE)))) {
	        FontMetrics fontMetrics = FontMetrics.getForMinimenu();
	        int i_4 = fontMetrics.getWidthNoSprites(LocalizedText.CHOOSE_OPTION.translate(client.CURRENT_LANGUAGE));
	        int i_5;
	        int i_7;
	        if (!MiniMenu.aBool162) {
	            for (MiniMenuEntry class282Sub50Sub79 = (MiniMenuEntry) MiniMenu.aClass482171.getFirst(); class282Sub50Sub79 != null; class282Sub50Sub79 = (MiniMenuEntry) MiniMenu.aClass482171.next()) {
	                i_7 = MiniMenu.getWidthForEntryWithQuests(class282Sub50Sub79, fontMetrics);
	                if (i_7 > i_4) {
	                    i_4 = i_7;
	                }
	            }
	            i_4 += 8;
	            i_5 = MiniMenu.entryCount * MiniMenu.anInt178 + 21;
	            MiniMenu.anInt1104 = (FontMetrics.useBoldFont ? 26 : 22) + MiniMenu.entryCount * MiniMenu.anInt178;
	        } else {
	            for (ColoredMiniMenuEntry menuEntry = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getNext(); menuEntry != null; menuEntry = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getDirectNext()) {
	                if (menuEntry.queueSize == 1) {
	                    i_7 = MiniMenu.getWidthForEntryWithQuests((MiniMenuEntry) menuEntry.nodeQueue.current.next, fontMetrics);
	                } else {
	                    i_7 = MiniMenu.getPrefixTextWidth(menuEntry, fontMetrics);
	                }
	                if (i_7 > i_4) {
	                    i_4 = i_7;
	                }
	            }
	            i_4 += 8;
	            i_5 = MiniMenu.anInt178 * MiniMenu.anInt170 + 21;
	            MiniMenu.anInt1104 = MiniMenu.anInt178 * MiniMenu.anInt170 + (FontMetrics.useBoldFont ? 26 : 22);
	        }
	        i_4 += 10;
	        int i_8 = mouseX - i_4 / 2;
	        if (i_8 + i_4 > GameShell.screenCanvasWidth) {
	            i_8 = GameShell.screenCanvasWidth - i_4;
	        }
	        if (i_8 < 0) {
	            i_8 = 0;
	        }
	        i_7 = mouseY;
	        if (mouseY + i_5 > GameShell.screenCanvasHeight * -969250379) {
	            i_7 = GameShell.screenCanvasHeight * -969250379 - i_5;
	        }
	        if (i_7 < 0) {
	            i_7 = 0;
	        }
	        StaticMethods.anInt3555 = i_8;
	        StaticMethods.anInt2300 = i_7;
	        MiniMenu.anInt8975 = i_4;
	        NativeSprite.anInt156 = (int) (Math.random() * 24.0D);
	        MiniMenu.isOpen = true;
	    }
	}
	
	public static int[] anIntArray36;
	public static SoundBankCache aClass2505450;
	public static int anInt2262;
	public static int anInt1005;

	public static int anInt148;
	public static int anInt3555;
	public static int anInt4664;
	public static boolean aBool1962;

	public static String decodeFriendchatName(CharSequence charsequence_0) {
	    String string_2 = TextUtils.decodeUidName(Utils.charseqTouid(charsequence_0));
	    if (string_2 == null) {
	        string_2 = "";
	    }
	    return string_2;
	}

	public static int method7931(Random random, int limit) {
	    if (limit <= 0) {
	        throw new IllegalArgumentException();
	    } 
	    if (OpenGlTexture.isPowerOfTwo(limit)) {
	        return (int) ((random.nextInt() & 0xffffffffL) * limit >> 32);
	    } 
        int i_3 = Integer.MIN_VALUE - (int) (4294967296L % limit);
        int randomInteger;
        do {
            randomInteger = random.nextInt();
        } while (randomInteger >= i_3);
        return method5360(randomInteger, limit);
	}

	public static void method4250() {
	    for (VorbisNode vorbis = INDEX36_FILE_CACHE.first(); vorbis != null; vorbis = INDEX36_FILE_CACHE.next()) {
	        StaticMethods.method1563(vorbis.fileId);
	    }
	}

	public static int anInt5396;

	public static void setVorbisSound(int fileId) {
	    VorbisNode vorbis = INDEX36_FILE_CACHE.get(fileId);
	    if (vorbis != null) {
	        vorbis.aBool7995 = !vorbis.aBool7995;
	        vorbis.vfReference.method4924(vorbis.aBool7995);
	    }
	}

	public static NativeSprite aNativeSprite9315;

	public static void method1563(int fileId) {
	    VorbisNode varbisNode = INDEX36_FILE_CACHE.get(fileId);
	    if (varbisNode != null) {
	        varbisNode.vfReference.method4947();
	        SoundTrigger.playVorbis(varbisNode.fileId, varbisNode.sendPacket);
	        varbisNode.unlink();
	    }
	}

	public static int anInt2300;
	public static MusicTrack aNodeSub79563;
	public static NativeSprite aNativeSprite1571;
	public static int anInt3145;
	public static int anInt2912;
	public static int anInt5438;

	public static void queryItemParamInt(String name, boolean tradeableOnly, int paramId, int paramIntQuery) {
	    CS2Interpreter.queryItem(name, tradeableOnly, paramId, paramIntQuery, null, false);
	}
}
