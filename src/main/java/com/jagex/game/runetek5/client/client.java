package com.jagex.game.runetek5.client;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.botdetection.*;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.buffer.CircularBuffer;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.impl.*;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.console.CommandExecutor;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.input.keyboard.CanvasKeyRecorder;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.AbstractMouseManager;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer;
import com.jagex.game.runetek5.client.input.mouse.MouseActionClient;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.login.LoginProt;
import com.jagex.game.runetek5.client.login.LoginStateUpdate;
import com.jagex.game.runetek5.client.menu.MenuInteraction;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.periodic.PulseEventHandler;
import com.jagex.game.runetek5.client.periodic.PulseEventQueue;
import com.jagex.game.runetek5.client.window.WindowSettings;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.config.IndexReference;
import com.jagex.game.runetek5.config.cursor.CursorTypeList;
import com.jagex.game.runetek5.config.cutscene.*;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement;
import com.jagex.game.runetek5.config.defaults.EntityDefaults;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.midi.VorbisNode;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.sequence.MovementType;
import com.jagex.game.runetek5.config.sequence.PositionedGfx;
import com.jagex.game.runetek5.config.sun.Sun;
import com.jagex.game.runetek5.entity.*;
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper;
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCUpdate;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.BasicFontProvider;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.FontProvider;
import com.jagex.game.runetek5.graphics.hint.HintDetails;
import com.jagex.game.runetek5.graphics.hint.HintRenderer;
import com.jagex.game.runetek5.graphics.hint.HintTrail;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.graphics.loginscreen.LoginScreenComponent;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1;
import com.jagex.game.runetek5.graphics.model.particle.ParticleProducer;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.icmp.IcmpServiceJaclib;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel;
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat;
import com.jagex.game.runetek5.js5.dependency.CacheableDependencies;
import com.jagex.game.runetek5.js5.dependency.Dependency;
import com.jagex.game.runetek5.js5.request.RequestManager;
import com.jagex.game.runetek5.js5.request.local.LocalRequester;
import com.jagex.game.runetek5.js5.request.remote.RemoteFileRequester;
import com.jagex.game.runetek5.math.*;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder;
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies;
import com.jagex.game.runetek5.network.*;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.GrandExchangeSlot;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.network.world.WorldMeta;
import com.jagex.game.runetek5.rendering.MessageBox;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.*;
import com.jagex.game.runetek5.scene.environment.Environment;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.player.area.AmbientSound;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.Browser;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.tools.TimeTools;
import com.jagex.game.runetek5.utils.*;
import com.jagex.game.runetek5.utils.Base64;
import com.jagex.game.runetek5.vardomain.VarProviders;

import jaclib.ping.Ping;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.*;

public class client extends GameShell {

    public static final HintTrail[] HINT_TRAILS = new HintTrail[9];
    public static final NodeCollection MOUSE_MOVEMENTS = new NodeCollection();
    public static final KeyRecord[] KEYS_PRESSED = new KeyRecord[131];
    public static final KeyRecord[] KEY_RECORDS = new KeyRecord[75];
    public static final Color[] DEBUG_LOAD_COLOR = {new Color(9179409), new Color(3289650), new Color(3289650), new Color(3289650)};
    public static final Color[] DEBUG_BG_COLOR = {new Color(9179409), new Color(16777215), new Color(16726277), new Color(16726277)};
    public static final Color[] aColorArray7200 = {new Color(16777215), new Color(16777215), new Color(16741381), new Color(16741381)};
    public static final FontProvider FONT_PROVIDER = new BasicFontProvider();
    public static final IterableNodeMap<ObjectNode> NPC_MAP = new IterableNodeMap<ObjectNode>(64);
    public static final ObjectNode[] NPC_ARRAY = new ObjectNode[1024];
    public static final int[] NPC_UPDATE_INDICES = new int[1024];
    public static final BufferedConnectionContext LOBBY_CONNECTION_CONTEXT = new BufferedConnectionContext();
    public static final BufferedConnectionContext GAME_CONNECTION_CONTEXT = new BufferedConnectionContext();
    public static final BufferedConnectionContext[] connectionContexts;
    public static final Object MAP_REGION_LOADER_LOCK;
    public static final Object ASYNC_MAPLOADING_LOCK;
    public static final int[] npcSpawnMaps;
    public static final int[] OBJECT_TYPE_SLOTS;
    public static final int anInt7270;
    public static final int[][][] anIntArrayArrayArray7302;
    public static final float[] HINT_POSITION;
    public static final IterableNodeMap<GroundItemWrapper> GROUND_ITEMS;
    public static final NodeCollection PROJECTILES;
    public static final IterableNodeMap<PositionedGfx> ACTIVE_GFX;
    public static final EntityNodeList TILE_MESSAGES;
    public static final int[] SKILL_LEVEL_VISIBLE;
    public static final int[] SKILL_LEVEL_ACTUAL;
    public static final int[] SKILL_XP;
    public static final NodeCollection PENDING_HOOK_REQUESTS;
    public static final boolean[] IS_COMPONENT_ON_TOP;
    public static final GrandExchangeSlot[] GRAND_EXCHANGE_SLOTS;
    public static final ComparableGraphNode INTERACTIVE_OBJECT_COMPARABLE;
    public static final LRUCache DUMMY = new LRUCache(4);
    public static final Matrix43 SUN_MATRIX = new Matrix43();
    public static final Matrix43 INTERFACE_MODEL_MATRIX = new Matrix43();
    public static final long aLong7409 = (long) (Math.random() * 9.999999999E9D);
    public static final LRUCache OVERHEAD_SPRITES = new LRUCache(8);
    public static final int[] NPC_INDICES = new int[261];
    public static final int[] LOCAL_NPC_INDICES = new int[1005];
    public static final int[] bigX;
    public static final int[] bigY;
    public static final int[] WALKING_STEPS_X;
    public static final int[] WALKING_STEPS_Y;
    public static final int[] NPC_TURN_DIRECTIONS;
    public static final Random RANDOMIZER;
    public static final int[] ENTITY_INDEX_REFERENCES;
    public static final int[] NPC_INDEX_REFERENCES;
    public static final EntityNodeList ENTITY_OVERHEADS;
    public static final int BORDER_WIDTH;
    public static final int BORDER_HEIGHT;
    public static final short[] aShortArray7239;
    public static final int[] PLAYER_OPTION_CURSORS;
    public static final String[] PLAYER_OPTIONS;
    public static final boolean[] PLAYER_OPTION_REDUCED_PRIORITY;
    public static final boolean DISPLAY_HIDDEN_MENU_OPTIONS;
    public static final boolean prefixMinimenuOps;
    public static final boolean CLIP_INTERFACES;
    public static final int[] PULSE_EVENT_CONFIGS_CACHE;
    public static final int[] RECENTLY_USED_ITEM_CONTAINERS;
    public static final int[] anIntArray7283;
    public static final int[] anIntArray7150;
    public static final int[] recentMouseWheels;
    public static final int[] CLAN_VAR_KEYS;
    public static final NodeCollection aClass4827233;
    public static final NodeCollection aClass4827404;
    public static final IterableNodeMap ICOMPONENT_SETTINGS_SLOTS;
    public static final boolean[] PREV_COMPONENT_ON_TOP;
    public static final Rectangle[] INTERFACE_BG_COMPONENTS;
    public static final int anInt7412;
    public static final int[] anIntArray7438;
    public static final int MAX_HINTS;
    public static final int[] HINT_X;
    public static final int[] HINT_Y;
    public static final int[] HINT_WIDTH;
    public static final HintDetails[] HINT_DETAILS;
    public static final int[] anIntArray7148;
    public static final long[] aLongArray7424;
    public static final boolean[] CAM_SHAKING;
    public static final int[] anIntArray7268;
    public static final int[] CUSTOM_LOWEST_YAW;
    public static final int[] anIntArray7462;
    public static final int[] anIntArray7435;
    public static final EntityNodeList PLAYER_LOGINSTATE_UPDATES;
    public static final Calendar ACTIVE_TIMEZONE_CALENDER;
    public static final int[] HITSPLAT_SIZE_CACHE;
    public static Game CURRENT_GAME;
    public static boolean isApplet;
    public static int AFFILIATE;
    public static int DEBUG_DISPLAY_COLOR_INDEX;
    public static boolean aBool7151;
    public static byte[] base64Url;
    public static long aLong7153;
    public static boolean isForcedSafemode;
    public static boolean safemodeForcedDuringStartup;
    public static String loginScreenImageResourceNames;
    public static boolean aBool7158;
    public static boolean aBool7159;
    public static int ENTITY_TARGET;
    public static String aString7165;
    public static GameState gameState = GameState.UNK_4;
    public static boolean renderWithCpu;
    public static int TEST_ENVIRONMENT_APPEARANCE_OVERRIDES;
    public static int cycles;
    public static boolean preferencesSavedOnServer = true;
    public static int REBOOT_TIMER;
    public static boolean typelistCycleUpdate;
    public static boolean needsResize;
    public static int sceneDummyOp;
    public static boolean jagtheoraLoaded;
    public static boolean aBool7189;
    public static int keyCachePointerPoss;
    public static int maximumHeldKeys;
    public static int npcCount;
    public static int NPC_UPDATE_INDEX;
    public static PingRequester PING_REQUESTER;
    public static volatile boolean maploadingLockTaken;
    public static int npcSpawnCursorPos;
    public static int CURRENT_CUTSCENE;
    public static int MAP_LOADING_STAGE;
    public static boolean aBool7321;
    public static int anInt7262;
    public static int anInt7376;
    public static int anInt7271;
    public static boolean sendCameraAngleToServer;
    public static int anInt7277;
    public static int anInt7448;
    public static int anInt7279;
    public static int anInt7280;
    public static int anInt7256;
    public static int anInt7327;
    public static int anInt7182;
    public static int timesSceneRendered;
    public static int GAME_WIDTH;
    public static int GAME_HEIGHT;
    public static boolean IS_MEMBER;
    public static boolean memberWorld;
    public static int PLAYER_RIGHTS;
    public static int PLAYER_MOD_LEVEL;
    public static boolean USERDETAIL_QUICKCHAT;
    public static boolean VERIFIED_EMAIL_ADDRESS;
    public static boolean aBool7323;
    public static boolean IS_QUICKCHAT_ONLY;
    public static int CURRENT_CURSOR;
    public static boolean interfaceSelectionActive;
    public static String useOnType;
    public static String useOnTarget;
    public static volatile int BASE_WINDOW_ID;
    public static IterableNodeMap<SubInterface> OPEN_INTERFACES;
    public static Component aClass1187352;
    public static int RUN_ENERGY;
    public static int RUN_WEIGHT;
    public static Component GAME_SCREEN_INTERFACE;
    public static int anInt7347;
    public static int anInt7391;
    public static int VARCS_BOOLS_LOADED;
    public static boolean clientIsActive;
    public static short aShort7436;
    public static short aShort7437;
    public static short aShort7394;
    public static short aShort7324;
    public static short aShort7276;
    public static short aShort7441;
    public static short aShort7214;
    public static short aShort7474;
    public static int gameAreaWidth;
    public static int gameAreaHeight;
    public static byte aByte7458;
    public static boolean showErrorOnFailedZap;
    public static String affiliateP;
    public static String aString7156;
    public static boolean aBool7160;
    public static boolean addGamescreenPadding;
    public static int anInt7163 = -1;
    public static String aString7164;
    public static long aLong7170;
    public static boolean awaitingPriorityRequests = true;
    public static boolean DRAW_DEBUG;
    public static boolean DEBUG_MODE;
    public static boolean aBool7241;
    public static boolean playingAltGame = true;
    public static int anInt7396;
    public static int anInt7191;
    public static int updateStage;
    public static int pulsesBeforeJs5Reconnect;
    public static int failedRequests;
    public static String js5ProxyAddress;
    public static int npcListSize;
    public static int npcUpdateCycle;
    public static int localNpcCount;
    public static int varVerifyId;
    public static boolean sendTransmitVarVerifyid;
    public static int cycleCutsceneLoaded;
    public static int CUTSCENE_MISC_ACTION_INDEX;
    public static int anInt7418;
    public static int minimapRotation;
    public static int anInt7187;
    public static int minimapZoom;
    public static int anInt7258;
    public static int anInt7259;
    public static int animatingStep;
    public static float aFloat7266;
    public static float aFloat7284;
    public static boolean aBool7331;
    public static boolean aBool7269;
    public static boolean aBool7355;
    public static boolean aBool7285;
    public static int anInt7304;
    public static int gameDrawOrder;
    public static int crossX;
    public static int crossY;
    public static int crossSpritePtr;
    public static int crossType;
    public static int anInt7297;
    public static int ROOF_OVERRIDE;
    public static int[] visibleBelowDrawheight;
    public static int[] visibleAboveX;
    public static int[] visibleBelowX;
    public static int[] visibleBelowY;
    public static int[] visibleAboveY;
    public static boolean aBool7322;
    public static boolean REDUCE_ATTACK_PRIORITY;
    public static int anInt7339;
    public static int defaultMenuCursor;
    public static int anInt7345;
    public static int anInt7346;
    public static int anInt7351;
    public static Component draggingComponent;
    public static Component selectedComponent;
    public static int mouseX;
    public static int mouseY;
    public static Component hoveredComponent;
    public static boolean isComponentSelected;
    public static int selectedComponentX;
    public static int selectedComponentY;
    public static int selectedComponentWidth;
    public static int selectedComponentHeight;
    public static boolean isDraggingComponent;
    public static int dragStartX;
    public static int dragStartY;
    public static boolean aBool7364;
    public static int menuItemCount;
    public static boolean isMapMoving;
    public static int pulseEventConfigsPtr;
    public static int RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS;
    public static int anInt7384;
    public static int keyCycle;
    public static int mouseWheelCycle;
    public static int CLAN_VAR_COUNTER;
    public static int anInt7386;
    public static int anInt7179;
    public static int anInt7192;
    public static int anInt7395;
    public static int anInt7320;
    public static int anInt7397;
    public static long exitTime;
    public static int anInt7407;
    public static int anInt7408;
    public static int currentHintPtr;
    public static int anInt7389;
    public static int anInt7429;
    public static int hintArrowOffsetX;
    public static int hintArrowOffsetY;
    public static int anInt7451;
    public static int anInt56;
    public static RequestManager requestManager;
    public static Language CURRENT_LANGUAGE;
    public static BuildType BUILD_ENVIRONMENT;
    public static long js5ConnectTime;
    public static Socket clientSocket;

