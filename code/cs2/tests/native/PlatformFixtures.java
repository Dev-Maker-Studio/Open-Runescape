import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuEntry;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.config.mec.MECTypeList;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.param.ParamTypeList;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.js5.library.NativeLibraryLoader;
import com.jagex.game.runetek5.network.HostNameIdentifier;
import com.jagex.game.runetek5.network.packet.PacketDecoder;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.preference.impl.ScreenSizePreference;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.utils.DirectoryPicker;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class PlatformFixtures {
    static sun.misc.Unsafe unsafe;
    static final String[] TEXTS = {"", "a", "Attack", "Use", "Walk here", "café ü", "x y", "<col=ff0000>Goblin", "Cancel", "/home/p/.darkan/prefs", "tab\there"};
    static final int[] CLICKS = {0, 1, 2, 3, 6, 8, 9, 13, 15, 16, 17, 18, 22, 25, 30, 44, 53, 54, 57, 58, 1001, 1003, 1004, 1007, 2000, 99, -1};
    static final int[] COMPONENT_OR_OTHER = {0, 1, 7, 14, 15, 16, 23, 25, 30, 44, 49, 53, 54, 57, 58, 99, 1007, 2000, -1};
    static final int[] VALUES = {0, 1, 2, 3, 5, 7, 255, 65535, -1, -2, 100, Integer.MIN_VALUE, Integer.MAX_VALUE};

    static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    static void set(Class<?> type, String name, Object target, Object value) throws Exception {
        field(type, name).set(target, value);
    }

    static String hex(String s) {
        if (s == null) return "~";
        var b = s.getBytes(StandardCharsets.UTF_8);
        var out = new StringBuilder("x");
        for (byte v : b) out.append(String.format("%02x", v & 0xff));
        return out.toString();
    }

    record Entry(String action, String color, String target, int click) {
        String enc() {
            return hex(action) + ":" + hex(color) + ":" + hex(target) + ":" + click;
        }

        MiniMenuEntry make() {
            var e = new MiniMenuEntry(action, color, 0, click, -1, 0L, 0, 0, false, false, 0L, false);
            e.targetName = target;
            return e;
        }
    }

    static final class Mec {
        int id;
        String name;
        int icon, category, zoom;
        Map<Integer, Object> params = new TreeMap<>();
    }

    static final class State {
        Integer language;
        boolean fss = true, frame, fwm;
        int fixed;
        boolean hid;
        String host;
        int clipKind;
        String clip;
        int gs;
        String pref;
        int dpKind;
        String dpPath;
        boolean jt;
        boolean open;
        Entry act, sec;
        Mec mec;
        int ptId = -1;
        boolean ptString;
        int ptDefault;
        String ptName;

        String enc() {
            var sb = new StringBuilder();
            sb.append("lang=").append(language == null ? "~" : language);
            sb.append("|fss=").append(fss ? 1 : 0).append("|ff=").append(frame ? 1 : 0).append("|fwm=").append(fwm ? 1 : 0);
            sb.append("|fixed=").append(fixed).append("|hid=").append(hid ? 1 : 0).append("|host=").append(hex(host));
            sb.append("|clip=").append(hex(clipKind == 3 ? clip : null)).append("|gs=").append(gs).append("|pref=").append(hex(pref));
            sb.append("|dp=").append(dpEnc(dpKind, dpPath)).append("|jt=").append(jt ? 1 : 0).append("|open=").append(open ? 1 : 0);
            sb.append("|act=").append(act == null ? "~" : act.enc()).append("|sec=").append(sec == null ? "~" : sec.enc());
            if (mec != null) {
                var p = new StringBuilder();
                for (var en : mec.params.entrySet()) {
                    if (p.length() > 0) p.append(",");
                    p.append(en.getKey()).append(en.getValue() instanceof String s ? "s" + hex(s) : "i" + en.getValue());
                }
                sb.append("|mec=").append(mec.id).append(":").append(hex(mec.name)).append(":").append(mec.icon).append(":").append(mec.category).append(":").append(mec.zoom).append(":").append(p.length() == 0 ? "-" : p);
            } else {
                sb.append("|mec=~");
            }
            sb.append("|pt=").append(ptId).append(":").append(ptString ? 1 : 0).append(":").append(ptDefault).append(":").append(hex(ptName));
            return sb.toString();
        }
    }

    static String dpEnc(int kind, String path) {
        if (kind == 0) return "~";
        return (kind == 1 ? "0" : "1") + ":" + hex(kind == 3 ? path : null);
    }

    static final class FixedTransferable implements Transferable {
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.imageFlavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    static void apply(State s, CS2Executor e) throws Exception {
        set(Client.class, "CURRENT_LANGUAGE", null, s.language == null ? null : Language.values()[s.language]);
        set(WindowMode.class, "fullScreenSupported", null, s.fss);
        set(WindowMode.class, "fullScreenFrame", null, s.frame ? unsafe.allocateInstance(java.awt.Frame.class) : null);
        set(WindowMode.class, "fullScreenWindowMode", null, s.fwm);
        var screen = unsafe.allocateInstance(ScreenSizePreference.class);
        set(ScreenSizePreference.class, "value", screen, s.fixed);
        var prefs = unsafe.allocateInstance(GamePreferences.class);
        set(GamePreferences.class, "fixedScreen", prefs, screen);
        set(PreferenceManager.class, "preferences", null, prefs);
        if (s.hid) {
            var id = unsafe.allocateInstance(HostNameIdentifier.class);
            set(HostNameIdentifier.class, "host", id, s.host);
            set(PacketDecoder.class, "HOSTNAME_IDENTIFIER", null, id);
        } else {
            set(PacketDecoder.class, "HOSTNAME_IDENTIFIER", null, null);
        }
        Clipboard clipboard = null;
        if (s.clipKind >= 1) {
            clipboard = new Clipboard("fixture");
            if (s.clipKind == 3) clipboard.setContents(new StringSelection(s.clip), null);
            else if (s.clipKind == 2) clipboard.setContents(new FixedTransferable(), null);
        }
        set(Console.class, "clipboard", null, clipboard);
        set(Client.class, "gameState", null, GameState.values()[s.gs]);
        set(PreferenceManager.class, "preferenceFile", null, s.pref == null ? null : new File(s.pref));
        if (s.dpKind == 0) {
            e.filePicker = null;
        } else {
            var picker = (DirectoryPicker) unsafe.allocateInstance(DirectoryPicker.class);
            picker.fileChoserOpen = s.dpKind != 1;
            picker.pickedDirectory = s.dpKind == 3 ? new File(s.dpPath) : null;
            e.filePicker = picker;
        }
        var loader = (NativeLibraryLoader) unsafe.allocateInstance(NativeLibraryLoader.class);
        loader.loadedLibraryExecutor = new Hashtable<>();
        if (s.jt) ((Hashtable) loader.loadedLibraryExecutor).put("jagtheora", Object.class);
        set(Resource.class, "NATIVE_LIBRARY_LOADER", null, loader);
        set(MiniMenu.class, "isOpen", null, s.open);
        set(MiniMenu.class, "activeEntry", null, s.act == null ? null : s.act.make());
        set(MiniMenu.class, "secondaryEntry", null, s.sec == null ? null : s.sec.make());
        var mecList = (MECTypeList) unsafe.allocateInstance(MECTypeList.class);
        mecList.definitionCache = new LRUCache(128);
        for (int id = 0; id < 4; id++) if (s.mec == null || s.mec.id != id) mecList.definitionCache.put(new MECType(), (long) id);
        if (s.mec != null) {
            var t = new MECType();
            t.areaName = s.mec.name;
            t.defaultIconArchive = s.mec.icon;
            t.categoryId = s.mec.category;
            t.baseTextzoom = s.mec.zoom;
            if (!s.mec.params.isEmpty()) {
                t.parameters = new IterableNodeMap<>(8);
                for (var en : s.mec.params.entrySet()) {
                    var node = en.getValue() instanceof String str ? new ObjectNode(str) : new IntNode((Integer) en.getValue());
                    t.parameters.put(node, (long) en.getKey());
                }
            }
            mecList.definitionCache.put(t, (long) s.mec.id);
        }
        set(TypeLists.class, "MEC", null, mecList);
        var params = new ParamTypeList(null);
        for (int id = 0; id < 5; id++) if (s.ptId != id) params.cache.put(new ParamType(), (long) id);
        if (s.ptId != -1) {
            var pt = new ParamType();
            pt.typeChar = s.ptString ? 's' : (char) 0;
            pt.defaultInt = s.ptDefault;
            pt.typeName = s.ptName;
            params.cache.put(pt, (long) s.ptId);
        }
        set(TypeLists.class, "PARAMS", null, params);
    }

    static CS2Executor executor(int[] given) {
        var e = new CS2Executor();
        Arrays.fill(e.intStack, 0x5a5a5a5a);
        e.intStackPtr = 0;
        e.stringStackPtr = 0;
        e.intOpValues = new int[] {0};
        e.instrPtr = 0;
        for (int v : given) e.intStack[e.intStackPtr++] = v;
        return e;
    }

    static void run(ClientScriptOpCode op, CS2Executor e) {
        switch (op) {
            case MAP_LANG, GET_ACTIVE_MINIMENU_ENTRY, GET_SECOND_MINIMENU_ENTRY -> PlayerWorldOps.dispatch(op, e);
            case MEC_TEXT, MEC_GRAPHIC, MEC_TEXTSIZE, MEC_CATEGORY, MEC_PARAM, VIDEOADVERT_ISSUPPORTED, VIDEO_ADVERT_PLAY, VIDEO_ADVERT_FORCE_REMOVE, VIDEO_ADVERT_ALLOW_SKIP, VIDEO_ADVERT_HAS_FINISHED -> WorldMapUserDetailOps.dispatch(op, e);
            default -> QuickChatDisplayOps.dispatch(op, e);
        }
    }

    static String pick(Random r, String[] a) {
        return a[r.nextInt(a.length)];
    }

    static Entry entry(Random r, boolean open) {
        int[] clicks = open ? CLICKS : COMPONENT_OR_OTHER;
        return new Entry(pick(r, TEXTS), pick(r, TEXTS), r.nextInt(3) == 0 ? null : pick(r, TEXTS), clicks[r.nextInt(clicks.length)]);
    }

    static State random(Random r, String name) {
        var s = new State();
        s.language = r.nextInt(6) == 0 ? null : r.nextInt(6);
        s.fss = r.nextBoolean();
        s.frame = r.nextBoolean();
        s.fwm = r.nextBoolean();
        s.fixed = VALUES[r.nextInt(VALUES.length)];
        s.hid = r.nextInt(4) != 0;
        s.host = r.nextInt(3) == 0 ? null : pick(r, TEXTS);
        s.clipKind = r.nextInt(4);
        s.clip = pick(r, TEXTS);
        s.gs = r.nextInt(GameState.values().length);
        s.pref = r.nextInt(3) == 0 ? null : pick(r, TEXTS);
        s.dpKind = r.nextInt(4);
        s.dpPath = "/tmp/p" + pick(r, TEXTS).replace('\t', '_');
        s.jt = r.nextBoolean();
        s.open = r.nextInt(3) == 0;
        s.act = r.nextInt(5) == 0 ? null : entry(r, s.open);
        s.sec = r.nextInt(5) == 0 ? null : entry(r, s.open);
        if (name.startsWith("MEC")) {
            var m = new Mec();
            m.id = r.nextInt(4);
            m.name = r.nextInt(3) == 0 ? null : pick(r, TEXTS);
            m.icon = r.nextInt(3) == 0 ? -1 : VALUES[r.nextInt(VALUES.length)];
            m.category = r.nextInt(3) == 0 ? -1 : VALUES[r.nextInt(VALUES.length)];
            m.zoom = VALUES[r.nextInt(VALUES.length)];
            int n = r.nextInt(4);
            for (int i = 0; i < n; i++) {
                int key = r.nextInt(5);
                m.params.put(key, r.nextBoolean() ? pick(r, TEXTS) : (Object) VALUES[r.nextInt(VALUES.length)]);
            }
            s.mec = r.nextInt(5) == 0 ? null : m;
            if (r.nextInt(6) != 0) {
                s.ptId = r.nextInt(5);
                s.ptString = r.nextBoolean();
                s.ptDefault = VALUES[r.nextInt(VALUES.length)];
                s.ptName = pick(r, TEXTS);
            }
        }
        return s;
    }

    static final String[] NAMES = {
        "MAP_LANG", "GET_ACTIVE_MINIMENU_ENTRY", "GET_SECOND_MINIMENU_ENTRY", "GETWINDOWMODE", "GETDEFAULTWINDOWMODE", "GETHOSTNAME", "GETCLIPBOARD",
        "IS_GAMESCREEN_STATE", "GETPREFERENCEFILE", "GETDIRECTORYPICKERRESULT", "MEC_TEXT", "MEC_GRAPHIC", "MEC_TEXTSIZE", "MEC_CATEGORY", "MEC_PARAM",
        "VIDEOADVERT_ISSUPPORTED", "VIDEO_ADVERT_PLAY", "VIDEO_ADVERT_FORCE_REMOVE", "VIDEO_ADVERT_ALLOW_SKIP", "VIDEO_ADVERT_HAS_FINISHED"
    };

    public static void main(String[] args) throws Exception {
        var uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        uf.setAccessible(true);
        unsafe = (sun.misc.Unsafe) uf.get(null);
        var random = new Random(0x727c5228L ^ 0x7f1a7L);
        var out = new StringBuilder();
        int total = 0;
        for (var name : NAMES) {
            for (int n = 0; n < 160; n++) {
                var s = random(random, name);
                int[] given = switch (name) {
                    case "MEC_TEXT", "MEC_GRAPHIC", "MEC_TEXTSIZE", "MEC_CATEGORY" -> new int[] {random.nextInt(5) == 0 ? random.nextInt(4) : (s.mec != null ? s.mec.id : 0)};
                    case "MEC_PARAM" -> new int[] {s.mec != null && random.nextInt(5) != 0 ? s.mec.id : random.nextInt(4), s.ptId != -1 && random.nextInt(5) != 0 ? s.ptId : random.nextInt(5)};
                    case "VIDEO_ADVERT_PLAY" -> new int[] {VALUES[random.nextInt(VALUES.length)]};
                    default -> new int[0];
                };
                if (name.startsWith("MEC") && random.nextInt(25) == 0 && given.length > 0) given = Arrays.copyOf(given, given.length - 1);
                var e = executor(given);
                String result = "ok";
                try {
                    apply(s, e);
                    run(ClientScriptOpCode.valueOf(name), e);
                } catch (Throwable error) {
                    if (System.getProperty("fixture.debug") != null) error.printStackTrace();
                    result = "error";
                }
                var ints = new StringBuilder();
                for (int i = 0; i < Math.max(0, e.intStackPtr); i++) ints.append(i > 0 ? "," : "").append(e.intStack[i]);
                var strings = new StringBuilder();
                for (int i = 0; i < Math.max(0, e.stringStackPtr); i++) strings.append(i > 0 ? "," : "").append(hex((String) e.stringStack[i]));
                String post = e.filePicker == null ? "~" : "picker";
                out.append("C\t").append(name).append('\t').append(given.length == 0 ? "-" : Arrays.toString(given).replaceAll("[\\[\\] ]", "")).append('\t').append(s.enc()).append('\t')
                    .append(result).append('\t').append(ints.length() == 0 ? "-" : ints).append('\t').append(strings.length() == 0 ? "-" : strings).append('\t').append(post).append('\n');
                total++;
            }
        }
        var clickState = new State();
        int[][] ranges = {{0, 140}, {990, 1020}, {1990, 2020}, {-3, 0}};
        for (var open : new boolean[] {false, true}) {
            for (var range : ranges) {
                for (int click = range[0]; click < range[1]; click++) {
                    var entry = new Entry("a", "b", null, click).make();
                    set(MiniMenu.class, "isOpen", null, open);
                    int value = MiniMenu.getClickType(entry);
                    out.append("K\t").append(click).append('\t').append(open ? 1 : 0).append('\t').append(value).append('\n');
                    total++;
                }
            }
            set(MiniMenu.class, "isOpen", null, open);
            out.append("K\t-\t").append(open ? 1 : 0).append('\t').append(MiniMenu.getClickType(null)).append('\n');
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.createDirectories(Path.of(args[0]));
        Files.writeString(Path.of(args[0]).resolve("platform.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
