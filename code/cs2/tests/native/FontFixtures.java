import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.MathStringOps;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/**
 * Drives the real MathStringOps dispatch for STRINGWIDTH, PARAWIDTH, PARAHEIGHT and PARAHEIGHT_EXTRA.
 * The fonts are the real cache fonts (raw bytes rebuilt from the read-only cache by the generator)
 * plus synthetic variable-width fonts, installed into Resource.FONT_METRICS through a provider whose
 * archive files are filled directly. The inline sprite groups and the name icon sprites are fakes
 * with known sizes, so the sprite= and img= widths are exercised through the real classes too.
 */
public final class FontFixtures {
    static final int PROVIDER_SIZE = 10000;
    static final StringBuilder out = new StringBuilder();
    static final TreeMap<String, Integer> counts = new TreeMap<>();
    static final TreeMap<String, Integer> errors = new TreeMap<>();
    static int serial;

    static final class FakeSprite extends NativeSprite {
        final int width;
        FakeSprite(int width) { this.width = width; }
        public void draw(int a, int b, int c, int d, int e) {}
        public void setSize(int a, int b, int c, int d) {}
        public void cacheSizeInto(int[] a) {}
        public int getWidth() { return width; }
        public int getSpriteHeight() { return 0; }
        public void captureScreenRegion(int a, int b, int c, int d, int e, int f) {}
        public void pixelTransfer() {}
        public void drawColoredCircle(int a, int b, int c, int d, int e, int f, int g) {}
        public void drawAffine(float a, float b, float c, float d, float e, float f, int g, int h, int i) {}
        public void drawAffineMasked(float a, float b, float c, float d, float e, float f, int g, ComponentSurface h, int i, int j) {}
        public void drawScaledColored(int a, int b, int c, int d, int e, int f, int g) {}
        public void drawWithSurface(int a, int b, ComponentSurface c, int d, int e) {}
        public int getHeight() { return 0; }
        public int getSpriteWidth() { return width; }
        public FrameBuffer getFramebuffer() { return null; }
    }

