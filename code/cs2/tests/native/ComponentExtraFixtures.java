import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.periodic.PulseEvent;
import com.jagex.game.runetek5.client.periodic.PulseEventHandler;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentType;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ComponentExtraFixtures {
    static final String[] NAMES = {
        "CC_CREATE", "CC_DELETE", "CC_DELETEALL", "CC_FIND", "IF_FIND",
        "CC_GETX", "IF_GETX", "CC_GETY", "IF_GETY", "CC_GETWIDTH", "IF_GETWIDTH",
        "CC_GETHEIGHT", "IF_GETHEIGHT", "CC_GETHIDE", "IF_GETHIDE", "CC_GETCOLOUR", "IF_GETCOLOUR",
        "CC_GETGRAPHIC", "IF_GETGRAPHIC", "CC_GETTRANS", "IF_GETTRANS",
        "CC_GETSCROLLX", "IF_GETSCROLLX", "CC_GETSCROLLY", "IF_GETSCROLLY",
        "CC_GETSCROLLWIDTH", "IF_GETSCROLLWIDTH", "CC_GETSCROLLHEIGHT", "IF_GETSCROLLHEIGHT",
        "CC_GETTEXT", "IF_GETTEXT",
        "CC_SETPOSITION", "IF_SETPOSITION", "CC_SETSIZE", "IF_SETSIZE", "CC_SETHIDE", "IF_SETHIDE",
        "CC_SETCOLOUR", "IF_SETCOLOUR", "CC_SETTEXT", "IF_SETTEXT", "CC_SETGRAPHIC", "IF_SETGRAPHIC",
        "CC_SETTRANS", "IF_SETTRANS",
        "CC_SETFILL", "IF_SETFILL",
        "CC_SETLINEWID", "IF_SETLINEWID",
        "CC_SETALPHA", "IF_SETALPHA",
        "CC_SETTEXTSHADOW", "IF_SETTEXTSHADOW",
        "CC_SETOUTLINE", "IF_SETOUTLINE",
        "CC_SETGRAPHICSHADOW", "IF_SETGRAPHICSHADOW",
        "CC_SETVFLIP", "IF_SETVFLIP",
        "CC_SETHFLIP", "IF_SETHFLIP",
        "CC_SET2DANGLE", "IF_SET2DANGLE",
        "CC_SETTILING", "IF_SETTILING",
        "CC_SETLINEDIRECTION", "IF_SETLINEDIRECTION",
        "CC_SETMAXLINES", "IF_SETMAXLINES",
        "CC_SETNOCLICKTHROUGH", "IF_SETNOCLICKTHROUGH",
        "CC_SETSCROLLPOS", "IF_SETSCROLLPOS",
        "CC_SETMODELZOOM", "IF_SETMODELZOOM",
        "CC_SETTEXTFONT", "IF_SETTEXTFONT",
        "CC_SETFONTMONO", "IF_SETFONTMONO",
        "CC_SETMODELORTHOG", "IF_SETMODELORTHOG",
        "CC_SETMODELORIGIN", "IF_SETMODELORIGIN",
        "CC_SETTEXTALIGN", "IF_SETTEXTALIGN",
        "CC_SETTARGETVERB", "IF_SETTARGETVERB",
        "CC_SETPAUSETEXT", "IF_SETPAUSETEXT",
        "CC_SETCLICKMASK", "IF_SETCLICKMASK",
        "CC_SETTEXTANTIMACRO", "IF_SETTEXTANTIMACRO",
        "CC_SETDRAGDEADZONE", "IF_SETDRAGDEADZONE",
        "CC_SETMODELANGLE", "IF_SETMODELANGLE",
        "CC_SETMODELLIGHTING", "IF_SETMODELLIGHTING",
        "CC_SETMODELLIGHTING_SUNROTATION", "IF_SETMODELLIGHTING_SUNROTATION",
        "CC_SETMODELTINT", "IF_SETMODELTINT",
        "CC_SETMODEL", "IF_SETMODEL",
        "CC_SETMODEL_ITEMCONTAINER", "IF_SETMODEL_ITEMCONTAINER",
        "CC_SETSCROLLSIZE", "IF_SETSCROLLSIZE",
        "CC_SETASPECT", "IF_SETASPECT",
        "CC_GETMODELZOOM", "IF_GETMODELZOOM",
        "CC_GETMODELANGLE_X", "IF_GETMODELANGLE_X",
        "CC_GETMODELANGLE_Y", "IF_GETMODELANGLE_Y",
        "CC_GETMODELANGLE_Z", "IF_GETMODELANGLE_Z",
        "CC_GETMODELXOF", "IF_GETMODELXOF",
        "CC_GETMODELYOF", "IF_GETMODELYOF",
        "CC_GETMODEL", "IF_GETMODEL"
    };
    static final int[] VALUES = {0, 1, -1, 2, 3, 5, 10, 50, 100, 255, 256, 640, 65535, -100, -7, 0x7fff0000, Integer.MAX_VALUE, Integer.MIN_VALUE, 0xff00ff};
    static final int[] TYPES = {0, 1, 4, 5, 9, 10, -1, 255, 256, 3, 7};
    static final int[] SLOTS = {-1, 0, 1, 2, 3, 4, 5, 8, 20, 70000, -2, Integer.MAX_VALUE, Integer.MIN_VALUE};
    static final String[] TEXTS = {null, "", "a", "hello", "é€", "line\tbreak", "x".repeat(300)};
    static final long SEED = 0x727c5221L;
    static final int STEPS = 9000;
    static final Map<String, Integer> ORDINAL = new HashMap<>();

    static final class World {
        final String name;
        final int capacity;
        final int width;
        final int height;
        final List<String> lines = new ArrayList<>();
        final List<int[]> interfaces = new ArrayList<>();
        final Map<Integer, List<String[]>> components = new LinkedHashMap<>();
        final Map<Integer, Boolean> prevent = new LinkedHashMap<>();
        int cache = 1;

        World(String name, int capacity, int width, int height) {
            this.name = name;
            this.capacity = capacity;
            this.width = width;
            this.height = height;
        }

        void iface(int id, boolean prevention, int count) {
            components.put(id, new ArrayList<>(Collections.nCopies(count, (String[]) null)));
            prevent.put(id, prevention);
        }

        void comp(int iface, int index, int type, int parent, int x, int y, int w, int h, int bx, int by, int bw, int bh, int axt, int ayt, int awt, int aht, int aw, int ah, int ax, int ay, int hidden, int sx, int sy, int sw, int sh, int sprite, String text, int color, int trans, boolean rendered) {
            int hash = (iface << 16) | index;
            String[] row = {
                "C", "" + index, "" + type, "" + hash, "" + parent, "" + x, "" + y, "" + w, "" + h, "" + bx, "" + by, "" + bw, "" + bh,
                "" + axt, "" + ayt, "" + awt, "" + aht, "" + aw, "" + ah, "" + ax, "" + ay, "" + hidden,
                "" + sx, "" + sy, "" + sw, "" + sh, "" + sprite, NativeFixtures.hex(text), "" + color, "" + trans,
                rendered ? "1" : "0", "" + cache++
            };
            components.get(iface).set(index, row);
        }
    }

    static int argumentCount(String name) {
        return switch (name) {
            case "CC_CREATE" -> 3;
            case "CC_DELETE" -> 0;
            case "CC_DELETEALL", "IF_FIND" -> 1;
            case "CC_FIND" -> 2;
            default -> {
                String bare = name.substring(name.indexOf('_') + 1);
                int n = bare.equals("SETMODELANGLE") ? 6
                    : bare.equals("SETMODELLIGHTING") || bare.equals("SETMODELTINT") || bare.equals("SETMODEL_ITEMCONTAINER") ? 4
                    : bare.equals("SETMODELLIGHTING_SUNROTATION") || bare.equals("SETSCROLLSIZE") || bare.equals("SETASPECT") ? 2
                    : bare.equals("SETMODEL") ? 1
                    : name.endsWith("SETPOSITION") || name.endsWith("SETSIZE") ? 4
                    : name.endsWith("SETTEXTALIGN") ? 3
                    : name.endsWith("SETSCROLLPOS") || name.endsWith("SETMODELORIGIN") ? 2
                    : name.endsWith("SETTEXT") || name.endsWith("SETTARGETVERB") || name.endsWith("SETPAUSETEXT") ? 0
                    : name.contains("_SET") ? 1 : 0;
                yield name.startsWith("IF_") ? n + 1 : n;
            }
        };
    }

    static String hex(String text) {
        return text == null ? "null" : "hex:" + NativeFixtures.hex(text);
    }

    static World worldMain() {
        var w = new World("main", 8, 765, 503);
        w.iface(3, true, 8);
        w.comp(3, 0, 0, -1, 0, 0, 400, 300, 0, 0, 400, 300, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 500, 400, -1, "", 0, 0, true);
        w.comp(3, 1, 0, 3 << 16, 10, 20, 100, 50, 10, 20, 100, 50, 1, 2, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, false);
        w.comp(3, 2, 4, 3 << 16, 5, 6, 80, 12, 5, 6, 80, 12, 2, 1, 1, 1, 50, 25, 1, 1, 0, 0, 0, 0, 0, -1, "hello", 0xff0000, 10, true);
        w.comp(3, 3, 5, 3 << 16, 1, 2, 3, 4, 1, 2, 3, 4, 3, 3, 2, 2, 7, 9, 1, 1, 1, 0, 0, 0, 0, 77, "", 0, 0, false);
        w.comp(3, 4, 0, (3 << 16) | 1, 0, 0, 10, 10, 0, 0, 10, 10, 4, 4, 3, 3, 4, 4, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, true);
        w.comp(3, 6, 0, 3 << 16, 0, 0, 90, 90, 0, 0, 90, 90, 0, 0, 4, 4, 0, 0, 1, 1, 0, 11, 12, 300, 400, -1, "", 0, 0, true);
        w.comp(3, 7, 9, -1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "z", 5, 255, false);
        w.iface(2, false, 5);
        w.comp(2, 0, 0, -1, 0, 0, 300, 200, 0, 0, 300, 200, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, true);
        w.comp(2, 1, 4, 2 << 16, 3, 4, 50, 20, 3, 4, 50, 20, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "t", 1, 2, true);
        w.comp(2, 2, 5, 2 << 16, 7, 8, 16, 16, 7, 8, 16, 16, 1, 1, 1, 1, 100, 100, 1, 1, 0, 0, 0, 0, 0, 5, "", 0, 0, true);
        w.comp(2, 3, 0, -1, 0, 0, 20, 20, 0, 0, 20, 20, 2, 2, 2, 2, 5, 5, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, true);
        w.iface(1, true, 1);
        w.comp(1, 0, -1, -1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, false);
        w.iface(4, true, 2);
        w.comp(4, 1, 0, -1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, false);
        w.iface(5, true, 0);
        w.iface(6, true, 2);
        w.comp(6, 0, 0, -1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, false);
        w.components.get(6).get(0)[3] = "" + (7 << 16);
        return w;
    }

    static World worldDense() {
        var w = new World("dense", 8, 1024, 768);
        w.iface(2, false, 9);
        w.comp(2, 0, 0, -1, 0, 0, 500, 400, 0, 0, 500, 400, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, true);
        for (int i = 1; i <= 7; i++) {
            w.comp(2, i, i % 2 == 0 ? 0 : 4, i == 5 ? (2 << 16) | 4 : (2 << 16) | (i - 1), i * 3, i * 5, 40 + i, 30 + i, i, i, 40 + i, 30 + i, i % 5, (i + 1) % 5, (i + 2) % 5, (i + 3) % 5, i * 11, i * 7, 1, 1, i % 2, 0, 0, 0, 0, i, "c" + i, i, i, i % 3 != 0);
        }
        w.iface(3, true, 3);
        w.comp(3, 0, 0, -1, 0, 0, 100, 100, 0, 0, 100, 100, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, true);
        w.comp(3, 1, 0, 3 << 16, 0, 0, 10, 10, 0, 0, 10, 10, 3, 3, 4, 4, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, true);
        w.comp(3, 2, 0, (3 << 16) | 1, 0, 0, 10, 10, 0, 0, 10, 10, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, -1, "", 0, 0, true);
        return w;
    }

    static List<Integer> hashPool(World w) {
        var pool = new ArrayList<Integer>();
        for (var entry : w.components.entrySet()) {
            int id = entry.getKey();
            for (int i = 0; i <= entry.getValue().size() + 1; i++) pool.add((id << 16) | i);
        }
        pool.addAll(List.of(9 << 16, (9 << 16) | 3, -1, w.capacity << 16, 0xffffffff, (8 << 16) | 2, Integer.MIN_VALUE, (1 << 16) | 0xffff, (3 << 16) | 0xffff));
        return pool;
    }

    static void unlinkAll(List<PulseEvent> events) {
        for (var event : events) {
            event.unlink();
            event.unlinkSub();
        }
    }

    static String dump(Interface[] interfaces, int capacity, Map<String, String> out) {
        out.clear();
        for (int id = 0; id < capacity; id++) {
            var inter = interfaces[id];
            if (inter == null) continue;
            for (int index = 0; index < inter.components.length; index++) {
                var c = inter.components[index];
                if (c == null) continue;
                out.put("i" + id + "." + index, describe(c));
                var children = c.slotChildren;
                if (children != null) {
                    for (int slot = 0; slot < children.length; slot++) {
                        var child = (Component) (Object) children[slot];
                        if (child != null) out.put("i" + id + "." + index + ".s" + slot, describe(child));
                    }
                }
            }
        }
        return null;
    }

    static String describe(Component c) {
        int children = c.slotChildren == null ? -1 : c.slotChildren.length;
        boolean top = c.cacheReference >= 0 && c.cacheReference < Client.Companion.getIS_COMPONENT_ON_TOP().length && Client.Companion.getIS_COMPONENT_ON_TOP()[c.cacheReference];
        return String.join("\t",
            c.type == null ? "-1" : "" + c.type.id, "" + c.idHash, "" + c.parentId, "" + c.slotId,
            "" + c.x, "" + c.y, "" + c.width, "" + c.height, "" + c.basePositionX, "" + c.basePositionY, "" + c.baseWidth, "" + c.baseHeight,
            "" + c.aspectXType, "" + c.aspectYType, "" + c.aspectWidthType, "" + c.aspectHeightType,
            "" + c.aspectWidth, "" + c.aspectHeight, "" + c.aspectX, "" + c.aspectY, c.hidden ? "1" : "0",
            "" + c.scrollX, "" + c.scrollY, "" + c.scrollWidth, "" + c.scrollHeight, "" + c.spriteId,
            NativeFixtures.hex(c.text), "" + c.color, "" + c.transparency, top ? "1" : "0", "" + children,
            c.filled ? "1" : "0", "" + c.lineWidth, c.alpha ? "1" : "0", c.shadow ? "1" : "0", "" + c.borderThickness, "" + c.spriteShadow,
            c.flipVertical ? "1" : "0", c.flipHorizontal ? "1" : "0", "" + c.angle2d, c.tiling ? "1" : "0", c.lineDirection ? "1" : "0", "" + c.maxTextLines,
            c.preventClickThrough ? "1" : "0", "" + c.spriteScale, "" + c.fontId, c.monospaced ? "1" : "0", c.usesOrthogonal ? "1" : "0",
            "" + c.originX, "" + c.originY, "" + c.textHorizontalAli, "" + c.textVerticalAli, "" + c.lineSpacing,
            hex(c.targetVerb), hex(c.resumeText), c.clickMask ? "1" : "0", c.textAntiMacro ? "1" : "0", "" + c.dragDeadzone,
            "" + c.offsetX, "" + c.offsetY, "" + c.spritePitch, "" + c.spriteRoll, "" + c.spriteYaw, "" + c.skyboxId, "" + c.lightPositionX, "" + c.lightPositionY, "" + c.lightPositionZ, "" + c.sunRotationX, "" + c.sunRotationY, "" + c.modelTintRed, "" + c.modelTintGreen, "" + c.modelTintBlue, "" + c.modelTintScalar, "" + c.mediaType.getId(), "" + c.primaryMediaId, "" + c.secondaryMediaId, c.wearCol ? "1" : "0");
    }

    static String eventKind(int type) {
        return switch (type) {
            case 3 -> "text";
            case 6 -> "color";
            case 7 -> "hide";
            case 11 -> "position";
            case 14 -> "graphic";
            case 8 -> "angle";
            case 12 -> "scrollpos";
            case 16 -> "font";
            case 20 -> "monospaced";
            case 21 -> "clickmask";
            case 22 -> "antimacro";
            case 10 -> "offset";
            case 4 -> "content";
            default -> "other" + type;
        };
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

    static String strings(String[] values, int pointer) {
        if (pointer <= 0) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < pointer; i++) {
            if (i > 0) sb.append(',');
            sb.append(hex(values[i]));
        }
        return sb.toString();
    }

    static void dispatch(ClientScriptOpCode op, CS2Executor e) {
        String name = op.name();
        if (name.equals("CC_CREATE") || name.equals("CC_DELETE") || name.equals("CC_DELETEALL") || name.equals("CC_FIND") || name.equals("IF_FIND")) {
            ComponentLifecycleOps.dispatch(op, e);
        } else if (name.startsWith("CC_GET")) {
            ComponentQueryOps.dispatch(op, e);
        } else if (name.startsWith("IF_GET")) {
            InterfaceQueryOps.dispatch(op, e);
        } else if (name.startsWith("CC_SET")) {
            ComponentSetterOps.dispatch(op, e);
        } else {
            InterfaceSetterOps.dispatch(op, e);
        }
    }

    static void build(World w, StringBuilder out, Interface[] interfaces, boolean[] loaded) {
        out.append("W\t").append(w.name).append('\t').append(w.capacity).append('\t').append(w.width).append('\t').append(w.height).append('\n');
        for (var entry : w.components.entrySet()) {
            int id = entry.getKey();
            var rows = entry.getValue();
            out.append("I\t").append(id).append('\t').append(w.prevent.get(id) ? 1 : 0).append('\t').append(rows.size()).append('\n');
            var array = new Component[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                if (row == null) {
                    out.append("N\t").append(i).append('\n');
                    continue;
                }
                out.append(String.join("\t", row)).append('\n');
                var c = new Component();
                int p = 2;
                int typeId = Integer.parseInt(row[p++]);
                c.type = typeId < 0 ? null : ComponentType.forId(typeId);
                c.idHash = Integer.parseInt(row[p++]);
                c.parentId = Integer.parseInt(row[p++]);
                c.x = Integer.parseInt(row[p++]);
                c.y = Integer.parseInt(row[p++]);
                c.width = Integer.parseInt(row[p++]);
                c.height = Integer.parseInt(row[p++]);
                c.basePositionX = Integer.parseInt(row[p++]);
                c.basePositionY = Integer.parseInt(row[p++]);
                c.baseWidth = Integer.parseInt(row[p++]);
                c.baseHeight = Integer.parseInt(row[p++]);
                c.aspectXType = (byte) Integer.parseInt(row[p++]);
                c.aspectYType = (byte) Integer.parseInt(row[p++]);
                c.aspectWidthType = (byte) Integer.parseInt(row[p++]);
                c.aspectHeightType = (byte) Integer.parseInt(row[p++]);
                c.aspectWidth = Integer.parseInt(row[p++]);
                c.aspectHeight = Integer.parseInt(row[p++]);
                c.aspectX = Integer.parseInt(row[p++]);
                c.aspectY = Integer.parseInt(row[p++]);
                c.hidden = Integer.parseInt(row[p++]) != 0;
                c.scrollX = Integer.parseInt(row[p++]);
                c.scrollY = Integer.parseInt(row[p++]);
                c.scrollWidth = Integer.parseInt(row[p++]);
                c.scrollHeight = Integer.parseInt(row[p++]);
                c.spriteId = Integer.parseInt(row[p++]);
                c.text = unhex(row[p++]);
                c.color = Integer.parseInt(row[p++]);
                c.transparency = Integer.parseInt(row[p++]);
                c.lastRenderedCycle = row[p++].equals("1") ? Client.Companion.getLastRenderedCycle() : 0;
                c.cacheReference = Integer.parseInt(row[p++]);
                array[i] = c;
            }
            interfaces[id] = new Interface(w.prevent.get(id), array);
            loaded[id] = true;
        }
    }

    static String unhex(String hex) {
        if (hex.isEmpty()) return "";
        var bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static int inverse(int factor) {
        int inv = factor;
        for (int i = 0; i < 6; i++) inv *= 2 - factor * inv;
        return inv;
    }

    static final Map<String, int[]> STATS = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var out = new StringBuilder();
        var random = new Random(SEED);
        var weights = new ArrayList<String>();
        for (var name : NAMES) {
            weights.add(name);
            if (name.equals("CC_FIND") || name.equals("IF_FIND") || name.equals("CC_CREATE")) {
                weights.add(name);
                weights.add(name);
            }
        }
        var worlds = List.of(worldMain(), worldDense());
        var previous = new HashMap<String, String>();
        var current = new HashMap<String, String>();
        int total = 0;
        for (var w : worlds) {
            var interfaces = new Interface[w.capacity];
            var loaded = new boolean[w.capacity];
            Interface.INTERFACES = interfaces;
            Interface.INTERFACES_LOADED = loaded;
            GameShell.screenCanvasWidth = w.width;
            GameShell.screenCanvasHeight = w.height * inverse(-969250379);
            Client.Companion.setLastRenderedCycle(-2);
            Arrays.fill(Client.Companion.getIS_COMPONENT_ON_TOP(), false);
            build(w, out, interfaces, loaded);
            var e = new CS2Executor();
            e.hookedInterface1.reset();
            e.hookedInterface2.reset();
            var pool = hashPool(w);
            var valid = new ArrayList<Integer>();
            for (var entry : w.components.entrySet()) {
                for (var row : entry.getValue()) if (row != null) valid.add(Integer.parseInt(row[3]));
            }
            for (int secondary = 0; secondary < 2; secondary++) {
                var hook = secondary == 0 ? e.hookedInterface1 : e.hookedInterface2;
                var ids = new ArrayList<>(w.components.keySet());
                int id = ids.get(random.nextInt(ids.size()));
                var rows = w.components.get(id);
                int index = rows.isEmpty() ? 0 : random.nextInt(rows.size());
                if (!rows.isEmpty() && rows.get(index) != null) {
                    hook.inter = interfaces[id];
                    hook.comp = interfaces[id].components[index];
                    out.append("K\t").append(secondary).append('\t').append(id).append('\t').append(index).append('\n');
                }
            }
            dump(interfaces, w.capacity, previous);
            for (int step = 0; step < STEPS; step++) {
                var name = weights.get(random.nextInt(weights.size()));
                var op = ClientScriptOpCode.valueOf(name);
                int operand = random.nextInt(2);
                int count = argumentCount(name);
                var ints = new int[count];
                boolean byHash = name.startsWith("IF_") || name.equals("CC_CREATE") || name.equals("CC_DELETEALL");
                for (int i = 0; i < count; i++) ints[i] = VALUES[random.nextInt(VALUES.length)];
                String[] texts = new String[0];
                if (name.endsWith("SETTEXT") || name.endsWith("SETTARGETVERB") || name.endsWith("SETPAUSETEXT")) texts = new String[] {TEXTS[random.nextInt(TEXTS.length)]};
                int hashAt = -1;
                if (name.equals("CC_CREATE")) {
                    hashAt = 0;
                    ints[1] = random.nextInt(4) != 0 ? new int[] {1, 4, 5, 9}[random.nextInt(4)] : TYPES[random.nextInt(TYPES.length)];
                    ints[2] = random.nextInt(4) != 0 ? random.nextInt(5) : SLOTS[random.nextInt(SLOTS.length)];
                } else if (name.equals("CC_FIND")) {
                    hashAt = 0;
                    ints[1] = random.nextInt(3) == 0 ? -1 : random.nextInt(8) == 0 ? SLOTS[random.nextInt(SLOTS.length)] : random.nextInt(5);
                } else if (name.equals("IF_FIND") || name.equals("CC_DELETEALL")) {
                    hashAt = 0;
                } else if (name.startsWith("IF_")) {
                    hashAt = count - 1;
                }
                if (hashAt >= 0) ints[hashAt] = random.nextInt(4) != 0 ? valid.get(random.nextInt(valid.size())) : pool.get(random.nextInt(pool.size()));
                for (int i = 0; i < ints.length; i++) {
                    if (random.nextInt(8) == 0 && i != hashAt && !(name.equals("CC_CREATE") || name.equals("CC_FIND")))  ints[i] = VALUES[random.nextInt(VALUES.length)];
                }
                e.intStackPtr = ints.length;
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                System.arraycopy(ints, 0, e.intStack, 0, ints.length);
                e.stringStackPtr = texts.length;
                Arrays.fill(e.stringStack, "stale");
                for (int i = 0; i < texts.length; i++) e.stringStack[i] = texts[i];
                e.hasIntOperand = operand == 1;
                String result = "ok";
                try {
                    dispatch(op, e);
                } catch (Throwable error) {
                    result = "error:" + error.getClass().getSimpleName();
                }
                var queued = new ArrayList<PulseEvent>();
                for (CacheableNode node = PulseEventHandler.LOW_PRIORITY.getFirst(); node != null; node = PulseEventHandler.LOW_PRIORITY.getFirst()) {
                    queued.add((PulseEvent) node);
                }
                var eventText = new StringBuilder();
                for (var event : queued) {
                    if (eventText.length() > 0) eventText.append(',');
                    eventText.append(eventKind(event.getType())).append(':').append((int) event.getValue());
                }
                unlinkAll(queued);
                out.append("T\t").append(w.name).append('\t').append(name).append('\t').append(operand).append('\t')
                    .append(stack(ints, ints.length)).append('\t').append(strings(texts, texts.length)).append('\n');
                out.append("R\t").append(result).append('\t').append(stack(e.intStack, e.intStackPtr)).append('\t')
                    .append(strings(Arrays.copyOf(e.stringStack, e.stringStack.length, String[].class), e.stringStackPtr)).append('\t').append(eventText.length() == 0 ? "-" : eventText).append('\t');
                out.append(hookText(e.hookedInterface1, interfaces, w.capacity)).append('\t').append(hookText(e.hookedInterface2, interfaces, w.capacity)).append('\n');
                dump(interfaces, w.capacity, current);
                var keys = new TreeSet<String>(current.keySet());
                keys.addAll(previous.keySet());
                for (var key : keys) {
                    var now = current.get(key);
                    if (!Objects.equals(now, previous.get(key))) out.append("D\t").append(key).append('\t').append(now == null ? "-" : now).append('\n');
                }
                previous.clear();
                previous.putAll(current);
                var stat = STATS.computeIfAbsent(name, k -> new int[2]);
                stat[result.equals("ok") ? 0 : 1]++;
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.writeString(output.resolve("components-extra.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        var stats = new StringBuilder();
        for (var entry : STATS.entrySet()) stats.append(entry.getKey()).append('\t').append(entry.getValue()[0]).append('\t').append(entry.getValue()[1]).append('\n');
        Files.writeString(output.resolve("components-extra.stats.tsv"), stats.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }

    static String hookText(CS2Interface hook, Interface[] interfaces, int capacity) {
        if (hook.comp == null || hook.inter == null) return "-";
        int id = -1;
        for (int i = 0; i < capacity; i++) if (interfaces[i] == hook.inter) id = i;
        return id + ":" + hook.comp.idHash + ":" + hook.comp.slotId;
    }
}
