package com.jagex.game.runetek5.client;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.DefaultsFile;
import com.jagex.game.runetek5.config.IndexReference;
import com.jagex.game.runetek5.config.bas.BasTypeList;
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList;
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsTypeList;
import com.jagex.game.runetek5.config.cursor.CursorTypeList;
import com.jagex.game.runetek5.config.defaults.EntityDefaults;
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType;
import com.jagex.game.runetek5.config.defaults.InteractionDefaultsType;
import com.jagex.game.runetek5.config.enumtype.EnumTypeList;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.config.gametip.GameTipTypeList;
import com.jagex.game.runetek5.config.gametip.GameTipUpdateDetails;
import com.jagex.game.runetek5.config.hitbar.HitbarTypeList;
import com.jagex.game.runetek5.config.hitsplat.HitSplatTypeList;
import com.jagex.game.runetek5.config.identitykit.IdkTypeList;
import com.jagex.game.runetek5.config.inputsubscriber.CutsceneDefaultsType;
import com.jagex.game.runetek5.config.light.LightTypeList;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.config.mapsprite.MapSpriteTypeList;
import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.config.material.texture.TextureTypeList;
import com.jagex.game.runetek5.config.mec.MECTypeList;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.param.ParamTypeList;
import com.jagex.game.runetek5.config.quest.QuestTypeList;
import com.jagex.game.runetek5.config.quickchat.QuickchatCategoryTypeList;
import com.jagex.game.runetek5.config.quickchat.QuickchatFiller;
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageTypeList;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentType;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerTypeList;
import com.jagex.game.runetek5.config.sequence.SeqTypeList;
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimTypeList;
import com.jagex.game.runetek5.config.skybox.SkyboxTypeList;
import com.jagex.game.runetek5.config.struct.StructTypeList;
import com.jagex.game.runetek5.config.sun.SunTypeList;
import com.jagex.game.runetek5.config.varc.*;
import com.jagex.game.runetek5.config.varn.VarnBitTypeList;
import com.jagex.game.runetek5.config.varn.VarnTypeList;
import com.jagex.game.runetek5.config.varp.VarpTypeList;
import com.jagex.game.runetek5.config.varp.VarpbitTypeList;
import com.jagex.game.runetek5.encoding.Huffman;
import com.jagex.game.runetek5.encoding.HuffmanList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.FontCombo;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.loginscreen.DrawableGameTip;
import com.jagex.game.runetek5.graphics.loginscreen.GameTipsLoader;
import com.jagex.game.runetek5.graphics.loginscreen.LoginScreenComponent;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.icmp.IcmpServiceJaclib;
import com.jagex.game.runetek5.js5.dependency.CacheableDLL;
import com.jagex.game.runetek5.js5.dependency.CacheableDependencies;
import com.jagex.game.runetek5.js5.dependency.CacheableHuffman;
import com.jagex.game.runetek5.js5.dependency.CacheableResource;
import com.jagex.game.runetek5.js5.dependency.CacheableWorldMap;
import com.jagex.game.runetek5.js5.dependency.Dependency;
import com.jagex.game.runetek5.js5.library.NativeLibraryLoader;
import com.jagex.game.runetek5.js5.request.RequestManager;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.environment.EnvironmentManager;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.vardomain.PlayerVarDomain;
import com.jagex.game.runetek5.vardomain.VarProviders;

import jaclib.ping.Ping;

public class ClientStartup {

    public static final int[] PERCENT_SPLIT_REFERENCE_TABLE_SIZE = {4, 4, 1, 2, 6, 4, 2, 44, 2, 2, 2, 2, 2, 1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1};
    public static int referenceTableLoadingCompletion = -1;
    
	public static Dependency[] fullyCachedDependencies;
	public static int loadingPercentDependencies;
	public static int loadingStatusDependencies;
	public static client activeClient;

