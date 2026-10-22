import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class FontLayoutFixtures {
    static final String[] PIECES = {
        "a", "b", "i", "w", "M", " ", " ", " ", ".", "-", "-", "e", "t", "<br>", "<col=ff0000>", "</col>", "<lt>", "<gt>",
        "<nbsp>", "<euro>", "<copy>", "<reg>", "<img=3>", "<shad>", "<u>", "</u>", "<", ">", "é", "€", "…", "Ā", "x", "y"
    };

    static String hex(byte[] bytes) {
        var sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    static String text(String s) {
        return s == null ? "-" : hex(s.getBytes(StandardCharsets.UTF_16BE));
    }

    static String lines(String[] buffer, int count) {
        if (count == 0) return "e";
        var sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(';');
            sb.append(text(buffer[i]));
        }
        return sb.toString();
    }

    static byte[] fixed(Random r) {
        var out = new ByteArrayOutputStream();
        out.write(0);
        out.write(0);
        for (int i = 0; i < 256; i++) out.write(i == 32 ? 3 + r.nextInt(4) : 2 + r.nextInt(8));
        out.write(10 + r.nextInt(6));
        out.write(0);
        out.write(0);
        out.write(8 + r.nextInt(5));
        out.write(2 + r.nextInt(4));
        return out.toByteArray();
    }

    static byte[] variable(Random r) {
        var out = new ByteArrayOutputStream();
        out.write(0);
        out.write(1);
        int[] widths = new int[256];
        int[] heights = new int[256];
        for (int i = 0; i < 256; i++) {
            widths[i] = i == 32 ? 3 : r.nextInt(7);
            heights[i] = i == 32 ? 8 : r.nextInt(5);
        }
        for (int i = 0; i < 256; i++) out.write(2 + r.nextInt(8));
        for (int i = 0; i < 256; i++) out.write(widths[i]);
        for (int i = 0; i < 256; i++) out.write(heights[i]);
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < widths[i]; j++) out.write(r.nextInt(5) - 2);
            }
        }
        out.write(0);
        out.write(0);
        out.write(8 + r.nextInt(5));
        out.write(2 + r.nextInt(4));
        return out.toByteArray();
    }

    static byte[] sprite(Random r) {
        int count = 1 + r.nextInt(4);
        int[] minX = new int[count], minY = new int[count], w = new int[count], h = new int[count];
        int biggestW = 0, biggestH = 0;
        for (int i = 0; i < count; i++) {
            minX[i] = r.nextInt(3);
            minY[i] = r.nextInt(3);
            w[i] = r.nextInt(6);
            h[i] = r.nextInt(6);
            biggestW = Math.max(biggestW, minX[i] + w[i]) + r.nextInt(2);
            biggestH = Math.max(biggestH, minY[i] + h[i]) + r.nextInt(2);
        }
        for (int i = 0; i < count; i++) {
            biggestW = Math.max(biggestW, minX[i] + w[i]);
            biggestH = Math.max(biggestH, minY[i] + h[i]);
        }
        int colors = r.nextInt(6);
        var out = new ByteArrayOutputStream();
        for (int i = 0; i < count; i++) {
            int flags = r.nextInt(4);
            boolean opaque = r.nextInt(3) == 0;
            out.write(flags);
            int n = w[i] * h[i];
            int planes = (flags & 2) != 0 ? 2 : 1;
            for (int plane = 0; plane < planes; plane++) {
                for (int k = 0; k < n; k++) {
                    out.write(plane == 0 ? r.nextInt(colors + 1) : (opaque ? 255 : r.nextInt(256)));
                }
            }
        }
        for (int i = 0; i < colors; i++) {
            int c = r.nextInt(4) == 0 ? 0 : r.nextInt(0x1000000);
            out.write(c >> 16);
            out.write(c >> 8);
            out.write(c);
        }
        out.write(biggestW >> 8);
        out.write(biggestW);
        out.write(biggestH >> 8);
        out.write(biggestH);
        out.write(colors);
        for (int[] column : new int[][] {minX, minY, w, h}) {
            for (int i = 0; i < count; i++) {
                out.write(column[i] >> 8);
                out.write(column[i]);
            }
        }
        out.write(count >> 8);
        out.write(count);
        return out.toByteArray();
    }

    static String sprites(byte[] data) {
        var frames = SpriteType.decode(data);
        var sb = new StringBuilder();
        for (int i = 0; i < frames.length; i++) {
            var f = frames[i];
            if (i > 0) sb.append(';');
            sb.append(f.minX).append(',').append(f.minY).append(',').append(f.width).append(',').append(f.height).append(',').append(f.width + f.minX + f.borderX).append(',').append(f.height + f.minY + f.borderY).append(',');
            for (int k = 0; k < f.pixels.length; k++) {
                int p = f.pixels[k] & 0xff;
                int argb = f.alpha != null ? ((f.alpha[k] & 0xff) << 24) | f.pallete[p] : (p != 0 ? 0xff000000 | f.pallete[p] : 0);
                if (k > 0) sb.append('.');
                sb.append(Integer.toHexString(argb));
            }
            sb.append(',');
            for (int k = 0; k < f.pixels.length; k++) {
                int mask = f.alpha != null ? (f.alpha[k] & 0xff) : ((f.pixels[k] & 0xff) != 0 ? 255 : 0);
                if (k > 0) sb.append('.');
                sb.append(mask);
            }
        }
        return sb.toString();
    }

    static String randomText(Random r) {
        var sb = new StringBuilder();
        int n = r.nextInt(40);
        for (int i = 0; i < n; i++) sb.append(PIECES[r.nextInt(PIECES.length)]);
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        var out = new ArrayList<String>();
        var random = new Random(0x727f0a7L);
        List<byte[]> metrics = new ArrayList<>();
        for (int i = 0; i < 12; i++) metrics.add(i % 2 == 0 ? fixed(random) : variable(random));
        int cases = 0;
        for (int m = 0; m < metrics.size(); m++) {
            out.add("M\t" + m + "\t" + hex(metrics.get(m)));
            var fm = new FontMetrics(metrics.get(m));
            for (int c = 0; c < 60; c++) {
                String t = randomText(random);
                int limit = 8 + random.nextInt(120);
                String[] a = new String[100];
                String[] b = new String[100];
                int na = fm.splitLinesToBuffer(t, null, a, null);
                int nb = fm.splitLinesToBuffer(t, new int[] {limit}, b, null);
                out.add(String.join("\t", "C", "" + m, text(t), "" + limit, "" + fm.getWidthNoSprites(t),
                    text(fm.fitText(t, limit, null)), lines(a, na), lines(b, nb)));
                cases++;
            }
        }
        int sprites = 0;
        for (int i = 0; i < 300; i++) {
            byte[] data = sprite(random);
            out.add("P\t" + i + "\t" + hex(data) + "\t" + sprites(data));
            sprites++;
        }
        out.add("Z\tfont\t" + metrics.size() + "\t" + cases + "\t" + sprites);
        Files.writeString(Path.of(args[0]).resolve("font.native.tsv"), String.join("\n", out) + "\n");
    }
}
