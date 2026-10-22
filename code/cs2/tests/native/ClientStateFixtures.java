import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.AbstractMouseManager;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.CS2Dispatch;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.Preference;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ClientStateFixtures {
    static final long SEED = 0x727c5228L;
    static final String[] PREFS = {
        "soundEffectVolume", "ambientSoundVolume", "voiceOverVolume", "musicVolume", "soundLevel", "monoStereo", "brightness", "water",
        "customCursor", "removeRoofs", "removeRoofsOptionOverride", "sceneryShadows", "skyboxes", "particles", "antiAliasing",
        "antiAliasingPreview", "bloom", "characterShadows", "cpu", "fog", "graphics", "goundBlending", "groundDecoration",
        "idleAnimations", "lightDetail", "mapSize", "maxScreenSize", "textures", "defaultToolkit", "flickeringEffects",
        "gametipStartStagePreference", "toolkit"
    };
    static final String[][] STATICS = {
        {"Client", "cycles", "i"}, {"Client", "AFFILIATE", "i"}, {"Client", "billingFlag", "b"}, {"Client", "base64Url", "k"},
        {"Client", "machineUid", "l"}, {"Client", "isForcedSafemode", "b"}, {"Client", "safemodeForcedDuringStartup", "b"},
        {"Client", "customStringParam", "s"}, {"Client", "hasJustCreatedAccount", "b"}, {"Client", "isApplet", "b"},
        {"Client", "mapOwnerIdentifier", "s"}, {"Client", "IS_QUICKCHAT_ONLY", "b"}, {"Client", "CURRENT_CURSOR", "i"},
        {"Client", "interfaceSelectionActive", "b"}, {"Client", "useOnType", "s"}, {"Client", "useOnTarget", "s"},
        {"Client", "USERDETAIL_QUICKCHAT", "b"}, {"Client", "VERIFIED_EMAIL_ADDRESS", "b"}, {"GameShell", "appletHasFocus", "b"},
        {"LoginManager", "loginQueuePosition", "i"}, {"LoginManager", "loginResponseDetail", "i"},
        {"LoginManager", "loginResponseExtraInfo", "i"}, {"StaticMethods", "lobbyLoginResponse", "i"},
        {"StaticMethods", "worldLoginResponse", "i"}, {"StaticMethods", "loginHandshakeCode", "i"}, {"MiniMenu", "entryCount", "i"},
        {"MiniMenu", "coloredEntryCount", "i"}, {"CursorManager", "defaultAttackCursor", "i"}, {"CursorManager", "defaultCursorId", "i"},
        {"CS2Executor", "instructionCount", "i"}, {"Client", "preferencesSavedOnServer", "b"}, {"Client", "needsResize", "b"},
        {"ParticleSystemManager", "detailsSetting", "i"}, {"AreaSoundPlayer", "AREA_SOUND_CACHE_SIZE", "i"},
        {"AreaSoundPlayer", "currentMusicArchive", "i"}, {"AreaSoundPlayer", "titlescreenMusicArchive", "i"},
        {"AreaSoundPlayer", "SOUND_LEVEL", "i"}, {"SoundResourceManager", "songLoadState", "i"},
        {"SoundEffectPlayer", "soundJinglePlaying", "b"}
    };
    static final Map<String, Class<?>> CLASSES = new HashMap<>();
    static final int[] POOL = {0, 1, 2, 3, -1, 4, 5, 6, 7, 8, 9, 10, 50, 100, 255, 256, 257, 300, 700, 701, 1000, 65535, Integer.MAX_VALUE, Integer.MIN_VALUE, -2, 12345};
    static final int[] SMALL = {0, 1, 2, 3, 4, -1, 5, 255, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final int[] MEMORY = {-1, 100, 244, 245, 499, 500, 1000};
    static final String[] TEXTS = {"alpha", "Alpha", "ALPHA", "", "beta", "x y", "-1"};
    static final Object[][] OPS = {
        {"SOUND_SYNTH", 3, 0}, {"SOUND_SONG", 1, 0}, {"SOUND_JINGLE", 2, 0}, {"SOUND_SYNTH_VOLUME", 4, 0}, {"SOUND_SONG_VOLUME", 3, 0},
        {"SOUND_JINGLE_VOLUME", 3, 0}, {"SOUND_VORBIS_VOLUME", 4, 0}, {"SOUND_SPEECH_VOLUME", 4, 0}, {"SOUND_SYNTH_RATE", 5, 0},
        {"SOUND_VORBIS_RATE", 5, 0}, {"SOUND_VORBIS_PRELOAD", 2, 0}, {"SOUND_VORBIS_STOP", 1, 0}, {"SOUND_VORBIS_PLAY", 1, 0},
        {"CLIENTCLOCK", 0, 0}, {"MAP_QUICKCHAT", 0, 0}, {"AFFILIATE", 0, 0}, {"APPLET_HASFOCUS", 0, 0}, {"FROMBILLING", 0, 0},
        {"GET_CURRENTCURSOR", 0, 0}, {"GET_MINIMENU_LENGTH", 0, 0}, {"GET_MINIMENU_TARGET", 0, 0}, {"GET_MOUSEX", 0, 0},
        {"GET_MOUSEY", 0, 0}, {"GET_MOUSEBUTTONS", 0, 0}, {"KEYHELD_ALT", 0, 0}, {"KEYHELD_CTRL", 0, 0}, {"KEYHELD_SHIFT", 0, 0},
        {"MAP_ISOWNER", 0, 1}, {"HAS_SIGNONKEY", 0, 0}, {"USERFLOWFLAGS", 0, 0}, {"CREATE_UNDER13", 0, 0}, {"CREATE_SETUNDER13", 0, 0},
        {"CREATE_GET_EMAIL", 0, 0}, {"LOGIN_HOPTIME", 0, 0}, {"LOGIN_QUEUE_POSITION", 0, 0}, {"LOGIN_REPLY_WORLD_ALT", 0, 0},
        {"LOBBY_ENTERGAMEREPLY", 0, 0}, {"LOBBY_ENTERLOBBYREPLY", 0, 0}, {"LOGIN_DISALLOWTRIGGER", 0, 0}, {"LOGIN_DISALLOWRESULT", 0, 0},
        {"IS_APPLET", 0, 0}, {"NOOP", 0, 0}, {"OPCOUNT", 0, 0}, {"USERDETAIL_QUICKCHAT", 0, 0}, {"NOTIFY_ACCOUNTCREATED", 0, 0},
        {"SETDEFAULTCURSORS", 2, 0}, {"SETHARDCODEDOPCURSORS", 2, 0}, {"AUTOSETUP_GETLEVEL", 0, 0},
        {"DETAILGET_SAFEMODE", 0, 0}, {"DETAILGET_CHOSESAFEMODE", 0, 0}, {"DETAILGET_CANCHOOSESAFEMODE", 0, 0},
        {"DETAIL_SOUNDVOL", 1, 0}, {"DETAIL_BGSOUNDVOL", 1, 0}, {"DETAIL_SPEECHVOL", 1, 0}, {"DETAIL_MUSICVOL", 1, 0}, {"DETAIL_LOGINVOL", 1, 0},
        {"DETAIL_IDLEANIMS", 1, 0}, {"DETAIL_IDLEANIMS_MANY", 1, 0}, {"DETAIL_LOADINGSCREENTYPE", 1, 0}, {"DETAIL_SKYDETAIL", 1, 0},
        {"DETAIL_REMOVEROOFS_OPTION", 1, 0}, {"DETAIL_REMOVEROOFS_OPTION_OVERRIDE", 1, 0}, {"DETAIL_WATERDETAIL_HIGH", 1, 0},
        {"DETAIL_TOOLKIT", 1, 0}, {"DETAIL_TOOLKIT_DEFAULT", 2, 0}, {"DETAIL_BLOOM", 1, 0}, {"DETAIL_PARTICLES", 1, 0},
        {"DETAIL_BRIGHTNESS", 1, 0}, {"DETAIL_CUSTOMCURSORS", 1, 0}, {"DETAIL_SPOTSHADOWS_ON", 1, 0}, {"DETAIL_TEXTURING", 1, 0},
        {"DETAIL_MAXSCREENSIZE", 1, 0}, {"DETAIL_HARDSHADOWS", 1, 0}, {"DETAIL_BUILDAREA", 1, 0}, {"DETAIL_GROUNDBLENDING", 1, 0},
        {"DETAIL_CPUUSAGE", 1, 0}, {"DETAIL_FLICKERING_ON", 1, 0}, {"DETAIL_FOG_ON", 1, 0}, {"DETAIL_GROUNDDECOR_ON", 1, 0},
        {"DETAIL_ANTIALIASING_DEFAULT", 1, 0}, {"DETAIL_ANTIALIASING", 1, 0}, {"DETAIL_STEREO", 1, 0}, {"DETAIL_LIGHTDETAIL_HIGH", 1, 0},
        {"DETAILGET_BRIGHTNESS", 0, 0}, {"DETAILGET_REMOVEROOFS_OPTION", 0, 0}, {"DETAILGET_GROUNDDECOR_ON", 0, 0},
        {"DETAILGET_IDLEANIMS_MANY", 0, 0}, {"DETAILGET_FLICKERING_ON", 0, 0}, {"DETAILGET_SPOTSHADOWS_ON", 0, 0},
        {"DETAILGET_HARDSHADOWS", 0, 0}, {"DETAILGET_LIGHTDETAIL_HIGH", 0, 0}, {"DETAILGET_WATERDETAIL_HIGH", 0, 0},
        {"DETAILGET_FOG_ON", 0, 0}, {"DETAILGET_ANTIALIASING", 0, 0}, {"DETAILGET_STEREO", 0, 0}, {"DETAILGET_SOUNDVOL", 0, 0},
        {"DETAILGET_MUSICVOL", 0, 0}, {"DETAILGET_BGSOUNDVOL", 0, 0}, {"DETAILGET_PARTICLES", 0, 0},
        {"DETAILGET_ANTIALIASING_DEFAULT", 0, 0}, {"DETAILGET_BUILDAREA", 0, 0}, {"DETAILGET_BLOOM", 0, 0},
        {"DETAILGET_CUSTOMCURSORS", 0, 0}, {"DETAILGET_IDLEANIMS", 0, 0}, {"DETAILGET_GROUNDBLENDING", 0, 0},
        {"DETAILGET_TOOLKIT", 0, 0}, {"DETAILGET_TOOLKIT_DEFAULT", 0, 0}, {"DETAILGET_CPUUSAGE", 0, 0}, {"DETAILGET_TEXTURING", 0, 0},
        {"DETAILGET_MAXSCREENSIZE", 0, 0}, {"DETAILGET_SPEECHVOL", 0, 0}, {"DETAILGET_LOGINVOL", 0, 0},
        {"DETAILGET_LOADINGSCREENTYPE", 0, 0}, {"DETAILGET_DEFAULTTOOLKIT", 0, 0}, {"DETAILGET_SKYDETAIL", 0, 0},
        {"DETAILCANMOD_GROUNDDECOR", 0, 0}, {"DETAILCANMOD_CHARSHADOWS", 0, 0}, {"DETAILCANMOD_SPOTSHADOWS", 0, 0},
        {"DETAILCANMOD_WATERDETAIL", 0, 0}, {"DETAILCANMOD_ANTIALIASING", 0, 0}, {"DETAILCANMOD_PARTICLES", 0, 0},
        {"DETAILCANMOD_BUILDAREA", 0, 0}, {"DETAILCANMOD_BLOOM", 0, 0}, {"DETAILCANMOD_GROUNDBLENDING", 0, 0},
        {"DETAILCANMOD_TEXTURING", 0, 0}, {"DETAILCANMOD_MAXSCREENSIZE", 0, 0}, {"DETAILCANMOD_FOG", 0, 0},
        {"DETAILCANMOD_DEFAULTTOOLKIT", 0, 0}, {"DETAILCANMOD_TOOLKIT_DEFAULT", 0, 0}, {"DETAILCANMOD_SKYDETAIL", 0, 0},
        {"DETAILCANSET_GROUNDDECOR", 1, 0}, {"DETAILCANSET_CHARSHADOWS", 1, 0}, {"DETAILCANSET_SPOTSHADOWS", 1, 0},
        {"DETAILCANSET_WATERDETAIL", 1, 0}, {"DETAILCANSET_ANTIALIASING", 1, 0}, {"DETAILCANSET_PARTICLES", 1, 0},
        {"DETAILCANSET_BUILDAREA", 1, 0}, {"DETAILCANSET_BLOOM", 1, 0}, {"DETAILCANSET_GROUNDBLENDING", 1, 0},
        {"DETAILCANSET_TEXTURING", 1, 0}, {"DETAILCANSET_MAXSCREENSIZE", 1, 0}, {"DETAILCANSET_FOG", 1, 0},
        {"DETAILCANSET_DEFAULTTOOLKIT", 1, 0}, {"DETAILCANSET_TOOLKIT_DEFAULT", 1, 0}, {"DETAILCANSET_SKYDETAIL", 1, 0}
    };
    static sun.misc.Unsafe unsafe;

    static final class Mouse extends AbstractMouseManager {
        int x, y;
        boolean l, r, m;
        public boolean leftButtonDown() { return l; }
        public boolean rightButtonDown() { return r; }
        public boolean middleButtonDown() { return m; }
        public int getMouseX() { return x; }
        public int getMouseY() { return y; }
        public MouseRecord nextSubmission() { return null; }
        public void reset() { }
        public void syncMouseState() { }
    }

    static final class Keys extends KeyRecorder {
        boolean alt, ctrl, shift;
        public void prepareAwaitingKeys() { }
        public boolean held(int key) { return key == 86 ? alt : key == 82 ? ctrl : key == 81 && shift; }
        public KeyRecord popHead() { return null; }
        public void addKeyListeners() { }
    }

    static Field field(Class<?> type, String name) throws Exception {
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    static Object[] base(Field f) {
        return new Object[] {unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f)};
    }

    static void setNumber(Class<?> type, String name, long value) throws Exception {
        var f = field(type, name);
        var b = base(f);
        var t = f.getType();
        if (t == int.class) unsafe.putInt(b[0], (long) b[1], (int) value);
        else if (t == boolean.class) unsafe.putBoolean(b[0], (long) b[1], value != 0);
        else if (t == long.class) unsafe.putLong(b[0], (long) b[1], value);
        else if (t == short.class) unsafe.putShort(b[0], (long) b[1], (short) value);
        else if (t == byte.class) unsafe.putByte(b[0], (long) b[1], (byte) value);
        else throw new IllegalStateException(f.toString());
    }

    static long getNumber(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        var b = base(f);
        var t = f.getType();
        if (t == int.class) return unsafe.getInt(b[0], (long) b[1]);
        if (t == boolean.class) return unsafe.getBoolean(b[0], (long) b[1]) ? 1 : 0;
        if (t == long.class) return unsafe.getLong(b[0], (long) b[1]);
        if (t == short.class) return unsafe.getShort(b[0], (long) b[1]);
        if (t == byte.class) return unsafe.getByte(b[0], (long) b[1]);
        throw new IllegalStateException(f.toString());
    }

    static void setObject(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        var b = base(f);
        unsafe.putObject(b[0], (long) b[1], value);
    }

    static Object getObject(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        var b = base(f);
        return unsafe.getObject(b[0], (long) b[1]);
    }

    static String stack(int[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    static String strings(Object[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append('|');
            sb.append(values[i] == null ? "~null" : "s:" + values[i]);
        }
        return sb.toString();
    }

    static String text(Object value) {
        return value == null ? "~null" : "s:" + value;
    }

    static final class Profile {
        int id, game, memory;
        int[] prefs = new int[PREFS.length];
        long[] statics = new long[STATICS.length];
        String[] texts = new String[STATICS.length];
        int mouse;
        int[] mouseValues = new int[5];
        int keys;
        int rendering;
        int[] keyValues = new int[3];
    }

    static void apply(Profile p) throws Exception {
        setNumber(GameShell.class, "maxMemory", p.memory);
        var prefs = new GamePreferences(Game.values()[p.game]);
        for (int i = 0; i < PREFS.length; i++) {
            if (PREFS[i].equals("toolkit")) continue;
            var pref = (Preference) field(GamePreferences.class, PREFS[i]).get(prefs);
            pref.value = p.prefs[i];
        }
        PreferenceManager.preferences = prefs;
        for (int i = 0; i < STATICS.length; i++) {
            var spec = STATICS[i];
            var type = CLASSES.get(spec[0]);
            switch (spec[2]) {
                case "s" -> setObject(type, spec[1], p.texts[i]);
                case "k" -> setObject(type, spec[1], p.texts[i] == null ? null : new byte[] {1});
                default -> setNumber(type, spec[1], p.statics[i]);
            }
        }
        Client.gameState = p.rendering == 0 ? com.jagex.game.runetek5.client.GameState.UNK_4 : com.jagex.game.runetek5.client.GameState.LOGGED_INGAME;
        if (p.mouse == 0) {
            MouseRecord.recorder = null;
        } else {
            var m = new Mouse();
            m.x = p.mouseValues[0];
            m.y = p.mouseValues[1];
            m.l = p.mouseValues[2] != 0;
            m.r = p.mouseValues[3] != 0;
            m.m = p.mouseValues[4] != 0;
            MouseRecord.recorder = m;
        }
        if (p.keys == 0) {
            KeyRecorder.keyRecorder = null;
        } else {
            var k = new Keys();
            k.alt = p.keyValues[0] != 0;
            k.ctrl = p.keyValues[1] != 0;
            k.shift = p.keyValues[2] != 0;
            KeyRecorder.keyRecorder = k;
        }
    }

    static String dump(Profile p) throws Exception {
        var prefs = PreferenceManager.preferences;
        var sb = new StringBuilder();
        for (int i = 0; i < PREFS.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(((Preference) field(GamePreferences.class, PREFS[i]).get(prefs)).getValue());
        }
        for (int i = 0; i < STATICS.length; i++) {
            var spec = STATICS[i];
            var type = CLASSES.get(spec[0]);
            sb.append(',');
            switch (spec[2]) {
                case "s" -> sb.append(text(getObject(type, spec[1])).replace(',', ';'));
                case "k" -> sb.append(getObject(type, spec[1]) == null ? 0 : 1);
                default -> sb.append(getNumber(type, spec[1]));
            }
        }
        return sb.toString();
    }

    static int pick(Random random, int[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        CLASSES.put("Client", Client.class);
        CLASSES.put("GameShell", GameShell.class);
        CLASSES.put("LoginManager", LoginManager.class);
        CLASSES.put("StaticMethods", StaticMethods.class);
        CLASSES.put("MiniMenu", MiniMenu.class);
        CLASSES.put("CursorManager", CursorManager.class);
        CLASSES.put("CS2Executor", CS2Executor.class);
        CLASSES.put("ParticleSystemManager", ParticleSystemManager.class);
        CLASSES.put("AreaSoundPlayer", com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer.class);
        CLASSES.put("SoundResourceManager", com.jagex.game.runetek5.sound.SoundResourceManager.class);
        CLASSES.put("SoundEffectPlayer", com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer.class);
        var out = new StringBuilder();
        var random = new Random(SEED);
        int profileCount = 24;
        var profiles = new ArrayList<Profile>();
        var games = Game.values();
        for (int s = 0; s < profileCount; s++) {
            var p = new Profile();
            p.id = s;
            p.game = s % 2 == 0 ? games.length - 1 : random.nextInt(games.length - 1);
            p.memory = MEMORY[(s / 2) % MEMORY.length];
            setNumber(GameShell.class, "maxMemory", p.memory);
            var base = new GamePreferences(games[p.game]);
            for (int i = 0; i < PREFS.length; i++) {
                int value = ((Preference) field(GamePreferences.class, PREFS[i]).get(base)).getValue();
                if (!PREFS[i].equals("toolkit") && s > 1) value = random.nextInt(4) == 0 ? pick(random, POOL) : random.nextInt(4);
                p.prefs[i] = value;
            }
            p.prefs[Arrays.asList(PREFS).indexOf("toolkit")] = 3;
            for (int i = 0; i < STATICS.length; i++) {
                var spec = STATICS[i];
                switch (spec[2]) {
                    case "s" -> p.texts[i] = random.nextInt(4) == 0 ? null : pick(random, new int[] {0}) == 0 ? TEXTS[random.nextInt(TEXTS.length)] : null;
                    case "k" -> p.texts[i] = random.nextBoolean() ? "k" : null;
                    case "b" -> p.statics[i] = random.nextBoolean() ? 1 : 0;
                    case "l" -> p.statics[i] = random.nextLong();
                    default -> p.statics[i] = random.nextInt(3) == 0 ? pick(random, POOL) : random.nextInt(30);
                }
            }
            for (int i = 0; i < STATICS.length; i++) if (STATICS[i][1].equals("detailsSetting")) p.statics[i] = random.nextInt(3);
            p.mouse = s % 5 == 4 ? 0 : 1;
            for (int i = 0; i < 5; i++) p.mouseValues[i] = i < 2 ? pick(random, POOL) : random.nextInt(2);
            p.keys = s % 6 == 5 ? 0 : 1;
            for (int i = 0; i < 3; i++) p.keyValues[i] = random.nextInt(2);
            p.rendering = s % 3 == 0 ? 1 : 0;
            profiles.add(p);
            out.append("P\t").append(s).append('\t').append(p.game == games.length - 1 ? 1 : 0).append('\t').append(p.memory).append('\t');
            for (int i = 0; i < PREFS.length; i++) out.append(i > 0 ? "," : "").append(p.prefs[i]);
            out.append('\t');
            for (int i = 0; i < STATICS.length; i++) {
                if (i > 0) out.append(',');
                var spec = STATICS[i];
                switch (spec[2]) {
                    case "s" -> out.append(text(p.texts[i]).replace(',', ';'));
                    case "k" -> out.append(p.texts[i] == null ? 0 : 1);
                    default -> out.append(p.statics[i]);
                }
            }
            out.append('\t').append(p.mouse == 0 ? "-" : Arrays.toString(p.mouseValues).replaceAll("[\\[\\] ]", ""));
            out.append('\t').append(p.keys == 0 ? "-" : Arrays.toString(p.keyValues).replaceAll("[\\[\\] ]", "")).append('\t').append(p.rendering).append('\n');
        }
        var cases = new ArrayList<int[]>();
        for (int o = 0; o < OPS.length; o++) {
            int n = (String) OPS[o][0] == null ? 0 : 1;
            int count = (int) OPS[o][1] == 0 && (int) OPS[o][2] == 0 ? profileCount : 90;
            for (int c = 0; c < count; c++) cases.add(new int[] {o, c % profileCount, random.nextInt()});
        }
        Collections.shuffle(cases, random);
        var stats = new TreeMap<String, int[]>();
        int total = 0;
        for (var item : cases) {
            var spec = OPS[item[0]];
            var op = ClientScriptOpCode.valueOf((String) spec[0]);
            var profile = profiles.get(item[1]);
            apply(profile);
            var local = new Random(item[2]);
            int ints = (int) spec[1], strs = (int) spec[2];
            var name = (String) spec[0];
            boolean small = name.startsWith("DETAILCANSET") || name.startsWith("DETAIL_") && !name.contains("SOUNDVOL");
            var e = new CS2Executor();
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            var argText = new StringBuilder();
            for (int i = 0; i < ints; i++) {
                int v = small && local.nextInt(5) != 0 ? local.nextInt(5) - (local.nextInt(8) == 0 ? 1 : 0) : pick(local, POOL);
                e.intStack[e.intStackPtr++] = v;
                if (i > 0) argText.append(',');
                argText.append(v);
            }
            var strText = new StringBuilder();
            for (int i = 0; i < strs; i++) {
                String v = local.nextInt(6) == 0 ? null : TEXTS[local.nextInt(TEXTS.length)];
                e.stringStack[e.stringStackPtr++] = v;
                if (i > 0) strText.append('|');
                strText.append(text(v));
            }
            String result = "ok";
            try {
                CS2Dispatch.execute(op, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            out.append("C\t").append(name).append('\t').append(profile.id).append('\t').append(ints == 0 ? "-" : argText).append('\t')
                .append(strs == 0 ? "-" : strText).append('\t').append(result).append('\t').append(stack(e.intStack, e.intStackPtr)).append('\t')
                .append(strings(e.stringStack, e.stringStackPtr)).append('\t').append(dump(profile)).append('\n');
            stats.computeIfAbsent(name, k -> new int[2])[result.equals("ok") ? 0 : 1]++;
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("client-state.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var summary = new StringBuilder();
        for (var entry : stats.entrySet()) summary.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("client-state.stats.tsv"), summary.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