    public static int initialize() {
        if (PreferenceManager.preferences.safeMode.getValue() == 0) {
            for (int i = 0; i < client.keyCachePointerPoss; i++) {
                if (client.KEYS_PRESSED[i].getCharacter() == 115 || client.KEYS_PRESSED[i].getCharacter() == 83) {
                    PreferenceManager.preferences.setValue(PreferenceManager.preferences.safeMode, 1);
                    client.safemodeForcedDuringStartup = true;
                    break;
                }
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_JS5) {
            if (client.requestManager == null) {
                client.requestManager = new RequestManager(Resource.remoteFileRequesterJs5, Resource.localRequesterJs5);
            }

            if (!client.requestManager.initialize()) {
                return 0;
            }

            Renderers.prepareForRendering(0, null, true);
            Resource.USING_JAGEX_IMAGE_FORMAT = !NativeSprite.supportsGzippedSprites();
            Resource.FONT = Resource.createProvider(Resource.USING_JAGEX_IMAGE_FORMAT ? IndexReference.INDEX_34 : IndexReference.INDEX_32, false, 1);
            Resource.GAME_TIPS = Resource.createProvider(IndexReference.INDEX_33, false, 1);
            Resource.FONT_METRICS = Resource.createProvider(IndexReference.INDEX_13, false, 1);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_TIPS) {
        	boolean tipsLoaded = Resource.GAME_TIPS.fullyLoaded();
            int stageCompletionPercent = Resource.remoteFileWorkersByIndex[IndexReference.INDEX_33.getIndex()].getReferenceTableCompletion();
            stageCompletionPercent += Resource.remoteFileWorkersByIndex[Resource.USING_JAGEX_IMAGE_FORMAT ? IndexReference.INDEX_34.getIndex() : IndexReference.INDEX_32.getIndex()].getReferenceTableCompletion();
            stageCompletionPercent += Resource.remoteFileWorkersByIndex[IndexReference.INDEX_13.getIndex()].getReferenceTableCompletion();
            stageCompletionPercent += tipsLoaded ? 100 : Resource.GAME_TIPS.getLoadingPercentage();
            if (stageCompletionPercent != 400) {
                return stageCompletionPercent / 4;
            }

            Resource.fontCrc = Resource.FONT.getCrc();
            Resource.gameTipsCrc = Resource.GAME_TIPS.getCrc();
            FontManager.loadFontArchiveIds(Resource.FONT);
            int tipsStartStagePreference = PreferenceManager.preferences.gametipStartStagePreference.getValue();
            TypeLists.GAME_TIPS = new GameTipTypeList(Resource.GAME_TIPS);
            GameTipUpdateDetails[] updateDetails = TypeLists.GAME_TIPS.getUpdateDetails(tipsStartStagePreference);
            if (updateDetails.length == 0) {
                updateDetails = TypeLists.GAME_TIPS.getUpdateDetails(0);
            }

            GameTipsLoader tipsLoader = new GameTipsLoader(Resource.FONT, Resource.FONT_METRICS);
            if (updateDetails.length > 0) {
                LoadingStage.gametipsToDisplay = new LoginScreenComponent[updateDetails.length];

                for (int index = 0; index < LoadingStage.gametipsToDisplay.length; index++) {
                    LoadingStage.gametipsToDisplay[index] = new DrawableGameTip(TypeLists.GAME_TIPS.list(
                		updateDetails[index].tipFileId),
                		updateDetails[index].displayDurationMs,
                		updateDetails[index].timeBetweenUpdatesMs,
                            tipsLoader
                    );
                }
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CREATE_FONTS) {
            StaticMethods.initializedFontcombos = new FontCombo(Resource.FONT, Resource.FONT_METRICS, FontManager.getFontSpecifications());
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CACHE_FONTS) {
        	int cached = StaticMethods.initializedFontcombos.getNumCached();
            int total = StaticMethods.initializedFontcombos.getFontCount();
            if (cached < total) {
                return cached * 100 / total;
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.PREPARE_FONTS) {
            if (LoadingStage.gametipsToDisplay != null && LoadingStage.gametipsToDisplay.length > 0) {
                if (LoadingStage.gametipsToDisplay[0].calculateLoadingPercent() < 100) {
                    return 0;
                }

                if (LoadingStage.gametipsToDisplay.length > 1 && TypeLists.GAME_TIPS.hasTipsFile() && LoadingStage.gametipsToDisplay[1].calculateLoadingPercent() < 100) {
                    return 0;
                }
            }

            StaticMethods.initializedFontcombos.initializeFontMap(client.FONT_PROVIDER);
            FontManager.initialize();
            client.setGameState(GameState.UNK_15);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CREATE_JS5) {
            Resource.SPRITES = Resource.createProvider(IndexReference.INDEX_8, false, 1);
            Resource.FRAME_SET = Resource.createProvider(IndexReference.INDEX_0, false, 1);
            Resource.FRAME_BASE = Resource.createProvider(IndexReference.INDEX_1, false, 1);
            Resource.CONFIG = Resource.createProvider(IndexReference.INDEX_2, false, 1);
            Resource.INTERFACE = Resource.createProvider(IndexReference.INDEX_3, false, 1);
            Resource.SOUND_EFFECT = Resource.createProvider(IndexReference.INDEX_4, false, 1);
            Resource.MAPS = Resource.createProvider(IndexReference.INDEX_5, true, 1);
            Resource.MUSIC = Resource.createProvider(IndexReference.INDEX_6, true, 1);
            Resource.MODEL = Resource.createProvider(IndexReference.INDEX_7, false, 1);
            Resource.TEXTURE = Resource.createProvider(IndexReference.INDEX_9, false, 1);
            Resource.HUFFMAN = Resource.createProvider(IndexReference.INDEX_10, false, 1);
            Resource.JINGLE = Resource.createProvider(IndexReference.INDEX_11, false, 1);
            Resource.CS2 = Resource.createProvider(IndexReference.INDEX_12, false, 1);
            Resource.MIDI_INSTRUMENT = Resource.createProvider(IndexReference.INDEX_14, false, 1);
            Resource.SOUND_MIDI = Resource.createProvider(IndexReference.INDEX_15, false, 1);
            Resource.LOC = Resource.createProvider(IndexReference.INDEX_16, false, 1);
            Resource.ENUM = Resource.createProvider(IndexReference.INDEX_17, false, 1);
            Resource.NPC = Resource.createProvider(IndexReference.INDEX_18, false, 1);
            Resource.OBJ = Resource.createProvider(IndexReference.INDEX_19, false, 1);
            Resource.SEQ = Resource.createProvider(IndexReference.INDEX_20, false, 1);
            Resource.GFX = Resource.createProvider(IndexReference.INDEX_21, false, 1);
            Resource.VARBIT = Resource.createProvider(IndexReference.INDEX_22, false, 1);
            Resource.WORLD_MAP = Resource.createProvider(IndexReference.INDEX_23, true, 1);
            Resource.QC_MESSAGES = Resource.createProvider(IndexReference.INDEX_24, false, 1);
            Resource.QC_MENU = Resource.createProvider(IndexReference.INDEX_25, false, 1);
            Resource.MATERIAL = Resource.createProvider(IndexReference.INDEX_26, true, 1);
            Resource.PARTICLE = Resource.createProvider(IndexReference.INDEX_27, false, 1);
            Resource.DEFAULTS = Resource.createProvider(IndexReference.INDEX_28, true, 1);
            Resource.BILLBOARD = Resource.createProvider(IndexReference.INDEX_29, false, 1);
            Resource.CUTSCENE = Resource.createProvider(IndexReference.INDEX_35, true, 1);
            Resource.NATIVE_LIBRARY = Resource.createProvider(IndexReference.INDEX_30, true, 1);
            Resource.SHADER = Resource.createProvider(IndexReference.INDEX_31, true, 1);
            Resource.VORBIS = Resource.createProvider(IndexReference.INDEX_36, true, 2);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_SPRITES) {
        	int completion = 0;
            for (int index = 0; index < Resource.remoteFileWorkersByIndex.length; index++) {
                if (Resource.remoteFileWorkersByIndex[index] != null) {
                    completion += Resource.remoteFileWorkersByIndex[index].getReferenceTableCompletion() * PERCENT_SPLIT_REFERENCE_TABLE_SIZE[index] / 100;
                }
            }
            if (completion != 100) {
                if (referenceTableLoadingCompletion < 0) {
                    referenceTableLoadingCompletion = completion;
                }
                return (completion - referenceTableLoadingCompletion) * 100 / (100 - referenceTableLoadingCompletion);
            }
            StaticMedia.loadGroupIds(Resource.SPRITES);
            StaticMethods.initializedFontcombos = new FontCombo(Resource.SPRITES, Resource.FONT_METRICS, FontManager.getFontSpecifications());
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_AUDIO) {
            byte[] data = Resource.DEFAULTS.getFile(DefaultsFile.TITLESCREEN_MUSIC_INFO.fileId);
            if (data == null) {
                return 0;
            }

            decodeTitlescreenMusic(data);
            SoundEffectPlayer.initializeAudio();
            client.setGameState(GameState.UNK_1);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.PREPARE_NATIVE_LIBRARY_LOADER) {
            Resource.NATIVE_LIBRARY_LOADER = new NativeLibraryLoader(Resource.NATIVE_LIBRARY);
            AbstractToolkit.setNativeLibraryLoader(Resource.NATIVE_LIBRARY_LOADER);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_NATIVES) {
        	int nativesLoadPercentage = ClientStartup.loadDependencies();
            if (nativesLoadPercentage < 100) {
                return nativesLoadPercentage;
            }

            EnvironmentManager.decodeCubeTextures(Resource.DEFAULTS.getFile(DefaultsFile.SCENE_CUBE_TEXTURES.fileId));
            client.ENTITY_DEFAULTS = new EntityDefaults(Resource.DEFAULTS);
            PlayerAppearance.recolorPaletteSrc = client.ENTITY_DEFAULTS.recolorPaletteSrc;
            PlayerAppearance.recolorPaletteDst = client.ENTITY_DEFAULTS.recolorPaletteDst;
            if (client.ENTITY_DEFAULTS.gameWidthDefault != -1 && client.ENTITY_DEFAULTS.gameHeightDefault != -1) {
                client.GAME_WIDTH = client.ENTITY_DEFAULTS.gameWidthDefault;
                client.GAME_HEIGHT = client.ENTITY_DEFAULTS.gameHeightDefault;
            }

            TypeLists.EQUIPMENT_DEFAULTS = new EquipmentDefaultsType(Resource.DEFAULTS);
            TypeLists.INTERACTION_DEFAULTS = new InteractionDefaultsType(Resource.DEFAULTS);
            TypeLists.CUTSCENE_DEFAULTS = new CutsceneDefaultsType(Resource.DEFAULTS);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_TYPELISTS) {
            if (client.ENTITY_DEFAULTS.profilingMesh != -1 && !Resource.MODEL.isCached(client.ENTITY_DEFAULTS.profilingMesh, 0)) {
                return 99;
            }

            TypeLists.MATERIALS = new MaterialTypeList(Resource.MATERIAL, Resource.TEXTURE, Resource.SPRITES);
            TypeLists.PARAMS = new ParamTypeList(Resource.CONFIG);
            TypeLists.BAS = new BasTypeList(Resource.CONFIG, TypeLists.EQUIPMENT_DEFAULTS);
            TypeLists.CURSOR = new CursorTypeList(Resource.CONFIG, Resource.SPRITES);
            TypeLists.ENUM = new EnumTypeList(Resource.ENUM);
            TypeLists.FLO = new FloTypeList(Resource.CONFIG);
            TypeLists.FLU = new FluTypeList(Resource.CONFIG);
            TypeLists.HITSPLAT = new HitSplatTypeList(Resource.CONFIG, Resource.SPRITES);
            TypeLists.HITBAR = new HitbarTypeList(Resource.CONFIG, Resource.SPRITES);
            TypeLists.IDK = new IdkTypeList(Resource.CONFIG, Resource.MODEL);
            TypeLists.CONTAINER = new ItemContainerTypeList(Resource.CONFIG);
            TypeLists.LIGHT = new LightTypeList(Resource.CONFIG);
            TypeLists.LOC = new ObjectTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, Resource.LOC, Resource.MODEL);
            Resource.mapRegion.setLoctypelist(TypeLists.LOC);
            Resource.mapRegionLoaderTask.initializeRegion(new ObjectTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, Resource.LOC, Resource.MODEL));
            TypeLists.MEC = new MECTypeList(Resource.CONFIG, Resource.SPRITES);
            TypeLists.MAP_SPRITE = new MapSpriteTypeList(Resource.CONFIG, Resource.SPRITES);
            TypeLists.NPC = new NpcTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, Resource.NPC, Resource.MODEL);
            TypeLists.OBJ = new ItemTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, TypeLists.PARAMS, Resource.OBJ, Resource.MODEL);
            TypeLists.QUEST = new QuestTypeList(Resource.CONFIG);
            TypeLists.SEQ = new SeqTypeList(Resource.SEQ, Resource.FRAME_SET, Resource.FRAME_BASE);
            TypeLists.SKYBOX = new SkyboxTypeList(Resource.CONFIG);
            TypeLists.SUN = new SunTypeList(Resource.CONFIG);
            TypeLists.SPOT_ANIM = new SpotAnimTypeList(Resource.GFX, Resource.MODEL);
            TypeLists.STRUCT = new StructTypeList(Resource.CONFIG);
            TypeLists.VARC_STRING = new VarcStringTypeList(Resource.CONFIG);
            TypeLists.VARC = new VarcTypeList(Resource.CONFIG);
            TypeLists.VARPBIT = new VarpbitTypeList(Resource.VARBIT);
            TypeLists.VAR = new VarpTypeList(Resource.CONFIG);
            TypeLists.VARN = new VarnTypeList(Resource.CONFIG);
            TypeLists.VARNBIT = new VarnBitTypeList(Resource.CONFIG);
            TypeLists.CLAN_VAR = new ClanVarTypeList(Resource.CONFIG);
            TypeLists.CLAN_VAR_SETTINGS = new ClanVarSettingsTypeList(Resource.CONFIG);
            Interface.initialize(Resource.INTERFACE, Resource.MODEL, Resource.SPRITES);
            BillboardTypeList.initialize(Resource.BILLBOARD);
            TypeLists.QUICK_CHAT_CATEGORY = new QuickchatCategoryTypeList(Resource.QC_MESSAGES, Resource.QC_MENU);
            TypeLists.QUICK_CHAT_MESSAGE = new QuickchatMessageTypeList(Resource.QC_MESSAGES, Resource.QC_MENU, new QuickchatFiller());
            VarProviders.PLAYER_VAR_PROVIDER = new PlayerVarDomain();
            GraphicsCache.resetGraphicCaches();
            SeqTypeList.initialize(TypeLists.SEQ);
            ParticleSystemManager.initializeIndex(Resource.PARTICLE);
            TextureTypeList.initialize(Resource.MODEL, TypeLists.MATERIALS);
            Huffman huffman = new Huffman(Resource.HUFFMAN.getNamedFile("huffman", ""));
            HuffmanList.initialize(huffman);
            GameShell.cycleManager = GameShell.getCycleManager();
            SystemInfo.SYSTEM_INFO = new SystemInfo();
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CACHE_STATIC_MEDIA) {
        	int staticMediaLoadPercent = StaticMedia.fullyCached(Resource.SPRITES) + StaticMethods.initializedFontcombos.getNumCached(true);
            int staticMediaCount = StaticMethods.initializedFontcombos.getFontCount() + 12;
            if (staticMediaLoadPercent < staticMediaCount) {
                return staticMediaLoadPercent * 100 / staticMediaCount;
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_WORLDMAP) {
            WorldMap.initialize(Resource.WORLD_MAP, TypeLists.FLO, TypeLists.FLU, Resource.mapRegion.getLoctypeList(), TypeLists.MEC, TypeLists.MAP_SPRITE, VarProviders.PLAYER_VAR_PROVIDER);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.DISCARD_UNPACKED) {
            VarcTypeList.VARC_INT = new int[TypeLists.VARC.size];
            VarcTypeList.VARC_BOOL = new boolean[TypeLists.VARC.size];
            VarcTypeList.VARC_STRING = new String[TypeLists.VARC_STRING.size];

            for (int varcId = 0; varcId < TypeLists.VARC.size; varcId++) {
                if (TypeLists.VARC.list(varcId).anInt4983 == 0) {
                    VarcTypeList.VARC_BOOL[varcId] = true;
                    client.VARCS_BOOLS_LOADED++;
                }

                VarcTypeList.VARC_INT[varcId] = -1;
            }

            VarcTypeList.readVarcIntPreferences();
            Resource.MAPS.discardUnpacked(false);
            Resource.MUSIC.discardUnpacked(true);
            Resource.SPRITES.discardUnpacked(true);
            Resource.FONT_METRICS.discardUnpacked(true);
            Resource.HUFFMAN.discardUnpacked(true);
            client.typelistCycleUpdate = true;
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.DISPLAY_LOGIN_WINDOW) {
            if (!Interface.interfaceLoaded(client.ENTITY_DEFAULTS.loginInterfaceId, null)) {
                return 0;
            }
            boolean interfaceCached = true;
            for (int index = 0; index < Interface.INTERFACES[client.ENTITY_DEFAULTS.loginInterfaceId].components.length; index++) {
                Component component = Interface.INTERFACES[client.ENTITY_DEFAULTS.loginInterfaceId].components[index];
                if (component.type == ComponentType.SPRITE && component.spriteId != -1 && !Resource.SPRITES.isCached(component.spriteId, 0)) {
                    interfaceCached = false;
                }
            }
            if (!interfaceCached) {
                return 0;
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_PLAYER) {
            ClientStartup.preparePlayer(true);
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.PREPARE_TOOLKIT) {
            LoadingStage.plainLoginScreenRenderer.stop();
            try {
                LoadingStage.loginScreenRendererThread.join();
            } catch (InterruptedException ie) {
            	ie.printStackTrace();
                return 0;
            }
            LoadingStage.plainLoginScreenRenderer = null;
            LoadingStage.loginScreenRendererThread = null;
            Resource.FONT = null;
            Resource.GAME_TIPS = null;
            TypeLists.GAME_TIPS = null;
            LoadingStage.gametipsToDisplay = null;
            NativeSprite.resetCache();
            client.isForcedSafemode = PreferenceManager.preferences.safeMode.getValue() == 1;
            PreferenceManager.preferences.setValue(PreferenceManager.preferences.safeMode, 1);
            if (client.isForcedSafemode) {
                PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, 0);
            } else if (PreferenceManager.preferences.toolkit.isDefault && SystemInfo.SYSTEM_INFO.ram < 512 && SystemInfo.SYSTEM_INFO.ram != 0) {
                PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, 0);
            }
            PreferenceManager.savePreferences();
            if (client.isForcedSafemode) {
                client.loadAssets(0, false);
            } else {
                client.loadAssets(PreferenceManager.preferences.toolkit.getValue(), false);
            }
            WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false);
            StaticMethods.initializedFontcombos.initializeFontMap(client.FONT_PROVIDER);
            FontManager.initialize();
            StaticMedia.loadSprites(Renderers.activeToolkit, Resource.SPRITES);
            StaticMedia.initializeChatIcons(StaticMedia.nameIconSprites);
        }

        return finalizeStartup();
    }
    
	public static int loadDependencies() {
	    if (ClientStartup.loadingStatusDependencies == 0) {
	        CacheableDependencies.JACLIB.inializeDependency(new CacheableDLL("jaclib"));
	        if (CacheableDependencies.JACLIB.getDependency().getCompletion() != 100) {
	            return 1;
	        }
	        if (!((CacheableDLL) CacheableDependencies.JACLIB.getDependency()).isReady()) {
	            ClientStartup.activeClient.startJaclib();
	            IcmpServiceJaclib.initialize(); 
	            try {
	                Ping.init();
	            } catch (Throwable throwable) {
	            	throwable.printStackTrace();
	            }
	        }
	        ClientStartup.loadingStatusDependencies = 1;
	    }
	    if (ClientStartup.loadingStatusDependencies == 1) {
	        ClientStartup.fullyCachedDependencies = CacheableDependencies.getAll();
	        CacheableDependencies.DEFAULTS.inializeDependency(new CacheableResource(Resource.DEFAULTS));
	        CacheableDependencies.JAGGL.inializeDependency(new CacheableDLL("jaggl"));
	        CacheableDependencies.JAGDX.inializeDependency(new CacheableDLL("jagdx"));
	        CacheableDependencies.SW3D.inializeDependency(new CacheableDLL("sw3d"));
	        CacheableDependencies.HW3D.inializeDependency(new CacheableDLL("hw3d"));
	        CacheableDependencies.JAGTHEORA.inializeDependency(new CacheableDLL("jagtheora"));
	        CacheableDependencies.SHADERS.inializeDependency(new CacheableResource(Resource.SHADER));
	        CacheableDependencies.MATERIALS.inializeDependency(new CacheableResource(Resource.MATERIAL));
	        CacheableDependencies.CONFIG.inializeDependency(new CacheableResource(Resource.CONFIG));
	        CacheableDependencies.LOC.inializeDependency(new CacheableResource(Resource.LOC));
	        CacheableDependencies.ENUM.inializeDependency(new CacheableResource(Resource.ENUM));
	        CacheableDependencies.NPC.inializeDependency(new CacheableResource(Resource.NPC));
	        CacheableDependencies.OBJ.inializeDependency(new CacheableResource(Resource.OBJ));
	        CacheableDependencies.SEQ.inializeDependency(new CacheableResource(Resource.SEQ));
	        CacheableDependencies.GFX.inializeDependency(new CacheableResource(Resource.GFX));
	        CacheableDependencies.VARBIT.inializeDependency(new CacheableResource(Resource.VARBIT));
	        CacheableDependencies.QC_MSG.inializeDependency(new CacheableResource(Resource.QC_MESSAGES));
	        CacheableDependencies.QC_MENU.inializeDependency(new CacheableResource(Resource.QC_MENU));
	        CacheableDependencies.TEXTURE_MEDIA.inializeDependency(new CacheableResource(Resource.TEXTURE));
	        CacheableDependencies.PARTICLES.inializeDependency(new CacheableResource(Resource.PARTICLE));
	        CacheableDependencies.BILLBOARDS.inializeDependency(new CacheableResource(Resource.BILLBOARD));
	        CacheableDependencies.HUFFMAN.inializeDependency(new CacheableHuffman(Resource.HUFFMAN));
	        CacheableDependencies.INTERFACES.inializeDependency(new CacheableResource(Resource.INTERFACE));
	        CacheableDependencies.CS2.inializeDependency(new CacheableResource(Resource.CS2));
	        CacheableDependencies.FONTS.inializeDependency(new CacheableResource(Resource.FONT_METRICS));
	        CacheableDependencies.WORLD_MAP.inializeDependency(new CacheableWorldMap(Resource.WORLD_MAP));
	        for (int index = 0; index < ClientStartup.fullyCachedDependencies.length; index++) {
	            if (ClientStartup.fullyCachedDependencies[index].getDependency() == null) {
	                throw new RuntimeException();
	            }
	        }
	        int loadingPercent = 0;
	        Dependency[] dependencies = ClientStartup.fullyCachedDependencies;
	        for (int index = 0; index < dependencies.length; index++) {
	            Dependency dependency = dependencies[index];
	            int archiveCount = dependency.getArchiveCount();
	            int completionPercent = dependency.getDependency().getCompletion();
	            loadingPercent += completionPercent * archiveCount / 100;
	        }
	        ClientStartup.loadingPercentDependencies = loadingPercent;
	        ClientStartup.loadingStatusDependencies = 2;
	    }
	    if (ClientStartup.fullyCachedDependencies == null) {
	        return 100;
	    } 
		int totalArchives = 0;
	    int totalLoadPercent = 0;
	    boolean fullyCached = true;
	    Dependency[] dependencies = ClientStartup.fullyCachedDependencies;
	    for (int index = 0; index < dependencies.length; index++) {
	        Dependency dependency = dependencies[index];
	        int archiveCount = dependency.getArchiveCount();
	        int completionPercent = dependency.getDependency().getCompletion();
	        if (completionPercent < 100) {
	            fullyCached = false;
	        }
	        totalArchives += archiveCount;
	        totalLoadPercent += archiveCount * completionPercent / 100;
	    }
	    if (fullyCached) {
	        if (!((CacheableDLL) CacheableDependencies.JAGTHEORA.getDependency()).isReady()) {
	            client.jagtheoraLoaded = ClientStartup.activeClient.loadJagtheora();
	        }
	        ClientStartup.fullyCachedDependencies = null;
	    }
	    totalLoadPercent -= ClientStartup.loadingPercentDependencies;
	    totalArchives -= ClientStartup.loadingPercentDependencies;
	    int averageProgressPercent = totalArchives > 0 ? totalLoadPercent * 100 / totalArchives : 100;
	    if (!fullyCached && averageProgressPercent > 99) {
	        averageProgressPercent = 99;
	    }
	    return averageProgressPercent;
	}


    public static int finalizeStartup() {
        int stage = LoadingStage.LOADING_STAGE.number;
        if (stage < LoadingStage.LOADING_STAGES.length - 1) {
            LoadingStage.LOADING_STAGE = LoadingStage.LOADING_STAGES[stage + 1];
        }
        return 100;
    }

    public static void decodeTitlescreenMusic(byte[] data) {
        Packet buffer = new Packet(data);
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            if (opcode == 1) {
                int titlescreenMusicArchive = buffer.readUShort();
                if (AreaSoundPlayer.titlescreenMusicArchive == -1) {
                    AreaSoundPlayer.titlescreenMusicArchive = titlescreenMusicArchive;
                }
            }
        }
    }

	public static void preparePlayer(boolean prepareWindow) {
	    if (prepareWindow) {
	        if (client.BASE_WINDOW_ID != -1) {
	            Interface.unloadInterface(client.BASE_WINDOW_ID);
	        }
	        for (SubInterface subInterface = client.OPEN_INTERFACES.first(); subInterface != null; subInterface = client.OPEN_INTERFACES.next()) {
	            if (!subInterface.linked()) {
	                subInterface = client.OPEN_INTERFACES.first();
	                if (subInterface == null) {
	                    break;
	                }
	            }
	            Interface.closeChildInterface(subInterface, true, false);
	        }
	        client.BASE_WINDOW_ID = -1;
	        client.OPEN_INTERFACES = new IterableNodeMap<SubInterface>(8);
	        Interface.initializeTables();
	        client.BASE_WINDOW_ID = client.ENTITY_DEFAULTS.loginInterfaceId;
	        WindowMode.interfaceResize(false);
	        GameShell.setComponentsOnTop();
	        Component.runScriptsIfLoaded(client.BASE_WINDOW_ID, null);
	    }
	    ClientStartup.method12708();
	    LoginManager.currentlyInLobby = false;
	    MiniMenu.addCancelOption();
	    client.defaultMenuCursor = -1;
	    CursorTypeList.setCustomCursor(CursorManager.defaultCursorId);
	    PlayerUpdate.MY_PLAYER = new PlayerEntity(null);
	    PlayerUpdate.MY_PLAYER.setBasePosition((Resource.mapRegion.getSceneSizeX() * 512 / 2), 0.0F, (Resource.mapRegion.getSceneSizeY() * 512 / 2));
	    PlayerUpdate.MY_PLAYER.movementBaseX[0] = Resource.mapRegion.getSceneSizeX() / 2;
	    PlayerUpdate.MY_PLAYER.movementBaseY[0] = Resource.mapRegion.getSceneSizeY() / 2;
	    Camera.cameraPositionAbsoluteY = 0;
	    Camera.cameraPositionAbsoluteX = 0;
	    if (Camera.cameraCalculationType == 5) {
	        Camera.cameraPositionAbsoluteX = Camera.cameraMovementLocalX << 9;
	        Camera.cameraPositionAbsoluteY = Camera.cameraMovementLocalY << 9;
	    } else {
	        Camera.calculateCutsceneCameraPosition();
	    }
	    Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
	}

	public static void method12708() {
	    StaticMethods.aString102 = "";
	    StaticMethods.aString99 = "";
	}
}
