import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.flo.FloType;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluType;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.scene.MapLoader;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;
import java.io.BufferedWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sun.misc.Unsafe;

public final class GroundGeometryOracle {
    private static final String SHA256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(object, value);
                return;
            } catch (NoSuchFieldException missing) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Map<String, Object> object(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put((String) entries[i], entries[i + 1]);
        }
        return result;
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        StringBuilder result = new StringBuilder();
        if (value instanceof Map<?, ?> map) {
            result.append('{');
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (result.length() > 1) result.append(',');
                result.append(json(entry.getKey())).append(':').append(json(entry.getValue()));
            }
            return result.append('}').toString();
        }
        result.append('[');
        if (value instanceof Iterable<?> list) {
            for (Object entry : list) {
                if (result.length() > 1) result.append(',');
                result.append(json(entry));
            }
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i != 0) result.append(',');
                result.append(json(Array.get(value, i)));
            }
        } else {
            throw new IllegalArgumentException(value.getClass().getName());
        }
        return result.append(']').toString();
    }

    private static final class RecordingGround extends Ground {
        final List<Map<String, Object>> calls = new ArrayList<>();
        MapLoader loader;

        RecordingGround(int width, int height, int[][] heights) {
            super(width, height, 512, heights);
        }

        @Override public void addBlendedTile(int x, int z, int[] offsetsX, int[] heightsA, int[] offsetsZ, int[] heightsB, int[] colors, int[] blending, int[] textures, int[] scales, WaterTile water, boolean shadows) {
            calls.add(object("x", x, "z", z, "offsets_x", copy(offsetsX), "offsets_z", copy(offsetsZ),
                "draw_heights_a", copy(heightsA), "draw_heights_b", copy(heightsB), "main_colors", copy(colors),
                "blending_colors", copy(blending), "textures", copy(textures), "texture_scales", copy(scales),
                "shadows", shadows, "effective_shape", loader.getPathShape(), "effective_rotation", loader.getTileRotation(),
                "overlay_supports_blending", loader.getOverlaySupportsBlending(), "unblendable", loader.getUnblendable(),
                "blend_priorities", copy(loader.getWaterSlots()), "blend_function_masks", copy(loader.getOverlayFunctionMask())));
        }

        private static int[] copy(int[] values) { return values == null ? null : values.clone(); }
        private UnsupportedOperationException unused() { return new UnsupportedOperationException("unexpected Ground operation"); }
        @Override public void setTileShadow(int x, int z, int displacement) { throw unused(); }
        @Override public void calculateBlockTiles(int x, int z, int[] a, int[] b, int[] c, int[] d, int[] e, int[] f, int[] g, int[] h, int[] i, int[] j, int[] k, WaterTile water) { throw unused(); }
        @Override public void resetLight() { throw unused(); }
        @Override public void addShadow(Shadow shadow, int x, int height, int z) { throw unused(); }
        @Override public void removeShadow(Shadow shadow, int x, int height, int z) { throw unused(); }
        @Override public boolean hasRenderingShadows(Shadow shadow, int x, int height, int z) { throw unused(); }
        @Override public void addLight(PointLight light, int[] displacement) { throw unused(); }
        @Override public void renderMinimapFloor(int a, int b, int c, int d, int e, int f, boolean[][] visible) { throw unused(); }
        @Override public void renderFloor(int x, int z, int range, boolean[][] visible, boolean below) { throw unused(); }
        @Override public Shadow createShadowAt(int x, int z, Shadow shadow) { throw unused(); }
    }

    private static final class Case {
        final String name;
        final int width;
        final int height;
        final int[][] heights;
        final int[][] palette;
        final int[][] current;
        final int[][] next;
        final byte[][][] underlayIds;
        final byte[][][] overlayIds;
        final byte[][][] shapes;
        final byte[][][] rotations;
        final FloTypeList overlays;
        final FluTypeList underlays;
        final Map<Integer, MaterialType> materials = new LinkedHashMap<>();
        final List<FloType> definitions = new ArrayList<>();

        Case(String[] fields) throws Exception {
            name = fields[1];
            width = Integer.parseInt(fields[2]);
            height = Integer.parseInt(fields[3]);
            if (width < 1 || height < 1 || width > 5 || height > 5) throw new IllegalArgumentException("unbounded synthetic map");
            heights = new int[width + 1][height + 1];
            palette = new int[width][height];
            current = fields[4].equals("1") ? new int[width + 1][height + 1] : null;
            next = fields[5].equals("1") ? new int[width + 1][height + 1] : null;
            underlayIds = new byte[1][width][height];
            overlayIds = new byte[1][width][height];
            shapes = new byte[1][width][height];
            rotations = new byte[1][width][height];
            overlays = (FloTypeList) UNSAFE.allocateInstance(FloTypeList.class);
            set(overlays, "overlayCache", new LRUCache(64));
            underlays = (FluTypeList) UNSAFE.allocateInstance(FluTypeList.class);
            set(underlays, "cache", new LRUCache(128));
        }

        void accept(String[] f) {
            int[] v = new int[f.length - 1];
            for (int i = 1; i < f.length; i++) v[i - 1] = Integer.parseInt(f[i]);
            switch (f[0]) {
                case "overlay" -> {
                    FloType overlay = new FloType();
                    overlay.overlayId = v[0] - 1;
                    overlay.tileColorHsl = v[1];
                    overlay.minimapColorHsl = v[2];
                    overlay.texture = v[3];
                    overlay.textureScale = v[4];
                    overlay.slot = v[5];
                    overlay.blendsWithUnderlay = v[6] != 0;
                    overlay.shadowed = v[7] != 0;
                    overlay.occlude = v[8] != 0;
                    overlays.overlayCache.put(overlay, v[0] - 1L);
                    definitions.add(overlay);
                }
                case "underlay" -> {
                    FluType underlay = new FluType();
                    underlay.texture = v[1];
                    underlay.scale = v[2];
                    underlay.shadowed = v[3] != 0;
                    underlay.occlude = v[4] != 0;
                    underlays.cache.put(underlay, v[0] - 1L);
                }
                case "material" -> {
                    MaterialType material = new MaterialType();
                    material.colorHsl = (short) v[1];
                    material.detailsOnly = v[2] != 0;
                    materials.put(v[0], material);
                }
                case "tile" -> {
                    int x = v[0], z = v[1];
                    underlayIds[0][x][z] = (byte) v[2];
                    overlayIds[0][x][z] = (byte) v[3];
                    shapes[0][x][z] = (byte) v[4];
                    rotations[0][x][z] = (byte) v[5];
                    palette[x][z] = v[6];
                }
                case "height" -> {
                    heights[v[0]][v[1]] = v[2];
                    if (current != null) current[v[0]][v[1]] = v[3];
                    if (next != null) next[v[0]][v[1]] = v[4];
                }
                default -> throw new IllegalArgumentException(f[0]);
            }
        }

        Map<String, Object> execute() throws Exception {
            MaterialProvider provider = (MaterialProvider) Proxy.newProxyInstance(MaterialProvider.class.getClassLoader(), new Class<?>[]{MaterialProvider.class}, (proxy, method, args) -> {
                if (!method.getName().equals("getMaterial")) throw new UnsupportedOperationException(method.getName());
                MaterialType material = materials.get((Integer) args[0]);
                if (material == null) throw new IllegalArgumentException("undefined material " + args[0]);
                return material;
            });
            SafeModeToolkit toolkit = (SafeModeToolkit) UNSAFE.allocateInstance(SafeModeToolkit.class);
            set(toolkit, "materialProvider", provider);
            Scene scene = (Scene) UNSAFE.allocateInstance(Scene.class);
            scene.tiles = new Tile[4][width][height];
            MapLoader loader = new MapLoader(scene, 1, width, height, false, overlays, underlays, new SettingsBits(width, height));
            set(loader, "underlayIds", underlayIds);
            set(loader, "overlayIds", overlayIds);
            set(loader, "overlayPathShapes", shapes);
            set(loader, "overlayRotations", rotations);
            RecordingGround ground = new RecordingGround(width, height, heights);
            ground.loader = loader;
            List<Integer> rendered = new ArrayList<>();
            for (FloType overlay : definitions) rendered.add(ColorUtil.getOverlayColorHsl(toolkit, overlay));
            Ground currentGround = current == null ? null : new RecordingGround(width, height, current);
            Ground nextGround = next == null ? null : new RecordingGround(width, height, next);
            loader.addBlendedTiles(toolkit, ground, 0, palette, currentGround, nextGround);
            List<int[]> loaded = new ArrayList<>();
            for (int x = 0; x < width; x++) for (int z = 0; z < height; z++) if (scene.tiles[0][x][z] != null) loaded.add(new int[]{x, z});
            return object("name", name, "rendered_colors", rendered, "calls", ground.calls, "loaded_tiles", loaded);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("jar input.tsv output.jsonl");
        Path jar = Path.of(args[0]).toRealPath();
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        if (!actual.equals(SHA256)) throw new IllegalStateException("unexpected jar SHA-256 " + actual);
        Path loaded = Path.of(MapLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!loaded.equals(jar)) throw new IllegalStateException("MapLoader was loaded from " + loaded);
        int count = 0;
        try (BufferedWriter output = Files.newBufferedWriter(Path.of(args[2]))) {
            Case current = null;
            for (String line : Files.readAllLines(Path.of(args[1]))) {
                if (line.isBlank()) continue;
                String[] fields = line.split("\t");
                if (fields[0].equals("case")) {
                    if (current != null) throw new IllegalStateException("unterminated case");
                    current = new Case(fields);
                } else if (fields[0].equals("end")) {
                    if (current == null) throw new IllegalStateException("no current case");
                    output.write(json(current.execute()));
                    output.newLine();
                    current = null;
                    count++;
                } else {
                    if (current == null) throw new IllegalStateException("no current case");
                    current.accept(fields);
                }
            }
            if (current != null) throw new IllegalStateException("unterminated case");
        }
        String after = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        if (!after.equals(SHA256)) throw new IllegalStateException("jar changed");
        System.out.println("GROUND_GEOMETRY_NATIVE_ORACLE_OK cases=" + count);
    }
}
