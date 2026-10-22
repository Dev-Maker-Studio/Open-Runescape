import com.jagex.game.runetek5.config.rsinterface.cs2.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Cs2RunOracle {
    static final Map<Integer, String> SCRIPTS = Map.of(
        1146, "0001f400000000004f00989680013f00000001007b00000003004f0000ff8000b800007b0000000901f400000000004f000186a0013f00000001007b00000003004f00ffffff00b800007b00000002004f00ffff0000b800004f0000000000b80000000012000100000000000100000000000001",
        1602, "00031f0000000000393c6c743e00004f00000000004f00000001006c0000393e0003860000091c00b80000000008000000010000000000010000000001",
        2332, "00031f00000000031f00000001004f00000000034c0000a800000000031f0000000100f70000a80000000101f400000000004fffffffff02b500000001007b00000014031f00000000004f0000000001f400000000006c00031f00000002031f0000000001f40000000001f400000001011700031f0000000000f700006c00021300000003016400000000031f00000000031f0000000101f400000000034c0000a800000000007bffffffe8031f0000000000b80000000022000200030000000000030000000001");

    static String hex(String value) { return value == null ? "null" : HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8)); }

    public static void main(String[] args) throws Exception {
        CS2Executor.CS2_CACHE.reset();
        Map<Integer, CS2Script> scripts = new HashMap<>();
        for (var entry : SCRIPTS.entrySet()) {
            var script = CS2Script.decodeScript(HexFormat.of().parseHex(entry.getValue()));
            scripts.put(entry.getKey(), script);
            CS2Executor.CS2_CACHE.put(script, entry.getKey().longValue());
        }
        for (String line : args) {
            String[] parts = line.split("\\|", -1);
            int id = Integer.parseInt(parts[0]);
            int[] ints = parts[1].isEmpty() ? new int[0] : Arrays.stream(parts[1].split(",")).mapToInt(Integer::parseInt).toArray();
            String[] strings = parts[2].isEmpty() ? new String[0] : parts[2].equals("~") ? new String[] {""} : parts[2].split(",", -1);
            var root = scripts.get(id);
            var exec = new CS2Executor();
            exec.intLocals = new int[root.intLocalsCount];
            exec.objectLocals = new Object[root.stringLocalsCount];
            exec.longLocals = new long[root.longLocalsCount];
            System.arraycopy(ints, 0, exec.intLocals, 0, ints.length);
            for (int i = 0; i < strings.length; i++) exec.objectLocals[i] = new String(HexFormat.of().parseHex(strings[i]), StandardCharsets.UTF_8);
            CS2Executor.CURRENT_CS2_EXEC_IDX = 1;
            CS2Executor.instructionCount = 0;
            CS2Executor.executeScript(root, 20000, exec);
            var out = new StringBuilder(id + " [" + parts[1] + "] -> n=" + CS2Executor.instructionCount + " ints=");
            for (int i = 0; i < exec.intStackPtr; i++) out.append(i == 0 ? "" : ",").append(exec.intStack[i]);
            out.append(" strings=");
            for (int i = 0; i < exec.stringStackPtr; i++) out.append(i == 0 ? "" : ",").append(hex((String) exec.stringStack[i]));
            System.out.println(out);
        }
    }
}
