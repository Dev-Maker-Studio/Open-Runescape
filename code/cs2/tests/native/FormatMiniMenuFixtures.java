import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuEntry;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.js5.Js5MasterIndex;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class FormatMiniMenuFixtures {
    static final int[] VALUES = {0, 1, 2, 5, 7, 255, 520, -1, -2, 100, 0xff00ff, 65535, Integer.MIN_VALUE, Integer.MAX_VALUE};
    static sun.misc.Unsafe unsafe;

    static Field field(Class<?> type, String name) throws Exception {
        for (var c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    static void set(Object target, String name, Object value) throws Exception {
        var f = field(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    static Object staticGet(Class<?> type, String name) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        return f.get(null);
    }

    static void staticSet(Class<?> type, String name, Object value) throws Exception {
        var f = field(type, name);
        f.setAccessible(true);
        f.set(null, value);
    }

    static String style() throws Exception {
        return String.join(",",
            "" + staticGet(TextureOpIrregularBricks.class, "backgroundColor"), "" + NativeSprite.backgroundTransparency, "" + MiniMenu.menuHoverHighlightColor,
            "" + StaticMethods.menuBackgroundAlpha, "" + StaticMethods.loadingBarBackgroundSpriteId, "" + NativeSprite.loadingBarTileSpriteId,
            "" + StaticMethods.menuTopBorderSpriteId, "" + StaticMethods.menuSideBorderSpriteId, "" + StaticMethods.menuCornerSpriteId,
            "" + MiniMenu.menuTextColor, "" + StaticMethods.menuHoverTextColor, "" + StaticMethods.menuFontIndex);
    }

    static String state() throws Exception {
        return (MiniMenu.isOpen ? 1 : 0) + "," + (MiniMenu.activeEntry != null ? 1 : 0) + "," + (MiniMenu.secondaryEntry != null ? 1 : 0) + "," + (FontMetrics.useBoldFont ? 1 : 0) + "," + style();
    }

    public static void main(String[] args) throws Exception {
        var theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
        var index = (Js5MasterIndex) unsafe.allocateInstance(Js5MasterIndex.class);
        set(index, "fileCounts", new int[0]);
        var provider = (Js5ResourceProvider) unsafe.allocateInstance(Js5ResourceProvider.class);
        provider.masterIndex = index;
        staticSet(Resource.class, "SPRITES", provider);
        staticSet(Resource.class, "FONT_METRICS", provider);
        var random = new Random(0x727c5229L);
        var out = new StringBuilder();
        int total = 0;
        for (int n = 0; n < 400; n++) {
            MiniMenu.isOpen = random.nextBoolean();
            var entry = (MiniMenuEntry) unsafe.allocateInstance(MiniMenuEntry.class);
            MiniMenu.activeEntry = random.nextBoolean() ? entry : null;
            MiniMenu.secondaryEntry = random.nextBoolean() ? entry : null;
            FontMetrics.useBoldFont = random.nextBoolean();
            staticSet(TextureOpIrregularBricks.class, "backgroundColor", random.nextInt());
            NativeSprite.backgroundTransparency = random.nextInt(300);
            MiniMenu.menuHoverHighlightColor = random.nextInt();
            StaticMethods.menuBackgroundAlpha = random.nextInt(300);
            var given = new int[12];
            for (int i = 0; i < 12; i++) given[i] = random.nextInt(4) == 0 ? VALUES[random.nextInt(VALUES.length)] : random.nextInt(1 << 20);
            var pre = state();
            var e = new CS2Executor();
            Arrays.fill(e.intStack, 0x5a5a5a5a);
            e.intStackPtr = 0;
            e.stringStackPtr = 0;
            e.intOpValues = new int[] {0};
            e.instrPtr = 0;
            for (int v : given) e.intStack[e.intStackPtr++] = v;
            String result = "ok";
            try {
                QuickChatDisplayOps.dispatch(ClientScriptOpCode.FORMATMINIMENU, e);
            } catch (Throwable error) {
                result = "error:" + error.getClass().getSimpleName();
            }
            var stack = new StringBuilder();
            for (int i = 0; i < Math.max(0, e.intStackPtr); i++) stack.append(i > 0 ? "," : "").append(e.intStack[i]);
            var args12 = new StringBuilder();
            for (int i = 0; i < 12; i++) args12.append(i > 0 ? "," : "").append(given[i]);
            out.append("C\t").append(args12).append('\t').append(pre).append('\t').append(result).append('\t')
                .append(stack.length() == 0 ? "-" : stack).append('\t').append(state()).append('\n');
            total++;
        }
        out.append("Z\t").append(total).append('\n');
        Files.createDirectories(Path.of(args[0]));
        Files.writeString(Path.of(args[0]).resolve("format-mini-menu.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
