import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.bas.BasTypeList;
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType;
import com.jagex.game.runetek5.config.identitykit.IdkType;
import com.jagex.game.runetek5.config.identitykit.IdkTypeList;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.item.ItemTypeCustomisation;
import com.jagex.game.runetek5.config.item.ItemTypeList;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.tools.Language;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public final class EntityOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static sun.misc.Unsafe unsafe;
    private static IdkTypeList idks;
    private static BasTypeList bases;
    private static ItemTypeList items;
    private static NpcTypeList npcs;
    private static Js5ResourceProvider modelRes;
    private static EquipmentDefaultsType defaults;

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 12);
    }

    private static Object get(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static int num(Object target, String name) throws Exception {
        return ((Number) get(target, name)).intValue();
    }

    private static void ints(DataOutputStream out, int[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        for (int value : values) out.writeInt(value);
    }

    private static void shorts(DataOutputStream out, short[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        for (short value : values) out.writeShort(value);
    }

    private static void bytes(DataOutputStream out, byte[] values) throws Exception {
        if (values == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        out.writeInt(values.length);
        out.write(values);
    }

    private static byte[] canonical(Mesh m) throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(raw);
        out.writeInt(m.version);
        out.writeInt(m.numVertices);
        out.writeInt(m.maxDepth);
        out.writeInt(m.numFaces);
        out.writeByte(m.priority);
        out.writeInt(m.numTextures);
        bytes(out, m.texturesMapping);
        ints(out, m.verticesX);
        ints(out, m.verticesY);
        ints(out, m.verticesZ);
        shorts(out, m.facesA);
        shorts(out, m.facesB);
        shorts(out, m.facesC);
        ints(out, m.vertexGroups);
        bytes(out, m.faceRenderType);
        bytes(out, m.facesLayer);
        bytes(out, m.facesAlpha);
        ints(out, m.faceGroups);
        shorts(out, m.facesMaterial);
        shorts(out, m.facesColor);
        bytes(out, m.facesTexture);
        shorts(out, m.textureFaceX);
        shorts(out, m.textureFaceY);
        shorts(out, m.textureFaceZ);
        ints(out, m.texturesScaleX);
        ints(out, m.texturesScaleY);
        ints(out, m.texturesScaleZ);
        bytes(out, m.texturesRotation);
        bytes(out, m.texturesDirection);
        ints(out, m.texturesSpeed);
        ints(out, m.texturesUtrans);
        ints(out, m.texturesVtrans);
        if (m.particleEmitters == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            out.writeInt(m.particleEmitters.length);
            for (EmissiveTriangle e : m.particleEmitters) {
                out.writeInt(e.producerId);
                out.writeInt(e.faceA);
                out.writeInt(e.faceB);
                out.writeInt(e.faceC);
                out.writeByte(e.priority);
            }
        }
        if (m.particleEffectors == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            out.writeInt(m.particleEffectors.length);
            for (EffectiveVertex e : m.particleEffectors) {
                out.writeInt(e.effectId);
                out.writeInt(e.appliesOnVertex);
            }
        }
        if (m.billboards == null) {
            out.writeByte(0);
        } else {
            out.writeByte(1);
            out.writeInt(m.billboards.length);
            for (Billboard b : m.billboards) {
                out.writeInt(b.billboardId);
                out.writeInt(b.appliesOnFace);
                out.writeInt(b.depth);
                out.writeInt(b.distance);
            }
        }
        shorts(out, m.verticesSubmeshes);
        shorts(out, m.facesSubmeshes);
        out.flush();
        return raw.toByteArray();
    }

    private static String meshResult(Mesh mesh) throws Exception {
        return mesh == null ? "none" : digest(canonical(mesh));
    }

    private static Js5ResourceProvider provider(int[] fileCounts, byte[][][] files) throws Exception {
        Js5MasterIndex index = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        set(index, "fileCounts", fileCounts);
        set(index, "archiveCount", fileCounts.length);
        Js5ResourceProvider res = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        set(res, "loadType", 0);
        set(res, "masterIndex", index);
        Object[] archives = new Object[fileCounts.length];
        Object[][] archiveFiles = new Object[fileCounts.length][];
        for (int a = 0; a < fileCounts.length; a++) {
            if (files[a] != null) {
                archiveFiles[a] = new Object[fileCounts[a]];
                for (int f = 0; f < fileCounts[a]; f++) archiveFiles[a][f] = files[a][f];
            }
        }
        set(res, "archives", archives);
        set(res, "archiveFiles", archiveFiles);
        return res;
    }

    private static byte[][] readBlock(DataInputStream in, int[] sizeOut) throws Exception {
        int count = in.readInt();
        int size = in.readInt();
        sizeOut[0] = size;
        byte[][] data = new byte[size][];
        for (int i = 0; i < count; i++) {
            int id = in.readInt();
            byte[] bytes = new byte[in.readInt()];
            in.readFully(bytes);
            data[id] = bytes;
        }
        return data;
    }

    private static Js5ResourceProvider grouped(byte[][] data, int size, int perArchive) throws Exception {
        int archives = (size + perArchive - 1) / perArchive;
        int[] counts = new int[archives];
        byte[][][] files = new byte[archives][][];
        for (int id = 0; id < size; id++) {
            if (data[id] != null) {
                int a = id / perArchive;
                if (files[a] == null) {
                    files[a] = new byte[perArchive][];
                    counts[a] = perArchive;
                    for (int f = 0; f < perArchive; f++) files[a][f] = new byte[] {0};
                }
                files[a][id % perArchive] = data[id];
            }
        }
        return provider(counts, files);
    }

    private static void load(Path bundle) throws Exception {
        java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        int[] size = new int[1];
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(bundle), 1 << 20))) {
            byte[][] models = readBlock(in, size);
            int[] modelCounts = new int[size[0]];
            byte[][][] modelFiles = new byte[size[0]][][];
            for (int id = 0; id < size[0]; id++) {
                if (models[id] != null) {
                    modelCounts[id] = 1;
                    modelFiles[id] = new byte[][] {models[id]};
                }
            }
            modelRes = provider(modelCounts, modelFiles);
            byte[][] idkData = readBlock(in, size);
            int idkSize = size[0];
            byte[][] basData = readBlock(in, size);
            int basSize = size[0];
            int configSize = 33;
            int[] configCounts = new int[configSize];
            byte[][][] configFiles = new byte[configSize][][];
            configCounts[3] = idkSize;
            configFiles[3] = new byte[idkSize][];
            for (int i = 0; i < idkSize; i++) configFiles[3][i] = idkData[i] != null ? idkData[i] : new byte[] {0};
            configCounts[32] = basSize;
            configFiles[32] = new byte[basSize][];
            for (int i = 0; i < basSize; i++) configFiles[32][i] = basData[i] != null ? basData[i] : new byte[] {0};
            Js5ResourceProvider config = provider(configCounts, configFiles);
            byte[][] itemData = readBlock(in, size);
            Js5ResourceProvider itemRes = grouped(itemData, size[0], 256);
            byte[][] npcData = readBlock(in, size);
            Js5ResourceProvider npcRes = grouped(npcData, size[0], 128);
            byte[] equipment = new byte[in.readInt()];
            in.readFully(equipment);
            int[] eqCounts = new int[] {7};
            byte[][][] eqFiles = new byte[1][7][];
            for (int f = 0; f < 7; f++) eqFiles[0][f] = new byte[] {0};
            eqFiles[0][6] = equipment;
            defaults = new EquipmentDefaultsType(provider(eqCounts, eqFiles));
            idks = new IdkTypeList(config, modelRes);
            bases = new BasTypeList(config, defaults);
            ItemTypeList list = (ItemTypeList) unsafe.allocateInstance(ItemTypeList.class);
            set(list, "game", Game.RUNESCAPE);
            set(list, "language", Language.ENGLISH);
            set(list, "objResource", itemRes);
            set(list, "meshResource", modelRes);
            set(list, "typeCache", new LRUCache(64));
            set(list, "isMember", true);
            set(list, "defaultGroundActions", new String[6]);
            set(list, "defaultInventoryActions", new String[5]);
            items = list;
            npcs = new NpcTypeList(Game.RUNESCAPE, Language.ENGLISH, npcRes, modelRes);
        }
    }

    private static ItemTypeCustomisation customisation(ItemType item, String hex) throws Exception {
        if (hex.equals("-")) return null;
        return ItemTypeCustomisation.Companion.decodeItemEffects(item, new Packet(HexFormat.of().parseHex(hex)));
    }

    private static int[] csv(String text) {
        if (text.equals("-") || text.isEmpty()) return new int[0];
        String[] p = text.split(",");
        int[] out = new int[p.length];
        for (int i = 0; i < p.length; i++) out[i] = Integer.parseInt(p[i]);
        return out;
    }

    private static void transform(Mesh mesh, int[] t) {
        int tx = 0, ty = 0, tz = 0, rx = 0, ry = 0, rz = 0;
        if (t != null) {
            tx = t[0];
            ty = t[1];
            tz = t[2];
            rx = t[3] << 3;
            ry = t[4] << 3;
            rz = t[5] << 3;
        }
        if (rx != 0 || ry != 0 || rz != 0) mesh.rotateWithoutNormals(rx, ry, rz);
        if (tx != 0 || ty != 0 || tz != 0) mesh.translate(tx, ty, tz);
    }

    private static PlayerAppearance player(String[] f) throws Exception {
        boolean female = f[1].equals("1");
        int basId = Integer.parseInt(f[2]);
        int[] appearance = csv(f[3]);
        PlayerAppearance pa = new PlayerAppearance();
        pa.appearance = appearance;
        pa.female = female;
        pa.basId = basId;
        int[] recolor = csv(f[7]);
        pa.recolorDstIndices = recolor;
        if (!f[4].equals("-")) {
            ItemTypeCustomisation[] mods = new ItemTypeCustomisation[appearance.length];
            for (String entry : f[4].split(";")) {
                String[] p = entry.split(":");
                int slot = Integer.parseInt(p[0]);
                ItemType item = items.list(appearance[slot] & 0x3fffffff);
                mods[slot] = customisation(item, p[1]);
            }
            pa.meshModifiers = mods;
        }
        return pa;
    }

    private static Mesh playerBody(PlayerAppearance pa, int shield, int weapon) throws Exception {
        int[] parts = pa.appearance.clone();
        boolean hasShield = false;
        boolean hasWeapon = false;
        if (shield >= 0 || weapon >= 0) {
            if (shield >= 0 && defaults.shieldSlot != -1) {
                hasShield = true;
                parts[defaults.shieldSlot] = shield == 65535 ? 0 : (shield | 0x40000000);
                for (int slot : defaults.hiddenAnimationShieldSlots) parts[slot] = 0;
            }
            if (weapon >= 0 && defaults.weaponSlot != -1) {
                hasWeapon = true;
                parts[defaults.weaponSlot] = weapon == 65535 ? 0 : (weapon | 0x40000000);
                for (int slot : defaults.hiddenAnimationWeaponSlots) parts[slot] = 0;
            }
        }
        BasType bas = pa.basId != -1 ? bases.list(pa.basId) : null;
        Mesh[] meshes = new Mesh[parts.length];
        for (int slot = 0; slot < parts.length; slot++) {
            int part = parts[slot];
            boolean replaced = (slot == 5 && hasShield) || (slot == 3 && hasWeapon);
            if ((part & 0x40000000) != 0) {
                ItemTypeCustomisation modifier = null;
                if (!replaced && pa.meshModifiers != null && pa.meshModifiers[slot] != null) modifier = pa.meshModifiers[slot];
                meshes[slot] = items.list(part & 0x3fffffff).getBodyMesh(pa.female, modifier);
            } else if ((part & 0x80000000) != 0) {
                meshes[slot] = idks.list(part & 0x3fffffff).getBodyMesh();
            }
        }
        if (bas != null && bas.objVerticeTransformations != null) {
            for (int index = 0; index < bas.objVerticeTransformations.length; index++) {
                if (meshes[index] != null) transform(meshes[index], bas.objVerticeTransformations[index]);
            }
        }
        return new Mesh(meshes, meshes.length);
    }

    private static Mesh playerHead(PlayerAppearance pa) throws Exception {
        Mesh[] meshes = new Mesh[pa.appearance.length];
        int count = 0;
        for (int slot = 0; slot < pa.appearance.length; slot++) {
            int part = pa.appearance[slot];
            if ((part & 0x40000000) != 0) {
                ItemTypeCustomisation modifier = null;
                if (pa.meshModifiers != null && pa.meshModifiers[slot] != null) modifier = pa.meshModifiers[slot];
                Mesh mesh = items.list(part & 0x3fffffff).getHeadMesh(pa.female, modifier);
                if (mesh != null) meshes[count++] = mesh;
            } else if ((part & 0x80000000) != 0) {
                Mesh mesh = idks.list(part & 0x3fffffff).getHeadMesh();
                if (mesh != null) meshes[count++] = mesh;
            }
        }
        return new Mesh(meshes, count);
    }

    private static String npcBody(int id, int basParam) throws Exception {
        NPCType npc = npcs.list(id);
        if (get(npc, "alternativeIds") != null) return "skip";
        int[] modelIds = (int[]) get(npc, "modelIds");
        int[][] offsets = (int[][]) get(npc, "modelOffsets");
        int npcBas = num(npc, "npcBasId");
        BasType bas = null;
        if (basParam != -1) bas = bases.list(basParam);
        else if (npcBas != -1) bas = bases.list(npcBas);
        Mesh[] meshes = new Mesh[modelIds.length];
        for (int i = 0; i < modelIds.length; i++) {
            if (modelIds[i] != -1) {
                meshes[i] = Mesh.Companion.fromResource(modelRes, modelIds[i]);
                if (meshes[i] != null) {
                    if (meshes[i].version < 13) meshes[i].upscale();
                    if (offsets != null && offsets[i] != null) meshes[i].translate(offsets[i][0], offsets[i][1], offsets[i][2]);
                }
            }
        }
        if (bas != null && bas.objVerticeTransformations != null) {
            for (int index = 0; index < bas.objVerticeTransformations.length; index++) {
                if (index < meshes.length && meshes[index] != null) transform(meshes[index], bas.objVerticeTransformations[index]);
            }
        }
        Mesh mesh = meshes.length == 1 ? meshes[0] : new Mesh(meshes, meshes.length);
        return meshResult(mesh);
    }

    private static String npcHead(int id) throws Exception {
        NPCType npc = npcs.list(id);
        if (get(npc, "alternativeIds") != null) return "skip";
        int[] heads = (int[]) get(npc, "headModels");
        if (heads == null) return "none";
        Mesh[] meshes = new Mesh[heads.length];
        for (int i = 0; i < heads.length; i++) {
            if (heads[i] != -1) {
                meshes[i] = Mesh.Companion.fromResource(modelRes, heads[i]);
                if (meshes[i] != null && meshes[i].version < 13) meshes[i].upscale();
            }
        }
        Mesh mesh = meshes.length == 1 ? meshes[0] : new Mesh(meshes, meshes.length);
        return meshResult(mesh);
    }

    private static String join(int... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(values[i]);
        }
        return out.toString();
    }

    private static String itemMeta(int id) throws Exception {
        ItemType t = items.list(id);
        int recol = get(t, "recolorSrc") == null ? -1 : ((short[]) get(t, "recolorSrc")).length;
        int retex = get(t, "retexSrc") == null ? -1 : ((short[]) get(t, "retexSrc")).length;
        return join(num(t, "maleEquip1"), num(t, "maleEquip2"), num(t, "maleEquip3"),
                num(t, "femaleEquip1"), num(t, "femaleEquip2"), num(t, "femaleEquip3"),
                num(t, "maleHead1"), num(t, "maleHead2"), num(t, "femaleHead1"), num(t, "femaleHead2"),
                num(t, "wearPos"), num(t, "wearPos2"), num(t, "wearPos3"), recol, retex);
    }

    private static String run(String[] f) throws Exception {
        switch (f[0]) {
            case "EQD": {
                int[] custom = defaults.customizableObjSlots;
                return join(defaults.shieldSlot, defaults.weaponSlot) + "|" + join(custom) + "|"
                        + join(defaults.hiddenAnimationShieldSlots) + "|" + join(defaults.hiddenAnimationWeaponSlots);
            }
            case "IM":
                return itemMeta(Integer.parseInt(f[1]));
            case "IB": {
                ItemType item = items.list(Integer.parseInt(f[1]));
                return meshResult(item.getBodyMesh(f[2].equals("1"), customisation(item, f[3])));
            }
            case "IH": {
                ItemType item = items.list(Integer.parseInt(f[1]));
                return meshResult(item.getHeadMesh(f[2].equals("1"), customisation(item, f[3])));
            }
            case "KB":
                return meshResult(idks.list(Integer.parseInt(f[1])).getBodyMesh());
            case "KH":
                return meshResult(idks.list(Integer.parseInt(f[1])).getHeadMesh());
            case "NB":
                return npcBody(Integer.parseInt(f[1]), Integer.parseInt(f[2]));
            case "NH":
                return npcHead(Integer.parseInt(f[1]));
            case "PB":
                return meshResult(playerBody(player(f), Integer.parseInt(f[5]), Integer.parseInt(f[6])));
            case "PH":
                return meshResult(playerHead(player(f)));
            case "PHASH": {
                PlayerAppearance pa = player(f);
                pa.calculateAppearanceHash();
                return Long.toString(pa.playerAppearanceHash);
            }
            case "PP": {
                int[] ids = csv(f[1]);
                Mesh[] meshes = new Mesh[3];
                int parts = 0;
                for (int id : ids) {
                    Mesh mesh = idks.list(id).getHeadMesh();
                    if (mesh != null) meshes[parts++] = mesh;
                }
                return meshResult(new Mesh(meshes, parts));
            }
            default:
                throw new IllegalArgumentException(f[0]);
        }
    }

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toRealPath();
        Path bundle = Path.of(args[1]);
        Path cases = Path.of(args[2]);
        Path output = Path.of(args[3]);
        if (!hash(jar).equals(PINNED)) throw new IllegalArgumentException("Unexpected oracle JAR");
        Path loaded = Path.of(Mesh.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!jar.equals(loaded)) throw new IllegalArgumentException("code source mismatch");
        load(bundle);
        List<String> lines = Files.readAllLines(cases, StandardCharsets.US_ASCII);
        int errors = 0;
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output), 1 << 20), false, StandardCharsets.US_ASCII)) {
            for (String line : lines) {
                String result;
                try {
                    result = run(line.split("\t", -1));
                } catch (Throwable error) {
                    errors++;
                    if (System.getProperty("oracle.debug") != null) error.printStackTrace();
                    result = "error";
                }
                out.println(line + "\t" + result);
            }
        }
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("ENTITY_ORACLE ok cases=" + lines.size() + " errors=" + errors);
    }
}
