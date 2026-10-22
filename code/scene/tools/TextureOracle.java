import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.material.MaterialTypeList;
import com.jagex.game.runetek5.config.material.texture.TextureOperation;
import com.jagex.game.runetek5.config.material.texture.TextureType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.js5.request.local.LocalFileWorker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TextureOracle {

    private static final int SPRITES = 8;
    private static final int TEXTURES = 9;
    private static final int MATERIALS = 26;
    private static final int TEXTURE_SIZE = 128;
    private static final int SMALL_TEXTURE_SIZE = 64;
    private static final float BRIGHTNESS = 0.7f;

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Path dumps = Path.of(args[1]);
        int sampleEvery = Integer.parseInt(args[2]);
        Files.createDirectories(dumps);

        Js5ResourceProvider materialResource = new Js5ResourceProvider(LocalFileWorker.of(MATERIALS), true, 1);
        Js5ResourceProvider textureResource = new Js5ResourceProvider(LocalFileWorker.of(TEXTURES), false, 1);
        Js5ResourceProvider spriteResource = new Js5ResourceProvider(LocalFileWorker.of(SPRITES), false, 1);
        MaterialTypeList list = new MaterialTypeList(materialResource, textureResource, spriteResource);

        int count = list.getNumMaterials();
        MaterialType[] materials = list.getMaterials();
        Set<Integer> spriteIds = new LinkedHashSet<>();
        Set<String> sampledClasses = new LinkedHashSet<>();
        StringBuilder sb = new StringBuilder(1 << 24);
        sb.append("{\n\"materialCount\":").append(count);
        sb.append(",\n\"textureSize\":").append(TEXTURE_SIZE);
        sb.append(",\n\"smallTextureSize\":").append(SMALL_TEXTURE_SIZE);
        sb.append(",\n\"brightness\":0.7");
        sb.append(",\n\"materials\":[\n");
        int rgb = 0;
        int argb = 0;
        int missing = 0;
        int errors = 0;
        int samples = 0;
        for (int id = 0; id < count; id++) {
            if (id > 0) {
                sb.append(",\n");
            }
            sb.append("{\"id\":").append(id);
            MaterialType material = materials[id];
            if (material == null) {
                sb.append(",\"present\":false}");
                missing++;
                continue;
            }
            sb.append(",\"present\":true,\"fields\":");
            fields(sb, material);
            TextureType type;
            byte[] textureData = null;
            try {
                type = list.getTextureType(id);
            } catch (RuntimeException e) {
                sb.append(",\"error\":").append(quote("getTextureType: " + e));
                sb.append('}');
                errors++;
                continue;
            }
            if (type == null) {
                sb.append(",\"texture\":null,\"path\":\"missing\"}");
                missing++;
                continue;
            }
            textureData = textureResource.getFile(id);
            sb.append(",\"texture\":").append(quote(Base64.getEncoder().encodeToString(textureData)));
            TextureOperation[] ops = type.getTextureOperations();
            sb.append(",\"ops\":[");
            List<String> names = new ArrayList<>();
            for (int i = 0; i < ops.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                String name = ops[i].getClass().getSimpleName();
                names.add(name);
                sb.append(quote(name));
            }
            sb.append(']');
            sb.append(",\"textureFileIds\":").append(ints(type.getTextureFileIds()));
            sb.append(",\"materialFileIds\":").append(ints(type.getMaterialFileIds()));
            sb.append(",\"opaqueOp\":").append(indexOf(ops, type.getOpaqueOperation()));
            sb.append(",\"opacityOp\":").append(indexOf(ops, type.getOpacityOperation()));
            sb.append(",\"hdrOp\":").append(indexOf(ops, type.getHdrOperation()));
            for (int spriteId : type.getTextureFileIds()) {
                spriteIds.add(spriteId);
            }
            boolean cached;
            try {
                cached = list.isCached(id);
            } catch (RuntimeException e) {
                sb.append(",\"error\":").append(quote("isCached: " + e));
                sb.append('}');
                errors++;
                continue;
            }
            sb.append(",\"cached\":").append(cached);
            if (!cached) {
                sb.append(",\"path\":\"uncached\"}");
                missing++;
                continue;
            }
            int size = material.isSmall ? SMALL_TEXTURE_SIZE : TEXTURE_SIZE;
            boolean opaque = material.effectCombiner != 2 && MaterialTypeList.isOpaqueEffect(material.effectId);
            int[] pixels;
            try {
                pixels = opaque ? list.getPixels(id, BRIGHTNESS, size, size, true) : list.getPixelsArgb(id, size, size);
            } catch (RuntimeException e) {
                sb.append(",\"error\":").append(quote("pixels: " + e));
                sb.append('}');
                errors++;
                continue;
            }
            if (opaque) {
                rgb++;
            } else {
                argb++;
            }
            sb.append(",\"path\":").append(quote(opaque ? "rgb" : "argb"));
            sb.append(",\"size\":").append(size);
            boolean mipmapped = material.mipmapping != 0;
            sb.append(",\"mipmapped\":").append(mipmapped);
            List<int[]> levels = mipmapped ? mipmapChain(pixels, size, size) : List.of(pixels);
            byte[] level0 = bytes(pixels);
            sb.append(",\"levels\":").append(levels.size());
            sb.append(",\"level0Sha256\":").append(quote(sha256(level0)));
            MessageDigest chain = MessageDigest.getInstance("SHA-256");
            for (int[] level : levels) {
                chain.update(bytes(level));
            }
            sb.append(",\"chainSha256\":").append(quote(hex(chain.digest())));
            boolean newClass = false;
            for (String name : names) {
                if (sampledClasses.add(name)) {
                    newClass = true;
                }
            }
            boolean sample = newClass || (sampleEvery > 0 && id % sampleEvery == 0);
            if (sample) {
                samples++;
                sb.append(",\"pixels\":").append(quote(hex(level0)));
                if (levels.size() > 1) {
                    sb.append(",\"level1\":").append(quote(hex(bytes(levels.get(1)))));
                }
            }
            Files.write(dumps.resolve("material-" + id + ".bgra"), level0);
            sb.append('}');
        }
        sb.append("\n],\n\"sprites\":{");
        boolean first = true;
        int spriteCount = 0;
        for (int spriteId : spriteIds) {
            byte[] data = spriteResource.getFile(spriteId);
            if (data == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            spriteCount++;
            sb.append('\n').append(quote(Integer.toString(spriteId))).append(':').append(quote(Base64.getEncoder().encodeToString(data)));
        }
        sb.append("\n},\n\"counts\":{\"rgb\":").append(rgb).append(",\"argb\":").append(argb).append(",\"missing\":").append(missing)
            .append(",\"errors\":").append(errors).append(",\"samples\":").append(samples).append(",\"sprites\":").append(spriteCount).append("}\n}\n");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println("materials=" + count + " rgb=" + rgb + " argb=" + argb + " missing=" + missing + " errors=" + errors + " samples=" + samples + " sprites=" + spriteCount);
    }

    private static void fields(StringBuilder sb, MaterialType m) {
        sb.append('{');
        sb.append("\"detailsOnly\":").append(m.detailsOnly);
        sb.append(",\"isSmall\":").append(m.isSmall);
        sb.append(",\"skipTriangles\":").append(m.skipTriangles);
        sb.append(",\"brightness\":").append(m.brightness);
        sb.append(",\"alpha\":").append(m.alpha);
        sb.append(",\"effectId\":").append(m.effectId);
        sb.append(",\"effectParam1\":").append(m.effectParam1);
        sb.append(",\"colorHsl\":").append(m.colorHsl);
        sb.append(",\"textureSpeedU\":").append(m.textureSpeedU);
        sb.append(",\"textureSpeedV\":").append(m.textureSpeedV);
        sb.append(",\"aBool2087\":").append(m.aBool2087);
        sb.append(",\"isBrickTile\":").append(m.isBrickTile);
        sb.append(",\"mipmapping\":").append(m.mipmapping);
        sb.append(",\"repeatS\":").append(m.repeatS);
        sb.append(",\"repeatT\":").append(m.repeatT);
        sb.append(",\"hdr\":").append(m.hdr);
        sb.append(",\"combineMode\":").append(m.combineMode);
        sb.append(",\"effectParam2\":").append(m.effectParam2);
        sb.append(",\"effectCombiner\":").append(m.effectCombiner);
        sb.append('}');
    }

    private static List<int[]> mipmapChain(int[] pixels, int width, int height) {
        List<int[]> levels = new ArrayList<>();
        int step = width;
        int currentHeight = height;
        int minDimension = Math.min(step, currentHeight);
        int halfWidth = step >> 1;
        int halfHeight = currentHeight >> 1;
        int[] argb = pixels;
        while (true) {
            levels.add(argb);
            if (minDimension <= 1) {
                return levels;
            }
            int[] next = new int[halfWidth * halfHeight];
            int dst = 0;
            int src = 0;
            int srcOffset = src + step;
            for (int row = 0; row < halfHeight; row++) {
                for (int col = 0; col < halfWidth; col++) {
                    int a = argb[src++];
                    int b = argb[src++];
                    int c = argb[srcOffset++];
                    int d = argb[srcOffset++];
                    int red = (a >> 24 & 0xff) + (b >> 24 & 0xff) + (c >> 24 & 0xff) + (d >> 24 & 0xff);
                    int green = (a >> 16 & 0xff) + (b >> 16 & 0xff) + (c >> 16 & 0xff) + (d >> 16 & 0xff);
                    int blue = (a >> 8 & 0xff) + (b >> 8 & 0xff) + (c >> 8 & 0xff) + (d >> 8 & 0xff);
                    int alpha = (a & 0xff) + (b & 0xff) + (c & 0xff) + (d & 0xff);
                    next[dst++] = (red & 0x3fc) << 22 | (green & 0x3fc) << 14 | (blue & 0x3fc) << 6 | (alpha & 0x3fc) >> 2;
                }
                src += step;
                srcOffset += step;
            }
            argb = next;
            step = halfWidth;
            currentHeight = halfHeight;
            halfWidth >>= 1;
            halfHeight >>= 1;
            minDimension >>= 1;
        }
    }

    private static byte[] bytes(int[] pixels) {
        byte[] out = new byte[pixels.length * 4];
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            out[i * 4] = (byte) p;
            out[i * 4 + 1] = (byte) (p >> 8);
            out[i * 4 + 2] = (byte) (p >> 16);
            out[i * 4 + 3] = (byte) (p >> 24);
        }
        return out;
    }

    private static int indexOf(TextureOperation[] ops, TextureOperation op) {
        for (int i = 0; i < ops.length; i++) {
            if (ops[i] == op) {
                return i;
            }
        }
        return -1;
    }

    private static String ints(int[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values[i]);
        }
        return sb.append(']').toString();
    }

    private static String sha256(byte[] data) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String hex(byte[] data) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            out[i * 2] = digits[data[i] >> 4 & 0xf];
            out[i * 2 + 1] = digits[data[i] & 0xf];
        }
        return new String(out);
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
