package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.DefaultsFile
import com.jagex.game.runetek5.config.IndexReference
import com.jagex.game.runetek5.config.bas.BasTypeList
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsTypeList
import com.jagex.game.runetek5.config.cursor.CursorTypeList
import com.jagex.game.runetek5.config.defaults.EntityDefaults
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType
import com.jagex.game.runetek5.config.defaults.InteractionDefaultsType
import com.jagex.game.runetek5.config.enumtype.EnumTypeList
import com.jagex.game.runetek5.config.flo.FloTypeList
import com.jagex.game.runetek5.config.flu.FluTypeList
import com.jagex.game.runetek5.config.gametip.GameTipTypeList
import com.jagex.game.runetek5.config.gametip.GameTipUpdateDetails
import com.jagex.game.runetek5.config.hitbar.HitbarTypeList
import com.jagex.game.runetek5.config.hitsplat.HitSplatTypeList
import com.jagex.game.runetek5.config.identitykit.IdkTypeList
import com.jagex.game.runetek5.config.inputsubscriber.CutsceneDefaultsType
import com.jagex.game.runetek5.config.light.LightTypeList
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.config.mapsprite.MapSpriteTypeList
import com.jagex.game.runetek5.config.material.MaterialTypeList
import com.jagex.game.runetek5.config.material.texture.TextureTypeList
import com.jagex.game.runetek5.config.mec.MECTypeList
import com.jagex.game.runetek5.config.npc.NpcTypeList
import com.jagex.game.runetek5.config.item.ItemTypeList
import com.jagex.game.runetek5.config.param.ParamTypeList
import com.jagex.game.runetek5.config.quest.QuestTypeList
import com.jagex.game.runetek5.config.quickchat.QuickchatCategoryTypeList
import com.jagex.game.runetek5.config.quickchat.QuickchatFiller
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageTypeList
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerTypeList
import com.jagex.game.runetek5.config.sequence.SeqTypeList
import com.jagex.game.runetek5.config.sequence.spot.SpotAnimTypeList
import com.jagex.game.runetek5.config.skybox.SkyboxTypeList
import com.jagex.game.runetek5.config.struct.StructTypeList
import com.jagex.game.runetek5.config.sun.SunTypeList
import com.jagex.game.runetek5.config.varc.*
import com.jagex.game.runetek5.config.varn.VarnBitTypeList
import com.jagex.game.runetek5.config.varn.VarnTypeList
import com.jagex.game.runetek5.config.varp.VarpTypeList
import com.jagex.game.runetek5.config.varp.VarpbitTypeList
import com.jagex.game.runetek5.encoding.Huffman
import com.jagex.game.runetek5.encoding.HuffmanList
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.FontCombo
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.loginscreen.DrawableGameTip
import com.jagex.game.runetek5.graphics.loginscreen.GameTipsLoader
import com.jagex.game.runetek5.graphics.loginscreen.LoginScreenComponent
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.icmp.IcmpServiceJaclib
import com.jagex.game.runetek5.js5.dependency.CacheableDLL
import com.jagex.game.runetek5.js5.dependency.CacheableDependencies
import com.jagex.game.runetek5.js5.dependency.CacheableHuffman
import com.jagex.game.runetek5.js5.dependency.CacheableResource
import com.jagex.game.runetek5.js5.dependency.CacheableWorldMap
import com.jagex.game.runetek5.js5.dependency.Dependency
import com.jagex.game.runetek5.js5.library.NativeLibraryLoader
import com.jagex.game.runetek5.js5.request.RequestManager
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.environment.EnvironmentManager
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.vardomain.PlayerVarDomain
import com.jagex.game.runetek5.vardomain.VarProviders
import jaclib.ping.Ping

object ClientStartup {

    @JvmField
    val PERCENT_SPLIT_REFERENCE_TABLE_SIZE = intArrayOf(4, 4, 1, 2, 6, 4, 2, 44, 2, 2, 2, 2, 2, 1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1)

    @JvmField
    var referenceTableLoadingCompletion = -1

    @JvmField
    var fullyCachedDependencies: Array<Dependency>? = null

    @JvmField
    var loadingPercentDependencies: Int = 0