    static {
        connectionContexts = new BufferedConnectionContext[]{GAME_CONNECTION_CONTEXT, LOBBY_CONNECTION_CONTEXT};
        varVerifyId = 0;
        sendTransmitVarVerifyid = false;
        PING_REQUESTER = new PingRequester();
        Resource.mapRegion = new MapRegion(false);
        maploadingLockTaken = false;
        MAP_REGION_LOADER_LOCK = new Object();
        ASYNC_MAPLOADING_LOCK = new Object();
        npcSpawnMaps = new int[64];
        npcSpawnCursorPos = 0;
        OBJECT_TYPE_SLOTS = new int[]{0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3};
        CURRENT_CUTSCENE = -1;
        MAP_LOADING_STAGE = 1;
        aBool7321 = false;
        cycleCutsceneLoaded = -1;
        CUTSCENE_MISC_ACTION_INDEX = 0;
        InterfaceComponentRenderer.cycleCutsceneFadeStarted = -1;
        InterfaceComponentRenderer.cutsceneFadeEndCycle = -1;
        InterfaceComponentRenderer.cutsceneFadeAlpha = 255;
        InterfaceComponentRenderer.cutsceneFadeRed = 0;
        InterfaceComponentRenderer.cutsceneFadeGreen = 0;
        InterfaceComponentRenderer.cutsceneFadeBlue = 0;
        bigX = new int[4096];
        bigY = new int[4096];
        WALKING_STEPS_X = new int[50];
        WALKING_STEPS_Y = new int[50];
        NPC_TURN_DIRECTIONS = new int[]{-1, 8192, 0, -1, 12288, 10240, 14336, -1, 4096, 6144, 2048};
        Camera.cameraRandomisationH = 0;
        Camera.cameraStepH = 2;
        Camera.cameraRandomisationV = 0;
        Camera.cameraStepV = 2;
        Camera.cameraRandomisationA = 0;
        Camera.cameraStepA = 1;
        anInt7418 = 0;
        minimapRotation = 0;
        anInt7187 = 2;
        minimapZoom = 0;
        anInt7258 = 1;
        anInt7259 = 0;
        RANDOMIZER = new Random();
        animatingStep = 0;
        anInt7262 = 0;
        anInt7376 = 0;
        Camera.camAngleX = 1052.0F;
        Camera.camAngleY = 0.0F;
        aFloat7266 = 0.0F;
        aFloat7284 = 0.0F;
        aBool7331 = false;
        aBool7269 = false;
        anInt7270 = 46;
        anInt7271 = anInt7270 + 800;
        sendCameraAngleToServer = true;
        Camera.regularLowestYaw = 0;
        anIntArrayArrayArray7302 = new int[2][][];
        anInt7277 = -1;
        anInt7448 = -1;
        anInt7279 = 0;
        anInt7280 = 0;
        anInt7256 = 0;
        anInt7327 = 0;
        anInt7182 = 0;
        aBool7355 = false;
        aBool7285 = false;
        timesSceneRendered = 0;
        ENTITY_INDEX_REFERENCES = new int[508];
        NPC_INDEX_REFERENCES = new int[502];
        anInt7304 = 0;
        ENTITY_OVERHEADS = new EntityNodeList();
        gameDrawOrder = 0;
        HINT_POSITION = new float[3];
        crossX = 0;
        crossY = 0;
        crossSpritePtr = 0;
        crossType = 0;
        anInt7297 = 0;
        ROOF_OVERRIDE = -1;
        visibleBelowDrawheight = new int[2];
        visibleAboveX = new int[2];
        visibleBelowX = new int[2];
        visibleBelowY = new int[2];
        visibleAboveY = new int[2];
        WindowMode.nextDimensionUpdateCheck = 0L;
        WindowMode.fullScreenWindowMode = true;
        BORDER_WIDTH = 32;
        BORDER_HEIGHT = 16;
        GAME_WIDTH = 765;
        GAME_HEIGHT = 553;
        PlayerUpdate.players = new PlayerEntity[2048];
        PlayerUpdate.myPlayerIndex = -1;
        IS_MEMBER = false;
        memberWorld = false;
        PLAYER_RIGHTS = 0;
        PLAYER_MOD_LEVEL = 0;
        USERDETAIL_QUICKCHAT = false;
        VERIFIED_EMAIL_ADDRESS = false;
        aBool7322 = false;
        aBool7323 = false;
        REDUCE_ATTACK_PRIORITY = true;
        IS_QUICKCHAT_ONLY = false;
        aShortArray7239 = new short[]{(short) 44, (short) 45, (short) 46, (short) 47, (short) 48, (short) 49, (short) 50, (short) 51};
        PLAYER_OPTION_CURSORS = new int[8];
        PLAYER_OPTIONS = new String[8];
        PLAYER_OPTION_REDUCED_PRIORITY = new boolean[8];
        CursorManager.activeCursorId = -1;
        GROUND_ITEMS = new IterableNodeMap<GroundItemWrapper>(64);
        PROJECTILES = new NodeCollection();
        ACTIVE_GFX = new IterableNodeMap<PositionedGfx>(16);
        TILE_MESSAGES = new EntityNodeList();
        SKILL_LEVEL_VISIBLE = new int[25];
        SKILL_LEVEL_ACTUAL = new int[25];
        SKILL_XP = new int[25];
        anInt7339 = 0;
        CursorManager.defaultCursorId = -1;
        defaultMenuCursor = -1;
        CursorManager.defaultAttackCursor = -1;
        CURRENT_CURSOR = -1;
        interfaceSelectionActive = false;
        anInt7345 = -1;
        anInt7346 = -1;
        useOnType = null;
        useOnTarget = null;
        BASE_WINDOW_ID = -1;
        OPEN_INTERFACES = new IterableNodeMap<SubInterface>(8);
        anInt7351 = 0;
        aClass1187352 = null;
        RUN_ENERGY = 0;
        RUN_WEIGHT = 0;
        DISPLAY_HIDDEN_MENU_OPTIONS = false;
        prefixMinimenuOps = false;
        GAME_SCREEN_INTERFACE = null;
        CLIP_INTERFACES = false;
        draggingComponent = null;
        selectedComponent = null;
        mouseX = 0;
        mouseY = 0;
        hoveredComponent = null;
        isComponentSelected = false;
        selectedComponentX = -1;
        selectedComponentY = -1;
        selectedComponentWidth = -1;
        selectedComponentHeight = -1;
        isDraggingComponent = false;
        dragStartX = -1;
        dragStartY = -1;
        aBool7364 = false;
        menuItemCount = 0;
        isMapMoving = false;
        CommandExecutor.teleportCommandRequested = false;
        anInt7347 = 1;
        PULSE_EVENT_CONFIGS_CACHE = new int[32];
        pulseEventConfigsPtr = 0;
        RECENTLY_USED_ITEM_CONTAINERS = new int[32];
        RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS = 0;
        anIntArray7283 = new int[32];
        anInt7384 = 0;
        anIntArray7150 = new int[32];
        keyCycle = 0;
        recentMouseWheels = new int[32];
        mouseWheelCycle = 0;
        CLAN_VAR_KEYS = new int[32];
        CLAN_VAR_COUNTER = 0;
        anInt7391 = 0;
        anInt7386 = 0;
        anInt7179 = 0;
        anInt7192 = 0;
        anInt7395 = 0;
        anInt7320 = 0;
        anInt7397 = 0;
        VARCS_BOOLS_LOADED = 0;
        clientIsActive = false;
        exitTime = -1L;
        PENDING_HOOK_REQUESTS = new NodeCollection();
        aClass4827233 = new NodeCollection();
        aClass4827404 = new NodeCollection();
        ICOMPONENT_SETTINGS_SLOTS = new IterableNodeMap(512);
        anInt7407 = 0;
        anInt7408 = -2;
        IS_COMPONENT_ON_TOP = new boolean[107];
        PREV_COMPONENT_ON_TOP = new boolean[107];
        INTERFACE_BG_COMPONENTS = new Rectangle[107];

        for (int i_0 = 0; i_0 < 107; i_0++) {
            INTERFACE_BG_COMPONENTS[i_0] = new Rectangle();
        }

        anInt7412 = 0;
        anIntArray7438 = new int[4];
        currentHintPtr = 0;
        MAX_HINTS = 51;
        HINT_X = new int[MAX_HINTS];
        HINT_Y = new int[MAX_HINTS];
        HINT_WIDTH = new int[MAX_HINTS];
        HINT_DETAILS = new HintDetails[MAX_HINTS];
        anIntArray7148 = new int[]{16776960, 16711680, 65280, 65535, 16711935, 16777215};
        aLongArray7424 = new long[100];
        anInt7389 = 0;
        FriendsChat.FC_NAME = null;
        FriendsChat.FC_OWNER_NAME = null;
        GRAND_EXCHANGE_SLOTS = new GrandExchangeSlot[6];
        anInt7429 = 0;
        CAM_SHAKING = new boolean[5];
        anIntArray7268 = new int[5];
        CUSTOM_LOWEST_YAW = new int[5];
        anIntArray7462 = new int[5];
        anIntArray7435 = new int[5];
        aShort7436 = 256;
        aShort7437 = 205;
        aShort7394 = 256;
        aShort7324 = 320;
        aShort7276 = 1;
        aShort7441 = 32767;
        aShort7214 = 1;
        aShort7474 = 32767;
        hintArrowOffsetX = 0;
        hintArrowOffsetY = 0;
        gameAreaWidth = 0;
        gameAreaHeight = 0;
        anInt7451 = 0;
        PLAYER_LOGINSTATE_UPDATES = new EntityNodeList();
        ACTIVE_TIMEZONE_CALENDER = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        aByte7458 = -6;
        INTERACTIVE_OBJECT_COMPARABLE = new ComparableInteractiveEntity();
        HITSPLAT_SIZE_CACHE = new int[4];
    }

    public static IFEvents getComponentSettings(Component icomponentdefinitions0) {
        IFEvents class282Sub101 = (IFEvents) ICOMPONENT_SETTINGS_SLOTS.get(((long) icomponentdefinitions0.idHash << 32) + icomponentdefinitions0.slotId);
        return class282Sub101 != null ? class282Sub101 : icomponentdefinitions0.targetParams;
    }

    public static Component method11634(Component component) {
        IFEvents targetParams = getComponentSettings(component);
        if (targetParams.ignoresDepthFlags()) {
            return client.frameComponent;
        }
        int depthFlags = targetParams.depthFlags();
        if (depthFlags == 0) {
            return null;
        }
        for (int i_3 = 0; i_3 < depthFlags; i_3++) {
            component = Interface.getSub(Interface.getInterface(component.idHash), component);
            if (component == null) {
                return client.frameComponent;
            }
        }

        return component;
    }

    public static void method11750() {
        anInt7304 = 0;

        for (int index = 0; index < NPC_UPDATE_INDEX; index++) {
            NpcEntity npc = (NpcEntity) (NPC_MAP.get(NPC_UPDATE_INDICES[index])).value;
            if (npc.isHidden && npc.getOverheadSprite() != -1) {
                int size = (npc.getSize() - 1) * 256 + 252;
                Vector3 abs = npc.getAbsolutePosition().coords;
                int x = (int) abs.x - size >> 9;
                int z = (int) abs.z - size >> 9;
                PathingEntity pathing = PathingEntity.getLargestEntity(npc.plane, x, z);
                if (pathing != null) {
                    int indexReference = pathing.playerIndex;
                    if (pathing instanceof NpcEntity) {
                        indexReference += 2048;
                    }

                    if (pathing.anInt10319 == 0 && pathing.getOverheadSprite() != -1) {
                        ENTITY_INDEX_REFERENCES[anInt7304] = indexReference;
                        NPC_INDEX_REFERENCES[anInt7304] = indexReference;
                        anInt7304++;
                        pathing.anInt10319++;
                    }

                    ENTITY_INDEX_REFERENCES[anInt7304] = indexReference;
                    NPC_INDEX_REFERENCES[anInt7304] = npc.playerIndex + 2048;
                    anInt7304++;
                    pathing.anInt10319++;
                }
            }
        }

        ArrayUtils.quicksort(NPC_INDEX_REFERENCES, ENTITY_INDEX_REFERENCES, 0, anInt7304 - 1);
    }

