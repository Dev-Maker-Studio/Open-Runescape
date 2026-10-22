import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.config.defaults.InteractionDefaultsType;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.config.param.ParamType;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerType;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.struct.StructType;
import com.jagex.game.runetek5.tools.Language;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import sun.misc.Unsafe;

public final class ConfigOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final int PARAM = 1;
    private static final int ITEM = 2;
    private static final int NPC = 3;
    private static final int SEQ = 4;
    private static final int STRUCT = 5;
    private static final int CONTAINER = 6;
    private static final int INTERACTION = 7;
    private static final Unsafe UNSAFE;
    private static final Map<Integer, byte[]> ITEMS = new HashMap<>();
    private static final Map<Integer, ItemType> ITEM_CACHE = new HashMap<>();
    private static ItemTypeList itemList;
    private static NpcTypeList npcList;
    private static Method itemDecode;
    private static Method itemPost;
    private static Method npcDecode;
    private static long duplicateKeys;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String text(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("u");
        for (int i = 0; i < value.length(); i++) {
            if (i > 0) out.append('.');
            out.append(Integer.toHexString(value.charAt(i)));
        }
        return out.toString();
    }

    private static String ints(int[] values) {
        if (values == null) return "[]";
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(values[i]);
        }
        return out.append(']').toString();
    }

    private static String texts(String[] values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(text(values[i]));
        }
        return out.append(']').toString();
    }

    private static String params(IterableNodeMap<Node<?>> map) {
        if (map == null) return "null";
        TreeSet<Long> keys = new TreeSet<>();
        int total = 0;
        for (Node<?> node = map.first(); node != null; node = map.next()) {
            keys.add(node.pointer);
            total++;
        }
        duplicateKeys += total - keys.size();
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (long key : keys) {
            Node<?> node = map.get(key);
            if (!first) out.append(',');
            first = false;
            out.append(key).append(':');
            if (node instanceof IntNode) out.append('i').append(((IntNode) node).value);
            else out.append(text((String) ((ObjectNode) node).value));
        }
        return out.append('}').toString();
    }

    private static int flag(boolean value) {
        return value ? 1 : 0;
    }

    private static Object get(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        UNSAFE.putObject(target, UNSAFE.objectFieldOffset(field), value);
    }

    private static void setBoolean(Object target, String name, boolean value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        UNSAFE.putBoolean(target, UNSAFE.objectFieldOffset(field), value);
    }

    private static ItemType item(int id) throws Exception {
        ItemType cached = ITEM_CACHE.get(id);
        if (cached != null) return cached;
        ItemType type = new ItemType();
        type.typeList = itemList;
        type.id = id;
        type.groundActions = itemList.defaultGroundActions.clone();
        type.inventoryActions = itemList.defaultInventoryActions.clone();
        byte[] data = ITEMS.get(id);
        if (data != null) itemDecode.invoke(type, new Packet(data));
        itemPost.invoke(type);
        if (type.certTemplateId != -1) type.generateNote(item(type.certTemplateId), item(type.certId));
        if (type.lendTemplateId != -1) type.generateLend(item(type.lendTemplateId), item(type.lendId));
        if (type.bindTemplateId != -1) type.generateBind(item(type.bindTemplateId), item(type.bindId));
        ITEM_CACHE.put(id, type);
        return type;
    }

    private static String canonicalItem(ItemType t) {
        List<String> f = new ArrayList<>();
        f.add("name=" + text(t.name));
        f.add("value=" + t.value);
        f.add("stackable=" + t.stackable);
        f.add("members=" + flag(t.membersOnly));
        f.add("wear=" + t.wearPos + "," + t.wearPos2 + "," + t.wearPos3);
        f.add("multi=" + t.multiStackSize);
        f.add("tooltip=" + flag(t.hasTooltipColor) + "," + t.tooltipColor);
        f.add("cert=" + t.certId + "," + t.certTemplateId);
        f.add("lend=" + t.lendId + "," + t.lendTemplateId);
        f.add("bind=" + t.bindId + "," + t.bindTemplateId);
        f.add("cursor=" + t.customCursorOp1 + "," + t.customCursorId1 + "," + t.customCursorOp2 + "," + t.customCursorId2);
        f.add("tradeable=" + flag(t.tradeable));
        f.add("unk6=" + t.unknownInt6);
        f.add("ground=" + texts(t.groundActions));
        f.add("inventory=" + texts(t.inventoryActions));
        f.add("params=" + params(t.parameters));
        return String.join("\t", f);
    }

    private static String canonicalParam(ParamType t) {
        return "string=" + flag(t.isString()) + "\tdefault=" + t.defaultInt + "\tname=" + text(t.typeName) + "\tautoDisable=" + flag(t.autoDisable);
    }

    @SuppressWarnings("unchecked")
    private static String record(int kind, int id, byte[] data) {
        try {
            switch (kind) {
                case PARAM: {
                    ParamType t = new ParamType();
                    if (data != null) t.decode(new Packet(data));
                    return canonicalParam(t);
                }
                case ITEM:
                    return canonicalItem(item(id));
                case NPC: {
                    NPCType t = new NPCType();
                    t.npcTypeList = npcList;
                    t.actions = new String[] {null, null, null, null, null};
                    if (data != null) npcDecode.invoke(t, new Packet(data));
                    return "params=" + params((IterableNodeMap<Node<?>>) get(t, "parameters"));
                }
                case SEQ: {
                    SeqType t = new SeqType();
                    if (data != null) t.decode(new Packet(data));
                    t.postDecode();
                    return "params=" + params(t.parameters);
                }
                case STRUCT: {
                    StructType t = new StructType();
                    if (data != null) t.decode(new Packet(data));
                    return "params=" + params(t.nodeMap);
                }
                case CONTAINER: {
                    ItemContainerType t = new ItemContainerType();
                    if (data != null) t.decode(new Packet(data));
                    return "max=" + t.maxItemSize + "\tsize=" + t.defaultItemSize + "\tids=" + ints(t.itemIds) + "\tamounts=" + ints(t.amounts);
                }
                case INTERACTION: {
                    InteractionDefaultsType t = (InteractionDefaultsType) UNSAFE.allocateInstance(InteractionDefaultsType.class);
                    t.decode(new Packet(data));
                    return "members=" + t.tooltipColorP2p + "\tfree=" + t.tooltipColorF2p;
                }
                default:
                    return "error";
            }
        } catch (Throwable error) {
            return "error";
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path bundle = Path.of(args[1]);
        Path output = Path.of(args[2]);
        if (!hash(jar).equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        for (Class<?> type : new Class<?>[] {Packet.class, ItemType.class, ItemTypeList.class, NPCType.class, ParamType.class, SeqType.class, StructType.class, ItemContainerType.class, InteractionDefaultsType.class}) {
            Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            if (!jar.equals(loaded)) throw new IllegalArgumentException(type.getName() + " code source mismatch");
        }
        for (Method method : ItemType.class.getDeclaredMethods()) {
            if (method.getName().equals("decodeType")) itemDecode = method;
            if (method.getName().equals("postDecode")) itemPost = method;
        }
        for (Method method : NPCType.class.getDeclaredMethods()) {
            if (method.getName().startsWith("decodeType")) npcDecode = method;
        }
        itemDecode.setAccessible(true);
        itemPost.setAccessible(true);
        npcDecode.setAccessible(true);
        itemList = (ItemTypeList) UNSAFE.allocateInstance(ItemTypeList.class);
        set(itemList, "language", Language.ENGLISH);
        setBoolean(itemList, "isMember", true);
        set(itemList, "defaultGroundActions", new String[] {null, null, "Take", null, null, "Examine"});
        set(itemList, "defaultInventoryActions", new String[] {null, null, null, null, "Drop"});
        npcList = (NpcTypeList) UNSAFE.allocateInstance(NpcTypeList.class);
        setBoolean(npcList, "isMember", true);
        List<int[]> order = new ArrayList<>();
        List<byte[]> payloads = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(bundle), 1 << 20))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int kind = in.readInt();
                int id = in.readInt();
                int length = in.readInt();
                byte[] data = null;
                if (length >= 0) {
                    data = new byte[length];
                    in.readFully(data);
                }
                order.add(new int[] {kind, id});
                payloads.add(data);
                if (kind == ITEM && data != null) ITEMS.put(id, data);
            }
        }
        int errors = 0;
        TreeMap<Integer, Integer> perKind = new TreeMap<>();
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output), 1 << 20), false, StandardCharsets.US_ASCII)) {
            for (int i = 0; i < order.size(); i++) {
                int[] key = order.get(i);
                String line = record(key[0], key[1], payloads.get(i));
                if (line.equals("error")) errors++;
                perKind.merge(key[0], 1, Integer::sum);
                out.println(key[0] + "\t" + key[1] + "\t" + line);
            }
        }
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("CONFIG_ORACLE ok records=" + order.size() + " errors=" + errors + " duplicateParamKeys=" + duplicateKeys + " perKind=" + perKind);
    }
}