    @JvmField
    var loadingStatusDependencies: Int = 0

    @JvmField
    var activeClient: client? = null

    @JvmStatic
    fun initialize(): Int {
        if (PreferenceManager.preferences!!.safeMode!!.getValue() == 0) {
            for (i in 0 until client.keyCachePointerPoss) {
                if (client.KEYS_PRESSED[i].getCharacter().code == 115 || client.KEYS_PRESSED[i].getCharacter().code == 83) {
                    PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.safeMode!!, 1)
                    client.safemodeForcedDuringStartup = true
                    break
                }
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_JS5) {
            if (client.requestManager == null) {
                client.requestManager = RequestManager(Resource.remoteFileRequesterJs5!!, Resource.localRequesterJs5!!)
            }

            if (!client.requestManager!!.initialize()) {
                return 0
            }

            Renderers.prepareForRendering(0, null, true)
            Resource.USING_JAGEX_IMAGE_FORMAT = !NativeSprite.supportsGzippedSprites()
            Resource.FONT = Resource.createProvider(if (Resource.USING_JAGEX_IMAGE_FORMAT) IndexReference.INDEX_34 else IndexReference.INDEX_32, false, 1)
            Resource.GAME_TIPS = Resource.createProvider(IndexReference.INDEX_33, false, 1)
            Resource.FONT_METRICS = Resource.createProvider(IndexReference.INDEX_13, false, 1)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_TIPS) {
            val tipsLoaded = Resource.GAME_TIPS!!.fullyLoaded()
            var stageCompletionPercent = Resource.remoteFileWorkersByIndex!![IndexReference.INDEX_33.getIndex()]!!.getReferenceTableCompletion()
            stageCompletionPercent += Resource.remoteFileWorkersByIndex!![if (Resource.USING_JAGEX_IMAGE_FORMAT) IndexReference.INDEX_34.getIndex() else IndexReference.INDEX_32.getIndex()]!!.getReferenceTableCompletion()
            stageCompletionPercent += Resource.remoteFileWorkersByIndex!![IndexReference.INDEX_13.getIndex()]!!.getReferenceTableCompletion()
            stageCompletionPercent += if (tipsLoaded) 100 else Resource.GAME_TIPS!!.getLoadingPercentage()
            if (stageCompletionPercent != 400) {
                return stageCompletionPercent / 4
            }

            Resource.fontCrc = Resource.FONT!!.getCrc()
            Resource.gameTipsCrc = Resource.GAME_TIPS!!.getCrc()
            FontManager.loadFontArchiveIds(Resource.FONT!!)
            val tipsStartStagePreference = PreferenceManager.preferences!!.gametipStartStagePreference!!.getValue()
            TypeLists.GAME_TIPS = GameTipTypeList(Resource.GAME_TIPS!!)
            var updateDetails = TypeLists.GAME_TIPS!!.getUpdateDetails(tipsStartStagePreference)
            if (updateDetails.isEmpty()) {
                updateDetails = TypeLists.GAME_TIPS!!.getUpdateDetails(0)
            }

            val tipsLoader = GameTipsLoader(Resource.FONT!!, Resource.FONT_METRICS!!)
            if (updateDetails.isNotEmpty()) {
                LoadingStage.gametipsToDisplay = Array(updateDetails.size) { index ->
                    DrawableGameTip(TypeLists.GAME_TIPS!!.list(updateDetails[index].tipFileId), updateDetails[index].displayDurationMs, updateDetails[index].timeBetweenUpdatesMs, tipsLoader)
                }
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CREATE_FONTS) {
            StaticMethods.initializedFontcombos = FontCombo(Resource.FONT!!, Resource.FONT_METRICS!!, FontManager.getFontSpecifications())
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CACHE_FONTS) {
            val cached = StaticMethods.initializedFontcombos!!.getNumCached()
            val total = StaticMethods.initializedFontcombos!!.getFontCount()
            if (cached < total) {
                return cached * 100 / total
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.PREPARE_FONTS) {
            if (LoadingStage.gametipsToDisplay != null && LoadingStage.gametipsToDisplay!!.isNotEmpty()) {
                if (LoadingStage.gametipsToDisplay!![0]!!.calculateLoadingPercent() < 100) {
                    return 0
                }

                if (LoadingStage.gametipsToDisplay!!.size > 1 && TypeLists.GAME_TIPS!!.hasTipsFile() && LoadingStage.gametipsToDisplay!![1]!!.calculateLoadingPercent() < 100) {
                    return 0
                }
            }

            StaticMethods.initializedFontcombos!!.initializeFontMap(client.FONT_PROVIDER)
            FontManager.initialize()
            client.setGameState(GameState.UNK_15)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CREATE_JS5) {
            Resource.SPRITES = Resource.createProvider(IndexReference.INDEX_8, false, 1)
            Resource.FRAME_SET = Resource.createProvider(IndexReference.INDEX_0, false, 1)
            Resource.FRAME_BASE = Resource.createProvider(IndexReference.INDEX_1, false, 1)
            Resource.CONFIG = Resource.createProvider(IndexReference.INDEX_2, false, 1)
            Resource.INTERFACE = Resource.createProvider(IndexReference.INDEX_3, false, 1)
            Resource.SOUND_EFFECT = Resource.createProvider(IndexReference.INDEX_4, false, 1)
            Resource.MAPS = Resource.createProvider(IndexReference.INDEX_5, true, 1)
            Resource.MUSIC = Resource.createProvider(IndexReference.INDEX_6, true, 1)
            Resource.MODEL = Resource.createProvider(IndexReference.INDEX_7, false, 1)
            Resource.TEXTURE = Resource.createProvider(IndexReference.INDEX_9, false, 1)
            Resource.HUFFMAN = Resource.createProvider(IndexReference.INDEX_10, false, 1)
            Resource.JINGLE = Resource.createProvider(IndexReference.INDEX_11, false, 1)
            Resource.CS2 = Resource.createProvider(IndexReference.INDEX_12, false, 1)
            Resource.MIDI_INSTRUMENT = Resource.createProvider(IndexReference.INDEX_14, false, 1)
            Resource.SOUND_MIDI = Resource.createProvider(IndexReference.INDEX_15, false, 1)
            Resource.LOC = Resource.createProvider(IndexReference.INDEX_16, false, 1)
            Resource.ENUM = Resource.createProvider(IndexReference.INDEX_17, false, 1)
            Resource.NPC = Resource.createProvider(IndexReference.INDEX_18, false, 1)
            Resource.OBJ = Resource.createProvider(IndexReference.INDEX_19, false, 1)
            Resource.SEQ = Resource.createProvider(IndexReference.INDEX_20, false, 1)
            Resource.GFX = Resource.createProvider(IndexReference.INDEX_21, false, 1)
            Resource.VARBIT = Resource.createProvider(IndexReference.INDEX_22, false, 1)
            Resource.WORLD_MAP = Resource.createProvider(IndexReference.INDEX_23, true, 1)
            Resource.QC_MESSAGES = Resource.createProvider(IndexReference.INDEX_24, false, 1)
            Resource.QC_MENU = Resource.createProvider(IndexReference.INDEX_25, false, 1)
            Resource.MATERIAL = Resource.createProvider(IndexReference.INDEX_26, true, 1)
            Resource.PARTICLE = Resource.createProvider(IndexReference.INDEX_27, false, 1)
            Resource.DEFAULTS = Resource.createProvider(IndexReference.INDEX_28, true, 1)
            Resource.BILLBOARD = Resource.createProvider(IndexReference.INDEX_29, false, 1)
            Resource.CUTSCENE = Resource.createProvider(IndexReference.INDEX_35, true, 1)
            Resource.NATIVE_LIBRARY = Resource.createProvider(IndexReference.INDEX_30, true, 1)
            Resource.SHADER = Resource.createProvider(IndexReference.INDEX_31, true, 1)
            Resource.VORBIS = Resource.createProvider(IndexReference.INDEX_36, true, 2)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_SPRITES) {
            var completion = 0
            for (index in Resource.remoteFileWorkersByIndex!!.indices) {
                if (Resource.remoteFileWorkersByIndex!![index] != null) {
                    completion += Resource.remoteFileWorkersByIndex!![index]!!.getReferenceTableCompletion() * PERCENT_SPLIT_REFERENCE_TABLE_SIZE[index] / 100
                }
            }
            if (completion != 100) {
                if (referenceTableLoadingCompletion < 0) {
                    referenceTableLoadingCompletion = completion
                }
                return (completion - referenceTableLoadingCompletion) * 100 / (100 - referenceTableLoadingCompletion)
            }
            StaticMedia.loadGroupIds(Resource.SPRITES!!)
            StaticMethods.initializedFontcombos = FontCombo(Resource.SPRITES!!, Resource.FONT_METRICS!!, FontManager.getFontSpecifications())
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_AUDIO) {
            val data = Resource.DEFAULTS!!.getFile(DefaultsFile.TITLESCREEN_MUSIC_INFO.fileId)
            if (data == null) {
                return 0
            }

            decodeTitlescreenMusic(data)
            SoundEffectPlayer.initializeAudio()
            client.setGameState(GameState.UNK_1)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.PREPARE_NATIVE_LIBRARY_LOADER) {
            Resource.NATIVE_LIBRARY_LOADER = NativeLibraryLoader(Resource.NATIVE_LIBRARY!!)
            AbstractToolkit.setNativeLibraryLoader(Resource.NATIVE_LIBRARY_LOADER!!)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_NATIVES) {
            val nativesLoadPercentage = loadDependencies()
            if (nativesLoadPercentage < 100) {
                return nativesLoadPercentage
            }

            EnvironmentManager.decodeCubeTextures(Resource.DEFAULTS!!.getFile(DefaultsFile.SCENE_CUBE_TEXTURES.fileId)!!)
            client.ENTITY_DEFAULTS = EntityDefaults(Resource.DEFAULTS!!)
            PlayerAppearance.recolorPaletteSrc = client.ENTITY_DEFAULTS!!.recolorPaletteSrc
            PlayerAppearance.recolorPaletteDst = client.ENTITY_DEFAULTS!!.recolorPaletteDst
            if (client.ENTITY_DEFAULTS!!.gameWidthDefault != -1 && client.ENTITY_DEFAULTS!!.gameHeightDefault != -1) {
                client.GAME_WIDTH = client.ENTITY_DEFAULTS!!.gameWidthDefault
                client.GAME_HEIGHT = client.ENTITY_DEFAULTS!!.gameHeightDefault
            }

            TypeLists.EQUIPMENT_DEFAULTS = EquipmentDefaultsType(Resource.DEFAULTS!!)
            TypeLists.INTERACTION_DEFAULTS = InteractionDefaultsType(Resource.DEFAULTS!!)
            TypeLists.CUTSCENE_DEFAULTS = CutsceneDefaultsType(Resource.DEFAULTS!!)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.LOAD_TYPELISTS) {
            if (client.ENTITY_DEFAULTS!!.profilingMesh != -1 && !Resource.MODEL!!.isCached(client.ENTITY_DEFAULTS!!.profilingMesh, 0)) {
                return 99
            }

            TypeLists.MATERIALS = MaterialTypeList(Resource.MATERIAL!!, Resource.TEXTURE!!, Resource.SPRITES!!)
            TypeLists.PARAMS = ParamTypeList(Resource.CONFIG!!)
            TypeLists.BAS = BasTypeList(Resource.CONFIG!!, TypeLists.EQUIPMENT_DEFAULTS!!)
            TypeLists.CURSOR = CursorTypeList(Resource.CONFIG!!, Resource.SPRITES!!)
            TypeLists.ENUM = EnumTypeList(Resource.ENUM!!)
            TypeLists.FLO = FloTypeList(Resource.CONFIG!!)
            TypeLists.FLU = FluTypeList(Resource.CONFIG!!)
            TypeLists.HITSPLAT = HitSplatTypeList(Resource.CONFIG!!, Resource.SPRITES!!)
            TypeLists.HITBAR = HitbarTypeList(Resource.CONFIG!!, Resource.SPRITES!!)
            TypeLists.IDK = IdkTypeList(Resource.CONFIG!!, Resource.MODEL!!)
            TypeLists.CONTAINER = ItemContainerTypeList(Resource.CONFIG!!)
            TypeLists.LIGHT = LightTypeList(Resource.CONFIG!!)
            TypeLists.LOC = ObjectTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, Resource.LOC!!, Resource.MODEL!!)
            Resource.mapRegion!!.setLoctypelist(TypeLists.LOC!!)
            Resource.mapRegionLoaderTask!!.initializeRegion(ObjectTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, Resource.LOC!!, Resource.MODEL!!))
            TypeLists.MEC = MECTypeList(Resource.CONFIG!!, Resource.SPRITES!!)
            TypeLists.MAP_SPRITE = MapSpriteTypeList(Resource.CONFIG!!, Resource.SPRITES!!)
            TypeLists.NPC = NpcTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, Resource.NPC!!, Resource.MODEL!!)
            TypeLists.OBJ = ItemTypeList(client.CURRENT_GAME, client.CURRENT_LANGUAGE, TypeLists.PARAMS!!, Resource.OBJ!!, Resource.MODEL!!)
            TypeLists.QUEST = QuestTypeList(Resource.CONFIG!!)
            TypeLists.SEQ = SeqTypeList(Resource.SEQ!!, Resource.FRAME_SET!!, Resource.FRAME_BASE!!)
            TypeLists.SKYBOX = SkyboxTypeList(Resource.CONFIG!!)
            TypeLists.SUN = SunTypeList(Resource.CONFIG!!)
            TypeLists.SPOT_ANIM = SpotAnimTypeList(Resource.GFX!!, Resource.MODEL!!)
            TypeLists.STRUCT = StructTypeList(Resource.CONFIG!!)
            TypeLists.VARC_STRING = VarcStringTypeList(Resource.CONFIG!!)
            TypeLists.VARC = VarcTypeList(Resource.CONFIG!!)
            TypeLists.VARPBIT = VarpbitTypeList(Resource.VARBIT!!)
            TypeLists.VAR = VarpTypeList(Resource.CONFIG!!)
            TypeLists.VARN = VarnTypeList(Resource.CONFIG!!)
            TypeLists.VARNBIT = VarnBitTypeList(Resource.CONFIG!!)
            TypeLists.CLAN_VAR = ClanVarTypeList(Resource.CONFIG!!)
            TypeLists.CLAN_VAR_SETTINGS = ClanVarSettingsTypeList(Resource.CONFIG!!)
            Interface.initialize(Resource.INTERFACE!!, Resource.MODEL!!, Resource.SPRITES!!)
            BillboardTypeList.initialize(Resource.BILLBOARD!!)
            TypeLists.QUICK_CHAT_CATEGORY = QuickchatCategoryTypeList(Resource.QC_MESSAGES!!, Resource.QC_MENU!!)
            TypeLists.QUICK_CHAT_MESSAGE = QuickchatMessageTypeList(Resource.QC_MESSAGES!!, Resource.QC_MENU!!, QuickchatFiller())
            VarProviders.PLAYER_VAR_PROVIDER = PlayerVarDomain()
            GraphicsCache.resetGraphicCaches()
            SeqTypeList.initialize(TypeLists.SEQ!!)
            ParticleSystemManager.initializeIndex(Resource.PARTICLE!!)
            TextureTypeList.initialize(Resource.MODEL!!, TypeLists.MATERIALS!!)
            val huffman = Huffman(Resource.HUFFMAN!!.getNamedFile("huffman", "")!!)
            HuffmanList.initialize(huffman)
            GameShell.cycleManager = GameShell.getCycleManager()
            SystemInfo.SYSTEM_INFO = SystemInfo()
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.CACHE_STATIC_MEDIA) {
            val staticMediaLoadPercent = StaticMedia.fullyCached(Resource.SPRITES!!) + StaticMethods.initializedFontcombos!!.getNumCached(true)
            val staticMediaCount = StaticMethods.initializedFontcombos!!.getFontCount() + 12
            if (staticMediaLoadPercent < staticMediaCount) {
                return staticMediaLoadPercent * 100 / staticMediaCount
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_WORLDMAP) {
            WorldMap.initialize(Resource.WORLD_MAP!!, TypeLists.FLO!!, TypeLists.FLU!!, Resource.mapRegion!!.getLoctypeList(), TypeLists.MEC!!, TypeLists.MAP_SPRITE!!, VarProviders.PLAYER_VAR_PROVIDER!!)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.DISCARD_UNPACKED) {
            VarcTypeList.VARC_INT = IntArray(TypeLists.VARC!!.size)
            VarcTypeList.VARC_BOOL = BooleanArray(TypeLists.VARC!!.size)
            VarcTypeList.VARC_STRING = Array<String?>(TypeLists.VARC_STRING!!.size) { null }

            for (varcId in 0 until TypeLists.VARC!!.size) {
                if (TypeLists.VARC!!.list(varcId).anInt4983 == 0) {
                    VarcTypeList.VARC_BOOL!![varcId] = true
                    client.VARCS_BOOLS_LOADED++
                }

                VarcTypeList.VARC_INT!![varcId] = -1
            }

            VarcTypeList.readVarcIntPreferences()
            Resource.MAPS!!.discardUnpacked(false)
            Resource.MUSIC!!.discardUnpacked(true)
            Resource.SPRITES!!.discardUnpacked(true)
            Resource.FONT_METRICS!!.discardUnpacked(true)
            Resource.HUFFMAN!!.discardUnpacked(true)
            client.typelistCycleUpdate = true
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.DISPLAY_LOGIN_WINDOW) {
            if (!Interface.interfaceLoaded(client.ENTITY_DEFAULTS!!.loginInterfaceId, null)) {
                return 0
            }
            var interfaceCached = true
            for (index in Interface.INTERFACES!![client.ENTITY_DEFAULTS!!.loginInterfaceId]!!.components!!.indices) {
                val component = Interface.INTERFACES!![client.ENTITY_DEFAULTS!!.loginInterfaceId]!!.components!![index]
                if (component!!.type == ComponentType.SPRITE && component.spriteId != -1 && !Resource.SPRITES!!.isCached(component.spriteId, 0)) {
                    interfaceCached = false
                }
            }
            if (!interfaceCached) {
                return 0
            }
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.INITIALIZE_PLAYER) {
            preparePlayer(true)
        }

        if (LoadingStage.LOADING_STAGE == LoadingStage.PREPARE_TOOLKIT) {
            LoadingStage.plainLoginScreenRenderer!!.stop()
            try {
                LoadingStage.loginScreenRendererThread!!.join()
            } catch (ie: InterruptedException) {
                ie.printStackTrace()
                return 0
            }
            LoadingStage.plainLoginScreenRenderer = null
            LoadingStage.loginScreenRendererThread = null
            Resource.FONT = null
            Resource.GAME_TIPS = null
            TypeLists.GAME_TIPS = null
            LoadingStage.gametipsToDisplay = null
            NativeSprite.resetCache()
            client.isForcedSafemode = PreferenceManager.preferences!!.safeMode!!.getValue() == 1
            PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.safeMode!!, 1)
            if (client.isForcedSafemode) {
                PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit!!, 0)
            } else if (PreferenceManager.preferences!!.toolkit!!.isDefault && SystemInfo.SYSTEM_INFO!!.ram < 512 && SystemInfo.SYSTEM_INFO!!.ram != 0) {
                PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.toolkit!!, 0)
            }
            PreferenceManager.savePreferences()
            if (client.isForcedSafemode) {
                client.loadAssets(0, false)
            } else {
                client.loadAssets(PreferenceManager.preferences!!.toolkit!!.getValue(), false)
            }
            WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
            StaticMethods.initializedFontcombos!!.initializeFontMap(client.FONT_PROVIDER)
            FontManager.initialize()
            StaticMedia.loadSprites(Renderers.activeToolkit!!, Resource.SPRITES!!)
            StaticMedia.initializeChatIcons(StaticMedia.nameIconSprites!!)
        }

        return finalizeStartup()
    }

    @JvmStatic
    fun loadDependencies(): Int {
        if (loadingStatusDependencies == 0) {
            CacheableDependencies.JACLIB.inializeDependency(CacheableDLL("jaclib"))
            if (CacheableDependencies.JACLIB.getDependency()!!.getCompletion() != 100) {
                return 1
            }
            if (!(CacheableDependencies.JACLIB.getDependency() as CacheableDLL).isReady()) {
                activeClient!!.startJaclib()
                IcmpServiceJaclib.initialize()
                try {
                    Ping.init()
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
            loadingStatusDependencies = 1
        }
        if (loadingStatusDependencies == 1) {
            fullyCachedDependencies = CacheableDependencies.getAll()
            CacheableDependencies.DEFAULTS.inializeDependency(CacheableResource(Resource.DEFAULTS!!))
            CacheableDependencies.JAGGL.inializeDependency(CacheableDLL("jaggl"))
            CacheableDependencies.JAGDX.inializeDependency(CacheableDLL("jagdx"))
            CacheableDependencies.SW3D.inializeDependency(CacheableDLL("sw3d"))
            CacheableDependencies.HW3D.inializeDependency(CacheableDLL("hw3d"))
            CacheableDependencies.JAGTHEORA.inializeDependency(CacheableDLL("jagtheora"))
            CacheableDependencies.SHADERS.inializeDependency(CacheableResource(Resource.SHADER!!))
            CacheableDependencies.MATERIALS.inializeDependency(CacheableResource(Resource.MATERIAL!!))
            CacheableDependencies.CONFIG.inializeDependency(CacheableResource(Resource.CONFIG!!))
            CacheableDependencies.LOC.inializeDependency(CacheableResource(Resource.LOC!!))
            CacheableDependencies.ENUM.inializeDependency(CacheableResource(Resource.ENUM!!))
            CacheableDependencies.NPC.inializeDependency(CacheableResource(Resource.NPC!!))
            CacheableDependencies.OBJ.inializeDependency(CacheableResource(Resource.OBJ!!))
            CacheableDependencies.SEQ.inializeDependency(CacheableResource(Resource.SEQ!!))
            CacheableDependencies.GFX.inializeDependency(CacheableResource(Resource.GFX!!))
            CacheableDependencies.VARBIT.inializeDependency(CacheableResource(Resource.VARBIT!!))
            CacheableDependencies.QC_MSG.inializeDependency(CacheableResource(Resource.QC_MESSAGES!!))
            CacheableDependencies.QC_MENU.inializeDependency(CacheableResource(Resource.QC_MENU!!))
            CacheableDependencies.TEXTURE_MEDIA.inializeDependency(CacheableResource(Resource.TEXTURE!!))
            CacheableDependencies.PARTICLES.inializeDependency(CacheableResource(Resource.PARTICLE!!))
            CacheableDependencies.BILLBOARDS.inializeDependency(CacheableResource(Resource.BILLBOARD!!))
            CacheableDependencies.HUFFMAN.inializeDependency(CacheableHuffman(Resource.HUFFMAN!!))
            CacheableDependencies.INTERFACES.inializeDependency(CacheableResource(Resource.INTERFACE!!))
            CacheableDependencies.CS2.inializeDependency(CacheableResource(Resource.CS2!!))
            CacheableDependencies.FONTS.inializeDependency(CacheableResource(Resource.FONT_METRICS!!))
            CacheableDependencies.WORLD_MAP.inializeDependency(CacheableWorldMap(Resource.WORLD_MAP!!))
            for (index in fullyCachedDependencies!!.indices) {
                if (fullyCachedDependencies!![index].getDependency() == null) {
                    throw RuntimeException()
                }
            }
            var loadingPercent = 0
            val dependencies = fullyCachedDependencies!!
            for (index in dependencies.indices) {
                val dependency = dependencies[index]
                val archiveCount = dependency.getArchiveCount()
                val completionPercent = dependency.getDependency()!!.getCompletion()
                loadingPercent += completionPercent * archiveCount / 100
            }
            loadingPercentDependencies = loadingPercent
            loadingStatusDependencies = 2
        }
        if (fullyCachedDependencies == null) {
            return 100
        }
        var totalArchives = 0
        var totalLoadPercent = 0
        var fullyCached = true
        val dependencies = fullyCachedDependencies!!
        for (index in dependencies.indices) {
            val dependency = dependencies[index]
            val archiveCount = dependency.getArchiveCount()
            val completionPercent = dependency.getDependency()!!.getCompletion()
            if (completionPercent < 100) {
                fullyCached = false
            }
            totalArchives += archiveCount
            totalLoadPercent += archiveCount * completionPercent / 100
        }
        if (fullyCached) {
            if (!(CacheableDependencies.JAGTHEORA.getDependency()!! as CacheableDLL).isReady()) {
                client.jagtheoraLoaded = activeClient!!.loadJagtheora()
            }
            fullyCachedDependencies = null
        }
        totalLoadPercent -= loadingPercentDependencies
        totalArchives -= loadingPercentDependencies
        var averageProgressPercent = if (totalArchives > 0) totalLoadPercent * 100 / totalArchives else 100
        if (!fullyCached && averageProgressPercent > 99) {
            averageProgressPercent = 99
        }
        return averageProgressPercent
    }

    @JvmStatic
    fun finalizeStartup(): Int {
        val stage = LoadingStage.LOADING_STAGE!!.number
        if (stage < LoadingStage.LOADING_STAGES!!.size - 1) {
            LoadingStage.LOADING_STAGE = LoadingStage.LOADING_STAGES!![stage + 1]
        }
        return 100
    }

    @JvmStatic
    fun decodeTitlescreenMusic(data: ByteArray?) {
        val buffer = Packet(data!!)
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            if (opcode == 1) {
                val titlescreenMusicArchive = buffer.readUShort()
                if (AreaSoundPlayer.titlescreenMusicArchive == -1) {
                    AreaSoundPlayer.titlescreenMusicArchive = titlescreenMusicArchive
                }
            }
        }
    }

    @JvmStatic
    fun preparePlayer(prepareWindow: Boolean) {
        if (prepareWindow) {
            if (client.BASE_WINDOW_ID != -1) {
                Interface.unloadInterface(client.BASE_WINDOW_ID)
            }
            var subInterface: SubInterface? = client.OPEN_INTERFACES.first()
            while (subInterface != null) {
                if (!subInterface.linked()) {
                    subInterface = client.OPEN_INTERFACES.first()
                    if (subInterface == null) {
                        break
                    }
                }
                Interface.closeChildInterface(subInterface, true, false)
                subInterface = client.OPEN_INTERFACES.next()
            }
            client.BASE_WINDOW_ID = -1
            client.OPEN_INTERFACES = IterableNodeMap(8)
            Interface.initializeTables()
            client.BASE_WINDOW_ID = client.ENTITY_DEFAULTS!!.loginInterfaceId
            WindowMode.interfaceResize(false)
            GameShell.setComponentsOnTop()
            Component.runScriptsIfLoaded(client.BASE_WINDOW_ID, null)
        }
        method12708()
        LoginManager.currentlyInLobby = false
        MiniMenu.addCancelOption()
        client.defaultMenuCursor = -1
        CursorTypeList.setCustomCursor(CursorManager.defaultCursorId)
        PlayerUpdate.MY_PLAYER = PlayerEntity(null)
        PlayerUpdate.MY_PLAYER!!.setBasePosition((Resource.mapRegion!!.getSceneSizeX() * 512 / 2).toFloat(), 0.0f, (Resource.mapRegion!!.getSceneSizeY() * 512 / 2).toFloat())
        PlayerUpdate.MY_PLAYER!!.movementBaseX!![0] = Resource.mapRegion!!.getSceneSizeX() / 2
        PlayerUpdate.MY_PLAYER!!.movementBaseY!![0] = Resource.mapRegion!!.getSceneSizeY() / 2
        Camera.cameraPositionAbsoluteY = 0
        Camera.cameraPositionAbsoluteX = 0
        if (Camera.cameraCalculationType == 5) {
            Camera.cameraPositionAbsoluteX = Camera.cameraMovementLocalX shl 9
            Camera.cameraPositionAbsoluteY = Camera.cameraMovementLocalY shl 9
        } else {
            Camera.calculateCutsceneCameraPosition()
        }
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
    }

    @JvmStatic
    fun method12708() {
        StaticMethods.aString102 = ""
        StaticMethods.aString99 = ""
    }
}