    public static void setGameState(GameState state) {
        if (state != gameState) {
            anInt7396 = 0;
            if (state == GameState.UNK_10 || state == GameState.UNK_17) {
                LoginManager.method1384();
            }

            if (state != GameState.UNK_10 && WorldConnection.gameConnection != null) {
                WorldConnection.gameConnection.close();
                WorldConnection.gameConnection = null;
            }

            if (state == GameState.UNK_5) {
                ClientStartup.preparePlayer(gameState == GameState.IN_ACCOUNT_CREATION || gameState == GameState.UNK_8 || BASE_WINDOW_ID != client.ENTITY_DEFAULTS.loginInterfaceId);
            }

            if (state == GameState.UNK_0) {
                LoginManager.openLobby(BASE_WINDOW_ID != client.ENTITY_DEFAULTS.lobbyWindow);
            }

            if (state != GameState.UNK_14 && state != GameState.UNK_12) {
                if (state == GameState.UNK_19 || state == GameState.UNK_7 && gameState != GameState.UNK_6) {
                    LoginManager.method1384();
                } else if (state == GameState.UNK_8) {
                    AccountCreationStage.method4251();
                }
            } else {
                LoginManager.method5921();
            }

            if (GameState.isLoadingRegion(state)) {
                Resource.mapRegion.setLoadingRegion();
                GraphicsCache.resetWorldmapCaches(true);
            }

            boolean isOutsideGame = state == GameState.UNK_1 || GameState.isLoggedOut(state) || GameState.inLobby(state);
            boolean wasOutsideGame = gameState == GameState.UNK_1 || GameState.isLoggedOut(gameState) || GameState.inLobby(gameState);
            if (isOutsideGame != wasOutsideGame) {
                if (isOutsideGame) {
                    AreaSoundPlayer.currentMusicArchive = AreaSoundPlayer.titlescreenMusicArchive;
                    if (PreferenceManager.preferences.soundLevel.getValue() != 0) {
                        AreaSoundPlayer.musicSourceUpdate(Resource.MUSIC, AreaSoundPlayer.titlescreenMusicArchive, PreferenceManager.preferences.soundLevel.getValue());
                        SongPlayer.method12658();
                    } else {
                        disableTitlescreenMusic();
                    }

                    Resource.remoteFileRequesterJs5.sendLoginState(false);
                } else {
                    disableTitlescreenMusic();
                    Resource.remoteFileRequesterJs5.sendLoginState(true);
                }
            }

            if (GameState.isLoadingRegion(state) || state == GameState.UNK_10 || state == GameState.UNK_17) {
                Renderers.activeToolkit.renderLastFrame();
            }

            gameState = state;
        }

    }

    public static void logout(boolean reconnect) {
        BufferedConnectionContext[] contexts = connectionContexts;
        for (int index = 0; index < contexts.length; index++) {
            BufferedConnectionContext connectionContext = contexts[index];
            try {
                connectionContext.flush();
            } catch (IOException ignored) {
                // NOOP
            }
            connectionContext.closeConnection();
        }
        LoginManager.method5558();
        TypeLists.resetCachedData(false);
        Resource.mapRegion.resetInteractiveEntities();
        Resource.mapRegion.resetBasePosition();
        GraphicsCache.resetWorldmapCaches(false);
        disableTitlescreenMusic();
        AreaSoundPlayer.currentMusicArchive = -1;
        SoundEffectPlayer.soundJinglePlaying = false;
        SongPlayer.method12658();
        AmbientSound.unlinkSounds(true);
        AbstractNativeToolkit.drawLoadingPleaseWaitHardware();
        method3367();
        client.method14562();
        if (reconnect) {
            setGameState(GameState.UNK_12);
        } else {
            setGameState(GameState.UNK_5);
        }
    }

    public static boolean hasDepthFlags(Component component) {
        IFEvents targetParams = getComponentSettings(component);
        return targetParams.depthFlags() > 0 || (targetParams.ignoresDepthFlags() || component.depthChildOverride != null);
    }

    public static void buildCanvasComponents() {
        KeyRecorder.keyRecorder.addKeyListeners();
        MouseRecord.recorder.reset();
        ClientStartup.activeClient.buildCanvas();
        gameCanvas.setBackground(Color.black);
        CURRENT_CURSOR = -1;
        KeyRecorder.keyRecorder = CanvasKeyRecorder.create(gameCanvas);
        MouseRecord.recorder = AbstractMouseManager.inializeMouseRecorder(gameCanvas);
    }

    public static void setLoading() {
        setGameState(GameState.LOADING_PLEASE_WAIT);
        AbstractNativeToolkit.drawLoadingPleaseWaitHardware();
        System.gc();
    }

    public static void shutdownIcmpservice() {
        IcmpServiceJaclib.exit();
    }

    public static long getCurrentCycle() {
        return cycleManager.getCurrentCycle();
    }

    public static void method3367() {
        WorldMapSub1.aClass4658025.clear();
        WorldMapSub1.aClass4658029.clear();
    }

    public static void method4195() {
        if (!aBool7269) {
            aFloat7266 += (24.0F - aFloat7266) / 2.0F;
            sendCameraAngleToServer = true;
            aBool7269 = true;
        }
    }

    public static boolean method4198(GameState i_0) {
        return i_0 == GameState.UNK_5 || i_0 == GameState.UNK_14 || i_0 == GameState.UNK_19 || i_0 == GameState.IN_ACCOUNT_CREATION || i_0 == GameState.UNK_8 || i_0 == GameState.UNK_9;
    }

    public static boolean method5789(GameState i_0) {
        return i_0 == GameState.UNK_0 || i_0 == GameState.UNK_7;
    }

    public static void disableTitlescreenMusic() {
        SoundResourceManager.anInt1730 = 1;
        AreaSoundPlayer.MUSIC_RESOURCE = null;
        SongPlayer.musicArchiveId = -1;
        SoundResourceManager.musicFileId = -1;
        SoundResourceManager.aNodeSub15Sub27836 = null;
        AreaSoundPlayer.SOUND_LEVEL = 0;
        StaticMethods.aBool1962 = false;
        CutsceneBasicMovement.anInt9440 = 2;
        SongPlayer.songTransition = null;
    }

    public static boolean requestVarVerifyid() {
        varVerifyId++;
        sendTransmitVarVerifyid = true;
        return true;
    }

    public static void method15386() {
        if (REBOOT_TIMER > 1) {
            REBOOT_TIMER--;
            anInt7397 = anInt7347;
        }
        if (GAME_CONNECTION_CONTEXT.dropped) {
            GAME_CONNECTION_CONTEXT.dropped = false;
            WorldConnection.dropClient();
        } else {
            if (!MiniMenu.isOpen) {
                MiniMenu.resetEntries();
            }
            for (int i_1 = 0; i_1 < 100 && PacketDecoder.decodePacketFromStream(GAME_CONNECTION_CONTEXT); i_1++) {
            }
            if (gameState == GameState.LOGGED_INGAME) {
                while (ReflectionCheckManager.hasPendingChecks()) {
                    TcpPacket reflectionCheckPacket = PacketEncoder.createPacket(ClientProt.REFLECTION_CHECK, GAME_CONNECTION_CONTEXT.isaac);
                    reflectionCheckPacket.buffer.writeByte(0);
                    int position = reflectionCheckPacket.buffer.currentPosition;
                    ReflectionCheckManager.collectResult(reflectionCheckPacket.buffer);
                    reflectionCheckPacket.buffer.writeIndex(reflectionCheckPacket.buffer.currentPosition - position);
                    GAME_CONNECTION_CONTEXT.queuePacket(reflectionCheckPacket);
                }
                if (PingRequester.CURRENT_REQUEST != null) {
                    if (PingRequester.CURRENT_REQUEST.ping != -1) {
                        TcpPacket writePingPacket = PacketEncoder.createPacket(ClientProt.WRITE_PING, GAME_CONNECTION_CONTEXT.isaac);
                        writePingPacket.buffer.writeShort(PingRequester.CURRENT_REQUEST.ping);
                        GAME_CONNECTION_CONTEXT.queuePacket(writePingPacket);
                        PingRequester.CURRENT_REQUEST = null;
                        PingRequester.NEXT_PING_UPDATE_TO_SERVER = Utils.time() + 30000L;
                    }
                } else if (Utils.time() >= PingRequester.NEXT_PING_UPDATE_TO_SERVER) {
                    PingRequester.CURRENT_REQUEST = PING_REQUESTER.createPingRequest(LoginManager.GAME_CONNECTION_INFO.host);
                }
                PacketEncoder.submitQueuedPackets();
                MouseRecord record = (MouseRecord) MOUSE_MOVEMENTS.getFirst();
                if (Resource.mapRegion.getScene() != null) {
                    if (Camera.cameraCalculationType == 5) {
                        Camera.calculateCameraPosition();
                    } else if (Camera.cameraCalculationType == 3) {
                        Camera.calculateCutsceneCameraPosition();
                    }
                }
                if (aBool7331) {
                    aBool7331 = false;
                } else {
                    aFloat7284 /= 2.0F;
                }
                if (aBool7269) {
                    aBool7269 = false;
                } else {
                    aFloat7266 /= 2.0F;
                }
                ComponentMinimap.drawGame();
                if (gameState == GameState.LOGGED_INGAME) {
                    Resource.mapRegion.getEnvironmentManager().updateAtmosphere(Resource.mapRegion);
                    LocAction.transformObjects();
                    AreaSoundPlayer.method1623();
                    if (anInt7396 > 10) {
                        ++GAME_CONNECTION_CONTEXT.idleReadPulses;
                    }
                    if (GAME_CONNECTION_CONTEXT.idleReadPulses > 2250) {
                        WorldConnection.dropClient();
                    } else {
                        if (MAP_LOADING_STAGE == 1) {
                            PlayerUpdate.movePlayers();
                            NPCUpdate.moveNpcs();
                        } else if (TypeLists.CUTSCENE_DEFAULTS.exitCutscene.valid(record, KEY_RECORDS, maximumHeldKeys, KeyRecorder.keyRecorder)) {
                            PacketEncoder.queueCutsceneExit(false);
                        } else {
                            if (MAP_LOADING_STAGE == 3 && CutsceneTypeList.isFullyCached(CURRENT_CUTSCENE)) {
                                Resource.mapRegion.initialize(new RegionLoadRequest(RegionLoadingType.CUTSCENE, null));
                                MAP_LOADING_STAGE = 0;
                            }
                            if (MAP_LOADING_STAGE == 0 && gameState != GameState.LOADING_PLEASE_WAIT) {
                                CutsceneTypeList.CONFIG_CUTSCENES.clear();
                                MAP_LOADING_STAGE = 4;
                                cycleCutsceneLoaded = cycles;
                                CUTSCENE_MISC_ACTION_INDEX = 0;
                                StaticMethods.method3953();
                            }
                            if (MAP_LOADING_STAGE == 4) {
                                int cyclesPassed = cycles - cycleCutsceneLoaded;
                                if (CUTSCENE_MISC_ACTION_INDEX < CutsceneTypeList.CUTSCENE_MISC_ACTIONS.length) {
                                    do {
                                        CutsceneAction action = CutsceneTypeList.CUTSCENE_MISC_ACTIONS[CUTSCENE_MISC_ACTION_INDEX];
                                        if (action.lengthInCycles > cyclesPassed) {
                                            break;
                                        }
                                        action.perform();
                                    } while (MAP_LOADING_STAGE == 4 && ++CUTSCENE_MISC_ACTION_INDEX < CutsceneTypeList.CUTSCENE_MISC_ACTIONS.length);
                                }
                                if (MAP_LOADING_STAGE == 4) {
                                    for (int index = 0; index < CutsceneTypeList.CUTSCENE_ENTITIES.length; index++) {
                                        CutsceneEntity cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES[index];
                                        if (cutsceneEntity.initialized) {
                                            PathingEntity pathingEntity = cutsceneEntity.getEntity();
                                            EntityUpdating.move(pathingEntity, true);
                                        }
                                    }
                                }
                            }
                        }
                        PlayerUpdate.method6233();
                        MapRegion.cycleGfx();
                        PulseEventHandler.processPulseEvents();
                        ++animatingStep;
                        if (crossType != 0) {
                            crossSpritePtr += 20;
                            if (crossSpritePtr >= 400) {
                                crossType = 0;
                            }
                        }
                        if (MenuInteraction.aClass1184825 != null) {
                            ++anInt7297;
                            if (anInt7297 >= 15) {
                                Component.redrawComponent(MenuInteraction.aClass1184825);
                                MenuInteraction.aClass1184825 = null;
                            }
                        }
                        hoveredComponent = null;
                        isDraggingComponent = false;
                        isComponentSelected = false;
                        WorldMap.worldmapComponent = null;
                        InterfaceComponentRenderer.method544(null, -1, -1);
                        if (!interfaceSelectionActive) {
                            defaultMenuCursor = -1;
                        }
                        Interface.ifHover();
                        ++anInt7347;
                        if (CommandExecutor.teleportCommandRequested) {
                            TcpPacket tpPacket = PacketEncoder.createPacket(ClientProt.WORLD_MAP_CLICK, GAME_CONNECTION_CONTEXT.isaac);
                            tpPacket.buffer.writeIntLittle(CommandExecutor.teleportCommandPositionZ << 28 | CommandExecutor.teleportCommandPositionX << 14 | CommandExecutor.teleportCommandPositionY);
                            GAME_CONNECTION_CONTEXT.queuePacket(tpPacket);
                            CommandExecutor.teleportCommandRequested = false;
                        }

                        while (true) {
                            Component icomponentdefinitions10;
                            Component icomponentdefinitions11;
                            HookRequest hookRequest;
                            do {
                                hookRequest = (HookRequest) aClass4827233.popHead();
                                if (hookRequest == null) {
                                    while (true) {
                                        do {
                                            hookRequest = (HookRequest) aClass4827404.popHead();
                                            if (hookRequest == null) {
                                                while (true) {
                                                    do {
                                                        hookRequest = (HookRequest) PENDING_HOOK_REQUESTS.popHead();
                                                        if (hookRequest == null) {
                                                            if (WorldMap.worldmapComponent == null) {
                                                                menuItemCount = 0;
                                                            }
                                                            if (draggingComponent != null) {
                                                                client.method2827();
                                                            }
                                                            MenuInteraction.method7703();
                                                            /**
                                                             * Shift & Control pressed --> Scroll mouse = change plane
                                                             */
                                                            if (PLAYER_RIGHTS > 0 && KeyRecorder.keyRecorder.held(82) && KeyRecorder.keyRecorder.held(81) && anInt7191 != 0) {
                                                                int plane = PlayerUpdate.MY_PLAYER.plane - anInt7191;
                                                                if (plane < 0) {
                                                                    plane = 0;
                                                                } else if (plane > 3) {
                                                                    plane = 3;
                                                                }
                                                                CoordGrid base = Resource.mapRegion.getBaseCoordinates();
                                                                CommandExecutor.executeTeleportCommand(plane, PlayerUpdate.MY_PLAYER.movementBaseX[0] + base.x, PlayerUpdate.MY_PLAYER.movementBaseY[0] + base.y);
                                                            }
                                                            for (int index = 0; index < 5; index++) {
                                                                anIntArray7435[index]++;
                                                            }
                                                            if (clientIsActive && exitTime < Utils.time() - 60000L) {
                                                                PacketEncoder.writeVarcPreferences();
                                                            }
                                                            for (LoginStateUpdate loginStateUpdate = (LoginStateUpdate) PLAYER_LOGINSTATE_UPDATES.getFirst(); loginStateUpdate != null; loginStateUpdate = (LoginStateUpdate) PLAYER_LOGINSTATE_UPDATES.getNext()) {
                                                                if (loginStateUpdate.time < Utils.time() / 1000L - 5L) {
                                                                    if (loginStateUpdate.actionType > 0) {
                                                                        ChatBox.writeMessage(5, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_IN.translate(CURRENT_LANGUAGE));
                                                                    }
                                                                    if (loginStateUpdate.actionType == 0) {
                                                                        ChatBox.writeMessage(5, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_OUT.translate(CURRENT_LANGUAGE));
                                                                    }
                                                                    loginStateUpdate.unlinkNode();
                                                                }
                                                            }
                                                            ++anInt7418;
                                                            if (anInt7418 > 508) {
                                                                anInt7418 = 0;
                                                                int direction = (int) (Math.random() * 8.0D);
                                                                if ((direction & 0x1) == 1) {
                                                                    Camera.cameraRandomisationH += Camera.cameraStepH;
                                                                }
                                                                if ((direction & 0x2) == 2) {
                                                                    Camera.cameraRandomisationV += Camera.cameraStepV;
                                                                }
                                                                if ((direction & 0x4) == 4) {
                                                                    Camera.cameraRandomisationA += Camera.cameraStepA;
                                                                }
                                                            }
                                                            if (Camera.cameraRandomisationH < -51) {
                                                                Camera.cameraStepH = 2;
                                                            }
                                                            if (Camera.cameraRandomisationH > 56) {
                                                                Camera.cameraStepH = -2;
                                                            }
                                                            if (Camera.cameraRandomisationV < -63) {
                                                                Camera.cameraStepV = 2;
                                                            }
                                                            if (Camera.cameraRandomisationV > 56) {
                                                                Camera.cameraStepV = -2;
                                                            }
                                                            if (Camera.cameraRandomisationA < -44) {
                                                                Camera.cameraStepA = 1;
                                                            }
                                                            if (Camera.cameraRandomisationA > 40) {
                                                                Camera.cameraStepA = -1;
                                                            }
                                                            ++anInt7259;
                                                            if (anInt7259 > 508) {
                                                                anInt7259 = 0;
                                                                int i_2 = (int) (Math.random() * 8.0D);
                                                                if ((i_2 & 0x1) == 1) {
                                                                    minimapRotation += anInt7187;
                                                                }
                                                                if ((i_2 & 0x2) == 2) {
                                                                    minimapZoom += anInt7258;
                                                                }
                                                            }
                                                            if (minimapRotation < -60) {
                                                                anInt7187 = 2;
                                                            }
                                                            if (minimapRotation > 65) {
                                                                anInt7187 = -2;
                                                            }
                                                            if (minimapZoom < -20) {
                                                                anInt7258 = 1;
                                                            }
                                                            if (minimapZoom > 11) {
                                                                anInt7258 = -1;
                                                            }
                                                            if (++GAME_CONNECTION_CONTEXT.lastFlush > 50) {
                                                                TcpPacket pingPacket = PacketEncoder.createPacket(ClientProt.PING, GAME_CONNECTION_CONTEXT.isaac);
                                                                GAME_CONNECTION_CONTEXT.queuePacket(pingPacket);
                                                            }
                                                            if (sendTransmitVarVerifyid) {
                                                                PacketEncoder.sendTransmitVarVerifyid();
                                                                sendTransmitVarVerifyid = false;
                                                            }
                                                            try {
                                                                GAME_CONNECTION_CONTEXT.flush();
                                                            } catch (IOException ioexception) {
                                                                WorldConnection.dropClient();
                                                            }
                                                            return;
                                                        }
                                                        icomponentdefinitions10 = hookRequest.source;
                                                        if (icomponentdefinitions10.slotId < 0) {
                                                            break;
                                                        }
                                                        icomponentdefinitions11 = Component.fromHash(icomponentdefinitions10.parentId);
                                                    } while (icomponentdefinitions11 == null || icomponentdefinitions11.slotChildren == null || icomponentdefinitions10.slotId >= icomponentdefinitions11.slotChildren.length || icomponentdefinitions10 != icomponentdefinitions11.slotChildren[icomponentdefinitions10.slotId]);
                                                    CS2Executor.executeHookInner(hookRequest);
                                                }
                                            }
                                            icomponentdefinitions10 = hookRequest.source;
                                            if (icomponentdefinitions10.slotId < 0) {
                                                break;
                                            }
                                            icomponentdefinitions11 = Component.fromHash(icomponentdefinitions10.parentId);
                                        } while (icomponentdefinitions11 == null || icomponentdefinitions11.slotChildren == null || icomponentdefinitions10.slotId >= icomponentdefinitions11.slotChildren.length || icomponentdefinitions10 != icomponentdefinitions11.slotChildren[icomponentdefinitions10.slotId]);
                                        CS2Executor.executeHookInner(hookRequest);
                                    }
                                }
                                icomponentdefinitions10 = hookRequest.source;
                                if (icomponentdefinitions10.slotId < 0) {
                                    break;
                                }
                                icomponentdefinitions11 = Component.fromHash(icomponentdefinitions10.parentId);
                            } while (icomponentdefinitions11 == null || icomponentdefinitions11.slotChildren == null || icomponentdefinitions10.slotId >= icomponentdefinitions11.slotChildren.length || icomponentdefinitions10 != icomponentdefinitions11.slotChildren[icomponentdefinitions10.slotId]);
                            CS2Executor.executeHookInner(hookRequest);
                        }
                    }
                }
            }
        }
    }

