import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.utils.MathUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class MeshOracle {
    private static final String PINNED = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String digest(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 12);
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

    private static void groups(DataOutputStream out, int[][] values) throws Exception {
        out.writeByte(2);
        out.writeInt(values.length);
        for (int[] group : values) ints(out, group);
    }

    private interface Builder {
        int[][] build() throws Exception;
    }

    private static void group(DataOutputStream out, Builder builder) throws Exception {
        try {
            groups(out, builder.build());
        } catch (Throwable error) {
            out.writeByte(3);
        }
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
        group(out, () -> m.createVertexGroups(true));
        group(out, () -> m.createVertexGroups(false));
        group(out, m::createFaceGroups);
        group(out, m::createBillboardGroups);
        out.flush();
        return raw.toByteArray();
    }

    private static void apply(Mesh m, String ops) {
        if (ops.equals("-")) return;
        for (String op : ops.split("\\|")) {
            String[] p = op.split(":");
            switch (p[0]) {
                case "R" -> m.recolor((short) Integer.parseInt(p[1]), (short) Integer.parseInt(p[2]));
                case "RF" -> {
                    short src = m.facesColor != null && m.numFaces > 0 ? m.facesColor[0] : 0;
                    m.recolor(src, (short) Integer.parseInt(p[1]));
                }
                case "T" -> m.retexture((short) Integer.parseInt(p[1]), (short) Integer.parseInt(p[2]));
                case "TF" -> {
                    short src = m.facesMaterial != null && m.numFaces > 0 ? m.facesMaterial[0] : 0;
                    m.retexture(src, (short) Integer.parseInt(p[1]));
                }
                case "ROT" -> m.rotateWithoutNormals(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                case "UP" -> m.upscale();
                case "TR" -> m.translate(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                default -> throw new IllegalArgumentException(op);
            }
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
        Map<Integer, byte[]> models = new HashMap<>();
        try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(bundle), 1 << 20))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                byte[] data = new byte[in.readInt()];
                in.readFully(data);
                models.put(id, data);
            }
        }
        List<String> lines = Files.readAllLines(cases, StandardCharsets.US_ASCII);
        int errors = 0;
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(output), 1 << 20), false, StandardCharsets.US_ASCII)) {
            for (String line : lines) {
                String[] f = line.split("\t");
                String result;
                try {
                    if (f[0].equals("TRIG")) {
                        ByteArrayOutputStream raw = new ByteArrayOutputStream();
                        DataOutputStream data = new DataOutputStream(raw);
                        for (int v : MathUtils.SINE) data.writeInt(v);
                        for (int v : MathUtils.COSINE) data.writeInt(v);
                        data.flush();
                        result = digest(raw.toByteArray());
                    } else {
                        String[] ids = f[1].split(",");
                        Mesh mesh;
                        if (f[0].equals("M")) {
                            Mesh[] parts = new Mesh[ids.length];
                            for (int i = 0; i < ids.length; i++) parts[i] = new Mesh(models.get(Integer.parseInt(ids[i])));
                            mesh = new Mesh(parts, parts.length);
                        } else {
                            mesh = new Mesh(models.get(Integer.parseInt(ids[0])));
                        }
                        apply(mesh, f[2]);
                        result = digest(canonical(mesh));
                    }
                } catch (Throwable error) {
                    errors++;
                    result = "error";
                }
                out.println(line + "\t" + result);
            }
        }
        if (!hash(jar).equals(PINNED)) throw new IllegalStateException("Oracle JAR changed");
        System.out.println("MESH_ORACLE ok cases=" + lines.size() + " errors=" + errors);
    }
}