    static sun.misc.Unsafe unsafe() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }

    static Js5ResourceProvider provider(Map<Integer, byte[]> files) throws Exception {
        var unsafe = unsafe();
        var provider = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        var index = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        var counts = new int[PROVIDER_SIZE];
        var archiveFiles = new Object[PROVIDER_SIZE][];
        for (var entry : files.entrySet()) {
            counts[entry.getKey()] = 1;
            archiveFiles[entry.getKey()] = new Object[] {entry.getValue()};
        }
        index.fileCounts = counts;
        provider.masterIndex = index;
        provider.archiveFiles = archiveFiles;
        return provider;
    }

    static byte[] syntheticFont(long seed, boolean variable) throws Exception {
        var random = new Random(seed);
        var bytes = new ByteArrayOutputStream();
        var data = new DataOutputStream(bytes);
        data.writeByte(0);
        data.writeByte(variable ? 1 : 0);
        var widths = new int[256];
        var heights = new int[256];
        for (int i = 0; i < 256; i++) {
            int glyph = (i == 32 || i == 160) ? 4 + random.nextInt(4) : (i < 32 ? 0 : 3 + random.nextInt(12));
            data.writeByte(glyph);
            widths[i] = variable ? (i < 32 ? 0 : 1 + random.nextInt(glyph + 4)) : 0;
            heights[i] = variable ? random.nextInt(4) : 0;
        }
        if (variable) {
            for (int i = 0; i < 256; i++) data.writeByte(widths[i]);
            for (int i = 0; i < 256; i++) data.writeByte(heights[i]);
            for (int i = 0; i < 256; i++) for (int j = 0; j < widths[i]; j++) data.writeByte(random.nextInt(3) - (j == 0 ? 0 : 1));
            for (int i = 0; i < 256; i++) for (int j = 0; j < widths[i]; j++) data.writeByte(random.nextInt(3) - (j == 0 ? 0 : 1));
        } else {
            data.writeByte(14);
        }
        data.writeByte(9);
        data.writeByte(8);
        data.writeByte(13 + random.nextInt(5));
        data.writeByte(2 + random.nextInt(4));
        return bytes.toByteArray();
    }

    static byte[] spriteGroup(int frames, int biggestWidth, int biggestHeight) throws Exception {
        var bytes = new ByteArrayOutputStream();
        var data = new DataOutputStream(bytes);
        for (int i = 0; i < frames; i++) {
            data.writeByte(0);
            data.writeByte(0);
        }
        data.writeShort(biggestWidth);
        data.writeShort(biggestHeight);
        data.writeByte(0);
        for (int i = 0; i < frames; i++) data.writeShort(0);
        for (int i = 0; i < frames; i++) data.writeShort(0);
        for (int i = 0; i < frames; i++) data.writeShort(1);
        for (int i = 0; i < frames; i++) data.writeShort(1);
        data.writeShort(frames);
        return bytes.toByteArray();
    }

    static String hexBytes(byte[] bytes) {
        return bytes.length == 0 ? "-" : HexFormat.of().formatHex(bytes);
    }

    static final int[] ICONS = {11, 0, 7, 13};
    // id, frames, width, height
    static final int[][] SPRITES = {{1, 1, 10, 12}, {2, 2, 20, 40}, {3, 1, 0, 0}, {4, 3, 64, 100}, {5, 1, 31, 9}, {6, 2, 5, 5}, {7, 1, 100, 30}};

    static final String[] TEXTS = {
        "", " ", "a", "Hello", "Hello world", "The quick brown fox jumps over the lazy dog", "   leading and trailing   ",
        "one<br>two", "one<br>", "<br>", "<br><br>x", "a<br>b<br>c<br>d", "Wrap this sentence at whatever width we are given today",
        "<lt>b<gt> &amp; <nbsp>sp<nbsp>ace", "soft<shy>hyphen", "2 <times> 3 <euro>5 <copy> <reg>", "<lt><lt><gt><gt>",
        "<col=ff0000>red</col> text <b>bold</b>", "<unknown>tag", "<a href=x>link</a>", "a < b", "a > b", "<<br>>", "x<", "x<y", ">x", "<>", "<lt", "lt>",
        "<img=0>x", "<img=1>x", "<img=2><img=3>", "<img=4>", "<img=-1>", "<img=x>", "<img=>", "<img=0 >", "<img=99999999999>", "a<img=0>b<img=2>c wide icon test line",
        "<sprite=1>", "<sprite=2>", "<sprite=2,1>", "<sprite=2,2>", "<sprite=3>", "<sprite=4,2>", "<sprite=4,3>", "<sprite=5,0>", "<sprite=6,-1>", "<sprite=7>",
        "<sprite=99>", "<sprite=x>", "<sprite=>", "<sprite=1,>", "<sprite=1,x>", "<sprite=1,0,5>", "<sprite=+1>", "<sprite=-1>", "<sprite= 1>",
        "<obj=4>", "<obj=4,2>", "<obj=>", "<obj=x>", "<obj=1,1,1>", "<obj=99999>", "text <obj=4> in <sprite=5> line <sprite=7> of mixed <img=0> tags",
        "<sprite=1><sprite=5><sprite=4>", "x<sprite=7><br>y<obj=1><br>z",
        "café über   ­ × © ®", "€ ‚ ƒ „ … † ‡ ˆ ‰ Š ‹ Œ Ž", "‘’“”•–—˜™š›œžŸ",
        "Ω Ж 中 Ā", "emoji 😀 pair", "nul \u0000 char", "tab\tchar", "\u0080\u0081\u009f",
        "dash-separated-words-that-can-break-at-hyphens in a line", "averyveryveryverylongwordwithoutanyspacestobreakat", "a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p",
        "AAAA BBBB CCCC DDDD EEEE FFFF GGGG HHHH IIII JJJJ KKKK LLLL MMMM NNNN",
        "iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii",
        "W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W W",
        "line one<br>line two is a bit longer than one<br><br>line four<br>",
        "<col=ff><img=0> <sprite=1> <obj=4> trailing space wrap test here ok",
    };

    static String manyLines(int lines, String piece) {
        return piece.repeat(lines);
    }

    static final int[] WIDTHS = {0, 1, 8, 47, 100, 400, 100000, -1, Integer.MAX_VALUE};

    static void run(ClientScriptOpCode opcode, int[] ints, String text, boolean hasText) {
        var executor = new CS2Executor();
        Arrays.fill(executor.intStack, 0);
        Arrays.fill(executor.stringStack, null);
        System.arraycopy(ints, 0, executor.intStack, 0, ints.length);
        executor.intStackPtr = ints.length;
        executor.stringStack[0] = text;
        executor.stringStackPtr = 1;
        executor.longStackPtr = 0;
        String result = "ok";
        try {
            MathStringOps.dispatch(opcode, executor);
        } catch (RuntimeException error) {
            result = "error:" + error.getClass().getSimpleName();
        }
        counts.merge(opcode.name(), 1, Integer::sum);
        if (!result.equals("ok")) errors.merge(opcode.name() + ":" + result, 1, Integer::sum);
        if (executor.longStackPtr != 0 || executor.intStackPtr < 0 || executor.stringStackPtr < 0) throw new AssertionError(opcode.name());
        var strings = new String[executor.stringStackPtr];
        for (int i = 0; i < strings.length; i++) strings[i] = (String) executor.stringStack[i];
        out.append(opcode.name().toLowerCase()).append('-').append(serial++).append('\t').append(opcode.name()).append('\t')
            .append(NativeFixtures.integers(ints)).append('\t').append(NativeFixtures.utf16(text)).append('\t').append(result).append('\t')
            .append(NativeFixtures.integers(Arrays.copyOf(executor.intStack, executor.intStackPtr))).append('\t')
            .append(NativeFixtures.texts(strings)).append('\n');
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        var cacheFonts = Path.of(args[1]);
        Files.createDirectories(output);
        var files = new TreeMap<Integer, byte[]>();
        try (var list = Files.list(cacheFonts)) {
            for (var path : (Iterable<Path>) list.sorted()::iterator) {
                var name = path.getFileName().toString();
                if (name.endsWith(".bin")) files.put(Integer.parseInt(name.substring(0, name.length() - 4)), Files.readAllBytes(path));
            }
        }
        int real = files.size();
        files.put(9001, syntheticFont(1, true));
        files.put(9002, syntheticFont(2, true));
        files.put(9003, syntheticFont(3, false));
        Resource.FONT_METRICS = provider(files);
        var sprites = new TreeMap<Integer, byte[]>();
        for (var sprite : SPRITES) sprites.put(sprite[0], spriteGroup(sprite[1], sprite[2], sprite[3]));
        StaticMethods.SPRITES_INDEX = provider(sprites);
        var icons = new NativeSprite[ICONS.length];
        for (int i = 0; i < icons.length; i++) icons[i] = new FakeSprite(ICONS[i]);
        StaticMedia.nameIconSprites = icons;

        var table = new StringBuilder();
        for (var entry : files.entrySet()) {
            var metrics = new FontMetrics(entry.getValue());
            var widths = metrics.glyphWidths;
            var kerning = new byte[metrics.kerning == null ? 0 : 65536];
            if (metrics.kerning != null) for (int i = 0; i < 256; i++) System.arraycopy(metrics.kerning[i], 0, kerning, i * 256, 256);
            table.append("font\t").append(entry.getKey()).append('\t').append(hexBytes(entry.getValue())).append('\t')
                .append(metrics.verticalSpacing).append('\t').append(metrics.topPadding).append('\t').append(metrics.bottomPadding).append('\t')
                .append(hexBytes(widths)).append('\t').append(hexBytes(kerning)).append('\n');
        }
        for (int i = 0; i < ICONS.length; i++) table.append("icon\t").append(i).append('\t').append(ICONS[i]).append('\n');
        for (var sprite : SPRITES) table.append("sprite\t").append(sprite[0]).append('\t').append(sprite[1]).append('\t').append(sprite[2]).append('\t').append(sprite[3]).append('\n');
        Files.writeString(output.resolve("fonts.table.tsv"), table);

        var texts = new ArrayList<>(Arrays.asList(TEXTS));
        texts.add(manyLines(150, "a<br>"));
        texts.add(manyLines(99, "a<br>"));
        texts.add(manyLines(98, "a<br>") + "tail");
        texts.add(manyLines(99, "a<br>") + "tail");
        texts.add(manyLines(100, "a<br>"));
        texts.add(manyLines(101, "a<br>"));
        texts.add(manyLines(120, "word "));
        texts.add(manyLines(60, "<obj=4> <sprite=7>\n"));
        var ids = new ArrayList<>(files.keySet());
        var random = new Random(727);
        var heavy = new HashSet<>(List.of(494, 9001, 9002));
        for (int id : ids) {
            for (var text : texts) {
                run(ClientScriptOpCode.STRINGWIDTH, new int[] {id}, text, true);
                int[] widths = heavy.contains(id) ? WIDTHS : new int[] {WIDTHS[1 + random.nextInt(6)]};
                for (int width : widths) {
                    run(ClientScriptOpCode.PARAWIDTH, new int[] {width, id}, text, true);
                    run(ClientScriptOpCode.PARAHEIGHT, new int[] {width, id}, text, true);
                    run(ClientScriptOpCode.PARAHEIGHT_EXTRA, new int[] {width, id}, text, true);
                }
            }
        }
        // errors: absent fonts and null text
        for (int id : new int[] {0, 1, -1, 9000, 9004, 9999, 10000, 10001, 100000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            for (var text : new String[] {"Hello world", "", null}) {
                run(ClientScriptOpCode.STRINGWIDTH, new int[] {id}, text, true);
                run(ClientScriptOpCode.PARAWIDTH, new int[] {20, id}, text, true);
                run(ClientScriptOpCode.PARAHEIGHT, new int[] {20, id}, text, true);
                run(ClientScriptOpCode.PARAHEIGHT_EXTRA, new int[] {20, id}, text, true);
            }
        }
        for (int id : new int[] {494, 9001, 9003}) {
            run(ClientScriptOpCode.STRINGWIDTH, new int[] {id}, null, true);
            run(ClientScriptOpCode.PARAWIDTH, new int[] {20, id}, null, true);
            run(ClientScriptOpCode.PARAHEIGHT, new int[] {20, id}, null, true);
            run(ClientScriptOpCode.PARAHEIGHT_EXTRA, new int[] {20, id}, null, true);
        }
        Files.writeString(output.resolve("fonts.native.tsv"), out);
        System.out.println("Native font cases " + counts);
        System.out.println("Native font error classes " + errors);
        System.out.println("Generated " + serial + " actual native font dispatch cases over " + files.size() + " fonts (" + real + " from the cache)");
        System.out.flush();
        Runtime.getRuntime().halt(0);
    }
}