    public static ClanChannel CLAN_CHANNEL;
    public static String aString3643;
    public static int anInt3644;
    public static String aString5555;
    public static Component frameComponent;
    public static EntityDefaults ENTITY_DEFAULTS;
    public static ClanSettings CLAN_SETTINGS;
    public static ClanSettings GUEST_CLAN_SETTINGS;
    public static short[] recolorPalette;
    public static int randomSeed;
    public static int COUNTRY;
    public static ClanChannel LISTENED_CLAN_CHANNEL;
    public static int anInt2831;
    public static byte[][] TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA;
    public static int JCOINS;

    @Override
    public void init() {
        String buildEnvironmentMinor = "";
        Parameter[] parameters = Parameter.values();

        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            String paramString = GameShell.suppliedApplet.getParameter(parameter.parameterName);
            if (paramString != null) {
                switch (Integer.parseInt(parameter.parameterName)) {
                    case 1:
                        base64Url = Base64.decode(TextUtils.decodeUrl(paramString));
                        if (base64Url.length < 16) {
                            base64Url = null;
                        }
                        break;
                    case 2:
                        aBool7158 = "true".equalsIgnoreCase(paramString);
                        break;
                    case 3:
                        aString7165 = paramString;
                        break;
                    case 4:
                        aString7156 = paramString;
                        if (aString7156.length() > 100) {
                            aString7156 = null;
                        }
                        break;
                    case 5:
                        if (WorldConnection.LOBBY_CONNECTION_INFO == null) {
                            WorldConnection.LOBBY_CONNECTION_INFO = new ConnectionInfo();
                        }

                        WorldConnection.LOBBY_CONNECTION_INFO.host = paramString;
                        break;
                    case 6:
                        client.COUNTRY = Integer.parseInt(paramString);
                        break;
                    case 7:
                        buildEnvironmentMinor = paramString;
                        break;
                    case 8:
                        CURRENT_GAME = Game.forid(Integer.parseInt(paramString));
                        break;
                    case 9:
                        isApplet = "true".equalsIgnoreCase(paramString);
                        break;
                    case 10:
                        showErrorOnFailedZap = "true".equalsIgnoreCase(paramString);
                        break;
                    case 11:
                        addGamescreenPadding = "true".equalsIgnoreCase(paramString);
                    case 12:
                    case 21:
                        break;
                    case 13:
                        client.aString5555 = paramString;
                        break;
                    case 14:
                        if (WorldConnection.LOBBY_CONNECTION_INFO == null) {
                            WorldConnection.LOBBY_CONNECTION_INFO = new ConnectionInfo();
                        }

                        WorldConnection.LOBBY_CONNECTION_INFO.worldId = Integer.parseInt(paramString);
                        break;
                    case 15:
                        BUILD_ENVIRONMENT = BuildType.parse(Integer.parseInt(paramString));
                        if (BUILD_ENVIRONMENT != BuildType.WIP && BUILD_ENVIRONMENT != BuildType.RC && BUILD_ENVIRONMENT != BuildType.BUILD_LIVE && BUILD_ENVIRONMENT != BuildType.LIVE) {
                            BUILD_ENVIRONMENT = BuildType.LIVE;
                        }
                        break;
                    case 16:
                        WorldConnection.GAME_CONNECTION = new ConnectionInfo();
                        WorldConnection.GAME_CONNECTION.worldId = Integer.parseInt(paramString);
                        break;
                    case 17:
                        aString7164 = paramString;
                        break;
                    case 18:
                        if ("true".equalsIgnoreCase(paramString)) {
                            IS_MEMBER = true;
                        }
                        break;
                    case 19:
                    default:
                        PacketEncoder.sendErrorToServer("", new RuntimeException());
                        break;
                    case 20:
                        loginScreenImageResourceNames = paramString;
                        break;
                    case 22:
                        aBool7160 = "true".equalsIgnoreCase(paramString);
                        break;
                    case 23:
                        AFFILIATE = Integer.parseInt(paramString);
                        break;
                    case 24:
                        ServerEnvironment.SERVER_ENVIRONMENT = (ServerEnvironment) Identifiable.identify(ServerEnvironment.values(), Integer.parseInt(paramString));
                        if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LOCAL) {
                            ServerEnvironment.SERVER_ENVIRONMENT = ServerEnvironment.WTWIP;
                        } else if (!ServerEnvironment.isHostedTestEnvironment(ServerEnvironment.SERVER_ENVIRONMENT) && ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
                            ServerEnvironment.SERVER_ENVIRONMENT = ServerEnvironment.LIVE;
                        }
                        break;
                    case 25:
                        aBool7159 = "true".equalsIgnoreCase(paramString);
                        break;
                    case 26:
                        aLong7153 = Long.parseLong(paramString);
                        break;
                    case 27:
                        anInt7163 = Integer.parseInt(paramString);
                        break;
                    case 28:
                        aBool7189 = "true".equals(paramString);
                        break;
                    case 29:
                        aBool7151 = "true".equals(paramString);
                        break;
                    case 30:
                        client.anInt2831 = Integer.parseInt(paramString);
                        break;
                    case 31:
                        CURRENT_LANGUAGE = Language.getLanguage(Integer.parseInt(paramString));
                        break;
                    case 32:
                        client.aString3643 = paramString;
                        break;
                    case 33:
                        affiliateP = paramString;
                        break;
                    case 34:
                        Browser.errorWebsiteBaseUrl = paramString;
                        break;
                    case 35:
                        DEBUG_DISPLAY_COLOR_INDEX = Integer.parseInt(paramString);
                        if (DEBUG_DISPLAY_COLOR_INDEX < 0 || DEBUG_DISPLAY_COLOR_INDEX >= DEBUG_LOAD_COLOR.length) {
                            DEBUG_DISPLAY_COLOR_INDEX = 0;
                        }
                }
            }
        }

        if (affiliateP == null) {
            affiliateP = "";
        }

        assert CURRENT_GAME != null;
        WindowSettings windowSettings = new WindowSettings(GAME_WIDTH, GAME_HEIGHT, BORDER_WIDTH, BORDER_HEIGHT, CURRENT_GAME.formattedName);
        ClientStartup.activeClient = this;
        String buildEnvironmentMajor = BUILD_ENVIRONMENT.name;
        int storeId = BUILD_ENVIRONMENT.id + 32;
        if (!buildEnvironmentMinor.isEmpty()) {
            buildEnvironmentMajor = buildEnvironmentMajor + "_" + buildEnvironmentMinor;
            storeId = 0;
        }

        initializeFrame(windowSettings, CURRENT_GAME.name, buildEnvironmentMajor, storeId, IndexReference.getMaxIndexReference(), addGamescreenPadding);

    }

    @Override
    void onPulse() {
        if (PreferenceManager.preferences.activeToolkit.getValue() == 2) {
            try {
                pulse();
            } catch (ThreadDeath threaddeath3) {
                throw threaddeath3;
            } catch (Throwable throwable4) {
                //was added throwable_4.printStackTrace();
                PacketEncoder.sendErrorToServer(throwable4.getMessage() + " " + getDebugString(), throwable4);
                renderWithCpu = true;
                client.loadAssets(0, false);
            }
        } else {
            pulse();
        }
    }

    public void pulse() {
        if (gameState != GameState.UNK_16) {
            //System.out.println("[Client]GameState: "+client.gameState);
            if (maploadingLockTaken) {
                synchronized (ASYNC_MAPLOADING_LOCK) {
                    ASYNC_MAPLOADING_LOCK.notify();
                }
                synchronized (MAP_REGION_LOADER_LOCK) {
                    try {
                        MAP_REGION_LOADER_LOCK.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }

                maploadingLockTaken = false;
            }


            ++cycles;
            if (cycles % 1000 == 1) {
                GregorianCalendar calender = new GregorianCalendar();
                client.randomSeed = calender.get(Calendar.HOUR_OF_DAY) * 600 + calender.get(Calendar.MINUTE) * 10 + calender.get(Calendar.SECOND) / 6;
                RANDOMIZER.setSeed(client.randomSeed);
            }

            GAME_CONNECTION_CONTEXT.cycle();
            LOBBY_CONNECTION_CONTEXT.cycle();
            pulseFileRequester();
            if (requestManager != null) {
                requestManager.pulse();
            }

            StaticMethods.method8020();
            VorbisNode.method15536();
            KeyRecorder.keyRecorder.prepareAwaitingKeys();
            MouseRecord.recorder.method3589();
            if (Renderers.activeToolkit != null) {
                Renderers.activeToolkit.animateTextures((int) Utils.time());
            }

            keyCachePointerPoss = 0;
            maximumHeldKeys = 0;

            for (KeyRecord record = KeyRecorder.keyRecorder.popHead(); record != null; record = KeyRecorder.keyRecorder.popHead()) {
                int type = record.getType();
                if (type != 2 && type != 3) {
                    if (type == 0 && maximumHeldKeys < 75) {
                        KEY_RECORDS[maximumHeldKeys++] = record;
                    }
                } else {
                    char keyCode = record.getCharacter();
                    if (GameState.isIngame() && (keyCode == 96 || keyCode == 167 || keyCode == 178)) {
                        if (Console.isOpen()) {
                            Console.close();
                        } else {
                            Console.open();
                        }
                    } else if (keyCachePointerPoss < 131) {
                        KEYS_PRESSED[keyCachePointerPoss++] = record;
                    }
                }
            }

            anInt7191 = 0;

            for (MouseRecord record = MouseRecord.recorder.nextSubmission(); record != null; record = MouseRecord.recorder.nextSubmission()) {
                int type = record.getClickType();
                if (type == -1) {
                    MouseRecord.record(record);
                } else if (type == 6) {
                    anInt7191 += record.getMeta();
                    record.pushToTop();
                } else if (MouseRecord.clicktypeRequiresRecord(type)) {
                    MOUSE_MOVEMENTS.addFirst(record);
                    if (MOUSE_MOVEMENTS.size() > 10) {
                        MouseRecord head = (MouseRecord) MOUSE_MOVEMENTS.popHead();
                        if (head != null) {
                            head.pushToTop();
                        }
                    }
                }
            }

            if (Console.isOpen()) {
                Console.processConsoleInput();
            }

            if (GameState.isOnloginScreen(gameState)) {
                LoadingStage.cycle();
                GameShell.resetDrawCache();
            } else if (GameState.isLoadingRegion(gameState)) {
                Resource.mapRegion.loadRegion();
            }

            if (GameState.isLoggedOut(gameState) && !GameState.isLoadingRegion(gameState)) {
                method12044();
                AccountCreationStage.handleAccountCreationStart();
                LoginManager.loginBlock();
            } else if (GameState.inLobby(gameState) && !GameState.isLoadingRegion(gameState)) {
                method12044();
                LoginManager.loginBlock();
            } else if (gameState == GameState.UNK_12) {
                LoginManager.loginBlock();
            } else if (GameState.isRenderingScene(gameState) && !GameState.isLoadingRegion(gameState)) {
                method15386();
            } else if (gameState == GameState.UNK_10 || gameState == GameState.UNK_17) {
                LoginManager.loginBlock();
                if (StaticMethods.anInt106 != -3 && StaticMethods.anInt106 != 2 && StaticMethods.anInt106 != 15) {
                    if (gameState == GameState.UNK_17) {
                        StaticMethods.anInt109 = LoginManager.anInt112;
                        StaticMethods.anInt108 = LoginManager.anInt94;
                        StaticMethods.anInt107 = StaticMethods.anInt106;
                        if (WorldConnection.reconnectingWorld) {
                            LoginManager.setGameHost(WorldConnection.connectionInfo.worldId, WorldConnection.connectionInfo.host);
                            GAME_CONNECTION_CONTEXT.reset();
                            setGameState(GameState.UNK_10);
                        } else {
                            logout(LoginManager.currentlyInLobby);
                        }
                    } else {
                        logout(false);
                    }
                }
            }

            InterfaceComponentRenderer.renderItems(Renderers.activeToolkit);
            MOUSE_MOVEMENTS.popHead();
        }

    }

    public void redraw() {
        if (gameState != GameState.UNK_16) {
            long long_2 = getCurrentCycle() / 1000000L - aLong7170;
            aLong7170 = getCurrentCycle() / 1000000L;
            boolean bool4 = SoundResourceManager.method7730();
            if (bool4 && SoundEffectPlayer.soundJinglePlaying && SoundEffectPlayer.jinglePlayer != null) {
                SoundEffectPlayer.jinglePlayer.start();
            }

            if (GameState.isWindowUpdate(gameState)) {
                if (WindowMode.nextDimensionUpdateCheck != 0L && Utils.time() > WindowMode.nextDimensionUpdateCheck) {
                    WindowMode.requestWindowMode(WindowMode.getWindowMode(), -1, -1, false);
                } else if (!Renderers.activeToolkit.supportsFullscreen() && clippingUpdateNeeded) {
                    WindowMode.exitFullscreen();
                }
            }
            if (WindowMode.fullScreenFrame == null) {
                Container activeContainer = WindowMode.getActiveContainer();
                int width = activeContainer.getSize().width;
                int height = activeContainer.getSize().height;
                if (activeContainer == engineFrame) {
                    Insets engineFrameInsets = engineFrame.getInsets();
                    width -= engineFrameInsets.left + engineFrameInsets.right;
                    height -= engineFrameInsets.bottom + engineFrameInsets.top;
                }

                if (width != WindowMode.resizableScreenWidth || height != WindowMode.resizableScreenHeight || needsResize) {
                    if (Renderers.activeToolkit != null && !Renderers.activeToolkit.forceFixedScreenmode()) {
                        WindowMode.resizableScreenWidth = width;
                        WindowMode.resizableScreenHeight = height;
                    } else {
                        WindowMode.adjustActiveWindow();
                    }

                    WindowMode.nextDimensionUpdateCheck = Utils.time() + 500L;
                    needsResize = false;
                }
            }

            if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null && !GameShell.appletHasFocus && GameState.isWindowUpdate(gameState)) {
                WindowMode.requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false);
            }

            boolean drawBackground = false;
            if (WindowMode.redrawWindowBackground) {
                WindowMode.redrawWindowBackground = false;
                drawBackground = true;
            }

            if (drawBackground) {
                WindowMode.fillWindowRect();
            }

            if (Renderers.activeToolkit != null && Renderers.activeToolkit.supportsFullscreen() || WindowMode.getWindowMode() != 1) {
                GameShell.setComponentsOnTop();
            }

            if (GameState.isOnloginScreen(gameState)) {
                LoadingStage.initialize(drawBackground);
            } else if (method4198(gameState)) {
                client.method11501();
            } else if (method5789(gameState)) {
                client.method11501();
            } else if (GameState.isLoadingRegion(gameState)) {
                if (Resource.mapRegion.getCurrentLoadingStage() == RegionLoadingStage.LOADING_MAPS) {
                    int completion = Resource.mapRegion.getCompletion() / 2;
                    MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE) + "<br>" + "(" + completion + "%)", true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
                } else if (Resource.mapRegion.getCurrentLoadingStage() == RegionLoadingStage.LOADING_LOCS) {
                    int completion = 50 + Resource.mapRegion.locLoadingPercent() / 2;
                    MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE) + "<br>" + "(" + completion + "%)", true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
                } else {
                    MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE), true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
                }
            } else if (gameState == GameState.LOGGED_INGAME) {
                client.method2601(long_2);
            } else if (gameState == GameState.UNK_10) {
                MessageBox.draw(LocalizedText.CONNECTION_LOST.translate(CURRENT_LANGUAGE) + "<br>" + LocalizedText.ATTEMPTING_TO_REESTABLISH.translate(CURRENT_LANGUAGE), false, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
            } else if (gameState == GameState.UNK_17) {
                MessageBox.draw(LocalizedText.PLEASE_WAIT.translate(CURRENT_LANGUAGE), false, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
            }

            if (anInt7412 == 3) {
                for (int index = 0; index < anInt7407; index++) {
                    Rectangle rectangle10 = INTERFACE_BG_COMPONENTS[index];
                    if (PREV_COMPONENT_ON_TOP[index]) {
                        assert Renderers.activeToolkit != null;
                        Renderers.activeToolkit.drawOpaqueRectangle(rectangle10.x, rectangle10.y, rectangle10.width, rectangle10.height, -65281);
                    } else {
                        assert Renderers.activeToolkit != null;
                        Renderers.activeToolkit.drawOpaqueRectangle(rectangle10.x, rectangle10.y, rectangle10.width, rectangle10.height, -16711936);
                    }
                }
            }

            if (Console.isOpen()) {
                assert Renderers.activeToolkit != null;
                Console.renderConsole(Renderers.activeToolkit);
            }

            if (!GameState.isOnloginScreen(gameState) && !GameState.isLoadingRegion(gameState) && BASE_WINDOW_ID != -1) {
                try {
                    assert Renderers.activeToolkit != null;
                    Renderers.activeToolkit.renderFrame();
                } catch (FrameRenderException sceneRenderException) {
                    PacketEncoder.sendErrorToServer(sceneRenderException.getMessage() + " " + getDebugString(), sceneRenderException);
                    client.loadAssets(0, false);
                }
            }

            VorbisNode.method6511();
            int cpuUsagePreference = PreferenceManager.preferences.cpu.getValue();
            if (cpuUsagePreference == 0) {
                TimeTools.sleep(15L);
            } else if (cpuUsagePreference == 1) {
                TimeTools.sleep(10L);
            } else if (cpuUsagePreference == 2) {
                TimeTools.sleep(5L);
            } else if (cpuUsagePreference == 3) {
                TimeTools.sleep(2L);
            }

            if (typelistCycleUpdate) {
                TypeLists.clean();
            }

            if (PreferenceManager.preferences.safeMode.getValue() == 1 && gameState == GameState.UNK_5 && BASE_WINDOW_ID != -1) {
                PreferenceManager.preferences.setValue(PreferenceManager.preferences.safeMode, 0);
                PreferenceManager.savePreferences();
            }
        }
    }

    @Override
    public String getDebugString() {
        String debug = " ";

        try {
            CoordGrid base = Resource.mapRegion.getBaseCoordinates();
            debug = debug + base.x + "," + base.y + "," + Resource.mapRegion.getSceneSizeX() + "," + Resource.mapRegion.getSceneSizeY() + " ";
            if (PlayerUpdate.MY_PLAYER != null) {
                debug = debug + PlayerUpdate.MY_PLAYER_PLANE + "," + (PlayerUpdate.MY_PLAYER.movementBaseX[0] + base.x) + "," + (PlayerUpdate.MY_PLAYER.movementBaseY[0] + base.y) + " ";
            } else {
                debug = debug + PlayerUpdate.MY_PLAYER_PLANE + "," + PlayerUpdate.MY_PLAYER_PLANE + "," + PlayerUpdate.MY_PLAYER_PLANE + "," + " ";
            }

            debug = debug + PreferenceManager.preferences.activeToolkit.getValue() + " " + PreferenceManager.preferences.antiAliasing.getValue() + " "
                    + WindowMode.getWindowMode() + " " + GameShell.screenCanvasWidth + "," + screenCanvasHeight * -969250379 + " ";
            debug = debug + PreferenceManager.preferences.lightDetail.getValue() + " ";
            debug = debug + PreferenceManager.preferences.sceneryShadows.getValue() + " ";
            debug = debug + PreferenceManager.preferences.water.getValue() + " ";
            debug = debug + PreferenceManager.preferences.textures.getValue() + " ";
            debug = debug + PreferenceManager.preferences.bloom.getValue() + " ";
            debug += "0 ";
            debug = debug + maxMemory + " ";
            debug = debug + gameState + " ";
            if (SystemInfo.SYSTEM_INFO != null) {
                debug += SystemInfo.SYSTEM_INFO.ram;
            } else {
                debug += -1;
            }

            debug += " ";
            if (aString7164 != null) {
                debug += aString7164;
            } else {
                debug += ",";
            }

            try {
                if (PreferenceManager.preferences.activeToolkit.getValue() == 2) {
                    Class<?> classLoader = Class.forName("java.lang.ClassLoader");
                    Field nativeLibsField = classLoader.getDeclaredField("nativeLibraries");
                    Class<?> accessibleObject = Class.forName("java.lang.reflect.AccessibleObject");
                    Method setAccessibleMethod = accessibleObject.getDeclaredMethod("setAccessible", Boolean.TYPE);
                    setAccessibleMethod.invoke(nativeLibsField, Boolean.TRUE);
                    Vector<?> objectVector = (Vector<?>) nativeLibsField.get(client.class.getClassLoader());

                    for (int index = 0; index < objectVector.size(); index++) {
                        try {
                            Object object = objectVector.elementAt(index);
                            Field field = object.getClass().getDeclaredField("name");
                            setAccessibleMethod.invoke(field, Boolean.TRUE);

                            try {
                                String fieldName = (String) field.get(object);
                                if (fieldName != null && fieldName.indexOf("sw3d.dll") != -1) {
                                    Field handleField = object.getClass().getDeclaredField("handle");
                                    setAccessibleMethod.invoke(handleField, Boolean.TRUE);
                                    debug = debug + " " + Long.toHexString(handleField.getLong(object));
                                    setAccessibleMethod.invoke(handleField, Boolean.FALSE);
                                }
                            } catch (Throwable throwable) {
                                // NOOP
                            }

                            setAccessibleMethod.invoke(field, Boolean.FALSE);
                        } catch (Throwable throwable) {
                            // NOOP
                        }
                    }
                }
            } catch (Throwable throwable) {
                // NOOP
            }
        } catch (Throwable throwable) {
            // NOOP
        }

        return debug;
    }

    public void pulseFileRequester() {
        boolean connected = Resource.remoteFileRequesterJs5.pulse();
        if (!connected) {
            setupJs5Connection();
        }
    }

    public void setupJs5Connection() {
        if (Resource.remoteFileRequesterJs5.failedRequests > failedRequests) {
            LoginManager.GAME_CONNECTION_INFO.swap();
            pulsesBeforeJs5Reconnect = Resource.remoteFileRequesterJs5.failedRequests * 250 - 250;
            if (pulsesBeforeJs5Reconnect > 3000) {
                pulsesBeforeJs5Reconnect = 3000;
            }

            if (Resource.remoteFileRequesterJs5.failedRequests >= 2 && Resource.remoteFileRequesterJs5.connectionStatus == 6) {
                raiseErrorScreen("js5connect_outofdate");
                gameState = GameState.UNK_16;
                return;
            }

            if (Resource.remoteFileRequesterJs5.failedRequests >= 1 && Resource.remoteFileRequesterJs5.connectionStatus == 48) {
                raiseErrorScreen("sessionexpired");
                gameState = GameState.UNK_16;
                return;
            }

            if (Resource.remoteFileRequesterJs5.failedRequests >= 4 && Resource.remoteFileRequesterJs5.connectionStatus == -1) {
                raiseErrorScreen("js5crc");
                gameState = GameState.UNK_16;
                return;
            }

            if (Resource.remoteFileRequesterJs5.failedRequests >= 4 && GameState.isOnloginScreen(gameState)) {
                if (Resource.remoteFileRequesterJs5.connectionStatus != 7 && Resource.remoteFileRequesterJs5.connectionStatus != 9) {
                    if (Resource.remoteFileRequesterJs5.connectionStatus > 0) {
                        if (js5ProxyAddress == null) {
                            raiseErrorScreen("js5connect");
                        } else {
                            raiseErrorScreen("js5proxy_" + js5ProxyAddress.trim());
                        }
                    } else {
                        raiseErrorScreen("js5io" + Resource.remoteFileRequesterJs5.connectionStatus);
                    }
                } else {
                    raiseErrorScreen("js5connect_full");
                }

                gameState = GameState.UNK_16;
                return;
            }
        }

        failedRequests = Resource.remoteFileRequesterJs5.failedRequests;
        if (pulsesBeforeJs5Reconnect > 0) {
            pulsesBeforeJs5Reconnect--;
            return;
        }
        try {
            if (updateStage == 0) {
                clientSocket = LoginManager.GAME_CONNECTION_INFO.createSocket();
                updateStage++;
            }

            if (updateStage == 1) {
                Connection.updateConnection = AsyncConnection.createAsyncConnection(clientSocket, 125000);
                int length = 13 + aString7164.length();
                Packet stream = new Packet(length + 4);
                stream.writeByte(LoginProt.INIT_JS5REMOTE_CONNECTION.id);
                stream.writeByte(length);
                stream.writeInt(Loader.CLIENT_BUILD);
                stream.writeInt(Loader.MAJOR_BUILD);
                stream.writeInt(Loader.MINOR_BUILD);
                stream.writeString(aString7164);
                Connection.updateConnection.write(stream.buffer, length + 2);
                updateStage++;
                js5ConnectTime = Utils.time();
            }

            if (updateStage == 2) {
                if (Connection.updateConnection.available(1)) {
                    byte[] js5Action = new byte[1];
                    int status = Connection.updateConnection.read(js5Action, 0, 1);
                    if (js5Action[0] == 25) {
                        return;
                    }

                    if (js5Action[0] != 0) {
                        updateJs5ConnectionStatus(status);
                        return;
                    }

                    updateStage++;
                } else if (Utils.time() - js5ConnectTime > 30000L) {
                    updateJs5ConnectionStatus(1001);
                    return;
                }
            }

            if (updateStage == 3) {
                Dependency[] dependencies = CacheableDependencies.getAll();
                int byteCount = dependencies.length * 4;
                if (Connection.updateConnection.available(byteCount)) {
                    Packet buffer = new Packet(byteCount);
                    Connection.updateConnection.read(buffer.buffer, 0, buffer.buffer.length);

                    for (int index = 0; index < dependencies.length; index++) {
                        dependencies[index].setArchiveCount(buffer.readInt());
                    }

                    boolean loggedOut = GameState.isOnloginScreen(gameState) || GameState.isLoggedOut(gameState) || GameState.inLobby(gameState);
                    Resource.remoteFileRequesterJs5.init(Connection.updateConnection, !loggedOut);
                    clientSocket = null;
                    Connection.updateConnection = null;
                    updateStage = 0;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            updateJs5ConnectionStatus(1002);
        }
    }

    public void updateJs5ConnectionStatus(int connectionStatus) {
        clientSocket = null;
        Connection.updateConnection = null;
        updateStage = 0;
        Resource.remoteFileRequesterJs5.failedRequests++;
        Resource.remoteFileRequesterJs5.connectionStatus = connectionStatus;
    }

    @Override
    void mainRedraw() {
        if (PreferenceManager.preferences.activeToolkit.getValue() == 2) {
            try {
                redraw();
            } catch (ThreadDeath threadDeath) {
                throw threadDeath;
            } catch (Throwable throwable) {
                PacketEncoder.sendErrorToServer(throwable.getMessage() + " " + getDebugString(), throwable);
                renderWithCpu = true;
                client.loadAssets(0, false);
            }
        } else {
            redraw();
        }
    }

    @Override
    void shutdown() {
        if (clientIsActive) {
            PacketEncoder.writeVarcPreferences();
        }

        LoadingStage.killLoginscreenThread();
        if (Renderers.activeToolkit != null) {
            Renderers.activeToolkit.cleanupAndShutdown();
        }

        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.disposeFrame(WindowMode.activeWindowMode, WindowMode.fullScreenFrame);
            WindowMode.fullScreenFrame = null;
        }

        GAME_CONNECTION_CONTEXT.closeConnection();
        GAME_CONNECTION_CONTEXT.pinger.disable();
        LOBBY_CONNECTION_CONTEXT.closeConnection();
        LOBBY_CONNECTION_CONTEXT.pinger.disable();
        SoundEffectPlayer.shutdownPlayers();
        Resource.remoteFileRequesterJs5.closeStream();
        Resource.localRequesterJs5.shutdown();
        if (PING_REQUESTER != null) {
            PING_REQUESTER.shutdown();
            PING_REQUESTER = null;
        }

        try {
            Ping.quit();
        } catch (Throwable ignored) {
            // NOOP
        }

        shutdownIcmpservice();

        try {
            Console.shutdown();
        } catch (Exception ignored) {
            // NOOP
        }

        if (raiseBrowserErrorscreen) {
            Browser.openBrowserErrorscreenOpenjs();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    void initializeEssentials() {
        Frame frame = new Frame(" ");
        frame.pack();
        frame.dispose();
        client.frameComponent = new Component();
        WindowMode.adjustActiveWindow();
        Resource.localRequesterJs5 = new LocalRequester();
        Resource.remoteFileRequesterJs5 = new RemoteFileRequester();
        CircularBuffer.initialize(new int[]{20, 260}, new int[]{1000, 100});
        Vector3Pool.initVectorStack();
        QuaternionPool.initialize();
        MouseActionClient.initialize();
        MouseActionServer.initialize();
        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
            client.TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA = new byte[50][];
        }

        PreferenceManager.preferences = PreferenceManager.getGamePreferences();
        if (PreferenceManager.preferences.asyncMaploading.getValue() == 1) {
            Scene.synchronousMaploading = false;
        }

        if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LIVE) {
            WorldConnection.GAME_CONNECTION.host = GameShell.suppliedApplet.getCodeBase().getHost();
        } else if (ServerEnvironment.isHostedTestEnvironment(ServerEnvironment.SERVER_ENVIRONMENT)) {
            WorldConnection.GAME_CONNECTION.host = GameShell.suppliedApplet.getCodeBase().getHost();
            WorldConnection.GAME_CONNECTION.devWorld1 = WorldConnection.GAME_CONNECTION.worldId * -1708079975 + 1140744768;
            WorldConnection.LOBBY_CONNECTION_INFO.devWorld1 = WorldConnection.LOBBY_CONNECTION_INFO.worldId * -1708079975 + 1140744768;
            WorldConnection.GAME_CONNECTION.devWorld2 = WorldConnection.GAME_CONNECTION.worldId * -1473668237 + 1047080176;
            WorldConnection.LOBBY_CONNECTION_INFO.devWorld2 = WorldConnection.LOBBY_CONNECTION_INFO.worldId * -1473668237 + 1047080176;
        } else if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LOCAL) {
            WorldConnection.GAME_CONNECTION.host = "127.0.0.1";
            WorldConnection.LOBBY_CONNECTION_INFO.host = "127.0.0.1";
            WorldConnection.GAME_CONNECTION.devWorld1 = WorldConnection.GAME_CONNECTION.worldId * -1708079975 + 1140744768;
            WorldConnection.LOBBY_CONNECTION_INFO.devWorld1 = WorldConnection.LOBBY_CONNECTION_INFO.worldId * -1708079975 + 1140744768;
            WorldConnection.GAME_CONNECTION.devWorld2 = WorldConnection.GAME_CONNECTION.worldId * -1473668237 + 1047080176;
            WorldConnection.LOBBY_CONNECTION_INFO.devWorld2 = WorldConnection.LOBBY_CONNECTION_INFO.worldId * -1473668237 + 1047080176;
        }

        LoginManager.GAME_CONNECTION_INFO = WorldConnection.GAME_CONNECTION;
        if (Game.RUNESCAPE == CURRENT_GAME) {
            playingAltGame = false;
        }

        client.recolorPalette = ObjectType.locRecolorPalette = NPCType.npcRecolorPalette = ItemType.recolorPalette = new short[256];

        try {
            Console.clipboard = WindowMode.getActiveContainer().getToolkit().getSystemClipboard();
        } catch (Exception exception) {
            // NOOP
        }

        KeyRecorder.keyRecorder = CanvasKeyRecorder.create(GameShell.gameCanvas);
        MouseRecord.recorder = AbstractMouseManager.inializeMouseRecorder(GameShell.gameCanvas);
        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
            DRAW_DEBUG = true;
        }

        translatedLoadingText = LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE);
        Resource.mapRegionLoaderTask = new AsyncMapLoader();
        (new Thread(Resource.mapRegionLoaderTask)).start();
    }

    public void method12044() {
        if (gameState == GameState.UNK_0 && !LoginManager.method3360() || gameState == GameState.UNK_7 && StaticMethods.anInt106 == 42 || gameState == GameState.IN_ACCOUNT_CREATION) {
            if (REBOOT_TIMER > 1) {
                REBOOT_TIMER--;
                anInt7397 = anInt7347;
            }

            if (!MiniMenu.isOpen) {
                MiniMenu.resetEntries();
            }

            for (int packetLoop = 0; packetLoop < 100 && PacketDecoder.decodePacketFromStream(LOBBY_CONNECTION_CONTEXT); packetLoop++) { }
        }

        PulseEventHandler.processPulseEvents();
        MouseActionServer.refreshCache();
        animatingStep++;
        MiniMenu.method6451(-1, -1);
        InterfaceComponentRenderer.method544(null, -1, -1);
        Interface.ifHover();
        anInt7347++;

        for (int index = 0; index < npcCount; index++) {
            NpcEntity npc = (NpcEntity) NPC_ARRAY[index].value;
            if (npc != null) {
                byte walkMask = npc.definitions.walkingFlags;
                if ((walkMask & 0x1) != 0) {
                    int size = npc.getSize();
                    if ((walkMask & 0x2) != 0 && npc.movementStepsLeft == 0 && Math.random() * 1000.0D < 10.0D) {
                        int random1 = (int) Math.round(Math.random() * 10.0D - 5.0D);
                        int random2 = (int) Math.round(Math.random() * 10.0D - 5.0D);
                        if (random1 != 0 || random2 != 0) {
                            int x = random1 + npc.movementBaseX[0];
                            int y = random2 + npc.movementBaseY[0];
                            if (x < 0) {
                                x = 0;
                            } else if (x > Resource.mapRegion.getSceneSizeX() - size - 1) {
                                x = Resource.mapRegion.getSceneSizeX() - size - 1;
                            }

                            if (y < 0) {
                                y = 0;
                            } else if (y > Resource.mapRegion.getSceneSizeY() - size - 1) {
                                y = Resource.mapRegion.getSceneSizeY() - size - 1;
                            }

                            int numSteps = RouteFinder.findRoute(npc.movementBaseX[0], npc.movementBaseY[0], size, RouteStrategies.forNpc(x, y, size, size),
                                    Resource.mapRegion.getCollisionMap(npc.plane), true, WALKING_STEPS_X, WALKING_STEPS_Y);
                            if (numSteps > 0) {
                                if (numSteps > 9) {
                                    numSteps = 9;
                                }

                                for (int step = 0; step < numSteps; step++) {
                                    npc.movementBaseX[step] = WALKING_STEPS_X[numSteps - step - 1];
                                    npc.movementBaseY[step] = WALKING_STEPS_Y[numSteps - step - 1];
                                    npc.movementTypes[step] = MovementType.WALKING.id;
                                }

                                npc.movementStepsLeft = numSteps;
                            }
                        }
                    }

                    EntityUpdating.updatePosition(npc, true);
                    int rotation = EntityUpdating.turnTo(npc);
                    EntityUpdating.rotateEquipment(npc);
                    PathingEntity.animateMovement(npc, EntityUpdating.currentMovementType, EntityUpdating.turnDirectionIndex, rotation);
                    PlayerUpdate.animateMovement(npc, EntityUpdating.currentMovementType);
                    PathingEntity.animateMovement(npc);
                    Quaternion quaternion = QuaternionPool.newQuaternion();
                    quaternion.rotateEuler(MathUtils.getTheta(npc.yaw.getRotation()), MathUtils.getTheta(npc.pitch.getRotation()), MathUtils.getTheta(npc.roll.getRotation()));
                    npc.rotationUpdate(quaternion);
                    quaternion.store();
                }
            }
        }

        if ((gameState == GameState.UNK_5 || gameState == GameState.UNK_7 || gameState == GameState.UNK_0 || gameState == GameState.IN_ACCOUNT_CREATION) && (!LoginManager.method3360() || gameState == GameState.UNK_7 && StaticMethods.anInt106 == 42) && !AccountCreationStage.method1466()) {
            if (Camera.cameraCalculationType == 5) {
                Camera.calculateCameraPosition();
            } else {
                Camera.calculateCutsceneCameraPosition();
            }

            if (Camera.cameraPositionAbsoluteX >> 9 < 14 || Camera.cameraPositionAbsoluteX >> 9 >= Resource.mapRegion.getSceneSizeX() - 14 || Camera.cameraPositionAbsoluteY >> 9 < 14 || Camera.cameraPositionAbsoluteY >> 9 >= Resource.mapRegion.getSceneSizeY() - 14) {
                Resource.mapRegion.initialize(new RegionLoadRequest(RegionLoadingType.ANIMATED_LOGIN_SCREEN, null));
            }
        }

        while (true) {
            HookRequest hookRequest;
            Component parentComponent;
            Component srcComponent;
            do {
                hookRequest = (HookRequest) aClass4827233.popHead();
                if (hookRequest == null) {
                    while (true) {
                        do {
                            hookRequest = (HookRequest) aClass4827404.popHead();
                            if (hookRequest == null) {
                                while (true) {
                                    do {
                                        hookRequest = (HookRequest) PENDING_HOOK_REQUESTS.popHead();
                                        if (hookRequest == null) {
                                            if (draggingComponent != null) {
                                                client.method2827();
                                            }

                                            if (gameState == GameState.UNK_0 && !LoginManager.method3360() || gameState == GameState.UNK_7 && StaticMethods.anInt106 == 42) {
                                                MenuInteraction.method7703();
                                            }

                                            WorldMeta.pingWorlds();
                                            if (clientIsActive && exitTime < Utils.time() - 60000L) {
                                                PacketEncoder.writeVarcPreferences();
                                            }

                                            for (LoginStateUpdate loginStateUpdate = (LoginStateUpdate) PLAYER_LOGINSTATE_UPDATES.getFirst(); loginStateUpdate != null; loginStateUpdate = (LoginStateUpdate) PLAYER_LOGINSTATE_UPDATES.getNext()) {
                                                if (loginStateUpdate.time < Utils.time() / 1000L - 5L) {
                                                    if (loginStateUpdate.actionType > 0) {
                                                        ChatBox.writeMessage(5, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_IN.translate(CURRENT_LANGUAGE));
                                                    }

                                                    if (loginStateUpdate.actionType == 0) {
                                                        ChatBox.writeMessage(5, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_OUT.translate(CURRENT_LANGUAGE));
                                                    }

                                                    loginStateUpdate.unlinkNode();
                                                }
                                            }

                                            if (gameState == GameState.UNK_0 && !LoginManager.method3360() || gameState == GameState.UNK_7 && StaticMethods.anInt106 == 42 || gameState == GameState.IN_ACCOUNT_CREATION) {
                                                if (gameState != GameState.UNK_7 && LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
                                                    logout(false);
                                                } else {
                                                    if (++LOBBY_CONNECTION_CONTEXT.lastFlush > 50) {
                                                        TcpPacket ping = PacketEncoder.createPacket(ClientProt.PING, LOBBY_CONNECTION_CONTEXT.isaac);
                                                        LOBBY_CONNECTION_CONTEXT.queuePacket(ping);
                                                    }

                                                    try {
                                                        LOBBY_CONNECTION_CONTEXT.flush();
                                                    } catch (IOException ioexception) {
                                                        if (gameState == GameState.UNK_7) {
                                                            LOBBY_CONNECTION_CONTEXT.closeConnection();
                                                        } else {
                                                            logout(false);
                                                        }
                                                    }
                                                }
                                            }

                                            return;
                                        }

                                        srcComponent = hookRequest.source;
                                        if (srcComponent.slotId < 0) {
                                            break;
                                        }

                                        parentComponent = Component.fromHash(srcComponent.parentId);
                                    } while (parentComponent == null || parentComponent.slotChildren == null || srcComponent.slotId >= parentComponent.slotChildren.length || srcComponent != parentComponent.slotChildren[srcComponent.slotId]);

                                    CS2Executor.executeHookInner(hookRequest);
                                }
                            }

                            srcComponent = hookRequest.source;
                            if (srcComponent.slotId < 0) {
                                break;
                            }

                            parentComponent = Component.fromHash(srcComponent.parentId);
                        } while (parentComponent == null || parentComponent.slotChildren == null || srcComponent.slotId >= parentComponent.slotChildren.length || srcComponent != parentComponent.slotChildren[srcComponent.slotId]);

                        CS2Executor.executeHookInner(hookRequest);
                    }
                }

                srcComponent = hookRequest.source;
                if (srcComponent.slotId < 0) {
                    break;
                }

                parentComponent = Component.fromHash(srcComponent.parentId);
            } while (parentComponent == null || parentComponent.slotChildren == null || srcComponent.slotId >= parentComponent.slotChildren.length || srcComponent != parentComponent.slotChildren[srcComponent.slotId]);

            CS2Executor.executeHookInner(hookRequest);
        }
    }

    public static void drawGameArea(int x, int y, int gameAreaWidth, int gameAreaHeight, boolean bool4) {
        if (Resource.mapRegion.getScene() == null) {
            Renderers.activeToolkit.clearPixels(x, y, gameAreaWidth, gameAreaHeight, -16777216);
        } else {
            Vector3 abs = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
            boolean clearPixels = false;
            if (MAP_LOADING_STAGE != 1) {
                if (!aBool7321) {
                    clearPixels = true;
                }
            } else if ((int) abs.x < 0 || (int) abs.x >= Resource.mapRegion.getSceneSizeX() * 512
                    || (int) abs.z < 0 || (int) abs.z >= Resource.mapRegion.getSceneSizeY() * 512) {
                clearPixels = true;
            }
            if (clearPixels) {
                Renderers.activeToolkit.clearPixels(x, y, gameAreaWidth, gameAreaHeight, -16777216);
            } else {
                timesSceneRendered++;
                if (PlayerUpdate.MY_PLAYER != null
                        && (int) abs.x - (PlayerUpdate.MY_PLAYER.getSize() - 1) * 256 >> 9 == ComponentMinimap.minimapFlagX
                        && (int) abs.z - (PlayerUpdate.MY_PLAYER.getSize() - 1) * 256 >> 9 == ComponentMinimap.minimapFlagY) {
                    ComponentMinimap.minimapFlagX = -1;
                    ComponentMinimap.minimapFlagY = -1;
                    PulseEventQueue.delayedClearMinimapFlagUpdate();
                }
                EntityUpdating.method14519();
                if (!bool4) {
                    MapRegion.addProjectileSpawnsToScene();
                }
                MapRegion.addGfxsToScene();
                AbstractToolkit.calculateGameArea(x, y, gameAreaWidth, gameAreaHeight, true);
                x = client.hintArrowOffsetX;
                y = client.hintArrowOffsetY;
                gameAreaWidth = client.gameAreaWidth;
                gameAreaHeight = client.gameAreaHeight;
                MiniMenu.method6451(x, y);
                if (Camera.cameraCalculationType == 2) {
                    int yaw = (int) Camera.camAngleX;
                    if (Camera.regularLowestYaw >> 8 > yaw) {
                        yaw = Camera.regularLowestYaw >> 8;
                    }
                    if (CAM_SHAKING[4] && CUSTOM_LOWEST_YAW[4] + 128 > yaw) {
                        yaw = CUSTOM_LOWEST_YAW[4] + 128;
                    }
                    int roll = (int) Camera.camAngleY + Camera.cameraRandomisationA & 0x3fff;
                    Camera.setCameraPosition(Camera.cameraBaseX, MapRegion.tileheightForAbsolutePosition((int) abs.x, (int) abs.z, PlayerUpdate.MY_PLAYER_PLANE) - 200, Camera.cameraBaseY, yaw, roll, (yaw >> 3) * 3 + Camera.cameraZoom << 2, gameAreaHeight);
                } else if (Camera.cameraCalculationType == 4) {
                    int yaw = (int) Camera.camAngleX;
                    if (Camera.regularLowestYaw >> 8 > yaw) {
                        yaw = Camera.regularLowestYaw >> 8;
                    }
                    if (CAM_SHAKING[4] && CUSTOM_LOWEST_YAW[4] + 128 > yaw) {
                        yaw = CUSTOM_LOWEST_YAW[4] + 128;
                    }
                    int roll = (int) Camera.camAngleY & 0x3fff;
                    Camera.setCameraPosition(Camera.cameraBaseX, MapRegion.tileheightForAbsolutePosition(anInt7262, anInt7376, PlayerUpdate.MY_PLAYER_PLANE) - 200, Camera.cameraBaseY, yaw, roll, (yaw >> 3) * 3 + Camera.cameraZoom << 2, gameAreaHeight);
                } else if (Camera.cameraCalculationType == 1) {
                    Camera.method3920(gameAreaHeight);
                }
                int camAbsX = Camera.cameraPositionAbsoluteX;
                int camAbsZ = Camera.cameraPositionAbsoluteZ;
                int camAbsY = Camera.cameraPositionAbsoluteY;
                int yaw = Camera.cameraYaw;
                int roll = Camera.cameraRoll;
                for (int slot = 0; slot < 5; slot++) {
                    if (CAM_SHAKING[slot]) {
                        int transition = (int) (Math.random() * (anIntArray7268[slot] * 2 + 1) - anIntArray7268[slot] +
                                Math.sin(anIntArray7435[slot] * (anIntArray7462[slot] / 100.0D)) * CUSTOM_LOWEST_YAW[slot]);
                        if (slot == 0) {
                            Camera.cameraPositionAbsoluteX += transition << 2;
                        }
                        if (slot == 1) {
                            Camera.cameraPositionAbsoluteZ += transition << 2;
                        }
                        if (slot == 2) {
                            Camera.cameraPositionAbsoluteY += transition << 2;
                        }
                        if (slot == 3) {
                            Camera.cameraRoll = transition + Camera.cameraRoll & 0x3fff;
                        }
                        if (slot == 4) {
                            Camera.cameraYaw += transition;
                            if (Camera.cameraYaw < 1024) {
                                Camera.cameraYaw = 1024;
                            } else if (Camera.cameraYaw > 3072) {
                                Camera.cameraYaw = 3072;
                            }
                        }
                    }
                }
                if (Camera.cameraPositionAbsoluteX < 0) {
                    Camera.cameraPositionAbsoluteX = 0;
                }
                if (Camera.cameraPositionAbsoluteX > (Resource.mapRegion.getScene().sceneDimensionX << 9) - 1) {
                    Camera.cameraPositionAbsoluteX = (Resource.mapRegion.getScene().sceneDimensionX << 9) - 1;
                }
                if (Camera.cameraPositionAbsoluteY < 0) {
                    Camera.cameraPositionAbsoluteY = 0;
                }
                if (Camera.cameraPositionAbsoluteY > (Resource.mapRegion.getScene().sceneDimensionY << 9) - 1) {
                    Camera.cameraPositionAbsoluteY = (Resource.mapRegion.getScene().sceneDimensionY << 9) - 1;
                }
                MapRegion.handleRoofDisplay();
                Renderers.activeToolkit.setDrawableArea(x, y, gameAreaWidth, gameAreaHeight);
                Renderers.activeToolkit.setClipping(x, y, gameAreaWidth + x, gameAreaHeight + y);
                Environment sceneAtmosphere = Resource.mapRegion.getEnvironmentManager().getEnvironment();
                int fogColor = sceneAtmosphere.getFogColor();
                SUN_MATRIX.setViewPosition(-Camera.cameraPositionAbsoluteX, -Camera.cameraPositionAbsoluteZ, -Camera.cameraPositionAbsoluteY);
                SUN_MATRIX.rotate(0.0F, -1.0f, 0.0F, MathUtils.getTheta(-Camera.cameraRoll & 0x3fff));
                SUN_MATRIX.rotate(-1.0f, 0.0F, 0.0F, MathUtils.getTheta(-Camera.cameraYaw & 0x3fff));
                SUN_MATRIX.rotate(0.0F, 0.0F, -1.0f, MathUtils.getTheta(-Camera.cameraPitch & 0x3fff));
                Renderers.activeToolkit.setSunPosition(SUN_MATRIX);
                StaticMethods.method3850(Camera.aClass3847167, gameAreaWidth / 2, gameAreaHeight / 2, anInt7451 << 1, anInt7451 << 1, gameAreaWidth, gameAreaHeight);
                Renderers.activeToolkit.setModelMatrix(Camera.aClass3847167);
                if (sceneAtmosphere.getSkybox() != null) {
                    Renderers.activeToolkit.updateSun(1.0F);
                    Renderers.activeToolkit.applyLighting(16777215, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
                    sceneAtmosphere.getSkybox().render(Renderers.activeToolkit, Environment.skyboxRotation << 3, x, y, gameAreaWidth, gameAreaHeight, Camera.cameraYaw, Camera.cameraRoll, Camera.cameraPitch, fogColor);
                } else {
                    Renderers.activeToolkit.applyClearFlags(3, fogColor);
                }
                Camera.method6934(SUN_MATRIX, Camera.aClass3847167, gameAreaWidth, gameAreaHeight);
                Resource.mapRegion.getEnvironmentManager().updateAtmosphere(Resource.mapRegion);
                byte roofPreference = PreferenceManager.preferences.removeRoofsOptionOverride.getValue() == 2 ? (byte) timesSceneRendered : 1;
                Resource.mapRegion.getScene().drawScene(cycles,
                        Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteZ, Camera.cameraPositionAbsoluteY, Resource.mapRegion.getBits(),
                        visibleBelowDrawheight, visibleAboveX, visibleBelowX, visibleBelowY, visibleAboveY,
                        PlayerUpdate.MY_PLAYER.plane + 1, roofPreference, PreferenceManager.preferences.flickeringEffects.getValue() == 0);
                anInt7396++;
                SoundEffectPlayer.updatePlayers();
                if (gameState == GameState.LOGGED_INGAME) {
                    StaticMethods.drawTileMessages(x, y);
                    EntityUpdating.drawEntitites(x, y, gameAreaWidth, gameAreaHeight);
                    HintRenderer.drawHintArrow(x, y);
                    StaticMedia.drawClickCross();
                }
                Resource.mapRegion.getScene().clearInteractives();
                Camera.cameraPositionAbsoluteX = camAbsX;
                Camera.cameraPositionAbsoluteZ = camAbsZ;
                Camera.cameraPositionAbsoluteY = camAbsY;
                Camera.cameraYaw = yaw;
                Camera.cameraRoll = roll;
                if (awaitingPriorityRequests && Resource.remoteFileRequesterJs5.priorityRequestCount() == 0) {
                    awaitingPriorityRequests = false;
                }
                if (awaitingPriorityRequests) {
                    Renderers.activeToolkit.clearPixels(x, y, gameAreaWidth, gameAreaHeight, -16777216);
                    MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE), false, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
                }
                StaticMethods.method3850(Camera.aClass3847167, (x + gameAreaWidth / 2), (gameAreaHeight / 2 + y), (anInt7451 << 1), (anInt7451 << 1), gameAreaWidth, gameAreaHeight);
                Renderers.activeToolkit.setModelMatrix(Camera.aClass3847167);
                Camera.method13296(Camera.aClass3847167);
            }
        }
    }

    public static void initializeTips() {
        if (LoadingStage.gametipsToDisplay != null) {
            LoginScreenComponent[] tips = LoadingStage.gametipsToDisplay;
            for (int index = 0; index < tips.length; index++) {
                LoginScreenComponent tip = tips[index];
                tip.initialize();
            }
        }
    }

    public static BufferedConnectionContext getConnectionContext() {
        return GameState.inLobby(gameState) ? LOBBY_CONNECTION_CONTEXT : GAME_CONNECTION_CONTEXT;
    }

    public static void resetCutscenes() {
        CURRENT_CUTSCENE = -1;
        MAP_LOADING_STAGE = 1;
        MapRegion.CUTSCENE_MAP_XTEAS = null;
        CutsceneTypeList.CUTSCENE_BUFFER = null;
        CutsceneTypeList.resetCutsceneConfigs();
    }

    public static void method7323() {
        if (Renderers.activeToolkit != null) {
            Resource.mapRegion.resetInteractiveEntities();
            ComponentMinimap.reset();
            StaticMedia.resetChatMediaAndIcons();
            RenderedEntityCache.reset();
            Resource.mapRegion.resetAtmosphere();
            Sun.reset();
            TypeLists.resetCachedData(false);
            StaticMedia.reset();
            GraphicsCache.resetWorldmapCaches(true);
            WorldMapSub1.reset();
            client.initializeTips();
            for (int hintIndex = 0; hintIndex < HINT_TRAILS.length; hintIndex++) {
                HintTrail hint = HINT_TRAILS[hintIndex];
            }
            for (int playerIndex = 0; playerIndex < 2048; playerIndex++) {
                PlayerEntity player = PlayerUpdate.players[playerIndex];
                if (player != null) {
                    for (int index = 0; index < player.models.length; index++) {
                        player.models[index] = null;
                    }
                }
            }
            for (int npcIndex = 0; npcIndex < npcCount; npcIndex++) {
                NpcEntity npc = (NpcEntity) NPC_ARRAY[npcIndex].value;
                if (npc != null) {
                    for (int index = 0; index < npc.models.length; index++) {
                        npc.models[index] = null;
                    }
                }
            }
            ACTIVE_GFX.clear();
            Renderers.activeToolkit.cleanupAndShutdown();
            Renderers.activeToolkit = null;
        }
    }

    public static void method11501() {
        ParticleProducer.cycle(Renderers.activeToolkit, cycles);
        if (BASE_WINDOW_ID != -1) {
            Component.update(BASE_WINDOW_ID);
        }

        for (int index = 0; index < anInt7407; index++) {
            PREV_COMPONENT_ON_TOP[index] = IS_COMPONENT_ON_TOP[index];
            IS_COMPONENT_ON_TOP[index] = false;
        }

        anInt7408 = cycles;
        if (BASE_WINDOW_ID != -1) {
            anInt7407 = 0;
            StaticMethods.method7039();
        }

        Renderers.activeToolkit.resetClipping();
        MiniMenu.method15411(Renderers.activeToolkit);
        int cursorId = MiniMenu.getMinimenuCursor();
        if (cursorId == -1) {
            cursorId = defaultMenuCursor;
        }

        if (cursorId == -1) {
            cursorId = CursorManager.defaultCursorId;
        }

        CursorTypeList.setCustomCursor(cursorId);
        animatingStep = 0;
    }

    public static void method2827() {
        Component.redrawComponent(draggingComponent);
        ++InterfaceRenderer.anInt8308;
        if (isDraggingComponent && isComponentSelected) {
            int i_1 = MouseRecord.recorder.getMouseX();
            int i_2 = MouseRecord.recorder.getMouseY();
            i_1 -= mouseX;
            i_2 -= mouseY;
            if (i_1 < selectedComponentX) {
                i_1 = selectedComponentX;
            }
            if (i_1 + draggingComponent.width > selectedComponentWidth + selectedComponentX) {
                i_1 = selectedComponentWidth + selectedComponentX - draggingComponent.width;
            }
            if (i_2 < selectedComponentY) {
                i_2 = selectedComponentY;
            }
            if (i_2 + draggingComponent.height > selectedComponentY + selectedComponentHeight) {
                i_2 = selectedComponentHeight + selectedComponentY - draggingComponent.height;
            }
            int i_3;
            int i_4;
            if (selectedComponent == client.frameComponent) {
                i_3 = i_1;
                i_4 = i_2;
            } else {
                i_3 = i_1 - selectedComponentX + selectedComponent.scrollX;
                i_4 = i_2 - selectedComponentY + selectedComponent.scrollY;
            }
            HookRequest hookrequest5;
            if (!MouseRecord.recorder.isMouseButtonPressed()) {
                if (aBool7364) {
                    InterfaceComponentRenderer.method1170();
                    if (draggingComponent.anObjectArray1394 != null) {
                        hookrequest5 = new HookRequest();
                        hookrequest5.source = draggingComponent;
                        hookrequest5.mouseX = i_3;
                        hookrequest5.mouseY = i_4;
                        hookrequest5.aClass1188057 = hoveredComponent;
                        hookrequest5.params = draggingComponent.anObjectArray1394;
                        CS2Executor.executeHookInner(hookrequest5);
                    }
                    if (hoveredComponent != null && method11634(draggingComponent) != null) {
                        PacketEncoder.method2076(draggingComponent, hoveredComponent);
                    }
                } else if ((anInt7339 == 1 || MiniMenu.method12562()) && MiniMenu.entryCount > 2) {
                    StaticMethods.method4164(mouseX + dragStartX, dragStartY + mouseY);
                } else if (MiniMenu.hasEntries()) {
                    StaticMethods.method4164(dragStartX + mouseX, dragStartY + mouseY);
                }
                draggingComponent = null;
                selectedComponent = null;
            } else {
                if (InterfaceRenderer.anInt8308 > draggingComponent.anInt1381) {
                    int i_7 = i_1 - dragStartX;
                    int i_6 = i_2 - dragStartY;
                    if (i_7 > draggingComponent.anInt1380 || i_7 < -draggingComponent.anInt1380 || i_6 > draggingComponent.anInt1380 || i_6 < -draggingComponent.anInt1380) {
                        aBool7364 = true;
                    }
                }
                if (draggingComponent.anObjectArray1451 != null && aBool7364) {
                    hookrequest5 = new HookRequest();
                    hookrequest5.source = draggingComponent;
                    hookrequest5.mouseX = i_3;
                    hookrequest5.mouseY = i_4;
                    hookrequest5.params = draggingComponent.anObjectArray1451;
                    CS2Executor.executeHookInner(hookrequest5);
                }
            }
        } else if (InterfaceRenderer.anInt8308 > 1) {
            draggingComponent = null;
            selectedComponent = null;
        }
    }

    public static void loadAssets(int toolkitId, String leftTopScreenboxText, boolean verifySettingsPreUpdate) {
        LoadingStage.killLoginscreenThread();
        client.method7323();
        buildCanvasComponents();
        Renderers.prepareForRendering(toolkitId, leftTopScreenboxText, verifySettingsPreUpdate);
        StaticMethods.initializedFontcombos.resetFontMap();
        StaticMethods.initializedFontcombos.initializeFontMap(FONT_PROVIDER);
        FontManager.initialize();
        StaticMedia.loadSprites(Renderers.activeToolkit, Resource.SPRITES);
        StaticMedia.resetChatMediaAndIcons();
        StaticMedia.initializeChatIcons(StaticMedia.nameIconSprites);
        GameShell.setComponentsOnTop();
        GraphicsCache.resetGraphicCaches();
        if (gameState == GameState.UNK_5) {
            setGameState(GameState.UNK_11);
        } else if (gameState == GameState.UNK_0) {
            setGameState(GameState.UNK_2);
        } else if (gameState == GameState.UNK_7) {
            setGameState(GameState.UNK_6);
        } else if (gameState == GameState.LOGGED_INGAME) {
            setGameState(GameState.LOADING_PLEASE_WAIT);
        } else if (gameState != GameState.UNK_15 && gameState != GameState.UNK_1) {
            if (gameState == GameState.IN_ACCOUNT_CREATION) {
                setGameState(GameState.UNK_9);
            }
        } else {
            client.initializeTips();
        }
    }

    public static void loadAssets(int toolkitId, boolean verifySettingsPreUpdate) {
        client.loadAssets(toolkitId, LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE), verifySettingsPreUpdate);
    }

    public static void method2601(long long_0) {
        if (Resource.mapRegion.getScene() != null) {
            if (Camera.cameraCalculationType != 2 && Camera.cameraCalculationType != 1) {
                if (Camera.cameraCalculationType == 4) {
                    Camera.method4974(long_0);
                }
            } else {
                Camera.method6286(long_0);
            }
        }
        ParticleProducer.cycle(Renderers.activeToolkit, cycles);
        if (BASE_WINDOW_ID != -1) {
            Component.update(BASE_WINDOW_ID);
        }
        for (int index = 0; index < anInt7407; index++) {
            PREV_COMPONENT_ON_TOP[index] = IS_COMPONENT_ON_TOP[index];
            IS_COMPONENT_ON_TOP[index] = false;
        }
        anInt7408 = cycles;
        MiniMenu.method6451(-1, -1);
        if (BASE_WINDOW_ID != -1) {
            anInt7407 = 0;
            StaticMethods.method7039();
        }
        Renderers.activeToolkit.resetClipping();
        MiniMenu.method15411(Renderers.activeToolkit);
        int cursor = MiniMenu.getMinimenuCursor();
        if (cursor == -1) {
            cursor = defaultMenuCursor;
        }
        if (cursor == -1) {
            cursor = CursorManager.defaultCursorId;
        }
        CursorTypeList.setCustomCursor(cursor);
        int size = PlayerUpdate.MY_PLAYER.getSize() << 8;
        Vector3 absolute = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
        SoundEffectPlayer.method14675(PlayerUpdate.MY_PLAYER.plane, size + (int) absolute.x, size + (int) absolute.z, animatingStep);
        animatingStep = 0;
    }

    public static void method14562() {
        client.resetCutscenes();
        Resource.mapRegion.resetBasePosition();

        for (int index = 0; index < PlayerUpdate.HINT_ARROWS.length; index++) {
            PlayerUpdate.HINT_ARROWS[index] = null;
        }

        for (int index = 0; index < HINT_TRAILS.length; index++) {
            HINT_TRAILS[index] = null;
        }

        PlayerUpdate.reset();

        for (int index = 0; index < 2048; index++) {
            PlayerUpdate.players[index] = null;
        }

        NPC_UPDATE_INDEX = 0;
        NPC_MAP.clear();
        npcCount = 0;
        GROUND_ITEMS.clear();
        Camera.smoothReset();
        varVerifyId = 0;
        VarProviders.PLAYER_VAR_PROVIDER.reset();
        client.GUEST_CLAN_SETTINGS = null;
        client.CLAN_SETTINGS = null;
        client.LISTENED_CLAN_CHANNEL = null;
        client.CLAN_CHANNEL = null;
        PingRequester.CURRENT_REQUEST = null;
        PingRequester.NEXT_PING_UPDATE_TO_SERVER = 0L;
        MiniMenu.close();
        MiniMenu.resetEntries();
    }
}
