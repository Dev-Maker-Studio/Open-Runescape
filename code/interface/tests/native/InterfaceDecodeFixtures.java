import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class InterfaceDecodeFixtures {
    static final long SEED = 0x727c1f4eL;
    static final int DECODE_CASES = 2500;
    static final int LAYOUT_CASES = 500;
    static final int[] FORMATS = {255, 0, 1, 2, 3};
    static final int[] CONTENT = {0, 328, 1337, 1403, 1338, 1339, 1400, 1401, 1405, 1406, 1407, 1408, 1409, 1, 65535, 999};
    static final int[] SIZES = {0, 1, 2, 3, 7, 16, 100, 765, 503, 1000, 16384, 65535};
    static final int[] SHORTS = {0, 1, -1, 5, -5, 100, -100, 300, 32767, -32768, 12345};
    static final String[] WORDS = {"", "a", "Hello", "Use", "RuneScape", "runescape", "RUNESCAPE rules", "Welcome to Runescape!", "x<br>y", "tab\there", "é€™", "mixedRuneScapeRuneScape", "runescap"};

    static final class Enc {
        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        Enc u8(int v) { o.write(v & 255); return this; }
        Enc u16(int v) { u8(v >> 8); return u8(v); }
        Enc u24(int v) { u8(v >> 16); u8(v >> 8); return u8(v); }
        Enc i32(int v) { u8(v >> 24); u8(v >> 16); u8(v >> 8); return u8(v); }
        Enc raw(byte[] b) { o.write(b, 0, b.length); return this; }
        Enc str(byte[] b) { raw(b); return u8(0); }
        Enc big(int v) { if (v == -1) return u16(32767); if (v >= 0 && v < 32767) return u16(v); return i32(v | 0x80000000); }
        byte[] bytes() { return o.toByteArray(); }
    }

    static byte[] cp1252(Random r, int length) {
        var b = new byte[length];
        for (int i = 0; i < length; i++) {
            int v = 1 + r.nextInt(255);
            if (r.nextInt(3) != 0) v = 32 + r.nextInt(95);
            b[i] = (byte) v;
        }
        return b;
    }

    static byte[] word(Random r) {
        if (r.nextInt(4) == 0) return cp1252(r, r.nextInt(10));
        var w = WORDS[r.nextInt(WORDS.length)];
        var out = new ByteArrayOutputStream();
        for (char c : w.toCharArray()) {
            int v = c;
            if (v == 0xe9) v = 0xe9;
            else if (v == 0x20ac) v = 0x80;
            else if (v == 0x2122) v = 0x99;
            out.write(v);
        }
        return out.toByteArray();
    }

    static int pick(Random r, int[] values) { return values[r.nextInt(values.length)]; }

    static final class Spec {
        int format = 255;
        int type = 0;
        boolean named;
        int content;
        int bx, by, bw, bh;
        int axt, ayt, awt, aht;
        int parent = 65535;
        int hiddenBits;
        int scrollW, scrollH;
        boolean fuzz;
    }

    static Spec randomSpec(Random r) {
        var s = new Spec();
        s.format = pick(r, FORMATS);
        s.type = r.nextInt(12) == 0 ? 10 + r.nextInt(3) : r.nextInt(10);
        s.named = r.nextInt(4) == 0;
        s.content = pick(r, CONTENT);
        s.bx = pick(r, SHORTS);
        s.by = pick(r, SHORTS);
        s.bw = pick(r, SIZES);
        s.bh = pick(r, SIZES);
        s.axt = r.nextInt(6) - (r.nextInt(8) == 0 ? 1 : 0);
        s.ayt = r.nextInt(6);
        s.awt = r.nextInt(5);
        s.aht = r.nextInt(5);
        s.parent = r.nextInt(3) == 0 ? 65535 : r.nextInt(40);
        s.hiddenBits = r.nextInt(4);
        s.scrollW = r.nextInt(3) == 0 ? 0 : pick(r, SIZES);
        s.scrollH = r.nextInt(3) == 0 ? 0 : pick(r, SIZES);
        s.fuzz = true;
        return s;
    }

    static void script(Enc e, Random r) {
        int n = r.nextInt(3) == 0 ? 1 + r.nextInt(5) : 0;
        e.u8(n);
        for (int i = 0; i < n; i++) {
            int t = r.nextInt(3);
            e.u8(t);
            if (t == 0) e.i32(r.nextInt());
            else if (t == 1) e.str(word(r));
        }
    }

    static void ints(Enc e, Random r) {
        int n = r.nextInt(4) == 0 ? 1 + r.nextInt(4) : 0;
        e.u8(n);
        for (int i = 0; i < n; i++) e.i32(r.nextInt());
    }

    static byte[] gen(Random r, Spec s) {
        var e = new Enc();
        int fmt = s.format;
        e.u8(fmt);
        int formatValue = fmt == 255 ? -1 : fmt;
        e.u8(s.type | (s.named ? 0x80 : 0));
        if (s.named) e.str(word(r));
        e.u16(s.content);
        e.u16(s.bx).u16(s.by).u16(s.bw).u16(s.bh);
        e.u8(s.awt).u8(s.aht).u8(s.axt).u8(s.ayt);
        e.u16(s.parent);
        int mask = s.hiddenBits & 1;
        if (formatValue >= 0) mask |= (s.hiddenBits & 2);
        e.u8(mask);
        if (s.type == 0) {
            e.u16(s.scrollW).u16(s.scrollH);
            if (formatValue < 0) e.u8(r.nextInt(3));
        }
        if (s.type == 5) {
            e.i32(r.nextInt(4) == 0 ? -1 : r.nextInt(5000));
            e.u16(r.nextInt(65536));
            e.u8(r.nextInt(4));
            e.u8(r.nextInt(256));
            e.u8(r.nextInt(8));
            e.i32(r.nextInt());
            e.u8(r.nextInt(3));
            e.u8(r.nextInt(3));
            e.i32(r.nextInt());
            if (formatValue >= 3) e.u8(r.nextInt(3));
        }
        if (s.type == 6) {
            e.big(r.nextInt(4) == 0 ? -1 : r.nextInt(r.nextInt(2) == 0 ? 30000 : 2000000000));
            int flags = r.nextInt(16);
            e.u8(flags);
            if ((flags & 1) == 1) {
                e.u16(r.nextInt(65536)).u16(r.nextInt(65536));
                e.u16(r.nextInt(65536)).u16(r.nextInt(65536)).u16(r.nextInt(65536)).u16(r.nextInt(65536));
            } else if ((flags & 2) == 2) {
                e.u16(r.nextInt(65536)).u16(r.nextInt(65536)).u16(r.nextInt(65536));
                e.u16(r.nextInt(65536)).u16(r.nextInt(65536)).u16(r.nextInt(65536));
                e.u16(r.nextInt(65536));
            }
            e.big(r.nextInt(3) == 0 ? -1 : r.nextInt(9000));
            if (s.awt != 0) e.u16(r.nextInt(65536));
            if (s.aht != 0) e.u16(r.nextInt(65536));
        }
        if (s.type == 4) {
            e.big(r.nextInt(5) == 0 ? -1 : r.nextInt(100));
            if (formatValue >= 2) e.u8(r.nextInt(3));
            e.str(word(r));
            e.u8(r.nextInt(20));
            e.u8(r.nextInt(4));
            e.u8(r.nextInt(4));
            e.u8(r.nextInt(3));
            e.i32(r.nextInt());
            e.u8(r.nextInt(256));
            if (formatValue >= 0) e.u8(r.nextInt(6));
        }
        if (s.type == 3) {
            e.i32(r.nextInt());
            e.u8(r.nextInt(3));
            e.u8(r.nextInt(256));
        }
        if (s.type == 9) {
            e.u8(r.nextInt(6));
            e.i32(r.nextInt());
            e.u8(r.nextInt(3));
        }
        int optionMask = r.nextInt(3) == 0 ? 0 : r.nextInt(1 << 24);
        e.u24(optionMask);
        int triggers = r.nextInt(3) == 0 ? 1 + r.nextInt(3) : 0;
        if (triggers == 0) {
            e.u8(0);
        } else {
            var slots = new ArrayList<Integer>();
            for (int i = 0; i < 11; i++) slots.add(i);
            java.util.Collections.shuffle(slots, r);
            int first = (slots.get(0) + 1) << 4 | r.nextInt(16);
            e.u8(first);
            for (int i = 0; i < triggers; i++) {
                e.u8(r.nextInt(256));
                e.u8(r.nextInt(3));
                e.u8(r.nextInt(256));
                if (i + 1 < triggers) e.u8((slots.get(i + 1) + 1) << 4 | r.nextInt(16));
                else e.u8(0);
            }
        }
        e.str(word(r));
        int optionCount = r.nextInt(4) == 0 ? r.nextInt(6) : 0;
        int cursorCount = r.nextInt(5) == 0 ? 1 + r.nextInt(2) : 0;
        e.u8(optionCount | cursorCount << 4);
        for (int i = 0; i < optionCount; i++) e.str(word(r));
        int firstIndex = 0;
        if (cursorCount > 0) {
            firstIndex = r.nextInt(8);
            e.u8(firstIndex);
            e.u16(r.nextInt(3) == 0 ? 65535 : r.nextInt(2000));
        }
        if (cursorCount > 1) {
            e.u8(s.fuzz && r.nextInt(12) == 0 ? firstIndex + 1 + r.nextInt(4) : r.nextInt(firstIndex + 1));
            e.u16(r.nextInt(3) == 0 ? 65535 : r.nextInt(2000));
        }
        e.str(word(r));
        e.u8(r.nextInt(12));
        e.u8(r.nextInt(100));
        e.u8(r.nextInt(8) == 0 ? 3 + r.nextInt(3) : r.nextInt(3));
        e.str(word(r));
        if (((optionMask >> 11) & 0x7f) != 0) {
            e.u16(r.nextInt(3) == 0 ? 65535 : r.nextInt(2000));
            e.u16(r.nextInt(3) == 0 ? 65535 : r.nextInt(2000));
            e.u16(r.nextInt(3) == 0 ? 65535 : r.nextInt(2000));
        }
        if (formatValue >= 0) {
            e.u16(r.nextInt(3) == 0 ? 65535 : r.nextInt(2000));
            int intParams = r.nextInt(3) == 0 ? 1 + r.nextInt(3) : 0;
            e.u8(intParams);
            for (int i = 0; i < intParams; i++) {
                e.u24(r.nextInt(1 << 24));
                e.i32(r.nextInt());
            }
            int strParams = r.nextInt(3) == 0 ? 1 + r.nextInt(3) : 0;
            e.u8(strParams);
            for (int i = 0; i < strParams; i++) {
                e.u24(r.nextInt(1 << 24));
                e.u8(s.fuzz && r.nextInt(30) == 0 ? 1 : 0);
                e.str(word(r));
            }
        }
        int scripts = formatValue >= 0 ? 21 : 20;
        for (int i = 0; i < scripts; i++) script(e, r);
        for (int i = 0; i < 5; i++) ints(e, r);
        return e.bytes();
    }

    static String hex(byte[] bytes) {
        var sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 255));
        return sb.toString();
    }

    static String text(String s) {
        if (s == null) return "-";
        return "s" + hex(s.getBytes(StandardCharsets.UTF_8));
    }

    static String arr(int[] a) {
        if (a == null) return "-";
        if (a.length == 0) return "e";
        var sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i > 0) sb.append(','); sb.append(a[i]); }
        return sb.toString();
    }

    static String script(Object[] a) {
        if (a == null) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            Object v = a[i];
            if (v == null) sb.append('n');
            else if (v instanceof Integer) sb.append('i').append(v);
            else sb.append(text((String) v));
        }
        return sb.toString();
    }

    static String keys(Component c) {
        if (c.keyTriggeringAction == null) return "-";
        var sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            if (i > 0) sb.append(',');
            var a = c.keyTriggeringAction[i];
            var p = c.keyTriggerParams[i];
            if (a == null) sb.append('n');
            else sb.append(a[0]).append('/').append(p[0]);
        }
        return sb.toString();
    }

    static String params(Component c) {
        var out = new ArrayList<String>();
        for (Node<?> n : c.clientParams) {
            if (n instanceof IntNode in) out.add(n.pointer + ":i:" + in.value);
            else out.add(n.pointer + ":s:" + text((String) ((ObjectNode) n).value));
        }
        java.util.Collections.sort(out);
        return out.isEmpty() ? "-" : String.join(";", out);
    }

    static String b(boolean v) { return v ? "1" : "0"; }

    static String dump(Component c) {
        var f = new ArrayList<String>();
        f.add(c.type == null ? "-1" : "" + c.type.id);
        f.add(text(c.name));
        f.add(c.contentType == null ? "-1" : "" + c.contentType.id);
        f.add("" + c.basePositionX); f.add("" + c.basePositionY); f.add("" + c.baseWidth); f.add("" + c.baseHeight);
        f.add("" + c.aspectWidthType); f.add("" + c.aspectHeightType); f.add("" + c.aspectXType); f.add("" + c.aspectYType);
        f.add("" + c.parentId); f.add(b(c.hidden)); f.add(b(c.preventClickThrough));
        f.add("" + c.scrollWidth); f.add("" + c.scrollHeight);
        f.add("" + c.spriteId); f.add("" + c.angle2d); f.add(b(c.tiling)); f.add(b(c.alpha)); f.add("" + c.transparency);
        f.add("" + c.borderThickness); f.add("" + c.spriteShadow); f.add(b(c.flipVertical)); f.add(b(c.flipHorizontal));
        f.add("" + c.color); f.add(b(c.clickMask));
        f.add("" + c.primaryMediaId); f.add(b(c.hasOrigin)); f.add(b(c.usesOrthogonal)); f.add(b(c.priorityRender));
        f.add("" + c.originX); f.add("" + c.originY); f.add("" + c.originZ);
        f.add("" + c.spritePitch); f.add("" + c.spriteRoll); f.add("" + c.spriteYaw); f.add("" + c.spriteScale);
        f.add("" + c.animation); f.add("" + c.aspectWidth); f.add("" + c.aspectHeight);
        f.add("" + c.fontId); f.add(b(c.monospaced)); f.add(text(c.text));
        f.add("" + c.lineSpacing); f.add("" + c.textHorizontalAli); f.add("" + c.textVerticalAli); f.add(b(c.shadow));
        f.add("" + c.maxTextLines); f.add(b(c.filled)); f.add("" + c.lineWidth); f.add(b(c.lineDirection));
        f.add(keys(c)); f.add(arr(c.cycleSteps)); f.add(c.keyCharCodes == null ? "-" : arr(c.keyCharCodes)); f.add(b(c.hasInteraction));
        f.add(text(c.opBase));
        if (c.options == null) f.add("-");
        else {
            var sb = new StringBuilder();
            for (int i = 0; i < c.options.length; i++) { if (i > 0) sb.append(','); sb.append(text(c.options[i])); }
            f.add(c.options.length == 0 ? "e" : sb.toString());
        }
        f.add(arr(c.opCursors)); f.add(text(c.resumeText));
        f.add("" + c.dragDeadzone); f.add("" + c.dragDeadTime); f.add(c.dragType == null ? "-1" : "" + c.dragType.value);
        f.add(text(c.targetVerb));
        f.add("" + c.targetOverCursor); f.add("" + c.targetLeaveCursor); f.add("" + c.moveOverCursor);
        f.add("" + c.targetParams.settingsHash); f.add("" + c.targetParams.interfaceId);
        f.add(params(c));
        f.add(script(c.onLoadScript)); f.add(script(c.onMouseOver)); f.add(script(c.onMouseLeaveScript)); f.add(script(c.hookParams));
        f.add(script(c.onTargetEnter)); f.add(script(c.onVarpTransmit)); f.add(script(c.mouseLeaveScript)); f.add(script(c.onStatTransmit));
        f.add(script(c.onTimer)); f.add(script(c.params)); f.add(script(c.onTargetLeave)); f.add(script(c.popupScript));
        f.add(script(c.onClick)); f.add(script(c.onClickRepeat)); f.add(script(c.onRelease)); f.add(script(c.onHold));
        f.add(script(c.onDrag)); f.add(script(c.onDragComplete)); f.add(script(c.onMouseMove)); f.add(script(c.onKey));
        f.add(script(c.onScrollWheel)); f.add(script(c.onKeyType)); f.add(b(c.hasScript));
        f.add(arr(c.varps)); f.add(arr(c.mouseLeaveArrayParams)); f.add(arr(c.statTransmitFilter)); f.add(arr(c.keyPressArray)); f.add(arr(c.mouseWheelArray));
        return String.join("\t", f);
    }

    static Component decode(byte[] bytes, int idHash) {
        var c = new Component();
        c.idHash = idHash;
        c.clientParams = new IterableNodeMap<>(16);
        c.decode(new Packet(bytes));
        return c;
    }

    static String run(byte[] bytes, int idHash) {
        try {
            var c = decode(bytes, idHash);
            return "ok\t" + dump(c);
        } catch (Throwable t) {
            return "error\t" + t.getClass().getSimpleName();
        }
    }

    static byte[] mutate(Random r, byte[] in) {
        var b = in.clone();
        int kind = r.nextInt(3);
        if (kind == 0 && b.length > 1) {
            var cut = new byte[r.nextInt(b.length)];
            System.arraycopy(b, 0, cut, 0, cut.length);
            return cut;
        }
        int flips = 1 + r.nextInt(3);
        for (int i = 0; i < flips; i++) b[r.nextInt(b.length)] = (byte) r.nextInt(256);
        return b;
    }

    static void decodeCases(Random r, StringBuilder out) {
        int ok = 0, error = 0;
        for (int i = 0; i < DECODE_CASES; i++) {
            var s = randomSpec(r);
            var bytes = gen(r, s);
            int idHash = (r.nextInt(500) << 16) | r.nextInt(60);
            if (i % 4 == 3) bytes = mutate(r, bytes);
            var result = run(bytes, idHash);
            if (result.startsWith("ok")) ok++; else error++;
            out.append("T\t").append(i).append('\t').append(idHash).append('\t').append(hex(bytes)).append('\t').append(result).append('\n');
        }
        out.append("Z\tdecode\t").append(ok).append('\t').append(error).append('\n');
    }

    static void layoutCases(Random r, StringBuilder out) {
        Client.OPEN_INTERFACES = new IterableNodeMap<>(8);
        int rendered = 0;
        for (int i = 0; i < LAYOUT_CASES; i++) {
            int iface = 1 + r.nextInt(900);
            int count = 1 + r.nextInt(14);
            int width = pick(r, new int[] {0, 1, 100, 503, 765, 1024, 1920, 3000});
            int height = pick(r, new int[] {0, 1, 100, 334, 503, 768, 1080, 2000});
            var comps = new Component[count];
            var specs = new Spec[count];
            var bytes = new byte[count][];
            var aspect = new int[count][];
            var scroll = new int[count][];
            for (int k = 0; k < count; k++) {
                if (r.nextInt(10) == 0) continue;
                var s = new Spec();
                s.format = pick(r, FORMATS);
                s.type = r.nextInt(3) == 0 ? pick(r, new int[] {3, 4, 5, 9}) : 0;
                s.content = 0;
                s.bx = pick(r, SHORTS);
                s.by = pick(r, SHORTS);
                s.bw = pick(r, new int[] {0, 1, 10, 100, 765, 503, 8192, 16384, 20000, 65535});
                s.bh = pick(r, new int[] {0, 1, 10, 100, 334, 503, 8192, 16384, 20000, 65535});
                s.axt = r.nextInt(6);
                s.ayt = r.nextInt(6);
                s.awt = pick(r, new int[] {0, 0, 1, 2, 3, 4});
                s.aht = pick(r, new int[] {0, 0, 1, 2, 3, 4});
                s.parent = (k == 0 || r.nextInt(3) == 0) ? 65535 : r.nextInt(k);
                s.hiddenBits = 0;
                s.scrollW = r.nextInt(3) == 0 ? 0 : pick(r, new int[] {0, 50, 300, 1000, 5000});
                s.scrollH = r.nextInt(3) == 0 ? 0 : pick(r, new int[] {0, 50, 300, 1000, 5000});
                specs[k] = s;
                bytes[k] = gen(r, s);
                comps[k] = decode(bytes[k], (iface << 16) | k);
                aspect[k] = new int[] {1 + r.nextInt(2000), 1 + r.nextInt(2000)};
                comps[k].aspectX = aspect[k][0];
                comps[k].aspectY = aspect[k][1];
                scroll[k] = new int[] {r.nextInt(4000) - 100, r.nextInt(4000) - 100};
                comps[k].scrollX = scroll[k][0];
                comps[k].scrollY = scroll[k][1];
            }
            out.append("L\t").append(i).append('\t').append(iface).append('\t').append(width).append('\t').append(height).append('\t').append(count).append('\n');
            for (int k = 0; k < count; k++) {
                if (comps[k] == null) { out.append("N\t").append(k).append('\n'); continue; }
                out.append("P\t").append(k).append('\t').append(hex(bytes[k])).append('\t').append(aspect[k][0]).append('\t').append(aspect[k][1])
                    .append('\t').append(scroll[k][0]).append('\t').append(scroll[k][1]).append('\n');
            }
            String status = "ok";
            try {
                InterfaceRenderer.INSTANCE.updateInterfaceDimensions(comps, -1, width, height, false);
            } catch (Throwable t) {
                status = "error:" + t.getClass().getSimpleName();
            }
            out.append("R\t").append(i).append('\t').append(status).append('\n');
            for (int k = 0; k < count; k++) {
                var c = comps[k];
                if (c == null) continue;
                out.append("V\t").append(k).append('\t').append(c.x).append('\t').append(c.y).append('\t').append(c.width).append('\t').append(c.height)
                    .append('\t').append(c.scrollX).append('\t').append(c.scrollY).append('\n');
            }
            if (status.equals("ok")) rendered++;
        }
        out.append("Z\tlayout\t").append(rendered).append('\n');
    }

    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        try {
            generate(output);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    static void generate(Path output) throws Exception {
        var r = new Random(SEED);
        var decoded = new StringBuilder();
        decodeCases(r, decoded);
        Files.writeString(output.resolve("decode.native.tsv"), decoded.toString());
        var layout = new StringBuilder();
        layoutCases(r, layout);
        Files.writeString(output.resolve("layout.native.tsv"), layout.toString());
    }
}
