import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import com.jagex.game.runetek5.config.rsinterface.cs2.ops.*;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class MiniMenuFixtures {
    static final int[] VALUES = {0, 1, 2, 5, 7, 255, 520, -1, -2, 100, Integer.MIN_VALUE, Integer.MAX_VALUE};

    public static void main(String[] args) throws Exception {
        var random = new Random(0x727c5228L ^ 0x31171L);
        var out = new StringBuilder();
        int total = 0;
        for (var name : new String[] {"MINIMENU_SETMAXENTRIES", "DEFAULTMINIMENU"}) {
            for (int n = 0; n < 120; n++) {
                MiniMenu.maxVisibleEntries = random.nextInt(3) == 0 ? -1 : VALUES[random.nextInt(VALUES.length)];
                FontMetrics.useBoldFont = random.nextBoolean();
                var given = name.equals("DEFAULTMINIMENU") ? new int[0] : random.nextInt(15) == 0 ? new int[0] : new int[] {VALUES[random.nextInt(VALUES.length)]};
                var pre = MiniMenu.maxVisibleEntries + "," + (FontMetrics.useBoldFont ? 1 : 0);
                var e = new CS2Executor();
                Arrays.fill(e.intStack, 0x5a5a5a5a);
                e.intStackPtr = 0;
                e.stringStackPtr = 0;
                e.intOpValues = new int[] {0};
                e.instrPtr = 0;
                for (int v : given) e.intStack[e.intStackPtr++] = v;
                String result = "ok";
                try {
                    var op = ClientScriptOpCode.valueOf(name);
                    QuickChatDisplayOps.dispatch(op, e);
                } catch (Throwable error) {
                    result = "error";
                }
                var stack = new StringBuilder();
                for (int i = 0; i < Math.max(0, e.intStackPtr); i++) stack.append(i > 0 ? "," : "").append(e.intStack[i]);
                out.append("C\t").append(name).append('\t').append(given.length == 0 ? "-" : String.valueOf(given[0])).append('\t').append(pre).append('\t')
                    .append(result).append('\t').append(stack.length() == 0 ? "-" : stack).append('\t')
                    .append(MiniMenu.maxVisibleEntries).append(',').append(FontMetrics.useBoldFont ? 1 : 0).append('\n');
                total++;
            }
        }
        out.append("Z\t").append(total).append('\n');
        Files.createDirectories(Path.of(args[0]));
        Files.writeString(Path.of(args[0]).resolve("mini-menu.native.tsv"), out.toString(), StandardCharsets.UTF_8);
        System.exit(0);
    }
}
